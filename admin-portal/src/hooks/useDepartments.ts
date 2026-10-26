import { useQuery } from "@tanstack/react-query"
import { fetchDepartments } from "@/lib/departments"

export const useDepartments = () =>
  useQuery({
    queryKey: ["departments"],
    queryFn: fetchDepartments,
  })
