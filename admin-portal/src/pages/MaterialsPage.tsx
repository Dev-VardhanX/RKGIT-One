import { useMemo, useState } from "react"
import { Link } from "react-router-dom"
import { ExternalLink, Pencil, Plus, Trash2 } from "lucide-react"
import { toast } from "sonner"

import { useDeleteMaterial, useMaterials } from "@/hooks/useMaterials"
import {
  errorMessage,
  formatBytes,
  formatDate,
  publicFileUrl,
  semestersForYear,
  type Material,
} from "@/lib/materials"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"

export default function MaterialsPage() {
  const {
    data: materials,
    isLoading,
    error,
    refetch,
  } = useMaterials()

  const del = useDeleteMaterial()

  const [q, setQ] = useState("")
  const [year, setYear] = useState("all")
  const [semester, setSemester] = useState("all")
  const [subject, setSubject] = useState("all")
  const [toDelete, setToDelete] = useState<Material | null>(null)

  const matchesYearSem = (m: Material) =>
    (year === "all" || String(m.subjects.year) === year) &&
    (semester === "all" ||
      String(m.subjects.semester) === semester)

  const subjectOptions = useMemo(
    () =>
      [
        ...new Set(
          (materials ?? [])
            .filter(matchesYearSem)
            .map((m) => m.subjects.name),
        ),
      ].sort(),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [materials, year, semester],
  )

  const filtered = useMemo(() => {
    const t = q.trim().toLowerCase()

    return (materials ?? []).filter(
      (m) =>
        matchesYearSem(m) &&
        (subject === "all" || m.subjects.name === subject) &&
        (!t ||
          [m.title, m.file_name, m.subjects.name].some((s) =>
            s.toLowerCase().includes(t),
          )),
    )
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [materials, q, year, semester, subject])

  const semesterChoices =
    year === "all"
      ? [1, 2, 3, 4, 5, 6, 7, 8]
      : semestersForYear(Number(year))

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">
            Study materials
          </h1>

          <p className="text-sm text-muted-foreground">
            Files students see in the app, by year, semester, and subject.
          </p>
        </div>

        <Button render={<Link to="/materials/new" />}>
            <Plus className="size-4" />
            Add material
        </Button>
      </div>

      <div className="flex flex-wrap gap-3">
        <Input
          className="w-full sm:w-64"
          placeholder="Search title, file, or subject"
          value={q}
          onChange={(e) => setQ(e.target.value)}
        />

        <Select
        value={year}
        onValueChange={(v) => {
            if (typeof v !== "string") return
            setYear(v)
            setSemester("all")
            setSubject("all")
        }}
        >
          <SelectTrigger className="w-36">
            <SelectValue />
          </SelectTrigger>

          <SelectContent>
            <SelectItem value="all">All years</SelectItem>

            {[1, 2, 3, 4].map((y) => (
              <SelectItem key={y} value={String(y)}>
                Year {y}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={semester}
          onValueChange={(v) => {
            if (typeof v !== "string") return
            setSemester(v)
            setSubject("all")
          }}
        >
          <SelectTrigger className="w-40">
            <SelectValue />
          </SelectTrigger>

          <SelectContent>
            <SelectItem value="all">All semesters</SelectItem>

            {semesterChoices.map((s) => (
              <SelectItem key={s} value={String(s)}>
                Semester {s}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={subject}
          onValueChange={(v) => {
            if(typeof v === "string"){
                setSubject(v)
            }
          }}
        >
          <SelectTrigger className="w-52">
            <SelectValue />
          </SelectTrigger>

          <SelectContent>
            <SelectItem value="all">All subjects</SelectItem>

            {subjectOptions.map((s) => (
              <SelectItem key={s} value={s}>
                {s}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      <div className="rounded-lg border bg-background">
        {isLoading ? (
          <div className="space-y-3 p-4">
            {[0, 1, 2, 3].map((i) => (
              <Skeleton
                key={i}
                className="h-10 w-full"
              />
            ))}
          </div>
        ) : error ? (
          <div className="p-8 text-center">
            <p className="text-sm text-destructive">
              {errorMessage(error)}
            </p>

            <Button
              variant="outline"
              className="mt-3"
              onClick={() => refetch()}
            >
              Retry
            </Button>
          </div>
        ) : filtered.length === 0 ? (
          <div className="p-10 text-center text-sm text-muted-foreground">
            {(materials ?? []).length === 0
              ? "No study materials yet. Add your first one."
              : "No materials match these filters."}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Title</TableHead>
                  <TableHead>Year / Sem</TableHead>
                  <TableHead>Subject</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Size</TableHead>
                  <TableHead>Added</TableHead>
                  <TableHead className="text-right">
                    Actions
                  </TableHead>
                </TableRow>
              </TableHeader>

              <TableBody>
                {filtered.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell className="max-w-64">
                      <div className="truncate font-medium">
                        {m.title}
                      </div>

                      <div className="truncate text-xs text-muted-foreground">
                        {m.file_name}
                      </div>
                    </TableCell>

                    <TableCell>
                      Y{m.subjects.year} / S{m.subjects.semester}
                    </TableCell>

                    <TableCell>
                      {m.subjects.name}
                    </TableCell>

                    <TableCell>
                      <Badge variant="secondary">
                        {m.file_name
                          .split(".")
                          .pop()
                          ?.toUpperCase()}
                      </Badge>
                    </TableCell>

                    <TableCell>
                      {formatBytes(m.file_size)}
                    </TableCell>

                    <TableCell>
                      {formatDate(m.created_at)}
                    </TableCell>

                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button
                            render={
                                <a
                                href={publicFileUrl(m.file_path)}
                                target="_blank"
                                rel="noreferrer"
                                aria-label="View file"
                                />
                            }
                            variant="ghost"
                            size="icon"
                            >
                            <ExternalLink className="size-4" />
                        </Button>

                        <Button
                            render={<Link to={`/materials/${m.id}/edit`} aria-label="Edit" />}
                            variant="ghost"
                            size="icon"
                            >
                            <Pencil className="size-4" />
                        </Button>

                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Delete"
                          onClick={() => setToDelete(m)}
                        >
                          <Trash2 className="size-4 text-destructive" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      <AlertDialog
        open={!!toDelete}
        onOpenChange={(o) =>
          !o && !del.isPending && setToDelete(null)
        }
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>
              Delete this material?
            </AlertDialogTitle>

            <AlertDialogDescription>
              “{toDelete?.title}” and its file will be
              permanently removed. Students will no longer
              see it in the app.
            </AlertDialogDescription>
          </AlertDialogHeader>

          <AlertDialogFooter>
            <AlertDialogCancel disabled={del.isPending}>
              Cancel
            </AlertDialogCancel>

            <AlertDialogAction
              disabled={del.isPending}
              onClick={(e) => {
                e.preventDefault()

                if (!toDelete) return

                del.mutate(toDelete, {
                  onSuccess: () => {
                    toast.success("Material deleted")
                    setToDelete(null)
                  },
                  onError: (err) =>
                    toast.error(errorMessage(err)),
                })
              }}
            >
              {del.isPending ? "Deleting..." : "Delete"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}