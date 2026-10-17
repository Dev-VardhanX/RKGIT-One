# RKGIT ONE — Project Instructions

## Before Every Task
Read these project documents before making changes:
- docs/design.md
- docs/build-order.md
- docs/decisions.md


## Rules:
- Follow the build order. Work only on the current phase.
- Stack is fixed: Kotlin, Compose, MVVM (Android); React, Vite, TypeScript, Tailwind, shadcn/ui (portal); Supabase (backend). Do not change it.
- Never add features outside the v1 scope in docs/design.md.
- Never use or request the Supabase service_role key. Clients use the anon key only.
- Never commit secrets. Use .env files and keep .env.example updated.
- Database changes go in supabase/migrations as SQL files. Never edit the dashboard schema by hand.
- Show me every migration before it is applied, and never run anything against production.