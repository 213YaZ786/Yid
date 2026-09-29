package com.yid.app.di

import com.yid.app.data.marks.PostMarks
import com.yid.app.core.link.RedirectResolver
import com.yid.app.core.debug.LogExporter
import com.yid.app.core.debug.RequestLog
import com.yid.app.core.media.AutoMediaDownloader
import com.yid.app.core.media.OfflineMedia
import com.yid.app.core.media.MediaDownloader
import com.yid.app.core.media.MediaSavingNotice
import com.yid.app.core.network.ConnectivityMonitor
import com.yid.app.core.network.HostThrottle
import com.yid.app.core.network.HttpClientFactory
import com.yid.app.data.bsky.BskyApi
import com.yid.app.data.bsky.BskyVideo
import com.yid.app.core.link.LinkRouter
import com.yid.app.data.accounts.AccountStore
import com.yid.app.data.cache.FeedCache
import com.yid.app.data.repository.FeedRepository
import com.yid.app.data.repository.TimelineRepository
import com.yid.app.data.read.ReadMarks
import com.yid.app.data.settings.SettingsStore
import com.yid.app.feature.accounts.AccountsViewModel
import com.yid.app.feature.post.PostDetailViewModel
import com.yid.app.feature.search.SearchViewModel
import com.yid.app.feature.settings.SettingsViewModel
import com.yid.app.feature.feed.FeedViewModel
import com.yid.app.feature.media.SavedMediaViewModel
import com.yid.app.feature.timeline.TimelineViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

/**
 * Single composition root. Layers get added here as they land:
 * step 3 sources, step 4 database.
 */
val appModule = module {

    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Default) }

    single { RequestLog() }
    single { RedirectResolver() }
    single { LogExporter(androidContext()) }
    single { HostThrottle() }
    single { HttpClientFactory.create() }
    single { BskyApi(get(), get(), get()) }
    single { BskyVideo(get(), get()) }
    single { ConnectivityMonitor(androidContext()) }
    single {
        val videos: BskyVideo = get()
        MediaDownloader(androidContext(), get(named("appScope"))) { playlist -> videos.fileFor(playlist) }
    }
    single { OfflineMedia(androidContext()) }
    single { ReadMarks(androidContext()) }
    single { MediaSavingNotice(androidContext(), get(named("appScope"))) }
    single { AutoMediaDownloader(get(), get(), get(), get(), get(), get()) }
    single { AccountStore(androidContext()) }
    single { PostMarks(androidContext()) }
    single { LinkRouter() }
    single { FeedRepository(get()) }
    single {
        val settings: SettingsStore = get()
        FeedCache(androidContext(), get()) { settings.current.keepPostsDays }
    }
    single { SettingsStore(androidContext()) }
    single { TimelineRepository(get(), get(), get()) }

    viewModel { AccountsViewModel(get(), get(), get(), get()) }
    viewModel { PostDetailViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { FeedViewModel(get(), get(), get()) }
    viewModel { TimelineViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get(), get(), get(), androidContext()) }
    viewModel { SavedMediaViewModel(get()) }
}
