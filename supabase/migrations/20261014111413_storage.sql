insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types) values
  ('study-materials', 'study-materials', true, 26214400, array[
     'application/pdf',
     'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
     'application/vnd.openxmlformats-officedocument.presentationml.presentation'
   ]),
  ('event-posters', 'event-posters', true, 5242880, array[
     'image/jpeg', 'image/png', 'image/webp'
   ])
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

-- Public buckets serve files by URL, so no public policy is needed.
-- Admins get full control (select is included so listing and deleting work reliably).
create policy "admin read" on storage.objects for select
  using (bucket_id in ('study-materials','event-posters') and public.is_admin());
create policy "admin upload" on storage.objects for insert
  with check (bucket_id in ('study-materials','event-posters') and public.is_admin());
create policy "admin update" on storage.objects for update
  using (bucket_id in ('study-materials','event-posters') and public.is_admin());
create policy "admin delete" on storage.objects for delete
  using (bucket_id in ('study-materials','event-posters') and public.is_admin());