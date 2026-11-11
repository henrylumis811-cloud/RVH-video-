# RVH Video — Deferred Google Play Readiness

> **Status:** Deferred by product decision. Current priority is a properly signed, downloadable APK for direct installation. Play Store preparation will resume after at least three RVH apps are release-ready.

## Current release identity

- Application ID: `com.rvh.video` (treat as permanent once published)
- Display name: RVH Video
- Initial version: `1.0` (`versionCode` 1)
- Minimum Android version: Android 8.0 / API 26
- Target API: 37 (exceeds the current Google Play minimum of API 36 as of 31 August 2026)
- Current distribution format: signed Android APK for direct installation
- Future Play distribution: decide AAB and Play App Signing configuration after the three-app readiness milestone

## Signing and CI

The release build requires the permanent RVH Video release key supplied only through environment variables. It refuses to use the Android debug key for release packaging:

- `RVH_UPLOAD_STORE_FILE`
- `RVH_UPLOAD_STORE_PASSWORD`
- `RVH_UPLOAD_KEY_ALIAS`
- `RVH_UPLOAD_KEY_PASSWORD`

For GitHub Actions, configure these repository secrets:

- `RVH_RELEASE_KEYSTORE_BASE64` — base64-encoded RVH Video release JKS
- `RVH_UPLOAD_STORE_PASSWORD`
- `RVH_UPLOAD_KEY_ALIAS`
- `RVH_UPLOAD_KEY_PASSWORD`

The direct-download workflow decodes the keystore into the ephemeral runner's temporary directory, checks its certificate fingerprint against the approved RVH Video identity, builds the optimized APK, verifies the APK signature and publishes the APK with its SHA-256 checksum. If the secrets are missing or the key fingerprint differs, release packaging fails. No debug-key fallback is allowed.

Never commit the keystore, passwords, signing properties, or decoded key. Keep at least two secure backups of the upload keystore and its recovery details.

## Remaining pre-submission work

- [ ] After at least three RVH apps are release-ready, configure the Play Console path and determine whether to register this identity as the app-signing key or use a distinct upload key.
- [ ] Run a successful release build and unit tests; inspect R8/minification output.
- [ ] Complete on-device regression on supported Android 8.0+ devices, including Android 8.0 (API 26) and 8.1 (API 27), permission denial, background playback, PiP, rotation, and process death.
- [ ] Create and publish a privacy policy that accurately describes local media access and the external email/WhatsApp/Telegram actions initiated by the user.
- [ ] Complete Play Console Data safety, content rating, target audience, app access, ads, and other declarations truthfully.
- [ ] Prepare store listing: short description, full description, 512×512 icon, feature graphic, phone screenshots, and localized assets as needed.
- [ ] Create the Play Console app, enable Play App Signing, upload the signed AAB to internal testing, and validate generated APKs and pre-launch report (deferred).
- [ ] Complete any required testing track and account-specific production-access steps shown in Play Console.
- [ ] Review final release notes, version code, support contact, and rollout settings before production submission.

## Product/privacy facts to validate before writing public declarations

RVH Video is designed as an offline-first local media player. Its core library and playback features operate on media selected/accessed on the device. The app includes explicit user-initiated external contact actions for feedback and community links. Confirm the exact behavior of every SDK and dependency, including any diagnostics, before finalizing the Data safety form and privacy policy. Do not claim that no data is ever shared without verifying the complete release build and its dependencies.

## Release gate

Do not label the direct-download APK `1.0 production-ready` until it has been built with the permanent key, its signer fingerprint and SHA-256 have been verified, and it has passed installation and device regression. Play Store readiness is a separate later gate.
