# Stage 1 — `singleInstanceCounter`: single-instance counter + local cache

This package is the first working implementation of the URL shortener. The goal at this
stage was correctness and basic collision-safety on a single machine — not scale. This
document describes what was actually built, why it was built this way, and precisely
where and why it stops working as traffic grows, which is the reason a later package
in this repo replaces it.

## What's actually implemented here

**Note on the name:** this package was originally called `Base62`, but the code
actually encodes with `java.util.Base64` (`getBase64Hash`), not a true base-62
alphabet — Base64 output can contain `+` and `/`, which aren't URL-safe. The package
was renamed to `singleInstanceCounter` to describe what it actually is: a short-code
generator built around a single, process-local atomic counter, not a base-62 encoder.

**Short code generation** (`UrlService.getBase64Hash`):
- A shared `AtomicLong counterVar` is incremented (`updateAndGet`, genuinely atomic —
  an earlier version used `getAndSet(get()+1)`, which is *not* atomic as a whole
  operation and could produce duplicate values under concurrent calls).
- `Base64.encode(counter + longUrl)`, truncated to the first 6 characters.

**Collision handling** (`UrlService.getShortUrl`):
- Checks the generated code against both the local `ConcurrentHashMap` **and** the
  database (`existsById`) before accepting it — checking the map alone was the
  original version and missed collisions with rows that existed in the DB but weren't
  in this process's cache (e.g. after a restart).
- Bounded to ~10 regeneration attempts; on exhaustion, returns a distinct sentinel
  URL instead of silently failing.
- On a DB-detected collision, the existing row is pulled into the local map (`existsById`)
  so the next lookup for that code doesn't need another DB round trip.
- The DB check itself is fail-closed: if `existsById` throws, that exception now
  propagates and aborts the request, rather than being swallowed and treated as
  "code is free" (which risked overwriting an existing mapping).

**Storage / caching**:
- `ConcurrentHashMap<String, UrlModel>` as an in-process, unbounded cache — nothing
  ever evicts an entry (there's a commented-out `TODO` in the code acknowledging this).
- H2 as the backing store, configured as `jdbc:h2:mem:testdb` — an **in-memory**
  database. It is not a file on disk.
- Expiry (currently 2 minutes, see `getShortUrl`) is enforced by comparing
  `expireEpoch` against `System.currentTimeMillis()` at read time, both in the map
  path and the DB path.

This is a reasonable "make it work" design: it's correct for a single process, it's
thread-safe where it needs to be, and it doesn't trust a bare in-memory check when a
persistent store is available. The problems all come from *what a single process is*,
not from a logic bug.

## Why this can't carry 1M requests/day

The throughput math alone isn't actually the scary part:

- 1,000,000 requests/day ≈ **11.6 requests/sec** average.
- Real traffic isn't flat — a 5–10x peak-to-average ratio is normal for web traffic,
  so peak load is more like **60–120 requests/sec**.
- For a link shortener, reads (redirects) vastly outnumber writes (new links) — a
  common rule of thumb is on the order of 100:1. So most of that load is `GET
  /{shorturl}`, not `POST /short`.

116 requests/sec is, by itself, not a lot for a single Spring Boot instance — a naive
"just run one bigger server" could plausibly absorb the raw QPS. **The actual failure
mode is that this design cannot be scaled out or made resilient at all**, regardless of
how much headroom one instance has:

1. **The database is not shared.** `jdbc:h2:mem:testdb` is a private, in-process
   database. Run a second instance behind a load balancer for capacity or redundancy,
   and it gets its own empty database — a short link created on instance A returns
   `404` on instance B. There is no meaningful "scale out" with this datasource.
2. **The cache is not shared.** The `ConcurrentHashMap` is a plain instance field.
   Same problem as above: each instance has its own view of what exists.
3. **The counter is not shared.** `counterVar` resets to `0` on every restart and is
   independent per instance. Two instances can feed the same counter value + same
   long URL into the hash function around the same time, which increases collision
   odds at exactly the moment you're scaling out to relieve load — the opposite of
   what you want.
4. **Nothing survives a restart.** Because the datastore is in-memory, every deploy,
   crash, or restart destroys *all* previously issued short links, not just the cache.
   A production service that loses its entire dataset on every deploy isn't a scale
   problem to fix later — it's disqualifying on its own, before traffic is even a
   factor.
5. **The cache is unbounded.** Even on a single instance that never scales out or
   restarts, every short link ever created stays in the map forever. At real volume
   this is a slow, guaranteed memory leak, not a hypothetical one.

In short: the bottleneck isn't CPU or raw request handling — it's that every piece of
state that matters (data, cache, counter) lives inside one JVM's heap and dies with it.
1M requests/day just makes that concrete: it's high enough that you'd want more than
one instance and you'd expect the service to survive a restart, and this design permits
neither.

## What has to change, and why

- **A real, shared, persistent database** (e.g. Postgres/MySQL) instead of in-memory
  H2 — so data outlives a restart and is visible to every instance, not just the one
  that created it.
- **A shared cache** (Redis — already added as a dependency and given a starter
  config in `Configuration/RedisConfiguration.java`, but not wired into `UrlService`
  yet) instead of the local `ConcurrentHashMap`, so cache state is consistent across
  instances and reads don't force a DB hit just because they landed on a different
  node. Redis's own key TTL is also a much better fit for expiring short links than
  hand-rolled `expireEpoch` comparisons.
- **A code-generation strategy that doesn't rely on process-local state** — either a
  distributed counter (e.g. `INCR` in Redis, atomic across instances) or a scheme that
  doesn't need a counter at all, so scaling out doesn't increase collision risk.
- **Bounded/evicting cache** instead of an unbounded map, regardless of what backs it.

None of that is implemented in this package on purpose — this stage exists to establish
a correct, single-instance baseline first, and to make the exact scaling failure modes
concrete before paying for the complexity of a distributed design.
