# RKGIT ONE: Final System Design

Native Android student app and web admin portal on a shared Supabase backend.

## 1. Overview

RKGIT ONE is a college app that gives students one place to find study materials and stay informed about college events.

- **Student Android app:** browse study materials (Year → Semester → Subject → Materials), open or download PDFs for offline reading, and view college events with a registration link.
- **Admin portal (website):** an administrator logs in to manage study materials and events.
- **Shared backend:** both interfaces talk to the same Supabase project, so anything the admin publishes shows up in the app on the next fetch.

### v1 scope (nothing else)

1. Student access to study materials, organised by year, semester, subject
2. Open a PDF in the app, and download it to read later offline
3. Recent and upcoming events, with event details and a registration link
4. Admin login
5. Admin management of study materials and events
6. Secure backend access
7. Polished, responsive admin interface

## 2. Technology stack

Only the admin web platform changed from the original plan. Everything else is as specified.

| Component | Technology |
| --- | --- |
| Student app | Native Android, Kotlin, Android Studio |
| UI | Jetpack Compose |
| Architecture | MVVM |
| Navigation | Navigation for Compose |
| **Admin website (changed)** | **React + Vite + TypeScript, Tailwind CSS, shadcn/ui** |
| Web data and forms | supabase-js, TanStack Query, React Hook Form + Zod, React Router |
| Backend | Supabase |
| Database | PostgreSQL (Supabase) |
| File storage | Supabase Storage |
| Authentication | Supabase Auth |
| Access control | Row Level Security and storage policies |
| Website hosting | Cloudflare Pages or Vercel (static build) |
| Version control | Git and GitHub |

**Android supporting libraries:** supabase-kt (Postgrest), Ktor and kotlinx.serialization, Coil (images), Hilt (DI, or manual DI), Room (local index of downloads), WorkManager (downloads), Coroutines and Flow.

**Why React instead of plain HTML/CSS/JS:** the portal is a form- and table-heavy app. React gives structure for state, routing, and validation. shadcn/ui provides the premium look (sidebar, cards, tables, dialogs, toasts). TypeScript catches wrong column names at build time. It is still a static site with no server, so it deploys to the same free hosts.

## 3. Architecture

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
│                              event-posters       │
└──────────────────────────────────────────────────┘
```

Principles:

- No custom server. Supabase's generated REST API plus RLS is the entire backend.
- Both clients use only the public anon key. Security comes from RLS and storage policies, not from hiding keys.
- The service-role key never appears in the Android app, the website code, or the repository.
- The student app is read-only and needs no login. The admin portal is the only writer.

## 4. Data model

```
subjects ──< study_materials
events
admins   (allow-list of auth users)
```

### 4.1 Schema

```sql
-- Admin allow-list
create table admins (
  user_id uuid primary key references auth.users(id) on delete cascade
);

create function is_admin() returns boolean
language sql security definer set search_path = public stable as $$
  select exists (select 1 from admins where user_id = auth.uid());
$$;

-- Subjects
create table subjects (
  id         uuid primary key default gen_random_uuid(),
  year       smallint not null check (year between 1 and 4),
  semester   smallint not null check (semester between 1 and 8),
  name       text not null check (length(trim(name)) > 0),
  created_at timestamptz not null default now(),
  check (semester between year * 2 - 1 and year * 2)
);
create unique index subjects_unique on subjects (year, semester, lower(name));

-- Study materials
create table study_materials (
  id          uuid primary key default gen_random_uuid(),
  subject_id  uuid not null references subjects(id) on delete restrict,
  title       text not null,
  description text,
  file_path   text not null,   -- path inside the bucket
  file_name   text not null,   -- original name shown to students
  file_type   text not null,   -- MIME type
  file_size   bigint not null, -- bytes
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create index on study_materials (subject_id);
create index on study_materials (created_at desc);

-- Events
create table events (
  id               uuid primary key default gen_random_uuid(),
  title            text not null,
  description      text,
  event_date       timestamptz not null,
  venue            text,
  poster_path      text,   -- path inside event-posters bucket
  registration_url text check (registration_url ~* '^https?://'),
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);
create index on events (event_date);

-- updated_at trigger
create function touch_updated_at() returns trigger language plpgsql as $$
begin new.updated_at = now(); return new; end $$;
create trigger t_materials before update on study_materials
  for each row execute function touch_updated_at();
create trigger t_events before update on events
  for each row execute function touch_updated_at();
```

Design notes:

- **Upcoming vs recent** events are derived by comparing `event_date` with the current time. There is no status column to keep in sync.
- **Semester check:** Year 1 allows semesters 1 and 2, Year 2 allows 3 and 4, and so on.
- **`on delete restrict`** prevents deleting a subject that still has materials.
- **Registration link:** `registration_url` is optional. The database only accepts `http(s)` URLs, which blocks `javascript:` and similar schemes. The portal validates it as well for friendly errors.

### 4.2 Row Level Security

```sql
alter table subjects        enable row level security;
alter table study_materials enable row level security;
alter table events          enable row level security;
alter table admins          enable row level security;

-- Everyone (the app) can read
create policy "public read" on subjects        for select using (true);
create policy "public read" on study_materials for select using (true);
create policy "public read" on events          for select using (true);

-- Only admins can write
create policy "admin write" on subjects        for all using (is_admin()) with check (is_admin());
create policy "admin write" on study_materials for all using (is_admin()) with check (is_admin());
create policy "admin write" on events          for all using (is_admin()) with check (is_admin());

-- A user can check only their own admin row; nobody edits it via the API
create policy "self read" on admins for select using (user_id = auth.uid());
```

### 4.3 Storage

| Bucket | Access | Allowed types | Limit |
| --- | --- | --- | --- |
| `study-materials` | public read | PDF first; DOCX/PPTX optional | e.g. 25 MB |
| `event-posters` | public read | JPEG, PNG, WebP | e.g. 5 MB |

Bucket-level MIME and size limits are enforced by Supabase itself. Paths are unguessable and **always new on upload**: `materials/{uuid}/{sanitized-name}` and `posters/{uuid}.webp`. A replaced file therefore always gets a new path, which is how the app detects updates.

```sql
create policy "admin upload" on storage.objects for insert
  with check (bucket_id in ('study-materials','event-posters') and is_admin());
create policy "admin update" on storage.objects for update
  using (bucket_id in ('study-materials','event-posters') and is_admin());
create policy "admin delete" on storage.objects for delete
  using (bucket_id in ('study-materials','event-posters') and is_admin());
```

The buckets are public-read because students do not log in. Unguessable paths make casual discovery unlikely, but anyone with a URL can open the file. If content must ever be restricted to RKGIT students only, add student sign-in and signed URLs. That is outside v1 and should be decided before the backend is built.

### 4.4 Authentication setup

- Disable public sign-ups in Supabase Auth settings.
- Create the admin user manually in the dashboard, then insert their `user_id` into `admins`.
- Even if an account were created some other way, `is_admin()` would reject every write.

## 5. Student Android app

### 5.1 Features

- **Home:** recent materials, recent and upcoming events, shortcuts to Materials and Events.
- **Materials:** Year → Semester → Subject → list of materials, each showing title, file type, size, and date. Actions: Open, Download.
- **Events:** list with poster, name, date and time, venue. Event detail shows the full description, the poster, and a **Register** button when `registration_url` exists.
- **Downloads:** every file saved for offline reading, available without internet.
- Every screen has loading, empty, and error (with Retry) states, and a consistent visual identity.

### 5.2 Package structure

```
com.rkgit.one
├── data
│   ├── remote       Supabase client, DTOs (@Serializable)
│   ├── local        AppDatabase, DownloadDao, DownloadedMaterial (Room)
│   ├── download     DownloadWorker, DownloadRepository
│   ├── repository   MaterialsRepository, EventsRepository
│   └── mapper       DTO → domain model
├── domain
│   └── model        Subject, StudyMaterial, Event
├── ui
│   ├── navigation   NavGraph, Routes
│   ├── theme        Color, Type, Shape, Theme
│   ├── components   LoadingView, EmptyView, ErrorView, MaterialCard, EventCard
│   └── screens
│       ├── home
│       ├── materials   (YearSemester, Subjects, MaterialList)
│       ├── viewer      (PdfViewerScreen, PdfPageCache)
│       ├── downloads   (DownloadsScreen)
│       └── events      (EventList, EventDetail)
└── MainActivity, App
```

### 5.3 MVVM and UI state

Each screen has a ViewModel exposing a `StateFlow<UiState<T>>`. Composables render loading, empty, error, or content from that single state.

```kotlin
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
```

### 5.4 Navigation

```
Bottom bar: Home | Materials | Events

Materials ─ Subjects(year, semester) ─ MaterialList(subjectId) ─ PdfViewer(materialId)
Downloads ─────────────────────────────────────────────────── PdfViewer(materialId)
Events ─ EventDetail(eventId)
```

Downloads is reached from the Materials tab (and optionally Home).

### 5.5 Key queries

```kotlin
// Home: recent materials with subject info
postgrest["study_materials"]
  .select(Columns.raw("*, subjects(name, year, semester)")) {
      order("created_at", Order.DESCENDING); limit(5)
  }

// Subjects for a semester
postgrest["subjects"].select {
    filter { eq("year", y); eq("semester", s) }; order("name", Order.ASCENDING)
}

// Materials for a subject
postgrest["study_materials"].select {
    filter { eq("subject_id", id) }; order("created_at", Order.DESCENDING)
}

// Events: upcoming ascending, recent past descending (two queries)
postgrest["events"].select { filter { gte("event_date", nowIso) }; order("event_date", Order.ASCENDING) }
postgrest["events"].select { filter { lt("event_date", nowIso) };  order("event_date", Order.DESCENDING); limit(10) }
```

### 5.6 Opening and downloading files

The PDF viewer always renders a **local file**. The two buttons differ only in where the file lives.

| Action | Behaviour | File location |
| --- | --- | --- |
| **Open** | Use the downloaded copy if it exists, otherwise fetch it first | `cacheDir` (Android may clear it) |
| **Download** | Save for offline use; move from cache if already fetched | `filesDir/materials/` (persistent, app-private) |

App-private storage needs no storage permission. The only added permission is `POST_NOTIFICATIONS` (Android 13+) for download progress.

**Per-material state**

```kotlin
sealed interface DownloadState {
    data object NotDownloaded : DownloadState
    data class Downloading(val progress: Float) : DownloadState
    data object Downloaded : DownloadState
    data object UpdateAvailable : DownloadState
    data class Failed(val reason: String) : DownloadState
}
```

`DownloadRepository` combines Room rows, WorkManager progress, and the material's current `file_path` into one `Flow<DownloadState>` per material, so cards update live.

**Local index (Room)**

```kotlin
@Entity(tableName = "downloads")
data class DownloadedMaterial(
    @PrimaryKey val materialId: String,
    val title: String, val subjectName: String,
    val year: Int, val semester: Int,
    val fileName: String, val fileType: String, val sizeBytes: Long,
    val remoteFilePath: String,   // file_path at download time
    val localPath: String,
    val downloadedAt: Long
)
```

Room lets the Downloads screen work offline. **Update detection** compares `remoteFilePath` with the current `file_path`. Comparing `updated_at` would trigger false updates when only the title was edited.

**Download procedure (WorkManager + OkHttp)**

1. Build the public URL from the bucket and `file_path`.
2. Stream into `{materialId}.pdf.part`, reporting progress from `Content-Length`.
3. On completion, rename to `{materialId}.pdf` (atomic), then insert the Room row.
4. On failure or cancel, delete the `.part` file and set `Failed`.
5. On app start, delete leftover `.part` files.
6. Before starting, check free space against `file_size`. For large files (about 10 MB or more) on mobile data, ask for confirmation.

**PDF viewer:** Android's built-in `PdfRenderer`, no third-party library.

- Pages render to bitmaps at screen width on a background thread, shown in a `LazyColumn`, with an `LruCache` to keep memory low.
- Pinch-to-zoom via `transformable`, page indicator ("Page 3 / 24"), and resume at the last page read.
- Close the renderer and file descriptor and recycle bitmaps when leaving the screen.
- `PdfRenderer` cannot open password-protected PDFs. On any error, offer **Open in another app** or **Re-download**.

**Other file types (DOCX, PPTX):** download, then open with `Intent.ACTION_VIEW` through a `FileProvider`, so a system app handles them. The same action serves as "Open in another app" for PDFs.

**Registration link:** the Register button opens `registration_url` in a Custom Tab. Check that the scheme is `http` or `https` before opening, as a second layer of defense.

### 5.7 Offline behaviour

- The Downloads screen reads only from Room, so it works fully offline. It groups files by year, semester, and subject, shows sizes, and supports delete and clear all.
- When offline, the online screens show their normal error state with Retry, plus a hint: "You're offline. View your Downloads."
- The full catalog is not cached in v1. Offline means files the student chose to download.
- If the admin deletes a material, the local copy stays readable and is labelled "No longer available online" after the next online refresh.

## 6. Admin portal

### 6.1 Structure

```
admin-portal/
├── src/
│   ├── lib/          supabase client, zod schemas, upload and URL helpers
│   ├── hooks/        useMaterials, useEvents, useSubjects (TanStack Query)
│   ├── components/   Sidebar, StatCard, DataTable, ConfirmDialog, FileDropzone, EmptyState
│   ├── pages/        Login, Dashboard, Materials, MaterialForm, Events, EventForm
│   ├── routes.tsx    protected routes
│   └── main.tsx
├── .env              VITE_SUPABASE_URL, VITE_SUPABASE_ANON_KEY (anon key only)
└── vite.config.ts
```

### 6.2 Pages

- **Login:** email and password through Supabase Auth. After login, confirm the user exists in `admins`, otherwise sign out and show "Not authorized." No registration page exists.
- **Dashboard:** total materials and events, the latest materials and events, and quick links to both management sections.
- **Materials:** searchable table with year, semester, and subject filters, and Edit and Delete actions. The form has Title, Year, Semester, a Subject combobox (choose existing or type a new one), and a file dropzone.
- **Events:** table with search and an Upcoming/Past filter. The form has Title, Date and time, Venue, Description, Poster upload, and an optional **Registration link** (must start with https://, validated on input).

### 6.3 Design

Light theme with white or soft-gray backgrounds, clear typography and hierarchy, consistent spacing, a simple sidebar (a drawer on mobile), dashboard cards, clean tables, professional forms, search and filters where useful, success and error toasts, confirmation dialogs for destructive actions, loading and empty states, and responsive layouts for desktop, tablet, and mobile browsers.

### 6.4 Critical flows

**Add material**

1. Validate with Zod (file type and size are checked on the client too).
2. If the subject is new, upsert it on (year, semester, name).
3. Upload the file to `study-materials/materials/{uuid}/{name}`.
4. Insert the `study_materials` row.
5. If step 4 fails, delete the uploaded file to avoid orphans, and show an error toast.

**Replace a file:** upload the new file to a new UUID path, update the row (`file_path`, `file_name`, `file_type`, `file_size`), then delete the old file. Never delete the old file before the row points to the new one.

**Delete:** confirmation dialog, delete the row first, then the file. An orphaned file is harmless, but a row pointing to a missing file is not.

**Events and posters:** the same upload, replace, and delete pattern with the `event-posters` bucket. Resize and compress posters in the browser (for example, max 1200 px wide) before upload.

## 7. Cross-cutting concerns

| Concern | Approach |
| --- | --- |
| Security | RLS on every table, storage policies, sign-ups disabled, admin allow-list, anon key only in clients |
| Validation | Zod on the client for UX, database constraints as the real guard, bucket MIME and size limits |
| Sync | Both clients use the same API. The app fetches fresh data on screen load and pull-to-refresh. No push or sync layer needed |
| Performance | Indexes on `subject_id`, `created_at`, `event_date`. Paginate with `.range()` if lists grow beyond about 50 items |
| Errors | Failures map to `UiState.Error` with Retry (Android) and toasts (web) |
| Environments | Separate Supabase projects for dev and prod. Schema lives as SQL migrations in the repo (Supabase CLI) |
| Backups | Supabase backups on paid plans. On the free plan, export schema and data periodically |
| Free-tier caution | Free Supabase projects pause after inactivity. Keep the project active or upgrade before launch |

## 8. Repository layout

```
rkgit-one/
├── android/          Android Studio project
├── admin-portal/     React + Vite app
├── supabase/
│   └── migrations/   SQL from section 4
└── README.md
```

## 9. Build order

1. **Backend:** create the Supabase project, run the migrations, create the buckets and policies, create the admin user, and insert test data by hand.
2. **Android read-only flow:** Home, Materials (Year → Semester → Subject → list), Events with details and the Register button.
3. **PDF flow, in two steps:** first Open (download to cache, render in the viewer), then persistent Download with Room and WorkManager, the Downloads screen, and update detection.
4. **Admin portal:** login and route protection, then Materials CRUD, Events CRUD, and the Dashboard last.
5. **Polish:** empty and error states, responsive checks, file limits, poster compression.
6. **Deploy:** portal to Cloudflare Pages or Vercel, and an APK or Play Store build for the app.

## 10. Edge cases checklist

- Interrupted download: `.part` file removed, Retry shown.
- Low storage: space pre-check with a clear message.
- Corrupt or protected PDF: viewer error with Open in another app or Re-download.
- Double tap on Download: unique work keyed by `materialId`, no duplicate jobs.
- Subject renamed or moved: no effect on downloads, which are keyed by `materialId`.
- Admin replaces a file: students see Update available and keep the old copy until they update.
- Admin deletes a material: local copy stays, labelled no longer available online.
- Invalid registration link: rejected by the portal form and by the database check.

## 11. Open decision

**Public or student-only access to materials.** v1 assumes public read with no student login. If materials should be restricted to RKGIT students, add student authentication and signed URLs. Settle this before the backend is built, because it changes the auth and storage design.
