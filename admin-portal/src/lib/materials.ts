import { supabase } from "@/lib/supabase"

export const MATERIAL_TYPES = [
  { value: "notes", label: "Notes" },
  { value: "syllabus", label: "Syllabus" },
  { value: "st_exam", label: "ST Exam" },
  { value: "put_exam", label: "PUT Exam" },
  { value: "external_exam", label: "External Exam" },
  { value: "lab_manual", label: "Lab Manual" },
] as const

export type MaterialType = (typeof MATERIAL_TYPES)[number]["value"]
export const DEFAULT_MATERIAL_TYPE: MaterialType = "notes"

export function materialTypeLabel(value: string): string {
  const match = MATERIAL_TYPES.find((t) => t.value === value)
  return match ? match.label : value
}

export function isMaterialType(value: unknown): value is MaterialType {
  return (
    typeof value === "string" &&
    MATERIAL_TYPES.some((t) => t.value === value)
  )
}

export type Subject = {
  id: string
  year: number
  semester: number
  name: string
}

export type Material = {
  id: string
  subject_id: string
  title: string
  description: string | null
  file_path: string
  file_name: string
  file_type: string
  file_size: number
  material_type: MaterialType
  created_at: string
  updated_at: string
  subjects: Subject
}

export type MaterialInput = {
  title: string
  description: string
  materialType: MaterialType
  year: number
  semester: number
  subjectName: string
}

export const BUCKET = "study-materials"
export const MAX_FILE_SIZE = 25 * 1024 * 1024

export const ALLOWED_TYPES: Record<string, string> = {
  pdf: "application/pdf",
  docx: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  pptx: "application/vnd.openxmlformats-officedocument.presentationml.presentation",
}

const SELECT = "*, subjects(id, year, semester, name)"

function extOf(name: string) {
  return name.split(".").pop()?.toLowerCase() ?? ""
}

export function validateFile(file: File): string | null {
  if (!ALLOWED_TYPES[extOf(file.name)]) {
    return "Only PDF, DOCX, or PPTX files are allowed."
  }

  if (file.size === 0) {
    return "The file is empty."
  }

  if (file.size > MAX_FILE_SIZE) {
    return "The file must be 25 MB or smaller."
  }

  return null
}

function sanitizeFileName(name: string) {
  const ext = extOf(name)
  const base = name.slice(0, name.lastIndexOf("."))
  const clean =
    base
      .normalize("NFKD")
      .replace(/[^\w-]+/g, "-")
      .replace(/-+/g, "-")
      .replace(/^-|-$/g, "")
      .slice(0, 80) || "file"

  return `${clean}.${ext}`
}

export const semestersForYear = (year: number) =>
  year >= 1 && year <= 4 ? [year * 2 - 1, year * 2] : []

export const publicFileUrl = (path: string) =>
  supabase.storage.from(BUCKET).getPublicUrl(path).data.publicUrl

export function formatBytes(n: number) {
  return n < 1024 * 1024
    ? `${Math.max(1, Math.round(n / 1024))} KB`
    : `${(n / 1024 / 1024).toFixed(1)} MB`
}

export const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  })

export function errorMessage(e: unknown) {
  if (typeof e === "object" && e && "message" in e) {
    return String((e as { message: unknown }).message)
  }

  return "Something went wrong."
}

// Reads

export async function fetchSubjects(): Promise<Subject[]> {
  const { data, error } = await supabase
    .from("subjects")
    .select("id, year, semester, name")
    .order("name")

  if (error) throw error

  return data as Subject[]
}

export async function fetchMaterials(): Promise<Material[]> {
  const { data, error } = await supabase
    .from("study_materials")
    .select(SELECT)
    .order("created_at", { ascending: false })

  if (error) throw error

  return data as unknown as Material[]
}

export async function fetchMaterial(id: string): Promise<Material> {
  const { data, error } = await supabase
    .from("study_materials")
    .select(SELECT)
    .eq("id", id)
    .single()

  if (error) throw error

  return data as unknown as Material
}

// Subject lookup: find existing subject or create it.

async function getOrCreateSubject(
  year: number,
  semester: number,
  rawName: string,
): Promise<string> {
  const name = rawName.trim().replace(/\s+/g, " ")
  const pattern = name.replace(/[\\%_]/g, "\\$&")

  const find = async () => {
    const { data, error } = await supabase
      .from("subjects")
      .select("id")
      .eq("year", year)
      .eq("semester", semester)
      .ilike("name", pattern)
      .maybeSingle()

    if (error) throw error

    return data?.id ?? null
  }

  const existing = await find()

  if (existing) return existing

  const { data, error } = await supabase
    .from("subjects")
    .insert({ year, semester, name })
    .select("id")
    .single()

  if (!error) return data.id

  if (error.code === "23505") {
    const again = await find()

    if (again) return again
  }

  throw error
}

// File upload

async function uploadFile(file: File) {
  const contentType = ALLOWED_TYPES[extOf(file.name)]
  const path = `materials/${crypto.randomUUID()}/${sanitizeFileName(file.name)}`

  const { error } = await supabase.storage
    .from(BUCKET)
    .upload(path, file, {
      contentType,
      upsert: false,
    })

  if (error) throw error

  return { path, contentType }
}

const removeFile = (path: string) =>
  supabase.storage.from(BUCKET).remove([path])

// Create material

export async function createMaterial(input: MaterialInput, file: File) {
  const subjectId = await getOrCreateSubject(
    input.year,
    input.semester,
    input.subjectName,
  )

  const { path, contentType } = await uploadFile(file)

  const { error } = await supabase.from("study_materials").insert({
    subject_id: subjectId,
    title: input.title.trim(),
    description: input.description.trim() || null,
    material_type: input.materialType,
    file_path: path,
    file_name: file.name,
    file_type: contentType,
    file_size: file.size,
  })

  if (error) {
    await removeFile(path)
    throw error
  }
}

// Update material

export async function updateMaterial(
  existing: Material,
  input: MaterialInput,
  file?: File,
) {
  const subjectId = await getOrCreateSubject(
    input.year,
    input.semester,
    input.subjectName,
  )

  const patch: Record<string, unknown> = {
    subject_id: subjectId,
    title: input.title.trim(),
    description: input.description.trim() || null,
    material_type: input.materialType,
  }

  let newPath: string | null = null

  if (file) {
    const up = await uploadFile(file)

    newPath = up.path

    Object.assign(patch, {
      file_path: up.path,
      file_name: file.name,
      file_type: up.contentType,
      file_size: file.size,
    })
  }

  const { data, error } = await supabase
    .from("study_materials")
    .update(patch)
    .eq("id", existing.id)
    .select("id")

  if (error || !data || data.length === 0) {
    if (newPath) await removeFile(newPath)

    throw (
      error ??
      new Error("Material was not updated. Check your admin permissions.")
    )
  }

  if (file) {
    const { data: removed, error: rmErr } = await removeFile(
      existing.file_path,
    )

    if (rmErr || !removed?.length) {
      console.warn("Old file cleanup failed", rmErr?.message)
    }
  }
}

// Delete material

export async function deleteMaterial(m: Material) {
  const { data, error } = await supabase
    .from("study_materials")
    .delete()
    .eq("id", m.id)
    .select("id")

  if (error) throw error

  if (!data || data.length === 0) {
    throw new Error(
      "Material was not deleted. Check your admin permissions.",
    )
  }

  const { data: removed, error: rmErr } = await removeFile(m.file_path)

  if (rmErr || !removed?.length) {
    console.warn("File cleanup failed", rmErr?.message)
  }
}