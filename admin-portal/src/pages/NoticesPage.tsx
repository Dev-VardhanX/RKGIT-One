import { useMemo, useState } from "react"
import { Link } from "react-router-dom"
import { Bell, ExternalLink, Pencil, Pin, Plus, Trash2 } from "lucide-react"
import { toast } from "sonner"
import { useDeleteNotice, useNotices } from "@/hooks/useNotices"
import { useDepartments } from "@/hooks/useDepartments"
import { useSendNotification } from "@/hooks/useNotify"
import { notifyToastMessage } from "@/lib/notify"
import { CATEGORIES, categoryLabel, audienceLabel, isExpired, publicNoticeFileUrl, type Notice } from "@/lib/notices"
import { errorMessage, formatBytes, formatDate } from "@/lib/materials"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Badge } from "@/components/ui/badge"
import { Skeleton } from "@/components/ui/skeleton"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent,
  AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle,
} from "@/components/ui/alert-dialog"

function formatNoticeDateTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(d)
}

export default function NoticesPage() {
  const { data: notices, isLoading, error, refetch } = useNotices()
  const { data: departments } = useDepartments()
  const del = useDeleteNotice()
  const notifyMutation = useSendNotification()
  const [q, setQ] = useState("")
  const [category, setCategory] = useState("all")
  const [status, setStatus] = useState("all")
  const [toDelete, setToDelete] = useState<Notice | null>(null)
  const [toNotify, setToNotify] = useState<Notice | null>(null)

  const filtered = useMemo(() => {
    const t = q.trim().toLowerCase()
    return (notices ?? []).filter(
      (n) =>
        (category === "all" || n.category === category) &&
        (status === "all" || (status === "expired" ? isExpired(n) : !isExpired(n))) &&
        (!t || [n.title, n.body ?? "", n.file_name ?? ""].some((s) => s.toLowerCase().includes(t)))
    )
  }, [notices, q, category, status])

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold">Notices</h1>
          <p className="text-sm text-muted-foreground">Official notices students see in the app: datesheets, holidays, and more.</p>
        </div>
        <Button render={<Link to="/notices/new" />}>
          <Plus className="size-4" />
          Add notice
        </Button>
      </div>

      <div className="flex flex-wrap gap-3">
        <Input className="w-full sm:w-64" placeholder="Search title, message, or file" value={q} onChange={(e) => setQ(e.target.value)} />
        <Select value={category} onValueChange={(value) => { if (typeof value === "string") setCategory(value) }} >
          <SelectTrigger className="w-44"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All categories</SelectItem>
            {CATEGORIES.map((c) => <SelectItem key={c.value} value={c.value}>{c.label}</SelectItem>)}
          </SelectContent>
        </Select>
        <Select value={status} onValueChange={(value) => { if (typeof value === "string") setStatus(value) }} >
          <SelectTrigger className="w-40"><SelectValue /></SelectTrigger>
          <SelectContent>
            <SelectItem value="all">All notices</SelectItem>
            <SelectItem value="active">Active</SelectItem>
            <SelectItem value="expired">Expired</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div className="rounded-lg border bg-background">
        {isLoading ? (
          <div className="space-y-3 p-4">
            {[0, 1, 2, 3].map((i) => <Skeleton key={i} className="h-10 w-full" />)}
          </div>
        ) : error ? (
          <div className="p-8 text-center">
            <p className="text-sm text-destructive">{errorMessage(error)}</p>
            <Button variant="outline" className="mt-3" onClick={() => refetch()}>Retry</Button>
          </div>
        ) : filtered.length === 0 ? (
          <div className="p-10 text-center text-sm text-muted-foreground">
            {(notices ?? []).length === 0 ? "No notices yet. Add your first one." : "No notices match these filters."}
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Title</TableHead>
                <TableHead>Category</TableHead>
                <TableHead>Audience</TableHead>
                <TableHead>Attachment</TableHead>
                <TableHead>Published</TableHead>
                <TableHead>Expires</TableHead>
                <TableHead className="whitespace-nowrap">Push</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((n) => (
                <TableRow key={n.id} className={isExpired(n) ? "opacity-60" : undefined}>
                  <TableCell className="max-w-72">
                    <div className="flex items-center gap-2">
                      {n.pinned && <Pin className="size-4 shrink-0 text-primary" aria-label="Pinned" />}
                      <span className="truncate font-medium">{n.title}</span>
                    </div>
                    {n.body && <div className="truncate text-xs text-muted-foreground">{n.body}</div>}
                  </TableCell>
                  <TableCell><Badge variant="secondary">{categoryLabel(n.category)}</Badge></TableCell>
                  <TableCell className="whitespace-nowrap">
                    <Badge variant="outline">
                      {audienceLabel(n.target_years, n.target_departments, departments ?? [])}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {n.file_name ? (
                      <div className="flex items-center gap-2">
                        <Badge variant="outline">{n.file_name.split(".").pop()?.toUpperCase()}</Badge>
                        <span className="text-xs text-muted-foreground">{formatBytes(n.file_size ?? 0)}</span>
                      </div>
                    ) : (
                      <span className="text-muted-foreground">None</span>
                    )}
                  </TableCell>
                  <TableCell>{formatDate(n.published_at)}</TableCell>
                  <TableCell>
                    {n.expires_at ? (
                      <div className="flex items-center gap-2">
                        {formatDate(n.expires_at)}
                        {isExpired(n) && <Badge variant="destructive">Expired</Badge>}
                      </div>
                    ) : (
                      <span className="text-muted-foreground">Never</span>
                    )}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {n.notified_at ? (
                      <Badge
                        variant="secondary"
                        title={`Sent on ${formatNoticeDateTime(n.notified_at)}`}
                      >
                        Sent
                      </Badge>
                    ) : (
                      <Badge variant="outline">Not sent</Badge>
                    )}
                  </TableCell>
                  <TableCell className="text-right">
                    <div className="flex justify-end gap-1">
                      {!n.notified_at && !isExpired(n) && (
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="Send notification"
                          title="Send notification"
                          onClick={() => setToNotify(n)}
                        >
                          <Bell className="size-4" />
                        </Button>
                      )}
                      {n.file_path && (
                        <Button
                          variant="ghost"
                          size="icon"
                          aria-label="View attachment"
                          render={
                            <a
                              href={publicNoticeFileUrl(n.file_path)}
                              target="_blank"
                              rel="noreferrer"
                            />
                          }
                        >
                          <ExternalLink className="size-4" />
                        </Button>
                      )}
                      <Button
                        variant="ghost"
                        size="icon"
                        aria-label="Edit"
                        render={<Link to={`/notices/${n.id}/edit`} />}
                      >
                        <Pencil className="size-4" />
                      </Button>
                      <Button variant="ghost" size="icon" aria-label="Delete" onClick={() => setToDelete(n)}>
                        <Trash2 className="size-4 text-destructive" />
                      </Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </div>

      <AlertDialog open={!!toNotify} onOpenChange={(open) => !open && !notifyMutation.isPending && setToNotify(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Send notification?</AlertDialogTitle>
            <AlertDialogDescription>
              Students will receive a push notification about “{toNotify?.title}”. This can only be done once.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={notifyMutation.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={notifyMutation.isPending}
              onClick={async (e) => {
                e.preventDefault()
                if (!toNotify) return
                const res = await notifyMutation.mutateAsync({
                  kind: "notice",
                  id: toNotify.id,
                })
                const msg = notifyToastMessage(res)
                if (msg.type === "success") {
                  toast.success(msg.text)
                } else if (msg.type === "warning") {
                  toast.warning(msg.text)
                } else {
                  toast.error(msg.text)
                }
                setToNotify(null)
                void refetch()
              }}
            >
              {notifyMutation.isPending ? "Sending..." : "Send"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={!!toDelete} onOpenChange={(o) => !o && !del.isPending && setToDelete(null)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this notice?</AlertDialogTitle>
            <AlertDialogDescription>
              “{toDelete?.title}” and its attachment (if any) will be permanently removed. Students will no longer see it in the app.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={del.isPending}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              disabled={del.isPending}
              onClick={(e) => {
                e.preventDefault()
                if (!toDelete) return
                del.mutate(toDelete, {
                  onSuccess: () => { toast.success("Notice deleted"); setToDelete(null) },
                  onError: (err) => toast.error(errorMessage(err)),
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
