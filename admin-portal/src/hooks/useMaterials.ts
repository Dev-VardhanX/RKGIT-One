import {
  useMutation,
  useQuery,
  useQueryClient,
} from "@tanstack/react-query"

import * as api from "@/lib/materials"

export const useMaterials = () =>
  useQuery({
    queryKey: ["materials"],
    queryFn: api.fetchMaterials,
  })

export const useSubjects = () =>
  useQuery({
    queryKey: ["subjects"],
    queryFn: api.fetchSubjects,
  })

export const useMaterial = (id?: string) =>
  useQuery({
    queryKey: ["material", id],
    queryFn: () => api.fetchMaterial(id!),
    enabled: !!id,
    gcTime: 0,
  })

function useRefreshAll() {
  const qc = useQueryClient()

  return () =>
    Promise.all([
      qc.invalidateQueries({ queryKey: ["materials"] }),
      qc.invalidateQueries({ queryKey: ["subjects"] }),
      qc.invalidateQueries({ queryKey: ["material"] }),
    ])
}

export function useCreateMaterial() {
  const refresh = useRefreshAll()

  return useMutation({
    mutationFn: (v: { input: api.MaterialInput; file: File }) =>
      api.createMaterial(v.input, v.file),
    onSuccess: refresh,
  })
}

export function useUpdateMaterial() {
  const refresh = useRefreshAll()

  return useMutation({
    mutationFn: (v: {
      existing: api.Material
      input: api.MaterialInput
      file?: File
    }) => api.updateMaterial(v.existing, v.input, v.file),
    onSuccess: refresh,
  })
}

export function useDeleteMaterial() {
  const refresh = useRefreshAll()

  return useMutation({
    mutationFn: (m: api.Material) => api.deleteMaterial(m),
    onSuccess: refresh,
  })
}