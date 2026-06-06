# Changelog

All notable changes to MangaMojo are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Versioning policy

MangaMojo follows [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`).

- **v1.0.0** is the first release - the complete Phase 1 MVP.
- **v1.1.0** is the current latest release tag.
- **Subsequent updates to the current Phase 1 track are released as PATCH versions**
  (`1.1.1`, `1.1.2`, ...): bug fixes, performance and UI polish, dependency bumps, and
  small refinements that do not change the public app behavior in a breaking way.
- The next planned **MINOR** version (`1.2.0`) is reserved for **Phase 2** - multi-provider
  support (additional sources, result merging, fallback, source priority, deduplication).
  See [ROADMAP.md](ROADMAP.md).
- `versionName` in `app/build.gradle.kts` is the source of truth and currently reads
  `1.1.0` (`versionCode = 2`). Bump the patch digit and `versionCode` together for each
  patch release.

---

## [1.1.0] - 2026-06-06

Current latest release tag.

### Changed
- Bumped Android app metadata to `versionName = "1.1.0"` and `versionCode = 2`.
- Updated the main README and downloads index so users can click a version and download
  that APK directly.

## [1.0.0] - 2026-06-03

First release. A local-first manga reader for Android built on a clean, provider-pluggable architecture.

### Added

**App foundation**
- Jetpack Compose app shell with Material 3, bottom navigation, and a theme driven by the
  user's preference (system / light / dark).
- Hilt dependency injection across all layers; WorkManager wired through a Hilt worker
  factory; Coil image loader sharing the app's OkHttp client.
- Clean, layered architecture: `core`, `domain`, `data`, `providers`, `ui`, `reader`,
  `sync`, `di`.

**Source layer**
- `MangaProvider` abstraction with provider implementations.
- Retrofit + OkHttp client with timeouts, polite `User-Agent`, transient-failure retry,
  and kotlinx-serialization; DTO-to-domain mappers for provider integration.
- Robust JSON handling for provider-specific data format quirks.

**Features**
- **Home** - Library preview rail plus a **Popular / Latest** discovery feed shown as a
  vertical grid (chip-pill tabs; paginates as you scroll to the bottom).
- **Search** - Debounced search, infinite-scroll grid, loading/empty/error states.
- **Details** - Cover, title, authors/artists, status, tags, description, favorite toggle,
  and the de-duplicated chapter list with read indicators; external chapters open in browser.
- **Reader** - Vertical (webtoon) reader with nearby-page preloading, resume-from-last-page,
  next/previous chapter navigation, and automatic, debounced progress saving.
- **Library & History** - Reactive favorites and reading history, individually clearable.
- **Settings** - Theme mode, reading-direction placeholder, data saver, content ratings,
  and clear cache / history / favorites.

**Local persistence**
- Room database with cached manga & chapters, favorites, history, reading progress, and a
  bookmarks table; DataStore for preferences.
- Read-through caching with offline fallback; cache freshness TTLs and history capping.

**Background**
- `CacheCleanupWorker` (daily) evicts stale cache while preserving favorites/history.
- `LibraryRefreshWorker` (twice daily, network-gated) refreshes favorites for offline use.

### Notes
- Phase 1 deliberately excludes accounts, cloud sync, and any backend. All user data is
  local-only.
- The `MangaProvider` contract evolved slightly from the original sketch: `getMangaDetails`
  returns the richer `MangaDetails`, `search` is typed/paginated via `SearchQuery` /
  `SearchResult`, and the provider exposes `id`/`name`. The single-source contract is
  unchanged.

[1.1.0]: #110---2026-06-06
[1.0.0]: #100---2026-06-03
