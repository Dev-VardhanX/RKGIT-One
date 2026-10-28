import test from "node:test";
import assert from "node:assert/strict";
import {
  YEARS,
  sanitizeYears,
  sanitizeCodes,
  normalizePrefix,
  isValidTopic,
  noticeTopics,
  eventTopics,
} from "./topics.ts";

test("sanitizeYears", () => {
  assert.deepEqual(sanitizeYears([2, 1, 2, "3", 5, 0, 1.5, null]), [1, 2]);
  assert.deepEqual(sanitizeYears("x"), []);
  assert.deepEqual(sanitizeYears(undefined), []);
  assert.deepEqual(sanitizeYears([4, 3]), [3, 4]);
});

test("sanitizeCodes", () => {
  assert.deepEqual(
    sanitizeCodes(["it", "cse", "it", "IT", "bad code", "a", "x".repeat(21)]),
    ["it", "cse"]
  );
  assert.deepEqual(sanitizeCodes("not-an-array"), []);
  assert.deepEqual(sanitizeCodes(null), []);
});

test("normalizePrefix", () => {
  assert.equal(normalizePrefix(undefined), "");
  assert.equal(normalizePrefix(null), "");
  assert.equal(normalizePrefix(""), "");
  assert.equal(normalizePrefix("  "), "");
  assert.equal(normalizePrefix("dev-"), "dev-");
  assert.equal(normalizePrefix(" dev- "), "dev-");

  assert.throws(() => normalizePrefix("dev"), /Invalid FCM_TOPIC_PREFIX/);
  assert.throws(() => normalizePrefix("DEV-"), /Invalid FCM_TOPIC_PREFIX/);
  assert.throws(() => normalizePrefix("a-b-"), /Invalid FCM_TOPIC_PREFIX/);
  assert.throws(() => normalizePrefix("waytoolongprefix-"), /Invalid FCM_TOPIC_PREFIX/);
  assert.throws(() => normalizePrefix("dev -"), /Invalid FCM_TOPIC_PREFIX/);
  assert.throws(() => normalizePrefix("de v-"), /Invalid FCM_TOPIC_PREFIX/);
});

test("isValidTopic", () => {
  assert.equal(isValidTopic("n-all"), true);
  assert.equal(isValidTopic("dev-n-y1-cse"), true);
  assert.equal(isValidTopic(""), false);
  assert.equal(isValidTopic("a b"), false);
  assert.equal(isValidTopic("x".repeat(201)), false);
  assert.equal(isValidTopic("n/all"), false);
});

test("noticeTopics", () => {
  // both empty -> ["n-all"]
  assert.deepEqual(noticeTopics([], [], [], ""), ["n-all"]);
  // with prefix "dev-"
  assert.deepEqual(noticeTopics([], [], [], "dev-"), ["dev-n-all"]);

  // years [1,2] and ["cse","it"]
  assert.deepEqual(
    noticeTopics([1, 2], ["cse", "it"], [], ""),
    ["n-y1-cse", "n-y1-it", "n-y2-cse", "n-y2-it"]
  );

  // years [] and ["cse"] -> [1,2,3,4] with ["cse"]
  assert.deepEqual(
    noticeTopics([], ["cse"], [], ""),
    ["n-y1-cse", "n-y2-cse", "n-y3-cse", "n-y4-cse"]
  );

  // years [3] and [] with active ["cse","it","me"]
  assert.deepEqual(
    noticeTopics([3], [], ["cse", "it", "me"], ""),
    ["n-y3-cse", "n-y3-it", "n-y3-me"]
  );

  // years [3] and [] with NO active departments -> []
  assert.deepEqual(noticeTopics([3], [], [], ""), []);

  // invalid entries ignored ([7, "x"] counts as no years -> fallback to all years)
  assert.deepEqual(
    noticeTopics([7, "x"], ["cse"], [], ""),
    ["n-y1-cse", "n-y2-cse", "n-y3-cse", "n-y4-cse"]
  );

  // duplicates removed
  assert.deepEqual(
    noticeTopics([1, 1], ["cse", "cse"], [], ""),
    ["n-y1-cse"]
  );

  // prefix is applied to every topic
  const prefixed = noticeTopics([1, 2], ["cse"], [], "prod-");
  assert.deepEqual(prefixed, ["prod-n-y1-cse", "prod-n-y2-cse"]);

  // every returned topic passes isValidTopic
  for (const topic of prefixed) {
    assert.equal(isValidTopic(topic), true);
  }
});

test("eventTopics", () => {
  assert.deepEqual(eventTopics(""), ["e-all"]);
  assert.deepEqual(eventTopics("dev-"), ["dev-e-all"]);
});

