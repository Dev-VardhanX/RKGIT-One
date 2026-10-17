import { useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { toast } from "sonner"
import { useCreateNotice, useNotice, useUpdateNotice } from "@/hooks/useNotices"
import {
  CATEGORIES, publicNoticeFileUrl, validateNoticeFile,
  type AttachmentChange, type Notice,
} from "@/lib/notices"
import { errorMessage } from "@/lib/materials"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Switch } from "@/components/ui/switch"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"

const schema = z.object({
  title: z.string().trim().min(1, "Title is required").max(150, "Keep the title under 150 characters"),
  body: z.string().trim().max(5000, "Keep the message under 5000 characters"),
  category: z.enum(["general", "academic", "exam", "datesheet", "holiday"]),
  pinned: z.boolean(),
  linkUrl: z
    .string()
    .trim()
    .max(500, "The link is too long")
    .refine((v) => v === "" || /^https?:\/\/\S+$/i.test(v), "Link must start with http:// or https://"),
  expiresOn: z.string(),
})
type FormValues = z.infer<typeof schema>

function toDateInput(iso: string | null) {
  if (!iso) return ""
  const d = new Date(iso)
  const p = (n: number) => String(n).padStart(2, "0")
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

function NoticeForm({ initial }: { initial?: Notice }) {
  const navigate = useNavigate()
  const create = useCreateNotice()
  const update = useUpdateNotice()
  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(null)
  const [removeExisting, setRemoveExisting] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const { register, handleSubmit, control, formState: { errors } } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: initial?.title ?? "",
      body: initial?.body ?? "",
      category: initial?.category ?? "general",
      pinned: initial?.pinned ?? false,
      linkUrl: initial?.link_url ?? "",
      expiresOn: toDateInput(initial?.expires_at ?? null),
    },
  })

  const pending = create.isPending || update.isPending
  const hasExisting = !!initial?.file_path

  async function onSubmit(v: FormValues) {
    setFormError(null)

    const hasAttachmentAfter = file ? true : hasExisting ? !removeExisting : false
    if (!v.body && !hasAttachmentAfter) return setFormError("Add a message or an attachment.")

    if (file) {
      const msg = validateNoticeFile(file)
      if (msg) return setFileError(msg)
    }

    let expiresAt: string | null = null
    if (v.expiresOn) {
      const unchanged = !!initial?.expires_at && v.expiresOn === toDateInput(initial.expires_at)
      expiresAt = unchanged ? initial!.expires_at : new Date(`${v.expiresOn}T23:59:59`).toISOString()
      const publishedAt = initial ? new Date(initial.published_at) : new Date()
      if (new Date(expiresAt!) <= publishedAt) return setFormError("The expiry date must be after the publish date.")
    }

    const input = {
      title: v.title,
      body: v.body,
      category: v.category,
      pinned: v.pinned,
      linkUrl: v.linkUrl,
      expiresAt,
    }

    try {
      if (initial) {
        const change: AttachmentChange = file
          ? { kind: "replace", file }
          : removeExisting
            ? { kind: "remove" }
            : { kind: "keep" }
        await update.mutateAsync({ existing: initial, input, change })
      } else {
        await create.mutateAsync({ input, file: file ?? undefined })
      }
      toast.success(initial ? "Notice updated" : "Notice added")
      navigate("/notices")
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">{initial ? "Edit notice" : "Add notice"}</h1>
        <p className="text-sm text-muted-foreground">A notice needs a message, an attachment, or both.</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-5 rounded-lg border bg-background p-6" noValidate>
        <div className="space-y-2">
          <Label htmlFor="title">Title</Label>
          <Input id="title" {...register("title")} />
          {errors.title && <p className="text-sm text-destructive">{errors.title.message}</p>}
        </div>

        <div className="space-y-2">
          <Label htmlFor="category">Category</Label>
          <Controller
            control={control}
            name="category"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger id="category" className="w-full sm:w-64"><SelectValue /></SelectTrigger>
                <SelectContent>
                  {CATEGORIES.map((c) => <SelectItem key={c.value} value={c.value}>{c.label}</SelectItem>)}
                </SelectContent>
              </Select>
            )}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="body">Message</Label>
          <Textarea id="body" rows={5} {...register("body")} />
          {errors.body && <p className="text-sm text-destructive">{errors.body.message}</p>}
        </div>

        <div className="space-y-2">
          <Label htmlFor="file">{hasExisting ? "Replace attachment (optional)" : "Attachment (optional)"}</Label>
          {hasExisting && (
            <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
              <span>
                Current:{" "}
                <a className="underline" href={publicNoticeFileUrl(initial!.file_path!)} target="_blank" rel="noreferrer">
                  {initial!.file_name}
                </a>
                {removeExisting && !file && " (will be removed)"}
              </span>
              {!file && (
                <Button type="button" variant="outline" size="sm" onClick={() => setRemoveExisting(!removeExisting)}>
                  {removeExisting ? "Keep attachment" : "Remove attachment"}
                </Button>
              )}
            </div>
          )}
          <Input
            id="file"
            type="file"
            accept=".pdf,.jpg,.jpeg,.png,.webp"
            onChange={(e) => {
              const f = e.target.files?.[0] ?? null
              setFile(f)
              setFileError(f ? validateNoticeFile(f) : null)
            }}
          />
          <p className="text-xs text-muted-foreground">PDF, JPG, PNG, or WebP up to 10 MB.</p>
          {fileError && <p className="text-sm text-destructive">{fileError}</p>}
        </div>

        <div className="space-y-2">
          <Label htmlFor="linkUrl">Link (optional)</Label>
          <Input id="linkUrl" type="url" placeholder="https://" {...register("linkUrl")} />
          {errors.linkUrl && <p className="text-sm text-destructive">{errors.linkUrl.message}</p>}
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="expiresOn">Expires on (optional)</Label>
            <Input id="expiresOn" type="date" {...register("expiresOn")} />
            <p className="text-xs text-muted-foreground">The notice stays visible through the end of this day.</p>
          </div>
          <div className="flex items-center justify-between gap-4 rounded-md border p-3">
            <div>
              <Label htmlFor="pinned">Pin to top</Label>
              <p className="text-xs text-muted-foreground">Pinned notices appear first in the app.</p>
            </div>
            <Controller
              control={control}
              name="pinned"
              render={({ field }) => <Switch id="pinned" checked={field.value} onCheckedChange={field.onChange} />}
            />
          </div>
        </div>

        {formError && <p className="text-sm text-destructive">{formError}</p>}

        <div className="flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            disabled={pending}
            render={<Link to="/notices" />}
            >
            Cancel
            </Button>
          <Button type="submit" disabled={pending}>
            {pending ? (file ? "Uploading..." : "Saving...") : "Save"}
          </Button>
        </div>
      </form>
    </div>
  )
}

export default function NoticeFormPage() {
  const { id } = useParams()
  const { data, isLoading, error } = useNotice(id)

  if (!id) return <NoticeForm />
  if (isLoading) return <Skeleton className="mx-auto h-96 max-w-2xl" />
  if (error || !data) return <p className="text-center text-sm text-destructive">Notice not found.</p>
  return <NoticeForm key={data.id} initial={data} />
}