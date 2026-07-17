# ReplaceBlocks Release Review

Date: 2026-07-17

Perspective: upstream MCA Selector maintainer/reviewer. Scope reviewed against `AGENTS.md`, `docs/ROADMAP.md`, `docs/DEV_NOTES_REPLACE_BLOCKS.md`, `docs/TEST_PLAN.md`, and the parser, Builder, preview, region-save, configuration, and modern version-specific implementations.

## A. Merge blockers

None found in the reviewed candidate after the B-class hardening changes and automated release gates.

The previously blocking risks now have explicit controls: parsing is syntax-only and shared by field/diagnostics/Builder restoration; invalid regex is rejected before execution; ReplaceBlocks-only chunk failure prevents that region from reaching the save/relight job; preset write failure rolls back memory; preview remains non-mutating; unsupported conditional execution fails closed; duplicate target block entities, lighting, heightmaps, and copied-world loads have recorded coverage.

## B. Strong recommendations addressed

- Architecture/style: parser, catalogue selection, preset persistence, preview formatting, autocomplete behavior, and block-input compatibility policy have dedicated package-private collaborators. The Dialog retains layout and event orchestration. Builder dropdown CSS remains scoped to the Builder.
- Over-design: no automatic world detection, ID-renaming table, custom JavaFX Skin, new persistence format, or parser/catalogue coupling was introduced.
- PR scope: the candidate can be reviewed as four logical commits/PRs upstream: parser/diagnostics; catalogue/Builder compatibility; region/config safety plus performance; UI isolation/docs/tests. Keeping those boundaries would make upstream review and revert safer than one feature-sized PR.
- Legacy behavior: bare/quoted regex sources, exact source SNBT, ordered overlap behavior, advanced text input, mixed-field per-chunk error handling, and version dispatch remain intact.
- Data safety: ReplaceBlocks-only region exceptions are fail-stop with chunk coordinates and no save job; preset mutations roll back when persistence fails; preview paths do not call replacement/save APIs.
- Tests: added parser error-code, future/modded ID, catalogue switching, preset rollback (false and exception), region abort, and biome/tile context call-count coverage. Existing preview/execution, popup, translation, packaging, and copied-world gates remain applicable.
- Documentation: `docs/REPLACE_BLOCKS.md` documents syntax, catalogue limits, warnings, backups, preview, relighting, old-format behavior, and the lack of automatic ID migration.
- JavaFX: popup/navigation logic is isolated and no longer depends on `.clipped-container`; the existing JavaFX 21 first-popup geometry tracker remains local to From/To/Biome autocomplete only.
- Exceptions: configuration write results are observable; runtime write failures are handled; ReplaceBlocks region failures preserve the original cause and chunk coordinate.
- Performance: regex patterns are cached. Modern ordinary rules skip biome reads, source tile-location indexes, and per-block `Point3i`; contextual rules retain preview/execution parity.

## C. Follow-up items

- The Builder Dialog is still large because the concrete From/To control owns substantial JavaFX property-editor wiring. The reusable compatibility base and autocomplete service reduce risk, but a future UI-only PR could move the remaining concrete control after adding component-level JavaFX tests. This should not be mixed with world-processing code.
- Java 1.18.2, 1.20.6, 1.21.9, 1.21.11, and 26.2 catalogues are bundled. The newest is selected by default and selection is manual; automatic world-version selection and cross-version ID conversion remain outside this release.
- The catalogue-switch title/message/note use native English and Simplified Chinese; every other locale has the English fallback. Additional native translations can follow without blocking behavior.
- Add an integration test that forces a real filesystem-level global-config write failure if the configuration path becomes injectable. Current repository tests deterministically cover false and thrown writer outcomes.
- If profiling shows tile-heavy rules are hot, packed-long block-entity coordinates can replace the current string index in a focused performance PR. Do not change this without preview/execution parity tests.

## D. Upstream PR assessment

The feature is technically credible and substantially safer than the earlier Builder-only implementation. It preserves the existing ReplaceBlocks text contract and legacy matching semantics, keeps catalogue data advisory, and treats world mutation failures conservatively. The implementation is larger than a typical MCA Selector UI PR, so upstream submission should be split along the boundaries above and should carry the copied-world evidence plus the concise user documentation.

Recommendation: acceptable for merge after the final automated gate and the pending focused catalogue-switch reset UI check. That check must prove direct empty switching, Cancel preservation, Confirm full reset, and non-blocking custom-preset compatibility warnings; earlier B-class/Phase 6 UI evidence is historical and does not mark this new behavior passed. No remaining issue justifies reopening parser semantics, adding version-ID conversion, or upgrading JavaFX as part of this release.
