#!/usr/bin/env node
/*
 * Simple concurrent-user load test for the CourseChecker backend.
 *
 * Each virtual user logs in once, then loops:
 *   search "COMPSCI" -> pick a random returned course -> fetch its detail
 * until the duration expires. Reports throughput, error rate, and
 * p50/p95/p99 latency per endpoint.
 *
 * Usage:
 *   node loadtest/run-load-test.mjs [--users N] [--duration S] [--base-url URL]
 *
 * Requires the backend running on the target URL with the dev profile
 * (so seeded users and courses are present):
 *   docker compose up -d
 *   cd server && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
 */

const args = parseArgs(process.argv.slice(2));
const BASE_URL = args.baseUrl ?? "http://localhost:8080";
const USERS = args.users ?? 10;
const DURATION_S = args.duration ?? 20;

const SEEDED_ACCOUNTS = [
  { email: "jdoe@umass.edu", password: "password123!" },
  { email: "jsmith@umass.edu", password: "password123!" },
];

const stats = new Map();

async function main() {
  console.log(
    `Load test: ${USERS} virtual users, ${DURATION_S}s, target ${BASE_URL}`,
  );

  const probe = await fetch(`${BASE_URL}/api/v1/courses/search?q=COMPSCI`).catch(
    (error) => ({ ok: false, status: 0, error }),
  );
  if (!probe.ok) {
    console.error(
      `\nBackend not reachable at ${BASE_URL}. Start it first:\n` +
        `  docker compose up -d\n` +
        `  cd server && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`,
    );
    process.exit(1);
  }

  const startedAt = performance.now();
  const deadline = startedAt + DURATION_S * 1000;

  const progress = setInterval(() => process.stdout.write("."), 1000);

  const workers = Array.from({ length: USERS }, (_, i) =>
    runVirtualUser(i, deadline),
  );
  await Promise.all(workers);

  clearInterval(progress);
  process.stdout.write("\n");

  const elapsedS = (performance.now() - startedAt) / 1000;
  printSummary(elapsedS);
}

async function runVirtualUser(id, deadline) {
  const account = SEEDED_ACCOUNTS[id % SEEDED_ACCOUNTS.length];

  try {
    await login(account);
  } catch {
    return;
  }

  while (performance.now() < deadline) {
    try {
      const results = await search("COMPSCI");
      if (Array.isArray(results) && results.length > 0) {
        const pick = results[Math.floor(Math.random() * results.length)];
        await fetchDetail(pick.courseCode);
      }
    } catch {
      // errors already recorded by timed()
    }
  }
}

function login(account) {
  return timed("POST /api/v1/auth/login        ", async () => {
    const response = await fetch(`${BASE_URL}/api/v1/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(account),
    });
    if (!response.ok) throw new Error(`login HTTP ${response.status}`);
    return response.json();
  });
}

function search(query) {
  return timed("GET  /api/v1/courses/search    ", async () => {
    const response = await fetch(
      `${BASE_URL}/api/v1/courses/search?q=${encodeURIComponent(query)}`,
    );
    if (!response.ok) throw new Error(`search HTTP ${response.status}`);
    return response.json();
  });
}

function fetchDetail(courseCode) {
  return timed("GET  /api/v1/courses/:code     ", async () => {
    const response = await fetch(
      `${BASE_URL}/api/v1/courses/${encodeURIComponent(courseCode)}`,
    );
    if (!response.ok) throw new Error(`detail HTTP ${response.status}`);
    return response.json();
  });
}

async function timed(label, action) {
  const start = performance.now();
  try {
    const result = await action();
    record(label, performance.now() - start, true);
    return result;
  } catch (error) {
    record(label, performance.now() - start, false);
    throw error;
  }
}

function record(label, latencyMs, ok) {
  if (!stats.has(label)) {
    stats.set(label, { latencies: [], errors: 0 });
  }
  const entry = stats.get(label);
  if (ok) {
    entry.latencies.push(latencyMs);
  } else {
    entry.errors += 1;
  }
}

function printSummary(elapsedS) {
  let totalReqs = 0;
  let totalErrs = 0;
  for (const { latencies, errors } of stats.values()) {
    totalReqs += latencies.length + errors;
    totalErrs += errors;
  }

  const errorPct = totalReqs === 0 ? 0 : (totalErrs / totalReqs) * 100;

  console.log(`\nLoad Test Summary`);
  console.log(`=================`);
  console.log(`Duration:       ${elapsedS.toFixed(2)} s`);
  console.log(`Virtual users:  ${USERS}`);
  console.log(`Total requests: ${totalReqs}`);
  console.log(`Throughput:     ${(totalReqs / elapsedS).toFixed(1)} req/s`);
  console.log(`Errors:         ${totalErrs} (${errorPct.toFixed(2)}%)`);
  console.log(`\nPer-endpoint latency (ms):`);

  for (const [label, { latencies, errors }] of stats.entries()) {
    const sorted = [...latencies].sort((a, b) => a - b);
    const line =
      `  ${label} ` +
      `p50=${pad(quantile(sorted, 0.5))} ` +
      `p95=${pad(quantile(sorted, 0.95))} ` +
      `p99=${pad(quantile(sorted, 0.99))} ` +
      `n=${latencies.length.toString().padStart(5)} ` +
      `errors=${errors}`;
    console.log(line);
  }
}

function quantile(sortedAsc, q) {
  if (sortedAsc.length === 0) return 0;
  const index = Math.min(sortedAsc.length - 1, Math.floor(q * sortedAsc.length));
  return sortedAsc[index];
}

function pad(n) {
  return Math.round(n).toString().padStart(4);
}

function parseArgs(argv) {
  const out = {};
  for (let i = 0; i < argv.length; i++) {
    const flag = argv[i];
    if (!flag.startsWith("--")) continue;
    const key = flag.slice(2).replace(/-([a-z])/g, (_, c) => c.toUpperCase());
    const raw = argv[++i];
    const num = Number(raw);
    out[key] = Number.isFinite(num) && raw.trim() !== "" ? num : raw;
  }
  return out;
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
