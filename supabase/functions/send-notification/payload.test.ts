import test from "node:test";
import assert from "node:assert/strict";
import {
  truncate,
  isUuid,
  parseRequest,
  noticeData,
  eventData,
  buildFcmMessage,
} from "./payload.ts";

test("truncate", () => {
  // shorter text unchanged
  assert.equal(truncate("hello", 10), "hello");
  // exact length unchanged
  assert.equal(truncate("hello", 5), "hello");
  // longer text cut to max code points ending in "…" (total length max)
  const cut = truncate("hello world", 6);
  assert.equal(cut, "hello…");
  assert.equal(Array.from(cut).length, 6);

  // an emoji at the cut is never split (use a string of emoji)
  const emojiStr = "😀😃😄😁😆";
  const cutEmoji = truncate(emojiStr, 4);
  assert.equal(cutEmoji, "😀😃😄…");
  assert.equal(Array.from(cutEmoji).length, 4);
});

test("isUuid", () => {
  assert.equal(isUuid("12345678-1234-1234-1234-123456789abc"), true);
  assert.equal(isUuid("12345678-1234-1234-1234-123456789ABC"), true);
  assert.equal(isUuid("123"), false);
  assert.equal(isUuid(""), false);
  assert.equal(isUuid(null), false);
  assert.equal(isUuid(undefined), false);
  assert.equal(isUuid("g0000000-0000-0000-0000-000000000000"), false);
  assert.equal(isUuid("x".repeat(36)), false);
});

test("parseRequest", () => {
  const validUuid = "12345678-1234-1234-1234-123456789abc";
  // valid notice and event
  assert.deepEqual(parseRequest({ type: "notice", id: validUuid }), {
    ok: true,
    type: "notice",
    id: validUuid,
  });
  assert.deepEqual(parseRequest({ type: "event", id: validUuid }), {
    ok: true,
    type: "event",
    id: validUuid,
  });

  // type "other" -> error
  const invalidType = parseRequest({ type: "other", id: validUuid });
  assert.equal(invalidType.ok, false);

  // missing id -> error
  const missingId = parseRequest({ type: "notice" });
  assert.equal(missingId.ok, false);

  // id not a UUID -> error
  const notUuid = parseRequest({ type: "notice", id: "123" });
  assert.equal(notUuid.ok, false);

  // body null, a string, an array -> error
  assert.equal(parseRequest(null).ok, false);
  assert.equal(parseRequest("some string").ok, false);
  assert.equal(parseRequest([]).ok, false);

  // extra fields ignored
  const extraFields = parseRequest({
    type: "notice",
    id: validUuid,
    extra: "ignored value",
    another: 1234,
  });
  assert.deepEqual(extraFields, {
    ok: true,
    type: "notice",
    id: validUuid,
  });
});

test("noticeData and eventData", () => {
  const longTitle = "A".repeat(300);
  const n = noticeData({
    id: "12345678-1234-1234-1234-123456789abc",
    title: longTitle,
    category: "exam",
  });
  assert.deepEqual(Object.keys(n).sort(), ["category", "id", "title", "type"]);
  assert.equal(n.type, "notice");
  assert.equal(n.id, "12345678-1234-1234-1234-123456789abc");
  assert.equal(n.category, "exam");
  assert.equal(Array.from(n.title).length, 120);
  assert.equal(n.title.endsWith("…"), true);
  for (const val of Object.values(n)) {
    assert.equal(typeof val, "string");
  }

  const e = eventData({
    id: "12345678-1234-1234-1234-123456789abc",
    title: longTitle,
    event_date: "2026-11-01T10:00:00Z",
    venue: null,
  });
  assert.deepEqual(Object.keys(e).sort(), ["event_date", "id", "title", "type", "venue"]);
  assert.equal(e.type, "event");
  assert.equal(e.id, "12345678-1234-1234-1234-123456789abc");
  assert.equal(e.event_date, "2026-11-01T10:00:00Z");
  assert.equal(e.venue, "");
  assert.equal(Array.from(e.title).length, 120);
  assert.equal(e.title.endsWith("…"), true);
  for (const val of Object.values(e)) {
    assert.equal(typeof val, "string");
  }
});

test("buildFcmMessage", () => {
  const data = { type: "notice", id: "uuid", title: "Test", category: "exam" };
  const msg = buildFcmMessage("n-all", data);
  const jsonStr = JSON.stringify(msg);

  // no "notification" key anywhere
  assert.equal(jsonStr.includes('"notification"'), false);
  assert.equal("notification" in msg.message, false);

  // android properties
  assert.equal(msg.message.android.priority, "HIGH");
  assert.equal(msg.message.android.ttl, "172800s");

  // data and topic preserved
  assert.deepEqual(msg.message.data, data);
  assert.equal(msg.message.topic, "n-all");
});

