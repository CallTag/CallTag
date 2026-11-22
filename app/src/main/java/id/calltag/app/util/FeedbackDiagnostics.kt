package id.calltag.app.util

/** Template berbagi hanya menerima metadata aman; tidak menerima sesi atau profil akun. */
object FeedbackDiagnostics {
    fun create(category: String, description: String, version: String, model: String, androidVersion: String): String {
        val safeDescription = description.trim()
            .replace(Regex("(?i)Bearer\\s+[^\\s;,]+"), "[token disamarkan]")
            .replace(Regex("(?i)(?:access_token|refresh_token|token)\\s*[:=]\\s*[^\\s;,]+"), "[token disamarkan]")
            .replace(Regex("(?<![\\w])\\+?\\d[\\d ().-]{6,}\\d"), "[nomor disamarkan]")
            .ifBlank { "(belum diisi)" }
        return """
            Feedback CallTag
            Kategori: $category
            Deskripsi: $safeDescription

            Diagnostik aplikasi
            Versi APK: $version
            Model perangkat: $model
            Android: $androidVersion
        """.trimIndent()
    }
}
