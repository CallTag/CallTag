package id.calltag.app.util

object PhoneNormalizer {

    const val MAX_AUTO_BLOCK_CACHE_AGE_MS = 14L * 24 * 60 * 60 * 1000L
    const val CACHE_DISPLAY_TTL_MS = 90L * 24 * 60 * 60 * 1000L

    private val CATEGORY_LABELS = mapOf(
        "INFO_TAG" to "Info Umum (bukan laporan spam)",
        "PINJOL_DC" to "Pinjol Ilegal & Debt Collector Meresahkan",
        "APK_SNIFFING" to "Penipuan Modus File APK (Kurir/Undangan/Tilang)",
        "IMPERSONATION" to "Impersonasi / CS Palsu (Bank/E-Wallet/Pajak)",
        "JUDOL" to "Promosi Judi Online (Judol)",
        "SOCIAL_ENG" to "Penipuan Sosial / Hipnotis Telepon",
        "TELEMARKETING" to "Telemarketing / Spam Promosi Agresif"
    )

    /**
     * Memeriksa apakah nomor adalah kode pendek darurat/layanan (1-6 digit seperti 112, 110, 14080).
     */
    fun isEmergencyOrShortCode(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val cleaned = phone.removePrefix("tel:").trim()
        if (cleaned.startsWith("+")) return false
        val digits = cleaned.filter { it.isDigit() }
        return digits.length in 1..6
    }

    /**
     * Menormalisasi nomor telepon Indonesia dari berbagai format (misal "0812-3456-7890",
     * "6281234567890", atau URI "tel:0812...") menjadi format E.164 ("+6281234567890").
     * Nomor darurat pendek (3-6 digit seperti 112, 110, 113, 119, 14080) dipertahankan.
     */
    fun normalizeToE164(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val cleaned = rawNumber.trim().removePrefix("tel:").trim()
        val digits = buildString {
            for (ch in cleaned) {
                if (ch.isDigit()) append(ch)
            }
        }
        if (digits.isEmpty()) return ""
        if (cleaned.startsWith("+")) return "+$digits"
        if (digits.length == 7 && digits.startsWith("150")) return digits
        if (digits.length <= 6) return digits

        return when {
            digits.startsWith("0") -> "+62" + digits.substring(1)
            digits.startsWith("62") -> "+$digits"
            digits.startsWith("8") -> "+62$digits"
            else -> "+$digits"
        }
    }

    fun detectCarrierContext(phone: String): String = CarrierContext.detect(phone)

    fun isVerificationUnexpired(expiresAt: String?, nowEpochMs: Long): Boolean =
        expiresAt == null || runCatching { java.time.Instant.parse(expiresAt).toEpochMilli() > nowEpochMs }.getOrDefault(false)

    /** Tautan hanya memakai nomor lengkap internasional, bukan kode layanan nasional. */
    fun whatsAppUrl(rawPhone: String): String? {
        val cleaned = rawPhone.trim().removePrefix("tel:").trim()
        if (cleaned.any { !it.isDigit() && it !in "+ -()." }) return null
        if (cleaned.count { it == '+' } > 1 || ('+' in cleaned && !cleaned.startsWith("+"))) return null
        val phone = normalizeToE164(cleaned)
        if (!phone.matches(Regex("\\+[1-9][0-9]{6,14}"))) return null
        val digits = phone.drop(1)
        val knownCountry = digits.startsWith("62") || CarrierContext.countries.keys.any { digits.startsWith(it) && digits.length > it.length }
        return if (knownCountry) "https://wa.me/$digits" else null
    }

    /**
     * Menyamarkan bagian tengah nomor telepon agar tidak bocor sebagai PII mentah di logcat atau DB log.
     * Contoh: "+6281234567005" -> "+62812****7005".
     */
    fun maskPhone(rawPhone: String?): String {
        if (rawPhone.isNullOrBlank()) return ""
        val normalized = normalizeToE164(rawPhone)
        if (normalized.isEmpty()) return rawPhone.trim()
        if (normalized.length <= 6) return normalized
        if (normalized.length <= 9) {
            return normalized.take(3) + "****" + normalized.takeLast(2)
        }
        return normalized.take(6) + "****" + normalized.takeLast(4)
    }

    /**
     * Menyamarkan nomor telepon pada segmen path URL `/api/v1/phones/{e164}` untuk log jaringan.
     */
    fun maskPhoneInUrl(url: String): String {
        val marker = "/api/v1/phones/"
        val idx = url.indexOf(marker)
        if (idx < 0) return url
        val start = idx + marker.length
        val endSlash = url.indexOf('/', start)
        val endQuery = url.indexOf('?', start)
        val end = listOf(endSlash, endQuery).filter { it >= 0 }.minOrNull() ?: url.length
        if (end <= start) return url
        val rawSeg = url.substring(start, end)
        return url.substring(0, start) + maskPhone(rawSeg) + url.substring(end)
    }

    fun getCategoryDisplayLabel(categoryCode: String?): String {
        if (categoryCode.isNullOrBlank()) return "Peringatan Komunitas CallTag"
        return CATEGORY_LABELS[categoryCode.uppercase()] ?: categoryCode
    }

    /**
     * Kebijakan Auto-Block Konservatif (T-302 / OD-01):
     * Hanya memblokir panggilan secara otomatis jika:
     * 1. Preferensi Auto-Block diaktifkan oleh pengguna (opt-in, default OFF),
     * 2. Nomor bukan whitelist resmi dan bukan kode pendek darurat,
     * 3. Reputasi tepat "DANGER_SCAM" (SPAM & SUSPICIOUS hanya tampilkan notifikasi/overlay),
     * 4. Sumber data adalah "SERVER" (bukan "FIXTURE" atau "LOCAL"),
     * 5. Umur cache server <= 14 hari.
     */
    fun shouldAutoBlock(
        reputationLevel: String,
        spamScore: Double = 0.0,
        primaryCategory: String = "",
        autoBlockEnabled: Boolean = true,
        source: String = "SERVER",
        fetchedAtEpochMs: Long = System.currentTimeMillis(),
        nowEpochMs: Long = System.currentTimeMillis(),
        isWhitelisted: Boolean = false,
        phoneE164: String = ""
    ): Boolean {
        if (!autoBlockEnabled || isWhitelisted) return false
        if (phoneE164.isNotBlank() && isEmergencyOrShortCode(phoneE164)) return false
        if (!reputationLevel.equals("DANGER_SCAM", ignoreCase = true)) return false
        if (!source.equals("SERVER", ignoreCase = true)) return false
        if (fetchedAtEpochMs <= 0L) return false
        val ageMs = nowEpochMs - fetchedAtEpochMs
        return ageMs <= MAX_AUTO_BLOCK_CACHE_AGE_MS
    }
}
