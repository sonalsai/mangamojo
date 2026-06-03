# MangaMojo — Downloads

Signed, installable Android APKs, organized by version. Pick a version folder and download
the `.apk`. Each release ships a matching `.sha256` so you can verify the download.

| Version | Date | APK | Size | Min Android | Notes |
| --- | --- | --- | --- | --- | --- |
| **v1.0.0** | 2026-06-03 | [`mangamojo-v1.0.0.apk`](v1.0.0/mangamojo-v1.0.0.apk) | 13.2 MB | 8.0 (API 26) | First release — Phase 1 MVP (MangaDex) |

> Future Phase 1 updates are **patch** releases (`v1.0.1`, `v1.0.2`, …); each gets its own
> folder here. See [`../docs/CHANGELOG.md`](../docs/CHANGELOG.md).

---

## Installing the APK

1. Copy the `.apk` to your Android device (or download it directly on the device).
2. Open it with a file manager and tap **Install**. You may need to allow
   *"Install unknown apps"* for that app/browser in **Settings → Apps → Special access**.
3. The build is signed with the project release key (APK Signature Scheme v2), so it
   installs and updates cleanly on Android 8.0+.

Or install over ADB from a computer:

```bash
adb install -r releases/v1.0.0/mangamojo-v1.0.0.apk
```

## Verifying a download

**Windows (PowerShell):**
```powershell
(Get-FileHash releases\v1.0.0\mangamojo-v1.0.0.apk -Algorithm SHA256).Hash
# compare against the value in mangamojo-v1.0.0.apk.sha256
```

**macOS / Linux:**
```bash
shasum -a 256 -c releases/v1.0.0/mangamojo-v1.0.0.apk.sha256
```

### v1.0.0 checksum
```
SHA-256: 64d1d95b3d06b8d14039859bc9e2b747090f647f6b71f71f0fbae7edc6f0af8a
```

---

## How these are built

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk  → copied here as mangamojo-v1.0.0.apk
```

Release signing reads from `keystore.properties` at the repo root (gitignored, along with
the `.jks` keystore — secrets are never committed). To produce signed builds on another
machine, recreate `keystore.properties` with your own keystore, or generate one with:

```powershell
& "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
  -keystore mangamojo-release.jks -alias mangamojo -keyalg RSA -keysize 2048 -validity 10000
```

> Note: this is a self-signed sideload/distribution key, not a Google Play upload key. For a
> Play Store release, enroll in Play App Signing and keep the upload key private.
