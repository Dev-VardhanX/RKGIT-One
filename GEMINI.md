# RKGIT ONE — Project Instructions

## Before Every Task
Read these project documents before making changes:
- docs/design.md
- docs/build-order.md
- docs/decisions.md

If any document is missing, tell me instead of guessing its contents.

## Development Order
Follow the build order exactly:
1. Phase 0: Setup
2. Phase 1: Backend
3. Phase 2: Admin portal
4. Phase 3: Android app
5. Phase 4: Testing and hardening
6. Phase 5: Release

Work only on the current phase. Do not start the next phase until the current phase passes its completion checks.

## Fixed Technology Stack

### Android
- Kotlin
- Android Studio
- Jetpack Compose
- MVVM architecture

### Admin Portal
- React
- Vite
- TypeScript
- Tailwind CSS
- shadcn/ui

### Backend
- Supabase
- PostgreSQL
- Supabase Storage
- Supabase Auth
- Row Level Security (RLS)

Do not change the agreed technology stack without my approval.

## V1 Scope
- Browse study materials by year, semester, and subject.
- Open PDFs and download them for offline reading.
- Browse recent and upcoming college events.
- View event details and registration links.
- Provide an administrator login.
- Allow administrators to manage study materials and events.
- Provide a polished, responsive admin portal.

Do not introduce features outside the documented v1 scope.

## Security
- Never request or use the Supabase service-role key.
- Use only public client keys in the Android app and admin portal.
- Never commit passwords, access tokens, or other secrets.
- Keep environment files out of Git.
- Maintain appropriate .env.example files.
- Enforce access through database RLS and storage policies.
- Public access is intended for published study materials and events.
- Only authorized administrators may modify published content.

## Database Changes
- Store database migrations in supabase/migrations/.
- Use SQL migration files for schema and policy changes.
- Show me each migration before applying it.
- Explain security implications before changing policies.
- Never run development changes against production.
- Ask for approval before destructive operations.

## Working Style
- Explain commands before asking me to run them.
- Assume I am a beginner in web development.
- Give PowerShell-compatible commands for Windows.
- Make small, verifiable changes.
- Check the result of each step before proceeding.
- Do not claim a task succeeded without verifying it.
- Ask before making decisions that change the agreed design.