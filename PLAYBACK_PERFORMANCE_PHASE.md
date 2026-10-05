# Playback Performance Phase

RVH Video treats playback as a shared resource rather than a screen-local implementation.

## Current direction

- A single application-level player manager owns normal movie/music playback.
- Player resources can be released and recreated without killing the manager lifecycle.
- Buffering is bounded to keep playback responsive on memory-constrained devices.
- Resume state is persisted in coarse intervals rather than on every progress tick.
- Playback failures expose a centralized retry path that preserves the current media position.
- Lightweight diagnostics are available through `PlayerManager.snapshot()` for future performance tooling without adding continuous logging overhead.
- Shorts use a bounded player pool so neighboring content can be prepared without keeping the entire feed decoded.

## Engineering principle

Measure playback behavior at the resource boundary first. The app should remain fast during ordinary playback, while diagnostics remain opt-in and cheap enough to leave the core playback path clean.


## Step 103A — Device Audio Reliability

The shared application-level player now owns Android media-audio behavior consistently across Movies and Music.

- Media3 audio attributes use `USAGE_MEDIA`.
- Audio focus is handled by the player rather than by individual screens.
- Playback automatically pauses when an audio route becomes noisy, such as a headset being disconnected.
- No duplicate per-screen audio-focus implementation was introduced.

This complements the existing MediaSession, PiP, resume persistence, bounded buffering, retry recovery, and playback diagnostics already present in the project.

## 103D — Single-owner playback architecture

Playback is now split cleanly at the Android lifecycle boundary:

- `PlaybackService` owns the ExoPlayer instance and MediaSession.
- UI surfaces use `PlaybackClient`, backed by a Media3 `MediaController`.
- PlayerView instances receive the controller rather than reaching into the service-owned player.
- Playback listeners survive the asynchronous controller connection.
- Resume persistence remains authoritative inside the service-owned player.
- Stop/clear removes the active media item before clearing persisted playback state, preventing stale state from being recreated by delayed callbacks.

This keeps Movies, Music, mini-player surfaces, system controls, and background playback on one authoritative playback path.

## Step 103Q — Lifecycle Boundary Reinforcement

Playback state consumed by Compose now uses lifecycle-aware collection across the application. This keeps screen-side observation aligned with the visible lifecycle while preserving the service-owned playback architecture established in the 103B–103P series.
