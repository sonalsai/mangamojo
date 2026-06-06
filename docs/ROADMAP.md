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

1. **Add a second provider.** Implement `MangaProvider` for a new source in `providers/`.
   It returns the same domain models - no UI or domain changes required.
2. **Multibind providers.** Change `ProviderModule` from `@Binds` to `@Binds @IntoSet`
   so Hilt provides a `Set<MangaProvider>`.
3. **Multi-source repository.** Update `MangaRepositoryImpl` to iterate the provider set
   instead of a single provider.
4. **Source priority ordering.** Add a settings-driven ordering so results and lookups
   prefer the user's chosen sources.
5. **Merge & deduplicate search.** Fan out `search` across providers concurrently, then
   dedupe by normalized title/year. `sourceId` on every model disambiguates duplicates.
6. **Fallback on failure.** When one provider's chapter/page fetch fails, fall through to
   the next provider by priority.
7. **Schema evolution.** Every entity already stores `sourceId`. When ids can collide
   across sources, migrate cache tables to a composite `(sourceId, mangaId)` key and add a
   real Room migration (replacing the destructive fallback).
8. **Paged reader modes.** Implement single/double-page horizontal pagers; `ReadingDirection`
   (LTR/RTL) and `dataSaver` are already plumbed through Settings.

### Explicitly out of scope (per product direction)
- User accounts, login, cloud sync, or any backend service.
- Desktop support.
- Scraping logic that violates a source's terms or copyright.
