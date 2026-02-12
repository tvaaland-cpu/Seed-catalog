# Seed Catalog Runbook (Current)

## Scope
This runbook captures the current delivery plan and operational decisions for Seed Catalog, including:
- Phases 0–7.
- Definitions of done.
- Scan flow (including multi-photo behavior).
- Backup/import decisions.

## Phases 0–7

### Phase 0 — Foundations
- Confirm product goals, target users, and data model boundaries.
- Establish project setup, baseline architecture, and coding conventions.
- Define initial seed entity fields and required metadata.

### Phase 1 — Core Catalog
- Implement create/read/update/delete for seed packets/lots.
- Add list and detail views.
- Support basic filtering and sorting for day-to-day use.

### Phase 2 — Capture & Photos
- Add packet/label photo capture from camera.
- Attach photos to the relevant seed lot.
- Preserve original image metadata and local references.

### Phase 3 — Scan/OCR Assist
- Introduce scan workflow to prefill fields from photos.
- Add OCR extraction and confidence-aware suggestions.
- Keep manual override as the default-safe behavior.

### Phase 4 — Data Quality & Validation
- Add validation rules for required/structured fields.
- Improve duplicate detection and user confirmation flows.
- Add edit history-friendly patterns (timestamps/source attribution).

### Phase 5 — Backup & Restore
- Add local backup export and import support.
- Verify schema compatibility and migration handling.
- Protect users against data loss with recoverable flows.

### Phase 6 — Bulk Import/Export
- Add CSV import path with template-driven columns.
- Support preview, row-level error reporting, and partial success behavior.
- Add export for user portability.

### Phase 7 — Polish & Release Readiness
- Improve UX copy, accessibility, and performance.
- Finalize QA pass, release checklist, and documentation updates.
- Lock migration/versioning strategy for production rollout.

## Definitions of Done (DoD)

### Feature DoD
A feature is done when all of the following are true:
1. Functional requirements are implemented and manually verified.
2. Edge cases are handled with explicit UX behavior.
3. Validation and error messaging are clear and actionable.
4. Data persistence/migration impact is tested.
5. Documentation is updated (including this runbook when relevant).

### Phase DoD
A phase is done when:
1. All scoped stories/tasks are completed or explicitly deferred.
2. Regressions are checked in impacted flows.
3. Known limitations are documented.
4. Any required template/import/backup artifacts are versioned in-repo.

## Scan Flow (Including Multi-Photo)

1. User opens **Scan** from add/edit seed flow.
2. User captures one or more photos (packet front, back, close-up label, etc.).
3. App stores each photo and associates it with the same draft lot record.
4. OCR runs per photo and produces candidate values by field.
5. Field candidates are merged using deterministic priority:
   - Higher OCR confidence first.
   - More specific pattern match next (e.g., date/lot patterns).
   - Most recent user-selected candidate wins ties.
6. UI presents extracted values with source photo references.
7. User confirms/edits values before saving.
8. Save persists finalized fields and retains all attached photos.

### Multi-Photo Rules
- Multi-photo capture is first-class; no single-photo limitation.
- Extraction is additive: each new photo can improve missing/low-confidence fields.
- User edits always override OCR suggestions.
- Deleting a photo removes only its extraction candidates, not confirmed user edits.

## Backup / Import Decisions

### Backup Decisions
- Backups are user-initiated and local-first.
- Backup payload includes seed records plus related photo references/metadata.
- Restore must validate format/version before applying changes.
- On incompatible backup versions, fail safely with a clear message.

### Import Decisions
- CSV import uses a fixed template header (`docs/seed_template.csv`).
- Import supports preview + row-level validation feedback.
- Invalid rows are rejected with reasons; valid rows can still be imported.
- Re-import behavior is explicit (no silent destructive overwrite).

## Notes
- This document reflects the current runbook baseline and should be updated when phase scope or data-handling decisions change.
