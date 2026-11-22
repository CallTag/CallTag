package id.calltag.app.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import id.calltag.app.BuildConfig
import id.calltag.app.CallTagApplication
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.util.PhoneNormalizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Layanan sistem Android (turunan CallScreeningService) untuk mendeteksi Caller ID
 * dan melakukan pemblokiran otomatis (Spam Guard) pada panggilan masuk.
 *
 * Alur eksekusi:
 * 1. Saat onScreenCall dipanggil, ekstrak nomor penelepon dan normalisasi ke E.164 (misal 08xx -> +628xx).
 * 2. Lakukan pengecekan ke Room Database (Local Cache) terlebih dahulu. Jika belum ada, panggil
 *    `GET /api/v1/phones/{e164}` dengan batas timeout ketat (800 ms).
 * 3. Jika nomor teridentifikasi memiliki skor bahaya tinggi (Pinjol Ilegal / Penipuan APK / CS Palsu / Judol)
 *    dan opsi "Auto-Block Scammer" aktif di DataStore, tolak panggilan secara otomatis menggunakan
 *    CallResponse.Builder().setDisallowCall(true).setRejectCall(true).
 * 4. Tampilkan Heads-Up Notification berprioritas tinggi & Overlay Caller ID berisi Nama Tag Penelepon,
 *    Skor Bahaya, dan Kategori Scam-nya.
 */
class CallTagScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        // Hanya saring panggilan masuk (DIRECTION_INCOMING pada API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (callDetails.callDirection != Call.Details.DIRECTION_INCOMING) {
                allowIncomingCall(callDetails)
                return
            }
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "Callback penyaringan panggilan masuk diterima")

        val rawNumber = callDetails.handle?.schemeSpecificPart
        val normalizedE164 = PhoneNormalizer.normalizeToE164(rawNumber)

        if (normalizedE164.isEmpty()) {
            allowIncomingCall(callDetails)
            return
        }

        val app = applicationContext as? CallTagApplication
        if (app == null) {
            allowIncomingCall(callDetails)
            return
        }

        val historyOwner = app.preferences.sessionIdSync()
        serviceScope.launch {
            val responded = AtomicBoolean(false)
            try {
                withTimeoutOrNull(4_200L) {
                    if (PhoneNormalizer.isEmergencyOrShortCode(rawNumber)) {
                        if (responded.compareAndSet(false, true)) {
                            allowIncomingCall(callDetails)
                        }
                        return@withTimeoutOrNull
                    }

                    val repository = app.repository
                    val preferences = app.preferences

                    val lookupResult = runCatching {
                        repository.screenIncomingPhone(normalizedE164)
                    }.getOrNull()

                    if (lookupResult == null ||
                        lookupResult.phoneTag.reputationLevel.equals("CLEAN", ignoreCase = true) ||
                        lookupResult.phoneTag.reputationLevel.equals("UNKNOWN", ignoreCase = true)
                    ) {
                        if (responded.compareAndSet(false, true)) {
                            allowIncomingCall(callDetails)
                        }
                        return@withTimeoutOrNull
                    }

                    val phoneTag = lookupResult.phoneTag
                    val autoBlockEnabled = preferences.isAutoBlockScammerEnabled()
                    val shouldBlock = PhoneNormalizer.shouldAutoBlock(
                        reputationLevel = phoneTag.reputationLevel,
                        spamScore = phoneTag.spamScore,
                        primaryCategory = phoneTag.primaryCategory,
                        autoBlockEnabled = autoBlockEnabled,
                        source = phoneTag.source,
                        fetchedAtEpochMs = phoneTag.fetchedAtEpochMs,
                        isWhitelisted = !phoneTag.verifiedLabel.isNullOrBlank() ||
                            preferences.isPersonallyWhitelisted(normalizedE164),
                        phoneE164 = phoneTag.phoneE164
                    )

                    val maskedPhone = PhoneNormalizer.maskPhone(phoneTag.phoneE164)
                    Log.i(
                        TAG,
                        "Screened call from $maskedPhone: level=${phoneTag.reputationLevel}, " +
                            "source=${phoneTag.source}, blocked=$shouldBlock"
                    )

                    if (responded.compareAndSet(false, true)) {
                        if (shouldBlock) {
                            val rejectResponse = CallResponse.Builder()
                                .setDisallowCall(true)
                                .setRejectCall(true)
                                .setSkipCallLog(false)
                                .setSkipNotification(false)
                                .build()
                            respondToCall(callDetails, rejectResponse)
                        } else {
                            allowIncomingCall(callDetails)
                        }
                    }

                    // Catat ke riwayat lokal Room DB (nomor sudah disamarkan)
                    repository.recordScreeningLog(
                        CallScreeningLog(
                            phoneE164 = maskedPhone,
                            tagLabel = phoneTag.topCommunityLabel,
                            categoryCode = phoneTag.primaryCategory,
                            reputationLevel = phoneTag.reputationLevel,
                            spamScore = phoneTag.spamScore,
                            wasBlocked = shouldBlock,
                            detectionSource = lookupResult.source
                        ),
                        owner = historyOwner
                    )

                    // Tampilkan Heads-Up Notification berprioritas tinggi & Overlay Caller ID bila bukan data fixture palsu
                    CallerIdNotifier.showCallerAlert(
                        context = applicationContext,
                        phoneTag = phoneTag,
                        wasAutoBlocked = shouldBlock,
                        showNotification = preferences.showHeadsUpAlertFlow.first()
                    )
                }
            } finally {
                if (responded.compareAndSet(false, true)) {
                    allowIncomingCall(callDetails)
                }
            }
        }
    }

    private fun allowIncomingCall(callDetails: Call.Details) {
        val allowResponse = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()
        respondToCall(callDetails, allowResponse)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "CallTagScreeningSvc"
    }
}
