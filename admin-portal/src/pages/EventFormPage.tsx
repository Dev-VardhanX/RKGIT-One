import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { z } from "zod";

import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";

import {
  ALLOWED_POSTER_TYPES,
  MAX_POSTER_SIZE,
  getPosterUrl,
  validateEventInput,
  type EventInput,
  type EventRecord,
} from "@/lib/events";

import {
  useCreateEvent,
  useEvent,
  useUpdateEvent,
} from "@/hooks/useEvents";
import { useSendNotification } from "@/hooks/useNotify";
import { notifyToastMessage } from "@/lib/notify";

const eventSchema = z.object({
  title: z.string().trim().min(1, "Event title is required."),
  description: z.string(),
  event_date: z.string().min(1, "Date and time are required."),
  venue: z.string(),
  registration_url: z.string(),
  notify: z.boolean(),
});

function toLocalDateTime(value: string): string {
  const date = new Date(value);

  const offset = date.getTimezoneOffset() * 60_000;

  return new Date(date.getTime() - offset)
    .toISOString()
    .slice(0, 16);
}

function EventForm({ initial }: { initial?: EventRecord }) {
  const navigate = useNavigate();
  const editing = Boolean(initial);

  const createMutation = useCreateEvent();
  const updateMutation = useUpdateEvent();
  const sendNotificationMutation = useSendNotification();

  const [title, setTitle] = useState(initial?.title ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [eventDate, setEventDate] = useState(
    initial ? toLocalDateTime(initial.event_date) : ""
  );
  const [venue, setVenue] = useState(initial?.venue ?? "");
  const [registrationUrl, setRegistrationUrl] = useState(
    initial?.registration_url ?? ""
  );
  const [poster, setPoster] = useState<File | null>(null);
  const [posterPreview, setPosterPreview] = useState<string | null>(
    getPosterUrl(initial?.poster_path ?? null)
  );
  const [existingPosterPath] = useState<string | null>(
    initial?.poster_path ?? null
  );
  const [notify, setNotify] = useState(!initial);
  const [formError, setFormError] = useState<string | null>(null);
  const [isNotifying, setIsNotifying] = useState(false);

  const pending = createMutation.isPending || updateMutation.isPending || isNotifying;

  function handlePosterChange(file: File | null) {
    setFormError(null);

    if (!file) {
      setPoster(null);
      setPosterPreview(getPosterUrl(existingPosterPath));
      return;
    }

    if (
      !ALLOWED_POSTER_TYPES.includes(
        file.type as (typeof ALLOWED_POSTER_TYPES)[number],
      )
    ) {
      setFormError("Choose a JPEG, PNG, or WebP image.");
      return;
    }

    if (file.size > MAX_POSTER_SIZE) {
      setFormError("Poster size must not exceed 5 MB.");
      return;
    }

    setPoster(file);
    setPosterPreview(URL.createObjectURL(file));
  }

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setFormError(null);

    const parsed = eventSchema.safeParse({
      title,
      description,
      event_date: eventDate,
      venue,
      registration_url: registrationUrl,
      notify,
    });

    if (!parsed.success) {
      const message = parsed.error.issues[0]?.message ?? "Invalid event details.";
      setFormError(message);
      return;
    }

    const input: EventInput = {
      title,
      description,
      event_date: eventDate,
      venue,
      registration_url: registrationUrl,
      poster,
    };

    const validationError = validateEventInput(input);

    if (validationError) {
      setFormError(validationError);
      return;
    }

    try {
      if (editing && initial) {
        await updateMutation.mutateAsync({
          id: initial.id,
          input,
          previousPosterPath: existingPosterPath,
        });

        toast.success("Event updated successfully.");
      } else {
        const res = await createMutation.mutateAsync(input);

        toast.success("Event created successfully.");

        if (notify && res?.id) {
          setIsNotifying(true);
          try {
            const notifyResult = await sendNotificationMutation.mutateAsync({
              kind: "event",
              id: res.id,
            });
            const toastMsg = notifyToastMessage(notifyResult);
            if (toastMsg.type === "success") {
              toast.success(toastMsg.text);
            } else if (toastMsg.type === "warning") {
              toast.warning(toastMsg.text);
            } else {
              toast.error(toastMsg.text);
            }
          } catch {
            // Notifications should not block navigation on unexpected failure
          } finally {
            setIsNotifying(false);
          }
        }
      }

      navigate("/events");
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "Something went wrong.";

      setFormError(message);
      toast.error(message);
    }
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <Button
          variant="ghost"
          render={<Link to="/events" />}
          className="mb-3 -ml-3"
        >
          ← Back to events
        </Button>

        <h1 className="text-2xl font-semibold tracking-tight">
          {editing ? "Edit event" : "Add event"}
        </h1>

        <p className="mt-1 text-sm text-muted-foreground">
          {editing
            ? "Update the event information and poster."
            : "Publish a new college event."}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Event details</CardTitle>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="space-y-2">
              <Label htmlFor="title">Event title *</Label>
              <Input
                id="title"
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="e.g. Annual Technical Fest"
                required
                maxLength={200}
              />
            </div>

            <div className="grid gap-5 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="eventDate">Date and time *</Label>
                <Input
                  id="eventDate"
                  type="datetime-local"
                  value={eventDate}
                  onChange={(event) => setEventDate(event.target.value)}
                  required
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="venue">Venue</Label>
                <Input
                  id="venue"
                  value={venue}
                  onChange={(event) => setVenue(event.target.value)}
                  placeholder="e.g. Main Auditorium"
                  maxLength={200}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                placeholder="Provide event details..."
                rows={5}
                maxLength={10000}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="registrationUrl">
                Registration link
              </Label>

              <Input
                id="registrationUrl"
                type="url"
                value={registrationUrl}
                onChange={(event) =>
                  setRegistrationUrl(event.target.value)
                }
                placeholder="https://example.com/register"
              />

              <p className="text-xs text-muted-foreground">
                Optional. The link must start with https://.
              </p>
            </div>

            <div className="space-y-3">
              <Label htmlFor="poster">Event poster</Label>

              <Input
                id="poster"
                type="file"
                accept="image/jpeg,image/png,image/webp"
                onChange={(event) =>
                  handlePosterChange(event.target.files?.[0] ?? null)
                }
              />

              <p className="text-xs text-muted-foreground">
                JPEG, PNG, or WebP. Maximum size: 5 MB.
                {editing && " Leave empty to keep the current poster."}
              </p>

              {posterPreview && (
                <div className="max-w-md space-y-2">
                  <img
                    src={posterPreview}
                    alt="Event poster preview"
                    className="max-h-80 w-full rounded-lg border object-contain"
                  />

                  {poster && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        setPoster(null);
                        setPosterPreview(getPosterUrl(existingPosterPath));
                      }}
                    >
                      Cancel poster replacement
                    </Button>
                  )}
                </div>
              )}
            </div>

            {!editing && (
              <div className="flex items-start justify-between gap-4 rounded-md border p-3">
                <div className="space-y-1">
                  <Label htmlFor="notify">Notify students</Label>
                  <p className="text-xs text-muted-foreground">
                    Sends a push notification to students' phones. This can be done only once per item.
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {notify
                      ? "Will notify: Everyone"
                      : "No notification will be sent. You can send it later from the list."}
                  </p>
                </div>
                <Switch
                  id="notify"
                  checked={notify}
                  onCheckedChange={(checked) => setNotify(checked)}
                />
              </div>
            )}

            {formError && (
              <div
                role="alert"
                className="rounded-md border border-destructive/30 bg-destructive/5 p-3 text-sm text-destructive"
              >
                {formError}
              </div>
            )}

            <div className="flex flex-col-reverse gap-3 border-t pt-5 sm:flex-row sm:justify-end">
              <Button
                type="button"
                variant="outline"
                render={<Link to="/events" />}
                disabled={pending}
              >
                Cancel
              </Button>

              <Button type="submit" disabled={pending}>
                {isNotifying
                  ? "Sending notification..."
                  : pending
                    ? "Saving..."
                    : editing
                      ? "Save changes"
                      : "Create event"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

export default function EventFormPage() {
  const { id } = useParams();
  const eventQuery = useEvent(id ?? "");

  if (!id) {
    return <EventForm />;
  }

  if (eventQuery.isPending) {
    return (
      <div className="mx-auto max-w-3xl space-y-4">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  if (eventQuery.isError || !eventQuery.data) {
    return (
      <Card>
        <CardContent className="space-y-4 pt-6">
          <p className="text-sm text-destructive">
            {eventQuery.error?.message ?? "Event not found."}
          </p>

          <Button
            variant="outline"
            onClick={() => eventQuery.refetch()}
          >
            Retry
          </Button>

          <Button render={<Link to="/events" />}>
            Back to events
          </Button>
        </CardContent>
      </Card>
    );
  }

  return <EventForm key={eventQuery.data.id} initial={eventQuery.data} />;
}
