# RKGIT ONE: Final Build Order

Last updated: 2026-10-19

Build the backend first, then the admin portal, then the Android app, then test and release. The portal comes before the app because it creates data in exactly the shape the app reads (unique file paths, file metadata, registration links), so the app is never tested against hand-typed rows. Each phase ends with a check; do not start the next phase until it passes.

The app has three sections: Notes (study materials), Events, and Notices. Notices were added after the first plan, so they have their own phases (1b and 2b) that come before the Android app.

After the Android app was built, five more features were decided on 2026-10-19: search, material types, year and department, push notifications, and app safety (crash reports and an update prompt). They are added as new phases 3A to 3F after Phase 3, followed by the UI design phase. Finished phases are not rewritten; only their status is marked. Each new phase follows the same order as before: backend, then portal, then Android app.

## Progress

| Phase | Status |
|---|---|
| Phase 0: Decide and set up | Done |
| Phase 1: Backend | Done |
| Phase 2: Admin portal (notes, events, dashboard, deploy) | Done, site is live |
| Phase 1b: Notices backend | Done |
| Phase 2b: Notices in the admin portal | Done |
| Phase 3: Android app | Done (all ten steps built and checked on a physical phone) |
| Phase 3A: Search | In progress (backend done, Android app next) |
| Phase 3B: Material types | Planned |
| Phase 3C: Year and department | Planned |
| Phase 3D: Notifications | Planned |
| Phase 3E: App safety | Planned |
| Phase 3F: UI design | Planned |
| Phase 4: Test and harden | Planned |
| Phase 5: Release | Planned |

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

## Phase 1b: Notices backend (done)

1. Create a migration `notices` with the `notices` table, indexes, the `updated_at` trigger, and RLS (public read, admin write).
2. In the same migration, create the `notice-files` bucket (10 MB, PDF, JPEG, PNG, WebP) and its admin-only storage policies.
3. Apply it with `supabase db push`.
4. Test with the anon key and the admin login:
   - Anon can read notices, and anon writes and file uploads are blocked.
   - The admin can insert a notice.
   - These inserts are rejected: no message and no file, an unknown category, a `javascript:` link, and a half-filled attachment (path without name).
5. Delete the test rows, then commit and push.

Done when: the table and bucket exist, all permission checks pass, and the migration is committed.

## Phase 2b: Notices in the admin portal (done)

1. Add the Notices data layer and hooks, following the same pattern as notes (upload then insert, cleanup on failure, replace file, delete row then file).
2. Notices list: search, a category filter, and indicators for pinned and expired notices, with loading, empty, and error states.
3. Notice form: Title, Message, Category, optional attachment (PDF or image, up to 10 MB, with replace and remove), optional link (https only), Pinned toggle, and optional Expires on date.
4. Add Notices to the sidebar and to the dashboard (count and latest notices).
5. Deploy and add real content: a datesheet PDF, a holiday notice, and a text-only notice.

Done when: you can add, edit, replace the attachment of, and delete a notice from the deployed site, and the database and the `notice-files` bucket show no orphans.

## Phase 3: Android app (done)

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

Status: all ten steps are built and were checked on a physical phone with scripted checks and unit tests. The checks that could not be automated are listed under Phase 4.

## Phase 3A: Search

Students look for a file or a notice by name, so search is the first of the new features. It searches notes, events, and notices from one place.

Backend (done):

- [x] A database function `search_all` that searches notes (title, description, subject name, file name), events (title, venue, description), and active notices (title, message). It is case-insensitive, needs 2 or more characters, escapes `%`, `_`, and `\`, limits each kind to 20 results, and runs with the caller's permissions so the public-read rules apply.
- [x] Tested with the anon key: real matches, case-insensitivity, too-short queries, wildcard and odd-character queries, and an expired notice not being returned.

Android app:

1. Search screen opened from a search icon on the Home top bar. The field waits 350 ms after typing stops, needs 2 or more characters, and ignores out-of-date responses.
2. Results in three sections (Notes, Events, Notices). Tapping a note opens the file viewer, and tapping an event or a notice opens its detail screen.
3. Loading, empty, and error states, with Retry and the offline hint.
4. Offline: search only the downloaded files (title, subject, file name) and say so in a caption.

Done when: a word from a note, an event, and a notice each finds the right item and opens it; special characters do not break it; and offline it searches downloads only.

## Phase 3B: Material types

Inside each subject, files are split into types: Notes, Syllabus, ST Exam, PUT Exam, External Exam, and Lab Manual.

Backend:

1. A migration adding `material_type` to `study_materials` (required, default `notes`, one of the six values), plus an index on `(subject_id, material_type)`. Existing rows become `notes`.
2. A new migration that replaces `search_all` so its result also carries the material type. The earlier search migration is not edited.
3. Tests: an unknown type is rejected, the default is `notes`, anon can read, and search returns the type.

Admin portal:

4. A Type dropdown in the notes form (required, default Notes), a Type column and a Type filter in the notes list. Existing notes can be edited to a different type.

Android app:

5. In the subject screen, filter chips: All plus the types that have files. Cards show the type label.
6. Type labels in Home's recent notes and in search results. The Downloads screen is unchanged, so the downloads database needs no migration.
7. Subject counts on the subject list still count all files.

Done when: a file uploaded as each of the six types appears under the right chip in the app, the old notes show as Notes, and search shows the type.

## Phase 3C: Year and department

Students choose their year and branch once. All notices are still shown, but the Notices list is filtered to their year and department by default, and the same choice will target push notifications in Phase 3D.

Backend:

1. A migration creating a `departments` table (code, name, sort order, active) with public read and admin write, seeded with RKGIT's real departments. Confirm the list with the admin before seeding.
2. In the same migration, add `target_years` and `target_departments` to `notices`. Empty means everyone. A trigger rejects a department code that is not in `departments`.
3. Tests: anon can read departments and cannot write; invalid years and unknown department codes are rejected; a notice with no audience stays valid.

Admin portal:

4. An Audience section in the notice form: Year (multi-select, default all) and Department (multi-select from the table, default all). The notices list shows an audience badge. Departments are edited in the Supabase dashboard for now.

Android app:

5. A first-launch setup screen to pick year and department, with a Skip option. The choice is stored on the phone only. The department list is cached for offline use.
6. A Settings screen (gear icon on Home) to change them and to see the app version.
7. The Notices list gets year and department filters that default to the student's choice, with an All option. A notice matches when its audience is empty or includes the student's year and department. The category chips stay.
8. Home's latest notices follow the same rule.

Done when: a notice for Year 1 CSE shows by default for a Year 1 CSE student and not for a Year 3 ME student, the All option shows it to both, a notice with no audience shows to everyone, and a skipped setup shows everything.

## Phase 3D: Notifications

Real push notifications through Firebase Cloud Messaging. The admin decides when to notify. Only notices and events send pushes. This phase needs Phase 3C.

Setup:

1. Create a Firebase project for development and register the Android app (`com.rkgit.one`). Download `google-services.json` for development. It is not committed to git.

Backend:

2. A migration adding `notified_at` to `notices` and `events`.
3. A Supabase Edge Function `send-notification`. It checks that the caller is an admin, loads the item, builds the message, and sends it through FCM to topics. It records `notified_at` first so one item cannot notify twice, and releases it if sending fails completely. The Firebase service-account key is stored as a Supabase secret, never in git or in any client.
4. Tests: a non-admin is rejected, an admin succeeds, a second call is refused as already notified, a targeted notice goes only to the matching topics, and a failed send releases `notified_at`.

Admin portal:

5. A "Notify students" toggle on the new notice and new event forms (on by default). After saving, the portal calls the function and shows the result. A "Send notification" action in the lists for items that were not notified. Editing an item never notifies again.

Android app:

6. Firebase Messaging dependency and a service for data-only messages. Two notification channels (Notices and Events).
7. Settings toggles for Notices and Events (both on by default). They subscribe and unsubscribe the topics, and the topics are updated when year or department change.
8. The Android 13+ permission is requested with a short explanation. If it is denied, Settings explains how to allow it.
9. Tapping a notification opens that notice or event, whether the app is in the foreground, in the background, or closed.

Done when: publishing a notice from the portal shows a notification on a phone within a minute; a Year 1 CSE notice reaches a phone set to Year 1 CSE and not a phone set to Year 3 ME; the toggles stop and restart pushes; tapping opens the right screen in all three app states; and sending the same item twice is refused.

## Phase 3E: App safety

Crash reports and an update prompt, so that problems in the field can be seen and fixed. It uses the Firebase project from Phase 3D.

Backend:

1. A migration creating a one-row `app_config` table (minimum version code, latest version code, update link, message) with public read and admin write. It is edited in the Supabase dashboard for now.

Android app:

2. Firebase Crashlytics in release builds only (disabled in debug). A forced test crash confirms reports arrive.
3. An update check at launch. Below the minimum version: a blocking "Update required" screen with the message and an Update button. Below the latest version: a dismissible banner. If the check fails or the phone is offline, nothing is blocked.
4. Note in the app and the store listing what Crashlytics collects. No personal data is collected.

Done when: a test crash shows in the Firebase console from a release build; raising the minimum version above the installed version shows the blocking screen; raising only the latest version shows a banner; and an offline launch never blocks.

## Phase 3F: UI design

The visual design of the whole app. Detailed steps are written when this phase starts, after the features above are finished, so screens are designed once.

Planned scope:

- A design system: colors, typography, shapes, spacing, and icons.
- A redesign of every Android screen, including the new Search, Settings, and first-launch screens.
- App icon and splash screen, dark theme, and consistent empty and error states.
- Accessibility: large text, contrast, and touch target sizes.
- Optional polish of the admin portal.

Done when: every screen follows the design system and has been reviewed on a small and a large phone.

## Phase 4: Test and harden

- Replace a file in the portal: the app shows Update available. Delete an item: the app keeps the copy and labels it.
- A notice past its expiry date is hidden in the app, and pinned notices come first.
- Replacing or deleting a notice attachment removes the old file from the bucket.
- Kill the app mid-download, fill the storage, and open a corrupt PDF and a password-protected PDF.
- Test on Android 13 or newer (notifications), on an older low-memory phone, and with a PDF of about 25 MB.
- Re-test RLS on all tables and buckets, and confirm no service-role key exists anywhere in the repo or the builds.
- Check the portal on a phone browser, tablet, and desktop.

Done when: every case above behaves as described.

### Carried over from Phase 3 (checks that could not be automated)

- Pinch to zoom and finger panning in the PDF viewer and the image viewer, including panning to the edges. Known behaviour: zooming a PDF keeps the horizontal focus but not the vertical position.
- A password-protected PDF, a corrupt PDF, a damaged image, a DOCX or PPTX note, a PDF of about 25 MB, an image of 8 to 10 MB, a PDF with many pages, and a PDF with mixed page sizes.
- Low free storage, and an item deleted in the portal while its card is on screen or while its download is queued (the "no longer available" message).
- The "Update available" label and Update button (replace a downloaded file in the portal), and the "No longer available online" label (delete it in the portal, and the saved copy must still open). Also replace and remove a notice attachment.
- The mobile-data confirmation (10 MB or more on a metered connection), the notification progress bar, the Android 13+ permission dialog, the permission-denied download path, killing the app mid-download, two downloads at once, and a restart of the phone.
- Richer data on Home and Notices: more than 3 notices (only 3 shown, pinned first), more than 5 notes, an upcoming event, a notice with a link, a PDF attachment, an image-only notice, a long title, and an expired notice hidden.
- The refresh-failure toast on a real screen, a slow or flaky connection, a server error showing "Something went wrong" without the offline hint, and a captive-portal Wi-Fi.
- Confirm that a notice attachment's viewer title and its Downloads card show the notice's own title. Review how offline errors are detected (`isOfflineError`) so it does not rely on message text. Small wording fix: the Clear all dialog says "1 files".
- Phones with strict battery savers (for example Oppo) and background downloads.

### Checks for Phases 3A to 3F

- Search: special characters, very long text, fast typing, results for each kind opening the right screen, and the offline downloads-only fallback.
- Material types: every type shows under the right chip; old notes show as Notes; filtering and counts are right.
- Year and department: relevance filtering for several student settings, a skipped setup, a changed setting, and a department that is later made inactive.
- Notifications: permission granted and denied, foreground, background, and closed app, targeted and untargeted notices, events, toggles, a second send refused, and delivery on a phone with battery saving on.
- App safety: a forced crash arrives in Crashlytics, the blocking and banner prompts, and an offline launch never blocks.
- The new screens in the UI design (3F) on small and large phones, dark theme, and large text.

## Phase 5: Release

1. Create the production Supabase project, run the same migrations, and create the production buckets, policies, and admin user.
2. Point the portal and the app at production using separate environment values, and redeploy.
3. Build a signed release APK or AAB and test that release build with shrinking on, not only the debug build.
4. Publish (APK or Play Store), export a backup of schema and data, and keep the free-tier project active or upgrade.

Done when: a fresh install of the release build reads production data created from the production portal.

### Added by Phases 3A to 3F

- Run all the new migrations on production (search, material types, departments and notice audience, `notified_at`, `app_config`) and seed the real departments and the `app_config` row.
- Create a production Firebase project and use its `google-services.json` for release builds only. Deploy the `send-notification` function to production and set its secrets there. Send a test notification from the production portal to a release build.
- Confirm Crashlytics is on in the release build and a test crash arrives.
- Fill in the store listing's data safety section (Crashlytics and push notifications) and link a privacy statement.

### Debugging after release

- During the first weeks, check the Crashlytics dashboard, the Supabase logs, and the Edge Function logs every day.
- Keep `min_version_code` low until a fix is published, and raise it only after most students have updated.
- Fix each reported problem on the development project first, then ship a new version.
