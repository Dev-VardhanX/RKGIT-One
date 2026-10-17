import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import * as api from "@/lib/notices"

export const useNotices = () => useQuery({ queryKey: ["notices", "list"], queryFn: api.fetchNotices })
export const useNotice = (id?: string) =>
  useQuery({ queryKey: ["notice", id], queryFn: () => api.fetchNotice(id!), enabled: !!id, gcTime: 0 })
export const useNoticeSummary = () => useQuery({ queryKey: ["notices", "summary"], queryFn: api.fetchNoticeSummary })

function useRefreshNotices() {
  const qc = useQueryClient()
  return () =>
    Promise.all([
      qc.invalidateQueries({ queryKey: ["notices"] }),
      qc.invalidateQueries({ queryKey: ["notice"] }),
    ])
}

export function useCreateNotice() {
  const refresh = useRefreshNotices()
  return useMutation({
    mutationFn: (v: { input: api.NoticeInput; file?: File }) => api.createNotice(v.input, v.file),
    onSuccess: refresh,
  })
}

export function useUpdateNotice() {
  const refresh = useRefreshNotices()
  return useMutation({
    mutationFn: (v: { existing: api.Notice; input: api.NoticeInput; change: api.AttachmentChange }) =>
      api.updateNotice(v.existing, v.input, v.change),
    onSuccess: refresh,
  })
}

export function useDeleteNotice() {
  const refresh = useRefreshNotices()
  return useMutation({ mutationFn: (n: api.Notice) => api.deleteNotice(n), onSuccess: refresh })
}