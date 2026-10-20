alter table public.study_materials
  add column material_type text not null default 'notes'
  check (material_type in ('notes','syllabus','st_exam','put_exam','external_exam','lab_manual'));

create index on public.study_materials (subject_id, material_type);