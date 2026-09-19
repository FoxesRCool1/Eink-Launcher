package io.github.foxesrcool1.einklauncher

import android.app.Application
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.log.CrashHandler

class EinkLauncherApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        CrashHandler.install(this)
        AppLog.i(
            "App",
            "Eink Launcher ${BuildConfig.VERSION_NAME} (${BuildConfig.FLAVOR}, " +
                "build ${BuildConfig.VERSION_CODE}) started",
        )
    }
}
