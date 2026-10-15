# RKGIT ONE: Project Decisions

## Product scope

- The app has three sections: **Notes** (study materials), **Events**, and **Notices**.
- Notices are official college notices such as datesheets, holiday notices, exam notices, and academic circulars.
- No other features in v1. Anything new must be decided and written here first.

## Access

- Public read access for notes, events, and notices.
- No student login in v1.
- Students can browse, open, and download published files without signing in.
- Anyone with a published file URL can access that file.
- Revisit student authentication only if content must be restricted. That would need student sign-in and signed URLs.

## Content rules

- Notes are organised as Year, Semester, Subject, then files. Semester must belong to the year (Year 1 has semesters 1 and 2, and so on).
- Events have a registration link (optional, `http(s)` only).
- Notices have a category (general, academic, exam, datesheet, holiday), an optional attachment (PDF or image), an optional link, a pinned flag, and an optional expiry date.
- Expired notices are hidden in the app but stay visible to the admin.
- A notice needs a message or an attachment.

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

## Admin portal

- React
- Vite
- TypeScript
- Tailwind CSS
- shadcn/ui
- TanStack Query, React Hook Form, Zod, React Router
- Static hosting on Cloudflare Pages or Vercel

## Backend

- Supabase
- PostgreSQL
- Supabase Storage
- Supabase Auth for administrators only
- Row Level Security (RLS)
- No custom backend server
- Public sign-ups disabled. Admins are listed in the `admins` table.

## Environments

- Separate Supabase development and production projects.
- Database changes must be stored in SQL migration files.
- Development changes must not be applied to production.

## Security

- Never commit secrets.
- Use only public client keys in the Android app and admin portal.
- Never include the service-role key in client applications, the repository, or AI agent prompts.
- Keep local environment files out of Git.
- Maintain appropriate `.env.example` files.
- Do not store the admin email or password in migrations or the repository.

## Process

- Build order: backend, admin portal, then Android app, then test, then release. See `build-order.md`.
- The AI agent (Antigravity) may write code, but every SQL migration is read before it is applied, and nothing runs against production without approval.