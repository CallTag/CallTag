package id.calltag.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

/**
 * Token tata letak & tipografi CallTag (lihat `DESIGN.md`).
 *
 * Alasan token ini dipusatkan: seluruh layar memakai satu skala spasi 4dp, satu skala
 * radius, dan satu skala tipografi. Membuat nilai `.dp`/`.sp` ad-hoc di dalam Composable
 * adalah sumber utama inkonsistensi visual (jarak 14 vs 16 vs 18, radius 10/12/14/16),
 * sehingga semua nilai baru WAJIB diambil dari objek ini.
 *
 * `DESIGN.md` Bagian 1 menetapkan prinsip anti-slop: tanpa gradien kosmetik, tanpa
 * bayangan kabur. Pemisahan antar elemen memakai border 1dp tegas (`BorderStroke`),
 * bukan elevation. Karena itu tidak ada token shadow/elevation di sini.
 */
object CallTagSpacing {
    /** 4dp — jarak antar ikon dan label yang menempel. */
    val xxs: Dp = 4.dp

    /** 8dp — jarak internal chip, ikon ke teks. */
    val xs: Dp = 8.dp

    /** 12dp — jarak antar field di dalam satu grup. */
    val sm: Dp = 12.dp

    /** 16dp — padding tepi layar dan jarak antar kartu (standar). */
    val md: Dp = 16.dp

    /** 20dp — padding dalam kartu utama. */
    val lg: Dp = 20.dp

    /** 24dp — pemisah antar bagian besar di dalam satu kartu. */
    val xl: Dp = 24.dp

    /** 32dp — jarak antar blok besar pada layar kosong/hero. */
    val xxl: Dp = 32.dp
}

/**
 * Skala radius. `DESIGN.md` menganjurkan kontrol tegas; radius besar hanya untuk
 * elemen berbentuk pil (chip/badge) yang memang berbentuk kapsul.
 */
object CallTagRadius {
    /** 0dp — permukaan yang menempel ke tepi (strip status). */
    val none: Dp = 0.dp

    /** 8dp — kotak kecil (thumbnail, kotak ikon). */
    val sm: Dp = 8.dp

    /** 12dp — kartu input, tombol. */
    val md: Dp = 12.dp

    /** 16dp — kartu konten utama. */
    val lg: Dp = 16.dp

    /** 999dp — pil penuh untuk chip dan badge. */
    val pill: Dp = 999.dp
}

/**
 * Skala tipografi. `DESIGN.md` Bagian 3 mengatur dua keluarga font:
 * - `SansSerif` untuk teks UI (judul, body, label).
 * - `Monospace` untuk nomor telepon, skor risiko, dan kode darurat (WAJIB).
 *
 * Gunakan `CallTagDesign.monoStyle(...)` untuk semua angka tersebut; token di sini
 * hanya untuk teks umum agar ukuran tidak dikarang per layar.
 */
object CallTagType {
    /** 24sp ExtraBold — judul utama satu layar (maksimal satu per layar). */
    val display: TextUnit = 24.sp

    /** 20sp ExtraBold — judul kartu utama. */
    val titleLarge: TextUnit = 20.sp

    /** 17sp Bold — judul kartu sekunder. */
    val title: TextUnit = 17.sp

    /** 15sp Bold — judul baris/list item. */
    val subtitle: TextUnit = 15.sp

    /** 14sp Normal — body utama. */
    val body: TextUnit = 14.sp

    /** 13sp Normal — body sekunder, deskripsi. */
    val bodySmall: TextUnit = 13.sp

    /** 12sp SemiBold — label chip, metadata. */
    val label: TextUnit = 12.sp

    /** 11sp Medium — caption, timestamp. */
    val caption: TextUnit = 11.sp
}

/**
 * Ukuran target sentuh minimum sesuai `DESIGN.md` Bagian 4:
 * - Tombol aksi & chip: minimal 56dp.
 * - Tombol panggil darurat one-tap: minimal 64dp.
 */
object CallTagTouch {
    /** 44dp — target minimum yang dapat diterima untuk ikon sekunder non-kritis. */
    val compact: Dp = 44.dp

    /** 56dp — minimum seluruh tombol aksi utama dan chip kategori. */
    val action: Dp = 56.dp

    /** 64dp — tombol darurat one-tap. */
    val emergency: Dp = 64.dp
}
