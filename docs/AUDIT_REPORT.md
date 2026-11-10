# Audit integrasi CallTag — 10 November 2026

## Putusan eksekutif

**PARTIALLY_VERIFIED.** Lookup Android → Retrofit → backend Go → MongoDB lokal → respons → UI/Room telah diuji menggunakan fixture yang tidak ada di seed APK. Pengiriman laporan melalui UI Android juga mencapai MongoDB lokal; rincian hasil ada di [QA_TEST_PLAN](QA_TEST_PLAN.md). Ini bukti koneksi lingkungan pengembangan, bukan kesiapan rilis: autentikasi aplikasi belum dipanggil, guest dapat memanipulasi reputasi, dispute tidak persisten, dan sync dapat kehilangan perubahan. Backend produksi dan screening panggilan nyata belum terverifikasi.

Audit ini mengubah dokumentasi saja. Risiko yang ditemukan belum diperbaiki dalam source. Visi, taksonomi enam kategori, empat tingkat reputasi, dan identitas Nusantara Teal dipertahankan.

## Baseline yang diperiksa

Snapshot fetch pada **10 November 2026, WIB**. Tidak ditemukan branch remote lain selain main dan feature di bawah. Pencarian PR GitHub `org:CallTag`, state all, mengembalikan daftar kosong sebelum PR audit dibuat. Ini snapshot hasil pencarian, bukan jaminan akses ke seluruh riwayat privat organisasi.

| Repository | Main | Branch/commit audit; waktu WIB | Kondisi dan dampak integrasi | Bukti file:line |
|---|---|---|---|---|
| `CallTag/CallTag` | `8a12e98`, 8 Nov 18:26 | `codex/frontend-mobile-refresh`, `42e9ceb`, 10 Nov 16:33 | Main sudah memiliki aplikasi; feature memperbarui UI. Branch audit berangkat dari feature agar dokumen sesuai UI terbaru. | [CallTagApplication.kt:20](https://github.com/CallTag/CallTag/blob/42e9ceb/app/src/main/java/id/calltag/app/CallTagApplication.kt#L20) |
| `CallTag/backend` | `024a01d`, 4 Nov 13:35 | `codex/workspace-initial-import`, `8676314`, 10 Nov 20:34 | Main hanya README/LICENSE/gitignore; source Go hanya di feature, belum merged. | [route/route.go:23](https://github.com/CallTag/backend/blob/8676314/route/route.go#L23) |
| `CallTag/apidocs` | `474b714`, 4 Nov 13:46 | `codex/workspace-initial-import`, `1ae8834`, 10 Nov 20:34 | Main hanya kerangka; OpenAPI dan UI interaktif hanya di feature. | [openapi.json:1](https://github.com/CallTag/apidocs/blob/1ae8834/openapi.json#L1) |
| `CallTag/calltag.github.io` | `b7965be`, 4 Nov 13:27 | `codex/frontend-mobile-refresh`, `c3e071b`, 10 Nov 16:33 | Situs dan aset hanya di feature; branch tidak membuktikan publikasi Pages. | [index.html:644](https://github.com/CallTag/calltag.github.io/blob/c3e071b/index.html#L644) |

Bukti source baseline: [wiring Android](https://github.com/CallTag/CallTag/blob/42e9ceb/app/src/main/java/id/calltag/app/CallTagApplication.kt#L20), [router backend](https://github.com/CallTag/backend/blob/8676314/route/route.go#L23), [kontrak OpenAPI](https://github.com/CallTag/apidocs/blob/1ae8834/openapi.json#L1), [website](https://github.com/CallTag/calltag.github.io/blob/c3e071b/index.html#L644). `git ls-tree -r origin/main` pada backend/apidocs/web menunjukkan tiga berkas kerangka; `git branch -avv`, `git log --all --format` dan `git fetch origin --prune` dipakai untuk membandingkan baseline. PRD asal main dan feature identik sebelum audit (SHA-256 `445dcf9bde336fc813d80e0fe53fefc8dc0d598b7b11a9e9db611fd240eb1235`).

## Standar status dan pembuktian

| Label | Arti |
|---|---|
| `VERIFIED_E2E` | Rantai tertentu benar-benar diuji sampai penyimpanan dan dibaca kembali; selalu sebut lingkungan dan batas rantai. |
| `IMPLEMENTED_NOT_E2E_TESTED` | Kode ditemukan; rantai penuh belum diuji. |
| `PARTIAL` | Ada bagian yang belum lengkap atau bertentangan dengan persyaratan. |
| `MOCK_OR_LOCAL_ONLY` | Seed, simulasi, cache, atau memory fallback; bukan bukti server persisten. |
| `SPEC_ONLY` | Ada di PRD/OpenAPI sebagai target, belum ada implementasinya. |
| `MISSING` | Tidak ditemukan pada baseline audit. |
| `BLOCKED` | Verifikasi membutuhkan konfigurasi, perangkat, akses, atau dependensi yang belum tersedia. |

Label fitur bukan severity. `VERIFIED_E2E` pada lookup lokal tidak meniadakan masalah keamanan dan tidak berlaku otomatis untuk sinkronisasi, auth, dispute atau panggilan nyata. Fakta source/runtime ditulis sebagai temuan; desain baru selalu diberi label **PROPOSED**.

## Sepuluh temuan paling penting

Path Android pada tabel memakai awalan `app/src/main/java/id/calltag/app/`; backend relatif repo `backend`. Nomor baris mengacu commit baseline, bukan PRD yang sedang diedit.

| ID / severity | Bukti fungsi dan lokasi | Dampak; minimum remediasi |
|---|---|---|
| A01 **Critical bila fallback secret dipakai** | backend `helper/auth.go:27–42`, `getSecretKey`/`HashIdentity`; `controller/auth.go:32–55` | Secret JWT default ada dalam source dan registrasi tidak membuktikan kepemilikan nomor. Wajib secret kuat di environment, startup strict dan principal yang dibuktikan; jangan menyebut akun ini terverifikasi. |
| A02 **High** | backend `helper/auth.go:113–140`, `ExtractClaimsFromRequest`; `controller/report.go:63–94` | Invalid token menjadi guest. Runtime 11 header perangkat berbeda menghasilkan 11 respons 201 dan `DANGER_SCAM`, skor 100, reporter 11 pada nomor sintetis. Tolak invalid bearer, ikat identitas server, quota dan moderasi sebelum kontribusi. |
| A03 **High** | backend `controller/report.go:98–133`, `SubmitDispute` | 201/UNDER_REVIEW tanpa write, antrean atau model dispute. DB test tidak memiliki koleksi dispute setelah request. Simpan pengajuan sebelum acknowledgment; sediakan review dan koreksi false positive. |
| A04 **High** | Android `data/repository/CallTagRepository.kt:215–297`, `submitSpamReport` | Semua error remote berubah menjadi skor lokal/reporter baru dan DTO sukses; tidak ada raw report outbox/retry. UI telah menyebut tersimpan di perangkat, tetapi repeated local report dapat memengaruhi screening. Pisahkan pending write dari reputasi otoritatif. |
| A05 **High** | Android `util/PhoneNormalizer.kt:56–66`; service `CallTagScreeningService.kt:71–91` | Auto-block lebih luas dari target DANGER-only: SPAM>=60 atau kategori berisiko>=50 juga diblokir. Default toggle ON; demo/stale cache dapat ikut keputusan. Gunakan kebijakan konservatif opt-in dan verifikasi freshness/override. |
| A06 **High** | backend `config/db.go:521–585`, `GetTopScammersForSync`; Android repository `:338–370`, DAO `PhoneTagDao.kt:22` | Risk-sort + max cursor + limit melewatkan versi; Unix detik tabrakan; Android MAX bercampur seed/lokal. Uji dua fixture membuktikan satu versi hilang. Perlu cursor terpisah, urutan stabil, pagination dan CLEAN/tombstone. |
| A07 **High** | Android repository `:77–83,127–181`; entity `CachedPhoneTag.kt:50`; overlay `CallerIdOverlayCard.kt:75–78` | Cache tidak TTL; API found=false tidak mencabut cache manual; cache miss diubah CLEAN. Home memiliki disclaimer, tetapi preview masih berkata NOMOR AMAN. Pisahkan UNKNOWN/error, provenance dan expiry. |
| A08 **High** | backend `config/db.go:148,344–403`; scoring `helper/scoring.go:224–255` | Indeks dedup tidak unique; duplicate persist dan label/count masih bertambah. Insert+aggregate tidak atomik; risiko retry/konkurensi. Runtime duplicate 2, reporter 1, score 16 membuktikan score dedup parsial saja. Return ID persisten, idempotency dan reconciliation. |
| A09 **High** | backend `config/db.go:35–67`; `controller/sync.go:83–94`; Android build `app/build.gradle.kts:60–63` | Mongo gagal startup turun memory; health 200 bukan bukti DB live. Default mobile HTTP emulator, API domain tidak resolve dari host audit. Production strict persistence/readiness dan environment release eksplisit dibutuhkan. |
| A10 **High** | backend `seeds/seeder.go:20–62`; Android `data/local/OfflineSeedData.kt:250–370,481–543`; repository `:134–138` | Nomor/label seed dan komentar sintetis dapat tampil seperti laporan nyata; timestamp load bukan verifikasi sumber kontak. Isolasi fixture, provenance dataset dan validasi direktori; jangan promosikan seed tuduhan ke publik. |

Rincian risiko, likelihood, effort dan prioritas ada di [FEATURE_GAP_ANALYSIS](FEATURE_GAP_ANALYSIS.md); rencana perbaikan di [IMPLEMENTATION_ROADMAP](IMPLEMENTATION_ROADMAP.md). Temuan keamanan A01 bersyarat konfigurasi; audit tidak menyatakan secret deployment produksi telah diketahui. Literal secret dan token tidak disalin ke dokumentasi.

## Readiness scorecard

| Lapisan / syarat lulus | Status audit | Bukti / kekurangan |
|---|---|---|
| Kode implementasi tersedia dan merged | `PARTIAL` | Empat feature sudah remote; backend/APIdocs/web main masih kerangka. |
| Android lookup → API → Mongo → UI/Room lokal | `VERIFIED_E2E` | Fixture API baru terlihat di UI dan SQL Room, bukan seed APK. |
| Android report → API → Mongo lokal | `VERIFIED_E2E` terbatas happy path | Label fixture dari form UI ditemukan di `spam_reports`; bukan auth principal terverifikasi. |
| Auth/onboarding dan otorisasi | `PARTIAL` | Method tersedia tetapi tidak dipanggil aplikasi; invalid bearer diterima guest. |
| Dispute acknowledgment dan storage | `PARTIAL` | Handler ada, persistence `MISSING`; test 201 bukan bukti pencatatan. |
| Delta lengkap, idempoten dan revocation | `PARTIAL` | Missing page, cursor lokal, tidak ada tombstone/scheduler. |
| Panggilan nyata, role dan auto-block aman | `IMPLEMENTED_NOT_E2E_TESTED` | Kode ada; perangkat fisik/operator dan false-positive suite `BLOCKED`. |
| Offline report dapat dikirim kembali | `MISSING` | Hanya agregat lokal, tanpa antrean raw report. |
| OpenAPI sesuai wire response | `PARTIAL` | 11 operasi; envelope/schema/auth berbeda, lihat matriks. |
| Deployment dan release production | `BLOCKED` | DNS API gagal dari host, tidak ada akses staging/Play Console/database produksi. |

Tidak digunakan persentase karena bobot dan kriteria sertifikasi belum disepakati.

## Register risiko dan penanganan

Likelihood berikut adalah penilaian teknis kualitatif berdasarkan kemudahan pemicu pada baseline, bukan probabilitas kejadian produksi. Severity, likelihood, prioritas dan status implementasi merupakan dimensi berbeda. Effort mengacu lingkup minimum, bukan seluruh solusi jangka panjang. Evidence A01–A10 merujuk tabel lokasi source di atas.

| ID / severity / likelihood | Dampak pengguna dan bukti | Minimum → jangka panjang | Effort / prioritas / owner |
|---|---|---|---|
| R01 Critical bersyarat / tinggi bila default | Pemalsuan credential dan identitas; A01, `helper/auth.go:27` | Secret wajib dan invalid 401, validasi claims/trust → rotasi key, revoke, proof-of-possession | M–L / P0 / A |
| R02 High / tinggi | Fitnah massal menjadi skor berbahaya; A02 runtime 11 guest | Principal server-issued, quota, pending moderation → deteksi Sybil/brigading dan review korelasi | L / P0 / A |
| R03 High / tinggi | Pemilik nomor percaya kasus ditinjau padahal hilang; A03 | Persist dispute+ID sebelum 201 → ownership verification, SLA review dan appeal | M / P0 / A |
| R04 High / tinggi | Submit offline/HTTP error menaikkan skor lokal tanpa terkirim; A04 | Status typed dan pending durable tanpa kontribusi skor → retry idempotent, recovery UI | L / P0 / B |
| R05 High / sedang | Panggilan penting bisa salah ditolak akibat policy luas/cache lama; A05/A07 | Opt-in konservatif, whitelist/freshness/fail-open → evaluasi false-positive berlabel dan audit keputusan | L / P0 / B |
| R06 High / tinggi | Cache kehilangan update/pencabutan tuduhan; A06 | Cursor urutan terpisah, page transaction, CLEAN/delete → journal durable, generation/full resync | L / P0 / A+B |
| R07 High / tinggi | Preview “aman” padahal tidak ada data; A07 | Pisahkan found/availability/source dari enum → confidence/provenance lintas semua surface | M / P0 / B |
| R08 High / sedang | Duplicate storage, score/label tidak konsisten saat retry/concurrent; A08 | ID persisten, dedup/idempotency dan consistency strategy → reconciliation dan performance bounded | L / P0 / A |
| R09 High / sedang | Write hilang karena memory fallback; readiness palsu; A09/T20 | Production strict, readiness ping dan error jujur → recovery/backup/restore drills dan alert | M / P0 / A |
| R10 High / sedang | Kontak salah/stale atau tuduhan demo disangka faktual; A10 | Isolasi fixture, sumber dan review tanggal nyata → jadwal kurasi serta provenance versioned | M+kurasi / P1 / A+B |
| R11 High / tinggi | Endpoint reports publik mengembalikan raw `SpamReport`, termasuk reporter_user_id dan komentar tanpa moderasi; backend model/model.go:96 dan controller/lookup.go:169. Log lokal memuat nomor (service:78); hak penghapusan belum ada | DTO publik teredaksi dan komentar dimoderasi, minimisasi log/consent/retention → deletion/export workflow, backup policy dan audit akses | L / P0 untuk respons publik, P1 lifecycle / A+B |
| R12 Medium / sedang | Toggle notifikasi/overlay tidak sesuai ekspektasi atau callback gagal; `CallerIdNotifier.kt:55`, `RolePermissionManager.kt:74`, service:71 | Hubungkan setting/grant dan finally fail-open → matriks OEM/OS dan regression suite | M / P1 / B |
| R13 Medium / sedang | Salah interpretasi statistik/latency dan rilis tak sesuai kanal; repository:97, web `publicStats.js:45`, build:51/80 | Perbaiki sumber metrik, environment/signing dan kanal → benchmark/observability terukur, CI release gate | M / P1 / A+B |

R11 membuktikan bentuk respons dan tidak adanya sanitasi dari source; ini bukan klaim data pengguna sungguhan telah bocor pada deployment publik. Reporter ID pseudonim bukan nomor pelapor langsung, tetapi masih memungkinkan korelasi. Sumber resmi privasi dirujuk di bagian referensi; audit ini tidak menerbitkan opini kepatuhan hukum. Ancaman, aset dan owner ditautkan pula ke FR/NFR pada PRD agar mitigasi dapat diuji.

## Batas audit dan keputusan produk

- Pembacaan source mencakup empat repo, PRD asli seluruh 529 baris, DESIGN 78 baris dan AGENTS. Tidak ada perubahan source produk atau OpenAPI. Source Go menggunakan `net/http` dan Mongo driver; nama/komentar gocroot bukan bukti dependency framework.
- MongoDB pengujian 8.3.9 di loopback port 28018, database `calltag_audit`, direktori `/tmp` khusus. Tidak memakai MongoDB host port 27017 atau database produksi. Semua request tulis hanya menuju backend lokal.
- Build APK dan emulator API 34 berhasil. Tidak melakukan panggilan darurat, pengiriman laporan ke instansi, pengujian operator fisik atau pemanggilan telepon riil.
- Direktori seed bukan dataset seluruh Indonesia dan sumber kontak belum divalidasi per entri. Tidak ada jaminan nomor darurat tertentu tersambung atau WhatsApp merespons.
- Moderator minimum **wajib P1 sebelum pilot publik**: login internal aman, antrean laporan/dispute, koreksi whitelist/false-positive, CRUD direktori dengan sumber/tanggal dan audit log. Cukup endpoint internal dan satu panel sederhana; analytics besar/super_admin ditunda.
- Privasi: salted hash bukan anonimisasi penuh. Consent, retention/delete, pembatasan komentar/PII, redaksi log dan backup memerlukan implementasi dan kebijakan. Kajian ini temuan teknis, bukan opini kepatuhan hukum.

## Referensi resmi dan konflik panduan

- [Android CallScreeningService](https://developer.android.com/reference/android/telecom/CallScreeningService): callback wajib dijawab dalam 5 detik. Panggilan kontak biasanya dikecualikan OS tanpa READ_CONTACTS; tidak adanya query buku kontak di app **bukan otomatis bug bypass kontak**. Deadline 800 ms API tidak membatasi seluruh callback Room/DataStore.
- [Google Play target API](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en): pada tanggal audit aplikasi/update baru membutuhkan target API 36 sejak 31 Agustus 2026. Source target 34 belum memenuhi jalur Play umum; APK sideload akademik harus disebut terpisah. Tidak ada keputusan menaikkan SDK otomatis dalam audit dokumen ini.
- [UU 27/2022 — JDIH BPK](https://peraturan.bpk.go.id/Details/229798/uu-no-27-tahun-2022): rujukan hak subjek dan tata kelola pemrosesan, bukan bukti bahwa implementasi memenuhi seluruh kewajiban.
- DESIGN mengelompokkan CLEAN/verified pada palet yang sama; PRD 1.1 menegaskan **warna tidak membuktikan verifikasi**. AGENTS menyebut no-tests untuk Android dan ini tetap benar; satu suite Go dan 6 tes web berbeda repo. AGENTS menginstruksikan mempertahankan fallback saat implementasi; audit hanya mengusulkan pemisahan tipe hasil/fail-closed produksi, tidak menghapus fallback.

Sumber resmi diakses 10 November 2026. Detail diagram, skema, kontrak, hasil uji, dan keputusan ada pada [indeks dokumentasi](README.md).
