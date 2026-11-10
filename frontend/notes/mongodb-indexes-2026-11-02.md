MongoDB query indexes and unique likes
=====================================

Implemented on codex/mongodb-query-indexes, stacked on codex/batch-feed-queries because PR #20 remains open. PR: https://github.com/Cadosca156/x-copy/pull/21.

Four startup-managed indexes cover general feed sorting, author feed sorting, post comments and unique like pairs. The migration reuses equivalent full indexes, rejects conflicting definitions, validates legacy like IDs, enables prepareUnique, archives duplicate snapshots with journaled acknowledgement and a read-back comparison, removes the redundant active copies, dry-runs and completes unique conversion. LikeService handles the concurrent-add duplicate-key case.

Validation: 116 backend unit tests passed locally. Eight new real-MongoDB migration/query-plan tests and the existing integration suite passed in CI, along with five browser E2E scenarios. Final run: https://github.com/Cadosca156/x-copy/actions/runs/41446310736.

Docker Desktop remained unavailable locally; no reset, production migration or database modification was performed. Runtime index conversion will occur after the code is merged and deployed. The initial index build may extend startup. Full explanation and interview material: docs/MONGODB_INDEXES_UK.md.
