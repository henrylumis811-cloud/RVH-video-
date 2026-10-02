# RVH Video

### Your media. Your world.

RVH Video is a premium Android media experience built around a simple idea: **local media should feel as polished, intelligent, and immersive as a flagship streaming product — without requiring a cloud account or sending a user's library anywhere.**

The project started as a local video player and evolved into a complete personal-media platform with intelligent discovery, persistent library organization, cinematic playback, gesture control, personalized recommendations, and an automotive-inspired interface.

> **Current milestone:** Feature 033 — Media Contrast & Shorts Resume Choice

---

## Product Vision

RVH Video is designed to make a device's own media library feel like a dedicated entertainment system.

The experience combines:

- Fast local media discovery
- Persistent personal organization
- Intelligent recommendations
- Resume-aware playback
- Movies, Music Videos, and Shorts experiences
- Collections, Favorites, and Watch Later
- Search and folder exploration
- Cinematic full-screen playback
- Picture-in-Picture support
- Gesture-based brightness, volume, and seeking
- A distinctive automotive-inspired visual identity
- Offline-first behavior for the core library experience

The long-term goal is not simply to reproduce a conventional video player. RVH Video is being engineered as a **personal media cockpit**: responsive, visually distinctive, intelligent, and deeply integrated with the user's own library.

---

## From Foundation to Flagship

RVH Video has been developed incrementally through a long sequence of focused milestones. Each stage expanded the product while preserving the core local-media architecture.

### Foundation — The Media Engine

The first stage established the core RVH experience:

- Android application foundation
- Local MediaStore video discovery
- Automatic media classification
- Movies, Music Videos, and Shorts sections
- Shared playback infrastructure
- Home experience and navigation
- Persistent local library state
- Room-backed data architecture
- Media3/ExoPlayer playback
- Compose-based interface

This created the foundation on which every later RVH system was built.

### Legendary 6–14 — Personal Library

RVH evolved from a player into a personal media library.

**Favorites** introduced persistent user curation. **Collections** added custom shelves and organization. **Watch Later** introduced another persistent intent state. **Playback Intelligence** added history, completion tracking, recent playback, and resume behavior.

The player then gained:

- Persistent resume positions
- Playback-speed restoration
- Improved session checkpointing
- Double-tap seeking
- Brightness and volume gestures
- Control locking
- Scale modes
- Rotation controls
- Mini-player seeking
- Playback error recovery

**Media Details** and **Discovery & Search** followed, giving users a unified way to inspect, search, filter, sort, and launch local media.

### Legendary 15–25 — Brand & Experience

The product received its visual identity and began moving beyond conventional media-app patterns.

Key additions included:

- Flagship visual system
- Persistent appearance settings
- Offline For You recommendations
- Folder exploration
- RVH launch/brand experience
- Cinematic automotive artwork
- Automotive hero experience
- Live library cockpit
- Garage media-library identity
- Resume-aware media cards
- Ignition playback transition

The automotive identity became more than decoration: it became the visual language for the product's operational surfaces.

### Legendary 26–40 — The RVH Cockpit

The Garage evolved into an operational media cockpit.

The product introduced a family of local intelligence surfaces inspired by automotive command systems:

- Overdrive
- Launch Control
- Pit Lane
- Race Control
- Live Telemetry
- Performance Mode
- Pit Wall
- Race Strategy
- Race Engineer
- Race Director
- Race Strategy Board
- Race Command Deck
- Race Control Tower
- Smart Queue

These systems use existing local library signals — playback progress, Favorites, Watch Later, freshness, completion, and active sessions — to make the media library more actionable without requiring a remote recommendation service.

### Legendary 44–65 — Command & Intelligence

The next generation consolidated the cockpit into more deliberate command surfaces:

- Collection Command Center
- Universal Quick Actions
- Global Media Command Bar
- Media Focus Mode
- RVH Media Command Center
- Command Center 2.0
- Live Session Control
- Race Strategy AI
- Personal Drive
- Mission Control
- Mission Sequence
- Mission Control 3.0

The recommendation system became increasingly adaptive while remaining local and deterministic. RVH could select a next media target based on current activity, resume momentum, user intent, favorites, and freshness.

### Legendary 67–80 — Cinematic Garage

The Garage received a deeper visual and interaction pass:

- RVH Command Deck
- Mission Timeline
- Race Dashboard
- Race Dashboard 2.0
- Cinematic Garage Flow
- Garage Command HUD
- Command Communication
- Garage System Sync
- Cinematic Mission Focus
- Final Garage Polish

This phase focused on turning a collection of individual features into a coherent operational experience.

### Legendary 81–82 — Engineering Hardening

The project gained a reproducible Android CI workflow and underwent a compiler-hardening pass across the application.

The engineering focus included:

- Automated Android builds
- Unit-test execution in CI
- Kotlin/Compose compiler cleanup
- Media3 API corrections
- Navigation and callback repairs
- UI compilation fixes
- Safer SDK compatibility handling

### Legendary 96–100 — Immersive Playback

The playback experience received its most direct interaction work:

- Gesture-based brightness control
- Gesture-based volume control
- Double-tap play/pause
- Edge double-tap seeking
- Immersive full-screen playback
- Hidden system chrome during cinematic playback
- Cleaner Picture-in-Picture presentation
- Direct Shorts launching from recommendation surfaces
- Curated Garage intelligence
- Deduplicated Home recommendations
- Category-aware playback routing

The result is a player designed to feel closer to a dedicated media device than a conventional Android screen.

### Legendary 103B–103P — Playback Platform Hardening

The playback system was audited as a production Android subsystem rather than treated as a collection of screen-level controls. The resulting architecture now centers playback ownership in a dedicated Media3 service and exposes it to the UI through a controller boundary.

Key hardening work includes:

- Dedicated `PlaybackService` ownership of the Media3 player and `MediaSession`
- `PlaybackClient` as the UI/ViewModel playback boundary
- Android playback resumption using the persisted local session snapshot
- Notification/session return routing back into the correct RVH playback context
- Unified Picture-in-Picture behavior across Movies and Music
- Device audio-focus and noisy-route handling in the shared playback layer
- Controller authorization with explicit trusted/untrusted command surfaces
- MediaController disconnect detection and connection-state recovery
- Stable playback metadata for system surfaces and resumption
- Recovery that preserves the failed item's position and playback speed
- Service lifecycle hardening for task removal and background playback
- Buffering and back-buffer tuning appropriate for local playback
- Lightweight opt-in playback diagnostics without a continuous logging tax
- Lifecycle-aware Compose collection so inactive screens stop consuming live UI flows
- Stable parameterized media flows and reduced per-row collection work

The important architectural invariant is now:

```text
UI / ViewModels → MediaController → PlaybackService → ExoPlayer
```

No screen owns a second playback engine, and system playback surfaces reuse the same service-owned session.

### Legendary 103Q — UI Lifecycle & Large-Library Polish

This pass tightened the boundaries around the existing feature set rather than adding another feature layer. Compose state collection now follows the screen lifecycle, parameterized Room flows are remembered for stable keys, and collection row actions no longer perform repeated linear searches through the same list.

The goal is simple: when RVH is not visible, it should not behave as though every screen is still on stage — and when a large collection is visible, per-row work should remain predictable.

### Legendary 103R–103Z — Production Reliability & Release Hardening

This sequence moved RVH from performance refinement into production-oriented reliability work without adding unnecessary feature complexity.

Highlights include:

- Atomic collection deletion and bulk collection writes
- Scanner concurrency protection and scoped-storage-safe MediaStore access
- Playback-history durability with atomic SQL updates
- Bounded scanner pruning for large libraries
- Protection against scanner/user-state races
- Transactional cleanup when media disappears
- Scanner busy-state recovery after failures
- Lifecycle-owned scanner coroutines
- Release R8/shrinker configuration
- CI verification of both Debug and minified Release builds
- Release APK artifact publication for every successful build

The guiding principle was to make existing behavior safer under real lifecycle, concurrency, storage, and release-build conditions rather than simply adding more features.

### Legendary 104A–104D — Database, Manifest & Schema Hardening

The Room layer was hardened for large libraries, manifest permissions were cleaned up, and schema traceability was made explicit with versioned Room snapshots.

The database work covers:

- Folder browsing ordered by modification time
- Favorites ordered by modification time
- Watch Later ordered by modification time
- Continue Watching ordered by modification time
- Ordered collection-item lookup by collection and position
- Collection identity and media-URI lookup paths
- Versioned Room schema snapshots for migration traceability

This keeps filtering and ordering work closer to SQLite's indexed query path instead of relying on increasingly expensive full-table scans as a user's local library grows.

Manifest permissions were also tightened without changing the core media-access model. No broad schema redesign was introduced: the existing local-media model remains intact.

### Legendary 102A–102K — Performance & Architecture Overhaul

The current phase moves the project from feature accumulation toward **engineering excellence**.

The first performance pass targets the hottest paths in the application:

- Reduced unnecessary playback persistence I/O
- Separated responsive playback-state publishing from persistence work
- Throttled checkpoint writes while retaining explicit saves for important lifecycle actions
- Reduced repeated database lookups during media scanning
- Reused in-memory indexes for classification-related scanner work
- Began restructuring large UI/data flows so expensive collection processing can leave the Compose hot path

The resulting architecture separates expensive work from the Compose hot path, isolates global playback state from unrelated screens, and keeps the UI responsive as the local library grows.

---

## Core Architecture

```text
                    ┌──────────────────────────┐
                    │       Android UI         │
                    │   Jetpack Compose        │
                    └────────────┬─────────────┘
                                 │
                         ViewModels / State
                                 │
              ┌──────────────────┴──────────────────┐
              │                                     │
       Library / Discovery                    Playback Layer
              │                                     │
       ┌──────┴──────┐                       ┌──────┴──────┐
       │ MediaStore  │                       │   Media3    │
       │   Scanner   │                       │  ExoPlayer  │
       └──────┬──────┘                       └──────┬──────┘
              │                                     │
              └──────────────┬──────────────────────┘
                             │
                       Repository Layer
                             │
                    ┌────────┴────────┐
                    │      Room       │
                    │ Local Library   │
                    │ User State      │
                    │ Playback State  │
                    └─────────────────┘
```

### Primary technologies

- **Kotlin**
- **Jetpack Compose**
- **Material 3**
- **Android Media3 / ExoPlayer**
- **Room**
- **KSP**
- **Paging**
- **Coil + video frame decoding**
- **Android MediaStore**
- **Jetpack Navigation**
- **GitHub Actions**

### Platform target

- Minimum SDK: **Android 11 / API 30**
- Target SDK: **Android 17 / API 37**
- JVM: **Java 17**

---

## Intelligent Local Media

RVH intentionally keeps its core intelligence close to the device.

Recommendation and discovery signals can be derived from:

- Playback progress
- Resume state
- Completion state
- Favorites
- Watch Later
- Playback history
- Play counts
- Media freshness
- Folder relationships
- Current sessions

This allows the product to provide a personalized experience without requiring a cloud recommendation backend for its core behavior.

---

## Playback System

Playback is built around a shared Media3 architecture rather than independent player implementations for every screen.

The system supports:

- Resume playback
- Persistent playback speed
- Playback history
- Completion tracking
- Mini-player behavior
- Picture-in-Picture
- Full-screen immersive mode
- Double-tap seeking
- Gesture brightness
- Gesture volume
- Control locking
- Scale modes
- Rotation control
- Playback error recovery
- Shorts player pooling

The current performance phase is specifically reducing work performed during active playback so the player remains responsive under real-world use.

---

## Design Language

RVH uses an automotive-inspired cinematic identity built around:

- Dark glass surfaces
- Strong visual hierarchy
- Teal/cyan system accents
- Cinematic imagery
- Subtle motion
- Operational dashboard patterns
- Compact telemetry
- Clear primary commands
- Full-screen media presentation

The goal is **premium without unnecessary visual noise**: information should be useful, controls should feel intentional, and playback should remain the visual priority.

---

## Performance Philosophy

Performance is now a first-class product requirement rather than a finishing step.

The engineering direction is centered on:

1. **Fast startup** — minimize work before the first useful frame.
2. **Smooth interaction** — keep expensive computation away from UI hot paths.
3. **Efficient playback** — keep player state responsive and persistence controlled.
4. **Large-library scalability** — avoid repeatedly processing the entire library in composables.
5. **Memory discipline** — control player, thumbnail, and media-object lifecycles.
6. **Predictable state** — give each state source a clear owner.
7. **Graceful recovery** — media failures should recover without destabilizing the application.
8. **Measurable progress** — performance improvements should be validated against realistic workloads.

---

## Current Engineering Priorities

The next development stages are focused on strengthening the foundations rather than simply increasing the feature count.

### 1. Library Engine

- Incremental MediaStore indexing
- Efficient metadata extraction
- Better large-library behavior
- Reduced duplicate transformations
- Smarter thumbnail caching

### 2. Compose Performance

- Move expensive filtering/sorting out of composables
- Reduce unnecessary recomposition
- Simplify oversized screens
- Establish clearer state ownership
- Improve navigation state handling

### 3. Playback Engine

- Refine player lifecycle management
- Improve preloading and buffering
- Separate telemetry from persistence
- Optimize Shorts player reuse
- Strengthen recovery paths

### 4. Reliability

- Stronger lifecycle handling
- Process-death recovery
- More deterministic state restoration
- Expanded automated testing
- Stress testing on large libraries and constrained devices

### 5. Product Polish

- Faster perceived interactions
- More deliberate motion
- Cleaner command surfaces
- Consistent empty/loading/error states
- Continued refinement of the RVH visual identity

---

## Project Structure

```text
app/src/main/java/com/rvh/video/
├── data/
│   ├── classification/      # Media discovery and classification
│   ├── local/               # Room, settings and repositories
│   └── model/               # Persistent media models
├── player/                  # Shared playback and PiP infrastructure
├── ui/
│   ├── home/                # Home, discovery and folders
│   ├── movies/              # Garage and movie playback
│   ├── music/               # Music-video experience
│   ├── shorts/              # Shorts experience and player pool
│   ├── collections/         # Personal collections
│   ├── details/             # Media details
│   ├── search/              # Unified search
│   ├── profile/             # Settings and personalization
│   ├── components/          # Shared UI/player components
│   ├── splash/              # RVH launch experience
│   └── theme/               # Visual system
└── RvhVideoApp.kt            # Application entry point
```

---

## Engineering Principles

RVH Video is being built around a few principles:

**Local-first**  
The user's media library belongs to the user.

**Performance is a feature**  
A beautiful interface that feels slow is not a premium experience.

**Intelligence should be useful**  
Recommendations and automation should reduce friction rather than add complexity.

**Playback comes first**  
Every library feature ultimately exists to help the user find and enjoy media.

**Architecture must scale with ambition**  
As the product grows, responsibilities must become clearer, not more tangled.

**Design and engineering work together**  
The visual identity should be backed by responsive, reliable engineering.

---

## Roadmap

RVH Video is actively evolving toward a stronger release-quality foundation.

**Completed:**

- Local media discovery and classification
- Movies / Music Videos / Shorts
- Persistent library state
- Favorites
- Watch Later
- Collections
- Playback history and resume
- Search and discovery
- For You recommendations
- Folder exploration
- Cinematic automotive identity
- Garage command experience
- Intelligent local queues
- Immersive player gestures
- Picture-in-Picture
- Android CI workflow
- Initial performance overhaul
- Production playback service and MediaSession architecture
- Durable playback queues and collection playback
- Native audio/subtitle track selection with per-media preferences
- Production seek scrubbing
- Brightness and volume gesture correction
- Orientation lifecycle hardening
- True 16:9 and 4:3 video viewports
- Shorts lifecycle continuity
- Collection create-and-save transaction flow
- Clean fullscreen player presentation
- Player controls and orientation redesign

**Current engineering focus:**

- GitHub CI build verification
- Release validation on real Android hardware
- Targeted polish only where testing reveals a concrete product issue

**Vision:**

A fast, intelligent, beautiful personal-media platform that makes a local Android library feel like a flagship entertainment system.

---

## Status

**RVH Video — Active Development**  
**Current milestone — Feature 018: Player Controls & Orientation Redesign**  
**Focus — Build verification, real-device validation, and release-quality product polish**

---

## License

This project is currently maintained as a private/proprietary project. Licensing and distribution terms may change as development progresses.


### 104A — Database Query Scalability
- Added Room migration 5→6 for targeted large-library indexes.
- Added a composite `collection_items(collectionId, position)` index so ordered collection reads match the database access pattern directly.
- Kept collection identity and video URI indexes intact; no data migration or behavioral change.
- Release and debug build workflows remain enabled.

## 104D — Database Schema Traceability

Room schema export is enabled and versioned schema snapshots are generated into `schemas/` during builds.


### Feature 006 — Durable Playback Queue
- Persisted the service-owned playback queue across process death, including queue position and playback mode.
- Restores the queue alongside Android MediaSession playback resumption instead of collapsing the session to a single media item.
- Queue navigation and mode changes update the durable session state.
- Clearing or replacing playback removes stale queue state.
- Queue persistence is bounded to a practical local-session limit to avoid turning SharedPreferences into an unbounded media database.

### Feature 004 — Truthful Shorts Engagement UI
- Removed generated like/comment counts that could be mistaken for real social engagement.
- Replaced those metrics with explicit local actions: Favorite, Comments, and Share.
- Marked the existing comment sheet as a local preview so generated comments are not presented as published user content.
- Removed fabricated timestamps/like counts from preview comments and cleaned the preview copy.
- Replaced generated captions with factual local media/category context.
- Keeps Shorts fully offline and avoids introducing a fake social backend or persistence layer.

### Feature 002 — Real Shorts Sharing
- Replaced the Shorts share placeholder with the native Android share sheet.
- Shares the original MediaStore/content URI instead of copying or transcoding the video.
- Grants temporary read access to compatible receiving apps.
- Includes the media title and a graceful fallback when no share target is available.
- Keeps sharing local to Android and does not introduce a backend or storage migration.

### Feature 001 — Legendary Search
- Reworked library search from basic substring filtering into ranked local discovery.
- Matches media titles, folders, and category names with token-aware matching.
- Exact and title-prefix matches receive stronger relevance weighting while the selected sort remains the deterministic tie-breaker.
- Added a short query debounce so rapid typing does not repeatedly trigger full-library ranking work.
- Added local recent-search history with an eight-entry cap, one-tap reuse, and clear-history control.
- Added a clear-search action plus dedicated empty-library and no-match states.
- Kept search fully offline and outside the Room schema, so the feature introduces no migration or playback-state coupling.

### Feature 008 — Native Audio & Subtitle Track Selection
- Added a real Media3-backed Audio & Subtitles control to the immersive Movie and Music Video players.
- Enumerates the tracks actually exposed by the current media instead of displaying fabricated language or quality options.
- Supports switching between available audio tracks and subtitle/text tracks, plus disabling subtitles.
- Respects the MediaController command surface so track changes are only attempted when the connected playback session permits them.
- Keeps the feature local to the existing service-owned ExoPlayer architecture with no database or backend changes.


### Device Polish Pass — First Real-Device Repair
- Repaired Home Resume so the Cockpit resume action opens the current unfinished item.
- Fixed the Movie player telemetry timeline so the filled segment follows actual playback progress.
- Removed the automotive shell background from Shorts while preserving it elsewhere in the app identity.
- Preserved per-folder browsing position when returning from playback.
- Added the approved cinematic RVH Video launcher artwork and reused the same artwork in the dedication screen.
- Reworked player control auto-hide around a fixed inactivity window, resetting the timer whenever the user interacts.
- Added explicit player rotation modes: AUTO, PORTRAIT, and LANDSCAPE.
- Replaced the system resize bottom sheet with an RVH-styled display-mode popup.
- Improved Movie thumbnail loading for smoother scrolling and added explicit Grid/List presentation controls.
- Added explicit Music List/Grid presentation controls and independent Movie/Music sorting preferences.
- Kept Music scrolling unchanged after real-device validation confirmed it is already smooth.


### Feature 021 — Media Details Command Center
- Media details now observe the selected library row live, so favorite, Watch Later, resume, and classification changes are reflected without manually reloading the screen.
- Added direct sharing from the details surface using the original MediaStore content URI with read permission.
- Added a real category-management dialog for Movies, Music Videos, and Shorts, including restoration to automatic classification.
- Unified the details surface with the RVH gold/translucent-black visual language.
- Removed unnecessary thumbnail crossfade and bounded the details-frame decode/cache keys for a lighter details experience.

### Feature 018 — Player Controls & Orientation Redesign
- Reworked the immersive player controls into a cleaner dark-cinematic presentation with restrained cool accents and reduced visual clutter.
- Removed the coupling between fullscreen playback and forced landscape orientation.
- Movie and Music players now open fullscreen while respecting the device's current orientation.
- Rotation is an explicit player control: automatic device orientation can be restored after a temporary lock.

### Feature 017 — Clean Fullscreen Players
- Removed the automotive hero/background artwork from the dedicated Movie and Music player surfaces.
- Kept the automotive visual identity in the broader app experience while giving video playback an uncluttered visual stage.
- Preserved immersive system-bar handling and player fullscreen behavior.

### Feature 016 — Collection Create & Save Flow
- Creating a collection from a video's Save-to-Collection flow now also saves that video immediately.
- Collection creation and first-item insertion are atomic.
- Existing collection creation and duplicate protections remain intact.

### Feature 015 — Shorts Lifecycle Continuity
- Shorts remember which pooled players were actively playing across app backgrounding.
- Returning to Shorts resumes the previously active item without restarting manually paused content.

### Feature 014 — True Video Scale Modes
- Replaced approximate 16:9 and 4:3 labels with real aspect-ratio viewports.
- Original and Full Screen retain their intended fit/crop behavior.
- Movie and Music Video players share the same scale-mode implementation.

### Feature 013 — Orientation State Hardening
- Orientation restoration now captures the current host Activity state correctly across Activity recreation.
- Prevents stale orientation state from a previous Activity instance being restored into a new player host.

### Feature 012 — Brightness & Gesture Correction
- Fixed continuous brightness and volume gesture tracking so movement no longer resets against the initial value on every drag event.
- Brightness changes remain scoped to the RVH Activity window and restore the previous window setting when the gesture overlay exits.

## Feature 011 — Seek Scrubbing

Player timeline scrubbing previews locally and commits one seek on release.

### Feature 025 — Search Contextual Media Actions
- Extended long-press actions in unified Search to use the same clean media command surface as Movies and Music.
- Removed the search-specific Favorite-only action dialog in favor of the shared contextual action surface.
- Search results now expose Favorite, Watch Later, collection, details, and category actions without adding visual controls to thumbnails.
- Preserved normal-tap playback/navigation and the existing cached, crossfade-free search thumbnails.

### Feature 022 — Garage Intelligence Popup
- Replaced the persistent Hide/Show Garage Intelligence section in Movies with a centered RVH command-center popup.
- Preserved the existing live fleet, strategy, and smart-queue intelligence using real local library/playback data.
- Added gold RVH command iconography, translucent-black surface, close action, and bounded internal scrolling.
- Popup dismisses before launching playback/details/focus actions where appropriate.


### Feature 026 — Music Search Polish
- Music search now matches titles, folders, and category names instead of titles alone.
- Added an inline clear-search action so a filtered Music library can be restored immediately.
- Added a lightweight filtered-result count without adding controls to media thumbnails.
- Kept filtering and sorting off the main thread through the existing `MusicViewModel` flow.
### Feature 027 — Global Mini Player Performance Polish

The global mini player was moved onto the shared reactive playback state, removing its independent position polling loop.

### Feature 028 — Background Playback Performance

The background playback bar now consumes the shared playback state instead of maintaining a second position polling loop.

### Feature 029 — Reactive Playback History

Playback history now reacts to the existing playback-state stream rather than waking periodically to query the player position.

### Feature 030 — Shorts Playback Intent Persistence

Shorts now remembers an explicit pause choice for each visited short. Swiping away and returning no longer silently overrides the user's pause decision; explicitly pressing play clears the pause override. The player pool still keeps a tightly bounded current-page/neighbor window to control memory and decoder pressure.

### Feature 028 — Background Playback Progress Performance — Global Mini Player Performance Polish
- Removed the mini-player's second 400 ms position polling loop.
- Progress is now derived directly from the application-scoped playback state already published by `PlayerManager`.
- Keeps the mini-player visually synchronized while reducing redundant coroutine wakeups and player-position reads.
- Preserves the existing controls, playback behavior, and clean media presentation.



## Feature 032 — Playback Lifecycle & Search Source Cleanup
- Removed duplicate Compose runtime imports from the Search surface.
- Hardened the PlayerManager ticker to respect coroutine cancellation explicitly.
- No functional change to playback controls or media presentation.


## Feature 032 — Search Result Compose Fix
- Fixed Compose context usage in search result thumbnail image request construction.
- Keeps `LocalContext.current` in the composable scope and reuses the captured context inside `remember`.


## Feature 033 — Media Contrast & Shorts Resume Choice

This milestone addresses two device-tested UX issues discovered after the Feature 032 release build:

- **Media header contrast:** Music and Movies top-bar titles and display/sort/grid actions now use explicit RVH text contrast so controls remain visible over the dark visual surfaces.
- **Shorts resume choice:** RVH remembers the last settled Short locally. When Shorts is reopened with a meaningful previous position, a compact prompt offers **Continue** or **Start Over**. Continue returns to the saved Short; Start Over resets the feed to the first Short.

The resume state is keyed by the Short's MediaStore URI, so it survives app restarts and remains robust when the library ordering changes.
