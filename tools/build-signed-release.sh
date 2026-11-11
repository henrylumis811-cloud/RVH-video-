#!/usr/bin/env bash
# Build and verify an RVH Video APK using the permanent RVH release identity.
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
EXPECTED_FINGERPRINT="E7169D86B8D10CDD5EB7BF00238DFBD65F6E6AA1DAFE87F25DB3507EF16837C7"

if [[ -z "${RVH_UPLOAD_STORE_FILE:-}" ]]; then
  read -r -p "Path to rvh-video-release.jks: " RVH_UPLOAD_STORE_FILE
fi
if [[ ! -f "$RVH_UPLOAD_STORE_FILE" ]]; then
  echo "Keystore file not found." >&2
  exit 1
fi
RVH_UPLOAD_STORE_FILE="$(realpath "$RVH_UPLOAD_STORE_FILE")"
export RVH_UPLOAD_STORE_FILE
export RVH_UPLOAD_KEY_ALIAS="${RVH_UPLOAD_KEY_ALIAS:-rvh-video-release}"
if [[ -z "${RVH_UPLOAD_STORE_PASSWORD:-}" ]]; then
  read -r -s -p "Keystore password: " RVH_UPLOAD_STORE_PASSWORD; echo
fi
if [[ -z "${RVH_UPLOAD_KEY_PASSWORD:-}" ]]; then
  read -r -s -p "Key password: " RVH_UPLOAD_KEY_PASSWORD; echo
fi
export RVH_UPLOAD_STORE_PASSWORD RVH_UPLOAD_KEY_PASSWORD RVH_UPLOAD_KEY_ALIAS

if [[ -x ./gradlew ]]; then
  GRADLE=(./gradlew)
elif command -v gradle >/dev/null 2>&1; then
  GRADLE=(gradle)
else
  echo "Gradle is required. Install Gradle or use the GitHub Actions signed-release workflow." >&2
  exit 1
fi

SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "$SDK_ROOT" || ! -d "$SDK_ROOT/build-tools" ]]; then
  echo "Set ANDROID_SDK_ROOT to an installed Android SDK." >&2
  exit 1
fi
APKSIGNER="$(find "$SDK_ROOT/build-tools" -type f -name apksigner | sort -V | tail -n 1)"
if [[ -z "$APKSIGNER" || ! -x "$APKSIGNER" ]]; then
  echo "Android build-tools apksigner was not found." >&2
  exit 1
fi

# Check the key before spending time on the Android build.
ACTUAL_KEY_FINGERPRINT="$(keytool -list -v -keystore "$RVH_UPLOAD_STORE_FILE" \
  -storepass:env RVH_UPLOAD_STORE_PASSWORD -alias "$RVH_UPLOAD_KEY_ALIAS" \
  | awk -F': ' '/SHA256:/{print $2; exit}' | tr -d ':' | tr '[:lower:]' '[:upper:]')"
if [[ "$ACTUAL_KEY_FINGERPRINT" != "$EXPECTED_FINGERPRINT" ]]; then
  echo "The supplied keystore is not the approved RVH Video release identity." >&2
  exit 1
fi

"${GRADLE[@]}" --no-daemon --stacktrace clean assembleRelease
SOURCE_APK="$(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' -print -quit)"
if [[ -z "$SOURCE_APK" ]]; then
  echo "Gradle completed but no release APK was found." >&2
  exit 1
fi
mkdir -p dist
cp "$SOURCE_APK" dist/RVH-Video-1.0-release.apk
"$APKSIGNER" verify --verbose --print-certs dist/RVH-Video-1.0-release.apk | tee dist/apk-signature-verification.txt
ACTUAL_APK_FINGERPRINT="$(awk -F': ' '/Signer #1 certificate SHA-256 digest:/{print $2; exit}' \
  dist/apk-signature-verification.txt | tr -d ':' | tr '[:lower:]' '[:upper:]')"
if [[ "$ACTUAL_APK_FINGERPRINT" != "$EXPECTED_FINGERPRINT" ]]; then
  echo "APK signature does not match the permanent RVH Video release identity." >&2
  exit 1
fi
sha256sum dist/RVH-Video-1.0-release.apk | tee dist/RVH-Video-1.0-release.apk.sha256
echo "Signed APK ready: $ROOT/dist/RVH-Video-1.0-release.apk"
