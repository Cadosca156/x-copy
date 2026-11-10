2026-11-07 — Post contract
Branch bugfix/post-api-contract, commit 4b22fdf, PR https://github.com/Cadosca156/x-copy/pull/27 targeting main.
Canonical flat PostResponse validated at create/feed API boundaries; alias guessing removed from Feed posts. 280 raw UTF-16 units before matching trim; text-or-nonempty-media enforced in PostRules before storage. Nullable legacy fields and old long posts readable. Comments unchanged.
Shared JSON fixture used by Java HTTP serialization and JS parser tests. Added direct multipart E2E rejection checks.
Verified locally: backend unitTest 159 passed, bootJar success; frontend 19 passed, lint/build success. Full services/E2E delegated to GitHub CI because local Docker is unavailable; CI pending at handoff. No deployment performed.
Explanation: docs/POST_CONTRACT_UK.md. Reusable rules saved in frontend-project/references/reliability.md.
