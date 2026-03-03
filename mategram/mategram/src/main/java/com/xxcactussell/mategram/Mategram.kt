package com.xxcactussell.mategram

import android.app.Application
import android.os.StrictMode
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.xxcactussell.data.TdClientManager
import com.xxcactussell.data.TelegramCredentials
import com.xxcactussell.jni.NativeStickerCore
import com.xxcactussell.presentation.localization.LocalizationManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class MategramApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .build()

    @Inject
    lateinit var tdClientManager: TdClientManager

    @Inject
    lateinit var localizationManager: LocalizationManager

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }

        val path = cacheDir.absolutePath;
        NativeStickerCore.setCacheDir(path);

        tdClientManager.initCred(this@MategramApplication)

        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            tdClientManager.initialize(this@MategramApplication)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        NativeStickerCore.shutdownCache()
    }
}