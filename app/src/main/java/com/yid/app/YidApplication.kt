package com.yid.app

import android.app.Application
import com.yid.app.core.media.OfflineMedia
import com.yid.app.data.cache.FeedCache
import com.yid.app.data.read.ReadMarks
import com.yid.app.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.core.qualifier.named
import com.yid.app.di.appModule
import com.yid.app.sync.SyncWorker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class YidApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
            androidContext(this@YidApplication)
            modules(appModule)
        }

        // WorkManager survives reboots, but re-applying on launch keeps the
        // schedule honest after an app update or a settings change made while
        // the worker was cancelled.
        val koin = org.koin.core.context.GlobalContext.get()
        // Old posts go at launch, not only when their account is next fetched,
        // and their saved media goes with them. One retention, one sweep.
        koin.get<CoroutineScope>(named("appScope")).launch {
            val cache = koin.get<FeedCache>()
            cache.applyRetention()
            val kept = cache.storedPostIds()
            koin.get<OfflineMedia>().keepOnly(kept)
            // Whatever was already on disk when the app opened is not new.
            // Without this a reader coming back after a week would face a
            // border on two hundred posts they had already seen.
            val marks = koin.get<ReadMarks>()
            marks.load()
            marks.markAllRead(kept)
        }

        val settings = koin.get<SettingsStore>()
        if (settings.current.backgroundSync) {
            SyncWorker.schedule(
                this,
                settings.current.syncIntervalMinutes,
                settings.current.syncOnWifiOnly
            )
        }
    }
}
