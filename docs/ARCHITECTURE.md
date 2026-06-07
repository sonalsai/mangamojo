# Architecture

MangaMojo follows a layered, **clean-architecture** approach. The guiding rule is that
dependencies point **inward**: UI depends on domain; data depends on domain; **domain
depends on nothing**. Everything source-specific (provider DTOs, endpoint quirks) is
isolated behind a provider interface so additional sources can be added later without
touching UI or domain code.

For Phase 1 this is a **single Gradle module** (`:app`) with strict *package*-level
layering. The boundaries are real (interfaces in `domain`, implementations in `data`),
so promoting layers to separate Gradle modules later is mechanical.

```
┌──────────────────────────────────────────────────────────────┐
│  ui / reader  (Compose screens, ViewModels, StateFlow state)   │
└───────────────┬────────────────────────────────────────────────┘
                │ calls use cases
┌───────────────▼────────────────────────────────────────────────┐
│  domain  (models, MangaProvider, repository interfaces, use cases)
│          — pure Kotlin, no Android/framework dependencies        │
└───────────────▲───────────────────────────▲────────────────────┘
                │ implements                 │ implements
┌───────────────┴───────────┐   ┌───────────┴────────────────────┐
│  data (repository impls,    │   │  providers                     │
│  Room, Retrofit, DataStore) │◄──┤  (provider implementations)     │
└────────────────────────────┘   └────────────────────────────────┘
                ▲
                │ provides
        ┌───────┴────────┐
        │  di (Hilt)     │   sync (WorkManager workers)
        └────────────────┘
```

---

## Package map

| Package | Responsibility |
| --- | --- |
| `com.mangamojo.app` | `MainActivity` (Compose host), `MainViewModel` (theme), `MangaMojoApp` (Hilt + WorkManager config + Coil image loader) |
| `core` | `Constants` (cache policy), `AppError` + `toAppError()` (error normalization), `UiState` (generic sealed UI state) |
| `domain.model` | Source-agnostic models: `Manga`, `MangaDetails`, `Chapter`, `Page`, `SearchResult`, `SearchQuery`, `ReadingProgress`, `Favorite`, `HistoryEntry`, `AppSettings`, enums |
| `domain.provider` | `MangaProvider`, `ProviderManager`, and provider-qualified id helpers |
| `domain.repository` | `MangaRepository`, `LibraryRepository`, `SettingsRepository` (interfaces) |
| `domain.usecase` | Thin use cases grouped by feature (Manga / Library / Reader / Settings) |
| `data.remote` | API client (Retrofit), `dto/` (serialization models), `mapper/` (DTO→domain), `RetryInterceptor` |
| `data.local` | `MangaMojoDatabase` (Room), `entity/`, `dao/`, `Converters`, `mapper/` (entity↔domain) |
| `data.repository` | Repository implementations that coordinate provider + cache + DataStore |
| `providers` | Concrete provider implementations |
| `di` | Hilt modules: `NetworkModule`, `DatabaseModule`, `RepositoryModule`, `ProviderModule` |
| `sync` | `CacheCleanupWorker`, `LibraryRefreshWorker` (`@HiltWorker`), `SyncScheduler` |
| `reader` | `ReaderViewModel` + `ReaderScreen` (page rendering, preloading, progress) |
| `ui.*` | One package per screen (`home`, `search`, `details`, `favorites`, `history`, `settings`) plus `components`, `navigation`, `theme` |

---

## Data flow (read path)

```
Composable ──collectAsStateWithLifecycle──► ViewModel.StateFlow
ViewModel ──suspend──► UseCase ──► Repository ──► { ProviderManager ──► MangaProvider(s)  ⇄  Room (cache) }
```

1. A screen observes its `ViewModel`'s `StateFlow` and renders `Loading / Success / Error`.
2. The ViewModel invokes a **use case**, which delegates to a **repository interface**.
3. `MangaRepositoryImpl` decides between the cache and the provider layer:
   - **Details / chapters** are *read-through cached*. If the cached row is fresh
     (within TTL) and no refresh is forced, it's returned immediately. Otherwise the
     provider is hit and the result is written back. **If the network fails, stale cache
     is returned as a fallback** so previously-viewed content stays available offline.
   - **Search** is not cached (query-specific, short-lived).
   - **Pages** are fetched live; the *images* are disk-cached by Coil.
4. Errors thrown by the provider are caught in the ViewModel and mapped via
   `Throwable.toAppError()` into a closed `AppError` set the UI can render.

### Threading
Retrofit `suspend` calls and Room `suspend`/`Flow` APIs are main-safe (each dispatches to
its own background executor), so repositories don't wrap calls in `withContext`. All work
runs in `viewModelScope`; reactive reads use `stateIn(...)` with
`WhileSubscribed`/`Eagerly`.

---

## Provider abstraction (the extension point)

```kotlin
interface MangaProvider {
    val id: String          // e.g. "mangadex", "another_source"
    val name: String        // e.g. "MangaDex", "Another Source"
    suspend fun search(query: SearchQuery): SearchResult
    suspend fun getMangaDetails(mangaId: String): MangaDetails
    suspend fun getChapters(mangaId: String, languages: List<String>): List<Chapter>
    suspend fun getPages(chapterId: String, dataSaver: Boolean): List<Page>
}
```

- Provider implementations own all source-specific details (query conventions, includes, pagination patterns, page-delivery flow, data normalization).
- They return **only normalized domain models** — DTOs never escape the `data.remote` layer.
- `ProviderModule` contributes providers through Hilt multibinding (`@Binds @IntoSet`).
- `ProviderManager` owns provider priority, merged search, source-aware routing, and
  provider failure isolation for search and supplemental chapter feeds.
- MangaDex is the canonical provider when the same title exists in multiple places.
  Supplement providers can add missing chapters under the canonical manga entry after
  exact title matching and chapter deduplication.
- URL-backed providers use provider-qualified ids so route/cache keys stay safe while
  MangaDex UUIDs remain unchanged for existing user data.

---

## Dependency injection (Hilt)

All graphs are installed in `SingletonComponent`:

| Module | Provides |
| --- | --- |
| `NetworkModule` | `Json`, `OkHttpClient` (User-Agent, timeouts, `RetryInterceptor`, debug logging), `Retrofit` (kotlinx-serialization converter), API clients |
| `DatabaseModule` | `MangaMojoDatabase`, each DAO, and the settings `DataStore<Preferences>` |
| `RepositoryModule` | `@Binds` the three repository interfaces to their impls |
| `ProviderModule` | `@Binds @IntoSet` provider contributions (`MangaDexProvider`, `MangaKakalotProvider`) |

`MangaMojoApp` is `@HiltAndroidApp` and also:
- implements `Configuration.Provider` to supply a `HiltWorkerFactory` so workers can be
  injected (the default WorkManager initializer is disabled in the manifest), and
- implements Coil's `SingletonImageLoader.Factory` to build an `ImageLoader` that reuses
  the app's shared `OkHttpClient` (consistent headers + timeouts for covers and pages).

---

## Networking

- `OkHttpClient`: 15 s connect / 20 s read / 15 s write timeouts, a polite `User-Agent`,
  default `Accept: application/json` when a request does not provide one, and a
  `RetryInterceptor` that retries transient `429`/`5xx` once with a fixed backoff.
- `MangaKakalotProvider` uses the same OkHttp client plus Jsoup for HTML parsing. Its
  base URL is configurable with `-PmangakakalotBaseUrl=...` because MangaKakalot-style
  domains have changed before.
- `Json` is configured with `ignoreUnknownKeys`, `coerceInputValues`, `isLenient`,
  `explicitNulls = false`.
- A `LenientStringMapSerializer` coerces provider-specific quirks (e.g., occasional
  `[]`-for-empty-object) back into an empty map so decoding never crashes.

---

## Persistence & caching

Room database `mangamojo.db` (see [DATA_MODEL.md](DATA_MODEL.md) for the full schema):

- **Cache tables** — `cached_manga`, `cached_chapters` (each row stamped with `cachedAt`).
- **User tables** — `favorites`, `history` (one row per manga), `reading_progress`
  (per chapter), `bookmarks`.
- **Settings** — DataStore Preferences (`mangamojo_settings`): theme mode, color theme,
  reading direction, data saver, content ratings, translated language.

**Cache policy** (`core/CachePolicy`):
- Manga metadata fresh for **12 h**, chapter feed fresh for **6 h**.
- Background eviction removes cached rows older than **7 days**, *preserving* anything
  that is favorited or in recent history.
- History is capped at **60** entries (trimmed on each write).
- Progress writes never regress the furthest-read page for a chapter.

---

## Background work (WorkManager)

`SyncScheduler.schedulePeriodicWork()` runs once on startup (idempotent via `KEEP`):

| Worker | Cadence | Job |
| --- | --- | --- |
| `CacheCleanupWorker` | every 24 h | `MangaRepository.evictStaleCache()` |
| `LibraryRefreshWorker` | every 12 h, requires network | refresh details + chapters for favorites so new chapters are ready offline |

Both are `@HiltWorker` `CoroutineWorker`s and are best-effort (`Result.retry()` on failure).

---

## Reader internals

- Vertical `LazyColumn` of full-width page images (Coil `AsyncImage`, `FillWidth`).
  Providers can attach per-page headers, which the reader forwards to Coil image requests.
- A fresh `LazyListState` per chapter, positioned at the **resume page** read from
  `reading_progress`.
- A `snapshotFlow` on the first visible index drives two effects: immediate **preloading**
  of the next few pages (`ImageLoader.enqueue`) and a **debounced progress save**.
- Next/previous chapter navigation reuses the (cached) chapter list; the feed is
  newest-first, so "next" (forward in story order) decrements the index.
- **External chapters** (`externalUrl != null`) are not rendered in-app — the reader shows
  an "Open in browser" action instead.

---

## UI conventions

- Each screen has a paired `@HiltViewModel` exposing immutable state via `StateFlow`.
- One-payload screens use the generic sealed `UiState<T>` (`Loading`/`Success`/`Error`);
  list screens use flat, immutable state data classes.
- The app uses fixed brand palettes (not dynamic color) driven by the user's `ThemeMode`
  and `ThemePalette`, for a consistent reader look across devices.
- A single top-level `Scaffold` owns the bottom navigation; screens render their own
  `TopAppBar`. Insets are handled once via the host `Scaffold` padding.
