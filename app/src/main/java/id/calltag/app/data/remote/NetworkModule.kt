package id.calltag.app.data.remote

import android.util.Log
import com.google.gson.Gson
import id.calltag.app.BuildConfig
import id.calltag.app.data.preferences.CallTagPreferences
import id.calltag.app.data.remote.dto.AuthLoginResponseDto
import id.calltag.app.data.remote.dto.RefreshRequestDto
import id.calltag.app.util.PhoneNormalizer
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val TAG = "CallTagHTTP"
    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    private val authLock = Any()
    private val gson = Gson()

    @Volatile
    private var appPreferences: CallTagPreferences? = null

    fun init(preferences: CallTagPreferences) {
        appPreferences = preferences
        preferences.getOrCreateInstallIdSync()
    }

    private val redactedLogger = HttpLoggingInterceptor.Logger { message ->
        Log.d(TAG, PhoneNormalizer.maskPhoneInUrl(message))
    }

    private val authHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .writeTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Interceptor autentikasi otomatis (T-305 / OD-02):
     * Menyertakan X-Device-Id (install_id UUID) & Bearer token, serta mencoba refresh satu kali pada 401.
     */
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val prefs = appPreferences
        val path = original.url.encodedPath

        val builder = original.newBuilder()
        if (prefs != null) {
            builder.header("X-Device-Id", prefs.getOrCreateInstallIdSync())
            val token = prefs.getAccessTokenSync()
            if (token.isNotBlank() && original.header("Authorization").isNullOrBlank()) {
                builder.header("Authorization", "Bearer $token")
            }
        }

        val firstAttempt = builder.build()
        val response = chain.proceed(firstAttempt)

        if (response.code != 401 || prefs == null || !prefs.isLoggedInSync() || path.contains("/api/v1/auth/")) {
            return@Interceptor response
        }

        val usedToken = firstAttempt.header("Authorization")?.removePrefix("Bearer ")?.trim().orEmpty()
        val refreshedToken = refreshSession(prefs, usedToken)
        if (refreshedToken.isNullOrBlank()) {
            return@Interceptor response
        }

        response.close()
        val retried = original.newBuilder()
            .header("X-Device-Id", prefs.getOrCreateInstallIdSync())
            .header("Authorization", "Bearer $refreshedToken")
            .build()
        chain.proceed(retried)
    }

    private fun refreshSession(prefs: CallTagPreferences, failedToken: String): String? {
        synchronized(authLock) {
            if (!prefs.isLoggedInSync()) return null
            val latestToken = prefs.getAccessTokenSync()
            if (latestToken != failedToken) return null

            val base = BuildConfig.BASE_API_URL.trimEnd('/')
            val generation = prefs.sessionGenerationSync()
            val currentRefresh = prefs.getRefreshTokenSync()
            if (currentRefresh.isNotBlank()) {
                val refreshJson = gson.toJson(RefreshRequestDto(refreshToken = currentRefresh))
                val req = okhttp3.Request.Builder()
                    .url("$base/api/v1/auth/refresh")
                    .post(refreshJson.toRequestBody(JSON_MEDIA))
                    .build()
                val tokenFromRefresh = executeAuthRequest(prefs, req, generation)
                if (!tokenFromRefresh.isNullOrBlank()) return tokenFromRefresh
            }

            return null
        }
    }

    private fun executeAuthRequest(prefs: CallTagPreferences, req: okhttp3.Request, generation: String): String? {
        return runCatching {
            authHttpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val raw = resp.body?.string().orEmpty()
                if (raw.isBlank()) return@use null
                val dataObj = JSONObject(raw).optJSONObject("data") ?: return@use null
                val parsed = gson.fromJson(dataObj.toString(), AuthLoginResponseDto::class.java)
                val access = parsed.accessToken.ifBlank { parsed.token }
                if (access.isBlank()) return@use null
                if (!prefs.updateRefreshedTokensSync(generation, access, parsed.refreshToken)) return@use null
                access
            }
        }.getOrNull()
    }

    /**
     * Client dengan batas timeout sangat ketat (800 ms) khusus untuk CallTagScreeningService
     * pada endpoint `GET /api/v1/phones/{e164}` sesuai PRD Bagian 4.2 & 5.3.
     */
    private val fastScreeningOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(800, TimeUnit.MILLISECONDS)
            .readTimeout(800, TimeUnit.MILLISECONDS)
            .writeTimeout(800, TimeUnit.MILLISECONDS)
            .addInterceptor { chain ->
                val prefs = appPreferences
                val req = chain.request().newBuilder().apply {
                    if (prefs != null) {
                        header("X-Device-Id", prefs.getOrCreateInstallIdSync())
                        val t = prefs.getAccessTokenSync()
                        if (t.isNotBlank()) header("Authorization", "Bearer $t")
                    }
                }.build()
                chain.proceed(req)
            }
            .build()
    }

    /**
     * Client standar untuk aktivitas aplikasi (pelaporan, sinkronisasi Room DB, pencarian direktori darurat).
     */
    private val standardOkHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor(redactedLogger).apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .writeTimeout(8, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(standardOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val fastScreeningApiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_API_URL)
            .client(fastScreeningOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
