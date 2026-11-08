package id.calltag.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calltag_settings")

/**
 * Mengelola pengaturan pengguna menggunakan Jetpack DataStore Preferences:
 * - auto_block_scammer: Otomatis menolak panggilan masuk dari nomor DANGER_SCAM / Pinjol Ilegal / Penipuan APK / Judol.
 * - show_heads_up_alert: Menampilkan Heads-Up Notification berprioritas tinggi saat panggilan berdering.
 * - auth_token: Menyimpan JWT Bearer token hasil POST /api/v1/auth/device.
 * - selected_city: Kota/Kabupaten pilihan pengguna untuk direktori Damkar/Polisi lokal.
 */
class CallTagPreferences(private val context: Context) {

    companion object {
        private val KEY_AUTO_BLOCK_SCAMMER = booleanPreferencesKey("auto_block_scammer")
        private val KEY_SHOW_HEADS_UP_ALERT = booleanPreferencesKey("show_heads_up_alert")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_SELECTED_CITY = stringPreferencesKey("selected_city")
    }

    val autoBlockScammerFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_BLOCK_SCAMMER] ?: true
    }

    val showHeadsUpAlertFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_HEADS_UP_ALERT] ?: true
    }

    val authTokenFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTH_TOKEN] ?: ""
    }

    val selectedCityFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_CITY] ?: "Kota Bandung"
    }

    suspend fun isAutoBlockScammerEnabled(): Boolean {
        return autoBlockScammerFlow.first()
    }

    suspend fun setAutoBlockScammer(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_BLOCK_SCAMMER] = enabled
        }
    }

    suspend fun setShowHeadsUpAlert(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SHOW_HEADS_UP_ALERT] = enabled
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun getAuthToken(): String {
        return authTokenFlow.first()
    }

    suspend fun setSelectedCity(city: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_CITY] = city
        }
    }
}
