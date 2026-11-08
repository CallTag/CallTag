package id.calltag.app.util

object PhoneNormalizer {

    private val CATEGORY_LABELS = mapOf(
        "PINJOL_DC" to "Pinjol Ilegal & Debt Collector Meresahkan",
        "APK_SNIFFING" to "Penipuan Modus File APK (Kurir/Undangan/Tilang)",
        "IMPERSONATION" to "Impersonasi / CS Palsu (Bank/E-Wallet/Pajak)",
        "JUDOL" to "Promosi Judi Online (Judol)",
        "SOCIAL_ENG" to "Penipuan Sosial / Hipnotis Telepon",
        "TELEMARKETING" to "Telemarketing / Spam Promosi Agresif"
    )

    private val HIGH_RISK_CATEGORIES = setOf(
        "PINJOL_DC",
        "APK_SNIFFING",
        "IMPERSONATION",
        "JUDOL",
        "SOCIAL_ENG"
    )

    /**
     * Menormalisasi nomor telepon Indonesia dari berbagai format (misal "0812-3456-7890",
     * "6281234567890", atau URI "tel:0812...") menjadi format E.164 ("+6281234567890").
     * Nomor darurat pendek (3-6 digit seperti 112, 110, 113, 119, 14080) dipertahankan.
     */
    fun normalizeToE164(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""
        val cleaned = rawNumber.removePrefix("tel:").trim()
        val digits = buildString {
            for (ch in cleaned) {
                if (ch.isDigit()) append(ch)
            }
        }
        if (digits.isEmpty()) return ""
        if (digits.length <= 6) return digits

        return when {
            digits.startsWith("0") -> "+62" + digits.substring(1)
            digits.startsWith("62") -> "+$digits"
            digits.startsWith("8") -> "+62$digits"
            else -> "+$digits"
        }
    }

    fun getCategoryDisplayLabel(categoryCode: String?): String {
        if (categoryCode.isNullOrBlank()) return "Peringatan Komunitas CallTag"
        return CATEGORY_LABELS[categoryCode.uppercase()] ?: categoryCode
    }

    /**
     * Menentukan apakah sebuah nomor memenuhi kriteria blokir otomatis (Auto-Block Scammer):
     * - Status DANGER_SCAM atau SPAM dengan skor >= 60.0
     * - Atau memiliki kategori berisiko tinggi (Pinjol Ilegal, Penipuan APK, CS Palsu, Judol, Hipnotis) dengan skor >= 50.0
     */
    fun shouldAutoBlock(
        reputationLevel: String,
        spamScore: Double,
        primaryCategory: String
    ): Boolean {
        val level = reputationLevel.uppercase()
        val category = primaryCategory.uppercase()
        if (level == "DANGER_SCAM") return true
        if (level == "SPAM" && spamScore >= 60.0) return true
        if (category in HIGH_RISK_CATEGORIES && spamScore >= 50.0) return true
        return false
    }
}
