2026-11-07 — Typed error handlers
Branch bugfix/typed-error-handlers; commit ee0a7f1.
Dedicated domain types/handlers for posts, comments, likes, cursor/page/author validation, session authentication, username conflicts, malformed login input and self-follow. Existing domain mappings kept; incorrect InvalidTwoFactorCodeException 502 corrected to 400. Framework request errors handled individually; unknown errors safe 500, infrastructure remains retryable 503. ResponseStatusException compatibility remains for existing auth/media paths. Public ErrorResponse format preserved.
Local checks: 192 backend unit/HTTP tests passed, bootJar succeeded; 20 frontend tests passed, ESLint passed. Full integration/E2E CI running at handoff; local Docker unavailable. No production changes or deployment.
Explanation docs/ERROR_HANDLING_UK.md; reusable knowledge in frontend-project/references/error-handling.md.
