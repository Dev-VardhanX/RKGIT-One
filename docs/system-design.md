# RKGIT ONE: System Design

Native Android student app and web admin portal on a shared Supabase backend.

Last updated: 2026-10-19

Status: sections 1 to 11 describe the app as built through Phase 3. Sections 12 to 16 describe the features added afterwards (Phases 3A to 3E). In those sections, search has its database function done and tested; everything else is planned.

## 1. Overview

RKGIT ONE is a college app that gives students one place to find study notes, college events, and official notices.

The app has **three sections**:

| Section | What it holds | Who manages it |
|---|---|---|
| **Notes** (study materials) | PDFs and documents organised by Year, Semester, Subject, and (from Phase 3B) by type | Admin portal |
| **Events** | College events with date, venue, description, poster, and a registration link | Admin portal |
| **Notices** | Official notices such as datesheets, holiday notices, exam notices, academic circulars | Admin portal |

Two interfaces share one backend:

- **Student Android app:** browse notes, events, and notices. Open PDFs in the app, and download them to read later offline.
- **Admin portal (website):** an administrator logs in to manage all three sections.
- **Shared backend:** Supabase. Anything the admin publishes shows up in the app on its next fetch.

Naming note: the app and docs say "Notes". The database table and code say `study_materials`.

After the first plan, five features were added (sections 12 to 16): search across notes, events, and notices; material types inside each subject (notes, syllabus, ST exam, PUT exam, external exam, lab manual); a year and department choice with filtered notices; push notifications; and crash reports with an update prompt.

## 2. v1 scope

1. Notes organised by year, semester, and subject
2. Open a PDF in the app, and download it to read later offline
3. Recent and upcoming events, event details, and a registration link
4. Official notices with optional attachment (PDF or image), pinning, and expiry
5. Admin login
6. Admin management of notes, events, and notices
7. Secure backend access
8. Polished, responsive admin interface

Added after the first plan (decided 2026-10-19, see sections 12 to 16):

9. Search across notes, events, and notices
10. Material types inside each subject: Notes, Syllabus, ST Exam, PUT Exam, External Exam, Lab Manual
11. Year and department choice, with notices filtered by it
12. Push notifications for notices and events, targeted by year and department
13. Crash reports and an update prompt

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
| Push notifications *(planned)* | Firebase Cloud Messaging (topics) |
| Notification sender *(planned)* | Supabase Edge Function (TypeScript on Deno) |
| Crash reports *(planned)* | Firebase Crashlytics (release builds only) |
| Version control | Git and GitHub |

Android supporting libraries: supabase-kt (Postgrest), Ktor and kotlinx.serialization, Coil (images), Hilt (DI), Room (local index of downloads), WorkManager (downloads), Coroutines and Flow. Planned: Firebase Messaging and Crashlytics. Minimum Android version: 8.0 (API 26).

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

- No custom server. Supabase's generated REST API plus RLS is the entire backend, with one planned exception: an Edge Function that sends push notifications (section 15).
- Search runs through one database function (section 12) instead of filters built in the app.
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

Tables and columns added by later phases are described in sections 12 to 16: `departments`, `app_config`, and new columns on `study_materials` (`material_type`) and `notices` (`target_years`, `target_departments`, `notified_at`), plus `notified_at` on `events`.

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
- Added by later phases (sections 12 to 16): **Search**, file **types** inside each subject, a first-launch **year and department** setup with a **Settings** screen, notification handling, and an update prompt.

### 6.2 Package structure

```
com.rkgit.one
├── data
│   ├── remote        SupabaseModule, PublicUrls, dto/ (@Serializable DTOs and mappers)
│   ├── local         AppDatabase, DownloadDao, DownloadedFile, DatabaseModule (Room)
│   ├── files         FileKind, FileCache, Downloader, CacheNaming, ContentKind, ViewerPrefs
│   ├── downloads     DownloadRepository, DownloadWorker, DownloadState, DownloadKeys,
│   │                 DownloadsGrouping, RemoteStatus
│   └── repository    MaterialsRepository, EventsRepository, NoticesRepository,
│                     RemoteFilesRepository
├── domain
│   └── model         Subject, StudyMaterial, RecentMaterial, Event, Notice
├── ui
│   ├── navigation    Routes, ViewerRoutes
│   ├── theme         Color, Type, Theme
│   ├── components    ScreenScaffold, StateViews, LoadableContent, OfflineHints
│   ├── util          Format, Links, Network
│   ├── UiState, LoadableViewModel, AppRoot
│   └── screens
│       ├── home
│       ├── materials    (Notes, Subjects, MaterialList)
│       ├── events       (Events, EventDetail)
│       ├── notices      (Notices, NoticeDetail)
│       ├── viewer       (Viewer, PdfPager, image viewer)
│       ├── downloads    (Downloads)
│       ├── search       (planned, section 12)
│       ├── settings     (planned, section 14)
│       └── onboarding   (planned, section 14)
└── RkgitApplication, MainActivity
```

### 6.3 MVVM and UI state

Each screen has a ViewModel exposing a `StateFlow<UiState<T>>`. Composables render loading, error, or content from that single state.

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val offline: Boolean = false) : UiState<Nothing>
}
```

Screens that load from the network extend `LoadableViewModel<T>`, which provides `state`, `refreshing`, `load()`, `refresh()`, and a `refreshErrors` flow. A failed first load becomes an error state. A failed pull-to-refresh keeps the old data and shows a short toast. `LoadableContent` renders the three states inside the pull-to-refresh container. `ErrorView` shows Retry and, when the failure is a connection problem, the hint "You're offline. View your Downloads." with an Open Downloads button (provided through the `LocalOpenDownloads` composition local).

### 6.4 Navigation

```
Bottom bar: Home | Notes | Events | Notices

Home      ─ top bar: Downloads (search and settings planned)
Notes     ─ Subjects(year, semester) ─ MaterialList(subjectId, subjectName, year, semester) ─ Viewer
Events    ─ EventDetail(eventId)
Notices   ─ NoticeDetail(noticeId) ─ Viewer
Downloads ─ Viewer                  (opened from the Notes screen and from Home)
Search    ─ Viewer / EventDetail / NoticeDetail      (planned)
```

The bottom bar is hidden on the Viewer. Tapping the current tab returns to that tab's first screen. Downloads counts as part of the Notes tab for highlighting. All routes are type-safe `@Serializable` classes.

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

Added by later phases: search calls the `search_all` database function (section 12), the notes list is filtered by `material_type` (section 13), and notices are filtered by the student's year and department on the phone (section 14).

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

### 6.8 As built (implementation notes)

- **Build:** minSdk 26; Kotlin 2.2 with the Android Gradle plugin's built-in Kotlin and KSP (two `gradle.properties` workarounds for KSP); supabase-kt 3.6 with Ktor 3.4 (newer supabase-kt versions need a newer Kotlin). The Room schema is exported to `android/app/schemas` (version 1, no destructive migration).
- **Files:** the viewer always renders a local file. Opening fetches into `cacheDir/files/<kind>/`, which is trimmed to 100 MB, and leftover `.part` files are removed at start. A persistent download lives in `filesDir/downloads/<kind>/`. If a complete cached copy exists, a download moves it instead of fetching again.
- **PDF viewer:** `PdfRenderer` with one page open at a time (mutex). Pages are rendered at 1.5 times the screen width (capped at 2048 px) into an LRU cache sized to a sixth of the heap. Zoom (100 to 300 percent) changes the page layout width inside a horizontal scroll, by pinch or by the zoom buttons. The last page read is kept in SharedPreferences. Known behaviour: zooming keeps the horizontal focus but not the vertical position.
- **Image viewer:** Coil decodes at most 2048 px. Zoom 100 to 400 percent by pinch or buttons, with panning limited to the edges.
- **Downloads:** WorkManager with Hilt, as an expedited foreground service of type data sync with a progress notification. One unique job per file, retries on connection errors up to three times, and the Room row is saved before the old file is deleted.
- **Update detection:** the Downloads screen compares each saved remote path with the server's current path (one query per kind). Items are labelled "Update available" or "No longer available online". If the check fails, it shows a caption and no labels, and the list stays local.
- **Offline detection:** an error counts as offline when its cause chain contains a connection exception. Download errors (such as a deleted file) do not count.

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

Added by later phases: a Type dropdown on notes (section 13), an Audience section on notices (section 14), and a Notify students toggle on notices and events (section 15).

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
├── android/          Android Studio project (Room schema exports in app/schemas)
├── admin-portal/     React + Vite app
├── supabase/
│   ├── migrations/   init_schema, rls_policies, storage, notices, search_all (and later changes)
│   └── functions/    send-notification (planned)
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
- Search: results for an item deleted a moment ago open with the normal error; offline search covers downloads only.
- Material types: a file without a type cannot exist, because the default is Notes.
- Audience: department codes never change; renaming a department changes only its name.
- Notifications: a second send for the same item is refused; a deleted item opened from an old notification shows the normal error.
- App safety: the update check never blocks when it fails.

## 11. Resolved decisions

- **Access:** public read, no student login in v1. Student-only access would require student sign-in and signed URLs.
- **Sections:** Notes, Events, and Notices are all in v1.
- **Portal before app:** the portal is built before the Android app so the app is tested against real data.
- **Search:** one database function, not filters built in the app.
- **Types:** six fixed types, stored per file.
- **Year and department:** kept on the phone and can be skipped. Notices are filtered by it, not hidden.
- **Notifications:** Firebase topics, controlled by the admin, for notices and events only. The Edge Function is the only server-side code.
- **App safety:** Crashlytics in release builds only; the update check fails open.

## 12. Search (Phase 3A)

Status: the database function is done and tested; the app screen is next.

### 12.1 What it does

One search across notes, events, and notices, opened from a search icon on the Home top bar.

### 12.2 Backend

A SQL function `search_all(q text, max_per_kind int default 20)`, defined in the migration `*_search_all.sql`. It returns rows with these columns:

| Column | Meaning |
|---|---|
| `kind` | `material`, `event`, or `notice` |
| `id` | the item's id |
| `title` | the item's title |
| `subtitle` | subject name (notes), venue (events), or category (notices) |
| `snippet` | the first 120 characters of the description or message |
| `rank` | 1 when the title matches, higher for other matches |
| `sort_date` | creation, event, or publish date |
| `extra` | JSON with what the app needs to open the item |

`extra` carries, for notes: subject id and name, year, semester, file path, file name, file type, and file size. For events: the event date and venue. For notices: the category, the pinned flag, and whether there is an attachment.

Rules:

- A query shorter than 2 characters returns nothing, and matching is case-insensitive.
- `%`, `_`, and `\` are escaped so they match literally.
- Each kind is capped (20 by default) and ordered by rank, then date.
- Expired notices are excluded.
- The function runs with the caller's permissions (`security invoker`), so the existing public-read policies apply and nothing new is exposed.

The app calls it with `postgrest.rpc("search_all", ...)` instead of building filter strings, because filter strings built from typed text break on commas and brackets.

### 12.3 Android app

- A Search screen with a text field that waits 350 ms after typing stops, needs at least 2 characters, accepts at most 100 characters, and ignores responses to older queries.
- Results grouped into Notes, Events, and Notices, each card showing the title, subtitle, and snippet. A note opens the file viewer directly (the `extra` data has everything the viewer route needs). An event or notice opens its detail screen.
- Loading, empty ("No results for ..."), and error states, with Retry and the offline hint.
- Offline: it searches the downloads table (title, subject, file name) and says so in a caption. No search history is kept.

### 12.4 Edge cases

Whitespace is trimmed. Fast typing cancels the previous request. Special characters are safe. After Phase 3B the results also show the material type. Search always covers everything: it is not filtered by year or department.

## 13. Material types (Phase 3B)

Status: planned.

### 13.1 Types

| Stored value | Label in the app |
|---|---|
| `notes` | Notes |
| `syllabus` | Syllabus |
| `st_exam` | ST Exam |
| `put_exam` | PUT Exam |
| `external_exam` | External Exam |
| `lab_manual` | Lab Manual |

### 13.2 Backend

```sql
alter table public.study_materials
  add column material_type text not null default 'notes'
  check (material_type in ('notes','syllabus','st_exam','put_exam','external_exam','lab_manual'));

create index on public.study_materials (subject_id, material_type);
```

Existing rows become `notes` through the default. A second migration replaces `search_all` so each material result's `extra` also carries `material_type`. The first search migration is not edited.

### 13.3 Admin portal

The notes form gets a required Type dropdown (default Notes). The notes list gets a Type column and a Type filter. An existing note's type can be changed.

### 13.4 Android app

- In the subject screen: filter chips "All" plus the types that have files, in the fixed order above. Cards show a type label.
- Subject cards keep counting all files.
- Home's recent notes and the search results show the type label.
- The Downloads screen is unchanged. The downloads table has no type column, which avoids a database migration in the app.

### 13.5 Notes

Exam papers carry their session in the title (for example "ST-1 2025"). A separate exam year field can be added later if students need to sort by it. Whether the types appear as chips or as folders is decided in the UI design phase (3F).

## 14. Year and department (Phase 3C)

Status: planned.

### 14.1 Purpose

Students choose their year and branch once. All notices stay available, but the list is filtered to the student's year and department by default. The same choice targets push notifications (section 15).

### 14.2 Data model

```sql
create table public.departments (
  id         uuid primary key default gen_random_uuid(),
  code       text not null unique check (code ~ '^[a-z0-9-]{2,20}$'),
  name       text not null check (length(trim(name)) > 0),
  sort_order int not null default 0,
  active     boolean not null default true
);

alter table public.notices
  add column target_years smallint[],
  add column target_departments text[],
  add constraint notices_target_years_valid
    check (target_years is null or target_years <@ array[1,2,3,4]::smallint[]);
```

- `departments` has public read and admin-only write. It is seeded with RKGIT's real departments (to be confirmed with the admin) and edited in the Supabase dashboard for now. The `code` is stable and lowercase, because it is used in notification topic names.
- `target_years` and `target_departments`: null or empty means everyone. A trigger rejects a department code that is not in `departments`.

### 14.3 Relevance rule

A notice is relevant to a student when its years are empty or include the student's year, and its departments are empty or include the student's department code. A student who skipped the setup sees everything. The rule is one pure function that is tested without a device.

### 14.4 Admin portal

An Audience section in the notice form with a Year multi-select and a Department multi-select (default: all). The notices list shows an audience badge ("All", "Year 1", "CSE, IT").

### 14.5 Android app

- First launch: a setup screen with Year (1 to 4) and Branch (from the departments table, cached for offline use) and a Skip option. The choice is kept on the phone only.
- A Settings screen (gear icon on Home): change year and branch, see the app version, and (from Phase 3D) the notification toggles.
- Notices list: year and branch filters default to the student's choice, with an All option for each. The category chips remain.
- Home's latest notices follow the same rule.
- Notes stay organised by year and semester for everyone and are not filtered by branch. If later-year subjects turn out to be branch-specific, a department column can be added to `subjects` in a later phase.

### 14.6 Edge cases

Offline first launch: the department list may not load, so the student can skip and set it later. A department that becomes inactive or is removed: Settings asks the student to choose again. At the start of a new academic year the student updates their year in Settings.

## 15. Notifications (Phase 3D)

Status: planned. Needs Phase 3C.

### 15.1 Principles

- Push messages through Firebase Cloud Messaging (FCM) topics. No student accounts, and no device tokens are stored on our server.
- Only notices and events send pushes. Notes do not.
- The admin decides when to notify, with a toggle in the portal. One item can be notified only once.
- Only the Supabase Edge Function can send. The Firebase service-account key lives in Supabase secrets.

### 15.2 Flow

```
Admin portal ── saves the item ──► Supabase database
      │
      └── calls send-notification (admin login token) ──► Edge Function
                                                          │ checks admin, loads the item,
                                                          │ claims notified_at
                                                          ▼
                                            Firebase Cloud Messaging ──► phones subscribed to the topics
```

### 15.3 Topics

| Topic | Subscribed by | Used for |
|---|---|---|
| `n-all` | students with Notices turned on | notices for everyone |
| `n-y{year}-{dept}` (for example `n-y1-cse`) | students with Notices turned on, matching year and department | targeted notices |
| `e-all` | students with Events turned on | all events |

Topic names only contain lowercase letters, digits, and hyphens, which the department code rule guarantees. A notice with no audience goes to `n-all`. A notice with an audience goes to the `n-y{year}-{dept}` topic of each combination (empty years mean all four years, empty departments mean all active departments). Students who skipped the setup only receive notices for everyone.

### 15.4 Data model

```sql
alter table public.notices add column notified_at timestamptz;
alter table public.events  add column notified_at timestamptz;
```

### 15.5 Edge Function `send-notification`

- Request: `{ "type": "notice" | "event", "id": "<uuid>" }` with the admin's login token. The function checks `is_admin()` with the caller's token, then uses the server-side key to read the item.
- It claims the item by setting `notified_at` only where it is still null, so a second call is refused (409). If sending fails completely, it clears `notified_at` again so the admin can retry.
- Message: data-only (type, id, title, body), high priority, with a short time-to-live (a couple of days) so an old notice does not arrive late.
- Responses: 200 with the topics sent to, 401 or 403 for non-admins, 404 for an unknown item, 409 already notified, and 502 when Firebase rejects the send.
- Secrets: the Firebase service-account JSON is set with `supabase secrets set` for each environment. It is never in git, in a client, or in an agent prompt.

### 15.6 Admin portal

A "Notify students" toggle on the new notice and new event forms (on by default). After saving, the portal calls the function and shows the result. If the call fails, the item stays saved and a "Send notification" action in the list lets the admin retry. Items already notified show a "Notified" badge. Editing never notifies again.

### 15.7 Android app

- A Firebase Messaging service receives the data-only messages and builds the notification itself, so it looks the same in every app state. Two channels: Notices and Events.
- Notice notification: the title is "New notice" with the category, and the text is the notice title. Event notification: the title is "New event", and the text is the event title and date.
- Settings has Notices and Events toggles (on by default). They subscribe or unsubscribe the topics, and the topics are updated when year or department change.
- Android 13 and later: the permission is requested with a short explanation when notifications are first enabled. If it is denied, Settings explains how to allow it in the system settings.
- Tapping a notification opens that notice or event: the app starts at Home with the detail screen on top, or opens the detail directly if it is already running.
- FCM keeps topic subscriptions across token refreshes, so no token handling is needed in our code.

### 15.8 Environments

Separate Firebase projects for development and production. Debug builds use the `google-services.json` of the development project and release builds use the production one, so a test push never reaches real students. The files are not committed; a template documents what they must contain.

### 15.9 Edge cases

- Phones with strict battery savers can delay or drop pushes. Test with the battery setting relaxed.
- Deleting a notice does not recall a push that was already sent. Opening it afterwards shows the normal error.
- Two admins saving at the same moment cannot send twice, because of the `notified_at` claim.
- If an admin changes a notice's audience after it was sent, nothing is sent again.

## 16. App safety: crash reports and update prompt (Phase 3E)

Status: planned. Uses the Firebase project from Phase 3D.

### 16.1 Crash reports

Firebase Crashlytics in release builds only (collection is off in debug builds). It collects crash stack traces, the Android version, the device model, and the app version. No personal data or student choices are attached. The store listing's data safety section and a short privacy statement say so. Unexpected errors (not connection problems) may be logged as non-fatal events.

### 16.2 Data model

```sql
create table public.app_config (
  id                  int primary key default 1 check (id = 1),
  min_version_code    int not null default 1,
  latest_version_code int not null default 1,
  update_url          text check (update_url ~* '^https?://'),
  message             text,
  updated_at          timestamptz not null default now()
);

insert into public.app_config (id) values (1);
```

Public read, admin-only write. It is edited in the Supabase dashboard for now; a portal page can be added later.

### 16.3 Update check in the app

On launch the app reads `app_config` and compares it with its own version code:

| Condition | Result |
|---|---|
| installed < `min_version_code` | blocking "Update required" screen with the message and an Update button that opens `update_url` |
| installed < `latest_version_code` | dismissible banner, shown once per version |
| otherwise, or the check fails or the phone is offline | nothing |

The check never blocks on failure, so a network problem cannot lock students out. The decision is a small pure function that is unit tested.

### 16.4 Edge cases

A wrong `min_version_code` would lock everyone out of the app. Change it first on the development project, and raise it on production only after most students have updated. The update link should point at the store listing or a download page.
