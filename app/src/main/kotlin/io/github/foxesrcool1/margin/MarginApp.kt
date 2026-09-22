package io.github.foxesrcool1.margin

import android.app.Application
import io.github.foxesrcool1.margin.core.log.AppLog
import io.github.foxesrcool1.margin.core.log.CrashHandler
import io.github.foxesrcool1.margin.core.storage.DataRoot
import kotlin.concurrent.thread

class MarginApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        // Tells on disk or network work on the main thread, in a debug build.
        io.github.foxesrcool1.margin.core.speed.SpeedWatch.watchMainThread(BuildConfig.DEBUG)
        CrashHandler.install(this)
        AppLog.i(
            "App",
            "Margin ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}, " +
                "build ${BuildConfig.VERSION_CODE}) started",
        )
        // The Kotlin version comes from the Android Gradle plugin now, and it
        // is useful to know which one ended up in the build.
        AppLog.i("App", "Kotlin ${KotlinVersion.CURRENT}, Android ${android.os.Build.VERSION.SDK_INT}")

        // A fast pen path that killed the app last time is switched off here,
        // before any screen can try it again.
        io.github.foxesrcool1.margin.core.eink.EinkDevices.settleAfterStart(this)

        // Making a few folders is fast, but a launcher must not touch the disk
        // on the main thread while the home screen is trying to appear.
        thread(name = "eink-data-root", isDaemon = true) {
            DataRoot.prepare(this)
            io.github.foxesrcool1.margin.core.update.UpdateManager.settleAfterStart(this)
        }
    }
}
