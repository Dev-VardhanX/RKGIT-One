import { useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  BookOpen,
  CalendarDays,
  LayoutDashboard,
  LogOut,
  ShieldCheck,
} from "lucide-react";

import { useAuth } from "@/auth/AuthProvider";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

export default function DashboardPage() {
  const { signOut, session } = useAuth();
  const navigate = useNavigate();
  const [signingOut, setSigningOut] = useState(false);
  const [error, setError] = useState("");

  async function handleSignOut() {
    setSigningOut(true);
    setError("");

    try {
      await signOut();
      navigate("/login", { replace: true });
    } catch {
      setError("Unable to sign out. Please try again.");
      setSigningOut(false);
    }
  }

  return (
    <main className="min-h-screen bg-muted/30">
      <header className="border-b bg-background">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
              <ShieldCheck className="size-5" />
            </div>
            <div>
              <p className="font-semibold tracking-tight">RKGIT ONE</p>
              <p className="text-xs text-muted-foreground">
                Administration Portal
              </p>
            </div>
          </div>

          <Button
            variant="outline"
            onClick={handleSignOut}
            disabled={signingOut}
          >
            <LogOut className="mr-2 size-4" />
            {signingOut ? "Signing out..." : "Sign out"}
          </Button>
        </div>
      </header>

      <div className="mx-auto max-w-7xl space-y-8 px-4 py-8 sm:px-6 lg:px-8">
        <section>
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <LayoutDashboard className="size-4" />
            Dashboard
          </div>

          <h1 className="mt-3 text-3xl font-semibold tracking-tight">
            Welcome to RKGIT ONE
          </h1>

          <p className="mt-2 text-muted-foreground">
            Manage your campus study materials and events from one place.
          </p>

          <p className="mt-2 text-sm text-muted-foreground">
            Signed in as {session?.user.email ?? "Administrator"}
          </p>
        </section>

        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        )}

        <section className="grid gap-5 md:grid-cols-2">
          <Card>
            <CardHeader>
              <div className="mb-2 flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <BookOpen className="size-5" />
              </div>
              <CardTitle>Study Materials</CardTitle>
              <CardDescription>
                Organize and publish study resources for students.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Materials management will be added in the next step.
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <div className="mb-2 flex size-11 items-center justify-center rounded-xl bg-primary/10 text-primary">
                <CalendarDays className="size-5" />
              </div>
              <CardTitle>Campus Events</CardTitle>
              <CardDescription>
                Publish campus events and registration information.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-muted-foreground">
                Events management will be added after materials management.
              </p>
            </CardContent>
          </Card>
        </section>
      </div>
    </main>
  );
}