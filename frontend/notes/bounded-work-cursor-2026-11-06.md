Performance steps 5 and 6
=========================

Original code-only improvement sequence: 1 batch feed metadata, 2 MongoDB indexes/unique likes, 3 reduce broad cache invalidation, 4 remove per-upload quota scans, 5 bound queues/pools/waits and expensive concurrency, 6 cursor pagination for post feeds. The durable explanations for each step live in frontend/docs.

Branch: perf/bounded-work-cursor-pagination, based on origin/codex/batch-feed-queries at 04db20d (contains points 1–4). PR: https://github.com/Cadosca156/x-copy/pull/24. Main at initial fetch still 61fdfbf. No merge/deployment or production mutation performed.

Point 5: finite Tomcat workers/queue/connections, pre-security admission budgets (global/auth/read/upload/media), Hikari/PostgreSQL timeouts, MongoDB pool/CSOT with separate administration budget, bounded Lettuce queue/offline rejection, finite SMTP waits, two-worker/sixteen-task recovery queue, generic retryable 503 dependency responses. JWT storage failures return 503 separately because controller advice does not catch filter errors. Category matching uses the container's decoded servlet path.

Point 6: pagination=cursor opt-in at existing global/author/following routes; scoped versioned date+ID cursor; strict range query with limit size+1 and maxTime, no skip/count/anchor lookup. Cache only first windows. Feed/Profile/following clients migrated to nextCursor. Legacy offset API retained for existing clients, comments unchanged. Backend must support both modes before new frontend rollout. Canonical current ObjectId IDs/valid immutable dates are the data invariant; no historical ID/date rewrite performed.

Validation: 146 local backend unit tests, 13 frontend tests, frontend lint/build and backend bootJar succeeded. Point-5 CI 42202034305 passed backend/frontend and all five E2E scenarios, including live Tomcat overload recovery and SQL pg_sleep cancellation. Initial combined CI 42203753651 passed backend/frontend but the expanded browser test searched the Following tab with role button, causing a timeout; trace review identified the incorrect test selector. Changed to role tab in 83a99fb. Final CI 42204927380 and 42204920184 both passed backend/frontend and all five expanded E2E scenarios; first run reported 23.1 seconds for Playwright. Real integration coverage includes cursor walk across equal dates/deleted anchor/new insertion and indexed explain checks at two depths on 1000 isolated documents. Deploy skipped. Diagnostic trace download removed after review. Local Docker engine remains unavailable; integration tests ran only in disposable CI services. PR body updated with final result.

No RPS or concurrent-user capacity measured. Per-stage timeouts are not a whole-request deadline; no automatic retries of writes. Limits are per process. Legacy offset still costs a prefix scan, and very large following-author sets remain a separate cost.

Walkthroughs/interview material: docs/BOUNDED_WORK_UK.md and docs/CURSOR_FEED_UK.md. Preserve final CI result here, not in skill instructions.
