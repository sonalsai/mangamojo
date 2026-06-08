# MangaMojo Documentation

**MangaMojo** is a local-first manga reader for Android with a clean, fast reader UI.
It is built as a *reader UI on top of a pluggable source layer*. **Current release (v1.1.0)**
ships a fully working end-to-end flow:

> search -> details -> chapter list -> reader -> save progress -> favorites / history

No accounts, no backend, no cloud sync. Everything the user owns (favorites, history,
bookmarks, reading progress, cached metadata) lives on-device.

Phase 2 development has started on the main codebase: search can now fan out across a
provider set and dedupe results into one catalog. MangaDex remains the canonical API
source, while MangaKakalot and MangaReader are configurable supplements for titles
whose MangaDex chapter feed is missing or incomplete.

---

## Documentation index

| Document | What's inside |
| --- | --- |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Layered/clean architecture, package map, data flow, provider abstraction, DI graph, persistence schema, networking, caching, background sync, reader internals |
| [DATA_MODEL.md](DATA_MODEL.md) | Domain models, Room entities & DAOs, settings keys |
| [CHANGELOG.md](CHANGELOG.md) | Release history, latest tag, and versioning policy |
| [ROADMAP.md](ROADMAP.md) | Phase 2 plan - multi-provider, merging, fallback, paged reader |

---

## Feature summary (v1.1.0)

- **Home** - Library preview rail plus a **Popular / Latest** discovery feed (chip-pill tabs) shown as a vertical grid that paginates as you scroll.
- **Search** - Debounced search with an infinite-scroll grid and loading/empty/error states.
- **Details** - Cover, title, authors/artists, status, description, tags, favorite toggle, and the full chapter list with read indicators. External (off-site) chapters open in the browser.
- **Reader** - Vertical (webtoon) reader with nearby-page preloading, resume-from-last-page, next/previous chapter navigation, and automatic progress saving.
- **Library / History** - Reactive favorites and reading history, each clearable.
- **Settings** - Theme mode, color theme, reading-direction placeholder, data saver, content ratings, and clear cache/history/favorites.
- **Background** - WorkManager jobs for periodic cache cleanup and favorites refresh.

---

## Tech stack

| Concern | Choice |
| --- | --- |
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose (Material 3) |
| DI | Hilt 2.59.2 |
| Local storage | Room 2.8.4 + DataStore (Preferences) 1.2.1 |
| Async | Coroutines 1.10.2 + Flow |
| UI state | ViewModel + StateFlow |
| Networking | Retrofit 3.0.0 + OkHttp 5.3.2 + kotlinx-serialization 1.9.0 |
| Images | Coil 3.4.0 (shares the app OkHttp client) |
| Background | WorkManager 2.11.2 |
| Navigation | Navigation Compose 2.9.8 |

**Build toolchain:** AGP 9.1.1, Gradle 9.3.1, KSP 2.2.10-2.0.2, compileSdk 36.1, minSdk 26, targetSdk 36.

---

## Building & running

`java` is not assumed on `PATH`; use the JDK bundled with Android Studio (JBR, JDK 21):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug          # build the debug APK
.\gradlew.bat :app:installDebug           # install to a connected device/emulator
```

Output APK: `app/build/outputs/apk/debug/app-debug.apk`.

> **AGP 9 note:** this project uses AGP's *built-in Kotlin* (no separate `kotlin-android`
> plugin). KSP (Room/Hilt code generation) requires
> `android.disallowKotlinSourceSets=false` in `gradle.properties` - already set.

---

## Attribution

Please support scanlation groups and original creators.
