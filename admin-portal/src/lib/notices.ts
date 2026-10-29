import { supabase } from "@/lib/supabase"
import type { Department } from "@/lib/departments"

export type NoticeCategory = "general" | "academic" | "exam" | "datesheet" | "holiday"

export const CATEGORIES: { value: NoticeCategory; label: string }[] = [
  { value: "general", label: "General" },
  { value: "academic", label: "Academic" },
  { value: "exam", label: "Exam" },
  { value: "datesheet", label: "Datesheet" },
  { value: "holiday", label: "Holiday" },
]
export const categoryLabel = (c: string) => CATEGORIES.find((x) => x.value === c)?.label ?? c

export function audienceLabel(
  years: number[] | null,
  departmentCodes: string[] | null,
  departments: Department[] = [],
): string {
  const hasYears = Boolean(years && years.length > 0)
  const hasDepts = Boolean(departmentCodes && departmentCodes.length > 0)

  if (!hasYears && !hasDepts) {
    return "Everyone"
  }

  let yearsPart: string
  if (hasYears && years) {
    const sortedYears = Array.from(new Set(years)).sort((a, b) => a - b)
    yearsPart = sortedYears.length === 1 ? `Year ${sortedYears[0]}` : `Year ${sortedYears.join(", ")}`
  } else {
    yearsPart = "All years"
  }

  let deptsPart: string
  if (hasDepts && departmentCodes) {
    const uniqueCodes = Array.from(new Set(departmentCodes))
    if (departments && departments.length > 0) {
      const deptMap = new Map(departments.map((d) => [d.code.toLowerCase(), d.sort_order]))
      uniqueCodes.sort((a, b) => {
        const orderA = deptMap.get(a.toLowerCase()) ?? 9999
        const orderB = deptMap.get(b.toLowerCase()) ?? 9999
        if (orderA !== orderB) return orderA - orderB
        return a.localeCompare(b)
      })
    }
    deptsPart = uniqueCodes.map((c) => c.toUpperCase()).join(", ")
  } else {
    deptsPart = "All departments"
  }

  return `${yearsPart} · ${deptsPart}`
}

export type Notice = {
  id: string
  title: string
  body: string | null
  category: NoticeCategory
  pinned: boolean
  link_url: string | null
  file_path: string | null
  file_name: string | null
  file_type: string | null
  file_size: number | null
  target_years: number[] | null
  target_departments: string[] | null
  published_at: string
  expires_at: string | null
  notified_at: string | null
  created_at: string
  updated_at: string
}

export type NoticeInput = {
  title: string
  body: string
  category: NoticeCategory
  pinned: boolean
  linkUrl: string
  expiresAt: string | null // ISO string or null
  targetYears: number[]
  targetDepartments: string[]
}

export type AttachmentChange =
  | { kind: "keep" }
  | { kind: "replace"; file: File }
  | { kind: "remove" }

export const NOTICE_BUCKET = "notice-files"
export const MAX_NOTICE_FILE_SIZE = 10 * 1024 * 1024 // must match the bucket limit
export const NOTICE_FILE_TYPES: Record<string, string> = {
  pdf: "application/pdf",
  jpg: "image/jpeg",
  jpeg: "image/jpeg",
  png: "image/png",
  webp: "image/webp",
}

// ---------- helpers ----------
function extOf(name: string) {
  return name.split(".").pop()?.toLowerCase() ?? ""
}

export function validateNoticeFile(file: File): string | null {
  if (!NOTICE_FILE_TYPES[extOf(file.name)]) return "Only PDF, JPG, PNG, or WebP files are allowed."
  if (file.size === 0) return "The file is empty."
  if (file.size > MAX_NOTICE_FILE_SIZE) return "The file must be 10 MB or smaller."
  return null
}

function sanitizeFileName(name: string) {
  const ext = extOf(name)
  const base = name.slice(0, name.lastIndexOf("."))
  const clean =
    base.normalize("NFKD").replace(/[^\w-]+/g, "-").replace(/-+/g, "-").replace(/^-|-$/g, "").slice(0, 80) || "file"
  return `${clean}.${ext}`
}

export const publicNoticeFileUrl = (path: string) =>
  supabase.storage.from(NOTICE_BUCKET).getPublicUrl(path).data.publicUrl

export const isExpired = (n: Pick<Notice, "expires_at">) =>
  !!n.expires_at && new Date(n.expires_at).getTime() <= Date.now()

// ---------- reads ----------
export async function fetchNotices(): Promise<Notice[]> {
  const { data, error } = await supabase
    .from("notices")
    .select("*")
    .order("pinned", { ascending: false })
    .order("published_at", { ascending: false })
  if (error) throw error
  return data as Notice[]
}

export async function fetchNotice(id: string): Promise<Notice> {
  const { data, error } = await supabase.from("notices").select("*").eq("id", id).single()
  if (error) throw error
  return data as Notice
}

export async function fetchNoticeSummary() {
  const [count, latest] = await Promise.all([
    supabase.from("notices").select("id", { count: "exact", head: true }),
    supabase.from("notices").select("id, title, category, published_at").order("published_at", { ascending: false }).limit(5),
  ])
  if (count.error) throw count.error
  if (latest.error) throw latest.error
  return {
    total: count.count ?? 0,
    latest: latest.data as Pick<Notice, "id" | "title" | "category" | "published_at">[],
  }
}

// ---------- file upload ----------
async function uploadNoticeFile(file: File) {
  const contentType = NOTICE_FILE_TYPES[extOf(file.name)]
  const path = `notices/${crypto.randomUUID()}/${sanitizeFileName(file.name)}`
  const { error } = await supabase.storage.from(NOTICE_BUCKET).upload(path, file, { contentType, upsert: false })
  if (error) throw error
  return { path, contentType }
}

const removeNoticeFile = (path: string) => supabase.storage.from(NOTICE_BUCKET).remove([path])

async function cleanupOldFile(path: string | null) {
  if (!path) return
  const { data, error } = await removeNoticeFile(path)
  if (error || !data?.length) console.warn("Notice file cleanup failed", error?.message)
}

// ---------- writes ----------
export async function createNotice(input: NoticeInput, file?: File): Promise<{ id: string }> {
  const up = file ? await uploadNoticeFile(file) : null

  const targetYears =
    input.targetYears.length > 0
      ? Array.from(new Set(input.targetYears)).sort((a, b) => a - b)
      : null
  const targetDepartments =
    input.targetDepartments.length > 0
      ? Array.from(new Set(input.targetDepartments))
      : null

  const { data, error } = await supabase
    .from("notices")
    .insert({
      title: input.title.trim(),
      body: input.body.trim() || null,
      category: input.category,
      pinned: input.pinned,
      link_url: input.linkUrl.trim() || null,
      expires_at: input.expiresAt,
      target_years: targetYears,
      target_departments: targetDepartments,
      ...(up && file
        ? { file_path: up.path, file_name: file.name, file_type: up.contentType, file_size: file.size }
        : {}),
    })
    .select("id")
    .single()

  if (error || !data) {
    if (up) await removeNoticeFile(up.path) // no orphaned file if the row fails
    throw error ?? new Error("Notice was not created.")
  }

  return { id: data.id as string }
}

export async function updateNotice(existing: Notice, input: NoticeInput, change: AttachmentChange) {
  const targetYears =
    input.targetYears.length > 0
      ? Array.from(new Set(input.targetYears)).sort((a, b) => a - b)
      : null
  const targetDepartments =
    input.targetDepartments.length > 0
      ? Array.from(new Set(input.targetDepartments))
      : null

  const patch: Record<string, unknown> = {
    title: input.title.trim(),
    body: input.body.trim() || null,
    category: input.category,
    pinned: input.pinned,
    link_url: input.linkUrl.trim() || null,
    expires_at: input.expiresAt,
    target_years: targetYears,
    target_departments: targetDepartments,
  }

  let uploadedPath: string | null = null
  if (change.kind === "replace") {
    const up = await uploadNoticeFile(change.file) // new UUID path, never reuses the old one
    uploadedPath = up.path
    Object.assign(patch, {
      file_path: up.path,
      file_name: change.file.name,
      file_type: up.contentType,
      file_size: change.file.size,
    })
  } else if (change.kind === "remove") {
    Object.assign(patch, { file_path: null, file_name: null, file_type: null, file_size: null })
  }

  const { data, error } = await supabase.from("notices").update(patch).eq("id", existing.id).select("id")
  if (error || !data || data.length === 0) {
    if (uploadedPath) await removeNoticeFile(uploadedPath)
    throw error ?? new Error("Notice was not updated. Check your admin permissions.")
  }

  // only now, after the row no longer points to the old file, remove it
  if (change.kind !== "keep") await cleanupOldFile(existing.file_path)
}

export async function deleteNotice(n: Notice) {
  const { data, error } = await supabase.from("notices").delete().eq("id", n.id).select("id")
  if (error) throw error
  if (!data || data.length === 0) throw new Error("Notice was not deleted. Check your admin permissions.")
  await cleanupOldFile(n.file_path)
}
