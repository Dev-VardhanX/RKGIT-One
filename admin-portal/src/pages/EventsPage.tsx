import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { Bell } from "lucide-react";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";

import { getPosterUrl, type EventRecord } from "@/lib/events";
import { useDeleteEvent, useEvents } from "@/hooks/useEvents";
import { useSendNotification } from "@/hooks/useNotify";
import { notifyToastMessage } from "@/lib/notify";

type EventFilter = "all" | "upcoming" | "past";

function formatEventDate(value: string): string {
  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(value));
}

function isUpcoming(event: EventRecord): boolean {
  return new Date(event.event_date).getTime() >= Date.now();
}

export default function EventsPage() {
  const eventsQuery = useEvents();
  const deleteMutation = useDeleteEvent();
  const notifyMutation = useSendNotification();

  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState<EventFilter>("all");
  const [eventToDelete, setEventToDelete] =
    useState<EventRecord | null>(null);
  const [eventToNotify, setEventToNotify] =
    useState<EventRecord | null>(null);

  const events = eventsQuery.data;

  const filteredEvents = useMemo(() => {
    const query = search.trim().toLowerCase();
    const list = events ?? [];

    return list.filter((event) => {
      const matchesSearch =
        !query ||
        event.title.toLowerCase().includes(query) ||
        (event.venue ?? "").toLowerCase().includes(query);

      const upcoming = isUpcoming(event);

      const matchesFilter =
        filter === "all" ||
        (filter === "upcoming" && upcoming) ||
        (filter === "past" && !upcoming);

      return matchesSearch && matchesFilter;
    });
  }, [events, search, filter]);

  async function handleDelete() {
    if (!eventToDelete) {
      return;
    }

    try {
      await deleteMutation.mutateAsync({
        id: eventToDelete.id,
        posterPath: eventToDelete.poster_path,
      });

      toast.success("Event deleted successfully.");
      setEventToDelete(null);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Could not delete event.";

      toast.error(message);
    }
  }

  const totalCount = events?.length ?? 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-semibold tracking-tight">
            Events
          </h1>

          <p className="mt-1 text-sm text-muted-foreground">
            Manage college events, posters, and registration links.
          </p>
        </div>

        <Button render={<Link to="/events/new" />}>
          + Add event
        </Button>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">All events</CardTitle>
        </CardHeader>

        <CardContent className="space-y-4">
          <div className="flex flex-col gap-3 md:flex-row">
            <Input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Search by event title or venue..."
              aria-label="Search events"
              className="md:max-w-sm"
            />

            <div className="flex flex-wrap gap-2">
              {(
                [
                  ["all", "All events"],
                  ["upcoming", "Upcoming"],
                  ["past", "Past"],
                ] as const
              ).map(([value, label]) => (
                <Button
                  key={value}
                  type="button"
                  size="sm"
                  variant={filter === value ? "default" : "outline"}
                  onClick={() => setFilter(value)}
                >
                  {label}
                </Button>
              ))}
            </div>
          </div>

          {eventsQuery.isPending ? (
            <div className="space-y-3">
              {Array.from({ length: 4 }).map((_, index) => (
                <Skeleton key={index} className="h-20 w-full" />
              ))}
            </div>
          ) : eventsQuery.isError ? (
            <div className="rounded-lg border border-destructive/30 p-6 text-center">
              <p className="text-sm text-destructive">
                {eventsQuery.error.message}
              </p>

              <Button
                variant="outline"
                className="mt-3"
                onClick={() => eventsQuery.refetch()}
              >
                Retry
              </Button>
            </div>
          ) : filteredEvents.length === 0 ? (
            <div className="rounded-lg border border-dashed px-4 py-12 text-center">
              <h3 className="font-medium">
                {totalCount === 0
                  ? "No events yet"
                  : "No matching events"}
              </h3>

              <p className="mt-1 text-sm text-muted-foreground">
                {totalCount === 0
                  ? "Create your first event to get started."
                  : "Try another search or filter."}
              </p>

              {totalCount === 0 && (
                <Button
                  className="mt-4"
                  render={<Link to="/events/new" />}
                >
                  Add your first event
                </Button>
              )}
            </div>
          ) : (
            <div className="overflow-x-auto rounded-md border">
              <table className="w-full min-w-[850px] text-sm">
                <thead className="bg-muted/50">
                  <tr className="border-b text-left">
                    <th className="px-4 py-3 font-medium">Event</th>
                    <th className="px-4 py-3 font-medium">Date and time</th>
                    <th className="px-4 py-3 font-medium">Venue</th>
                    <th className="px-4 py-3 font-medium">Status</th>
                    <th className="px-4 py-3 font-medium whitespace-nowrap">Push</th>
                    <th className="px-4 py-3 text-right font-medium">
                      Actions
                    </th>
                  </tr>
                </thead>

                <tbody>
                  {filteredEvents.map((event) => {
                    const upcoming = isUpcoming(event);
                    const posterUrl = getPosterUrl(event.poster_path);

                    return (
                      <tr
                        key={event.id}
                        className="border-b last:border-0 hover:bg-muted/30"
                      >
                        <td className="px-4 py-3">
                          <div className="flex items-center gap-3">
                            {posterUrl ? (
                              <img
                                src={posterUrl}
                                alt=""
                                className="h-12 w-12 shrink-0 rounded-md border object-cover"
                              />
                            ) : (
                              <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-md border bg-muted text-lg">
                                📅
                              </div>
                            )}

                            <div className="min-w-0">
                              <p className="font-medium">{event.title}</p>

                              {event.registration_url && (
                                <a
                                  href={event.registration_url}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="mt-1 block max-w-xs truncate text-xs text-primary hover:underline"
                                >
                                  Registration link
                                </a>
                              )}
                            </div>
                          </div>
                        </td>

                        <td className="px-4 py-3">
                          {formatEventDate(event.event_date)}
                        </td>

                        <td className="px-4 py-3">
                          {event.venue || "—"}
                        </td>

                        <td className="px-4 py-3">
                          <Badge variant={upcoming ? "default" : "secondary"}>
                            {upcoming ? "Upcoming" : "Past"}
                          </Badge>
                        </td>

                        <td className="px-4 py-3 whitespace-nowrap">
                          {event.notified_at ? (
                            <Badge
                              variant="secondary"
                              title={`Sent on ${formatEventDate(event.notified_at)}`}
                            >
                              Sent
                            </Badge>
                          ) : (
                            <Badge variant="outline">Not sent</Badge>
                          )}
                        </td>

                        <td className="px-4 py-3">
                          <div className="flex justify-end gap-2">
                            {!event.notified_at && (
                              <Button
                                size="sm"
                                variant="outline"
                                aria-label="Send notification"
                                title="Send notification"
                                onClick={() => setEventToNotify(event)}
                              >
                                <Bell className="size-4" />
                              </Button>
                            )}

                            <Button
                              size="sm"
                              variant="outline"
                              render={
                                <Link
                                  to={`/events/${event.id}/edit`}
                                  aria-label={`Edit ${event.title}`}
                                />
                              }
                            >
                              Edit
                            </Button>

                            <Button
                              size="sm"
                              variant="destructive"
                              onClick={() => setEventToDelete(event)}
                            >
                              Delete
                            </Button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}

          {!eventsQuery.isPending && !eventsQuery.isError && (
            <p className="text-xs text-muted-foreground">
              Showing {filteredEvents.length} of {totalCount} events.
            </p>
          )}
        </CardContent>
      </Card>

      <AlertDialog
        open={Boolean(eventToNotify)}
        onOpenChange={(open) => {
          if (!open && !notifyMutation.isPending) {
            setEventToNotify(null);
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Send notification?</AlertDialogTitle>

            <AlertDialogDescription>
              Students will receive a push notification about “{eventToNotify?.title}”. This can only be done once.
            </AlertDialogDescription>
          </AlertDialogHeader>

          <AlertDialogFooter>
            <AlertDialogCancel disabled={notifyMutation.isPending}>
              Cancel
            </AlertDialogCancel>

            <AlertDialogAction
              disabled={notifyMutation.isPending}
              onClick={async (event) => {
                event.preventDefault();
                if (!eventToNotify) return;
                const res = await notifyMutation.mutateAsync({
                  kind: "event",
                  id: eventToNotify.id,
                });
                const msg = notifyToastMessage(res);
                if (msg.type === "success") {
                  toast.success(msg.text);
                } else if (msg.type === "warning") {
                  toast.warning(msg.text);
                } else {
                  toast.error(msg.text);
                }
                setEventToNotify(null);
                void eventsQuery.refetch();
              }}
            >
              {notifyMutation.isPending ? "Sending..." : "Send"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog
        open={Boolean(eventToDelete)}
        onOpenChange={(open) => {
          if (!open && !deleteMutation.isPending) {
            setEventToDelete(null);
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Delete this event?</AlertDialogTitle>

            <AlertDialogDescription>
              This will permanently delete{" "}
              <strong>{eventToDelete?.title}</strong> and attempt to remove
              its poster. This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>

          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>
              Cancel
            </AlertDialogCancel>

            <AlertDialogAction
              disabled={deleteMutation.isPending}
              onClick={(event) => {
                event.preventDefault();
                void handleDelete();
              }}
              className="bg-destructive text-white hover:bg-destructive/90"
            >
              {deleteMutation.isPending ? "Deleting..." : "Delete event"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
