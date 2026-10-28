export function truncate(text: string, max: number): string {
  if (max <= 0) {
    return "";
  }
  const chars = Array.from(text);
  if (chars.length <= max) {
    return text;
  }
  return chars.slice(0, max - 1).join("") + "…";
}

export function isUuid(value: unknown): value is string {
  if (typeof value !== "string") {
    return false;
  }
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}

export function parseRequest(
  body: unknown
): { ok: true; type: "notice" | "event"; id: string } | { ok: false; error: string } {
  if (typeof body !== "object" || body === null || Array.isArray(body)) {
    return { ok: false, error: "Invalid request body" };
  }
  const record = body as Record<string, unknown>;
  const type = record.type;
  if (type !== "notice" && type !== "event") {
    return { ok: false, error: "Invalid type" };
  }
  const id = record.id;
  if (!isUuid(id)) {
    return { ok: false, error: "Invalid id" };
  }
  return { ok: true, type, id };
}

export function noticeData(n: {
  id: string;
  title: string;
  category: string;
}): Record<string, string> {
  return {
    type: "notice",
    id: String(n.id),
    title: truncate(String(n.title), 120),
    category: String(n.category),
  };
}

export function eventData(e: {
  id: string;
  title: string;
  event_date: string;
  venue: string | null;
}): Record<string, string> {
  return {
    type: "event",
    id: String(e.id),
    title: truncate(String(e.title), 120),
    event_date: String(e.event_date),
    venue: truncate(e.venue ?? "", 80),
  };
}

export function buildFcmMessage(
  topic: string,
  data: Record<string, string>
): {
  message: {
    topic: string;
    data: Record<string, string>;
    android: {
      priority: string;
      ttl: string;
    };
  };
} {
  return {
    message: {
      topic,
      data,
      android: {
        priority: "HIGH",
        ttl: "172800s",
      },
    },
  };
}

