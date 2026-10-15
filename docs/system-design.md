# RKGIT ONE: System Design

Native Android student app and web admin portal on a shared Supabase backend.

Last updated: 2026-10-15

## 1. Overview

RKGIT ONE is a college app that gives students one place to find study notes, college events, and official notices.

The app has **three sections**:

| Section | What it holds | Who manages it |
|---|---|---|
| **Notes** (study materials) | PDFs and documents organised by Year, Semester, Subject | Admin portal |
| **Events** | College events with date, venue, description, poster, and a registration link | Admin portal |
| **Notices** | Official notices such as datesheets, holiday notices, exam notices, academic circulars | Admin portal |

Two interfaces share one backend:

- **Student Android app:** browse notes, events, and notices. Open PDFs in the app, and download them to read later offline.
- **Admin portal (website):** an administrator logs in to manage all three sections.
- **Shared backend:** Supabase. Anything the admin publishes shows up in the app on its next fetch.

Naming note: the app and docs say "Notes". The database table and code say `study_materials`.

## 2. v1 scope

1. Notes organised by year, semester, and subject
2. Open a PDF in the app, and download it to read later offline
3. Recent and upcoming events, event details, and a registration link
4. Official notices with optional attachment (PDF or image), pinning, and expiry
5. Admin login
6. Admin management of notes, events, and notices
7. Secure backend access
8. Polished, responsive admin interface

Nothing else is added unless it is explicitly decided and written in `decisions.md`.

## 3. Technology stack

| Component | Technology |
|---|---|
| Student app | Native Android, Kotlin, Android Studio |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Navigation | Navigation for Compose |
| Admin website | React + Vite + TypeScript, Tailwind CSS, shadcn/ui |
| Web data and forms | supabase-js, TanStack Query, React Hook Form + Zod, React Router |
| Backend | Supabase |
| Database | PostgreSQL (Supabase) |
| File storage | Supabase Storage |
| Authentication | Supabase Auth (administrators only) |
| Access control | Row Level Security and storage policies |
| Website hosting | Cloudflare Pages or Vercel (static build) |
| Version control | Git and GitHub |

Android supporting libraries: supabase-kt (Postgrest), Ktor and kotlinx.serialization, Coil (images), Hilt (DI), Room (local index of downloads), WorkManager (downloads), Coroutines and Flow.

Why React for the portal: it is a form- and table-heavy app, so React gives structure for state, routing, and validation. shadcn/ui provides the polished look. TypeScript catches wrong column names at build time. It is still a static site with no server.

## 4. Architecture

```
┌──────────────────┐        ┌───────────────────────┐
│ Android app      │        │ Admin portal (SPA)    │
│ Kotlin/Compose   │        │ React + Vite + TS     │
│ MVVM             │        │ Cloudflare / Vercel   │
└────────┬─────────┘        └──────────┬────────────┘
         │ anon key, read-only         │ anon key + admin login
         │ (RLS enforced)              │ (RLS enforced)
         ▼                             ▼
┌──────────────────────────────────────────────────┐
│ Supabase                                         │
│  Auth (admin only)   REST API (PostgREST)        │
│  PostgreSQL + RLS    Storage: study-materials,   │
│                      event-posters, notice-files │
└──────────────────────────────────────────────────┘
```

Principles:

- No custom server. Supabase's generated REST API plus RLS is the entire backend.
- Both clients use only the public anon key. Security comes from RLS and storage policies.
- The service-role key never appears in the Android app, the website code, or the repository.
- The student app is read-only and needs no login. The admin portal is the only writer.

## 5. Data model

```
subjects ──< study_materials
events
notices
admins   (allow-list of auth users)
```

### 5.1 Schema

```sql
-- Admin allow-list
create table public.admins (
  user_id uuid primary key references auth.users(id) on delete cascade
);

create function public.is_admin() returns boolean
language sql security definer set search_path = public stable as $$
  select exists (select 1 from public.admins where user_id = auth.uid());
$$;

-- Subjects
create table public.subjects (
  id         uuid primary key default gen_random_uuid(),
  year       smallint not null check (year between 1 and 4),
  semester   smallint not null check (semester between 1 and 8),
  name       text not null check (length(trim(name)) > 0),
  created_at timestamptz not null default now(),
  check (semester between year * 2 - 1 and year * 2)
);
create unique index subjects_unique on public.subjects (year, semester, lower(name));

-- Notes (study materials)
create table public.study_materials (
  id          uuid primary key default gen_random_uuid(),
  subject_id  uuid not null references public.subjects(id) on delete restrict,
  title       text not null check (length(trim(title)) > 0),
  description text,
  file_path   text not null,
  file_name   text not null,
  file_type   text not null,
  file_size   bigint not null check (file_size > 0),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create index on public.study_materials (subject_id);
create index on public.study_materials (created_at desc);

-- Events
create table public.events (
  id               uuid primary key default gen_random_uuid(),
  title            text not null check (length(trim(title)) > 0),
  description      text,
  event_date       timestamptz not null,
  venue            text,
  poster_path      text,
  registration_url text check (registration_url ~* '^https?://'),
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);
create index on public.events (event_date);

-- Notices
create table public.notices (
  id           uuid primary key default gen_random_uuid(),
  title        text not null check (length(trim(title)) > 0),
  body         text,
  category     text not null default 'general'
               check (category in ('general','academic','exam','datesheet','holiday')),
  pinned       boolean not null default false,
  link_url     text check (link_url ~* '^https?://'),
  file_path    text,
  file_name    text,
  file_type    text,
  file_size    bigint check (file_size > 0),
  published_at timestamptz not null default now(),
  expires_at   timestamptz,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  check (
    (file_path is null) = (file_name is null)
    and (file_path is null) = (file_type is null)
    and (file_path is null) = (file_size is null)
  ),
  check (nullif(trim(coalesce(body, '')), '') is not null or file_path is not null),
  check (expires_at is null or expires_at > published_at)
);
create index on public.notices (pinned desc, published_at desc);
create index on public.notices (expires_at);

-- updated_at trigger
create function public.touch_updated_at() returns trigger
language plpgsql set search_path = public as $$
begin new.updated_at = now(); return new; end $$;

create trigger t_materials_touch before update on public.study_materials
  for each row execute function public.touch_updated_at();
create trigger t_events_touch before update on public.events
  for each row execute function public.touch_updated_at();
create trigger t_notices_touch before update on public.notices
  for each row execute function public.touch_updated_at();
```

Design notes:

- **Upcoming vs recent events** are derived by comparing `event_date` with the current time. There is no status column to keep in sync.
- **Semester check:** Year 1 allows semesters 1 and 2, Year 2 allows 3 and 4, and so on.
- **`on delete restrict`** prevents deleting a subject that still has materials.
- **Registration link and notice link:** optional, and the database only accepts `http(s)` URLs, which blocks `javascript:` and similar schemes. The portal validates too, for friendly errors.
- **Notices:** a notice needs a message or an attachment. The four attachment fields are all set or all empty. Categories are a fixed list. A notice with `expires_at` in the past is hidden by the app, but stays in the database so the admin can still see and edit it.
- **Subjects are created on demand** by the portal: it looks up the subject (case-insensitive), inserts it if missing, and looks again if the insert hits the unique index. A plain upsert does not work against the case-insensitive index.

### 5.2 Row Level Security

```sql
alter table public.subjects        enable row level security;
alter table public.study_materials enable row level security;
alter table public.events          enable row level security;
alter table public.notices         enable row level security;
alter table public.admins          enable row level security;

-- Everyone (the app) can read content
create policy "public read" on public.subjects        for select using (true);
create policy "public read" on public.study_materials for select using (true);
create policy "public read" on public.events          for select using (true);
create policy "public read" on public.notices         for select using (true);

-- Only admins can write
create policy "admin write" on public.subjects        for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write" on public.study_materials for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write" on public.events          for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write" on public.notices         for all using (public.is_admin()) with check (public.is_admin());

-- A signed-in user can read only their own admin row; nobody edits admins via the API
create policy "self read" on public.admins for select using (user_id = auth.uid());
```

### 5.3 Storage

| Bucket | Access | Allowed types | Limit |
|---|---|---|---|
| `study-materials` | public read | PDF, DOCX, PPTX | 25 MB |
| `event-posters` | public read | JPEG, PNG, WebP | 5 MB |
| `notice-files` | public read | PDF, JPEG, PNG, WebP | 10 MB |

MIME types and size limits are enforced by the bucket itself. Each bucket has admin-only select, insert, update, and delete policies on `storage.objects` (`public.is_admin()`).

Paths are unguessable and **always new on upload**:

- `materials/{uuid}/{sanitized-name}`
- `posters/{uuid}.webp`
- `notices/{uuid}/{sanitized-name}`

A replaced file therefore always gets a new path, which is how the app detects that a downloaded file is out of date.

The buckets are public-read because students do not log in. Unguessable paths make casual discovery unlikely, but anyone with a URL can open the file. If content must ever be restricted to RKGIT students only, add student sign-in and signed URLs. That is outside v1.

### 5.4 Authentication setup

- Public sign-ups are disabled in Supabase Auth settings.
- The admin user is created manually in the dashboard, and its `user_id` is inserted into `admins` with a one-off SQL statement (not stored in a migration, so the email stays out of git).
- Even if an account were created some other way, `is_admin()` rejects every write.

## 6. Student Android app

### 6.1 Features

- **Home:** latest notices (pinned first), recent notes, and upcoming events, with shortcuts to each section.
- **Notes:** Year → Semester → Subject → list of notes, each showing title, file type, size, and date. Actions: Open, Download.
- **Events:** list with poster, name, date and time, and venue. Event detail shows the full description, the poster, and a **Register** button when `registration_url` exists.
- **Notices:** list with pinned notices first, then newest, with category chips as a filter. Expired notices are hidden. Notice detail shows the message, an optional link button, and the optional attachment (PDF or image), which can be opened and downloaded.
- **Downloads:** every file saved for offline reading (notes and notice attachments), available without internet.
- Every screen has loading, empty, and error (with Retry) states and a consistent visual identity.

### 6.2 Package structure

```
com.rkgit.one
├── data
│   ├── remote       Supabase client, DTOs (@Serializable)
│   ├── local        AppDatabase, DownloadDao, DownloadedFile (Room)
│   ├── download     DownloadWorker, DownloadRepository
│   ├── repository   MaterialsRepository, EventsRepository, NoticesRepository
│   └── mapper       DTO → domain model
├── domain
│   └── model        Subject, StudyMaterial, Event, Notice
├── ui
│   ├── navigation   NavGraph, Routes
│   ├── theme        Color, Type, Shape, Theme
│   ├── components   LoadingView, EmptyView, ErrorView, MaterialCard, EventCard, NoticeCard
│   └── screens
│       ├── home
│       ├── materials   (YearSemester, Subjects, MaterialList)
│       ├── events      (EventList, EventDetail)
│       ├── notices     (NoticeList, NoticeDetail)
│       ├── viewer      (PdfViewerScreen, ImageViewerScreen, PdfPageCache)
│       └── downloads   (DownloadsScreen)
└── MainActivity, App
```

### 6.3 MVVM and UI state

Each screen has a ViewModel exposing a `StateFlow<UiState<T>>`. Composables render loading, empty, error, or content from that single state.

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

### 6.4 Navigation

```
Bottom bar: Home | Notes | Events | Notices

Notes   ─ Subjects(year, semester) ─ MaterialList(subjectId) ─ Viewer(kind, itemId)
Events  ─ EventDetail(eventId)
Notices ─ NoticeDetail(noticeId) ─ Viewer(kind, itemId)
Downloads ─────────────────────────────────────────────────── Viewer(kind, itemId)
```

Downloads is reached from the Notes tab (and optionally Home).

### 6.5 Key queries

```kotlin
// Notes: recent, with subject info
postgrest["study_materials"]
  .select(Columns.raw("*, subjects(name, year, semester)")) {
      order("created_at", Order.DESCENDING); limit(5)
  }

// Subjects for a semester
postgrest["subjects"].select {
    filter { eq("year", y); eq("semester", s) }; order("name", Order.ASCENDING)
}

// Notes for a subject
postgrest["study_materials"].select {
    filter { eq("subject_id", id) }; order("created_at", Order.DESCENDING)
}

// Events: upcoming ascending, recent past descending (two queries)
postgrest["events"].select { filter { gte("event_date", nowIso) }; order("event_date", Order.ASCENDING) }
postgrest["events"].select { filter { lt("event_date", nowIso) };  order("event_date", Order.DESCENDING); limit(10) }
```

Notices use the PostgREST filter `expires_at.is.null,expires_at.gt.<now>` (an `or` filter) to hide expired notices, ordered by `pinned` descending, then `published_at` descending.

### 6.6 Opening and downloading files

The viewer always renders a **local file**. The two actions differ only in where the file lives.

| Action | Behaviour | File location |
|---|---|---|
| **Open** | Use the downloaded copy if it exists, otherwise fetch it first | `cacheDir` (Android may clear it) |
| **Download** | Save for offline use; move from cache if already fetched | `filesDir/downloads/` (persistent, app-private) |

App-private storage needs no storage permission. The only added permission is `POST_NOTIFICATIONS` (Android 13+) for download progress.

**Per-file state**

```kotlin
sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data object Downloaded : DownloadState
    data object UpdateAvailable : DownloadState
    data class Failed(val reason: String) : DownloadState
}
```

`DownloadRepository` combines Room rows, WorkManager progress, and the item's current `file_path` into one `Flow<DownloadState>` per file, so cards update live.

**Local index (Room)** covers both notes and notice attachments:

```kotlin
@Entity(tableName = "downloads", primaryKeys = ["kind", "itemId"])
data class DownloadedFile(
    val kind: String,            // "material" or "notice"
    val itemId: String,
    val title: String,
    val subtitle: String,        // e.g. "Year 1 · Sem 2 · Maths" or the notice category
    val year: Int?, val semester: Int?,   // null for notices
    val fileName: String, val fileType: String, val sizeBytes: Long,
    val remoteFilePath: String,  // file_path at download time
    val localPath: String,
    val downloadedAt: Long
)
```

Room lets the Downloads screen work offline. **Update detection** compares `remoteFilePath` with the item's current `file_path`. Comparing `updated_at` would trigger false updates when only the title was edited.

**Download procedure (WorkManager + OkHttp)**

1. Build the public URL from the bucket and `file_path`.
2. Stream into `{kind}-{itemId}.part`, reporting progress from `Content-Length`.
3. On completion, rename to the final name (atomic), then insert the Room row.
4. On failure or cancel, delete the `.part` file and set `Failed`.
5. On app start, delete leftover `.part` files.
6. Before starting, check free space against `file_size`. For large files (about 10 MB or more) on mobile data, ask for confirmation.

**Viewers**

- **PDF:** Android's built-in `PdfRenderer`. Pages render to bitmaps at screen width on a background thread, shown in a `LazyColumn`, with an `LruCache`. Pinch-to-zoom, a page indicator, and resume at the last page read. Close the renderer and recycle bitmaps when leaving the screen. `PdfRenderer` cannot open password-protected PDFs, so on any error offer **Open in another app** or **Re-download**.
- **Images (notice attachments):** an in-app image viewer with zoom, using Coil.
- **DOCX and PPTX (notes only):** download, then open with `Intent.ACTION_VIEW` through a `FileProvider`. The same action serves as "Open in another app" for PDFs.

**Links:** the Register button (events) and the notice link button open the URL in a Custom Tab. Check that the scheme is `http` or `https` before opening, as a second layer of defense.

### 6.7 Offline behaviour

- The Downloads screen reads only from Room, so it works fully offline. It groups notes by year, semester, and subject, lists notice attachments separately, shows sizes, and supports delete and clear all.
- When offline, the online screens show their normal error state with Retry, plus a hint: "You're offline. View your Downloads."
- The full catalog is not cached in v1. Offline means files the student chose to download.
- If the admin deletes an item, the local copy stays readable and is labelled "No longer available online" after the next online refresh.

## 7. Admin portal

### 7.1 Structure

```
admin-portal/
├── src/
│   ├── auth/         AuthProvider.tsx, ProtectedRoute.tsx
│   ├── lib/          supabase.ts, materials.ts, events.ts, notices.ts (data access and helpers)
│   ├── hooks/        useMaterials.ts, useEvents.ts, useNotices.ts (TanStack Query)
│   ├── components/   AppLayout.tsx, ui/ (shadcn components)
│   ├── pages/        Login, Dashboard, Materials, MaterialForm, Events, EventForm, Notices, NoticeForm
│   └── main.tsx, App.tsx
├── .env.local        VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY (anon key only, never committed)
├── .env.example      same keys, empty values (committed)
└── vite.config.ts
```

### 7.2 Authentication flow

- `AuthProvider` tracks the session through `onAuthStateChange`.
- A separate effect then checks that the signed-in user has a row in `admins`. If not, the user is signed out and sees "This account is not authorized."
- The admin check runs outside the auth callback, because calling Supabase from inside that callback can deadlock.
- `ProtectedRoute` shows a loading state until both checks finish, then either renders the page or redirects to `/login`.
- Login errors are generic ("Invalid email or password"). There is no registration page.

### 7.3 Pages

- **Login:** email and password.
- **Dashboard:** counts of notes, events, and notices, the latest items of each, and quick links to the three management sections.
- **Notes:** searchable table with year, semester, and subject filters, and Edit and Delete actions. The form has Title, Year, Semester, a Subject text field with suggestions from existing subjects (pick one or type a new one), an optional description, and a file picker.
- **Events:** table with search and an Upcoming/Past filter. The form has Title, Date and time, Venue, Description, Poster upload, and an optional **Registration link** (must start with https://).
- **Notices:** table with search and a category filter, showing pinned and expired state. The form has Title, Message, Category, an optional attachment (with the option to remove or replace it), an optional link, a Pinned toggle, and an optional Expires on date.

### 7.4 Design

Light theme with white or soft-gray backgrounds, clear typography and hierarchy, consistent spacing, a simple sidebar (a top bar with a menu on mobile), dashboard cards, clean tables, professional forms, search and filters where useful, success and error toasts, confirmation dialogs for destructive actions, loading and empty states, and responsive layouts for desktop, tablet, and mobile browsers.

### 7.5 Critical flows

These apply to notes, event posters, and notice attachments.

**Add:** validate with Zod (file type and size are checked on the client too), upload the file to a new UUID path, then insert the row. If the insert fails, delete the uploaded file.

**Replace a file:** upload the new file to a new UUID path, update the row to point to it, then delete the old file. Never delete the old file before the row points to the new one.

**Remove a notice attachment:** clear the four attachment fields (only allowed if the notice has a message), then delete the file.

**Delete:** confirmation dialog, delete the row first, then the file. An orphaned file is harmless, but a row pointing to a missing file is not.

**Check results:** updates and deletes confirm that exactly one row changed, so a permission problem shows as an error rather than silently doing nothing.

**Posters:** resize and compress in the browser (for example, max 1200 px wide) before upload.

## 8. Cross-cutting concerns

| Concern | Approach |
|---|---|
| Security | RLS on every table, storage policies on every bucket, sign-ups disabled, admin allow-list, anon key only in clients |
| Validation | Zod on the client for UX, database constraints as the real guard, bucket MIME and size limits |
| Sync | Both clients use the same API. The app fetches fresh data on screen load and pull-to-refresh. No push or sync layer |
| Performance | Indexes on foreign keys and sort columns. Paginate with `.range()` if lists grow beyond about 50 items |
| Errors | Failures map to `UiState.Error` with Retry (Android) and toasts (web) |
| Environments | Separate Supabase projects for dev and prod. Schema lives as SQL migrations in the repo (Supabase CLI) |
| Backups | Supabase backups on paid plans. On the free plan, export schema and data periodically |
| Free-tier caution | Free Supabase projects pause after inactivity. Keep the project active or upgrade before launch |

## 9. Repository layout

```
rkgit-one/
├── android/          Android Studio project
├── admin-portal/     React + Vite app
├── supabase/
│   └── migrations/   init_schema, rls_policies, storage, notices (and later changes)
├── docs/             system-design.md, build-order.md, decisions.md
└── README.md
```

## 10. Edge cases checklist

- Interrupted download: `.part` file removed, Retry shown.
- Low storage: space pre-check with a clear message.
- Corrupt or protected PDF: viewer error with Open in another app or Re-download.
- Double tap on Download: unique work keyed by `kind` and `itemId`, so no duplicate jobs.
- Subject renamed or moved: no effect on downloads, which are keyed by item id.
- Admin replaces a file: students see Update available and keep the old copy until they update.
- Admin deletes an item: local copy stays, labelled no longer available online.
- Invalid registration or notice link: rejected by the portal form and by the database check.
- Notice with an expiry date in the past: hidden in the app, still visible to the admin.
- Notice with only an attachment and no message: allowed. With neither: rejected.

## 11. Resolved decisions

- **Access:** public read, no student login in v1. Student-only access would require student sign-in and signed URLs.
- **Sections:** Notes, Events, and Notices are all in v1.
- **Portal before app:** the portal is built before the Android app so the app is tested against real data.