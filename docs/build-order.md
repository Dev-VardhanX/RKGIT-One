# RKGIT ONE: Final Build Order

Last updated: 2026-10-15

Build the backend first, then the admin portal, then the Android app, then test and release. The portal comes before the app because it creates data in exactly the shape the app reads (unique file paths, file metadata, registration links), so the app is never tested against hand-typed rows. Each phase ends with a check; do not start the next phase until it passes.

The app has three sections: Notes (study materials), Events, and Notices. Notices were added after the first plan, so they have their own phases (1b and 2b) that come before the Android app.

## Progress

| Phase | Status |
|---|---|
| Phase 0: Decide and set up | Done |
| Phase 1: Backend | Done |
| Phase 2: Admin portal (notes, events, dashboard, deploy) | Done, site is live |
| Phase 1b: Notices backend | Next |
| Phase 2b: Notices in the admin portal | After 1b |
| Phase 3: Android app | After 2b |
| Phase 4: Test and harden | After 3 |
| Phase 5: Release | After 4 |

## Phase 0: Decide and set up (done)

- [x] Decide public or student-only access to materials. v1 is public read with no student login.
- [x] Create the GitHub monorepo (`android/`, `admin-portal/`, `docs/`, `supabase/`) and a Supabase dev project, and link the Supabase CLI.
- [x] Add `.gitignore`, `docs/decisions.md`, and the Antigravity project rules.

Done when: the decision is written down and the repo and dev project exist.

## Phase 1: Backend (done)

- [x] Migrations: tables (`admins`, `subjects`, `study_materials`, `events`), indexes, `updated_at` triggers, `is_admin()`, and RLS policies.
- [x] Buckets `study-materials` and `event-posters` with MIME and size limits, then the storage policies.
- [x] Public sign-ups disabled, admin user created, and its `user_id` inserted into `admins`.
- [x] Permission tests with the anon key and the admin login.

Done when: anon can read but cannot write rows or files, and the admin can do both.

## Phase 2: Admin portal (done)

- [x] Scaffold Vite, React, TypeScript, Tailwind, shadcn/ui, and the Supabase client.
- [x] Login, the check against `admins`, protected routes, and sign out.
- [x] Layout with sidebar.
- [x] Notes: list, search and filters, add, edit, replace file, and delete. Subjects are looked up or created on demand, with a text field that suggests existing subjects.
- [x] Events: list, add, edit, delete, poster upload, and registration link validation (must start with https://).
- [x] Dashboard: counts and latest items.
- [x] Search, filters, empty, loading, and error states, confirmation dialogs, and a responsive check at phone width.
- [x] Deploy to Cloudflare Pages or Vercel with the anon key only.

Rules used throughout:

- Every upload goes to a new UUID path, and a replaced file never reuses the old path.
- Add: upload the file, then insert the row, and delete the file if the insert fails.
- Replace: upload the new file, update the row, then delete the old file.
- Delete: delete the row first, then the file.

Done when: you can add, replace, and delete a PDF and an event from the deployed site, and the database and storage show no orphaned rows or files.

## Phase 1b: Notices backend (next)

1. Create a migration `notices` with the `notices` table, indexes, the `updated_at` trigger, and RLS (public read, admin write).
2. In the same migration, create the `notice-files` bucket (10 MB, PDF, JPEG, PNG, WebP) and its admin-only storage policies.
3. Apply it with `supabase db push`.
4. Test with the anon key and the admin login:
   - Anon can read notices, and anon writes and file uploads are blocked.
   - The admin can insert a notice.
   - These inserts are rejected: no message and no file, an unknown category, a `javascript:` link, and a half-filled attachment (path without name).
5. Delete the test rows, then commit and push.

Done when: the table and bucket exist, all permission checks pass, and the migration is committed.

## Phase 2b: Notices in the admin portal

1. Add the Notices data layer and hooks, following the same pattern as notes (upload then insert, cleanup on failure, replace file, delete row then file).
2. Notices list: search, a category filter, and indicators for pinned and expired notices, with loading, empty, and error states.
3. Notice form: Title, Message, Category, optional attachment (PDF or image, up to 10 MB, with replace and remove), optional link (https only), Pinned toggle, and optional Expires on date.
4. Add Notices to the sidebar and to the dashboard (count and latest notices).
5. Deploy and add real content: a datesheet PDF, a holiday notice, and a text-only notice.

Done when: you can add, edit, replace the attachment of, and delete a notice from the deployed site, and the database and the `notice-files` bucket show no orphans.

## Phase 3: Android app

1. Setup: theme, navigation (bottom bar with Home, Notes, Events, Notices), Supabase client, `UiState`, shared loading, empty, and error components, and Hilt.
2. Events: list, detail, poster, and the Register button (Custom Tab, with a check that the link is http or https).
3. Notes browsing: Year, Semester, Subject, then the list, with no file actions yet.
4. Notices: list (pinned first, then newest, category chips, expired hidden) and detail with the message and link button. The attachment button comes in step 7.
5. Home screen: latest notices, recent notes, and upcoming events.
6. Open PDF: fetch to cache, `PdfRenderer` viewer with zoom and resume page, and Open in another app through `FileProvider`.
7. Attachments for notices: open PDFs in the same viewer, open images in an image viewer.
8. Persistent Download for notes and notice attachments: Room (`downloads` table with a `kind` column), WorkManager, progress notification (`POST_NOTIFICATIONS`), atomic `.part` rename, and storage and mobile-data checks.
9. Downloads screen: both kinds of files, remove, update detection by comparing `file_path`, and the "No longer available online" label.
10. Offline hints and Retry on every online screen.

Done when: a PDF added in the portal opens in the app, downloads, and opens again in airplane mode, for both a note and a notice attachment.

## Phase 4: Test and harden

- Replace a file in the portal: the app shows Update available. Delete an item: the app keeps the copy and labels it.
- A notice past its expiry date is hidden in the app, and pinned notices come first.
- Replacing or deleting a notice attachment removes the old file from the bucket.
- Kill the app mid-download, fill the storage, and open a corrupt PDF and a password-protected PDF.
- Test on Android 13 or newer (notifications), on an older low-memory phone, and with a PDF of about 25 MB.
- Re-test RLS on all tables and buckets, and confirm no service-role key exists anywhere in the repo or the builds.
- Check the portal on a phone browser, tablet, and desktop.

Done when: every case above behaves as described.

## Phase 5: Release

1. Create the production Supabase project, run the same migrations, and create the production buckets, policies, and admin user.
2. Point the portal and the app at production using separate environment values, and redeploy.
3. Build a signed release APK or AAB and test that release build with shrinking on, not only the debug build.
4. Publish (APK or Play Store), export a backup of schema and data, and keep the free-tier project active or upgrade.

Done when: a fresh install of the release build reads production data created from the production portal.