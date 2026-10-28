export const YEARS = [1, 2, 3, 4];

export function sanitizeYears(input: unknown): number[] {
  if (!Array.isArray(input)) {
    return [];
  }
  const valid = new Set<number>();
  for (const item of input) {
    if (typeof item === "number" && Number.isInteger(item) && item >= 1 && item <= 4) {
      valid.add(item);
    }
  }
  return Array.from(valid).sort((a, b) => a - b);
}

export function sanitizeCodes(input: unknown): string[] {
  if (!Array.isArray(input)) {
    return [];
  }
  const result: string[] = [];
  const seen = new Set<string>();
  const codeRegex = /^[a-z0-9-]{2,20}$/;
  for (const item of input) {
    if (typeof item === "string" && codeRegex.test(item)) {
      if (!seen.has(item)) {
        seen.add(item);
        result.push(item);
      }
    }
  }
  return result;
}

export function normalizePrefix(raw: string | undefined | null): string {
  if (raw === undefined || raw === null) {
    return "";
  }
  const trimmed = raw.trim();
  if (trimmed === "") {
    return "";
  }
  if (/^[a-z0-9]{1,10}-$/.test(trimmed)) {
    return trimmed;
  }
  throw new Error("Invalid FCM_TOPIC_PREFIX");
}

export function isValidTopic(topic: string): boolean {
  return /^[a-zA-Z0-9\-_.~%]{1,200}$/.test(topic);
}

export function noticeTopics(
  targetYears: unknown,
  targetDepartments: unknown,
  activeDepartmentCodes: string[],
  prefix: string
): string[] {
  const years = sanitizeYears(targetYears);
  const depts = sanitizeCodes(targetDepartments);

  if (years.length === 0 && depts.length === 0) {
    const allTopic = prefix + "n-all";
    return isValidTopic(allTopic) ? [allTopic] : [];
  }

  const yearList = years.length > 0 ? years : YEARS;
  const deptList = depts.length > 0 ? depts : sanitizeCodes(activeDepartmentCodes);

  if (deptList.length === 0) {
    return [];
  }

  const result: string[] = [];
  const seen = new Set<string>();

  for (const year of yearList) {
    for (const code of deptList) {
      const topic = `${prefix}n-y${year}-${code}`;
      if (isValidTopic(topic) && !seen.has(topic)) {
        seen.add(topic);
        result.push(topic);
      }
    }
  }

  return result;
}

export function eventTopics(prefix: string): string[] {
  const topic = prefix + "e-all";
  return isValidTopic(topic) ? [topic] : [];
}

