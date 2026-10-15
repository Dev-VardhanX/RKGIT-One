create table public.notices (
  id           uuid primary key default gen_random_uuid(),
  title        text not null check (length(trim(title)) > 0),
  body         text,
  category     text not null default 'general'
               check (category in ('general','academic','exam','datesheet','holiday')),
  pinned       boolean not null default false,
  link_url     text check (link_url ~* '^https?://'),
  file_path    text,
  file_name    text,
  file_type    text,
  file_size    bigint check (file_size > 0),
  published_at timestamptz not null default now(),
  expires_at   timestamptz,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  -- attachment fields are all set or all empty
  check (
    (file_path is null) = (file_name is null)
    and (file_path is null) = (file_type is null)
    and (file_path is null) = (file_size is null)
  ),
  -- a notice needs a message or an attachment
  check (nullif(trim(coalesce(body, '')), '') is not null or file_path is not null),
  check (expires_at is null or expires_at > published_at)
);

create index on public.notices (pinned desc, published_at desc);
create index on public.notices (expires_at);

create trigger t_notices_touch before update on public.notices
  for each row execute function public.touch_updated_at();

alter table public.notices enable row level security;
create policy "public read" on public.notices for select using (true);
create policy "admin write" on public.notices for all
  using (public.is_admin()) with check (public.is_admin());

-- Attachment bucket: 10 MB, PDF and images
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values
  ('notice-files', 'notice-files', true, 10485760, array[
     'application/pdf', 'image/jpeg', 'image/png', 'image/webp'
   ])
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create policy "admin read notice files" on storage.objects for select
  using (bucket_id = 'notice-files' and public.is_admin());
create policy "admin upload notice files" on storage.objects for insert
  with check (bucket_id = 'notice-files' and public.is_admin());
create policy "admin update notice files" on storage.objects for update
  using (bucket_id = 'notice-files' and public.is_admin());
create policy "admin delete notice files" on storage.objects for delete
  using (bucket_id = 'notice-files' and public.is_admin());