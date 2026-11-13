# 17. Keputusan arsitektur dan keputusan terbuka

Status: **PROPOSED**. ADR-001 sampai ADR-011 tetap berlaku di [ARCHITECTURE_DECISIONS](../ARCHITECTURE_DECISIONS.md) dan tidak diulang. Dokumen ini menambah ADR-012 dan seterusnya (semua status **diusulkan, menunggu persetujuan**).

## ADR baru

| ADR | Keputusan | Alasan | Alternatif ditolak |
|---|---|---|---|
| ADR-012 | Role MVP: Guest, User (akun perangkat), Staf (MOD/ADMIN), System; Verified Owner ditunda | Tim 2 orang, biaya OTP | Verifikasi nomor sejak MVP |
| ADR-013 | Identitas pengguna tanpa nomor telepon; principal diterbitkan server | Minimisasi data, UU PDP | Registrasi dengan nomor tanpa OTP (as-is) |
| ADR-014 | Sumber data produksi: database mandiri + kurasi (Opsi A); getcontact-cli hanya riset | Legalitas, privasi, keberlanjutan | Opsi B/C/D sebagai dependensi |
| ADR-015 | Status hasil dua dimensi (`availability`, `reputation_level`) + `verified_label` | Nomor tanpa laporan tidak boleh tampak aman | Menambah `UNKNOWN` ke enum level |
| ADR-016 | Sinkronisasi dengan journal `sync_changes` dan cursor opak | Menutup A06 | Memperbaiki cursor `MAX(sync_version)` |
| ADR-017 | Admin sebagai halaman tertanam di backend | Satu deploy, same-origin | Repo admin terpisah; halaman di GitHub Pages |
| ADR-018 | Auto-block opt-in, DANGER-only, data server segar ≤ 14 hari, fail-open | Salah blokir lebih merugikan | Default ON dengan SPAM ≥ 60 (as-is) |
| ADR-019 | Idempotency-Key + unique index + dedup 30 hari | Menutup A08 | Hanya dedup pada skor |
| ADR-020 | Production strict: Mongo gagal berarti proses berhenti; memory fallback hanya `APP_ENV=dev` | Menutup A09 | Fallback senyap (as-is) |
| ADR-021 | Tetap `net/http` tanpa framework; tambah lapisan service/middleware | Menghindari rewrite | Migrasi ke gin/echo/gocroot |
| ADR-022 | Tidak memakai DI framework di Android; WorkManager ditambah | Skala kecil | Hilt |

## Keputusan terbuka (butuh persetujuan pemilik)

| ID | Pertanyaan | Rekomendasi default | Konsekuensi alternatif |
|---|---|---|---|
| OD-01 | Role MVP seperti ADR-012? | Ya | Menambah Verified Owner: +biaya OTP/WhatsApp, +2 sampai 3 minggu, perlu tinjauan hukum |
| OD-02 | Identitas pengguna berbasis perangkat tanpa nomor? | Ya | Memakai nomor + OTP: verifikasi lebih kuat, biaya dan pengumpulan data pribadi naik |
| OD-03 | Gerbang konfirmasi moderator untuk menjadi DANGER_SCAM? | Ya, untuk pilot | Otomatis penuh: lebih cepat tetapi rentan brigading |
| OD-04 | Kanal distribusi dan target SDK (sideload akademik vs Google Play)? | Sideload/akademik pada MVP; keputusan SDK 36 bila Play | Play: naikkan `targetSdk`, form Data Safety, tinjauan kebijakan overlay/call screening |
| OD-05 | Wilayah pilot dan kurator data? | Satu wilayah, dua kurator (tim) | Cakupan lebih luas: beban kurasi dan akurasi |
| OD-06 | Host staging/produksi dan anggaran | VPS kecil + Caddy; anggaran dikonfirmasi pemilik | PaaS terkelola: mahal tetapi operasi ringan |
| OD-07 | Lookup via path (`/phones/{e164}`) atau POST/hash prefix untuk privasi log | Pertahankan GET, mask log, retensi log pendek | POST: privasi lebih baik, kontrak berubah (v2) |
| OD-08 | MongoDB replica set satu node untuk transaksi? | Ya di staging/produksi | Rekonsiliasi tanpa transaksi: sederhana tetapi konsistensi eventual |
| OD-09 | SLA moderasi (7 hari dispute, 3 hari laporan tertahan) | Dicatat sebagai target | Lebih ketat: butuh staf lebih banyak |
| OD-10 | Izinkan guest mengajukan dispute (dengan kontak balik)? | Ya, kuota ketat | Hanya terdaftar: lebih aman dari spam, menyulitkan pemilik nomor |

Seluruh OD yang belum diputuskan menghambat T-001 dan karena itu menghambat Fase 1.
