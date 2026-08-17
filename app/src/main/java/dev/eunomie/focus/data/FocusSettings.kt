package dev.eunomie.focus.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Cap from ADR 5. Deliberately not user-adjustable — an adjustable cap drifts upward. */
const val MAX_ALLOWED_APPS = 5

private val Context.dataStore by preferencesDataStore("focus")

private val ACTIVE = booleanPreferencesKey("focus_active")
// Ordered, so the list on the focus screen is the order chosen here. A Set has no order
// to preserve, hence the new key rather than a migration.
private val ALLOWED = stringPreferencesKey("allowed_apps_ordered")
private val DISABLED_EFFECTS = stringSetPreferencesKey("disabled_effects")

/**
 * `active` is the *intended* state, and is what reconciliation compares device state
 * against after a crash. It is persisted before any effect is applied, so a process death
 * mid-transition still leaves a recoverable record.
 */
class FocusSettings(private val context: Context) {

    val active: Flow<Boolean> = context.dataStore.data.map { it[ACTIVE] == true }

    val allowedApps: Flow<List<String>> = context.dataStore.data.map { it.allowedList() }

    suspend fun isActive(): Boolean = active.first()

    suspend fun allowedAppsNow(): List<String> = allowedApps.first()

    suspend fun setActive(value: Boolean) {
        context.dataStore.edit { it[ACTIVE] = value }
    }

    private fun Preferences.allowedList(): List<String> =
        this[ALLOWED]?.split("\n")?.filter { it.isNotEmpty() }.orEmpty()

    /** Stored as the *disabled* set so a new effect defaults to on. */
    val effects: Flow<Effects> = context.dataStore.data.map { prefs ->
        val off = prefs[DISABLED_EFFECTS].orEmpty()
        Effects(
            zen = Effect.ZEN.name !in off,
            greyscale = Effect.GREYSCALE.name !in off,
            alwaysOn = Effect.ALWAYS_ON.name !in off,
            hideLockNotifications = Effect.HIDE_LOCK_NOTIFICATIONS.name !in off,
            wallpaper = Effect.WALLPAPER.name !in off,
        )
    }

    suspend fun effectsNow(): Effects = effects.first()

    suspend fun toggleEffect(effect: Effect) {
        context.dataStore.edit { prefs ->
            val off = prefs[DISABLED_EFFECTS].orEmpty()
            prefs[DISABLED_EFFECTS] =
                if (effect.name in off) off - effect.name else off + effect.name
        }
    }

    suspend fun toggleApp(packageName: String) {
        context.dataStore.edit { prefs ->
            prefs[ALLOWED] = AllowedApps
                .toggle(prefs.allowedList(), packageName, MAX_ALLOWED_APPS)
                .joinToString("\n")
        }
    }

    suspend fun moveApp(packageName: String, delta: Int) {
        context.dataStore.edit { prefs ->
            prefs[ALLOWED] = AllowedApps
                .move(prefs.allowedList(), packageName, delta)
                .joinToString("\n")
        }
    }
}
