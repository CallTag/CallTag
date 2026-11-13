# 15. Rencana pengujian

Status: **PROPOSED**. Bukti uji saat ini ada di [QA_TEST_PLAN](../QA_TEST_PLAN.md) (T01 sampai T24). Dokumen ini menetapkan strategi target; tidak ada hasil baru yang diklaim.

## 1. Strategi per jenis

| Jenis | Repo | Alat | Cakupan wajib | Gerbang |
|---|---|---|---|---|
| Unit | backend | `go test` tabel | Scoring U1 sampai U8, normalisasi nomor, validator PII, kebijakan kuota | CI wajib |
| Unit | Android | JUnit + Turbine | `PhoneNormalizer`, `ScreeningPolicy`, mapper DTO, `LookupResult` | CI wajib (menggantikan `NO-SOURCE`) |
| Integrasi | backend | `go test` + Mongo uji terisolasi (port khusus, DB sementara) | Alur laporan → agregasi → sync journal, dispute persist, transaksi/rekonsiliasi | CI |
| Kontrak API | backend + apidocs | Validasi respons terhadap `openapi.json` | 11 endpoint existing + N1 sampai N14 | CI |
| Database | backend | Skrip migrasi dry-run, uji unique index | Duplikat ditolak, validator `$jsonSchema`, restore | Pra-staging |
| Autentikasi | backend | Test HTTP | Token hilang/rusak/kedaluwarsa/`ver` usang/refresh bocor | CI |
| Role permission | backend | Test matriks | Tiap sel [02](02-ROLE-DAN-HAK-AKSES.md) bagian 2 | CI |
| Offline | Android | Instrumented + emulator mode pesawat | Cache berlabel STALE, pending bertahan setelah force-stop | Pra-rilis |
| Sinkronisasi | backend + Android | Fixture dua versi satu detik, limit 1; `generation` berubah | Tidak ada pembaruan yang hilang | CI + emulator |
| UI | Android | Compose test + screenshot emulator | State loading/empty/error/offline/success per halaman | Pra-rilis |
| Negatif | semua | Test | Nomor tak valid, body besar, PII, kuota, replay | CI |
| Keamanan | backend | Skrip + tinjauan | Bypass auth, IDOR pada `/disputes/{id}`, CSRF admin, CORS, kebocoran log | Sebelum pilot |
| Website | calltag.github.io | `node --test` | Status jujur, error tidak jadi sukses, rilis tak terverifikasi | CI (sudah ada 6 test) |
| E2E | semua | Skrip + emulator | Alur 3, 7, 8, 9, 10, 11, 16 | Sebelum demo/rilis |

## 2. Kasus kunci (traceability ke FR/NFR)

| Kasus | Memverifikasi | FR/NFR |
|---|---|---|
| K-01 11 perangkat berbeda melapor nomor sama | Tidak menjadi DANGER_SCAM tanpa gerbang | NFR-001, FR-012 |
| K-02 POST laporan dua kali, kunci sama | 1 dokumen | FR-005 |
| K-03 Dispute lalu restart server | Dispute masih ada | NFR-004, FR-007 |
| K-04 Dua perubahan di detik sama, `limit=1` | Keduanya tersinkron | FR-008 |
| K-05 Cache miss + API timeout | UI menampilkan OFFLINE, bukan CLEAN | FR-002 |
| K-06 Level SPAM atau cache seed | Tidak diblokir | FR-003 |
| K-07 Callback saat API lambat 10 detik | Respons < 5 detik, allow | NFR-003 |
| K-08 Non-staf akses `/admin/*` | 403 | FR-010 |
| K-09 Komentar berisi nomor/NIK | Ditahan atau dimasker | NFR-002 |
| K-10 `GET /phones/{e164}/reports` | Tanpa `reporter_user_id` | NFR-002 |
| K-11 Migrasi Room 1→2 dengan data | Data dipertahankan | NFR-004 |
| K-12 Hapus akun | Pseudonim dianonimkan, agregat tetap | FR-017 |

## 3. Suite false positive (untuk auto-block)

Set uji berlabel sintetis berisi nomor bersih, nomor bisnis terverifikasi, dan nomor DANGER; metrik: FPR dan recall, dilaporkan beserta ukuran sampel. Target PRD FPR < 0,5% hanyalah target; tidak ada klaim sebelum diukur. Tanpa data nyata pengguna.

## 4. Pelaporan bukti

Setiap uji mencatat: perintah, lingkungan (versi, port, DB sementara), hasil PASS/FAIL/BLOCKED, dan tautan log. Format sama dengan QA_TEST_PLAN agar traceability FR/NFR tetap utuh.
