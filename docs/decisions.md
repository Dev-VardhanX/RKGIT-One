\# RKGIT ONE — Project Decisions



\## Access

\- Public read access for study materials and events.

\- No student login in v1.

\- Students can browse, open, and download published materials without signing in.

\- Anyone with a published file URL can access that file.

\- Revisit student authentication only if materials must be restricted.



\## Android Application

\- Kotlin

\- Android Studio

\- Jetpack Compose

\- MVVM architecture



\## Admin Portal

\- React

\- Vite

\- TypeScript

\- Tailwind CSS

\- shadcn/ui



\## Backend

\- Supabase

\- PostgreSQL

\- Supabase Storage

\- Supabase Auth for administrators

\- Row Level Security (RLS)

\- No custom backend server



\## Environments

\- Separate Supabase development and production projects.

\- Database changes must be stored in SQL migration files.

\- Development changes must not be applied to production.



\## Security

\- Never commit secrets.

\- Use only public client keys in the Android app and admin portal.

\- Never include the service-role key in client applications or the repository.

\- Keep local environment files out of Git.

\- Maintain appropriate .env.example files.

