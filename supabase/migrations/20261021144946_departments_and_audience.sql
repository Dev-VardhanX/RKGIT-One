-- Departments
create table public.departments (
  id         uuid primary key default gen_random_uuid(),
  code       text not null unique check (code ~ '^[a-z0-9-]{2,20}$'),
  name       text not null check (length(trim(name)) > 0),
  sort_order int  not null default 0,
  active     boolean not null default true
);

alter table public.departments enable row level security;
create policy "public read" on public.departments for select using (true);
create policy "admin write" on public.departments for all
  using (public.is_admin()) with check (public.is_admin());

-- A department's code is used in notices and notification topics, so it never changes
create function public.departments_code_immutable() returns trigger
language plpgsql as $$
begin
  if new.code is distinct from old.code then
    raise exception 'A department code cannot be changed';
  end if;
  return new;
end $$;

create trigger t_departments_code_immutable
  before update on public.departments
  for each row execute function public.departments_code_immutable();

-- Draft list: confirm with the admin, then edit in the Supabase dashboard
insert into public.departments (code, name, sort_order, active) values
  ('cse',      'Computer Science and Engineering',                    10, true),
  ('cse-aiml', 'CSE (Artificial Intelligence and Machine Learning)',  20, true),
  ('cse-ds',   'CSE (Data Science)',                                  30, true),
  ('cse-iot',  'CSE (Internet of Things)',                            40, true),
  ('it',       'Information Technology',                              50, true),
  ('ece',      'Electronics and Communication Engineering',           60, true),
  ('me',       'Mechanical Engineering',                              70, true),
  ('cs',       'Computer Science',                                    80, false),
  ('ce',       'Civil Engineering',                                   90, false),
  ('ee',       'Electrical and Electronics Engineering',             100, false)
on conflict (code) do nothing;

-- Notice audience: null or empty means everyone
alter table public.notices
  add column target_years smallint[],
  add column target_departments text[],
  add constraint notices_target_years_valid
    check (target_years is null or target_years <@ array[1,2,3,4]::smallint[]);

create function public.validate_notice_targets() returns trigger
language plpgsql
set search_path = public
as $$
begin
  if new.target_departments is not null and cardinality(new.target_departments) > 0 then
    if exists (
      select 1
      from unnest(new.target_departments) as d(code)
      where not exists (select 1 from public.departments x where x.code = d.code)
    ) then
      raise exception 'Unknown department code in target_departments';
    end if;
  end if;
  return new;
end $$;

create trigger t_notices_validate_targets
  before insert or update of target_departments on public.notices
  for each row execute function public.validate_notice_targets();