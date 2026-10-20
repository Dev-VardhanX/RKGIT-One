import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  ArrowRight,
  BookOpen,
  CalendarDays,
  Clock3,
  FilePlus2,
  LayoutDashboard,
  LogOut,
  Plus,
  RefreshCw,
  ShieldCheck,
} from "lucide-react";

import { getPosterUrl } from "@/lib/events";
import { materialTypeLabel } from "@/lib/materials";

import { useAuth } from "@/auth/AuthProvider";
import { useMaterials } from "@/hooks/useMaterials";
import { useEvents } from "@/hooks/useEvents";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

function formatDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
  }).format(date);
}

function formatEventDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat("en-IN", {
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  }).format(date);
}

export default function DashboardPage() {
  const { signOut, session } = useAuth();
  const navigate = useNavigate();

  const [signingOut, setSigningOut] = useState(false);
  const [signOutError, setSignOutError] = useState("");

  const {
    data: materials,
    isLoading: materialsLoading,
    isError: materialsError,
    refetch: refetchMaterials,
  } = useMaterials();

  const {
    data: events,
    isLoading: eventsLoading,
    isError: eventsError,
    refetch: refetchEvents,
  } = useEvents();

  const hasError = materialsError || eventsError;

  const latestMaterials = [...(materials ?? [])]
    .sort(
      (a, b) =>
        new Date(b.created_at).getTime() -
        new Date(a.created_at).getTime(),
    )
    .slice(0, 5);

  const latestEvents = [...(events ?? [])]
    .sort(
      (a, b) =>
        new Date(b.created_at).getTime() -
        new Date(a.created_at).getTime(),
    )
    .slice(0, 5);

  async function handleSignOut() {
    setSigningOut(true);
    setSignOutError("");

    try {
      await signOut();
      navigate("/login", { replace: true });
    } catch {
      setSignOutError("Unable to sign out. Please try again.");
      setSigningOut(false);
    }
  }

  async function handleRetry() {
    await Promise.allSettled([
      refetchMaterials(),
      refetchEvents(),
    ]);
  }

  return (
    <main className="min-h-screen bg-muted/30">
      {/* Header */}
      <header className="border-b bg-background">
        <div className="mx-auto flex max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-primary text-primary-foreground">
              <ShieldCheck className="size-5" />
            </div>

            <div>
              <p className="font-semibold tracking-tight">
                RKGIT ONE
              </p>
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
        {/* Welcome */}
        <section>
          <div className="flex items-center gap-2 text-sm font-medium text-primary">
            <LayoutDashboard className="size-4" />
            Dashboard
          </div>

          <div className="mt-3 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
            <div>
              <h1 className="text-3xl font-semibold tracking-tight">
                Welcome to RKGIT ONE
              </h1>

              <p className="mt-2 text-muted-foreground">
                Manage your campus study materials and events from one place.
              </p>

              <p className="mt-2 text-sm text-muted-foreground">
                Signed in as{" "}
                <span className="font-medium text-foreground">
                  {session?.user.email ?? "Administrator"}
                </span>
              </p>
            </div>

            <div className="flex flex-wrap gap-2">
              <Button
                render={<Link to="/materials/new" />}
              >
                <Plus className="mr-2 size-4" />
                Add material
              </Button>

              <Button
                variant="outline"
                render={<Link to="/events/new" />}
              >
                <CalendarDays className="mr-2 size-4" />
                Add event
              </Button>
            </div>
          </div>
        </section>

        {signOutError && (
          <div
            role="alert"
            className="rounded-lg border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive"
          >
            {signOutError}
          </div>
        )}

        {/* Loading and error states */}
        {hasError && (
          <div
            role="alert"
            className="flex flex-col gap-3 rounded-xl border border-destructive/30 bg-background p-4 sm:flex-row sm:items-center sm:justify-between"
          >
            <div>
              <p className="font-medium">
                Unable to load some dashboard information
              </p>
              <p className="mt-1 text-sm text-muted-foreground">
                Your existing materials and events have not been changed.
                Try loading the dashboard again.
              </p>
            </div>

            <Button
              variant="outline"
              onClick={handleRetry}
            >
              <RefreshCw className="mr-2 size-4" />
              Retry
            </Button>
          </div>
        )}

        {/* Overview cards */}
        <section>
          <div className="mb-4">
            <h2 className="text-lg font-semibold tracking-tight">
              Overview
            </h2>
            <p className="text-sm text-muted-foreground">
              A quick look at the content published in RKGIT ONE.
            </p>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <Card>
              <CardContent className="flex items-center justify-between gap-4 p-6">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-muted-foreground">
                    Total study materials
                  </p>

                  {materialsLoading ? (
                    <Skeleton className="mt-2 h-9 w-20" />
                  ) : (
                    <p className="mt-2 text-3xl font-semibold tracking-tight">
                      {materialsError ? "—" : (materials?.length ?? 0)}
                    </p>
                  )}

                  <p className="mt-2 text-sm text-muted-foreground">
                    PDFs and other learning resources
                  </p>
                </div>

                <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <BookOpen className="size-6" />
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="flex items-center justify-between gap-4 p-6">
                <div className="min-w-0">
                  <p className="text-sm font-medium text-muted-foreground">
                    Total campus events
                  </p>

                  {eventsLoading ? (
                    <Skeleton className="mt-2 h-9 w-20" />
                  ) : (
                    <p className="mt-2 text-3xl font-semibold tracking-tight">
                      {eventsError ? "—" : (events?.length ?? 0)}
                    </p>
                  )}

                  <p className="mt-2 text-sm text-muted-foreground">
                    Published college events
                  </p>
                </div>

                <div className="flex size-12 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <CalendarDays className="size-6" />
                </div>
              </CardContent>
            </Card>
          </div>
        </section>

        {/* Latest content */}
        <section className="grid items-start gap-6 lg:grid-cols-2">
          {/* Latest materials */}
          <Card>
            <CardHeader className="flex flex-row items-start justify-between gap-3 space-y-0">
              <div>
                <CardTitle>Latest study materials</CardTitle>
                <CardDescription className="mt-1">
                  Your most recently added learning resources.
                </CardDescription>
              </div>

              <Button
                variant="outline"
                size="sm"
                render={<Link to="/materials" />}
              >
                View all
                <ArrowRight className="ml-2 size-4" />
              </Button>
            </CardHeader>

            <CardContent>
              {materialsLoading ? (
                <div className="space-y-4">
                  {Array.from({ length: 4 }).map((_, index) => (
                    <div
                      key={index}
                      className="flex items-center gap-3"
                    >
                      <Skeleton className="size-10 shrink-0 rounded-lg" />
                      <div className="min-w-0 flex-1 space-y-2">
                        <Skeleton className="h-4 w-3/4" />
                        <Skeleton className="h-3 w-1/2" />
                      </div>
                    </div>
                  ))}
                </div>
              ) : materialsError ? (
                <div className="py-8 text-center">
                  <p className="text-sm text-muted-foreground">
                    Study materials could not be loaded.
                  </p>
                </div>
              ) : latestMaterials.length === 0 ? (
                <div className="rounded-xl border border-dashed px-4 py-10 text-center">
                  <FilePlus2 className="mx-auto size-8 text-muted-foreground" />
                  <p className="mt-3 font-medium">
                    No study materials yet
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Add your first resource to get started.
                  </p>

                  <Button
                    className="mt-4"
                    render={<Link to="/materials/new" />}
                  >
                    <Plus className="mr-2 size-4" />
                    Add material
                  </Button>
                </div>
              ) : (
                <div className="divide-y">
                  {latestMaterials.map((material) => (
                    <Link
                      key={material.id}
                      to={`/materials/${material.id}/edit`}
                      className="group flex items-center gap-3 py-4 first:pt-0 last:pb-0"
                    >
                      <div className="flex size-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                        <BookOpen className="size-5" />
                      </div>

                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium group-hover:text-primary">
                          {material.title}
                          <span className="ml-2 text-xs font-normal text-muted-foreground">
                            {materialTypeLabel(material.material_type)}
                          </span>
                        </p>
                        <p className="mt-1 truncate text-xs text-muted-foreground">
                          {material.subjects.name}
                          {" · "}
                          Year {material.subjects.year}
                          {" · "}
                          Semester {material.subjects.semester}
                        </p>
                      </div>

                      <div className="hidden shrink-0 items-center gap-1 text-xs text-muted-foreground sm:flex">
                        <Clock3 className="size-3" />
                        {formatDate(material.created_at)}
                      </div>

                      <ArrowRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1" />
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>

          {/* Latest events */}
          <Card>
            <CardHeader className="flex flex-row items-start justify-between gap-3 space-y-0">
              <div>
                <CardTitle>Latest events</CardTitle>
                <CardDescription className="mt-1">
                  Your most recently published campus events.
                </CardDescription>
              </div>

              <Button
                variant="outline"
                size="sm"
                render={<Link to="/events" />}
              >
                View all
                <ArrowRight className="ml-2 size-4" />
              </Button>
            </CardHeader>

            <CardContent>
              {eventsLoading ? (
                <div className="space-y-4">
                  {Array.from({ length: 4 }).map((_, index) => (
                    <div
                      key={index}
                      className="flex items-center gap-3"
                    >
                      <Skeleton className="h-14 w-20 shrink-0 rounded-lg" />
                      <div className="min-w-0 flex-1 space-y-2">
                        <Skeleton className="h-4 w-3/4" />
                        <Skeleton className="h-3 w-1/2" />
                      </div>
                    </div>
                  ))}
                </div>
              ) : eventsError ? (
                <div className="py-8 text-center">
                  <p className="text-sm text-muted-foreground">
                    Events could not be loaded.
                  </p>
                </div>
              ) : latestEvents.length === 0 ? (
                <div className="rounded-xl border border-dashed px-4 py-10 text-center">
                  <CalendarDays className="mx-auto size-8 text-muted-foreground" />
                  <p className="mt-3 font-medium">
                    No events yet
                  </p>
                  <p className="mt-1 text-sm text-muted-foreground">
                    Publish your first campus event.
                  </p>

                  <Button
                    className="mt-4"
                    render={<Link to="/events/new" />}
                  >
                    <Plus className="mr-2 size-4" />
                    Add event
                  </Button>
                </div>
              ) : (
                <div className="divide-y">
                  {latestEvents.map((event) => (
                    <Link
                      key={event.id}
                      to={`/events/${event.id}/edit`}
                      className="group flex items-center gap-3 py-4 first:pt-0 last:pb-0"
                    >
                      {getPosterUrl(event.poster_path) ? (
                        <img
                          src={getPosterUrl(event.poster_path)!}
                          alt={`${event.title} poster`}
                          className="h-14 w-20 shrink-0 rounded-lg border object-cover"
                          loading="lazy"
                        />
                      ) : (
                        <div className="flex h-14 w-20 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                          <CalendarDays className="size-6" />
                        </div>
                      )}

                      <div className="min-w-0 flex-1">
                        <p className="truncate text-sm font-medium group-hover:text-primary">
                          {event.title}
                        </p>
                        <p className="mt-1 truncate text-xs text-muted-foreground">
                          {event.venue || "Venue not specified"}
                        </p>
                        <p className="mt-1 text-xs text-muted-foreground">
                          {formatEventDate(event.event_date)}
                        </p>
                      </div>

                      <ArrowRight className="size-4 shrink-0 text-muted-foreground transition-transform group-hover:translate-x-1" />
                    </Link>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </section>
      </div>
    </main>
  );
}