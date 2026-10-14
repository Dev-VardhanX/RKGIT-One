import { useState } from "react"
import { NavLink, Outlet } from "react-router-dom"
import {
  FileText,
  LayoutDashboard,
  LogOut,
  Menu,
  X,
} from "lucide-react"

import { useAuth } from "@/auth/AuthProvider"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

const links = [
  {
    to: "/",
    label: "Dashboard",
    icon: LayoutDashboard,
    end: true,
  },
  {
    to: "/materials",
    label: "Study materials",
    icon: FileText,
    end: false,
  },
]

export function AppLayout() {
  const [open, setOpen] = useState(false)
  const { signOut } = useAuth()

  const nav = (
    <nav className="flex flex-1 flex-col gap-1 p-3">
      {links.map(({ to, label, icon: Icon, end }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          onClick={() => setOpen(false)}
          className={({ isActive }) =>
            cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium",
              isActive
                ? "bg-primary text-primary-foreground"
                : "text-muted-foreground hover:bg-muted hover:text-foreground",
            )
          }
        >
          <Icon className="size-4" />
          {label}
        </NavLink>
      ))}
    </nav>
  )

  const signOutButton = (
    <div className="border-t p-3">
      <Button
        variant="ghost"
        className="w-full justify-start gap-3"
        onClick={signOut}
      >
        <LogOut className="size-4" />
        Sign out
      </Button>
    </div>
  )

  return (
    <div className="min-h-screen bg-muted/40">
      <aside className="fixed inset-y-0 left-0 hidden w-60 flex-col border-r bg-background md:flex">
        <div className="px-6 py-5 text-lg font-semibold">
          RKGIT ONE
        </div>

        {nav}
        {signOutButton}
      </aside>

      <header className="sticky top-0 z-30 flex items-center justify-between border-b bg-background px-4 py-3 md:hidden">
        <span className="font-semibold">RKGIT ONE</span>

        <Button
          variant="ghost"
          size="icon"
          aria-label="Toggle menu"
          onClick={() => setOpen(!open)}
        >
          {open ? (
            <X className="size-5" />
          ) : (
            <Menu className="size-5" />
          )}
        </Button>
      </header>

      {open && (
        <div className="sticky top-[57px] z-20 border-b bg-background md:hidden">
          {nav}
          {signOutButton}
        </div>
      )}

      <main className="md:pl-60">
        <div className="mx-auto max-w-6xl p-4 md:p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}