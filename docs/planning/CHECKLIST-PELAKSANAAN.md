# Checklist pelaksanaan CallTag

Centang `[x]` saat selesai. **[AI]** = bisa diserahkan ke AI coding agent lewat prompt, **[ANDA]** = harus Anda putuskan/kerjakan sendiri, **[KEDUANYA]** = AI mengerjakan, Anda meninjau. Kode tugas (T-xxx) merujuk [14-ROADMAP](14-ROADMAP-PENGEMBANGAN.md). Status per 13 November 2026.

## Tahap 0. Sudah selesai

- [x] Screening awal workspace (4 repo, branch, test, environment)
- [x] Baca hasil audit 10 Nov sebagai baseline dan validasi ulang (tidak ada commit baru)
- [x] Riset `getcontact-cli` (lisensi, status repo, kesimpulan: tidak dipakai di production)
- [x] Tulis dokumen perencanaan 00 sampai 17 di `docs/planning/`
- [x] Perbarui `PRD.md` ke 1.2.0-draft (bagian 10) dan indeks `docs/README.md`
- [x] Validasi tautan dan seluruh diagram Mermaid (termasuk katalog lengkap 17 diagram sistem di [13-DIAGRAM-SISTEM](13-DIAGRAM-SISTEM.md))
- [x] Commit lokal di branch `docs/master-plan`

## Tahap 1. Persetujuan & Penyelarasan Baseline (T-001 sampai T-004)

- [x] **[ANDA]** Baca [00-RINGKASAN](00-RINGKASAN-EKSEKUTIF.md) dan [17-KEPUTUSAN](17-KEPUTUSAN-ARSITEKTUR.md)
- [x] **[ANDA]** Putuskan OD-01 role MVP (Disetujui: 4 Role tanpa Verified Owner di MVP)
- [x] **[ANDA]** Putuskan OD-02 identitas berbasis perangkat (`install_id` UUID + JWT server) tanpa nomor telepon
- [x] **[ANDA]** Putuskan OD-03 gerbang konfirmasi moderator untuk `DANGER_SCAM` (Disetujui: Wajib konfirmasi moderator)
- [x] **[ANDA]** Putuskan OD-04 kanal distribusi (Disetujui: Sideload APK Akademik via GitHub Releases, `targetSdk 34`)
- [x] **[ANDA]** Putuskan OD-05 wilayah pilot dan kurator data (Disetujui: 7 Nasional + 5 Kota Utama, dikurasi tim berdua)
- [x] **[ANDA]** Putuskan OD-06 host staging/produksi dan anggaran (Disetujui: Skema Rp 0 Lokal/LAN + opsional MongoDB Atlas Free Tier & GitHub Pages)
- [x] **[ANDA]** Putuskan OD-07 sampai OD-10 (Disetujui: GET lookup + log masking, Idempotent Upsert + Rekonsiliasi tanpa wajib Replica Set lokal, SLA target dashboard, Dispute Guest diizinkan dengan rate limit)
- [x] **[ANDA]** Bagi peran: Pengembang A (`backend`) dan Pengembang B (`CallTag`, `apidocs`, `calltag.github.io`)
- [x] **[AI]** Perbarui `17-KEPUTUSAN`, `13-DIAGRAM-SISTEM` (17 diagram lengkap), dan `PRD.md` v1.2.0 final (T-001, T-002, T-003 selesai)
- [ ] **[KEDUANYA]** Push branch `docs/master-plan`, buka PR, review, merge (T-004 bagian docs)
- [ ] **[KEDUANYA]** PR promosi feature branch `backend`, `apidocs`, `calltag.github.io` ke `main` setelah ditinjau

## Tahap 2. Persiapan lingkungan

- [ ] **[ANDA]** Pastikan MongoDB lokal jalan (`systemctl status mongodb`) dan emulator `Pixel_5_Light` bisa start
- [ ] **[ANDA]** Siapkan satu perangkat Android fisik untuk uji panggilan nyata (T-605)
- [ ] **[ANDA]** Buat file `.env` lokal untuk `JWT_SECRET` kuat minimal 32 karakter (jangan di-commit)

## Tahap 3. Fase 1 keamanan backend (pemilik A) **belum dimulai**

Prompt ke agen: "Kerjakan T-101 sesuai `docs/planning/14-ROADMAP-PENGEMBANGAN.md` dan `10-KEAMANAN-DAN-PRIVASI.md`, TDD, hanya di repo backend, branch baru."

- [ ] T-102 secret wajib dan kuat **[AI]**
- [ ] T-107 `/healthz` dan `/readyz`, produksi tanpa memory fallback **[AI]**
- [ ] T-101 bearer tidak valid ditolak 401, guest tidak boleh melapor **[AI]**
- [ ] T-106 DTO publik teredaksi, filter PII, log tanpa nomor/token **[AI]**
- [ ] T-103 principal dari token, kuota dan rate limit **[AI]**
- [ ] T-104 idempotency key, unique index, dedup 30 hari **[AI]**
- [ ] T-105 dispute tersimpan sebelum 201 **[AI]**
- [ ] **[ANDA]** Review tiap PR, jalankan `go test ./...`, cek tidak ada secret di diff

## Tahap 4. Fase 2 backend dan database (A)

- [ ] T-202 `ScoringService` + test tabel U1 sampai U8 **[AI]**
- [ ] T-201 journal `sync_changes` + `GET /sync/changes` **[AI]**
- [ ] T-204 endpoint refresh, me, hapus akun, lacak dispute **[AI]**
- [ ] T-205 validator `$jsonSchema`, migrasi aman, uji restore **[KEDUANYA]**
- [ ] T-203 refactor service/middleware bertahap **[AI]**
- [ ] T-206 job decay, kedaluwarsa, retensi **[AI]**
- [ ] **[ANDA]** Bekukan kontrak OpenAPI per milestone dan beri tahu B

## Tahap 5. Fase 3 Android (B, paralel dengan Tahap 3 dan 4)

- [ ] T-301 Room v2: outbox, `sync_state`, migrasi tertulis **[AI]**
- [ ] T-303 `LookupResult` bertipe, cache kedaluwarsa, hapus CLEAN sintetis **[AI]**
- [ ] T-302 kebijakan auto-block konservatif, default OFF **[AI]**
- [ ] T-304 `SyncRepository` + WorkManager **[AI]**
- [ ] T-305 onboarding, `AuthInterceptor`, token aman **[AI]**
- [ ] T-306 isolasi fixture, hapus teks "AMAN", mask log **[AI]**
- [ ] T-307 halaman Riwayat, Sanggahan, Akun dan data **[AI]**
- [ ] **[ANDA]** Cek tampilan di emulator untuk tiap halaman (loading, kosong, error, offline, sukses)

## Tahap 6. Fase 4 admin (A)

- [ ] T-401 login staf + TOTP + CLI buat staf **[AI]**
- [ ] T-402 antrean kasus, keputusan, audit **[AI]**
- [ ] T-403 CRUD whitelist, direktori darurat, kanal resmi **[AI]**
- [ ] T-404 halaman HTML admin tertanam **[AI]**
- [ ] **[ANDA]** Buat akun staf pertama dan coba alur moderasi manual

## Tahap 7. Fase 5 apidocs dan website (B)

- [ ] T-501 OpenAPI skema lengkap, simulator jujur **[AI]**
- [ ] T-502 website: status jujur, matikan form lapor, perbaiki enum `call_type` **[AI]**
- [ ] T-503 landing final, statistik nyata, unduhan terverifikasi **[AI]**
- [ ] **[ANDA]** Cek `privacy.html` dan `terms.html` sesuai implementasi sebenarnya

## Tahap 8. Fase 6 QA

- [ ] T-601 test unit dan integrasi backend **[AI]**
- [ ] T-603 unit test dan instrumented Android **[AI]**
- [ ] T-602 contract test OpenAPI, backend, DTO **[AI]**
- [ ] T-604 uji keamanan (auth, RBAC, rate limit, PII) **[KEDUANYA]**
- [ ] T-605 uji perangkat fisik dan operator **[ANDA]**
- [ ] T-606 benchmark terukur **[KEDUANYA]**
- [ ] **[ANDA]** Kumpulkan bukti PASS/FAIL tiap uji untuk laporan

## Tahap 9. Fase 7 deployment

- [ ] **[ANDA]** Sewa/siapkan host dan domain sesuai OD-06
- [ ] T-701 staging HTTPS, build `staging`/`release` tanpa cleartext **[KEDUANYA]**
- [ ] T-702 signing release, gerbang CI, rilis hanya dari tag **[KEDUANYA]**
- [ ] **[ANDA]** Buat keystore release dan simpan aman (jangan di repo)
- [ ] T-703 backup terjadwal dan uji restore **[KEDUANYA]**
- [ ] T-704 runbook penggunaan **[AI]**

## Tahap 10. Demo dan penyerahan akademik

- [ ] Jalankan demo D1 sampai D9 ([16-DEPLOYMENT-DAN-DEMO](16-DEPLOYMENT-DAN-DEMO.md)) dan simpan bukti **[ANDA]**
- [ ] Pastikan semua nomor demo berlabel fixture, bukan data warga **[ANDA]**
- [ ] Perbarui PRD dan laporan dengan hasil nyata **[KEDUANYA]**
- [ ] Siapkan presentasi untuk pembimbing **[ANDA]**

