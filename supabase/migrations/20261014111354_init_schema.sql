-- Admin allow-list
create table public.admins (
  user_id uuid primary key references auth.users(id) on delete cascade
);

create function public.is_admin() returns boolean
language sql security definer set search_path = public stable as $$
  select exists (select 1 from public.admins where user_id = auth.uid());
$$;

-- Subjects
create table public.subjects (
  id         uuid primary key default gen_random_uuid(),
  year       smallint not null check (year between 1 and 4),
  semester   smallint not null check (semester between 1 and 8),
  name       text not null check (length(trim(name)) > 0),
  created_at timestamptz not null default now(),
  check (semester between year * 2 - 1 and year * 2)
);
create unique index subjects_unique on public.subjects (year, semester, lower(name));

-- Study materials
create table public.study_materials (
  id          uuid primary key default gen_random_uuid(),
  subject_id  uuid not null references public.subjects(id) on delete restrict,
  title       text not null check (length(trim(title)) > 0),
  description text,
  file_path   text not null,
  file_name   text not null,
  file_type   text not null,
  file_size   bigint not null check (file_size > 0),
  created_at  timestamptz not null default now(),
  updated_at  timestamptz not null default now()
);
create index on public.study_materials (subject_id);
create index on public.study_materials (created_at desc);

-- Events
create table public.events (
  id               uuid primary key default gen_random_uuid(),
  title            text not null check (length(trim(title)) > 0),
  description      text,
  event_date       timestamptz not null,
  venue            text,
  poster_path      text,
  registration_url text check (registration_url ~* '^https?://'),
  created_at       timestamptz not null default now(),
  updated_at       timestamptz not null default now()
);
create index on public.events (event_date);

-- updated_at trigger
create function public.touch_updated_at() returns trigger
language plpgsql set search_path = public as $$
begin new.updated_at = now(); return new; end $$;

create trigger t_materials_touch before update on public.study_materials
  for each row execute function public.touch_updated_at();
create trigger t_events_touch before update on public.events
  for each row execute function public.touch_updated_at();