# ReplaceBlocks Release Review

Date: 2026-07-28

Code candidate reviewed: `ba6c74da`

Perspective: upstream MCA Selector maintainer/reviewer. Scope reviewed against `AGENTS.md`, `docs/ROADMAP.md`, `docs/DEV_NOTES_REPLACE_BLOCKS.md`, `docs/TEST_PLAN.md`, and the actual parser, Builder, preview, region-save, configuration, catalogue, translation, and version-specific implementations.

## A. Merge blockers

None found in the reviewed candidate after the final packaged UI rerun.

The previously blocking mutation risks have explicit controls: parsing is syntax-only and shared by field/diagnostics/Builder restoration; invalid regex is rejected before execution; ReplaceBlocks-only chunk failure prevents that region from reaching the save/relight job; preset write failure rolls back memory; preview remains non-mutating; unsupported conditional execution fails closed; duplicate target block entities, lighting, heightmaps, and copied-world loads have recorded coverage.

## B. Strong recommendations addressed

- Architecture/style: parser, catalogue selection, preset persistence, preview formatting, autocomplete behavior, and block-input compatibility policy have dedicated package-private collaborators. The Dialog retains layout and event orchestration. Builder dropdown CSS remains scoped to the Builder.
- Over-design: no automatic world detection, ID-renaming table, custom JavaFX Skin, replacement list implementation, new persistence format, or parser/catalogue coupling was introduced.
- PR scope: one complete Draft PR is acceptable because the grammar, Builder restoration, preview/execution parity, and safety tests form one contract. The PR body should give reviewers an explicit order: syntax/compatibility, execution safety, Builder/catalogues, then UI/performance/docs. Split only if the maintainer requests a smaller review unit; stacked PRs are not required.
- Legacy behavior: bare/quoted regex sources, exact source SNBT, ordered overlap behavior, advanced text input, mixed-field per-chunk error handling, and version dispatch remain intact.
- Data safety: ReplaceBlocks-only region exceptions are fail-stop with chunk coordinates and no save job; preset mutations roll back when persistence fails; preview paths do not call replacement/save APIs; adjacent relighting touches only existing region files and does not write POI/entities sidecars.
- Tests: 173 local automated tests cover parser errors, future/modded IDs, catalogue switching, preset rollback, transactional Builder closing, region abort, preview/execution parity, popup navigation/geometry/framing, performance safeguards, translations, lighting, heightmaps, and copied-world helpers.
- Documentation: `docs/REPLACE_BLOCKS.md` documents syntax, catalogue limits, warnings, backups, preview, relighting, old-format behavior, and the lack of automatic ID migration.
- JavaFX: popup/navigation logic uses public JavaFX APIs and remains local to From/To/Biome autocomplete. Cross-platform checks accept JavaFX's native up/down direction while requiring the content edge to attach on both paths; popup resize tests stay inside the current screen's usable bounds. All Builder dropdowns remove the project-wide near-black ComboBox border and retain JavaFX's native popup frame, matching the main menu without changing global styling.
- Exceptions: configuration write results are observable; runtime write failures are handled; ReplaceBlocks region failures preserve the original cause and chunk coordinate; uncaught Java exceptions reach the normal fatal log and standard error.
- Performance: regex patterns are cached. Modern ordinary rules skip biome reads, source tile-location indexes, and per-block `Point3i`; long Builder lists retain JavaFX virtualization, bound width measurement, use plain empty-query cells, and preload the immutable catalogue on a daemon thread.

## C. Follow-up items

- `ReplaceBlocksRuleBuilderDialog` is still large because the concrete controls own substantial JavaFX property-editor wiring. A later UI-only PR may extract the remaining control code after adding component-level JavaFX tests; do not mix that refactor with world-processing changes.
- The five catalogue files were generated from Mojang server reports and pass structural consistency checks, but the repository does not pin the source server-JAR hashes in a provenance manifest. Add hashes when catalogues are regenerated rather than inventing runtime version conversion.
- All 19 locale files have the same keys and placeholder sequences, and the current Builder workflow terminology is localized. Some older Builder strings still intentionally use English fallback text in several locales; native contributor translations can follow without blocking behavior.
- GitHub-hosted macOS Intel/ARM builds and DMGs pass, but no physical macOS interaction pass was available. An upstream maintainer smoke test of modal focus, popup placement, and shortcuts is desirable.
- The release workflow's Windows build, tests, shadow JAR, and app-image `jpackage` pass in the fork. Its later installer-tool download fails HTTP 401 because the fork lacks a token accepted by `Querz/build-tools`; this is repository-secret infrastructure, not a source failure.
- Gradle reports a future Gradle 10 incompatibility from `Task.project` access during runtime-image tasks. Address it in a packaging-focused maintenance PR.
- Add a real filesystem-level global-config write-failure integration test if the configuration path becomes injectable. Current tests deterministically cover false and thrown writer outcomes.

## D. Upstream PR assessment

The feature is technically credible and substantially safer than a Builder-only UI addition. It preserves the ReplaceBlocks text contract and legacy matching semantics, keeps catalogue data advisory, and treats world mutation failures conservatively. The final diff is large, but it is a coherent end-to-end feature and can be reviewed as one Draft PR with the ordered guide above.

Hosted evidence on `41d6a127`: 172 tests plus Windows `build`, `shadowJar`, and `jpackage`, macOS Intel/ARM DMGs, and Linux x64/ARM64 DEB/RPM packages pass. Exact-candidate evidence on `ba6c74da`: 173 tests, `build`, `shadowJar`, Windows `jpackage`, and packaged-runtime `--help` pass. Copied-world and Minecraft load/save/reload checks are recorded in `docs/TEST_PLAN.md`.

The final packaged Windows candidate passed the user-run From/To/Biome first-popup, repeated empty-Biome, keyboard navigation, popup boundary, native frame, catalogue/reset, and transactional-close checks on 2026-07-28.

Recommendation: acceptable for upstream review as one Draft PR. No remaining issue justifies adding automatic version detection, cross-version ID conversion, pagination, a custom JavaFX Skin, or a JavaFX upgrade to this PR.
