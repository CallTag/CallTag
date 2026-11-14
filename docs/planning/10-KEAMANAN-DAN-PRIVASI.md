# 10. Keamanan, privasi, dan moderasi

Status: **PROPOSED**. Temuan A01 sampai A10 dan R01 sampai R13 tetap berlaku ([AUDIT_REPORT](../AUDIT_REPORT.md)); dokumen ini memetakan mitigasi dan menambah temuan baru. Bukan opini kepatuhan hukum.

## 1. Pemetaan temuan lama ke mitigasi

| Temuan | Mitigasi rancangan | Tugas ([14](14-ROADMAP-PENGEMBANGAN.md)) | Prioritas |
|---|---|---|---|
| JWT tidak valid diterima sebagai guest (A02) | Token ada tetapi tidak valid → `401`; guest tidak boleh melapor | T-101 | P0 |
| Identitas pelapor dari header (`X-Device-Id`) (A02) | Principal hanya dari klaim token; header diabaikan untuk identitas | T-101, T-103 | P0 |
| Secret fallback (A01) | Startup gagal bila `JWT_SECRET` kosong atau < 32 byte (kecuali `APP_ENV=dev` dengan peringatan) | T-102 | P0 |
| Laporan ganda (A08) | `idempotency_key` + unique index + dedup 30 hari | T-104 | P0 |
| Dispute tidak persisten (A03) | Tulis `moderation_cases` sebelum 201 | T-105 | P0 |
| Data lokal dianggap laporan server (A04) | `pending_reports` terpisah; tidak memengaruhi skor | T-301 | P0 |
| Risiko salah blokir (A05) | Opt-in default OFF, DANGER-only, data server segar, bukan whitelist/kontak, fail-open | T-302 | P0 |
| Cache kedaluwarsa (A07) | `expires_at`, label STALE, tidak diperlakukan sebagai CLEAN | T-303 | P0 |
| Sinkronisasi melewatkan update (A06) | Journal `sync_changes` + cursor opak | T-201, T-304 | P0 |
| Data demo dianggap nyata (A10) | `source=FIXTURE`, dipindah ke debug; release tidak menampilkan | T-306 | P1 |
| Kebocoran lewat API/log (R11) | DTO publik teredaksi; log tanpa nomor utuh dan tanpa token | T-106 | P0 (respons), P1 (log) |
| HTTP production | Build `release` tanpa cleartext; HTTPS di balik reverse proxy | T-701 | P1 |
| Konfigurasi release belum aman | Signing dari environment, workflow rilis tidak terpicu perubahan dokumen saja | T-702 | P1 |
| Readiness palsu (A09/T20) | `/readyz` ping Mongo; produksi gagal-cepat tanpa memory fallback | T-107 | P0 |

## 2. Temuan baru perencanaan

| ID | Temuan | Dampak | Rekomendasi | Prioritas |
|---|---|---|---|---|
| N01 | `authenticateDevice` mengirim `phone` pengguna ke server padahal tidak diperlukan untuk MVP | Pengumpulan data pribadi tanpa kebutuhan jelas | Hapus dari alur wajib (lihat [02](02-ROLE-DAN-HAK-AKSES.md)) | P1 |
| N02 | `CachedPhoneTag.unique_reporters` diisi konstanta 5 saat sync | UI/kebijakan dapat menyiratkan syarat DANGER terpenuhi | Gunakan nilai server; kolom nullable | P0 |
| N03 | Manifest mengizinkan cleartext global; log HTTP OkHttp level BASIC aktif di klien standar | URL (berisi nomor di path) dapat masuk logcat | Matikan logging di release; nomor di path berarti URL sensitif, pertimbangkan POST lookup atau hash prefix (keputusan OD-07) | P1 |
| N04 | Tidak ada pembatasan pada panjang/isi `comment` di sisi server yang terdokumentasi | Doxxing/PII | Batas 500 karakter + penyaring PII (nomor, NIK, rekening, email) | P0 |
| N05 | `targetSdk 34` | Tidak memenuhi syarat Play umum menurut audit | Keputusan kanal distribusi (OD-04) | P1 |

Catatan N03: path `GET /phones/{e164}` membuat nomor yang dicari tercatat di log proxy/server. Mitigasi minimum: log akses tanpa path penuh (mask) dan retensi log pendek.

## 3. Kontrol keamanan

| Area | Rancangan |
|---|---|
| Authentication | Lihat [02](02-ROLE-DAN-HAK-AKSES.md): token akses 15 menit, refresh dirotasi, staf argon2id + TOTP |
| RBAC | Middleware `RequireRole`, uji matriks izin otomatis |
| Rate limiting | Token bucket di memori per IP/principal (cukup untuk satu instance); Redis ditunda sampai > 1 instance |
| Anti-abuse | Kuota, trust weight, hold-for-review pada lonjakan, deteksi pelapor dari perangkat tunggal berulang |
| Idempotency | Header `Idempotency-Key` + unique index |
| Moderasi | [11](11-ADMIN-DAN-MODERASI.md) |
| Minimisasi data | Tanpa nomor pengguna, tanpa buku kontak, nomor target hanya yang dilaporkan; log mask |
| Consent | Layar onboarding menjelaskan data yang dikirim (nomor yang dicari/dilaporkan, komentar); persetujuan disimpan lokal dan versi kebijakan dikirim saat daftar |
| Retensi | Tabel di [06](06-DESAIN-DATABASE.md) dan ringkasan di bawah |
| Penghapusan | `DELETE /me` → anonimkan `reporter_user_id`, hapus `devices`/`refresh_tokens`, pertahankan agregat; tenggat 30 hari |
| Audit trail | `audit_logs` append-only untuk semua tindakan staf dan perubahan level |
| Backup/recovery | `mongodump` terjadwal harian terenkripsi, retensi 14 hari, uji restore di staging per rilis |
| HTTPS | TLS di reverse proxy (mis. Caddy/Nginx + Let's Encrypt); HSTS; Mongo TLS bila lintas host |
| Secret management | Variabel environment atau secret manager hosting; tidak di repo; rotasi saat staf berganti |

Ringkasan retensi: log akses aplikasi 14 hari; laporan mentah 24 bulan; komentar ditolak 12 bulan; kasus moderasi 36 bulan setelah ditutup; audit 24 bulan; refresh token mengikuti `expires_at` (TTL index).

## 4. UU PDP dan kebijakan platform (rujukan, bukan kepatuhan)

Pencarian web 13 November 2026 menunjukkan poin yang relevan dari UU 27/2022: persetujuan sah (Pasal 20), larangan mengungkap data pribadi yang bukan milik sendiri (Pasal 65 ayat 2), hak koreksi dan penghapusan, kewajiban pengendali data. Implikasi rancangan: tidak menampilkan identitas pemilik, menyediakan jalur sanggahan dan penghapusan, dan kebijakan privasi yang sesuai praktik nyata (halaman `privacy.html` saat ini menyatakan "tanpa unggah buku kontak" dan "anonimitas pelapor"; klaim ini harus diverifikasi terhadap implementasi akhir sebelum rilis, termasuk bahwa pelapor berpseudonim, bukan anonim penuh). Salted hash bukan anonimisasi. Perlu peninjauan hukum sebelum publikasi luas, di luar cakupan dokumen ini.

Kebijakan Play: target API 36 untuk aplikasi baru sejak 31 Agustus 2026 (rujukan audit); deklarasi izin sensitif (overlay, notifikasi, call screening role) dan form Data Safety harus akurat; pembuktian tidak dilakukan di sini.

## 5. Auto-block konservatif (aturan normatif)

1. Default OFF; mengaktifkan memerlukan konfirmasi penjelasan.
2. Blokir hanya jika: `reputation_level = DANGER_SCAM` **dan** sumber `SERVER` **dan** `fetched_at` ≤ 14 hari **dan** bukan `verified_label` **dan** bukan kontak tersimpan (OS sudah mengecualikan kontak tanpa `READ_CONTACTS`; jangan mengandalkan asumsi).
3. Seluruh keputusan lain: izinkan. Kesalahan/timeout/exception: izinkan (`finally`).
4. Panggilan terblokir muncul di notifikasi dengan tombol "Bukan spam" yang mengirim dispute/umpan balik lokal.
5. Kebijakan diuji dengan suite false positive berlabel sebelum diaktifkan sebagai opsi.

