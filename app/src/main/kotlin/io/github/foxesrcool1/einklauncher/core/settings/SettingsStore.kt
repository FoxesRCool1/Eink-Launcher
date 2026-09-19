package io.github.foxesrcool1.einklauncher.core.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * The small settings the launcher keeps.
 *
 * Plain files hold the user content (Step 4). DataStore holds only these few
 * values, which are cheap to rebuild if they are ever lost.
 */
class SettingsStore(context: Context) {

    private val store = context.applicationContext.settingsDataStore

    /**
     * Pinned apps, in the order the user pinned them. Stored as one string with
     * newlines, because a preference set does not keep an order.
     */
    val pinnedApps: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_PINNED]
            ?.split('\n')
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    suspend fun setPinnedApps(keys: List<String>) {
        store.edit { prefs ->
            prefs[KEY_PINNED] = keys.take(MAX_PINNED).joinToString("\n")
        }
    }

    suspend fun togglePinned(key: String) {
        store.edit { prefs ->
            val current = prefs[KEY_PINNED]
                ?.split('\n')
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val next = if (current.contains(key)) {
                current - key
            } else {
                (current + key).take(MAX_PINNED)
            }
            prefs[KEY_PINNED] = next.joinToString("\n")
        }
    }

    /** Section 5: the corner line art can be turned off. */
    val botanicalArt: Flow<Boolean> = store.data.map { it[KEY_ART] ?: true }

    suspend fun setBotanicalArt(enabled: Boolean) {
        store.edit { it[KEY_ART] = enabled }
    }

    companion object {
        /** Section 6: up to 8 pinned apps. */
        const val MAX_PINNED = 8

        private val KEY_PINNED = stringPreferencesKey("pinned_apps")
        private val KEY_ART = booleanPreferencesKey("botanical_art")
    }
}
