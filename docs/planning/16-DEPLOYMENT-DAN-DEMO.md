# 16. Deployment dan demo

Status: **PROPOSED**. Diagram: [13](13-DIAGRAM-SISTEM.md) bagian 7 dan 8. Host produksi belum diputuskan (OD-06).

## 1. Strategi deployment

| Aspek | Staging | Produksi |
|---|---|---|
| Hosting | Satu VPS kecil atau PaaS dengan Docker (usulan; biaya belum diverifikasi) | Sama, instance terpisah |
| TLS | Reverse proxy (Caddy atau Nginx) + sertifikat otomatis | Sama + HSTS |
| Database | MongoDB replica set satu node, DB terpisah | Replica set; backup harian terenkripsi, retensi 14 hari |
| Konfigurasi | Variabel environment (`APP_ENV`, `MONGOSTRING`, `DB_NAME`, `JWT_SECRET`, `ALLOWED_ORIGINS`, `HASH_SALT`) | Sama, secret manager |
| Website/apidocs | GitHub Pages staging branch atau repo fork | GitHub Pages `main` setelah T-004 |
| Android | Build `staging` dengan `BASE_API_URL` HTTPS staging | Build `release` bertanda tangan |
| Rilis | Otomatis ke staging pada merge | **Manual** dari tag setelah gerbang |

Gerbang rilis (menyempurnakan PRD 8.4): P0 tertutup, Must P1 diterima, kontrak sinkron, outage/offline/restart/retry/revocation diuji, moderasi false positive berfungsi, fixture terisolasi, sumber kontak diverifikasi, restore backup teruji, keputusan kanal/SDK diambil (OD-04).

## 2. Demo akademik (lokal, data sintetis)

Prasyarat umum: [CARA_RUN_APLIKASI](../../../CARA_RUN_APLIKASI.md) (MongoDB lokal, backend `:8080` dengan `APP_ENV=dev`, emulator `Pixel_5_Light`). Semua nomor adalah fixture berlabel `FIXTURE`; tidak ada data warga.

**Batas simulasi:** menampilkan overlay/notifikasi Caller ID lewat perintah emulator atau uji layanan **bukan** pengujian panggilan seluler nyata. Panggilan nyata hanya diuji pada perangkat fisik dengan operator (T-605) dan tidak diklaim dari demo emulator.

| Demo | Alur | Prasyarat dan data uji | Expected result | Bukti |
|---|---|---|---|---|
| D1 Pencarian jujur | Cari nomor fixture berlaporan dan nomor acak | Backend + seed fixture | Fixture berlaporan: level sesuai; nomor acak: UNKNOWN "Belum ada data"; backend mati: OFFLINE | Tangkapan layar + log request |
| D2 Pelaporan | Daftar perangkat → lapor nomor fixture → lihat status | Akun perangkat baru | Dokumen di `spam_reports` (periksa `mongosh`), status "diterima server" | Cuplikan `mongosh` |
| D3 Idempotensi dan kuota | Kirim ulang paket sama; kirim 11 laporan | Skrip `curl` | 1 dokumen; kuota ke-11 `429` | Keluaran skrip |
| D4 Offline lalu sinkron | Mode pesawat → lapor → matikan mode pesawat | Emulator | Status "tersimpan di perangkat" lalu "diterima server" tanpa duplikat | Layar + DB |
| D5 Simulasi Caller ID | Picu layanan screening dengan nomor fixture DANGER dan nomor UNKNOWN | Perintah uji/instrumentasi yang disepakati (bukan panggilan nyata) | DANGER: peringatan (blokir hanya bila opt-in); UNKNOWN: tidak ada klaim aman | Layar + log ter-mask |
| D6 Sanggahan dan moderasi | Ajukan dispute → login staf → terima → lihat reputasi berubah | Akun staf dari CLI | Kasus `OPEN` → `ACCEPTED`, audit tercatat, level turun, sinkron ke Android | Audit log + layar |
| D7 Sinkronisasi delta | Buat dua perubahan dalam satu detik di backend, sync dengan `limit=1` | Skrip | Keduanya diterima | Cursor sebelum/sesudah |
| D8 Direktori darurat | Mode pesawat, buka Darurat | Seed direktori dengan `verified_at` | Nomor nasional tampil offline | Layar |
| D9 Website | Buka situs lokal, cari nomor | `python3 -m http.server 8082` | Status sama dengan Android; form lapor tidak ada | Layar |

Setiap demo direkam dengan perintah dan hasil di berkas bukti (format QA_TEST_PLAN).
