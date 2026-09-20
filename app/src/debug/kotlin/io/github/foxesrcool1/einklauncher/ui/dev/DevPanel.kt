package io.github.foxesrcool1.einklauncher.ui.dev

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.core.storage.StorageBenchmark
import io.github.foxesrcool1.einklauncher.core.update.ApkInstaller
import io.github.foxesrcool1.einklauncher.core.update.UpdateManager
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.EinkTextField
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "DevPanel"
private const val PREFS = "eink_dev"
private const val KEY_URL = "build_url"
private const val DEFAULT_URL = "http://192.168.1.10:8000/app-viwoods-debug.apk"

/** Debug builds show the dev entry. */
const val DEV_PANEL_AVAILABLE: Boolean = true

/**
 * Gets the next build onto the tablet without adb.
 *
 * `tools/deploy.sh` on the dev machine builds the APK and serves it on the
 * local network. This panel downloads that file and hands it to the same
 * installer the in-app update uses. The debug key is fixed, so the new build
 * installs over the old one and keeps the app data.
 *
 * Builds from GitHub come through Settings, Help, "Check for updates". This
 * panel is for a build that is not pushed yet.
 */
@Composable
fun DevPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    var url by remember { mutableStateOf(prefs.getString(KEY_URL, DEFAULT_URL) ?: DEFAULT_URL) }
    var status by remember { mutableStateOf("Ready") }
    var benchmark by remember { mutableStateOf("Not measured yet") }
    var busy by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        CapsLabel(text = "Build URL", style = EinkType.capsSmall)
        Spacer(modifier = Modifier.height(6.dp))

        EinkTextField(
            value = url,
            onValueChange = {
                url = it
                prefs.edit().putString(KEY_URL, it).apply()
            },
            textStyle = EinkType.body,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = EinkDimens.touchTarget)
                .background(EinkColors.Paper)
                .border(width = EinkDimens.hairline, color = EinkColors.Ink)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        )

        Spacer(modifier = Modifier.height(EinkDimens.targetGap))

        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(
                text = "Get latest build",
                enabled = !busy,
                onClick = {
                    busy = true
                    status = "Downloading"
                    scope.launch {
                        status = withContext(Dispatchers.IO) {
                            download(context, url).mapCatching { file ->
                                AppLog.i(TAG, "Downloaded ${file.length()} bytes to ${file.name}")
                                ApkInstaller.install(context, file).getOrThrow()
                            }.fold(
                                onSuccess = { "Downloaded. Android should ask to install it." },
                                onFailure = { error ->
                                    AppLog.e(TAG, "The LAN update failed", error)
                                    "Failed: ${error.message}"
                                },
                            )
                        }
                        busy = false
                    }
                },
            )
            InvertPressButton(
                text = "Install sources",
                onClick = { ApkInstaller.openPermissionScreen(context) },
            )
        }

        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        EinkText(text = status, style = EinkType.body)

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        CapsLabel(text = "Storage speed", style = EinkType.capsSmall)
        Spacer(modifier = Modifier.height(6.dp))
        EinkText(
            text = "Plan step 4 asks for a measurement with 500 files before choosing " +
                "how the data folder is reached. Run this on the tablet and send me " +
                "the numbers. It cleans up after itself.",
            style = EinkType.body,
        )
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))

        InvertPressButton(
            text = "Time 500 files",
            enabled = !busy,
            onClick = {
                busy = true
                benchmark = "Running"
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        runCatching { StorageBenchmark.run(DataRoot.repository(context).store) }
                    }
                    busy = false
                    benchmark = result.fold(
                        onSuccess = { measured ->
                            measured.asLines().forEach { line ->
                                AppLog.i(TAG, "Storage benchmark: $line")
                            }
                            measured.asLines().joinToString("\n")
                        },
                        onFailure = { error ->
                            AppLog.e(TAG, "Storage benchmark failed", error)
                            "Failed: ${error.message}"
                        },
                    )
                }
            },
        )

        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        EinkText(text = benchmark, style = EinkType.body)
    }
}

private fun download(context: Context, url: String): Result<File> = runCatching {
    // The same folder as the in-app update, because that is the one folder
    // the file provider shares with the system installer.
    val dir = UpdateManager.downloadFolder(context).apply { mkdirs() }
    val target = File(dir, "lan-build.apk")
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 60_000
        requestMethod = "GET"
    }
    try {
        val code = connection.responseCode
        check(code == HttpURLConnection.HTTP_OK) { "HTTP $code" }
        connection.inputStream.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
    } finally {
        connection.disconnect()
    }
    check(target.length() > 0) { "The file is empty" }
    target
}
