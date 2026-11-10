Batch-feed query optimization
=============================

Implemented in branch codex/batch-feed-queries, PR https://github.com/Cadosca156/x-copy/pull/20.

PostService now loads page-scoped metadata in batches: SQL authors, MongoDB like counts, comment counts and viewer-liked post IDs. DTO mapping performs no database I/O. The shared loader serves general/profile/following feeds and creation responses; empty pages skip metadata reads, deleted/malformed authors retain the existing fallback, and missing groups default to zero.

Validation completed: 112 isolated backend unit tests passed locally. CI backend/frontend and five E2E scenarios passed at https://github.com/Cadosca156/x-copy/actions/runs/41307813193. New real-database tests verified one MongoDB find, three aggregates, zero getMore and one SQL statement for a cold twenty-post profile page, and checked page bounds and viewer isolation.

Local integration/E2E services could not start because Docker Desktop failed in its Inference Manager. No factory reset or production configuration change was performed; actual-database/E2E verification ran in CI.

Reusable detailed explanation and interview script: docs/BATCH_FEED_QUERIES_UK.md. No throughput/concurrency benchmark was run. MongoDB indexes, cache strategy and media quota accounting remain separate follow-up tasks. Production deployment requires merging the PR.
