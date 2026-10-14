alter table public.subjects        enable row level security;
alter table public.study_materials enable row level security;
alter table public.events          enable row level security;
alter table public.admins          enable row level security;

-- Everyone (the app) can read content
create policy "public read" on public.subjects        for select using (true);
create policy "public read" on public.study_materials for select using (true);
create policy "public read" on public.events          for select using (true);

-- Only admins can write
create policy "admin write" on public.subjects        for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write" on public.study_materials for all using (public.is_admin()) with check (public.is_admin());
create policy "admin write" on public.events          for all using (public.is_admin()) with check (public.is_admin());

-- A signed-in user can read only their own admin row; nobody edits admins via the API
create policy "self read" on public.admins for select using (user_id = auth.uid());