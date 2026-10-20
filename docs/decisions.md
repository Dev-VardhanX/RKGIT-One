# RKGIT ONE: Project Decisions

Last updated: 2026-10-19

## Product scope

- The app has three sections: **Notes** (study materials), **Events**, and **Notices**.
- Notices are official college notices such as datesheets, holiday notices, exam notices, and academic circulars.
- Features added after the first plan (decided 2026-10-19): **search**, **material types**, **year and department**, **push notifications**, and **app safety** (crash reports and an update prompt).
- Anything else must be decided and written here first.

## Access

- Public read access for notes, events, and notices.
- No student login.
- Students can browse, open, and download published files without signing in.
- Anyone with a published file URL can access that file.
- Revisit student authentication only if content must be restricted or data must follow a student across devices. That would need student sign-in and signed URLs.

## Content rules

- Notes are organised as Year, Semester, Subject, then files. Semester must belong to the year (Year 1 has semesters 1 and 2, and so on). Subjects are not tied to a department.
- Every file in a subject has a **type**: Notes, Syllabus, ST Exam, PUT Exam, External Exam, or Lab Manual. Files that existed before types were added are Notes.
- Events have a registration link (optional, `http(s)` only).
- Notices have a category (general, academic, exam, datesheet, holiday), an optional attachment (PDF or image), an optional link, a pinned flag, an optional expiry date, and an optional audience (years and departments).
- A notice with no audience is for everyone. Empty means everyone.
- Expired notices are hidden in the app but stay visible to the admin.
- A notice needs a message or an attachment.

## Year and department

- On first launch the app asks for the student's year and department. In the app the word is "Branch"; in the database it is `department`. This wording can change in the UI design phase.
- The choice is stored only on the phone. It can be skipped, and skipping means everything is shown.
- All notices stay available. The Notices list and Home's latest notices are filtered by the student's year and department by default, and the student can switch to All.
- A notice matches a student when its audience is empty or includes the student's year, and its department list is empty or includes the student's department.
- Departments are a table (`departments`, with a stable lowercase code). They are edited in the Supabase dashboard for now. The real list is confirmed with the admin.

## Search

- Search runs in one database function (`search_all`) and not in filters built by the app, so special characters cannot break it.
- It covers notes, events, and active notices. It needs 2 or more characters, is case-insensitive, and is limited to 20 results per kind.
- Offline, search covers downloaded files only.
- No search history.

## Notifications

- Push notifications through Firebase Cloud Messaging (FCM), using topics. No student accounts and no device tokens stored on our server.
- Only notices and events send pushes. Notes do not, because bulk uploads would flood students.
- The admin decides: the portal has a "Notify students" toggle (on by default for new items). Editing an item never sends again, and one item can only be notified once (`notified_at`).
- Notices are targeted by audience through topics for each year and department combination. Events go to everyone who has events turned on.
- Students can turn Notices and Events notifications on or off in Settings.
- A Supabase Edge Function sends the push. This is the only server-side code in the project.

## App safety

- Firebase Crashlytics in release builds only. No personal data is collected, and the app and the store listing say what is collected.
- An `app_config` table holds a minimum and a latest version code. Below the minimum, the app shows a blocking "Update required" screen. Below the latest, it shows a dismissible banner. If the check fails, nothing is blocked.
- `app_config` is edited in the Supabase dashboard for now.

## Files

- Buckets: `study-materials` (25 MB; PDF, DOCX, PPTX), `event-posters` (5 MB; JPEG, PNG, WebP), `notice-files` (10 MB; PDF, JPEG, PNG, WebP).
- Every upload goes to a new UUID path. A replaced file never reuses the old path.
- Order of operations: add uploads the file then inserts the row; replace uploads the new file, updates the row, then deletes the old file; delete removes the row first, then the file.
- In the app, students can open PDFs and also download them to read later offline. This applies to notes and to notice attachments.

## Android application

- Kotlin
- Android Studio
- Jetpack Compose
- MVVM architecture
- Navigation for Compose, Hilt, Room (local index of downloads), WorkManager (downloads), Coil
- PDFs are shown with the built-in `PdfRenderer`
- Firebase Messaging (push) and Firebase Crashlytics (crash reports)
- minSdk 26

## Admin portal

- React
- Vite
- TypeScript
- Tailwind CSS
- shadcn/ui (the components use Base UI: use the `render` prop instead of `asChild`)
- TanStack Query, React Hook Form, Zod, React Router
- Static hosting on Cloudflare Pages or Vercel

## Backend

- Supabase
- PostgreSQL
- Supabase Storage
- Supabase Auth for administrators only
- Row Level Security (RLS)
- No custom backend server. The one exception is a Supabase Edge Function that sends push notifications.
- Public sign-ups disabled. Admins are listed in the `admins` table.

## Environments

- Separate Supabase development and production projects.
- Separate Firebase development and production projects. Debug builds use the development project and release builds use the production project, so a test push can never reach real students.
- Database changes must be stored in SQL migration files.
- Development changes must not be applied to production.

## Security

- Never commit secrets.
- Use only public client keys in the Android app and admin portal.
- Never include the service-role key in client applications, the repository, or AI agent prompts.
- The Firebase service-account key lives only in Supabase secrets. It is never in git, in a client, or in an AI agent prompt.
- `google-services.json` is not committed. A template documents what it must contain.
- Keep local environment files out of Git.
- Maintain appropriate `.env.example` files.
- Do not store the admin email or password in migrations or the repository.

## Process

- Build order: backend, admin portal, then Android app, then test, then release. See `build-order.md`. New work is added as new phases and finished phases are not rewritten.
- The AI agent (Antigravity) may write code, but every SQL migration is read before it is applied, and nothing runs against production without approval.
- Each Android step is given to the agent as a self-contained loop prompt: build, install on the phone, check through the UI, fix, repeat. The agent must not commit, must not read `local.properties` or any key, must not call the Supabase API itself, must keep temporary files outside the repo, and must restore the phone's connectivity and rotation settings to the recorded originals.
- Checks that cannot be automated are written down and done by hand in Phase 4.
