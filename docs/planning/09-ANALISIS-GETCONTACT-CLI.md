# 09. Analisis getcontact-cli dan strategi sumber data

Status: **temuan fakta** (sumber dan tanggal dicantumkan) dan **rekomendasi**. Bukan nasihat hukum.

## 1. Bukti yang dikumpulkan (13 November 2026)

| Fakta | Sumber | Tingkat keyakinan |
|---|---|---|
| Repo `xdreizein666/getcontact-cli`, bahasa Python, 819 bintang, dibuat 20 Agu 2026, push terakhir 24 Agu 2026, tidak diarsipkan | GitHub REST API (`api.github.com/repos/...`) | Terverifikasi langsung |
| `license: null` (tidak ada berkas lisensi) | GitHub REST API | Terverifikasi langsung |
| README menyatakan proyek riset dan pembelajaran, tidak berafiliasi dengan GetContact, melarang pengumpulan data massal dan penyalahgunaan data pribadi | README (raw.githubusercontent.com) | Terverifikasi langsung |
| Fitur: lookup nomor (nama tampilan, jumlah tag, email), daftar tag, cek kuota, lookup massal CSV, penanganan captcha pada HTTP 403, pembuatan kredensial lewat verifikasi WhatsApp, multi-akun | README | Terverifikasi langsung |
| Mekanisme rinci (negosiasi kunci Diffie-Hellman, penyimpanan `credentials.json` izin 600, peniruan pendaftaran perangkat) | Ringkasan pencarian web | **Tidak diverifikasi dengan membaca kode**; kode sumber belum ditelaah baris demi baris |
| Ketentuan layanan GetContact | - | **Belum dibaca langsung**; kesimpulan di bawah memakai asumsi bahwa klien tidak resmi umumnya melarang akses otomatis |

Catatan: README itu sendiri mengakui bahwa penggunaan melalui klien tidak resmi dapat melanggar ToS layanan terkait dan berisiko penonaktifan akun.

## 2. Analisis

| Aspek | Penilaian |
|---|---|
| Cara kerja | Meniru klien seluler untuk memanggil API privat GetContact memakai kredensial akun pengguna |
| Lisensi | **Tidak ada lisensi**: secara default hak cipta dimiliki penulis; kode tidak boleh disalin/didistribusikan ulang tanpa izin. Jangan menyalin kode ke CallTag |
| Autentikasi dan kredensial | Membutuhkan kredensial/token sesi akun; CallTag tidak boleh meminta, menyimpan, atau memakai kredensial Getcontact pengguna |
| Batasan akses | Kuota harian, rate limit, captcha; memicu captcha berarti pembatasan yang tidak boleh dilewati (guideline proyek ini melarang menyelesaikan/melewati captcha) |
| Dependensi eksternal | API privat tanpa kontrak; dapat berubah/ditutup kapan saja; repo berusia < 3 bulan dan satu pemelihara |
| Risiko keamanan | Kredensial plaintext lokal, impersonasi klien, risiko rantai pasok untuk dependensi tak terkelola |
| Risiko privasi/hukum | Data yang dikembalikan berasal dari buku kontak pihak ketiga tanpa persetujuan pemilik nomor; menampilkannya kepada pengguna CallTag berpotensi bertentangan dengan UU 27/2022 (persetujuan, larangan mengungkap data yang bukan miliknya) |
| Kelayakan production | **Tidak layak** |

## 3. Perbandingan opsi

| Kriteria | A. Mandiri | B. API resmi | C. Hybrid | D. CLI sebagai referensi |
|---|---|---|---|---|
| Legalitas/izin | Jelas (data sendiri) | Jelas bila kontrak ada | Bergantung sumber | Referensi saja, tanpa integrasi |
| Keamanan | Terkendali | Bergantung vendor | Campuran | Tidak berlaku |
| Privasi | Terbaik (minim data) | Perlu perjanjian pemrosesan | Perlu kajian | Tidak berlaku |
| Biaya | Hosting saja | Berbayar; Truecaller tidak menyediakan lookup nomor terbuka (hanya SDK login berbasis persetujuan pengguna) menurut pencarian web, **belum diverifikasi ke vendor** | Sedang | Nol |
| Keandalan | Tergantung volume data | Tergantung vendor | Baik bila sumber ada | - |
| Kemudahan integrasi | Sudah ada | Perlu adapter | Perlu adapter + kebijakan | - |
| Keberlanjutan | Ditentukan tim | Kontrak | Campuran | - |
| Bekerja tanpa pihak ketiga | **Ya** | Tidak | Sebagian | Ya |

## 4. Rekomendasi

1. **Produksi: Opsi A**, diperkuat kurasi staf (whitelist, direktori, kanal resmi) dan, bila ada, dataset publik berlisensi jelas.
2. **Opsi D hanya sebagai bahan studi konsep**: pelajari konsep (agregasi tag, kuota, penanganan rate limit) tanpa menyalin kode, menjalankan terhadap layanan, atau memakai kredensial.
3. **Opsi B** dipertimbangkan hanya jika ada penyedia dengan kontrak tertulis, dokumentasi publik, dan perjanjian pemrosesan data. Dirancang sebagai `provider` interface dengan cache terbatas dan atribusi sumber; **tidak dibangun sekarang**.
4. **Opsi C** menyusul Opsi B; sumber eksternal tidak pernah menimpa kurasi staf dan selalu berlabel sumber.
5. Konsekuensi jujur Opsi A: cakupan awal kecil, banyak hasil UNKNOWN. Mitigasi: fokus pada kategori scam Indonesia, kanal lapor resmi, pilot wilayah, dan komunikasi yang jelas ke pengguna.

## 5. Larangan operasional (tercatat sebagai aturan proyek)

- Tidak mengintegrasikan API tidak resmi, tidak melakukan scraping massal, tidak menyimpan kredensial pihak ketiga, tidak melewati captcha/rate limit, tidak mengimpor data kontak tanpa izin.
- Tidak ada token/kredensial pihak ketiga di repositori atau dokumen.
