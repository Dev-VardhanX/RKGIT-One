alter table public.notices add column notified_at timestamptz;
alter table public.events  add column notified_at timestamptz;