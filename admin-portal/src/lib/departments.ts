import { supabase } from "@/lib/supabase"

export type Department = {
  id: string
  code: string
  name: string
  sort_order: number
  active: boolean
}

export async function fetchDepartments(): Promise<Department[]> {
  const { data, error } = await supabase
    .from("departments")
    .select("*")
    .order("sort_order")
    .order("name")

  if (error) throw error
  return data as Department[]
}
