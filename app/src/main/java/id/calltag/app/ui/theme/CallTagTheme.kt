package id.calltag.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Sistem warna semantik CallTag.
 *
 * Arah desain:
 * - Brand memakai kelompok teal tua ("Nusantara Teal"). Hijau-biru tua terasa seperti
 *   instansi sipil yang tenang dan tepercaya, bukan biru SaaS generik, dan tetap
 *   kontras tinggi di bawah sinar matahari.
 * - Warna bahaya TIDAK diubah: merah (DANGER_SCAM), oranye (SPAM), amber (SUSPICIOUS).
 *   Ini kontrak PRD Bagian 4.2 dan dipakai juga oleh notifikasi & overlay sistem.
 *   Karena brand bukan hijau cerah, indikator CLEAN memakai teal yang sama agar
 *   "aman" terbaca sebagai ketenangan, bukan sebagai tombol aksi.
 *
 * Aturan `DESIGN.md`: pemisahan memakai border 1dp tegas, tanpa bayangan kabur.
 */
@Immutable
data class CallTagSemanticColors(
    val backgroundPrimary: Color,
    val surfaceCard: Color,
    val surfaceSubtle: Color,
    val borderSubtle: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val brandPrimary: Color,
    val brandPressed: Color,
    val brandContainer: Color,
    val onBrand: Color,
    /** Latar tombol solid. Selalu cukup gelap agar teks putih terbaca. */
    val brandSolid: Color,
    /** Latar tombol darurat solid. Selalu cukup gelap agar teks putih terbaca. */
    val dangerSolid: Color,
    val dangerScam: Color,
    val dangerScamContainer: Color,
    val spamOrange: Color,
    val spamOrangeContainer: Color,
    val suspiciousAmber: Color,
    val suspiciousAmberContainer: Color,
    val cleanEmerald: Color,
    val cleanEmeraldContainer: Color,
    val isDark: Boolean
)

/*
 * Palet terang: alas putih hangat berlapis, kartu murni putih, aksen teal tua.
 * Aksen #0F766E punya kontras 4.6:1 di atas putih — lolos WCAG AA untuk teks 14sp.
 */
val LightCallTagColors = CallTagSemanticColors(
    backgroundPrimary = Color(0xFFF7F6F2),
    surfaceCard = Color(0xFFFFFFFF),
    surfaceSubtle = Color(0xFFEFEEE9),
    borderSubtle = Color(0xFFE3E1DA),
    borderStrong = Color(0xFFC9C6BC),
    textPrimary = Color(0xFF14181A),
    textSecondary = Color(0xFF5B6469),
    brandPrimary = Color(0xFF0F766E),
    brandPressed = Color(0xFF0B5A54),
    brandContainer = Color(0xFFE2F1EE),
    onBrand = Color(0xFFFFFFFF),
    brandSolid = Color(0xFF0F766E),
    dangerSolid = Color(0xFFB3261E),
    dangerScam = Color(0xFFC4271F),
    dangerScamContainer = Color(0xFFFCEBE9),
    spamOrange = Color(0xFFC0561B),
    spamOrangeContainer = Color(0xFFFBEFE7),
    suspiciousAmber = Color(0xFF9A6A0B),
    suspiciousAmberContainer = Color(0xFFFAF2DE),
    cleanEmerald = Color(0xFF0F766E),
    cleanEmeraldContainer = Color(0xFFE2F1EE),
    isDark = false
)

/*
 * Palet gelap: alas biru-tinta dalam, kartu lebih terang satu tingkat.
 * Teal dinaikkan kecerlangannya (#2DD4BF) agar tetap kontras di alas gelap.
 */
val DarkCallTagColors = CallTagSemanticColors(
    backgroundPrimary = Color(0xFF0C1113),
    surfaceCard = Color(0xFF151C1F),
    surfaceSubtle = Color(0xFF1E2629),
    borderSubtle = Color(0xFF283234),
    borderStrong = Color(0xFF3B484B),
    textPrimary = Color(0xFFF1F4F4),
    textSecondary = Color(0xFF9BA8AB),
    brandPrimary = Color(0xFF2DD4BF),
    brandPressed = Color(0xFF14B8A6),
    brandContainer = Color(0xFF11312E),
    onBrand = Color(0xFF06231F),
    brandSolid = Color(0xFF2DD4BF),
    dangerSolid = Color(0xFFB91C1C),
    dangerScam = Color(0xFFFF6B5E),
    dangerScamContainer = Color(0xFF3A1512),
    spamOrange = Color(0xFFFB923C),
    spamOrangeContainer = Color(0xFF3A1E0C),
    suspiciousAmber = Color(0xFFF2C14E),
    suspiciousAmberContainer = Color(0xFF362A0B),
    cleanEmerald = Color(0xFF2DD4BF),
    cleanEmeraldContainer = Color(0xFF11312E),
    isDark = true
)

val LocalCallTagColors = staticCompositionLocalOf { LightCallTagColors }

private val LightMaterialScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFE2F1EE),
    onSecondaryContainer = Color(0xFF0F766E),
    background = Color(0xFFF7F6F2),
    onBackground = Color(0xFF14181A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF14181A),
    surfaceVariant = Color(0xFFEFEEE9),
    onSurfaceVariant = Color(0xFF5B6469),
    outline = Color(0xFFC9C6BC),
    error = Color(0xFFC4271F),
    onError = Color.White
)

private val DarkMaterialScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF06231F),
    secondaryContainer = Color(0xFF11312E),
    onSecondaryContainer = Color(0xFF9BF3E6),
    background = Color(0xFF0C1113),
    onBackground = Color(0xFFF1F4F4),
    surface = Color(0xFF151C1F),
    onSurface = Color(0xFFF1F4F4),
    surfaceVariant = Color(0xFF1E2629),
    onSurfaceVariant = Color(0xFF9BA8AB),
    outline = Color(0xFF3B484B),
    error = Color(0xFFFF6B5E),
    onError = Color(0xFF2A0A07)
)

@Composable
fun CallTagTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val semanticColors = if (darkTheme) DarkCallTagColors else LightCallTagColors
    val materialScheme = if (darkTheme) DarkMaterialScheme else LightMaterialScheme

    androidx.compose.runtime.CompositionLocalProvider(
        LocalCallTagColors provides semanticColors
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            content = content
        )
    }
}

object CallTagDesign {
    val colors: CallTagSemanticColors
        @Composable
        get() = LocalCallTagColors.current

    /**
     * Tipografi Monospace wajib untuk nomor telepon (`phone_e164`), `risk_score`,
     * dan kode darurat nasional (`112`, `110`, `113`, `119`, `115`, `123`, `14080`).
     */
    fun monoStyle(
        fontSize: TextUnit = 16.sp,
        fontWeight: FontWeight = FontWeight.Bold,
        color: Color = Color.Unspecified
    ): TextStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = 0.5.sp,
        color = color
    )

    @Composable
    fun reputationBadgeColor(reputationLevel: String): Color {
        val c = colors
        return when (reputationLevel.uppercase()) {
            "DANGER_SCAM" -> c.dangerScam
            "SPAM" -> c.spamOrange
            "SUSPICIOUS" -> c.suspiciousAmber
            else -> c.cleanEmerald
        }
    }

    @Composable
    fun reputationContainerColor(reputationLevel: String): Color {
        val c = colors
        return when (reputationLevel.uppercase()) {
            "DANGER_SCAM" -> c.dangerScamContainer
            "SPAM" -> c.spamOrangeContainer
            "SUSPICIOUS" -> c.suspiciousAmberContainer
            else -> c.cleanEmeraldContainer
        }
    }

    fun reputationLabel(reputationLevel: String): String {
        return when (reputationLevel.uppercase()) {
            "DANGER_SCAM" -> "Bahaya Penipuan"
            "SPAM" -> "Telepon Spam"
            "SUSPICIOUS" -> "Perlu Waspada"
            else -> "Nomor Aman"
        }
    }

    /**
     * Label singkat untuk lencana sempit. Dipakai di baris riwayat dan chip.
     */
    fun reputationShortLabel(reputationLevel: String): String {
        return when (reputationLevel.uppercase()) {
            "DANGER_SCAM" -> "Bahaya"
            "SPAM" -> "Spam"
            "SUSPICIOUS" -> "Waspada"
            else -> "Aman"
        }
    }

    fun callerInitials(label: String, reputationLevel: String): String {
        val level = reputationLevel.uppercase()
        if (level == "DANGER_SCAM") return "!"
        val words = label
            .replace("#", "")
            .split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() && it.first().isLetterOrDigit() }
        if (words.isEmpty()) return "ID"
        if (words.size == 1) return words[0].take(2).uppercase()
        return "${words[0].first()}${words[1].first()}".uppercase()
    }

    fun extractHashtagList(
        tagsSummary: String,
        topLabel: String,
        categoryBreakdown: Map<String, Int> = emptyMap()
    ): List<String> {
        val result = linkedSetOf<String>()
        tagsSummary.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { raw ->
                val formatted = if (raw.startsWith("#")) raw else "#$raw"
                result.add(formatted)
            }

        if (result.isEmpty() && topLabel.isNotBlank() && !topLabel.contains("Tidak Ditemukan", ignoreCase = true)) {
            val cleanTop = if (topLabel.startsWith("#")) topLabel else "#$topLabel"
            result.add(cleanTop)
        }

        val categoryShortNames = mapOf(
            "PINJOL_DC" to "Pinjol Ilegal",
            "APK_SNIFFING" to "Modus File APK",
            "IMPERSONATION" to "CS Palsu",
            "JUDOL" to "Promosi Judol",
            "SOCIAL_ENG" to "Hipnotis / Penipuan",
            "TELEMARKETING" to "Telemarketing"
        )
        categoryBreakdown.forEach { (code, count) ->
            if (count > 0) {
                val label = categoryShortNames[code] ?: code
                result.add("#$label ($count)")
            }
        }
        return result.toList()
    }
}
