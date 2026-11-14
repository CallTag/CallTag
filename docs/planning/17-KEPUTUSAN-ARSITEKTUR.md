# 17. Keputusan arsitektur dan keputusan produk (Final Disetujui)

| Metadata | Nilai |
|---|---|
| Status | **DISETUJUI (14 November 2026)** untuk menjadi pedoman implementasi; kode produk belum diubah |
| Baseline AS-IS | [ARCHITECTURE_DECISIONS](../ARCHITECTURE_DECISIONS.md) (ADR-001 s.d. ADR-011, 10 November 2026) |

Dokumen ini mengunci ADR-012 s.d. ADR-022 serta keputusan produk OD-01 s.d. OD-10 yang telah disetujui pada 14 November 2026.

---

## 1. Architecture Decision Records (ADR-012 s.d. ADR-022)

| ADR | Status | Keputusan | Alasan | Alternatif yang ditolak |
|---|---|---|---|---|
| **ADR-012** | DISETUJUI | Role MVP terdiri atas 4 peran: `Guest`, `Registered User` (akun perangkat), `Staf` (`Moderator`/`Admin`), dan `System Service`. Role *Verified Phone Owner* ditunda ke P2/Post-MVP. | Tim 2 orang; menghindari biaya gateway SMS/WhatsApp OTP berbayar pada tahap akademik. Pemilik nomor tetap difasilitasi melalui jalur sanggahan (*dispute*) berbukti. | Membangun verifikasi nomor otomatis via OTP sejak MVP. |
| **ADR-013** | DISETUJUI | Identitas pelapor menggunakan akun perangkat (`install_id` UUID + JWT server) tanpa meminta nomor telepon pribadi pengguna saat instalasi. | Prinsip minimisasi data pribadi (UU PDP No. 27/2022), menghilangkan gesekan onboarding, dan menutup celah klaim nomor tanpa verifikasi. | Registrasi nomor telepon tanpa OTP (kondisi as-is). |
| **ADR-014** | DISETUJUI | Sumber data lookup produksi menggunakan **Opsi A** (database mandiri CallTag + kurasi staf). Repositori `xdreizein666/getcontact-cli` hanya dijadikan referensi studi konsep (**Opsi D**). | Kepatuhan hukum/privasi, ketiadaan lisensi pada repo `getcontact-cli`, dan kemandirian layanan tanpa bergantung pada API tidak resmi. | Integrasi langsung `getcontact-cli` atau scraping pihak ketiga. |
| **ADR-015** | DISETUJUI | Hasil pencarian nomor menggunakan dua dimensi: `availability` (`AVAILABLE`, `NOT_FOUND`, `UNAVAILABLE`) dan `reputation_level` (`CLEAN`, `SUSPICIOUS`, `SPAM`, `DANGER_SCAM`), ditambah `verified_label`. | Nomor yang belum pernah dilaporkan (`NOT_FOUND`) atau gagal diperiksa ketika offline (`UNAVAILABLE`) tidak boleh ditampilkan sebagai "Nomor Aman". | Menyatukan ketidaktahuan ke dalam nilai `CLEAN` (kondisi as-is). |
| **ADR-016** | DISETUJUI | Sinkronisasi delta menggunakan koleksi jurnal `sync_changes` dengan nomor urut monoton (`seq`), cursor opak (`generation:seq`), dan operasi `UPSERT`/`DELETE`. | Menutup bug kehilangan data pada `offline-bundle` yang mengurutkan berdasarkan skor risiko + `limit` (temuan A06). | Mempertahankan cursor `MAX(sync_version)` pada tabel `cached_phone_tags`. |
| **ADR-017** | DISETUJUI | Panel Admin & Moderasi disajikan sebagai halaman web internal yang tertanam di backend Go (`/admin`), diamankan dengan sesi cookie `HttpOnly` dan TOTP. | Satu proses deploy, *same-origin* (tanpa risiko CORS tambahan), dan tidak menambah repositori kelima untuk tim 2 orang. | Menyimpan halaman admin di GitHub Pages publik atau membuat repo SPA terpisah. |
| **ADR-018** | DISETUJUI | Pemblokiran otomatis (*Auto-Block*) bersifat *opt-in* (default `OFF`), hanya memblokir `DANGER_SCAM` yang bersumber dari server dengan umur cache ≤ 14 hari, bukan whitelist, dan selalu *fail-open* (`allow`) jika terjadi error/timeout. | Mencegah panggilan penting salah terblokir (*false positive*) akibat cache usang atau skor `SPAM` menengah (temuan A05). | Default `ON` dan memblokir `SPAM >= 60` (kondisi as-is). |
| **ADR-019** | DISETUJUI | Pengiriman laporan wajib menyertakan `Idempotency-Key`, dilindungi *unique index* per `(reporter_user_id, idempotency_key)` serta dedup kontribusi 30 hari per `(phone_e164, reporter_user_id)`. | Menutup duplikasi dokumen laporan saat *retry* jaringan (temuan A08). | Hanya melakukan deduplikasi pada saat perhitungan skor di memori. |
| **ADR-020** | DISETUJUI | Mode produksi bersifat *fail-closed* saat startup (wajib terhubung ke MongoDB dan memiliki `JWT_SECRET` kuat ≥ 32 byte); fallback penyimpanan memori hanya diizinkan bila `APP_ENV=dev`. | Mencegah server produksi berjalan diam-diam di memori dan kehilangan seluruh laporan saat restart (temuan A09). | Fallback memori otomatis tanpa memeriksa environment (kondisi as-is). |
| **ADR-021** | DISETUJUI | Backend tetap menggunakan Go standar `net/http` + driver resmi MongoDB dengan penambahan lapisan `middleware`, `service`, dan `repository`; konsistensi data menggunakan *idempotent upsert* + rekonsiliasi agregat (tanpa wajib Replica Set di lokal). | Menghindari penulisan ulang (*rewrite*) dan memastikan backend dapat berjalan di MongoDB *standalone* lokal maupun MongoDB Atlas. | Migrasi ke framework baru atau mewajibkan transaksi multi-dokumen Replica Set di semua mesin lokal. |
| **ADR-022** | DISETUJUI | Aplikasi Android tetap menggunakan manual *dependency wiring* melalui `CallTagApplication`, menaikkan Room ke versi 2 (`exportSchema=true`, migrasi eksplisit, tabel outbox `pending_reports`), serta menambahkan `WorkManager`. | Sesuai skala proyek; menjaga ketahanan data lokal saat skema diperbarui. | Menambahkan Hilt/Dagger atau mempertahankan `fallbackToDestructiveMigration`. |

---

## 2. Keputusan Produk & Teknis (OD-01 s.d. OD-10)

Seluruh keputusan terbuka pada tahap perencanaan telah **DISETUJUI** dengan ketetapan berikut:

| ID | Topik | Keputusan Final (Disetujui 14 Nov 2026) | Implikasi Teknis |
|---|---|---|---|
| **OD-01** | **Struktur Role MVP** | **4 Role**: `Guest`, `Registered User` (perangkat), `Staf` (`Moderator`/`Admin`), `System Service`. *Verified Phone Owner* ditunda ke P2. | Fokus implementasi RBAC pada middleware Go untuk `USER`, `MOD`, dan `ADMIN`. |
| **OD-02** | **Identitas Pengguna** | **Berbasis perangkat (`install_id` UUID + JWT server)** tanpa input nomor telepon pengguna. | Field `phone` pada `/api/v1/auth/device` dibuat opsional/diabaikan; Android tidak meminta nomor telepon pengguna saat onboarding. |
| **OD-03** | **Gerbang `DANGER_SCAM`** | **Wajib konfirmasi Moderator** sebelum status nomor naik menjadi `DANGER_SCAM` (otomatis tertahan di `SPAM` dengan penanda kandidat review). | Melindungi pengguna yang mengaktifkan *Auto-Block* dari serangan *brigading* laporan palsu. |
| **OD-04** | **Distribusi & Target SDK** | **Sideload APK Akademik via GitHub Releases (`targetSdk 34`)** untuk MVP/demo kuliah. | Tidak terhambat syarat `targetSdk 36` Google Play Store; workflow rilis hanya dijalankan saat *tag* versi dibuat. |
| **OD-05** | **Wilayah Pilot & Kurator** | **7 Nomor Darurat Nasional + 5 Kota Utama** (Jakarta, Bandung, Surabaya, Semarang, Yogyakarta), dikurasi tim berdua dengan URL sumber resmi. | Setiap dokumen `emergency_directory` wajib memiliki `source_url` dan `verified_at`. |
| **OD-06** | **Hosting & Anggaran** | **Skema Rp 0 (Zero-Cost)**: Demo utama di Lokal/LAN (`adb reverse` / `10.0.2.2`), staging opsional di MongoDB Atlas Free Tier (`M0`) + GitHub Pages. | Bebas biaya bulanan dan bebas ketergantungan internet eksternal saat sidang/demo. |
| **OD-07** | **Endpoint Lookup Nomor** | **Pertahankan `GET /api/v1/phones/{e164}`** + masking nomor di log (`+62812****0005`) + matikan log HTTP di APK release. | Kompatibel penuh dengan kode Android, Website, dan OpenAPI yang sudah ada. |
| **OD-08** | **Konsistensi MongoDB** | **Idempotent Upsert + Rekonsiliasi Agregat** dari koleksi `spam_reports` (tanpa wajib Replica Set di lokal). | Berjalan langsung pada instalasi `mongodb` lokal standar di Linux tanpa konfigurasi cluster. |
| **OD-09** | **SLA Moderasi** | **Indikator target di dashboard Admin**: Dispute ≤ 7 hari, Laporan tertahan ≤ 3 hari. | Ditampilkan sebagai lencana peringatan pada antrean `/admin/cases`. |
| **OD-10** | **Dispute oleh Guest** | **Diizinkan** melalui aplikasi maupun web dengan wajib mengisi alasan + kontak balik, dibatasi *rate limit* 3x/24 jam, dan diberikan `tracking_token`. | Korban salah lapor yang belum menginstal aplikasi tetap dapat mengajukan sanggahan dan memantau statusnya. |

---

## 3. Pembagian Peran Tim Pengembang (2 Orang)

| Peran | Cakupan Repositori | Fokus Tanggung Jawab |
|---|---|---|
| **Pengembang A** | `backend` | Autentikasi JWT & RBAC (T-101..T-103), Idempotency & Dispute Persistence (T-104..T-105), Scoring & Sync Journal (T-201..T-202), Panel Admin & Moderasi (T-401..T-404), Pengujian Go (T-601, T-604). |
| **Pengembang B** | `CallTag`, `apidocs`, `calltag.github.io` | Room v2 & Outbox (T-301), Lookup Jujur & Auto-Block Konservatif (T-302..T-303), WorkManager Sync & Onboarding (T-304..T-305), Penyelarasan OpenAPI & Website (T-501..T-503), Pengujian Android (T-603, T-605). |
