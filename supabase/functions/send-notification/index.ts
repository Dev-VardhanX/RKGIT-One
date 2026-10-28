import { createClient } from "npm:@supabase/supabase-js@2.117.3";
import { JWT } from "npm:google-auth-library@9.15.1";
import {
  eventTopics,
  normalizePrefix,
  noticeTopics,
  sanitizeCodes,
  sanitizeYears,
} from "./topics.ts";
import {
  buildFcmMessage,
  eventData,
  noticeData,
  parseRequest,
} from "./payload.ts";

interface ServiceAccountKey {
  project_id: string;
  client_email: string;
  private_key: string;
}

interface NoticeRow {
  id: string;
  title: string;
  category: string;
  target_years: number[] | null;
  target_departments: string[] | null;
  expires_at: string | null;
  notified_at: string | null;
}

interface EventRow {
  id: string;
  title: string;
  event_date: string;
  venue: string | null;
  notified_at: string | null;
}

interface DepartmentCodeRow {
  code: string;
}

interface TopicSendResult {
  topic: string;
  ok: boolean;
}

const CORS_HEADERS: Record<string, string> = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

function jsonResponse(data: Record<string, unknown>, status = 200): Response {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...CORS_HEADERS,
      "Content-Type": "application/json",
    },
  });
}

async function mapConcurrent<T, R>(
  items: T[],
  limit: number,
  fn: (item: T) => Promise<R>
): Promise<R[]> {
  const results = new Array<R>(items.length);
  let currentIndex = 0;

  async function worker(): Promise<void> {
    while (currentIndex < items.length) {
      const index = currentIndex++;
      results[index] = await fn(items[index]);
    }
  }

  const workerCount = Math.min(limit, items.length);
  const workers: Promise<void>[] = [];
  for (let i = 0; i < workerCount; i++) {
    workers.push(worker());
  }
  await Promise.all(workers);
  return results;
}

Deno.serve(async (req: Request): Promise<Response> => {
  // 1. CORS preflight
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204, headers: CORS_HEADERS });
  }

  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  // 2. Authorization header check
  const authHeader = req.headers.get("Authorization") ?? req.headers.get("authorization");
  if (!authHeader) {
    return jsonResponse({ error: "Missing authorization" }, 401);
  }

  // 3. Read body as text, enforce max size of 4096 characters, parse and validate
  const text = await req.text();
  if (text.length > 4096) {
    return jsonResponse({ error: "Payload too large" }, 413);
  }

  let rawBody: unknown;
  try {
    rawBody = JSON.parse(text);
  } catch {
    return jsonResponse({ error: "Invalid JSON" }, 400);
  }

  const parsed = parseRequest(rawBody);
  if (!parsed.ok) {
    return jsonResponse({ error: parsed.error }, 400);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const supabaseAnonKey = Deno.env.get("SUPABASE_ANON_KEY");
  const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

  if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceRoleKey) {
    return jsonResponse({ error: "Server is not configured" }, 500);
  }

  // 4. Security: Admin check FIRST before reading any data (using anon client + caller auth header)
  const anonClient = createClient(supabaseUrl, supabaseAnonKey, {
    global: {
      headers: { Authorization: authHeader },
    },
  });

  const { data: isAdmin, error: adminError } = await anonClient.rpc("is_admin");
  if (adminError || isAdmin !== true) {
    return jsonResponse({ error: "Not allowed" }, 403);
  }

  const table = parsed.type === "notice" ? "notices" : "events";
  const itemId = parsed.id;
  const serviceClient = createClient(supabaseUrl, supabaseServiceRoleKey);
  let claimed = false;

  try {
    // 5. Load item using service-role client (server side only)
    let payloadData: Record<string, string>;
    let targetYears: number[] | null = null;
    let targetDepts: string[] | null = null;

    if (parsed.type === "notice") {
      const { data: notice, error: noticeError } = await serviceClient
        .from("notices")
        .select("id,title,category,target_years,target_departments,expires_at,notified_at")
        .eq("id", itemId)
        .maybeSingle<NoticeRow>();

      if (noticeError || !notice) {
        return jsonResponse({ error: "Not found" }, 404);
      }

      // 6. Check already notified and notice expiry
      if (notice.notified_at !== null) {
        return jsonResponse({ error: "Already notified" }, 409);
      }

      if (notice.expires_at !== null && new Date(notice.expires_at).getTime() < Date.now()) {
        return jsonResponse({ error: "Notice has expired" }, 422);
      }

      payloadData = noticeData(notice);
      targetYears = notice.target_years;
      targetDepts = notice.target_departments;
    } else {
      const { data: event, error: eventError } = await serviceClient
        .from("events")
        .select("id,title,event_date,venue,notified_at")
        .eq("id", itemId)
        .maybeSingle<EventRow>();

      if (eventError || !event) {
        return jsonResponse({ error: "Not found" }, 404);
      }

      // 6. Check already notified
      if (event.notified_at !== null) {
        return jsonResponse({ error: "Already notified" }, 409);
      }

      payloadData = eventData(event);
    }

    // 7. Configuration read and validation (BEFORE claiming)
    const fcmB64 = Deno.env.get("FCM_SERVICE_ACCOUNT_B64");
    if (!fcmB64) {
      return jsonResponse({ error: "Server is not configured" }, 500);
    }

    let serviceAccount: ServiceAccountKey;
    try {
      const decoded = atob(fcmB64.trim());
      const parsedSa = JSON.parse(decoded) as Record<string, unknown>;
      if (
        typeof parsedSa.project_id !== "string" ||
        !parsedSa.project_id ||
        typeof parsedSa.client_email !== "string" ||
        !parsedSa.client_email ||
        typeof parsedSa.private_key !== "string" ||
        !parsedSa.private_key
      ) {
        return jsonResponse({ error: "Server is not configured" }, 500);
      }
      serviceAccount = {
        project_id: parsedSa.project_id,
        client_email: parsedSa.client_email,
        private_key: parsedSa.private_key,
      };
    } catch {
      return jsonResponse({ error: "Server is not configured" }, 500);
    }

    let prefix: string;
    try {
      prefix = normalizePrefix(Deno.env.get("FCM_TOPIC_PREFIX"));
    } catch {
      return jsonResponse({ error: "Server is not configured" }, 500);
    }

    // 8. Determine topics
    let topics: string[];
    if (parsed.type === "event") {
      topics = eventTopics(prefix);
    } else {
      const sanitizedYears = sanitizeYears(targetYears);
      const sanitizedDepts = sanitizeCodes(targetDepts);
      let activeDeptCodes: string[] = [];

      const exactlyOneNonEmpty = (sanitizedYears.length > 0) !== (sanitizedDepts.length > 0);
      if (exactlyOneNonEmpty) {
        const { data: deptRows, error: deptError } = await serviceClient
          .from("departments")
          .select("code")
          .eq("active", true)
          .order("sort_order", { ascending: true })
          .returns<DepartmentCodeRow[]>();

        if (deptError || !deptRows) {
          return jsonResponse({ error: "Unexpected error" }, 500);
        }
        activeDeptCodes = deptRows.map((r) => r.code);
      }
      topics = noticeTopics(sanitizedYears, sanitizedDepts, activeDeptCodes, prefix);
    }

    if (topics.length === 0) {
      return jsonResponse({ error: "No recipients" }, 422);
    }

    // 9. Obtain Google OAuth2 access token BEFORE claiming
    let accessToken: string;
    try {
      const jwtClient = new JWT({
        email: serviceAccount.client_email,
        key: serviceAccount.private_key.replace(/\\n/g, "\n"),
        scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
      });
      const creds = await jwtClient.authorize();
      if (!creds.access_token) {
        return jsonResponse({ error: "Could not authenticate with the push service" }, 502);
      }
      accessToken = creds.access_token;
    } catch {
      return jsonResponse({ error: "Could not authenticate with the push service" }, 502);
    }

    // 10. Security: CLAIM atomically where notified_at is null before sending
    const claimTimestamp = new Date().toISOString();
    const { data: claimedRows, error: claimError } = await serviceClient
      .from(table)
      .update({ notified_at: claimTimestamp })
      .eq("id", itemId)
      .is("notified_at", null)
      .select("id");

    if (claimError || !claimedRows || claimedRows.length === 0) {
      return jsonResponse({ error: "Already notified" }, 409);
    }
    claimed = true;

    // 11. Send one FCM v1 message per topic with bounded concurrency (<= 10)
    const fcmEndpoint = `https://fcm.googleapis.com/v1/projects/${encodeURIComponent(serviceAccount.project_id)}/messages:send`;
    const results = await mapConcurrent<string, TopicSendResult>(topics, 10, async (topic) => {
      try {
        const res = await fetch(fcmEndpoint, {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify(buildFcmMessage(topic, payloadData)),
          signal: AbortSignal.timeout(10_000),
        });
        await res.body?.cancel();
        return { topic, ok: res.ok };
      } catch {
        return { topic, ok: false };
      }
    });

    const succeeded: string[] = [];
    const failed: string[] = [];
    for (const res of results) {
      if (res.ok) {
        succeeded.push(res.topic);
      } else {
        failed.push(res.topic);
      }
    }

    console.log(`Notification sent: type=${parsed.type} id=${itemId} success=${succeeded.length} failed=${failed.length}`);

    // 12. If every send failed: release claim and return 502
    if (succeeded.length === 0) {
      // Security: release claim on total failure
      await serviceClient.from(table).update({ notified_at: null }).eq("id", itemId);
      claimed = false;
      return jsonResponse({ error: "Push service rejected the message" }, 502);
    }

    return jsonResponse({
      ok: true,
      type: parsed.type,
      topics: succeeded,
      failed,
    }, 200);

  } catch {
    // 13. Unexpected error: release claim if claimed (best effort)
    if (claimed) {
      try {
        await serviceClient.from(table).update({ notified_at: null }).eq("id", itemId);
      } catch {
        // best effort release
      }
    }
    console.error(`Unexpected error processing notification: type=${parsed.type} id=${itemId}`);
    return jsonResponse({ error: "Unexpected error" }, 500);
  }
});

