# 00. Ringkasan eksekutif perencanaan final CallTag

| Metadata | Nilai |
|---|---|
| Status | **DRAFT RENCANA, menunggu persetujuan**. Tidak ada kode produk yang diubah. |
| Tanggal | 13 November 2026 (Asia/Jakarta) |
| Branch dokumen | `docs/master-plan` (turunan `codex/integration-audit-prd`, belum di-push, belum di-merge) |
| Baseline | Audit 10 November 2026: Android `42e9ceb`, backend `8676314`, apidocs `1ae8834`, website `c3e071b` (lihat [AUDIT_REPORT](../AUDIT_REPORT.md)) |
| Sifat dokumen | Rancangan **PROPOSED**. Kondisi kode saat ini hanya dirujuk dari audit dan validasi ulang di bagian "Validasi 13 November". |

## 1. Kondisi saat ini dalam lima kalimat

1. Alur lookup dan laporan Android → Go → MongoDB lokal terbukti berjalan di lingkungan pengembangan (`VERIFIED_E2E` terbatas), bukan di produksi.
2. Autentikasi tidak dipakai aplikasi, token tidak valid diperlakukan sebagai guest, dan 11 perangkat berbeda dapat membuat nomor menjadi `DANGER_SCAM` (T11).
3. Dispute hanya mengembalikan `201` tanpa penyimpanan; sinkronisasi dapat melewatkan pembaruan; cache tidak punya masa berlaku.
4. Backend, apidocs, dan website **hanya ada di feature branch**; `main` ketiganya masih kerangka.
5. Tidak ada admin/moderasi, tidak ada test Android, dan produksi (`https://api.calltag.id`) tidak terverifikasi.

## 2. Validasi 13 November 2026 (hanya yang berubah atau perlu dikonfirmasi)

| Pemeriksaan | Hasil | Status |
|---|---|---|
| Keempat repo: working tree bersih, HEAD sama dengan baseline audit (`36c02a3`, `8676314`, `1ae8834`, `c3e071b`) | Cocok | Temuan lama **belum berubah** karena tidak ada commit baru |
| `go test ./...` backend | PASS (satu suite happy path, memakai memory fallback karena `MONGOSTRING` kosong) | Konsisten dengan T01 |
| `./gradlew testDebugUnitTest` Android | BUILD SUCCESSFUL, task `NO-SOURCE` | Konsisten: tidak ada test Android |
| `node tests/frontend-state.test.mjs` website | 6/6 PASS | Konsisten dengan T04 |
| Repo `xdreizein666/getcontact-cli` (GitHub API, 13 Nov) | Tanpa lisensi, dibuat 20 Agu 2026, push terakhir 24 Agu 2026, tidak diarsipkan | Lihat [09](09-ANALISIS-GETCONTACT-CLI.md) |

Tidak ada temuan A01 sampai A10 yang terbukti sudah diperbaiki. Tidak ada temuan baru yang menggugurkan audit. Temuan **baru** pada perencanaan ini ada di [10-KEAMANAN-DAN-PRIVASI](10-KEAMANAN-DAN-PRIVASI.md) bagian 2 (N01 sampai N05).

## 3. Keputusan rancangan utama (rekomendasi default)

| # | Keputusan | Alasan singkat |
|---|---|---|
| K1 | Role MVP: **Guest, Registered User (berbasis perangkat), Staf (Moderator+Admin satu role berlapis), System Service**. *Verified Phone Owner* ditunda ke P2. | Tim dua orang; verifikasi kepemilikan nomor butuh OTP berbiaya dan proses hukum. |
| K2 | Sumber data lookup: **Opsi A (database mandiri CallTag)** untuk produksi, ditambah whitelist/direktori terkurasi. Opsi B hanya bila ada kontrak resmi. **Opsi D** (getcontact-cli) hanya bahan studi, tidak menjadi dependensi. | Legalitas, privasi, keberlanjutan. |
| K3 | Backend tetap **modular monolith Go `net/http` + MongoDB**; tambah lapisan `service`, middleware auth/RBAC/rate-limit. | Tidak rewrite; memperbaiki P0 pada struktur yang ada. |
| K4 | Panel admin: **halaman terlindungi yang disajikan backend** (bukan repo ketiga, bukan GitHub Pages). | GitHub Pages statis dan publik; satu deploy; tanpa repo baru. |
| K5 | Status hasil lookup memakai **dua dimensi**: `availability` (AVAILABLE/NOT_FOUND/UNAVAILABLE) dan `reputation_level`; UI menurunkan status tampilan UNKNOWN/OFFLINE. | Nomor tanpa laporan tidak boleh tampak aman (ADR-004). |
| K6 | Auto-block: **opt-in, default OFF**, hanya `DANGER_SCAM` dengan data server segar, fail-open. | Salah blokir lebih merugikan daripada lolos. |
| K7 | Sinkronisasi memakai **journal perubahan (`sync_changes`)** dengan cursor opak, bukan urutan risiko. | Menutup A06. |

Alternatif dan konsekuensi lengkap ada di bagian J laporan akhir dan [17-KEPUTUSAN-ARSITEKTUR](17-KEPUTUSAN-ARSITEKTUR.md).

## 4. Peta dokumen perencanaan

| Dokumen | Isi |
|---|---|
| [01](01-VISI-DAN-RUANG-LINGKUP.md) | Visi, pengguna, perbandingan, MVP vs production, KPI |
| [02](02-ROLE-DAN-HAK-AKSES.md) | Role, matriks izin, RBAC, autentikasi, siklus akun, verifikasi pemilik |
| [03](03-SPESIFIKASI-FITUR.md) | Fitur, tag komunitas, scoring (algoritma, pseudocode, contoh uji) |
| [04](04-ALUR-PENGGUNA.md) | 16 alur end-to-end |
| [05](05-ARSITEKTUR-SISTEM.md) | Android, backend, website, admin, integrasi, environment |
| [06](06-DESAIN-DATABASE.md) | Koleksi MongoDB, Room, ERD, indeks |
| [07](07-KONTRAK-API.md) | Endpoint existing dan tambahan |
| [08](08-STRATEGI-PENCARIAN-NOMOR.md) | Alur lookup, status, sumber data |
| [09](09-ANALISIS-GETCONTACT-CLI.md) | Evaluasi getcontact-cli dan opsi A sampai D |
| [10](10-KEAMANAN-DAN-PRIVASI.md) | Ancaman, mitigasi, UU PDP, Play |
| [11](11-ADMIN-DAN-MODERASI.md) | MVP admin dan alur moderasi |
| [12](12-RANCANGAN-UI-UX.md) | Navigasi, halaman, state, landing page |
| [13](13-DIAGRAM-SISTEM.md) | Diagram baru (yang belum ada) dan indeks diagram lama |
| [14](14-ROADMAP-PENGEMBANGAN.md) | Fase 0 sampai 7, tugas, dependensi |
| [15](15-RENCANA-PENGUJIAN.md) | Strategi uji |
| [16](16-DEPLOYMENT-DAN-DEMO.md) | Staging/produksi, CI/CD, skenario demo |
| [17](17-KEPUTUSAN-ARSITEKTUR.md) | ADR-012 dan seterusnya, keputusan terbuka |

## 5. Yang sengaja tidak dilakukan

Tidak ada perubahan source Android/backend/website/apidocs, tidak ada push, merge, deployment, penghapusan branch, maupun akses data nyata. Implementasi menunggu persetujuan atas keputusan di [17](17-KEPUTUSAN-ARSITEKTUR.md).
