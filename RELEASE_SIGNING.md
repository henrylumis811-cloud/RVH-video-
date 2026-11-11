# RVH Video — Permanent Release Signing

RVH Video release APKs and AABs must be signed with the permanent RVH Video release identity. Release builds must never fall back to Android's debug key. The private keystore and passwords are not part of this repository or any source ZIP.

## Approved certificate

- Keystore type: JKS
- Alias: `rvh-video-release`
- Key algorithm: RSA 4096-bit
- Signature: SHA256withRSA
- SHA-256 certificate fingerprint: `E7:16:9D:86:B8:D1:0C:DD:5E:B7:BF:00:23:8D:FB:D6:5F:6E:6A:A1:DA:FE:87:F2:5D:B3:50:7E:F1:68:37:C7`
- Valid through: 27 March 2054

The generated `rvh-video-release.jks` and `KEY-CREDENTIALS.txt` are private owner-held files. Keep multiple encrypted offline backups. Never commit them, upload them as build artifacts, or send them through chat. Loss of this key can prevent installation of future updates over existing direct-download installations.

## Local signed APK build

Requirements: JDK 17, Android SDK (API 37 and build-tools 37.0.0), and Gradle 9.6.0 (or the project wrapper if one is added).

Run from the project root:

```bash
./tools/build-signed-release.sh
```

The script asks for the keystore path and prompts for passwords without echoing them. It verifies the keystore fingerprint before building, builds `assembleRelease`, verifies the resulting APK signature against the approved fingerprint, and writes the APK, signature report and SHA-256 file under `dist/`.

The equivalent environment variables are `RVH_UPLOAD_STORE_FILE`, `RVH_UPLOAD_STORE_PASSWORD`, `RVH_UPLOAD_KEY_ALIAS`, and `RVH_UPLOAD_KEY_PASSWORD`. Do not put passwords in shell history or source control.

## GitHub Actions signed release

Configure these repository Actions secrets:

1. `RVH_RELEASE_KEYSTORE_BASE64` — Base64 encoding of the binary `.jks` file, with no line breaks required.
2. `RVH_UPLOAD_STORE_PASSWORD` — keystore password.
3. `RVH_UPLOAD_KEY_ALIAS` — `rvh-video-release`.
4. `RVH_UPLOAD_KEY_PASSWORD` — private-key password.

Create the Base64 value locally (do not commit the output):

```bash
base64 -w 0 /secure/path/rvh-video-release.jks
```

On PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes('C:\secure\rvh-video-release.jks'))
```

The workflow rejects missing secrets and rejects a keystore whose certificate fingerprint differs from the approved RVH identity. It builds the optimized release APK, verifies its signature, records SHA-256, and publishes the signed APK from `main`. Play Store AAB generation and listing work are intentionally deferred until at least three RVH apps are ready. The private keystore is decoded only into the ephemeral GitHub runner's temporary directory and is not uploaded as an artifact.

## Release acceptance checklist

- [ ] GitHub secrets configured by the owner, or local signing credentials entered locally.
- [ ] CI/local Gradle release build succeeds.
- [ ] `apksigner verify --verbose --print-certs` succeeds.
- [ ] APK signer SHA-256 equals the approved fingerprint above.
- [ ] APK installs on a clean Android 8.0+ device, including API 26/27 where available.
- [ ] APK installs as an update over a prior build signed with this same key.
- [ ] Core playback, permissions, background audio, PiP, gestures, library scanning and support links pass device regression.
- [ ] APK SHA-256 is published alongside the exact tested APK.

A generated keystore is not itself a release. Do not describe the app as a verified downloadable release until the signed APK has been built, signature-checked, installed and device-tested.
