# Rencana QA dan bukti verifikasi

Tanggal pelaksanaan **10 November 2026 WIB**. Baseline dan definisi status ada di [AUDIT_REPORT](AUDIT_REPORT.md); implementasi patch yang disarankan belum dikerjakan. PASS berarti assertion kasus tersebut terbukti, bukan semua fitur aman. FAIL di bawah menunjukkan pelanggaran kebutuhan target yang berhasil direproduksi; bukan kegagalan menjalankan alat. BLOCKED digunakan ketika perangkat/konfigurasi/implementasi yang dibutuhkan belum ada.

## Lingkungan pengujian dan isolasi

| Komponen | Nilai yang benar-benar digunakan |
|---|---|
| Host | Linux, shell bash; waktu laporan Asia/Jakarta |
| Go | `go1.27.3-X:nodwarf5 linux/amd64` |
| Java | OpenJDK `27.0.1.0.3`; build ini berhasil, CI repo memakai JDK 17 |
| Android | SDK/target 34, emulator `Pixel_5_Light`, Android 34 x86_64; emulator 37.1.11 |
| Node | `v26.13.0` |
| Mongo | `mongod v8.3.9`, loopback `127.0.0.1:28018`, DB `calltag_audit`, data `/tmp/calltag-integration-audit.qrPmTN` |
| API Mongo | `http://127.0.0.1:8080`; emulator memakai APK default `http://10.0.2.2:8080/` |
| API memory | `127.0.0.1:18080`, URI kosong; API 18081 diarahkan port Mongo 28019 yang tidak ada |
| Tool tambahan | adb, sqlite3, mongosh, Chromium; Mermaid CLI 12.2.0 dengan Mermaid 12, Puppeteer untuk parse/render |

Mongo host port 27017 sudah ada sebelum audit dan tidak disentuh. DB/direktori uji baru tidak menggunakan production, credential tersimpan, atau nomor pengguna. Nomor panjang pada test merupakan fixture lokal sintetis, bukan verifikasi pemilik atau data publik. Fixture yang menyerupai nomor telepon tidak boleh dipromosikan sebagai daftar scam nyata. Tidak ada telepon darurat, WhatsApp keluar, atau laporan ke instansi yang dikirim.

Backend asli membaca environment, tidak otomatis memuat `.env`. [Contoh environment](examples/backend.env.example) aman disediakan sebagai dokumentasi; isi secret harus dihasilkan sendiri dan diinjeksi secara privat. Baseline mempunyai secret fallback yang sengaja **tidak dicetak**; ini kelemahan source, bukan rekomendasi menjalankan produksi dengan konfigurasi tersebut.

## Hasil yang diamati

| ID / hasil | Command atau input sintetis | Expected / actual dan batas bukti |
|---|---|---|
| T01 PASS | backend: `env MONGOSTRING= MONGODB_URI= go test ./... -count=1` | Suite `TestBackendPRDEndpoints` lulus; packages lain no test files. Hanya happy path API in-memory, bukan Mongo E2E. |
| T02 PASS | backend: `go vet ./...` | Exit 0, tidak ada diagnostic. Bukan bukti auth/logic benar. |
| T03 PASS | Android: `./gradlew assembleDebug --offline --console=plain` | BUILD SUCCESSFUL, 38 tasks up-to-date. Tidak ada test/androidTest; tidak mengklaim unit/instrumentation suite Android. |
| T04 PASS | web: `node --test --test-isolation=none tests/frontend-state.test.mjs` | 6 tests, 6 pass, 0 fail: unknown, initial demo, failed report, HTML 200, release status, directory count. |
| T05 PASS | apidocs: `node --check app.js`; parse JSON dan hitung operasi | Sintaks JS valid, OpenAPI 3.0.3, 11 operasi; bukan schema-contract validation semua wire response. |
| T06 PASS | Start Mongo 28018 terisolasi + API 8080 dengan `DB_NAME=calltag_audit` | Startup Mongo-connected; seed emergency 23, official 5, phone 6. Actual collections 5 diperiksa melalui mongosh. Data ini seed, bukan hasil kurasi resmi. |
| T07 PASS | HTTP harness lokal: health/auth/lookup/reports/national/local/hub/sync/stats/report/dispute | Seluruh 11 handler reachable. health/auth/read 200, report/dispute 201. Reachability tidak membuktikan persistence dispute. |
| T08 PASS | Auth fixture `081200000000`; report `081200000001`, tag `AUDIT_SYNTHETIC`, JWT di memory harness | Mongo insert dan GET reports readback 2 setelah duplicate; report_id komposit bukan `_id`. Token tidak ditulis ke log/evidence. |
| T09 FAIL terhadap idempotency | Kirim dua payload identik dengan principal sama | Actual 2 dokumen; unique_reporters 1, risk_score 16, SUSPICIOUS. Kontribusi skor dedup parsial, bukan anti-repeat storage atau quota. |
| T10 FAIL terhadap auth | POST reports dengan bearer invalid / tanpa auth + header fixture | Keduanya 201, bukan 401. GET phone reports tanpa auth 200. Ini perilaku aktual, bukan kontrak aman. |
| T11 FAIL terhadap anti-Sybil/rate limit | 11 POST lokal target `081200000004`, `X-Device-Id: audit-abuse-0..10`, tanpa JWT | Seluruh 201; Mongo akhir DANGER_SCAM, score 100, unique 11. Lima pertama SPAM, bukan DANGER; bobot trust guest 0,8 per principal. Tidak ada pengujian penyerangan host publik. |
| T12 FAIL terhadap dispute persistence | POST disputes dengan nomor fixture, owner Audit, reason fixture, `audit@example.invalid` | API 201 UNDER_REVIEW; tidak ada collection/model/write dispute. Tidak bisa readback atau diproses moderator. |
| T13 PASS validasi yang ada | phone abc / category INVALID / report 112, method GET reports yang salah, route tak dikenal | Actual 400/400/400, 405, 404. Report 112 mengembalikan 400 sebelum cabang 403, bukan whitelist workflow lengkap. |
| T14 PASS normalisasi terbatas | Lookup `%2B6281299887766`, `%20081200000001%20`, `112`, `abc` | Encoded+ dan whitespace terproses; short code 112 found false/CLEAN 200; abc 400. E.164 ASCII / max length / unicode / path segments belum tervalidasi menyeluruh. |
| T15 FAIL terhadap pagination | Dua dokumen DB test versi 9000000000001/2, risk 40/99; since 9000000000000, limit 1 | Halaman 1 mengirim versi 2/cursor 2; halaman 2 kosong; versi 1 terlewat. Fixture disisipkan hanya DB terisolasi untuk menguji invariant urutan. |
| T16 PASS lokal / VERIFIED_E2E lookup | Install APK; `am start ... --es screened_phone +6281200000001` | Nomor tidak ada di seed APK; UI menampilkan AUDIT_SYNTHETIC dan 16/100; SQL Room: SUSPICIOUS, 16, reporter 1, label sama. Rantai mobile→API→Mongo→UI/Room dibuktikan lokal. Intent melatih lookup biasa, bukan incoming Telecom. |
| T17 PASS lokal / VERIFIED_E2E report | UI tab Beri Tag: nomor `081200000005`, tag `AUDIT_MOBILE_E2E`, submit | UI `#Tag berhasil dikirim`; Mongo spam_reports count 1 untuk tag fixture dan nomor +6281200000005; Room CLEAN, 12.8, label sama. Tidak membuktikan onboarding auth; report guest diterima. |
| T18 PASS offline cache/cold start | Airplane on + svc wifi/data disabled, dumpsys `Active default network: none`, force-stop/start lookup T16 | UI masih AUDIT_SYNTHETIC, 16/100 dari cache persisten. Airplane mode saja sempat menyisakan Wi-Fi; tes final memakai kedua radio off. Koneksi dipulihkan sesudahnya. |
| T19 PASS membuktikan memory fallback | API 18080 tanpa URI; API 18081 Mongo URI 28019 tak terjangkau | health 200 `in-memory-seeded`; harness 18080 report/readback berhasil hanya memory. Tidak durable dan bukan Mongo fallback live sesudah DB mati. |
| T20 FAIL terhadap readiness | Shutdown hanya Mongo 28018, lalu health 8080 dan lookup/stat | health tetap 200 `mongodb-connected`, lookup 500, statistik 200 meski DB unavailable. Health menggunakan flag startup; rinci respons internal tidak dimasukkan dokumen. |
| T21 BLOCKED produksi | `curl --max-time 8 https://api.calltag.id/health` | DNS gagal (`Could not resolve host`, HTTP 000) dari host audit. Tidak menyimpulkan seluruh jaringan dunia atau deployment masa depan tidak ada. |
| T22 BLOCKED panggilan fisik | Role Telecom/operator, OEM, permission deny/revoke, actual screening/auto-block/emergency call | Emulator lookup/form bukan telepon fisik. Tidak ada test device/operator sah yang dipakai audit ini; jangan klaim panggilan nyata lulus. |
| T23 BLOCKED fitur belum ada | Outbox raw report, retry reconnect, moderator RBAC, dispute recovery, tombstone dan migration baru | Implementasi tidak ada sehingga acceptance target belum dapat lulus. Perlu patch B01–B14 dan test terpisah. |
| T24 PASS restart MongoDB lokal | Setelah shutdown MongoDB 28018, restart dari dbpath yang sama; hitung fixture dengan mongosh dan GET lookup fixture mobile | Dokumen `AUDIT_SYNTHETIC` tetap 4 dan `AUDIT_MOBILE_E2E` tetap 1; collection disputes tetap false. GET lookup fixture mobile kembali HTTP 200 setelah driver reconnect. Ini membuktikan persistensi fixture melintasi restart MongoDB lokal, bukan restart backend/proses penuh atau backup restore. |

T20 membedakan health yang menyesatkan dari lookup yang memang gagal. Setelah uji DB down, hanya layanan/dataset sementara milik audit yang dihentikan; tidak ada data pengguna dihapus. Bila direktori test masih tersimpan lokal, itu artefak sementara dan tidak dimasukkan commit.

## Prosedur reproduksi aman

Jalankan hanya pada mesin pengembangan dan database baru. Pastikan port bebas, jangan mengarahkan suite `main_test.go` ke URI produksi karena test tersebut melakukan write. Gunakan direktori `mktemp -d` khusus Mongo, bind loopback, port 28018 dan nama DB audit yang jelas.

```bash
# Terminal Mongo: ganti path dengan direktori baru hasil mktemp -d.
mongod --dbpath /tmp/DIREKTORI_AUDIT_BARU --port 28018 --bind_ip 127.0.0.1 --nounixsocket

# Terminal API, dari repo backend. Secret kuat harus diinjeksi privat.
env MONGOSTRING=mongodb://127.0.0.1:28018 DB_NAME=calltag_audit PORT=8080 go run .

# GET read-only ke lingkungan lokal.
curl --fail --max-time 8 http://127.0.0.1:8080/health
curl --fail --max-time 8 'http://127.0.0.1:8080/api/v1/phones/%2B6281200000001'

# Bukti metadata tanpa token/PII pengguna: filter hanya fixture audit.
mongosh --quiet mongodb://127.0.0.1:28018/calltag_audit --eval \
 'printjson({n:db.spam_reports.countDocuments({custom_tag:"AUDIT_SYNTHETIC"}),disputes:db.getCollectionNames().includes("disputes")})'

# Android: build memakai URL default emulator sesuai baseline.
./gradlew assembleDebug --offline --console=plain
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n id.calltag.app/.MainActivity --es screened_phone +6281200000001
```

Nilai `AUDIT_BASE`/fixture pada harness sementara tidak dikirim ke server publik. Untuk auth, simpan token hanya di memori test dan log status/field keys, jangan response user/token penuh. Query Room memakai `run-as id.calltag.app` pada APK debug, menyalin DB+WAL ke `/tmp`, lalu SELECT khusus nomor fixture. Jangan membuat dump database pengguna sebagai artefak PR.

### Fixture dan expected untuk tes lanjutan

```json
{
  "phone": "081200000005",
  "category_code": "APK_SNIFFING",
  "custom_tag": "AUDIT_SYNTHETIC",
  "comment": "Fixture lokal; bukan laporan warga",
  "call_type": "CELLULAR_CALL"
}
```

Contract test harus memvalidasi wrapper `{success,code,message,data,timestamp}` dan nullable/omitted fields, status 200 vs 201/400/401/403/405/409/413/422/429/500 sesuai desain yang akhirnya disepakati. 409/422/429 bukan status yang sudah terbukti pada baseline. Jangan membuat test yang mengharapkan semua status itu tersedia sekarang tanpa patch.

## Traceability acceptance

| Kebutuhan | Endpoint / komponen | Test saat audit atau acceptance berikut |
|---|---|---|
| FR-001, NFR-006 | RolePermissionManager/manifest/Compose | T03/T16 sebagian; deny/revoke/TalkBack/API 26/29/33/34/fisik T22 belum lulus |
| FR-002 | GET phones dan GET reports, Room | T14/T16/T18; tambah not found / cache miss / stale / authoritative found false / null schema |
| FR-003, FR-014, NFR-003 | Screening service/policy/notifier/preferences | T22; test truth table DANGER/SPAM/category/whitelist/seed/stale, deadline Room / DataStore error dan setting |
| FR-004, FR-007, FR-012, NFR-001 | POST auth/reports, scoring, indexes | T08–T11/T17 menunjukkan partial; tambah 401/403/quota/duplicate/parallel/replay/score threshold/time clock |
| FR-005, FR-008 | Sync GET/outbox Room/WorkManager | T15 FAIL/T23 BLOCKED; multi-page >250, equal version, restore, CLEAN/delete, retry/cancellation perlu patch |
| FR-006, FR-010, FR-011 | Disputes dan admin PROPOSED | T12 FAIL/T23 BLOCKED; DB restart, owner view, role 403, decision audit dan whitelist revocation |
| FR-009, NFR-007 | GET emergency/hub, Room directory, external intents | T06/T07 hanya API; sumber per kontak dan dialer/WA denied/offline fresh install perlu test terpisah tanpa panggilan riil |
| FR-013 | Semua 11 OpenAPI, web 8 callers | T04/T05; wire-schema snapshots, mock parity, zero-live-stats dan environment staging |
| FR-015/016 | Pesan/APK/GPS Post-MVP | Di luar gate MVP; spesifikasi dan permission test baru |
| FR-017, NFR-002 | Principal/delete/export/moderation | T23; test akses own records, redaction, consent/delete/retention saat fitur tersedia |
| NFR-004 | Mongo/Room migration/backup restore | Persistence readback T08/T17 dan restart MongoDB lokal T24; restart backend/proses penuh, restore dan migrasi belum diuji sebagai gate baru |
| NFR-005 | Latency cache/API/callback | Belum benchmark; field latency app bukan hasil end-to-end |
| NFR-008/009 | CI/environments/signing | T01–T05 compile/check saja; staging/Play Console/signing/restore T21/T23 BLOCKED |

## Verifikasi dokumentasi

Validator memakai filesystem untuk mengecek target tautan relatif serta browser Chromium+Mermaid untuk **parse dan render**, bukan sekadar menghitung code fence. SVG hanya artefak sementara `/tmp`, tidak disimpan sebagai diagram sumber repo. Hasil final dicatat pada [DOC_VALIDATION](DOC_VALIDATION.md). Versi renderer lokal tidak menjamin identik setiap saat dengan versi GitHub, tetapi diagram memakai sintaks standar flowchart/sequence/ER/state.

## Batas dan pekerjaan yang belum dilakukan

Tidak dilakukan benchmark latency/beban, Play Console check akun, sertifikasi audit hukum, verifikasi kontak lewat telepon, test perangkat fisik/operator, automated Android unit/instrumentation, tes race untuk seluruh Mongo, backup restore, migration non-destructive, dan monitoring produksi. Banyak acceptance memerlukan implementasi yang belum ada. Semua ini tetap tercatat sebagai belum terverifikasi, bukan dianggap lulus karena compile atau test happy path.
