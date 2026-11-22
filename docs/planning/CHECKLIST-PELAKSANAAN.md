# Checklist Pelaksanaan & Kumpulan Prompt Implementasi CallTag

Panduan penggunaan dokumen ini:
- Centang `[x]` setiap kali satu pekerjaan selesai dan terverifikasi.
- Label pelaksana:
  - **[ANDA]** = Keputusan atau aksi manual yang dilakukan oleh Anda/tim (misal: colok HP fisik, buat file `.env`, review PR).
  - **[AI]** = Pekerjaan koding/pengujian yang bisa langsung didelegasikan ke AI menggunakan **Prompt Siap Pakai** di setiap tahap.
  - **[KEDUANYA]** = AI menyiapkan kode/skrip, Anda meninjau dan menjalankannya.
- Kode tugas (`T-xxx`) merujuk penuh ke [14-ROADMAP-PENGEMBANGAN.md](14-ROADMAP-PENGEMBANGAN.md).

---

## Tahap 0. Screening, Audit, & Perencanaan Final (SELESAI)

- [x] **Screening awal workspace**: Memeriksa struktur 4 repositori (`CallTag`, `backend`, `apidocs`, `calltag.github.io`), kesiapan compiler Go/Gradle/Node, dan status service MongoDB lokal.
- [x] **Validasi baseline audit**: Memastikan temuan keamanan/integrasi A01 s.d. A10 dari laporan audit 10 November 2026 menjadi dasar perbaikan tanpa mengulang pekerjaan dari nol.
- [x] **Riset `getcontact-cli`**: Mengevaluasi aspek lisensi, cara kerja, dan risiko UU PDP; ditetapkan hanya sebagai referensi konsep (Opsi D), bukan dependensi produksi.
- [x] **Penyusunan 18 dokumen Master Plan**: Membuat dokumen `00` s.d. `17` di dalam folder `docs/planning/`.
- [x] **Pembaruan Master PRD v1.2.0**: Menambahkan Bagian 10 pada [PRD.md](../../PRD.md) berisi spesifikasi keamanan (`SEC`), data (`DATA`), API (`API`), dan UI/UX (`UX`).
- [x] **Pembuatan & validasi 17 Diagram Sistem**: Menyusun seluruh diagram arsitektur dan alur kerja dengan Mermaid di [13-DIAGRAM-SISTEM.md](13-DIAGRAM-SISTEM.md) (17/17 lulus uji render).
- [x] **Commit lokal perencanaan**: Tersimpan di branch `docs/master-plan`.

---

## Tahap 1. Persetujuan & Penyelarasan Branch (`T-001` s.d. `T-004`)

- [x] **[ANDA] Baca dokumen ringkasan & keputusan**: Meninjau [00-RINGKASAN-EKSEKUTIF.md](00-RINGKASAN-EKSEKUTIF.md) dan [17-KEPUTUSAN-ARSITEKTUR.md](17-KEPUTUSAN-ARSITEKTUR.md).
- [x] **[ANDA] Kunci OD-01 (Role MVP)**: Disetujui 4 role (`Guest`, `Registered User`, `Staf Moderator/Admin`, `System Service`); role *Verified Phone Owner* ditunda ke P2.
- [x] **[ANDA] Kunci OD-02 (Identitas Perangkat)**: Disetujui memakai `install_id` UUID + JWT server tanpa meminta nomor telepon pribadi pengguna saat instalasi.
- [x] **[ANDA] Kunci OD-03 (Gerbang `DANGER_SCAM`)**: Disetujui wajib melalui konfirmasi Moderator sebelum nomor naik ke status `DANGER_SCAM` agar aman dari serangan *brigading*.
- [x] **[ANDA] Kunci OD-04 (Distribusi APK)**: Disetujui fokus pada Sideload APK Akademik via GitHub Releases (`targetSdk 34`).
- [x] **[ANDA] Kunci OD-05 (Wilayah Pilot)**: Disetujui mencakup 7 Nomor Nasional + 5 Kota Utama (Jakarta, Bandung, Surabaya, Semarang, Yogyakarta) dengan sumber resmi.
- [x] **[ANDA] Kunci OD-06 (Skema Hosting Rp 0)**: Disetujui demo utama berjalan di Lokal/LAN (`10.0.2.2` / `adb reverse`) dan cloud opsional memakai MongoDB Atlas Free Tier + GitHub Pages.
- [x] **[ANDA] Kunci OD-07 s.d. OD-10**: Disetujui pertahankan `GET /phones/{e164}` dengan masking log, gunakan *Idempotent Upsert* + rekonsiliasi tanpa wajib Replica Set lokal, tampilkan indikator SLA di dashboard admin, dan izinkan *Guest* mengajukan *dispute* dengan kuota ketat.
- [x] **[ANDA] Kunci pembagian tugas 2 orang**: Pengembang A memegang `backend`; Pengembang B memegang `CallTag`, `apidocs`, dan `calltag.github.io`.
- [x] **[AI] Finalisasi dokumen keputusan & PRD (`T-001`, `T-002`, `T-003`)**: Mengunci seluruh keputusan di atas ke dalam dokumen perencanaan.
- [ ] **[KEDUANYA] Merge branch dokumentasi (`T-004a`)**: Menggabungkan branch `docs/master-plan` ke branch kerja utama di repositori `CallTag` agar menjadi acuan bersama.
- [ ] **[KEDUANYA] Promosi feature branch ke `main` (`T-004b`)**: Membuka dan me-merge Pull Request dari branch pengembangan (`codex/workspace-initial-import` pada `backend` & `apidocs`, serta `codex/frontend-mobile-refresh` pada `calltag.github.io`) ke branch `main` masing-masing repo karena `main` saat ini masih kosong/kerangka.

---

## Tahap 2. Persiapan Lingkungan Kerja Lokal

- [ ] **[ANDA] Pastikan service MongoDB & Emulator siap**: Jalankan `systemctl is-active mongodb` (pastikan `active`) dan pastikan AVD `Pixel_5_Light` dapat menyala normal.
- [ ] **[ANDA] Siapkan file environment backend**: Buat variabel environment lokal (`MONGOSTRING`, `DB_NAME="calltag_db"`, `APP_ENV="dev"`, `JWT_SECRET` minimal 32 karakter acak, `HASH_SALT`) agar tidak memakai secret bawaan kode.
- [ ] **[ANDA] Siapkan 1 HP Android fisik (opsional untuk tahap akhir)**: Digunakan khusus pada Tahap 8 (`T-605`) guna menguji panggilan masuk seluler nyata dan `adb reverse tcp:8080 tcp:8080`.

---

## Tahap 3. Fase 1: Keamanan & Integritas Data Backend (Pemilik A: Repo `backend`)

Tujuan: Menutup celah keamanan P0 pada server Go (secret hardcoded, token rusak dianggap guest, laporan ganda, dan dispute yang belum tersimpan ke database).

- [x] **[AI] `T-102` Wajibkan `JWT_SECRET` & `HASH_SALT` dari environment** (`helper/auth.go`, `config/db.go`): Hapus fallback secret statis pada mode produksi (`APP_ENV=prod`); server wajib gagal start (*fail-closed*) bila secret kosong atau kurang dari 32 byte.
- [x] **[AI] `T-107` Pisahkan endpoint `/healthz` dan `/readyz`** (`controller/sync.go`, `config/db.go`, `route/route.go`): `/healthz` untuk cek proses hidup, sedangkan `/readyz` melakukan `Ping` berbatas waktu ke MongoDB (kembalikan `503` jika MongoDB mati, bukan `200` palsu). Batasi fallback memori hanya untuk `APP_ENV=dev`.
- [x] **[AI] `T-101` Perketat autentikasi JWT & larang Guest melapor** (`helper/auth.go`, `controller/auth.go`, `controller/report.go`): Registrasi `/api/v1/auth/device` berbasis `install_id` (tanpa wajib nomor telepon). Jika header `Authorization` ada tetapi token rusak/kedaluwarsa, kembalikan `401 Unauthorized` (jangan diubah jadi Guest). Tolak `POST /api/v1/reports` tanpa token User yang valid.
- [x] **[AI] `T-103` Kunci identitas pelapor dari token & pasang Rate Limit** (`controller/report.go`, `config/db.go`): Ambil identitas pelapor murni dari klaim `sub` di JWT (abaikan header `X-Device-Id` agar tidak bisa dipalsukan). Terapkan kuota maksimal 10 laporan/24 jam dan 3 laporan/5 menit per akun (`429 Too Many Requests`).
- [x] **[AI] `T-104` Terapkan `Idempotency-Key` & deduplikasi laporan 30 hari** (`model/model.go`, `config/db.go`, `controller/report.go`): Wajibkan header `Idempotency-Key` pada `POST /reports`, pasang *unique index* di MongoDB, kembalikan `_id` asli sebagai `report_id`, dan pastikan pengiriman ulang paket yang sama tidak menambah dokumen ganda.
- [x] **[AI] `T-105` Simpan Sanggahan (*Dispute*) ke MongoDB sebelum respons `201`** (`model/model.go`, `config/db.go`, `controller/report.go`): Buat koleksi `moderation_cases` untuk menyimpan pengajuan `POST /api/v1/disputes` (termasuk dari Guest dengan batas 3x/24 jam), kembalikan `dispute_id` persisten beserta `tracking_token`, dan sediakan `GET /api/v1/disputes/{id}`.
- [x] **[AI] `T-106` Sanitasi PII pada komentar & redaksi DTO laporan publik** (`controller/lookup.go`, `helper/scoring.go`): Hilangkan `reporter_user_id` dari respons publik `GET /api/v1/phones/{e164}/reports`, saring pola NIK/nomor rekening/nomor telepon di komentar, dan samarkan nomor telepon pada log server (`+62812****0005`).
- [x] **[ANDA] Verifikasi Tahap 3**: Jalankan `cd backend && go test -v ./...` dan pastikan kasus uji negatif (token rusak = 401, kirim 2x kunci sama = 1 dokumen, dispute tersimpan di Mongo) semuanya `PASS`.

> **Prompt Siap Pakai — Tahap 3 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 3 (Fase 1 Backend: T-102, T-107, T-101, T-103, T-104, T-105, T-106) pada repositori `backend` sesuai spesifikasi di `CallTag/docs/planning/10-KEAMANAN-DAN-PRIVASI.md`, `06-DESAIN-DATABASE.md`, dan `07-KONTRAK-API.md`.
> Ketentuan wajib:
> 1. Gunakan pendekatan TDD: tulis pengujian gagal terlebih dahulu di `backend/main_test.go` untuk memverifikasi kasus T09 (idempotency), T10 (invalid bearer -> 401), T11 (rate limit & anti-Sybil), T12 (dispute persistence di MongoDB/memory), dan T20 (/readyz 503 saat DB putus).
> 2. Pastikan kompatibilitas mundur pada envelope JSON `/api/v1` dan tidak merusak endpoint existing.
> 3. Jalankan `go vet ./...` dan `go test -v ./...` sampai seluruh test PASS, lalu centang item Tahap 3 yang selesai di `CallTag/docs/planning/CHECKLIST-PELAKSANAAN.md`.
> ```

---

## Tahap 4. Fase 2: Scoring Engine, Sync Journal, & Endpoint Akun (Pemilik A: Repo `backend`)

Tujuan: Memperbaiki algoritma perhitungan skor reputasi, mengganti sinkronisasi yang suka melewatkan data dengan jurnal berurutan (`sync_changes`), dan menyiapkan manajemen akun.

- [x] **[AI] `T-202` Perbaiki `ScoringService` & gerbang moderator `DANGER_SCAM`** (`helper/scoring.go`, `config/db.go`): Batasi kontribusi maksimal 1 kali per pelapor unik per nomor, batasi `trust_weight` di rentang `0.25..1.5`, syaratkan minimal 2 pelapor unik (`N >= 2`) untuk naik ke `SUSPICIOUS`, dan tahan kandidat `DANGER_SCAM` di level `SPAM` sampai disetujui moderator (OD-03). Tambahkan unit test tabel `U1` s.d. `U8`.
- [x] **[AI] `T-201` Bangun jurnal `sync_changes` & endpoint `GET /api/v1/sync/changes`** (`model/model.go`, `config/db.go`, `controller/sync.go`): Setiap perubahan pada `phone_tags` atau `emergency_directory` menulis nomor urut monoton `seq` (`UPSERT`/`DELETE`). Sediakan paginasi dengan cursor opak `generation:seq` agar tidak ada pembaruan yang terlewat saat sinkronisasi.
- [x] **[AI] `T-204` Tambahkan endpoint profil, refresh token, & hapus akun** (`controller/auth.go`, `route/route.go`): Implementasikan `POST /api/v1/auth/refresh`, `GET /api/v1/me` (cek sisa kuota laporan), dan `DELETE /api/v1/me` (hak penghapusan/anonimisasi data pelapor sesuai UU PDP).
- [x] **[AI] `T-203` Rapikan struktur internal backend & middleware** (`route/route.go`, `helper/`): Pisahkan middleware logging teredaksi, CORS origin terdaftar, dan batas ukuran request body maksimal 16 KB.
- [x] **[AI] `T-205` & `T-206` Rekonsiliasi agregat & peluruhan skor (*decay*)**: Pastikan perhitungan ulang agregat `phone_tags` dari `spam_reports` bersifat idempoten (berjalan di MongoDB standalone tanpa wajib Replica Set sesuai OD-08) dan dukung kadaluwarsa data 12 bulan.
- [x] **[ANDA] Verifikasi Tahap 4**: Jalankan `cd backend && go test -v ./...` dan pastikan seluruh test tabel scoring `U1..U8` serta paginasi `sync/changes` lulus.

> **Prompt Siap Pakai — Tahap 4 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 4 (Fase 2 Backend: T-202, T-201, T-204, T-203, T-205, T-206) pada repositori `backend` sesuai spesifikasi di `CallTag/docs/planning/03-SPESIFIKASI-FITUR.md`, `06-DESAIN-DATABASE.md`, dan `07-KONTRAK-API.md`.
> Ketentuan wajib:
> 1. Buat unit test tabel U1 s.d. U8 untuk rumus reputasi dan gerbang konfirmasi moderator pada `DANGER_SCAM` (OD-03).
> 2. Implementasikan koleksi jurnal `sync_changes` dan endpoint `GET /api/v1/sync/changes?cursor=&limit=` beserta pengujian bahwa 2 perubahan pada detik yang sama dengan `limit=1` tidak ada yang terlewat (menutup temuan A06/T15).
> 3. Implementasikan `POST /api/v1/auth/refresh`, `GET /api/v1/me`, dan `DELETE /api/v1/me`.
> 4. Jalankan `go vet ./...` dan `go test -v ./...` hingga PASS, lalu perbarui progres di `CHECKLIST-PELAKSANAAN.md`.
> ```

---

## Tahap 4B. Fase 2B Backend: Dataset Nomor Resmi (`VERIFIED`), Detektor HLR/PSTN/Negara, & Importer CSV/JSON Open-Source (Pemilik A: Repo `backend`)

Tujuan: Mengatasi keterbatasan hasil `UNKNOWN` pada awal peluncuran secara jujur dan legal (Opsi A + C terkurasi di [09-ANALISIS-GETCONTACT-CLI.md](09-ANALISIS-GETCONTACT-CLI.md)) dengan menyediakan basis data nomor instansi/bisnis resmi terverifikasi (`verified_numbers`), identifikasi operator seluler HLR/kode kota PSTN/kode negara internasional, serta utilitas importer CSV/JSON terbuka berprovenans sebelum konsumsi di Android & Website.

- [x] **[AI] `T-207` Koleksi & Seed `verified_numbers` Resmi Indonesia (`OFFICIAL` / `PUBLIC_SERVICE` / `BUSINESS`)** (`model/model.go`, `config/db.go`, `controller/lookup.go`, `seeds/verified_numbers_seed.json`):
  - Tambah dukungan koleksi `verified_numbers` (`phone_e164`, `kind`, `display_label`, `source_url`, `verified_by`, `verified_at`, `expires_at`, `status`) sesuai [06-DESAIN-DATABASE.md](06-DESAIN-DATABASE.md).
  - Saat `GET /api/v1/phones/{e164}` dipanggil, periksa `verified_numbers` aktif sehingga nomor resmi mengembalikan `is_whitelisted_verified: true`, `verified_label`, `top_community_label`, serta metadata sumber (`verified_source_url`, `verified_at`).
  - Lakukan seed awal terverifikasi dari domain resmi (perbankan: Halo BCA, BRI, Mandiri, BNI, BSI; lembaga/utilitas: OJK 157, Kring Pajak 1500200, Bea Cukai 1500225, PLN 123, BPJS 165/175, Pos 1500161; ekspedisi/e-commerce/fintech: JNE, J&T, Shopee, Tokopedia Care WA, OVO, DANA).
- [x] **[AI] `T-208` Ekspansi Detektor Prefix HLR Indonesia, Kode Wilayah PSTN, & Kode Negara Asing** (`helper/scoring.go`, `controller/lookup.go`):
  - Perluas `DetectIndonesianCarrier` agar mengenali prefix seluler lengkap (Telkomsel Halo/Simpati/AS/Loop/by.U, Indosat IM3/Matrix/Mentari & Tri, XL & AXIS, Smartfren), kode wilayah telepon tetap PSTN Komdigi lebih lengkap (`021`..`098x`), serta kode negara ITU (`+65`, `+60`, `+63`, `+855` Kamboja, `+95` Myanmar, `+1`, dll.) disertai label peringatan konteks internasional.
  - Pastikan `LookupPhone` selalu mengembalikan label `carrier` informatif meskipun dokumen nomor masih kosong (`UNKNOWN`).
- [x] **[AI] `T-209` Utilitas CLI Importer Dataset Publik CSV/JSON Berprovenans** (`cmd/importdataset/main.go`, `config/db.go`, `seeds/`):
  - Bangun CLI importer (`go run ./cmd/importdataset -file <path> -kind verified|scam|tag_context -source <label> -dry-run`) dengan normalisasi E.164 seragam, filter PII, deduplikasi idempoten, dan pencatatan `sync_changes`.
  - Siapkan contoh template CSV/JSON + uji impor otomatis terhadap dataset berlisensi MIT yang jelas (mis. format `SysAdminDoc/CallShield` `spam_numbers.json` berlisensi MIT untuk entri penipuan dengan label sumber `EXTERNAL_OPEN_LIST`, dipisahkan jelas dari laporan warga CallTag).
- [x] **[AI] `T-210` Dukungan Kategori Netral Non-Penipu (`INFO_TAG`, Bobot Skor `0.0`)** (`model/model.go`, `helper/scoring.go`, `controller/report.go`):
  - Tambahkan kategori konteks non-spam (`INFO_TAG`, bobot $W = 0{,}0$) agar pengguna atau dataset resmi/komunitas dapat memberi `#Tag` informatif (mis. *"Kurir Ekspedisi"*, *"Layanan Pelanggan"*, *"Admin Toko"*, *"Sales Properti"*) dan menyimpannya ke database MongoDB (`spam_reports` + `phone_tags.tags[]`) **tanpa** menaikkan skor spam atau melabeli nomor sebagai penipu.
  - Tetap terapkan penyaring PII & aturan privasi UU PDP ([10-KEAMANAN-DAN-PRIVASI.md](10-KEAMANAN-DAN-PRIVASI.md)): `#Tag` menggambarkan konteks layanan/peran/usaha, bukan nama pribadi hasil pencurian buku kontak pihak lain.
- [ ] **[ANDA] *(Opsional — Bisa Menyusul Kapan Saja Nanti)* Siapkan Data Tambahan Sendiri**: Tidak memblokir kelanjutan aplikasi. Jika nanti ingin menambahkan daftar nomor khusus Anda sendiri (kampus/bisnis/survei), cukup isi template di `backend/seeds/optional_custom/` ([README-OPSIONAL-DATASET.md](../../../backend/seeds/optional_custom/README-OPSIONAL-DATASET.md)). Untuk lanjut sekarang, AI sudah mengambil data resmi & prefix open-source secara otomatis di `T-207` s.d. `T-209`, dan Anda cukup memverifikasi `cd backend && go vet ./... && go test -v ./...` setelah Tahap 4B selesai.

**Bukti implementasi 16 November 2026:** T-207–T-210 lulus `go vet ./...` dan suite `go test -v ./...` dengan MongoDB terisolasi, termasuk uji idempotensi, pemulihan jurnal, expiry/revocation, dan `INFO_TAG` netral. Lihat [laporan verifikasi](../verification/2026-11-16-stage-4b.md). Seed memuat 18 nomor resmi; Tokopedia Care WhatsApp belum dimasukkan karena sumber resmi belum dapat diverifikasi. Tabel negara mencakup 54 kode umum, dengan fallback konteks internasional untuk kode lain. Data tambahan pribadi tetap opsional.

> **Prompt Siap Pakai — Tahap 4B (Copy-Paste ke AI, atau ketik `lanjut Tahap 4B`):**
> ```text
> Kerjakan Tahap 4B (Fase 2B Backend: T-207, T-208, T-209, T-210) pada repositori `backend` sesuai spesifikasi di `CallTag/docs/planning/03-SPESIFIKASI-FITUR.md`, `06-DESAIN-DATABASE.md`, `07-KONTRAK-API.md`, `08-STRATEGI-PENCARIAN-NOMOR.md`, dan `09-ANALISIS-GETCONTACT-CLI.md`.
> Ketentuan wajib:
> 1. Implementasikan koleksi `verified_numbers` dan seed awal nomor layanan/instansi resmi Indonesia yang telah diverifikasi URL sumbernya (`source_url` & `verified_at`), lalu integrasikan ke `LookupPhone`.
> 2. Lengkapi `DetectIndonesianCarrier` di `helper/scoring.go` untuk seluruh prefix HLR seluler Indonesia, kode area PSTN Komdigi, dan kode negara ITU + label peringatan nomor internasional.
> 3. Tambahkan kategori netral `INFO_TAG` (bobot 0.0) agar penambahan `#Tag` konteks non-penipu tersimpan ke MongoDB tanpa menaikkan skor bahaya spam, dengan tetap menjaga saringan PII.
> 4. Buat CLI importer CSV/JSON idempoten (`cmd/importdataset`) yang mendukung `verified`, `scam`, dan `tag_context`, memvalidasi E.164, menyaring PII, menulis `sync_changes`, dan menjaga pemisahan label `source`.
> 5. Tambahkan unit test & integration test Go, lalu jalankan `go vet ./... && go test -v ./...` hingga PASS.
> ```

---

## Tahap 5. Fase 3: Pembaruan Aplikasi Android (Pemilik B: Repo `CallTag`)

Tujuan: Memperbaiki penyimpanan lokal Room ke versi 2 (dengan *outbox* laporan offline), menampilkan status pencarian nomor secara jujur (tanpa klaim "Nomor Aman" palsu), dan mengamankan kebijakan *Auto-Block*. *(Dapat dikerjakan paralel dengan Tahap 3 & 4).*

- [x] **[AI] `T-301` Naikkan Room Database ke Versi 2 + Tabel Outbox** (`CallTagDatabase.kt`, `entity/`, `dao/`): Tambahkan tabel `pending_reports`, `pending_disputes`, dan `sync_state`. Tambah kolom `availability`, `source`, `fetched_at_epoch_ms`, dan `expires_at_epoch_ms` pada `cached_phone_tags`. Tulis `Migration(1, 2)` eksplisit agar data lokal tidak terhapus saat update aplikasi.
- [x] **[AI] `T-303` Terapkan hasil Lookup 2 Dimensi & hapus klaim "Nomor Aman" palsu** (`CallTagRepository.kt`, `HomeScreen.kt`, `CallerIdOverlayCard.kt`): Bedakan status `Found`, `NotFound` (`UNKNOWN`: "Belum ada data, bukan jaminan aman"), `Unavailable` (`OFFLINE`), dan `Stale` (cache lewat masa berlaku). Jangan pernah menyimpan cache miss sebagai `CLEAN`.
- [x] **[AI] `T-302` Terapkan kebijakan Auto-Block Konservatif (`ScreeningPolicy`)** (`PhoneNormalizer.kt`, `CallTagScreeningService.kt`, `CallTagPreferences.kt`): Ubah default *toggle* Auto-Block menjadi `OFF` (*opt-in*). Hanya blokir otomatis jika level adalah `DANGER_SCAM`, sumber data `SERVER`, umur cache $\le 14$ hari, dan bukan nomor whitelist. Bungkus `onScreenCall` dengan batas waktu $< 4,5$ detik dan blok `finally` *fail-open* (`allowIncomingCall`).
- [x] **[AI] `T-304` Integrasikan `WorkManager` untuk Sync Delta & Kirim Ulang Outbox** (`app/build.gradle.kts`, `work/SyncWorker.kt`, `work/PendingReportWorker.kt`): Saat pengguna melapor dalam keadaan offline, simpan ke `pending_reports` tanpa menaikkan skor lokal, lalu kirim otomatis dengan `Idempotency-Key` yang sama begitu koneksi internet kembali.
- [x] **[AI] `T-305` Integrasikan autentikasi perangkat otomatis & `AuthInterceptor`** (`NetworkModule.kt`, `CallTagPreferences.kt`): Buat `install_id` UUID otomatis tanpa meminta nomor telepon pengguna (OD-02), panggil `/api/v1/auth/device`, simpan token dengan aman, dan pasangkan ke setiap request melalui OkHttp Interceptor.
- [x] **[AI] `T-306` & `T-307` Isolasi data seed/fixture & lengkapi state UI**: Pastikan data contoh di `OfflineSeedData.kt` berlabel `FIXTURE` (tidak tampil sebagai laporan warga nyata), samarkan nomor telepon di logcat, dan tampilkan daftar laporan tertunda (*outbox*) serta pelacakan *dispute* di layar UI.
- [ ] **[ANDA] Verifikasi Tahap 5 di Emulator**: Jalankan `./gradlew assembleDebug` dan cek di emulator `Pixel_5_Light` bahwa pencarian nomor acak menampilkan status *Belum Ada Data* (bukan *Nomor Aman*).

> **Prompt Siap Pakai — Tahap 5 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 5 (Fase 3 Android: T-301, T-303, T-302, T-304, T-305, T-306, T-307) pada repositori `CallTag` sesuai spesifikasi di `CallTag/docs/planning/05-ARSITEKTUR-SISTEM.md`, `06-DESAIN-DATABASE.md`, `08-STRATEGI-PENCARIAN-NOMOR.md`, dan `12-RANCANGAN-UI-UX.md`.
> Ketentuan wajib:
> 1. Naikkan Room ke versi 2 dengan migrasi eksplisit (`Migration(1, 2)`), tambahkan tabel `pending_reports` dan `sync_state`, serta pisahkan laporan offline agar tidak memalsukan kenaikan skor reputasi lokal (menutup A04).
> 2. Ubah kebijakan Auto-Block menjadi default OFF (opt-in) dan hanya memblokir `DANGER_SCAM` dari data server segar <= 14 hari dengan jaminan fail-open (menutup A05).
> 3. Pisahkan status hasil lookup menjadi `AVAILABLE`, `NOT_FOUND` (UNKNOWN), dan `UNAVAILABLE` (OFFLINE), serta hapus klaim teks "NOMOR AMAN" untuk nomor yang belum memiliki data (menutup A07).
> 4. Tambahkan unit test di `app/src/test/` untuk `PhoneNormalizer`, `ScreeningPolicy`, dan pemetaan `LookupResult`, lalu jalankan `./gradlew testDebugUnitTest assembleDebug` sampai BUILD SUCCESSFUL.
> ```

---

## Tahap 5B. Fase 3B Android: Penyempurnaan UX Profesional, Tab Akun, & Daya Tarik Identitas Nomor (Pemilik B: Repo `CallTag`)

Tujuan: Menghadirkan alur login opsional layaknya aplikasi nyata (tanpa memaksa pengguna baru), memisahkan fitur terbuka vs terkunci, melengkapi tab khusus **Akun & Pengaturan** (**Rekomendasi 1, 3, 4**), serta memperkuat daya tarik layar hasil pencarian nomor yang tersinkron dengan **Tahap 4B** (`T-308` s.d. `T-314` di [14-ROADMAP-PENGEMBANGAN.md](14-ROADMAP-PENGEMBANGAN.md)).

- [x] **[AI] `T-308` Mode Tamu (*Guest*) vs Akun Pengguna Terdaftar & Pembatasan Fitur (*Gating*)** (`CallTagPreferences.kt`, `CallTagMainViewModel.kt`, `HomeScreen.kt`, `ReportAndHubScreen.kt`):
  - Simpan status sesi eksplisit (*Guest* vs *Masuk Akun* memakai nama tampilan + nomor HP opsional via `POST /api/v1/auth/device`, dilengkapi tombol *Keluar / Logout*).
  - **Tetap terbuka untuk Tamu**: Pencarian nomor (`Cari`), Direktori Darurat (`Darurat`), Kanal Pengaduan Resmi, Lacak Sanggahan via `tracking_token`, Deteksi HLR/Wilayah/Negara, *Lihat di WhatsApp*, serta Tutorial Cara Pakai.
  - **Terkunci bila belum login** (tampilkan kartu ajakan *"Masuk Akun untuk Membuka Fitur Ini"*): Kirim `#Tag` laporan baru, daftar **Riwayat Panggilan Tersaring & Riwayat Laporan/Outbox**, Whitelist Pribadi, dan **Profil/Kuota (`/me`)**.
- [x] **[AI] `T-309` Tambah Tab ke-5 `Akun` & Pengaturan Lengkap + Tutorial Cara Pakai** (`MainScaffold.kt`, `AccountSettingsScreen.kt`, `SpamGuardScreen.kt`): Tambahkan tab ke-5 (`Cari` · `Darurat` · `Beri #Tag` · `Proteksi` · `Akun`) yang menampung Kartu Profil (Masuk/Keluar/Hapus Data UU PDP), pengaturan Tema (Terang/Gelap tersimpan persisten di DataStore), bersihkan cache lokal non-fixture, pintasan ke kontrol Proteksi, serta panduan tutorial langkah demi langkah aktivasi Caller ID dan arti lencana reputasi nomor.
- [x] **[AI] `T-310` *(Rekomendasi 1)* Diagnostik Kesehatan Izin Sistem (*Permission Health Check*)** (`AccountSettingsScreen.kt`, `RolePermissionManager.kt`): Tampilkan indikator status aktif/nonaktif untuk *Call Screening Role*, *Overlay/Popup*, Notifikasi (`POST_NOTIFICATIONS`), dan koneksi Backend (`/health`) beserta tombol 1 ketuk membuka Pengaturan Android.
- [x] **[AI] `T-311` *(Rekomendasi 3)* Daftar Pengecualian Pribadi (*Local Personal Whitelist*)** (`CallTagPreferences.kt` / `CallTagDatabase.kt`, `CallTagScreeningService.kt`, `AccountSettingsScreen.kt`): Pengguna login dapat menyimpan nomor kenalan/kurir/kantor ke whitelist lokal perangkat (tidak diunggah ke server) sehingga selalu dilewati aman dan terhubung ke parameter `isWhitelisted` pada `PhoneNormalizer.shouldAutoBlock`.
- [x] **[AI] `T-312` *(Rekomendasi 4)* Form Saran, Kritik, Lapor Bug Aplikasi, & Info Diagnostik Rilis** (`AccountSettingsScreen.kt`): Form dengan kategori (*Saran Fitur*, *Kritik Tampilan*, *Lapor Bug*), deskripsi masalah, lampiran otomatis info diagnostik aman (versi APK `BuildConfig.VERSION_NAME`, model HP, versi Android tanpa nomor HP mentah/token), tombol salin template laporan, dan kirim via Intent Email/Share/WhatsApp.
- [x] **[AI] `T-313` Identifikasi Konteks Offline: HLR Operator Indonesia, Kode Kota PSTN, Kode Negara Asing, & Lencana `VERIFIED`** (`PhoneNormalizer.kt`, `ApiDtos.kt`, `CallTagRepository.kt`, `HomeScreen.kt`, `CallerIdOverlayCard.kt`):
  - Sinkronkan tabel prefix HLR seluler, kode area PSTN Komdigi, dan kode negara ITU di `PhoneNormalizer.kt` agar identik dengan backend `T-208` (+ unit test tabel kasus bersama).
  - Tampilkan konteks jaringan/wilayah pada kartu hasil pencarian (`HomeScreen`) & overlay panggilan masuk (`CallerIdOverlayCard`) bahkan ketika status masih `UNKNOWN`/`OFFLINE`, serta tampilkan sumber verifikasi resmi (`verified_source_url`, `verified_at`) jika nomor berstatus `VERIFIED`.
- [x] **[AI] `T-314` Daya Tarik Aksi Cepat Hasil Pencarian: Awan `#Tag (count)` Langsung Tampil, Tambah Tag Konteks (`INFO_TAG`), & 1-Tap `"Lihat di WhatsApp"` (`wa.me`)** (`HomeScreen.kt`, `ReportAndHubScreen.kt`):
  - Tampilkan chip `#Tag` komunitas langsung terlihat bersama jumlah pelapornya (mis. `#Kurir Paket APK · 14` atau `#Kurir Paket Resmi · 5`) pada kartu hasil pencarian saat `tags` tersedia, disertai tombol cepat **`+ Tambah #Tag`**.
  - Pada tab `Beri #Tag`, dukung pemilihan antara **Laporkan Gangguan/Penipuan** *atau* **Beri #Tag Info Umum (`INFO_TAG`, non-penipu)** agar pengguna terdaftar bisa ikut menambahkan `#Tag` peran/layanan yang tersimpan ke MongoDB tanpa menaikkan skor spam.
  - Tambah tombol sekunder aman **"Lihat di WhatsApp"** berbasis deep-link resmi `https://wa.me/<digits>` via Android `ACTION_VIEW` (membuka profil/chat WhatsApp resmi tanpa menyimpan kontak dan **tanpa** scraping API WhatsApp/Getcontact; disembunyikan untuk nomor pendek darurat/call center $\le 6$ digit).
- [ ] **[ANDA] Verifikasi Tahap 4B, 5, & 5B di Emulator**: Pastikan backend menyala, cari nomor resmi terverifikasi (mis. Halo BCA `1500888` / `08111500998`), nomor asing (`+855...`), dan nomor acak (`UNKNOWN` + tetap tampil nama operator/kota), coba tombol *Lihat di WhatsApp*, uji mode *Guest* vs *Login*, serta uji semua sub-menu di tab **`Akun`** pada emulator `Pixel_5_Light`.

**Bukti implementasi 16 November 2026:** T-308–T-314 selesai; `./gradlew testDebugUnitTest assembleDebug --offline` lulus (14 pengujian, 0 gagal/terlewati). Emulator API 34 memverifikasi Tamu/Akun, kirim INFO_TAG skor 0, outbox PAUSED saat logout dan retry setelah login, tema persisten, whitelist, nomor resmi, serta konteks internasional/offline. Lihat [laporan dan tangkapan layar](../verification/2026-11-16-stage-5b.md). Butir verifikasi manual pengguna tetap tersedia untuk peninjauan mandiri dan pengujian aplikasi tujuan WhatsApp/Email.

> **Prompt Siap Pakai — Tahap 5B (Copy-Paste ke AI, atau cukup ketik `lanjut Tahap 5B`):**
> ```text
> Kerjakan Tahap 5B (Fase 3B Android: T-308 s.d. T-314) pada repositori `CallTag` sesuai spesifikasi di `CallTag/docs/planning/02-ROLE-DAN-HAK-AKSES.md`, `08-STRATEGI-PENCARIAN-NOMOR.md`, `12-RANCANGAN-UI-UX.md`, dan `14-ROADMAP-PENGEMBANGAN.md`.
> Implementasikan Tab ke-5 `Akun` (`AccountSettingsScreen.kt`), mode Guest vs Login Opsional + pembatasan fitur (Riwayat, Kirim Laporan, Whitelist, /me), Pengaturan Lengkap (termasuk tema persisten & tutorial), Diagnostik Kesehatan Izin Sistem, Local Personal Whitelist yang terhubung ke ScreeningPolicy, Form Saran/Kritik/Lapor Bug, Deteksi Konteks HLR/PSTN/Negara + Lencana VERIFIED (`T-313`), serta Awan `#Tag` terbuka + tombol `Lihat di WhatsApp` (`wa.me`) (`T-314`). Tambahkan unit test dan jalankan `./gradlew testDebugUnitTest assembleDebug` sampai BUILD SUCCESSFUL.
> ```

---

## Tahap 6. Fase 4: Panel Admin & Moderasi Internal (Pemilik A: Repo `backend`)

Tujuan: Menyediakan antarmuka web internal di `/admin` agar Moderator/Admin dapat meninjau sanggahan (*dispute*), mengesahkan kandidat `DANGER_SCAM` (OD-03), dan mengelola direktori darurat/whitelist dengan jejak audit.

- [x] **[AI] `T-401` Buat autentikasi Staf (`MOD`/`ADMIN`) & utilitas seed akun staf** (`controller/admin.go`, `helper/auth.go`): Login staf menggunakan username + password hash bcrypt + kode TOTP dengan cookie sesi `HttpOnly`, dilengkapi perintah/fungsi inisialisasi akun staf pertama.
- [x] **[AI] `T-402` Buat endpoint antrean & keputusan moderasi (`GET/PATCH /api/v1/admin/cases`)**: Moderator dapat menerima/menolak *dispute* atau mengesahkan kandidat `DANGER_SCAM` dengan alasan wajib, dilindungi *optimistic locking* (`version`), otomatis mencatat ke `audit_logs`, dan memicu event di `sync_changes`.
- [x] **[AI] `T-403` Buat endpoint CRUD Whitelist & Direktori Darurat (`N12`, `N13`, `N14`)**: Admin dapat menambah/mengubah nomor resmi (`verified_numbers`) dan posko darurat 5 kota pilot dengan syarat wajib mengisi `source_url` resmi dan tanggal verifikasi (OD-05), serta menyediakan endpoint/form unggah batch CSV/JSON yang memakai ulang validasi importer `T-209`.
- [x] **[AI] `T-404` Buat halaman HTML Panel Admin tertanam (`/admin`)**: Halaman ringkas dengan indikator SLA (Dispute $\le 7$ hari, Laporan $\le 3$ hari sesuai OD-09), tabel antrean kasus, form keputusan moderasi, form kurasi direktori, dan tabel `audit_logs`.
- [x] **[AI] `T-405` Buat Endpoint & Tombol Rekap Ekspor Data Admin (JSON & CSV)** (`controller/admin.go`, halaman `/admin`): Sediakan `GET /api/v1/admin/export?dataset=phone_tags|verified_numbers|moderation_cases&format=json|csv` khusus `ADMIN` yang mengekspor data terkurasi dalam format `.json` maupun `.csv` siap olah (tanpa `reporter_user_id`/token sensitif), lengkap dengan tombol unduh rekap di UI `/admin` sehingga data lapangan milik Anda dapat langsung diolah atau diarsipkan.
- [ ] **[ANDA] Verifikasi Tahap 6**: Buka `http://localhost:8080/admin` di browser, login sebagai staf, uji setujui 1 sanggahan (*dispute*), lalu coba unggah CSV dan unduh file rekap `.json` / `.csv`.

**Bukti implementasi 16 November 2026:** T-401–T-405 selesai; `go vet ./...` dan seluruh suite Go dengan MongoDB terisolasi lulus (0 gagal/terlewati), 41 pemeriksaan smoke HTTP panel lulus, dan alur login, antrean, serta detail kasus `/admin` diperiksa di Chromium headless. Lihat [laporan dan tangkapan layar](../verification/2026-11-16-stage-6.md). Butir verifikasi manual pengguna tetap tersedia untuk peninjauan mandiri.

> **Prompt Siap Pakai — Tahap 6 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 6 (Fase 4 Admin & Moderasi: T-401, T-402, T-403, T-404, T-405) pada repositori `backend` sesuai spesifikasi di `CallTag/docs/planning/11-ADMIN-DAN-MODERASI.md` dan `07-KONTRAK-API.md`.
> Ketentuan wajib:
> 1. Implementasikan endpoint admin N7 s.d. N14 + N16 (`GET /api/v1/admin/export`) dengan proteksi RBAC (`MOD` dan `ADMIN`), kewajiban mencantumkan `reason` pada setiap keputusan moderasi, dan kewajiban `source_url` pada kurasi direktori/whitelist.
> 2. Pastikan setiap tindakan staf menulis entri append-only ke `audit_logs` dan menerbitkan event baru ke `sync_changes`.
> 3. Sajikan halaman HTML/JS internal yang bersih di rute `/admin` untuk meninjau dispute, mengesahkan kandidat `DANGER_SCAM`, mengelola + upload batch CSV/JSON whitelist/direktori, melihat audit log, dan mengunduh rekap dataset `.json`/`.csv`.
> 4. Tulis pengujian RBAC (pengguna biasa/guest mendapat 403 pada `/api/v1/admin/*`, serta test ekspor JSON/CSV bebas PII) dan jalankan `go test -v ./...` hingga PASS.
> ```

---

## Tahap 7. Fase 5: Penyelarasan API Docs & Website Publik (Pemilik B: Repo `apidocs` & `calltag.github.io`)

Tujuan: Memastikan spesifikasi OpenAPI 3.0.3, simulator Swagger, dan portal web publik sinkron 100% dengan perilaku backend dan Android terbaru.

- [x] **[AI] `T-501` Lengkapi skema `openapi.json` & simulator `apidocs`** (`apidocs/openapi.json`, `apidocs/app.js`): Dokumentasikan skema respons lengkap untuk ke-11 endpoint utama + endpoint tambahan (`N1..N14`), selaraskan enum `call_type`, dan perbarui mode simulasi agar sesuai kontrak asli.
- [x] **[AI] `T-502` Selaraskan modul pencarian & sanggahan di Website** (`calltag.github.io/assets/js/`): Gunakan status 2 dimensi (`UNKNOWN` untuk nomor belum terlapor, bukan *Nomor Aman*), nonaktifkan form lapor cepat tanpa akun di web (atau arahkan ke form sanggahan/aplikasi), dan sediakan form pengajuan *Dispute Guest* (OD-10).
- [x] **[AI] `T-503` Rapikan statistik publik & informasi rilis di Website** (`calltag.github.io/index.html`, `tests/frontend-state.test.mjs`): Tampilkan angka statistik murni dari server (tanpa mencampur angka contoh sebagai data nyata) dan tambahkan pengujian otomatis di `frontend-state.test.mjs`.
- [ ] **[ANDA] Verifikasi Tahap 7**: Jalankan `cd calltag.github.io && node tests/frontend-state.test.mjs` dan pastikan seluruh test lulus.

**Bukti implementasi 16 November 2026:** T-501–T-503 selesai; `apidocs/openapi.json` diperbarui ke v1.2.0 mencakup 32 path endpoint (`/health`, `/healthz`, `/readyz`, auth/refresh, `/me`, lookup/verified, reports + `INFO_TAG`, guest disputes, `sync/changes`, dan admin panel), simulator `apidocs/app.js` disinkronkan dengan label simulasi offline (`4/4` tes kontrak di `apidocs/tests/contract.test.mjs` lulus). Website `calltag.github.io` menonaktifkan form lapor tanpa akun, menyediakan pengajuan & pelacakan Guest Dispute (`POST/GET /api/v1/disputes`), memperbaiki normalisasi internasional/`150xxxx` & deteksi konteks HLR/PSTN/negara, serta menampilkan statistik publik murni dari server (`10/10` tes di `calltag.github.io/tests/frontend-state.test.mjs` lulus; cek layout Chromium `1440×900`, `1024×768`, `390×844` menghasilkan 0 temuan). Butir verifikasi manual `[ANDA]` tetap tersedia untuk peninjauan mandiri.

> **Prompt Siap Pakai — Tahap 7 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 7 (Fase 5 API Docs & Website: T-501, T-502, T-503) pada repositori `apidocs` dan `calltag.github.io` sesuai spesifikasi di `CallTag/docs/planning/07-KONTRAK-API.md` dan `12-RANCANGAN-UI-UX.md`.
> Ketentuan wajib:
> 1. Perbarui `apidocs/openapi.json` dan `apidocs/app.js` agar seluruh skema request/response cocok dengan kontrak backend terbaru.
> 2. Perbarui `calltag.github.io` agar menampilkan status UNKNOWN secara jujur, mendukung pengajuan/pelacakan dispute oleh Guest (OD-10), dan tidak mencampur angka seed ke dalam statistik publik nyata.
> 3. Tambahkan test baru di `calltag.github.io/tests/frontend-state.test.mjs` dan jalankan `node tests/frontend-state.test.mjs` sampai semua test PASS.
> ```

---

## Tahap 8. Fase 6: Pengujian Menyeluruh / Quality Assurance (`T-601` s.d. `T-606`)

Tujuan: Membuktikan seluruh fitur, batasan keamanan, dan alur lintas repositori berjalan tanpa regresi sebelum demo/sidang.

- [x] **[AI] `T-601` & `T-604` Suite pengujian integrasi & keamanan Backend**: Menjalankan seluruh skenario uji positif dan negatif (`K-01` s.d. `K-12` di [15-RENCANA-PENGUJIAN.md](15-RENCANA-PENGUJIAN.md)) terhadap MongoDB uji terisolasi.
- [x] **[AI] `T-603` Suite Unit Test & Migrasi Android**: Memastikan `./gradlew testDebugUnitTest` menjalankan pengujian nyata (bukan `NO-SOURCE`) untuk normalisasi nomor, kebijakan *Auto-Block*, dan repositori.
- [x] **[AI] `T-602` Contract Test lintas repositori**: Memverifikasi kecocokan field JSON antara `backend`, `openapi.json`, dan DTO Kotlin di `CallTag`.
- [ ] **[ANDA] `T-605` Uji panggilan masuk di HP Android fisik**: Hubungkan HP fisik via USB (`adb reverse tcp:8080 tcp:8080`), berikan izin *Call Screening Role*, dan lakukan panggilan uji dari nomor lain untuk melihat tampilan *Caller ID Overlay*.
- [x] **[KEDUANYA] `T-606` Catat waktu respons (benchmark lokal)**: Mengukur latensi pembacaan cache Room dan respons API lokal untuk dicantumkan secara jujur di laporan.

**Bukti implementasi 16 November 2026:** T-601, T-602, T-603, T-604, dan pengukuran benchmark lokal T-606 selesai serta bukti lengkap dicatat pada [QA_TEST_PLAN](../QA_TEST_PLAN.md). Seluruh 7 paket Go bertest lulus (`0` fail, `0` skip) terhadap `mongod v8.3.9` terisolasi di `127.0.0.1:28019` (termasuk `qa_security_test.go`, `contract_openapi_test.go`, serta suite false-positive sintetis 75 sampel: `FP=0`, `FN=0`, `FPR=0.00%`, `Recall=100.00%`); `./gradlew testDebugUnitTest --rerun-tasks --offline` menjalankan `18` unit test Android di `6` kelas (`ContractParityTest`, `DatabaseMigrationTest` Room `1->2`, `CallTagTahap5UnitTest`, `PhoneContextTest`, `FeedbackDiagnosticsTest`, `SessionAccessPolicyTest` — `0` fail, `0` skipped); `apidocs/tests/contract.test.mjs` (`4/4` lulus) dan `calltag.github.io/tests/frontend-state.test.mjs` (`10/10` lulus). Tolok ukur lokal mencatat SQLite `p50=0.0149ms` / `p95=0.0170ms` (`n=500`, `1.001` baris) serta Go HTTP + MongoDB `28019` lookup `p50=0.539ms` / `p95=0.623ms` dan report write `p50=2.564ms` / `p95=2.920ms`. Butir **`T-605` (`[ANDA]`)** tetap terbuka untuk pengujian panggilan masuk pada perangkat HP Android fisik.

> **Prompt Siap Pakai — Tahap 8 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 8 (Fase 6 Quality Assurance: T-601, T-602, T-603, T-604, T-606) sesuai `CallTag/docs/planning/15-RENCANA-PENGUJIAN.md`.
> Jalankan seluruh test suite di `backend` (`go test -v ./...`), `CallTag` (`./gradlew testDebugUnitTest`), dan `calltag.github.io` (`node tests/frontend-state.test.mjs`), verifikasi kontrak terhadap `apidocs/openapi.json`, lalu perbarui bukti hasil pengujian di `CallTag/docs/QA_TEST_PLAN.md`.
> ```

---

## Tahap 9. Fase 7: Konfigurasi Rilis, Keamanan Jaringan, & CI/CD (`T-701` s.d. `T-704`)

Tujuan: Memastikan konfigurasi build APK, aturan trafik jaringan Android, dan workflow GitHub Actions aman serta rapi.

- [ ] **[AI] `T-701` Pisahkan `network_security_config` Android (`debug` vs `release`)**: Izinkan *cleartext HTTP* (`10.0.2.2` dan `127.0.0.1`) hanya pada build `debug`, dan wajibkan `HTTPS` penuh pada build `release`.
- [ ] **[AI] `T-702` Perbaiki GitHub Actions workflow (`release-android.yml`)**: Tambahkan langkah wajib menjalankan unit test sebelum build, dan ubah pemicu publikasi GitHub Release agar hanya berjalan ketika mendorong *git tag* versi (`v*.*.*`), bukan setiap kali mengubah file Markdown.
- [ ] **[AI] `T-703` & `T-704` Siapkan skrip backup/restore MongoDB & perbarui panduan operasional**: Tambahkan skrip utilitas `mongodump`/`mongorestore` untuk latihan pemulihan data dan pastikan [CARA_RUN_APLIKASI.md](../../../CARA_RUN_APLIKASI.md) sesuai dengan endpoint terbaru.

> **Prompt Siap Pakai — Tahap 9 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 9 (Fase 7 Finalisasi & CI/CD: T-701, T-702, T-703, T-704) sesuai `CallTag/docs/planning/16-DEPLOYMENT-DAN-DEMO.md`.
> 1. Atur `network_security_config` di Android agar cleartext hanya diizinkan pada debug dan HTTPS wajib pada release.
> 2. Perbarui `.github/workflows/release-android.yml` agar menjalankan unit test dan hanya menerbitkan rilis APK saat git tag `v*` dibuat.
> 3. Perbarui panduan di `CARA_RUN_APLIKASI.md` dan `PANDUAN_MENJALANKAN.md` bila ada penambahan variabel environment atau langkah admin.
> ```

---

## Tahap 10. Eksekusi Skenario Demo Akademik (`D1` s.d. `D9`)

Dilakukan saat seluruh kode di Tahap 3 s.d. 9 telah selesai untuk mengambil tangkapan layar (*screenshot*) dan bukti sidang sesuai [16-DEPLOYMENT-DAN-DEMO.md](16-DEPLOYMENT-DAN-DEMO.md):

- [ ] **[ANDA] `D1` Demo Pencarian Nomor Jujur**: Cari nomor scammer (muncul merah/oranye) dan nomor acak yang belum pernah dilaporkan (muncul status netral `UNKNOWN: Belum ada data`, bukan `Nomor Aman`).
- [ ] **[ANDA] `D2` & `D3` Demo Pelaporan & Anti-Spam/Kuota**: Kirim laporan dari aplikasi hingga masuk ke MongoDB, lalu tunjukkan pengiriman ulang kunci yang sama tidak menduplikasi data dan laporan ke-11 ditolak `429`.
- [ ] **[ANDA] `D4` Demo Mode Offline & Outbox**: Matikan koneksi di emulator, kirim laporan (status: *Tersimpan di perangkat*), nyalakan koneksi kembali, dan tunjukkan laporan terkirim otomatis.
- [ ] **[ANDA] `D5` Demo Caller ID & Auto-Block Konservatif**: Simulasikan panggilan masuk dari nomor `DANGER_SCAM` dan tunjukkan *banner* peringatan serta pemblokiran saat *toggle* Auto-Block diaktifkan.
- [ ] **[ANDA] `D6` Demo Sanggahan (*Dispute*) & Panel Moderator**: Ajukan sanggahan atas suatu nomor, buka `/admin` di browser, setujui sanggahan dengan alasan, dan tunjukkan status nomor di aplikasi Android ikut pulih setelah sinkronisasi.
- [ ] **[ANDA] `D7`, `D8`, & `D9` Demo Sinkronisasi Delta, Nomor Darurat Offline, & Website**: Tunjukkan pencarian posko Damkar/RS di 5 kota pilot dalam kondisi *Airplane Mode* dan pencarian nomor melalui portal web.
