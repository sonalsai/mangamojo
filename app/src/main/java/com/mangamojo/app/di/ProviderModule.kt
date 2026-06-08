package com.mangamojo.app.di

import com.mangamojo.app.domain.provider.MangaProvider
import com.mangamojo.app.providers.mangadex.MangaDexProvider
import com.mangamojo.app.providers.mangakakalot.MangaKakalotProvider
import com.mangamojo.app.providers.mangareader.MangaReaderProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/**
 * Binds the available manga sources. The repository consumes them through
 * ProviderManager so search can merge sources and detail/reader calls can be
 * routed by source-aware ids.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProviderModule {

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMangaDexProvider(impl: MangaDexProvider): MangaProvider

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMangaKakalotProvider(impl: MangaKakalotProvider): MangaProvider

    @Binds
    @IntoSet
    @Singleton
    abstract fun bindMangaReaderProvider(impl: MangaReaderProvider): MangaProvider
}
