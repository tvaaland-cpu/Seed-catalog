# AGENTS.md — Project working agreements for Codex

## Goal
Build an offline-first Android seed catalog app with:
- local searchable database (FTS), photos, notes, expiration tracking
- scan seed packet (multi-photo) → OCR → candidate match → autofill with sources
- backup/restore + CSV import
- bilingual UI (EN + Norwegian Bokmål)

## Tech choices (do not change without asking)
- Kotlin + Jetpack Compose + Navigation
- Room (SQLite) + FTS5 for search
- Images stored locally; DB stores URIs/metadata
- Offline-first; sync may be added later

## Rules for changes
- Keep changes minimal and focused to the request.
- Prefer small PRs; avoid large refactors.
- Avoid adding new heavy dependencies unless necessary; explain why.
- Maintain backward compatibility of the DB (use migrations if schema changes).
- Add/maintain basic error handling (nulls, permissions, missing photos, network failures).

## Documentation + templates
- Update docs/PLAN.md when phases/scope change.
- Keep docs/seed_template.csv in sync with import code.

## Internet autofill requirements
- User must confirm the selected match before writing fetched data.
- Store fetched text offline AND store source attribution per field (source name, URL, retrievedAt, confidence).
- Never overwrite user-edited fields without an explicit confirmation step.

## Quality bar
- App must compile and run after each PR.
- No hardcoded secrets/API keys in the repo.
