package io.github.foxesrcool1.einklauncher.ui.apps

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/** The "Always available" block is the way out of this launcher, so what is in it matters. */
@RunWith(RobolectricTestRunner::class)
class AppsRepositoryTest {

    private val context = RuntimeEnvironment.getApplication()

    private fun home(packageName: String, className: String, label: String, priority: Int) = ResolveInfo().apply {
        activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = className
            applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
        }
        nonLocalizedLabel = label
        this.priority = priority
    }

    @Test
    fun `the stock launcher is listed, and the nameless fallback home of Android is not`() {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        shadowOf(context.packageManager).addResolveInfoForIntent(
            homeIntent,
            listOf(
                home("com.wisky.launcher", "com.wisky.launcher.Main", "WiskyLauncher", 0),
                home("com.android.settings", "com.android.settings.FallbackHome", "", -1000),
                home(context.packageName, "io.github.foxesrcool1.einklauncher.HomeActivity", "Eink Launcher", 0),
            ),
        )

        val entries = AppsRepository(context).escapeEntries(emptyList())
        val homes = entries.filter { it.kind == EscapeKind.OtherHome }

        assertEquals(listOf("WiskyLauncher"), homes.map { it.label })
        assertTrue(entries.none { it.label.isBlank() })
    }

    @Test
    fun `a home app with no name is shown by its package, so the row is never blank`() {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        shadowOf(context.packageManager).addResolveInfoForIntent(
            homeIntent,
            listOf(home("com.example.barehome", "com.example.barehome.Main", "", 0)),
        )

        val homes = AppsRepository(context).escapeEntries(emptyList()).filter { it.kind == EscapeKind.OtherHome }
        assertEquals(listOf("com.example.barehome"), homes.map { it.label })
    }
}
