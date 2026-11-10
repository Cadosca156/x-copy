Performance points 3 and 4
==========================

Branch: perf/feed-cache-media-quotas. PR: https://github.com/Cadosca156/x-copy/pull/22.
Base: codex/batch-feed-queries (a13b191, includes index PR #21); main at inspection was 61fdfbf and did not yet include that later index merge. New branch names follow the user's type-specific convention, not codex/.

Commits: 35f7b3a (shared raw Post page cache and scoped Redis generations), ba00692 (recoverable quota counters and bounded parallel staging).

Cache: raw pages shared across viewers; fresh batched SQL authors and MongoDB metadata on every nonempty response. Scope versions for global/changed author are replaced by one MSET; old page values expire after configured 30-second TTL. 64 local lock stripes bound duplicate same-page loads. No cache invalidation for likes/comments/profiles. Invalidation still is not a transaction with MongoDB. Warm DTOs cost fresh metadata queries; no RPS improvement claimed.

Media: four staging copies per service, then JVM+OS locked commit with total/owner counter files, dirty marker, atomic moves, startup/dirty reconciliation. Deletes share accounting. Private paths blocked by media resource resolver. Failed post DB save cleans up uploaded media. Ordinary operations do not scan all files; first use/recovery does. Separate-process concurrency/power loss not tested; abandoned staging and out-of-band media maintenance require care.

Validation: 130 local isolated tests and bootJar passed. Local Docker engine unavailable. CI 42197718611 frontend/backend succeeded, including the real warm-cache integration scenario; all five browser E2E scenarios passed (34.4 seconds for the Playwright run). Deploy skipped on this feature branch. PR body updated with final CI result. No production deployment or database mutation performed.

Detailed walkthroughs and interview material: docs/FEED_CACHE_UK.md, docs/MEDIA_QUOTAS_UK.md. Reusable workflow and branch convention saved in frontend-project skill; keep dated results here.
