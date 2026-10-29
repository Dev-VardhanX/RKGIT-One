import {
  FunctionsFetchError,
  FunctionsHttpError,
  FunctionsRelayError,
} from "@supabase/supabase-js";
import { supabase } from "@/lib/supabase";

export type NotifyKind = "notice" | "event";

export type NotifyFailureReason =
  | "forbidden"
  | "not_found"
  | "already"
  | "expired"
  | "no_recipients"
  | "service"
  | "network";

export type NotifyResult =
  | { ok: true; sent: number; failed: number }
  | {
      ok: false;
      reason: NotifyFailureReason;
      message: string;
    };

export function mapNotifyFailure(
  status: number | null,
  serverError: string | null
): { reason: NotifyFailureReason; message: string } {
  if (status === null) {
    return {
      reason: "network",
      message:
        "Could not reach the notification service. The item is saved; you can send it again from the list.",
    };
  }

  if (status === 401 || status === 403) {
    return {
      reason: "forbidden",
      message: "Your account is not allowed to send notifications.",
    };
  }

  if (status === 404) {
    return {
      reason: "not_found",
      message: "That item no longer exists.",
    };
  }

  if (status === 409) {
    return {
      reason: "already",
      message: "Students were already notified about this item.",
    };
  }

  if (status === 422) {
    if (serverError === "Notice has expired") {
      return {
        reason: "expired",
        message: "This notice has expired, so no notification was sent.",
      };
    }
    if (serverError === "No recipients") {
      return {
        reason: "no_recipients",
        message: "No students match this audience, so no notification was sent.",
      };
    }
    return {
      reason: "service",
      message:
        "The notification service had a problem. The item is saved; you can send it again from the list.",
    };
  }

  return {
    reason: "service",
    message:
      "The notification service had a problem. The item is saved; you can send it again from the list.",
  };
}

export async function sendNotification(
  kind: NotifyKind,
  id: string
): Promise<NotifyResult> {
  try {
    const { data, error } = await supabase.functions.invoke("send-notification", {
      body: { type: kind, id },
    });

    if (error) {
      if (error instanceof FunctionsHttpError) {
        const response = error.context as Response;
        const status = response.status;
        let serverError: string | null = null;
        try {
          const body: unknown = await response.clone().json();
          if (
            body &&
            typeof body === "object" &&
            "error" in body &&
            typeof (body as { error: unknown }).error === "string"
          ) {
            serverError = (body as { error: string }).error;
          }
        } catch {
          // Response body was not JSON or already consumed
        }
        const failure = mapNotifyFailure(status, serverError);
        return { ok: false, ...failure };
      }

      if (
        error instanceof FunctionsRelayError ||
        error instanceof FunctionsFetchError
      ) {
        const failure = mapNotifyFailure(null, null);
        return { ok: false, ...failure };
      }

      const failure = mapNotifyFailure(null, null);
      return { ok: false, ...failure };
    }

    const topics =
      data &&
      typeof data === "object" &&
      "topics" in data &&
      Array.isArray((data as { topics: unknown }).topics)
        ? (data as { topics: unknown[] }).topics
        : [];
    const failed =
      data &&
      typeof data === "object" &&
      "failed" in data &&
      Array.isArray((data as { failed: unknown }).failed)
        ? (data as { failed: unknown[] }).failed
        : [];

    return {
      ok: true,
      sent: topics.length,
      failed: failed.length,
    };
  } catch {
    const failure = mapNotifyFailure(null, null);
    return { ok: false, ...failure };
  }
}

export function notifyToastMessage(result: NotifyResult): {
  type: "success" | "warning" | "error";
  text: string;
} {
  if (result.ok) {
    if (result.failed === 0) {
      return {
        type: "success",
        text: "Students will be notified.",
      };
    }
    return {
      type: "warning",
      text: `Notification sent to some groups, but ${result.failed} failed.`,
    };
  }

  if (result.reason === "already") {
    return {
      type: "warning",
      text: result.message,
    };
  }

  return {
    type: "error",
    text: result.message,
  };
}

