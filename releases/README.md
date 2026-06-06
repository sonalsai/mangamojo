# MangaMojo Downloads

Signed, installable Android APKs, organized by version. Click a version to download
the APK directly. Each release ships a matching `.sha256` so you can verify the download.

| Version | Date | Size | Min Android | Notes |
| --- | --- | --- | --- | --- |
| [**v1.1.0**](v1.1.0/mangamojo-v1.1.0.apk) | 2026-06-06 | 13.4 MB | 8.0 (API 26) | Current latest release |
| [**v1.0.0**](v1.0.0/mangamojo-v1.0.0.apk) | 2026-06-03 | 13.2 MB | 8.0 (API 26) | First release - Phase 1 MVP |

> Future Phase 1 updates are **patch** releases (`v1.1.1`, `v1.1.2`, ...); each gets its own
> folder here. See [`../docs/CHANGELOG.md`](../docs/CHANGELOG.md).

---

## Installing the APK

1. Copy the `.apk` to your Android device (or download it directly on the device).
2. Open it with a file manager and tap **Install**. You may need to allow
   *"Install unknown apps"* for that app/browser in **Settings > Apps > Special access**.
3. The build is signed with the project release key (APK Signature Scheme v2), so it
   installs and updates cleanly on Android 8.0+.

Or install over ADB from a computer:

```bash
adb install -r releases/v1.1.0/mangamojo-v1.1.0.apk
```

## Verifying a download

**Windows (PowerShell):**
```powershell
(Get-FileHash releases\v1.1.0\mangamojo-v1.1.0.apk -Algorithm SHA256).Hash
# compare against the value in mangamojo-v1.1.0.apk.sha256
```

**macOS / Linux:**
```bash
shasum -a 256 -c releases/v1.1.0/mangamojo-v1.1.0.apk.sha256
```

### v1.1.0 checksum
```
SHA-256: 5fdb3863d899774804aa73ca12cf0bc26c3a2c76b0a2ae22ef09a0687c1a31da
```

---

## How these are built

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk -> copied here as mangamojo-v1.1.0.apk
```

Release signing reads from `keystore.properties` at the repo root (gitignored, along with
the `.jks` keystore - secrets are never committed). To produce signed builds on another
machine, recreate `keystore.properties` with your own keystore, or generate one with:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
  -keystore mangamojo-release.jks -alias mangamojo -keyalg RSA -keysize 2048 -validity 10000
```

> Note: this is a self-signed sideload/distribution key, not a Google Play upload key. For a
> Play Store release, enroll in Play App Signing and keep the upload key private.
