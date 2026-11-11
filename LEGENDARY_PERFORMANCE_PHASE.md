# RVH Video — Legendary Performance Phase

## Checkpoint: 102A — Playback + Scan Hot Path

This checkpoint focuses on reducing unnecessary work without changing the visible product direction.

### Changes

- Playback session persistence is now throttled. The player still publishes position state every second for UI responsiveness, but SharedPreferences is no longer written every second during playback.
- Explicit playback actions (play/switch media, pause, seek, speed change, release) force persistence so resume behavior remains reliable.
- Scanner existing-video state is loaded once before the MediaStore pass. Changed files no longer trigger an additional Room lookup each time through the cursor.
- Existing user overrides, resume positions, favorites, and watch-later state continue to be carried forward during reclassification.

### Next engineering targets

1. Move Movies filtering/sorting/strategy calculations out of the composable hot path.
2. Rework the Movies screen into a single virtualized scrolling surface instead of combining an outer scroll container with a nested lazy grid.
3. Separate playback telemetry from persistence/history updates.
4. Optimize Shorts player pooling and preloading around the currently visible window.
5. Reduce MainActivity orchestration responsibilities and centralize navigation/player state.
6. Add performance instrumentation and repeatable large-library stress checks.

### Validation note

The uploaded project does not contain `gradlew` or `gradle-wrapper.jar`, and the execution environment does not have a system Gradle installation. Therefore this checkpoint was statically reviewed but could not be compiled locally here. The repository's GitHub Actions workflow remains the build authority until the wrapper/toolchain is restored.

## Step 102E — Media Ingestion & Thumbnail Efficiency

- Prefer MediaStore-indexed video rotation metadata during library scans.
- Fall back to MediaMetadataRetriever only when indexed metadata is incomplete or rotation is unavailable.
- Keep thumbnail cache keys stable for the same file version while invalidating them when `dateModified` changes.
- Balance image memory and disk cache usage so large libraries do not crowd the application heap while thumbnails remain reusable.
- Preserve the existing classification and OEM/downloader compatibility fallback.

## Step 103Q — Lifecycle-Aware UI & Collection Scalability

- Compose Flow collection now follows the lifecycle of the visible screen.
- Parameterized Room flows in Folder and Collections surfaces are remembered for stable keys instead of being recreated on every recomposition.
- Collection move controls use a precomputed URI-to-position index rather than performing a linear search for every visible row.
- A structural source audit caught and corrected an unmatched brace in `MoviesScreen.kt` that would prevent the source from compiling cleanly.

The focus remains on making the existing RVH feature set production-grade rather than layering additional features on top of unstable or unnecessarily expensive paths.
## Step 103S — Scanner Concurrency & Scoped-Storage Hardening

- Serialized MediaStore scans process-wide so startup permission scanning and manual rescans cannot race with each other.
- Replaced the legacy `MediaStore.DATA` folder lookup with scoped-storage-safe `RELATIVE_PATH` metadata.
- Preserved the classifier's folder signal while avoiding dependence on raw filesystem paths.
- Scanner failures now emit diagnostic warnings instead of being silently swallowed, while still protecting the library from destructive pruning after a failed query.
- Kept the existing successful-query-only pruning rule and all user-owned metadata preservation.

The scanner remains focused on the existing library feature: this pass improves correctness and modern Android compatibility without adding another ingestion system.

## Step 103Z — Release Shrinker & CI Hardening

- Added the missing app-specific `proguard-rules.pro` required by the minified release build.
- Kept only the two application-owned Android entry points that are instantiated from the manifest: `RvhVideoApp` and `PlaybackService`.
- Left Room and Media3 shrinker behavior to their dependency-provided consumer rules rather than adding broad keep rules that would unnecessarily increase the release APK.
- Upgraded GitHub Actions from debug-only verification to both `assembleDebug` and `assembleRelease`.
- Added release APK artifact publication so every successful CI run produces both debug and minified release outputs.
- Preserved the existing unit-test and test-report stages.

This checkpoint strengthens the delivery pipeline itself: release shrinking is now exercised continuously instead of remaining an unverified build variant.
