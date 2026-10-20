create or replace function public.search_all(q text, max_per_kind int default 20)
returns table (
  kind text,
  id uuid,
  title text,
  subtitle text,
  snippet text,
  rank int,
  sort_date timestamptz,
  extra jsonb
)
language sql
stable
security invoker
set search_path = public
as $$
  with p as (
    select
      trim(coalesce(q, '')) as term,
      '%' || replace(replace(replace(trim(coalesce(q, '')), '\', '\\'), '%', '\%'), '_', '\_') || '%' as pat
  )
  (
    select
      'material'::text,
      m.id,
      m.title,
      s.name,
      left(coalesce(m.description, ''), 120),
      case when m.title ilike p.pat then 1 when s.name ilike p.pat then 2 else 3 end,
      m.created_at,
      jsonb_build_object(
        'subject_id', s.id, 'subject_name', s.name, 'year', s.year, 'semester', s.semester,
        'file_path', m.file_path, 'file_name', m.file_name,
        'file_type', m.file_type, 'file_size', m.file_size,
        'material_type', m.material_type
      )
    from public.study_materials m
    join public.subjects s on s.id = m.subject_id
    cross join p
    where length(p.term) >= 2
      and (m.title ilike p.pat
        or coalesce(m.description, '') ilike p.pat
        or s.name ilike p.pat
        or m.file_name ilike p.pat)
    order by 6, 7 desc
    limit greatest(max_per_kind, 1)
  )
  union all
  (
    select
      'event'::text,
      e.id,
      e.title,
      e.venue,
      left(coalesce(e.description, ''), 120),
      case when e.title ilike p.pat then 1 else 2 end,
      e.event_date,
      jsonb_build_object('event_date', e.event_date, 'venue', e.venue)
    from public.events e
    cross join p
    where length(p.term) >= 2
      and (e.title ilike p.pat
        or coalesce(e.venue, '') ilike p.pat
        or coalesce(e.description, '') ilike p.pat)
    order by 6, 7 desc
    limit greatest(max_per_kind, 1)
  )
  union all
  (
    select
      'notice'::text,
      n.id,
      n.title,
      n.category,
      left(coalesce(n.body, ''), 120),
      case when n.title ilike p.pat then 1 else 2 end,
      n.published_at,
      jsonb_build_object(
        'category', n.category, 'pinned', n.pinned,
        'has_attachment', n.file_path is not null
      )
    from public.notices n
    cross join p
    where length(p.term) >= 2
      and (n.expires_at is null or n.expires_at > now())
      and (n.title ilike p.pat or coalesce(n.body, '') ilike p.pat)
    order by 6, 7 desc
    limit greatest(max_per_kind, 1)
  )
$$;