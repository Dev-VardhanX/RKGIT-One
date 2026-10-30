create table public.app_config (
  id                  int primary key default 1 check (id = 1),
  min_version_code    int not null default 1 check (min_version_code >= 1),
  latest_version_code int not null default 1 check (latest_version_code >= 1),
  update_url          text check (update_url is null or update_url ~* '^https?://'),
  message             text,
  updated_at          timestamptz not null default now(),
  check (latest_version_code >= min_version_code)
);

insert into public.app_config (id) values (1);

alter table public.app_config enable row level security;
create policy "public read" on public.app_config for select using (true);
create policy "admin write" on public.app_config for all
  using (public.is_admin()) with check (public.is_admin());

create trigger t_app_config_touch before update on public.app_config
  for each row execute function public.touch_updated_at();