Capacity review, 2026-11-01
===========================

Read-only inspection of local source and the existing Lightsail server. No load, stress, database mutation or deployment was performed.

Observed server snapshot: nproc=2; RAM total 3836 MiB, available 2272 MiB; no swap. Root disk 77 GiB, 12 GiB used, 66 GiB available. Backend approximately 410.5 MiB, MongoDB 369.7 MiB, PostgreSQL 37.13 MiB, Redis 4.176 MiB. These are a momentary low-load snapshot, not measured capacity.

The application MongoDB database reported zero documents in posts, likes and comments. Each collection had only the built-in _id index. This observation does not rule out indexes in other collections or later administrative changes.

Verified source bottlenecks:

- PostService maps each uncached post with an author lookup and three MongoDB operations (like count, comment count and viewer-like existence). A twenty-post page therefore invokes sixty additional MongoDB operations plus author lookups, whose actual SQL count depends on persistence-context reuse.
- FeedCacheService invalidates every post-feed cache entry after changes. Cache keys include the viewer; the configured TTL is thirty seconds. Write activity can reduce the cache hit rate sharply.
- MediaStorageService serializes uploads using a synchronized method and file lock, walking the upload directory to compute quotas before copying the file.
- Root Compose describes one backend with PostgreSQL, MongoDB and Redis in the same deployment, without explicit per-service CPU/memory limits or a replica/load-balancer configuration.
- Repository search found no load-test tooling/configuration or MongoDB performance-index declarations in the inspected paths. Runtime indexes were inspected separately as above.

Assessment: appropriate as an MVP architecture, with unmeasured sustained/concurrent capacity. Passing unit/E2E tests verifies functionality, not throughput, latency or failure recovery. No defensible concurrent-user/RPS limit is available.

Next measurement: an isolated workload with realistic data, cached and uncached reads plus a write mix, ramping concurrency in small steps. Record request rate, latency percentiles, error rate (including intended HTTP 429 separately), CPU/RAM and database behavior. Priorities to evaluate include query batching, query-specific MongoDB indexes, narrower cache invalidation and quota accounting that avoids a directory walk per upload.
