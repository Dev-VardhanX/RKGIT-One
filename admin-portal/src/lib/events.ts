import { supabase } from "@/lib/supabase";

export const EVENT_POSTERS_BUCKET = "event-posters";
export const MAX_POSTER_SIZE = 5 * 1024 * 1024;

export const ALLOWED_POSTER_TYPES = [
  "image/jpeg",
  "image/png",
  "image/webp",
] as const;

export type EventRecord = {
  id: string;
  title: string;
  description: string | null;
  event_date: string;
  venue: string | null;
  poster_path: string | null;
  registration_url: string | null;
  notified_at: string | null;
  created_at: string;
  updated_at: string;
};

export type EventInput = {
  title: string;
  description: string;
  event_date: string;
  venue: string;
  registration_url: string;
  poster: File | null;
};

function cleanOptionalValue(value: string): string | null {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}

export function validateEventInput(input: EventInput): string | null {
  if (!input.title.trim()) {
    return "Event title is required.";
  }

  if (!input.event_date || Number.isNaN(Date.parse(input.event_date))) {
    return "Please enter a valid event date and time.";
  }

  const registrationUrl = input.registration_url.trim();

  if (registrationUrl && !registrationUrl.startsWith("https://")) {
    return "Registration link must start with https://.";
  }

  if (registrationUrl) {
    try {
      const parsed = new URL(registrationUrl);

      if (
        parsed.protocol !== "https:" ||
        !parsed.hostname ||
        !parsed.hostname.includes(".")
      ) {
        return "Please enter a valid HTTPS registration link.";
      }
    } catch {
      return "Please enter a valid HTTPS registration link.";
    }
  }

  if (input.poster) {
    if (
      !ALLOWED_POSTER_TYPES.includes(
        input.poster.type as (typeof ALLOWED_POSTER_TYPES)[number],
      )
    ) {
      return "Poster must be a JPEG, PNG, or WebP image.";
    }

    if (input.poster.size > MAX_POSTER_SIZE) {
      return "Poster size must not exceed 5 MB.";
    }
  }

  return null;
}

function sanitizeFilename(filename: string): string {
  const sanitized = filename
    .normalize("NFKD")
    .replace(/[^a-zA-Z0-9._-]/g, "_")
    .replace(/_+/g, "_")
    .replace(/^\.+/, "")
    .slice(-100);

  return sanitized || "poster";
}

async function uploadPoster(file: File): Promise<string> {
  const uniqueFolder = crypto.randomUUID();
  const safeFilename = sanitizeFilename(file.name);
  const path = `${uniqueFolder}/${safeFilename}`;

  const { error } = await supabase.storage
    .from(EVENT_POSTERS_BUCKET)
    .upload(path, file, {
      contentType: file.type,
      upsert: false,
    });

  if (error) {
    throw new Error(`Poster upload failed: ${error.message}`);
  }

  return path;
}

async function removePoster(path: string): Promise<void> {
  const { error } = await supabase.storage
    .from(EVENT_POSTERS_BUCKET)
    .remove([path]);

  if (error) {
    throw new Error(`Poster cleanup failed: ${error.message}`);
  }
}

export function getPosterUrl(path: string | null): string | null {
  if (!path) {
    return null;
  }

  const { data } = supabase.storage
    .from(EVENT_POSTERS_BUCKET)
    .getPublicUrl(path);

  return data.publicUrl;
}

export async function getEvents(): Promise<EventRecord[]> {
  const { data, error } = await supabase
    .from("events")
    .select("*")
    .order("event_date", { ascending: true });

  if (error) {
    throw new Error(`Could not load events: ${error.message}`);
  }

  return (data ?? []) as EventRecord[];
}

export async function getEvent(id: string): Promise<EventRecord> {
  const { data, error } = await supabase
    .from("events")
    .select("*")
    .eq("id", id)
    .single();

  if (error) {
    throw new Error(`Could not load event: ${error.message}`);
  }

  return data as EventRecord;
}

export async function createEvent(input: EventInput): Promise<{ id: string }> {
  const validationError = validateEventInput(input);

  if (validationError) {
    throw new Error(validationError);
  }

  let uploadedPosterPath: string | null = null;

  try {
    if (input.poster) {
      uploadedPosterPath = await uploadPoster(input.poster);
    }

    const { data, error } = await supabase
      .from("events")
      .insert({
        title: input.title.trim(),
        description: cleanOptionalValue(input.description),
        event_date: new Date(input.event_date).toISOString(),
        venue: cleanOptionalValue(input.venue),
        poster_path: uploadedPosterPath,
        registration_url: cleanOptionalValue(input.registration_url),
      })
      .select("id")
      .single();

    if (error || !data) {
      throw new Error(`Could not create event: ${error?.message ?? "Unknown error"}`);
    }

    return { id: data.id as string };
  } catch (error) {
    if (uploadedPosterPath) {
      try {
        await removePoster(uploadedPosterPath);
      } catch (cleanupError) {
        console.error(
          "Could not clean up the uploaded poster:",
          cleanupError,
        );
      }
    }

    throw error;
  }
}

export async function updateEvent(
  id: string,
  input: EventInput,
  previousPosterPath: string | null,
): Promise<void> {
  const validationError = validateEventInput(input);

  if (validationError) {
    throw new Error(validationError);
  }

  let newPosterPath: string | null = null;
  let newPosterUploaded = false;

  try {
    if (input.poster) {
      newPosterPath = await uploadPoster(input.poster);
      newPosterUploaded = true;
    }

    const nextPosterPath = newPosterUploaded
      ? newPosterPath
      : previousPosterPath;

    const { data, error } = await supabase
      .from("events")
      .update({
        title: input.title.trim(),
        description: cleanOptionalValue(input.description),
        event_date: new Date(input.event_date).toISOString(),
        venue: cleanOptionalValue(input.venue),
        registration_url: cleanOptionalValue(input.registration_url),
        poster_path: nextPosterPath,
      })
      .eq("id", id)
      .select("id");

    if (error) {
      throw new Error(`Could not update event: ${error.message}`);
    }

    if (!data || data.length === 0) {
      throw new Error(
        "The event could not be updated. You may no longer have permission.",
      );
    }
  } catch (error) {
    if (newPosterUploaded && newPosterPath) {
      try {
        await removePoster(newPosterPath);
      } catch (cleanupError) {
        console.error(
          "Could not clean up the replacement poster:",
          cleanupError,
        );
      }
    }

    throw error;
  }

  if (
    newPosterUploaded &&
    previousPosterPath &&
    previousPosterPath !== newPosterPath
  ) {
    try {
      await removePoster(previousPosterPath);
    } catch (error) {
      console.error(
        "Event updated, but the previous poster could not be removed:",
        error,
      );

      throw new Error(
        "Event updated successfully, but the old poster could not be removed. Please contact support.",
        { cause: error },
      );
    }
  }
}

export async function deleteEvent(
  id: string,
  posterPath: string | null,
): Promise<void> {
  const { data, error } = await supabase
    .from("events")
    .delete()
    .eq("id", id)
    .select("id");

  if (error) {
    throw new Error(`Could not delete event: ${error.message}`);
  }

  if (!data || data.length === 0) {
    throw new Error("The event could not be deleted.");
  }

  if (posterPath) {
    await removePoster(posterPath);
  }
}
