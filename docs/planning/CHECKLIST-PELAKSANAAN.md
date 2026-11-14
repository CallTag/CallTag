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

- [ ] **[AI] `T-102` Wajibkan `JWT_SECRET` & `HASH_SALT` dari environment** (`helper/auth.go`, `config/db.go`): Hapus fallback secret statis pada mode produksi (`APP_ENV=prod`); server wajib gagal start (*fail-closed*) bila secret kosong atau kurang dari 32 byte.
- [ ] **[AI] `T-107` Pisahkan endpoint `/healthz` dan `/readyz`** (`controller/sync.go`, `config/db.go`, `route/route.go`): `/healthz` untuk cek proses hidup, sedangkan `/readyz` melakukan `Ping` berbatas waktu ke MongoDB (kembalikan `503` jika MongoDB mati, bukan `200` palsu). Batasi fallback memori hanya untuk `APP_ENV=dev`.
- [ ] **[AI] `T-101` Perketat autentikasi JWT & larang Guest melapor** (`helper/auth.go`, `controller/auth.go`, `controller/report.go`): Registrasi `/api/v1/auth/device` berbasis `install_id` (tanpa wajib nomor telepon). Jika header `Authorization` ada tetapi token rusak/kedaluwarsa, kembalikan `401 Unauthorized` (jangan diubah jadi Guest). Tolak `POST /api/v1/reports` tanpa token User yang valid.
- [ ] **[AI] `T-103` Kunci identitas pelapor dari token & pasang Rate Limit** (`controller/report.go`, `config/db.go`): Ambil identitas pelapor murni dari klaim `sub` di JWT (abaikan header `X-Device-Id` agar tidak bisa dipalsukan). Terapkan kuota maksimal 10 laporan/24 jam dan 3 laporan/5 menit per akun (`429 Too Many Requests`).
- [ ] **[AI] `T-104` Terapkan `Idempotency-Key` & deduplikasi laporan 30 hari** (`model/model.go`, `config/db.go`, `controller/report.go`): Wajibkan header `Idempotency-Key` pada `POST /reports`, pasang *unique index* di MongoDB, kembalikan `_id` asli sebagai `report_id`, dan pastikan pengiriman ulang paket yang sama tidak menambah dokumen ganda.
- [ ] **[AI] `T-105` Simpan Sanggahan (*Dispute*) ke MongoDB sebelum respons `201`** (`model/model.go`, `config/db.go`, `controller/report.go`): Buat koleksi `moderation_cases` untuk menyimpan pengajuan `POST /api/v1/disputes` (termasuk dari Guest dengan batas 3x/24 jam), kembalikan `dispute_id` persisten beserta `tracking_token`, dan sediakan `GET /api/v1/disputes/{id}`.
- [ ] **[AI] `T-106` Sanitasi PII pada komentar & redaksi DTO laporan publik** (`controller/lookup.go`, `helper/scoring.go`): Hilangkan `reporter_user_id` dari respons publik `GET /api/v1/phones/{e164}/reports`, saring pola NIK/nomor rekening/nomor telepon di komentar, dan samarkan nomor telepon pada log server (`+62812****0005`).
- [ ] **[ANDA] Verifikasi Tahap 3**: Jalankan `cd backend && go test -v ./...` dan pastikan kasus uji negatif (token rusak = 401, kirim 2x kunci sama = 1 dokumen, dispute tersimpan di Mongo) semuanya `PASS`.

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

- [ ] **[AI] `T-202` Perbaiki `ScoringService` & gerbang moderator `DANGER_SCAM`** (`helper/scoring.go`, `config/db.go`): Batasi kontribusi maksimal 1 kali per pelapor unik per nomor, batasi `trust_weight` di rentang `0.25..1.5`, syaratkan minimal 2 pelapor unik (`N >= 2`) untuk naik ke `SUSPICIOUS`, dan tahan kandidat `DANGER_SCAM` di level `SPAM` sampai disetujui moderator (OD-03). Tambahkan unit test tabel `U1` s.d. `U8`.
- [ ] **[AI] `T-201` Bangun jurnal `sync_changes` & endpoint `GET /api/v1/sync/changes`** (`model/model.go`, `config/db.go`, `controller/sync.go`): Setiap perubahan pada `phone_tags` atau `emergency_directory` menulis nomor urut monoton `seq` (`UPSERT`/`DELETE`). Sediakan paginasi dengan cursor opak `generation:seq` agar tidak ada pembaruan yang terlewat saat sinkronisasi.
- [ ] **[AI] `T-204` Tambahkan endpoint profil, refresh token, & hapus akun** (`controller/auth.go`, `route/route.go`): Implementasikan `POST /api/v1/auth/refresh`, `GET /api/v1/me` (cek sisa kuota laporan), dan `DELETE /api/v1/me` (hak penghapusan/anonimisasi data pelapor sesuai UU PDP).
- [ ] **[AI] `T-203` Rapikan struktur internal backend & middleware** (`route/route.go`, `helper/`): Pisahkan middleware logging teredaksi, CORS origin terdaftar, dan batas ukuran request body maksimal 16 KB.
- [ ] **[AI] `T-205` & `T-206` Rekonsiliasi agregat & peluruhan skor (*decay*)**: Pastikan perhitungan ulang agregat `phone_tags` dari `spam_reports` bersifat idempoten (berjalan di MongoDB standalone tanpa wajib Replica Set sesuai OD-08) dan dukung kadaluwarsa data 12 bulan.
- [ ] **[ANDA] Verifikasi Tahap 4**: Jalankan `cd backend && go test -v ./...` dan pastikan seluruh test tabel scoring `U1..U8` serta paginasi `sync/changes` lulus.

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

## Tahap 5. Fase 3: Pembaruan Aplikasi Android (Pemilik B: Repo `CallTag`)

Tujuan: Memperbaiki penyimpanan lokal Room ke versi 2 (dengan *outbox* laporan offline), menampilkan status pencarian nomor secara jujur (tanpa klaim "Nomor Aman" palsu), dan mengamankan kebijakan *Auto-Block*. *(Dapat dikerjakan paralel dengan Tahap 3 & 4).*

- [ ] **[AI] `T-301` Naikkan Room Database ke Versi 2 + Tabel Outbox** (`CallTagDatabase.kt`, `entity/`, `dao/`): Tambahkan tabel `pending_reports`, `pending_disputes`, dan `sync_state`. Tambah kolom `availability`, `source`, `fetched_at_epoch_ms`, dan `expires_at_epoch_ms` pada `cached_phone_tags`. Tulis `Migration(1, 2)` eksplisit agar data lokal tidak terhapus saat update aplikasi.
- [ ] **[AI] `T-303` Terapkan hasil Lookup 2 Dimensi & hapus klaim "Nomor Aman" palsu** (`CallTagRepository.kt`, `HomeScreen.kt`, `CallerIdOverlayCard.kt`): Bedakan status `Found`, `NotFound` (`UNKNOWN`: "Belum ada data, bukan jaminan aman"), `Unavailable` (`OFFLINE`), dan `Stale` (cache lewat masa berlaku). Jangan pernah menyimpan cache miss sebagai `CLEAN`.
- [ ] **[AI] `T-302` Terapkan kebijakan Auto-Block Konservatif (`ScreeningPolicy`)** (`PhoneNormalizer.kt`, `CallTagScreeningService.kt`, `CallTagPreferences.kt`): Ubah default *toggle* Auto-Block menjadi `OFF` (*opt-in*). Hanya blokir otomatis jika level adalah `DANGER_SCAM`, sumber data `SERVER`, umur cache $\le 14$ hari, dan bukan nomor whitelist. Bungkus `onScreenCall` dengan batas waktu $< 4,5$ detik dan blok `finally` *fail-open* (`allowIncomingCall`).
- [ ] **[AI] `T-304` Integrasikan `WorkManager` untuk Sync Delta & Kirim Ulang Outbox** (`app/build.gradle.kts`, `work/SyncWorker.kt`, `work/PendingReportWorker.kt`): Saat pengguna melapor dalam keadaan offline, simpan ke `pending_reports` tanpa menaikkan skor lokal, lalu kirim otomatis dengan `Idempotency-Key` yang sama begitu koneksi internet kembali.
- [ ] **[AI] `T-305` Integrasikan autentikasi perangkat otomatis & `AuthInterceptor`** (`NetworkModule.kt`, `CallTagPreferences.kt`): Buat `install_id` UUID otomatis tanpa meminta nomor telepon pengguna (OD-02), panggil `/api/v1/auth/device`, simpan token dengan aman, dan pasangkan ke setiap request melalui OkHttp Interceptor.
- [ ] **[AI] `T-306` & `T-307` Isolasi data seed/fixture & lengkapi state UI**: Pastikan data contoh di `OfflineSeedData.kt` berlabel `FIXTURE` (tidak tampil sebagai laporan warga nyata), samarkan nomor telepon di logcat, dan tampilkan daftar laporan tertunda (*outbox*) serta pelacakan *dispute* di layar UI.
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

## Tahap 6. Fase 4: Panel Admin & Moderasi Internal (Pemilik A: Repo `backend`)

Tujuan: Menyediakan antarmuka web internal di `/admin` agar Moderator/Admin dapat meninjau sanggahan (*dispute*), mengesahkan kandidat `DANGER_SCAM` (OD-03), dan mengelola direktori darurat/whitelist dengan jejak audit.

- [ ] **[AI] `T-401` Buat autentikasi Staf (`MOD`/`ADMIN`) & utilitas seed akun staf** (`controller/admin.go`, `helper/auth.go`): Login staf menggunakan email + password hash + kode TOTP dengan cookie sesi `HttpOnly`, dilengkapi perintah/fungsi inisialisasi akun staf pertama.
- [ ] **[AI] `T-402` Buat endpoint antrean & keputusan moderasi (`GET/PATCH /api/v1/admin/cases`)**: Moderator dapat menerima/menolak *dispute* atau mengesahkan kandidat `DANGER_SCAM` dengan alasan wajib, dilindungi *optimistic locking* (`version`), otomatis mencatat ke `audit_logs`, dan memicu event di `sync_changes`.
- [ ] **[AI] `T-403` Buat endpoint CRUD Whitelist & Direktori Darurat (`N12`, `N13`, `N14`)**: Admin dapat menambah/mengubah nomor resmi (`verified_numbers`) dan posko darurat 5 kota pilot dengan syarat wajib mengisi `source_url` resmi dan tanggal verifikasi (OD-05).
- [ ] **[AI] `T-404` Buat halaman HTML Panel Admin tertanam (`/admin`)**: Halaman ringkas dengan indikator SLA (Dispute $\le 7$ hari, Laporan $\le 3$ hari sesuai OD-09), tabel antrean kasus, form keputusan moderasi, form kurasi direktori, dan tabel `audit_logs`.
- [ ] **[ANDA] Verifikasi Tahap 6**: Buka `http://localhost:8080/admin` di browser, login sebagai staf, dan coba setujui 1 sanggahan (*dispute*) hingga status nomor berubah.

> **Prompt Siap Pakai — Tahap 6 (Copy-Paste ke AI):**
> ```text
> Kerjakan Tahap 6 (Fase 4 Admin & Moderasi: T-401, T-402, T-403, T-404) pada repositori `backend` sesuai spesifikasi di `CallTag/docs/planning/11-ADMIN-DAN-MODERASI.md` dan `07-KONTRAK-API.md`.
> Ketentuan wajib:
> 1. Implementasikan endpoint admin N7 s.d. N14 dengan proteksi RBAC (`MOD` dan `ADMIN`), kewajiban mencantumkan `reason` pada setiap keputusan moderasi, dan kewajiban `source_url` pada kurasi direktori/whitelist.
> 2. Pastikan setiap tindakan staf menulis entri append-only ke `audit_logs` dan menerbitkan event baru ke `sync_changes`.
> 3. Sajikan halaman HTML/JS internal yang bersih di rute `/admin` untuk meninjau dispute, mengesahkan kandidat `DANGER_SCAM`, mengelola whitelist/direktori, dan melihat audit log.
> 4. Tulis pengujian RBAC (pengguna biasa/guest mendapat 403 pada `/api/v1/admin/*`) dan jalankan `go test -v ./...` hingga PASS.
> ```

---

## Tahap 7. Fase 5: Penyelarasan API Docs & Website Publik (Pemilik B: Repo `apidocs` & `calltag.github.io`)

Tujuan: Memastikan spesifikasi OpenAPI 3.0.3, simulator Swagger, dan portal web publik sinkron 100% dengan perilaku backend dan Android terbaru.

- [ ] **[AI] `T-501` Lengkapi skema `openapi.json` & simulator `apidocs`** (`apidocs/openapi.json`, `apidocs/app.js`): Dokumentasikan skema respons lengkap untuk ke-11 endpoint utama + endpoint tambahan (`N1..N14`), selaraskan enum `call_type`, dan perbarui mode simulasi agar sesuai kontrak asli.
- [ ] **[AI] `T-502` Selaraskan modul pencarian & sanggahan di Website** (`calltag.github.io/assets/js/`): Gunakan status 2 dimensi (`UNKNOWN` untuk nomor belum terlapor, bukan *Nomor Aman*), nonaktifkan form lapor cepat tanpa akun di web (atau arahkan ke form sanggahan/aplikasi), dan sediakan form pengajuan *Dispute Guest* (OD-10).
- [ ] **[AI] `T-503` Rapikan statistik publik & informasi rilis di Website** (`calltag.github.io/index.html`, `tests/frontend-state.test.mjs`): Tampilkan angka statistik murni dari server (tanpa mencampur angka contoh sebagai data nyata) dan tambahkan pengujian otomatis di `frontend-state.test.mjs`.
- [ ] **[ANDA] Verifikasi Tahap 7**: Jalankan `cd calltag.github.io && node tests/frontend-state.test.mjs` dan pastikan seluruh test lulus.

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

- [ ] **[AI] `T-601` & `T-604` Suite pengujian integrasi & keamanan Backend**: Menjalankan seluruh skenario uji positif dan negatif (`K-01` s.d. `K-12` di [15-RENCANA-PENGUJIAN.md](15-RENCANA-PENGUJIAN.md)) terhadap MongoDB uji terisolasi.
- [ ] **[AI] `T-603` Suite Unit Test & Migrasi Android**: Memastikan `./gradlew testDebugUnitTest` menjalankan pengujian nyata (bukan `NO-SOURCE`) untuk normalisasi nomor, kebijakan *Auto-Block*, dan repositori.
- [ ] **[AI] `T-602` Contract Test lintas repositori**: Memverifikasi kecocokan field JSON antara `backend`, `openapi.json`, dan DTO Kotlin di `CallTag`.
- [ ] **[ANDA] `T-605` Uji panggilan masuk di HP Android fisik**: Hubungkan HP fisik via USB (`adb reverse tcp:8080 tcp:8080`), berikan izin *Call Screening Role*, dan lakukan panggilan uji dari nomor lain untuk melihat tampilan *Caller ID Overlay*.
- [ ] **[KEDUANYA] `T-606` Catat waktu respons (benchmark lokal)**: Mengukur latensi pembacaan cache Room dan respons API lokal untuk dicantumkan secara jujur di laporan.

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
