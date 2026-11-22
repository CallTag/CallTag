package id.calltag.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.datastore.preferences.core.stringSetPreferencesKey
import id.calltag.app.util.PhoneNormalizer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "calltag_settings")

/**
 * Mengelola pengaturan pengguna & kredensial perangkat:
 * - auto_block_scammer: Default OFF (opt-in) sesuai T-302 / OD-01.
 * - show_heads_up_alert: Notifikasi & overlay Caller ID saat panggilan berdering.
 * - auth_token / refresh_token / install_id: Sesi perangkat tanpa nomor telepon (OD-02 / T-305),
 *   disinkronkan dengan SharedPreferences privat agar OkHttp Interceptor dapat membaca secara sinkron.
 */
class CallTagPreferences(private val context: Context) {

    private val authPrefs by lazy {
        context.applicationContext.getSharedPreferences("calltag_secure_auth", Context.MODE_PRIVATE)
    }

    @Volatile
    private var cachedAccessToken: String? = null

    @Volatile
    private var cachedRefreshToken: String? = null

    @Volatile
    private var cachedInstallId: String? = null

    private val _sessionRevision = MutableStateFlow(0)
    val sessionRevision = _sessionRevision.asStateFlow()
    private val _isLoggedIn = MutableStateFlow(isLoggedInSync())
    val isLoggedInFlow = _isLoggedIn.asStateFlow()
    private val _displayName = MutableStateFlow(if (isLoggedInSync()) authPrefs.getString("display_name", "").orEmpty() else "")
    val sessionDisplayNameFlow = _displayName.asStateFlow()

    fun isLoggedInSync(): Boolean = SessionAccessPolicy.isLoggedIn(
        authPrefs.getBoolean("google_login", false), authPrefs.getString(PREF_ACCESS_TOKEN, "").orEmpty()
    )

    fun sessionIdSync(): String = if (isLoggedInSync()) authPrefs.getString("session_id", "").orEmpty() else ""

    fun reportOwnerSync(localId: Long): String = authPrefs.getString("report_owner_$localId", "").orEmpty()

    fun ownReport(localId: Long, sessionId: String) {
        authPrefs.edit().putString("report_owner_$localId", sessionId).commit()
    }

    suspend fun startExplicitSession(access: String, refresh: String, displayName: String, installId: String, userId: String) {
        synchronized(this) {
            authPrefs.edit().putBoolean("google_login", true)
                .putString("session_id", userId.ifBlank { UUID.randomUUID().toString() })
                .putString("session_generation", UUID.randomUUID().toString())
                .putString("display_name", displayName)
                .putString(PREF_ACCESS_TOKEN, access)
                .putString(PREF_REFRESH_TOKEN, refresh).commit()
            cachedAccessToken = access
            cachedRefreshToken = refresh
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = access
            prefs[KEY_REFRESH_TOKEN] = refresh
            prefs[KEY_INSTALL_ID] = installId
        }
        _displayName.value = displayName
        _isLoggedIn.value = true
        _sessionRevision.value++
    }

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_DARK_MODE] ?: false }
    suspend fun setDarkMode(enabled: Boolean) { context.dataStore.edit { it[KEY_DARK_MODE] = enabled } }

    private fun whitelistKey() = stringSetPreferencesKey("personal_whitelist_${sessionIdSync()}")
    val personalWhitelistFlow: Flow<Set<String>> = combine(sessionRevision, context.dataStore.data) { _, prefs ->
        if (isLoggedInSync()) prefs[whitelistKey()] ?: emptySet() else emptySet()
    }
    suspend fun addPersonalWhitelist(rawPhone: String) {
        check(isLoggedInSync()) { "Masuk akun untuk mengelola daftar pengecualian." }
        val phone = PhoneNormalizer.normalizeToE164(rawPhone)
        require(phone.length >= 8) { "Nomor telepon tidak valid." }
        val key = whitelistKey()
        val generation = sessionGenerationSync()
        context.dataStore.edit {
            check(isLoggedInSync() && generation == sessionGenerationSync()) { "Sesi berubah. Silakan coba kembali." }
            it[key] = (it[key] ?: emptySet()) + phone
        }
    }
    suspend fun removePersonalWhitelist(rawPhone: String) {
        check(isLoggedInSync()) { "Masuk akun untuk mengelola daftar pengecualian." }
        val phone = PhoneNormalizer.normalizeToE164(rawPhone)
        val key = whitelistKey()
        val generation = sessionGenerationSync()
        context.dataStore.edit {
            check(isLoggedInSync() && generation == sessionGenerationSync()) { "Sesi berubah. Silakan coba kembali." }
            it[key] = (it[key] ?: emptySet()) - phone
        }
    }
    suspend fun isPersonallyWhitelisted(phone: String): Boolean =
        isLoggedInSync() && PhoneNormalizer.normalizeToE164(phone) in personalWhitelistFlow.first()

    companion object {
        private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
        private val KEY_AUTO_BLOCK_SCAMMER = booleanPreferencesKey("auto_block_scammer")
        private val KEY_SHOW_HEADS_UP_ALERT = booleanPreferencesKey("show_heads_up_alert")
        private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val KEY_REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val KEY_INSTALL_ID = stringPreferencesKey("install_id")
        private val KEY_SELECTED_CITY = stringPreferencesKey("selected_city")

        private const val PREF_ACCESS_TOKEN = "access_token"
        private const val PREF_REFRESH_TOKEN = "refresh_token"
        private const val PREF_INSTALL_ID = "install_id"
    }

    val autoBlockScammerFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_BLOCK_SCAMMER] ?: false
    }

    val showHeadsUpAlertFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_HEADS_UP_ALERT] ?: true
    }

    val authTokenFlow: Flow<String> = context.dataStore.data.map {
        getAccessTokenSync()
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

    fun getOrCreateInstallIdSync(): String {
        val current = cachedInstallId ?: authPrefs.getString(PREF_INSTALL_ID, null)
        if (!current.isNullOrBlank()) {
            cachedInstallId = current
            return current
        }
        val created = "and-" + UUID.randomUUID().toString()
        cachedInstallId = created
        authPrefs.edit().putString(PREF_INSTALL_ID, created).apply()
        return created
    }

    fun getAccessTokenSync(): String {
        if (!isLoggedInSync()) return ""
        val token = cachedAccessToken ?: authPrefs.getString(PREF_ACCESS_TOKEN, "") ?: ""
        cachedAccessToken = token
        return token
    }

    fun getRefreshTokenSync(): String {
        if (!isLoggedInSync()) return ""
        val token = cachedRefreshToken ?: authPrefs.getString(PREF_REFRESH_TOKEN, "") ?: ""
        cachedRefreshToken = token
        return token
    }

    fun sessionGenerationSync(): String = authPrefs.getString("session_generation", "").orEmpty()

    @Synchronized
    fun updateRefreshedTokensSync(generation: String, access: String, refresh: String): Boolean {
        if (!isLoggedInSync() || generation != sessionGenerationSync()) return false
        saveSessionTokensSync(access, refresh)
        return true
    }

    @Synchronized
    fun saveSessionTokensSync(accessToken: String, refreshToken: String) {
        cachedAccessToken = accessToken
        cachedRefreshToken = refreshToken
        authPrefs.edit()
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .putString(PREF_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    @Synchronized
    fun clearSessionSync() {
        cachedAccessToken = ""
        cachedRefreshToken = ""
        authPrefs.edit()
            .remove("explicit_login")
            .remove("google_login")
            .remove("session_id")
            .remove("session_generation")
            .remove("display_name")
            .remove(PREF_ACCESS_TOKEN)
            .remove(PREF_REFRESH_TOKEN)
            .commit()
        _isLoggedIn.value = false
        _displayName.value = ""
        _sessionRevision.value++
    }

    suspend fun saveSessionTokens(accessToken: String, refreshToken: String, installId: String = getOrCreateInstallIdSync()) {
        saveSessionTokensSync(accessToken, refreshToken)
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = accessToken
            prefs[KEY_REFRESH_TOKEN] = refreshToken
            prefs[KEY_INSTALL_ID] = installId
        }
    }

    suspend fun clearSession() {
        clearSessionSync()
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_REFRESH_TOKEN)
        }
    }

    suspend fun saveAuthToken(token: String) {
        cachedAccessToken = token
        authPrefs.edit().putString(PREF_ACCESS_TOKEN, token).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTH_TOKEN] = token
        }
    }

    suspend fun getAuthToken(): String = getAccessTokenSync()

    suspend fun setSelectedCity(city: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SELECTED_CITY] = city
        }
    }
}
