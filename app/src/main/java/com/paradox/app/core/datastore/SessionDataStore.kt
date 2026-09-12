package com.paradox.app.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.paradox.app.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activeProfileIdKey = stringPreferencesKey("active_profile_id")
    private val isAppLockedKey = booleanPreferencesKey("is_app_locked")
    private val preferredCurrencyKey = stringPreferencesKey("preferred_currency")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val themePaletteKey = stringPreferencesKey("theme_palette")
    private val appLanguageKey = stringPreferencesKey("app_language")
    private val isAiEnabledKey = booleanPreferencesKey("is_ai_enabled")
    private val quickBallModeKey = stringPreferencesKey("quick_ball_mode")

    val activeProfileId: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[activeProfileIdKey]
    }

    val isAppLocked: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isAppLockedKey] ?: true
    }

    val preferredCurrency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[preferredCurrencyKey] ?: Constants.DEFAULT_CURRENCY
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[themeModeKey] ?: "SYSTEM"
    }

    val themePalette: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[themePaletteKey] ?: "SLATE"
    }

    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[appLanguageKey] ?: "en"
    }

    val isAiEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[isAiEnabledKey] ?: false
    }

    val quickBallMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[quickBallModeKey] ?: "IN_APP"
    }

    suspend fun setActiveProfileId(profileId: String?) {
        context.dataStore.edit { preferences ->
            if (profileId != null) {
                preferences[activeProfileIdKey] = profileId
            } else {
                preferences.remove(activeProfileIdKey)
            }
        }
    }

    suspend fun setAppLocked(locked: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isAppLockedKey] = locked
        }
    }

    suspend fun setPreferredCurrency(currencyCode: String) {
        context.dataStore.edit { preferences ->
            preferences[preferredCurrencyKey] = currencyCode
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = mode
        }
    }

    suspend fun setThemePalette(palette: String) {
        context.dataStore.edit { preferences ->
            preferences[themePaletteKey] = palette
        }
    }

    suspend fun setAppLanguage(languageCode: String) {
        context.dataStore.edit { preferences ->
            preferences[appLanguageKey] = languageCode
        }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[isAiEnabledKey] = enabled
        }
    }

    suspend fun setQuickBallMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[quickBallModeKey] = mode
        }
    }
}
