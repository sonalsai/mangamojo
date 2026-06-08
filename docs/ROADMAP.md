# Roadmap

## v1.1.x - Phase 1 patches (current track)

Patch releases that refine the MVP without changing core behavior:

- Bug fixes and crash hardening (network edge cases, malformed feeds).
- Reader polish: page-height placeholders to reduce layout jump, tap-to-toggle chrome.
- Search: tag/genre filters and sort options (the `SearchQuery` + `SearchSort` plumbing
  already exists), surfacing of genre shortcut tags.
- Bookmarks UI on top of the existing `bookmarks` table/DAO.
- Unit tests for provider mappers (normalization, chapter de-dup) and repository caching.
- Accessibility, string extraction/localization, and dependency bumps.

## v1.2.0 - Phase 2 (multi-provider)

The architecture was built for this; only the integration points below need implementation.

1. **Add additional providers.** Started with configurable scraper providers:
   `MangaKakalotProvider` and `MangaReaderProvider`.
2. **Multibind providers.** Done: `ProviderModule` now contributes providers with
   `@Binds @IntoSet` and Hilt provides a `Set<MangaProvider>`.
3. **Multi-source repository.** Started: `MangaRepositoryImpl` now delegates to
   `ProviderManager` for merged search, source-routed lookups, and supplemental
   chapter feeds.
4. **Source priority ordering.** Started with static priority (`MangaDex` first).
   MangaDex is treated as the canonical entry when duplicates exist; supplement
   providers fill missing data.
5. **Merge & deduplicate search.** Started: `ProviderManager` fans out search across
   providers concurrently and dedupes by normalized title/year. `sourceId` on every
   model disambiguates duplicates.
6. **Chapter supplementation.** Started: exact title matches from supplemental
   providers can add chapters under the MangaDex manga entry, with dedupe by
   chapter number/title.
7. **Fallback on failure.** Search already tolerates a failed provider when another
   succeeds. Chapter supplementation also survives a failed supplement provider.
   Next: when a specific provider chapter/page fetch fails, fall through to the
   next matching provider by priority.
8. **Provider health checks.** Started: providers expose `isAvailable()` and
   `ProviderManager.getHealthyProviders()` returns reachable sources.
9. **Reader image headers.** Done: `Page` can carry request headers and the reader
   passes them to Coil.
10. **Schema evolution.** Every entity already stores `sourceId`. When ids can collide
   across sources, migrate cache tables to a composite `(sourceId, mangaId)` key and add a
   real Room migration (replacing the destructive fallback).
11. **Paged reader modes.** Implement single/double-page horizontal pagers; `ReadingDirection`
   (LTR/RTL) and `dataSaver` are already plumbed through Settings.

### Explicitly out of scope (per product direction)
- User accounts, login, cloud sync, or any backend service.
- Desktop support.
- Scraping logic that violates a source's terms or copyright.
