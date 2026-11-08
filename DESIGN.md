# CallTag Indonesia: Design System & Visual Direction (`DESIGN.md`)

## 1. Identitas, Mood, & Karakter Produk
- **Archetype**: *High-Trust Civic Utility* (Utilitas Keamanan Sipil & Kesiapsiagaan Darurat Indonesia).
- **Karakter Utama**: Tegas, bersih, kontras tinggi (*high-contrast*), dan instan terbaca dalam waktu `< 1 detik` saat telepon asing berdering atau ketika warga dalam kondisi darurat mencari nomor Damkar, Kepolisian, atau Rumah Sakit terdekat.
- **Prinsip Anti-Slop**:
  - Tanpa gradien kosmetik biru-ungu, tanpa *glassmorphism* berlebihan, tanpa warna neon tanpa fungsi, dan tanpa bayangan kabur (*soft drop shadow*).
  - Setiap warna di layar memiliki arti semantik yang terikat langsung pada tingkat bahaya nomor (`reputation_level`) atau fungsi penyelamatan darurat.
  - Pemisahan antarelemen menggunakan garis border tegas `1dp`, bukan efek *floating shadow*.
- **Dials**:
  - `ENERGY`: `2` (Tegas, fokus pada hierarki sinyal bahaya dan kesiapsiagaan darurat)
  - `RHYTHM`: `2` (Bervariasi sesuai fungsi modul: Grid Cepat untuk Darurat 112/113, Panel Terstruktur untuk Hasil Lookup, dan Kartu Panduan Langkah-demi-Langkah untuk Pusat Lapor Resmi)
  - `MOTION`: `1` (Fungsional dan instan tanpa animasi dekoratif yang memperlambat respons darurat)

---

## 2. Sistem Warna (Adaptive Light/Dark + 4-Tier Semantic Reputation)

### 2.1 Permukaan & Brand Utama — Nusantara Teal
Brand CallTag adalah **Nusantara Teal**, bukan biru korporat. Disinkronkan dengan
`ui/theme/CallTagTheme.kt`:

| Token | Light Mode | Dark Mode | Kegunaan & Alasan Hierarki |
| :--- | :--- | :--- | :--- |
| `BackgroundPrimary` | `#F7F6F2` (Alabaster hangat) | `#0C1113` (Tinta kehijauan) | Latar layar utama; hangat di terang, gelap netral di malam hari. |
| `SurfaceCard` | `#FFFFFF` | `#151C1F` | Permukaan kartu informasi; satu tingkat di atas latar. |
| `SurfaceSubtle` | `#EFEEE9` | `#1E2629` | Kolom input, chip, blok sekunder. |
| `BorderSubtle` | `#E3E1DA` (`1dp`) | `#283234` (`1dp`) | Garis pemisah struktural pengganti *drop shadow*. |
| `BorderStrong` | `#C9C6BC` | `#3B484B` | Border elemen terpilih/tekan (switch, tiang input). |
| `TextPrimary` | `#14181A` | `#F1F4F4` | Teks utama, kontras di atas `#FFFFFF`/`#151C1F` ≥ AA. |
| `TextSecondary` | `#5B6469` | `#9BA8AB` | Teks sekunder, metadata, label. |
| `BrandPrimary` | `#0F766E` (Teal Nusantara Dalam) | `#2DD4BF` (Teal Nusantara Terang) | **Warna teks/awalan/ikon** — fungsi #Tag, navigasi aktif, skor aman. |
| `BrandContainer` | `#E2F1EE` | `#11312E` | Latar chip/badge yang menampung `BrandPrimary`. |
| `OnBrand` | `#FFFFFF` | `#06231F` | Warna konten di atas latar `BrandSolid`. |
| `BrandPressed` | `#0B5A54` | `#14B8A6` | Ripple/tekan tombol brand. |

> **Aturan peran (audit kontras):** token `brandPrimary`/`dangerScam`/`cleanEmerald`
> hanyalah warna **teks/awalan** — di mode gelap mereka dibuat terang agar tetap terbaca.
> Latar tombol WAJIB memakai varian **solid**: `brandSolid`, `dangerSolid` (lihat 2.2),
> yang selalu cukup gelap untuk menampung teks terang dengan kontras AA. Mencampur peran
> menghasilkan teal-di-atas-teal atau salmon-di-atas-putih yang tak terbaca.

### 2.2 Sistem Warna Reputasi 4-Tingkat (PRD Bagian 4.2)
Digunakan secara konsisten pada **Caller ID Overlay**, **Badge Status**, **Indikator Skor Risiko (`0-100`)**, dan **Riwayat Panggilan**. Pasangan `warna → container` mengikat:

| Level Reputasi (`reputation_level`) | Rentang Skor ($S$) | Light Mode | Dark Mode | Perilaku Visual |
| :--- | :--- | :--- | :--- | :--- |
| **`DANGER_SCAM`** | `S >= 70` (min. 5 pelapor) | `#C4271F` → `#FCEBE9` | `#FF6B5E` → `#3A1512` | Pita/badge/banner merah untuk bahaya kritis (Pinjol Ilegal, File APK, CS Palsu). |
| **`SPAM`** | `40 <= S < 70` | `#C0561B` → `#FBEFE7` | `#FB923C` → `#3A1E0C` | Banner & indikator oranye untuk telemarketing agresif / judi online. |
| **`SUSPICIOUS`** | `15 <= S < 40` | `#9A6A0B` → `#FAF2DE` | `#F2C14E` → `#362A0B` | Banner amber "Dilaporkan Komunitas: Hati-hati". |
| **`CLEAN` / Verified Official** | `S < 15` / `is_whitelisted_verified` | `#0F766E` → `#E2F1EE` | `#2DD4BF` → `#11312E` | Chip hijau-teal untuk nomor bersih/resmi (Whitelist Shield). Sejajar dengan `brandPrimary`. |

**Latar tombol** (tombol aksi utama & darurat) memakai token solid:
- `brandSolid`: `#0F766E` (light) / `#2DD4BF` gelap → konten `onBrand` (putih / `#06231F`).
- `dangerSolid`: `#B3261E` (light) / `#B91C1C` (dark) → konten putih (kontras ≥ 5.7:1).

**Tombol darurat nasional** tetap memakai `dangerSolid` + `FontFamily.Monospace` untuk
kode (112/113/118/119). Kontras pasangan di atas sudah diperiksa; pertukaran kombinasi
di luar tabel ini kemungkinan besar **gagal memenuhi AA** — ukur ulang sebelum dipakai.

---

## 3. Tipografi & Keterbacaan Angka
1. **UI, Judul, & Body (`FontFamily.SansSerif`)**:
   - Karakter geometris bersih dengan bobot tegas (`SemiBold` / `Bold` untuk judul modul dan nama instansi darurat; `Normal` / `Medium` untuk panduan pelaporan).
2. **Nomor Telepon, Skor Risiko, & Kode Darurat (`FontFamily.Monospace`)**:
   - Seluruh tampilan `phone_e164` (`+62812...`), `risk_score` (`84.5/100`), dan nomor pendek darurat (`112`, `110`, `113`, `118/119`, `115`, `123`, `14080`) WAJIB menggunakan `FontFamily.Monospace` dengan *letter-spacing* lega (`0.5.sp` - `1.sp`) agar setiap digit angka terbaca jelas tanpa risiko salah ketik atau salah baca.

---

## 4. Ergonomi & Aturan Komponen
1. **Target Sentuh (*Touch Targets*)**:
   - Seluruh tombol aksi utama, chip kategori taksonomi scam, dan tombol panggil lokal wajib memiliki tinggi minimal `56.dp`.
   - Tombol **One-Tap Direct Dial Nomor Darurat Nasional** wajib memiliki tinggi minimal `64.dp` dan ditempatkan pada area yang mudah dijangkau ibu jari.
2. **Kelengkapan Status UI (*UI States - R-27*)**:
   - Setiap layar yang memuat data wajib menampilkan status **Loading**, **Empty**, dan **Error / Offline Cache Active** secara jujur dan jelas.
3. **Kejujuran Fungsional (*Functional Completeness - C-2 & R-26*)**:
   - Setiap tombol di layar wajib menjalankan aksi nyata: memicu panggilan `Intent.ACTION_DIAL` (`tel:`), membuka WhatsApp posko Damkar/OJK (`https://wa.me/...`) dengan teks template darurat, menyalin nomor pelaku ke *clipboard*, membuka portal resmi (`https://aduannomor.id`, `https://iasc.ojk.go.id`, dll.), atau mengeksekusi operasi Room DB + Retrofit API.
