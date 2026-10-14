# RKGIT ONE: Final Build Order

Oct 13, 2026 · @Dev

Build the backend first, then the admin portal, then the Android app, then test and release. The portal comes before the app because it creates data in exactly the shape the app reads (unique file paths, file metadata, registration links), so the app is never tested against hand-typed rows. Each phase ends with a check; do not start the next phase until it passes.

## Phase 0: Decide and set up

- Decide public or student-only access to materials. The v1 default is public read with no student login. Student-only access would change the auth and storage design, so settle it now.
- Create the GitHub monorepo (`android/`, `admin-portal/`, `supabase/migrations/`) and a Supabase dev project, and install the Supabase CLI.
- Done when: the decision is written down and the empty repo and dev project exist.

## Phase 1: Backend

1. Run the migrations: tables (`admins`, `subjects`, `study_materials`, `events`), indexes, `updated_at` triggers, `is_admin()`, and RLS policies.
2. Create the `study-materials` and `event-posters` buckets with MIME and size limits, then the storage policies.
3. Disable public sign-ups, create the admin user, and insert its `user_id` into `admins`.
4. Test with the anon key and the admin login: anon can read but cannot write rows or files, and the admin can do both.

Done when: all four permission checks pass.

## Phase 2: Admin portal

1. Scaffold Vite, React, TypeScript, Tailwind, shadcn/ui, and the Supabase client, with the layout and sidebar.
2. Login, the check against `admins`, protected routes, and sign out.
3. Materials: list, subject combobox with create, add, replace file, edit, and delete.
4. Events: list, add, edit, delete, poster upload, and registration link validation (must start with https://).
5. Dashboard: counts and latest items.
6. Search, filters, empty, loading, and error states, confirmation dialogs, and a responsive check at phone width.
7. Deploy to Cloudflare Pages or Vercel with the anon key only, then add real content: a few subjects, PDFs, and events with registration links.

Key rules while building:

- Every upload goes to a new UUID path, and a replaced file never reuses the old path.
- Add: upload the file, then insert the row, and delete the file if the insert fails.
- Replace: upload the new file, update the row, then delete the old file.
- Delete: delete the row first, then the file.

Done when: you can add, replace, and delete a PDF and an event from the deployed site, and the database and storage show no orphaned rows or files.

## Phase 3: Android app

1. Setup: theme, navigation, Supabase client, `UiState`, shared loading, empty, and error components, and Hilt.
2. Events: list, detail, poster, and the Register button (Custom Tab, with a check that the link is http or https).
3. Materials browsing: Year, Semester, Subject, then the material list, with no file actions yet.
4. Home screen: recent materials and upcoming events.
5. Open PDF: fetch to cache, `PdfRenderer` viewer with zoom and resume page, and Open in another app through `FileProvider`.
6. Persistent Download: Room, WorkManager, progress notification (`POST_NOTIFICATIONS`), atomic `.part` rename, and storage and mobile-data checks.
7. Downloads screen: remove, update detection by comparing `file_path`, and the "No longer available online" label.
8. Offline hints and Retry on every online screen.

Done when: a PDF added in the portal opens in the app, downloads, and opens again in airplane mode.

## Phase 4: Test and harden

- Replace a file in the portal: the app shows Update available. Delete a material: the app keeps the copy and labels it.
- Kill the app mid-download, fill the storage, and open a corrupt PDF and a password-protected PDF.
- Test on Android 13 or newer (notifications), on an older low-memory phone, and with a PDF of about 25 MB.
- Re-test RLS, and confirm no service-role key exists anywhere in the repo or the builds.
- Check the portal on a phone browser, tablet, and desktop.

Done when: every case above behaves as described.

## Phase 5: Release

1. Create the production Supabase project, run the same migrations, and create the production buckets, policies, and admin user.
2. Point the portal and the app at production using separate environment values, and redeploy.
3. Build a signed release APK or AAB and test that release build with shrinking on, not only the debug build.
4. Publish (APK or Play Store), export a backup of schema and data, and keep the free-tier project active or upgrade.

Done when: a fresh install of the release build reads production data created from the production portal.
