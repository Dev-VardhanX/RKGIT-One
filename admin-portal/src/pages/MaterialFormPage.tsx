import { useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import { Controller, useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { z } from "zod"
import { toast } from "sonner"

import {
  useCreateMaterial,
  useMaterial,
  useSubjects,
  useUpdateMaterial,
} from "@/hooks/useMaterials"

import {
  errorMessage,
  publicFileUrl,
  semestersForYear,
  validateFile,
  type Material,
} from "@/lib/materials"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

const schema = z
  .object({
    title: z
      .string()
      .trim()
      .min(1, "Title is required")
      .max(150, "Keep the title under 150 characters"),

    description: z
      .string()
      .trim()
      .max(1000, "Keep the description under 1000 characters"),

    year: z.string().min(1, "Select a year"),

    semester: z.string().min(1, "Select a semester"),

    subjectName: z
      .string()
      .trim()
      .min(1, "Subject is required")
      .max(100, "Keep the subject under 100 characters"),
  })
  .refine(
    (v) =>
      semestersForYear(Number(v.year)).includes(
        Number(v.semester),
      ),
    {
      path: ["semester"],
      message:
        "This semester does not belong to the selected year",
    },
  )

type FormValues = z.infer<typeof schema>

function MaterialForm({
  initial,
}: {
  initial?: Material
}) {
  const navigate = useNavigate()

  const { data: subjects = [] } = useSubjects()

  const create = useCreateMaterial()
  const update = useUpdateMaterial()

  const [file, setFile] = useState<File | null>(null)
  const [fileError, setFileError] = useState<string | null>(
    null,
  )

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      title: initial?.title ?? "",
      description: initial?.description ?? "",
      year: initial
        ? String(initial.subjects.year)
        : "",
      semester: initial
        ? String(initial.subjects.semester)
        : "",
      subjectName: initial?.subjects.name ?? "",
    },
  })

  const year = watch("year")
  const semester = watch("semester")

  const suggestions = subjects.filter(
    (s) =>
      String(s.year) === year &&
      String(s.semester) === semester,
  )

  const pending = create.isPending || update.isPending

  async function onSubmit(v: FormValues) {
    if (!initial && !file) {
      setFileError("Choose a file to upload.")
      return
    }

    if (file) {
      const msg = validateFile(file)

      if (msg) {
        setFileError(msg)
        return
      }
    }

    const input = {
      title: v.title,
      description: v.description,
      year: Number(v.year),
      semester: Number(v.semester),
      subjectName: v.subjectName,
    }

    try {
      if (initial) {
        await update.mutateAsync({
          existing: initial,
          input,
          file: file ?? undefined,
        })
      } else {
        await create.mutateAsync({
          input,
          file: file!,
        })
      }

      toast.success(
        initial ? "Material updated" : "Material added",
      )

      navigate("/materials")
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <div>
        <h1 className="text-2xl font-semibold">
          {initial ? "Edit material" : "Add material"}
        </h1>

        <p className="text-sm text-muted-foreground">
          PDF, DOCX, or PPTX up to 25 MB.
        </p>
      </div>

      <form
        onSubmit={handleSubmit(onSubmit)}
        className="space-y-5 rounded-lg border bg-background p-6"
        noValidate
      >
        <div className="space-y-2">
          <Label htmlFor="title">Title</Label>

          <Input
            id="title"
            {...register("title")}
          />

          {errors.title && (
            <p className="text-sm text-destructive">
              {errors.title.message}
            </p>
          )}
        </div>

        <div className="grid gap-5 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="year">Year</Label>

            <Controller
              control={control}
              name="year"
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={(v) => {
                    field.onChange(v)
                    setValue("semester", "")
                  }}
                >
                  <SelectTrigger
                    id="year"
                    className="w-full"
                  >
                    <SelectValue placeholder="Select year" />
                  </SelectTrigger>

                  <SelectContent>
                    {[1, 2, 3, 4].map((y) => (
                      <SelectItem
                        key={y}
                        value={String(y)}
                      >
                        Year {y}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              )}
            />

            {errors.year && (
              <p className="text-sm text-destructive">
                {errors.year.message}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="semester">Semester</Label>

            <Controller
              control={control}
              name="semester"
              render={({ field }) => (
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={!year}
                >
                  <SelectTrigger
                    id="semester"
                    className="w-full"
                  >
                    <SelectValue placeholder="Select semester" />
                  </SelectTrigger>

                  <SelectContent>
                    {semestersForYear(Number(year)).map(
                      (s) => (
                        <SelectItem
                          key={s}
                          value={String(s)}
                        >
                          Semester {s}
                        </SelectItem>
                      ),
                    )}
                  </SelectContent>
                </Select>
              )}
            />

            {errors.semester && (
              <p className="text-sm text-destructive">
                {errors.semester.message}
              </p>
            )}
          </div>
        </div>

        <div className="space-y-2">
          <Label htmlFor="subjectName">Subject</Label>

          <Input
            id="subjectName"
            list="subject-options"
            placeholder="Pick an existing subject or type a new one"
            autoComplete="off"
            {...register("subjectName")}
          />

          <datalist id="subject-options">
            {suggestions.map((s) => (
              <option
                key={s.id}
                value={s.name}
              />
            ))}
          </datalist>

          {errors.subjectName && (
            <p className="text-sm text-destructive">
              {errors.subjectName.message}
            </p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="description">
            Description (optional)
          </Label>

          <Textarea
            id="description"
            rows={3}
            {...register("description")}
          />

          {errors.description && (
            <p className="text-sm text-destructive">
              {errors.description.message}
            </p>
          )}
        </div>

        <div className="space-y-2">
          <Label htmlFor="file">
            {initial
              ? "Replace file (optional)"
              : "File"}
          </Label>

          {initial && (
            <p className="text-sm text-muted-foreground">
              Current file:{" "}
              <a
                className="underline"
                href={publicFileUrl(initial.file_path)}
                target="_blank"
                rel="noreferrer"
              >
                {initial.file_name}
              </a>
            </p>
          )}

          <Input
            id="file"
            type="file"
            accept=".pdf,.docx,.pptx"
            onChange={(e) => {
              const f = e.target.files?.[0] ?? null

              setFile(f)
              setFileError(
                f ? validateFile(f) : null,
              )
            }}
          />

          {fileError && (
            <p className="text-sm text-destructive">
              {fileError}
            </p>
          )}
        </div>

        <div className="flex justify-end gap-3">
            <Button
            type="button"
            variant="outline"
            render={<Link to="/materials" />}
            disabled={pending}
            >
            Cancel
            </Button>

          <Button
            type="submit"
            disabled={pending}
          >
            {pending
              ? file
                ? "Uploading..."
                : "Saving..."
              : "Save"}
          </Button>
        </div>
      </form>
    </div>
  )
}

export default function MaterialFormPage() {
  const { id } = useParams()

  const {
    data,
    isLoading,
    error,
  } = useMaterial(id)

  if (!id) {
    return <MaterialForm />
  }

  if (isLoading) {
    return (
      <Skeleton className="mx-auto h-96 max-w-2xl" />
    )
  }

  if (error || !data) {
    return (
      <p className="text-center text-sm text-destructive">
        Material not found.
      </p>
    )
  }

  return (
    <MaterialForm
      key={data.id}
      initial={data}
    />
  )
}