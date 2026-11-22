# MASTER PROMPT — Audit End-to-End Ekosistem CallTag & Pembaruan PRD Profesional

> **Cara menggunakan:** Tempel seluruh isi file ini ke AI coding agent yang memiliki akses baca ke seluruh repository GitHub organisasi `CallTag` (misalnya Codex/agent di IDE dengan akses GitHub). Untuk menghasilkan perubahan file yang nyata, agent juga memerlukan workspace lokal atau izin menulis repository. **Tugas utama adalah audit berbasis bukti, validasi integrasi, dan pembaruan PRD yang sudah ada, bukan membuat produk baru dari nol.**

## 0. PERAN DAN MISI

Bertindaklah sebagai tim gabungan yang terdiri dari:
- **Principal Software Architect** untuk arsitektur lintas repository;
- **Senior Android Engineer** (Kotlin, Jetpack Compose, Room, Retrofit, Android CallScreeningService);
- **Senior Backend Engineer** (Go, pendekatan gocroot, REST API);
- **Database Architect** (MongoDB serta SQLite/Room untuk mode offline);
- **API Contract & Integration QA Engineer** (OpenAPI/Swagger, contract test, end-to-end test);
- **Application Security & Privacy Reviewer** (autentikasi, otorisasi, perlindungan data, penyalahgunaan pelaporan);
- **Senior Product Manager / Business Analyst / Technical Writer** (PRD, ruang lingkup produk, roadmap, kebutuhan akademik).

**Misi:** lakukan pemeriksaan menyeluruh atas aplikasi **CallTag**, khususnya integrasi **Android mobile ↔ Go backend ↔ MongoDB**. Identifikasi apa yang benar-benar sudah bekerja, belum terintegrasi, baru berupa mock/seed/fallback, belum dibuat, keliru implementasinya, atau berisiko. Setelah audit, **perbarui PRD yang sudah ada** agar sinkron dengan kode nyata dan rencana pengembangan yang layak, lengkap dengan model basis data, flowchart, dan diagram teknis dalam Markdown. Tulis laporan dan dokumentasi dalam **Bahasa Indonesia profesional**, sambil mempertahankan istilah teknis, nama variabel, path, endpoint, dan kode apa adanya.

Konteks penggunaan: proyek akademik/proyek akhir yang dikerjakan **oleh tim 2 orang**. Buat rekomendasi yang realistis dan dapat diprioritaskan; bedakan MVP wajib dari target jangka panjang. Tidak perlu mengada-ada untuk terlihat enterprise.

---

## 1. REPOSITORY DAN BASELINE YANG WAJIB DIAUDIT

Organisasi: **https://github.com/CallTag**

1. **Mobile Android:** https://github.com/CallTag/CallTag
   - Fokus utama: `app/src/main/`, konfigurasi Gradle, `AndroidManifest.xml`, service screening, Compose UI, Room, Retrofit, repository, preferences, DTO.
   - Dokumen yang harus dipertahankan dan dijadikan baseline: `PRD.md`, `DESIGN.md`, `AGENTS.md`.
2. **Backend Go:** https://github.com/CallTag/backend
   - Fokus utama: routing, controller, model, konfigurasi MongoDB, helper auth/scoring, seed, test, environment, deployment.
3. **API Documentation:** https://github.com/CallTag/apidocs
   - Fokus utama: OpenAPI/Swagger, request/response schema, auth, status code, error schema, kesesuaian kontrak.
4. **Website publik (sekunder):** https://github.com/CallTag/calltag.github.io
   - Fokus: fitur publik yang perlu sinkron dengan backend dan dokumen produk; audit setelah mobile/backend.

**PENTING: Jangan hanya membaca branch `main`.** Saat peninjauan awal, ditemukan:
- `backend/main` hanya berisi berkas dasar, sedangkan branch `backend/codex/workspace-initial-import` memiliki struktur kode Go seperti `route/route.go`, `controller/`, `model/`, `config/`, `main_test.go`, dan seed data.
- `apidocs/main` juga berisi berkas dasar, sedangkan `apidocs/codex/workspace-initial-import` memiliki `openapi.json`, `index.html`, dan file Swagger UI.
- Mobile memiliki branch `main` dan `codex/frontend-mobile-refresh`.
- Website memiliki branch `main` dan `codex/frontend-mobile-refresh`.

Anggap ini sebagai **petunjuk awal, bukan jaminan kondisi terbaru**. Verifikasi ulang branch, commit SHA, tanggal, pull request, status merge, dan file yang tersedia **pada saat audit**. Bandingkan implementasi `main` vs branch pengembangan yang relevan. Jangan menganggap kode di feature branch sudah ter-deploy atau sudah tersedia bagi pengguna. Jika ada branch lain yang lebih baru/relevan, jelaskan alasan pemilihannya.

**Output baseline:** tabel `Repository | Branch/Commit | Kondisi | Bukti (file:line) | Dampak pada integrasi`.

---

## 2. ATURAN KERJA DAN STANDAR PEMBUKTIAN

1. Baca sumber aslinya, bukan hanya README, PRD, atau komentar di source code. **Komentar bukan bukti implementasi**.
2. Untuk setiap temuan tulis **lokasi bukti**: repository, branch, path, fungsi/kelas, rentang baris jika tersedia; sertakan perintah dan ringkasan output jika ada pengujian.
3. Gunakan label status yang konsisten:
   - **VERIFIED_E2E** = benar-benar diuji dari aplikasi/API sampai penyimpanan database;
   - **IMPLEMENTED_NOT_E2E_TESTED** = kode ada, tetapi belum terbukti end-to-end;
   - **PARTIAL** = sebagian alur terimplementasi;
   - **MOCK_OR_LOCAL_ONLY** = seed, demo, cache, atau fallback lokal;
   - **SPEC_ONLY** = hanya tertulis di PRD/OpenAPI;
   - **MISSING** = tidak ditemukan di baseline audit;
   - **BLOCKED** = tak dapat diverifikasi karena lingkungan, izin, konfigurasi, atau dependensi.
4. **Jangan pernah menyatakan “sudah tersambung” hanya karena URL endpoint sama atau aplikasi berhasil dikompilasi.**
5. Bedakan fakta hasil audit, inferensi teknis, dan rekomendasi desain. Jika belum bisa dibuktikan, tulis **“Belum terverifikasi”** dan jelaskan cara memverifikasinya.
6. Jangan mengarang hasil build, log, status server, latency, hasil test, database produksi, deployment, atau URL API aktif. Jangan menganggap `https://api.calltag.id` aktif hanya karena tercantum di OpenAPI.
7. Jangan menampilkan token, password, API key, data pribadi, atau kredensial; samarkan jika ditemukan dan jelaskan risikonya.
8. Audit dahulu, kemudian dokumentasikan. **Jangan menghapus fitur, merombak arsitektur, mengubah endpoint secara breaking, atau menulis ulang semua source code tanpa alasan dan persetujuan.**
9. Perubahan dokumentasi boleh dikerjakan langsung pada **branch kerja terpisah**. Untuk perbaikan aplikasi/backend, buat rencana patch terurut; implementasikan kode hanya jika akses dan instruksi eksekusi jelas, dengan pengujian serta pull request terpisah.
10. Jika agent tidak memiliki terminal atau akses clone, lakukan audit statis semaksimal mungkin dan tulis batasannya. Jangan menyamakan audit statis dengan integration testing.

---

## 3. TAHAP A — INVENTARISASI PRODUK DAN KODE

### 3.1 Peta modul dan kesiapan
Buat inventarisasi menyeluruh berdasarkan kode riil, meliputi:
- Onboarding dan permission Android (Call Screening role, notification, overlay, battery/background restriction jika relevan);
- caller identification, incoming-call screening, spam guard, auto-block, notifikasi, overlay;
- pencarian reputasi nomor telepon dan normalisasi nomor Indonesia ke E.164;
- kategori spam/scam dan logika perhitungan `risk_score`, ambang `reputation_level`;
- pelaporan komunitas, komentar/tag, riwayat laporan, pencegahan duplikasi;
- autentikasi dan identitas perangkat, otorisasi, sesi/token;
- sanggahan/dispute, verifikasi nomor/whitelist dan tindak lanjut;
- direktori darurat nasional dan lokal, pencarian kota/provinsi, pemanggilan nomor;
- pusat pelaporan resmi, tautan keluar, WhatsApp, dan sumber data;
- sinkronisasi offline, seed data, pengelolaan cache, retry, konflik data;
- setelan pengguna, status proteksi, manajemen izin, aksesibilitas dan UI states;
- dashboard/admin/moderator (jika belum ada, tandai sebagai kebutuhan usulan, bukan fitur yang sudah dibuat);
- website publik, metrik publik, dan sumber data API.

Untuk setiap fitur: `ID | User journey | UI | ViewModel | Repository | Retrofit/API | Controller/backend | MongoDB/Room | Status | Bukti | Masalah | Prioritas`.

### 3.2 Audit keandalan mobile
Periksa khusus:
- `ApiService.kt`, `NetworkModule.kt`, `ApiDtos.kt`, `CallTagRepository.kt`, `CallTagApplication.kt`, Room DAOs/entities, `CallTagPreferences.kt`;
- `CallTagScreeningService`, `CallerIdNotifier`, `PhoneNormalizer`, `RolePermissionManager`;
- `BuildConfig.BASE_API_URL`, perbedaan emulator `10.0.2.2`, perangkat fisik, staging, production, HTTP/HTTPS dan konfigurasi cleartext;
- state loading/empty/error/offline, klik tombol yang tidak memiliki aksi nyata, deep link dan permission flow;
- crash risk, lifecycle, race condition, timeout, coroutine cancellation, thread safety, duplicate events, privacy leak;
- perilaku cache saat data stale dan saat jaringan kembali aktif;
- perlakuan nomor yang tidak dikenal: **“tidak ada laporan” tidak otomatis berarti “nomor aman/terverifikasi”**;
- validitas klaim fitur dan batasan Android/Google Play pada versi SDK yang benar-benar digunakan.

### 3.3 Audit backend dan database
Periksa routing, HTTP method, validasi input, penggunaan context/timeout, error handler, autentikasi, otorisasi, hashing/penanganan token, rate limiting, anti-spam, idempotency, deduplikasi pelapor, MongoDB collection/index/unique index, startup/health, seeding, environment, observability, concurrency, dan konsistensi status kode respons.

Pastikan data benar-benar dibaca/ditulis ke **MongoDB** bila diklaim demikian; jangan menyimpulkan hal ini hanya karena ada import driver atau deklarasi collection. Pisahkan perilaku saat MongoDB online vs fallback in-memory/seed.

---

## 4. TAHAP B — AUDIT INTEGRASI MOBILE ↔ BACKEND ↔ DATABASE ↔ OPENAPI

### 4.1 Matriks endpoint wajib
Mulailah dari seluruh endpoint yang ditemukan di source code dan OpenAPI. Minimal periksa kontrak awal berikut (verifikasi ulang, jangan asumsikan sudah diimplementasikan semua):

| Method | Endpoint |
|---|---|
| GET | `/health` |
| POST | `/api/v1/auth/device` |
| GET | `/api/v1/phones/{e164}` |
| GET | `/api/v1/phones/{e164}/reports` |
| POST | `/api/v1/reports` |
| POST | `/api/v1/disputes` |
| GET | `/api/v1/emergency/national` |
| GET | `/api/v1/emergency/local` |
| GET | `/api/v1/official-channels` |
| GET | `/api/v1/sync/offline-bundle` |
| GET | `/api/v1/stats/public` |

Untuk setiap endpoint bandingkan **4 lapisan**: Mobile Retrofit + DTO ↔ OpenAPI ↔ Backend route/controller/model ↔ operasi MongoDB, dengan kolom:
`Method/path | Mobile caller | Headers/auth | Path/query/body | Response fields/types | HTTP status/errors | Controller | DB operations | Cocok/tidak | Status bukti | Langkah perbaikan`.

Uji secara spesifik:
- E.164 `+62...`: normalisasi, path URL encoding, tanda `+`, whitespace, nomor darurat pendek, nomor invalid;
- kecocokan `@SerializedName`, field wajib vs opsional, nilai default, `null`, tanggal, tipe angka, array/list, enum, wrapper `{success, data, ...}`;
- kesesuaian `201` vs `200`, `401`, `403`, `404`, `409`, `422`, `429`, `500`, pengolahan error body;
- token Bearer, `X-Device-Id`, identitas unik dan penyalahgunaan fixed/static device ID;
- login/device registration di aplikasi benar-benar dipanggil dan token disimpan serta digunakan, bukan hanya endpoint tersedia;
- mekanisme report sukses: **server persistence** vs laporan hanya tersimpan di Room; apakah UI menampilkan status sumber yang jujur;
- sinkronisasi data offline, cursor/version, pagination, retry, pending write, duplicate event, replay, conflict resolution;
- skor dan label reputasi identik antara mobile, backend, database, dan PRD;
- whitelist/dispute/verifikasi dan kemampuan koreksi kesalahan label;
- direktori darurat: asal data, freshness, lokasi yang dipilih, kesalahan nomor, validitas kanal;
- penyebab fallback diam-diam yang menutupi API gagal;
- apakah endpoint website, app, dan OpenAPI menunjuk environment/versi yang sama.

### 4.2 Integration test dan bukti teknis
Jika akses environment memadai, lakukan bertahap:
1. Periksa versi tooling dan dependency, siapkan contoh `.env.example` yang aman (tanpa secrets).
2. Jalankan `go test ./...`, static checks yang tersedia, dan health check backend.
3. Nyalakan MongoDB lokal/test container atau environment test yang sah. Hindari menulis ke database produksi.
4. Jalankan backend dan uji tiap endpoint dengan request sah, invalid, unauthenticated, duplicate, serta server/database unavailable.
5. Jalankan build Android (`./gradlew assembleDebug` bila toolchain tersedia), lalu uji mobile emulator/device dengan `BASE_API_URL` yang sesuai.
6. Uji jalur end-to-end terukur: **mobile → REST API → controller → MongoDB → REST response → UI/Room**; buktikan record dibuat dan dapat dibaca ulang.
7. Uji offline: cache hit/miss, airplane mode, cold start, restart, report offline, pemulihan koneksi, sync serta penanganan konflik.
8. Uji permission dan incoming call secara realistis sesuai kemampuan emulator/device. Jangan mengklaim screening telepon nyata teruji jika hanya method unit-test yang berjalan.
9. Catat command, environment, input (data sintetis), expected, actual, serta hasil `PASS / FAIL / BLOCKED` dengan bukti log aman.

**Wajib simpulkan secara eksplisit:**
- **Apakah mobile dan backend sudah tersambung?**
- **Jika belum/tidak pasti, persis di lapisan mana putusnya?** (branch, deployment, URL, network, auth, DTO, routing, database, atau UI fallback)
- **Apa tiga hingga sepuluh tindakan teknis pertama untuk membuktikan/memperbaiki koneksi?**

---

## 5. TAHAP C — GAP ANALYSIS: FITUR, ADMIN, PRODUK, UX, DAN KEAMANAN

Susun **feature gap matrix** dan evaluasi apakah CallTag butuh fungsi berikut. **Jangan otomatis menambah fitur hanya agar terlihat lengkap**: justifikasi berdasarkan user journey, pencegahan penyalahgunaan, kapasitas tim 2 orang, dan MVP.

### 5.1 Evaluasi admin/moderasi
Analisis apakah CallTag memerlukan **web admin/moderator** atau MVP cukup melalui endpoint admin dan panel sederhana. Minimal nilai kebutuhan:
- login admin aman dengan **RBAC** (misal `user`, `moderator`, `admin`; `super_admin` hanya bila perlu);
- daftar dan pencarian nomor/laporan, filter status, verifikasi bukti, anti-duplicate/anti-brigading;
- review report yang disengketakan, dispute resolution, pemulihan false positive;
- whitelist/verified business/official number dengan bukti dan masa validitas;
- CRUD kontak darurat dan official channels, sumber resmi, tanggal terakhir verifikasi, audit perubahan;
- dashboard operasional sederhana (report masuk, pending moderation, dispute unresolved), bukan analytics berlebihan;
- audit log tindakan moderator dan pembatasan akses ke data sensitif;
- batasan publik vs internal pada daftar nomor, komentar, identitas pelapor.

Hasilkan **keputusan yang dibela secara teknis**: `wajib MVP`, `disarankan P1`, `opsional P2`, atau `tidak diperlukan`, disertai biaya implementasi, dependensi, risiko tanpa fitur tersebut, dan alternatif sederhana.

### 5.2 Evaluasi risiko inti
Audit prioritas tinggi:
- **False positive** dan dampak auto-block terhadap panggilan penting;
- penyalahgunaan laporan massal, konflik kepentingan, laporan fitnah dan bukti;
- data PII, nomor telepon, device ID, retention/deletion, consent, enkripsi, least privilege;
- minimisasi data dan kepatuhan UU PDP Indonesia (bedakan temuan teknis dari nasihat hukum);
- keamanan autentikasi, secret management, CORS (khusus web), HTTPS, abuse rate-limit;
- reliabilitas emergency call ketika internet tidak ada, nomor lokal salah, atau lokasi tidak akurat;
- Android permissions, batasan fitur call screening, kebijakan distribusi aplikasi, dan notifikasi;
- unavailable/stale backend, server timeout, dan UI yang mengklaim data aman padahal hanya tebakan;
- kebutuhan observability, backup/restore, migrasi database, dan pengujian regresi.

Buat tabel: `ID | Masalah | Dampak pengguna | Severity (Critical/High/Medium/Low) | Likelihood | Bukti | Solusi minimum | Solusi jangka panjang | Estimasi S/M/L | Prioritas P0-P3`.

---

## 6. TAHAP D — PERBARUI PRD YANG SUDAH ADA, JANGAN TULIS ULANG DARI NOL

**File utama yang harus diperbarui: `CallTag/PRD.md` pada branch kerja terpisah.**

Aturan:
1. **Baca seluruh PRD.md saat ini terlebih dahulu**, termasuk tabel existing, goals, personas, taxonomy, acceptance criteria, arsitektur, MongoDB, API, roadmap.
2. **Pertahankan visi, ruang lingkup, nama produk, identitas desain, kategori dan terminologi yang masih valid.** Revisi secara terarah, bukan penggantian total.
3. Pertahankan informasi lama yang benar; tandai konflik dengan kode lewat catatan perubahan dan keputusan arsitektur.
4. Bedakan secara tegas **Current State (as-is)**, **MVP Target State (to-be)**, dan **Post-MVP**. Jangan menyajikan fitur target sebagai fitur yang sudah ada.
5. Tingkatkan nomor versi dokumen dari versi aktual secara wajar, tambah `Last Updated`, reviewer/status, repository+commit basis audit, dan **Changelog** (apa yang diperbarui serta alasannya).
6. Pastikan konsisten dengan `DESIGN.md` dan `AGENTS.md`; bila ada pertentangan, dokumentasikan, jangan diam-diam menimpa panduan desain.
7. Semua diagram harus **Mermaid** di code fence `mermaid` agar dapat dirender GitHub dan dibaca langsung pada `.md`. Jangan mengandalkan gambar berbayar atau diagram yang hanya bisa dibuka aplikasi khusus.
8. Untuk MongoDB, ERD adalah **model hubungan logis dokumen**, bukan klaim bahwa MongoDB memakai foreign key relasional. Jelaskan embed vs reference dan pola akses.
9. Setiap kebutuhan harus punya **ID**, prioritas MoSCoW/P0-P3, acceptance criteria terukur, dependensi, dan status implementasi.
10. PRD harus lengkap namun tetap terbaca; spesifikasi sangat detail boleh dipecah menjadi `/docs/*.md` dengan ringkasan dan tautan silang di PRD.

### Struktur isi PRD yang diharapkan (adaptasikan dengan section existing)

1. **Metadata, version history, change log, document ownership**.
2. **Executive summary, masalah pengguna Indonesia, vision & value proposition**.
3. **Scope, assumptions, constraints, stakeholders, MVP vs non-goals**.
4. **Personas, user roles, user journeys, use-case matrix dan use-case diagram**.
5. **Product goals, KPI, metrik dengan cara pengukuran dan baseline yang jujur**.
6. **Current-state audit summary**, tingkat kesiapan tiap modul, status mobile–backend.
7. **Functional Requirements** berdasarkan modul dan user stories, `FR-xxx`, Gherkin/Given-When-Then, prioritas, edge cases.
8. **Non-Functional Requirements**, `NFR-xxx`: performa, availability, keamanan, privasi, auditability, aksesibilitas, offline-first, resilience, maintainability, logging, recovery.
9. **Role/permission model** dan evaluasi admin/moderator serta policy akses data.
10. **Arsitektur aplikasi dan system context** (mobile, backend, MongoDB, local Room, website, API docs, pihak ketiga bila relevan).
11. **Component/container diagram**: modul Compose/ViewModel/Repository/Retrofit/Room, backend router/controller/service/persistence.
12. **Data Flow Diagram (DFD Level 0 dan Level 1)**: lookup, report, dispute, offline sync, directory.
13. **Flowcharts**: onboarding/permission, lookup, incoming-call screening+auto-block, report spam/scam, offline sync, emergency directory, admin moderation/dispute.
14. **Sequence diagrams**: mobile–backend–MongoDB untuk lookup, submit report, sync dan dispute/moderation.
15. **State diagrams**: report moderation, dispute, status proteksi/offline-sync; transisi, pemilik transisi, kondisi.
16. **Database architecture**: MongoDB collections (current vs proposed), schema fields, tipe, required/nullable, indeks, unique constraint, TTL/retention bila layak, contoh JSON sintetis; hubungan logis (ERD Mermaid) dan strategi Room offline cache/migrasi.
17. **API contract**: endpoint, auth, query/path/body, request/response, status/error, pagination, versioning, rate limit, OpenAPI mapping, backward compatibility, idempotency.
18. **Scoring/reputation and abuse prevention**: kategori, perhitungan dan ambang, agregasi laporan unik, confidence, false-positive remediation, unknown ≠ safe.
19. **UI/UX specification**: screen inventory, navigasi, empty/loading/error/offline, permission denied, aksesibilitas, status real vs demo, desain konsisten.
20. **Security/privacy & threat model**: asset, attacker, threat, mitigation, ownership, remaining risk; penghapusan data, consent, privacy by design.
21. **Test & acceptance plan**: unit, contract, integration, E2E, offline, performance, permission, security, observability, traceability `FR ↔ endpoint ↔ test`.
22. **Deployment, environments, CI/CD, monitoring, incident/backup, release readiness**.
23. **Prioritised roadmap** untuk 2 orang, milestone, dependensi, estimasi effort, pembagian kerja masuk akal, Definition of Done.
24. **Open issues, decisions needed, risks, assumptions, references**, tautan ke dokumen rinci.

Jika isi PRD sudah memiliki topik tertentu, **revisi/perkaya bagian tersebut** daripada menduplikasi section baru.

### Diagram minimum (semuanya wajib benar-benar dihasilkan, bukan hanya disarankan)
- Mermaid **flowchart**: overview end-to-end CallTag.
- Mermaid **flowchart**: alur incoming call → cache → API → warning/auto-block.
- Mermaid **flowchart**: laporan spam/scam + status online/offline.
- Mermaid **flowchart**: moderator memproses laporan dan dispute.
- Mermaid **sequenceDiagram**: lookup nomor dan MongoDB.
- Mermaid **sequenceDiagram**: submit report dan sync offline.
- Mermaid **erDiagram**: model logis MongoDB, plus relasi Room bila relevan.
- Mermaid **stateDiagram-v2**: lifecycle laporan/dispute.
- Mermaid **flowchart**: DFD level 0/1 dan deployment/components jika sintaks yang lebih khusus tidak didukung.

**Syarat diagram:** label jelas, jalur error/offline ada, backend/database digambar sesuai temuan atau diberi label `PROPOSED`, nama entitas konsisten dengan koleksi aktual, sintaks valid untuk GitHub Mermaid, dan ada paragraf penjelasan setelah tiap diagram.

---

## 7. TAHAP E — FILE OUTPUT `.md` YANG WAJIB DIHASILKAN

Simpan hasil audit dan rancangan dalam **Markdown**. Lokasi di bawah adalah struktur usulan di repository mobile `CallTag/`, karena PRD utama berada di sana. Jika struktur docs sudah ada, integrasikan tanpa merusak file lama.

| Path | Isi minimum |
|---|---|
| `PRD.md` | **UPDATE IN PLACE**, bukan diganti total; versi terbaru dan diagram inti |
| `docs/AUDIT_REPORT.md` | executive verdict, scope, branch/commit, critical findings, evidence, limitation |
| `docs/INTEGRATION_MATRIX.md` | endpoint-by-endpoint, DTO, controller, DB, status verifikasi, langkah fix |
| `docs/FEATURE_GAP_ANALYSIS.md` | status seluruh fitur, admin/moderator, UX, safety, prioritas |
| `docs/SYSTEM_ARCHITECTURE.md` | C4-style context/container/component, DFD, dependencies, deployment Mermaid |
| `docs/DATABASE_DESIGN.md` | MongoDB schema, indeks, Room schema, ERD, lifecycle data, contoh dokumen |
| `docs/FLOWS_AND_DIAGRAMS.md` | flowchart, use-case, sequence, state, offline/error journeys |
| `docs/API_CONTRACT_AUDIT.md` | mismatch Retrofit–OpenAPI–backend; proposal kontrak final |
| `docs/QA_TEST_PLAN.md` | test matrix, scripts/commands aman, hasil PASS/FAIL/BLOCKED, acceptance traceability |
| `docs/IMPLEMENTATION_ROADMAP.md` | backlog P0-P3, dependensi, milestone, urutan pekerjaan tim 2 orang |
| `docs/ARCHITECTURE_DECISIONS.md` | ADR singkat: pilihan desain, alternatif, konsekuensi, keputusan tertunda |

**Pengaturan output:**
- Markdown rapi dengan heading dan tabel yang tidak terlalu lebar.
- Diagram Mermaid tertanam di `.md`; tautan internal relatif antarfile harus valid.
- Tidak perlu membuat `.docx`, `.pdf`, atau gambar diagram kecuali diminta terpisah.
- Jika isi redundan, cukup satu sumber detail dan sisanya tautkan, tetapi PRD tetap self-contained sebagai gambaran produk.
- Tambahkan `docs/README.md` sebagai indeks bila membantu navigasi.
- File OpenAPI yang ada (`openapi.json`/`.yaml`) tetap pada format mesin yang sesuai; audit/penjelasannya harus `.md`. Jangan membuat spesifikasi JSON yang tidak sinkron hanya demi menghasilkan lebih banyak file.

---

## 8. TAHAP F — PRIORITAS, REMEDIASI, DAN RENCANA IMPLEMENTASI

Urutkan backlog secara pragmatis:
- **P0 — Blocker:** aplikasi tidak dapat benar-benar berkomunikasi dengan backend; masalah auth kritis; data report tidak tersimpan tetapi UI menyatakan sukses; false-safe/auto-block berbahaya; risiko keamanan berat.
- **P1 — MVP:** endpoint dan DB berjalan, kontrak sinkron, retry/error jelas, moderation minimum, direktori darurat valid, test inti, release configuration.
- **P2 — Enhancement:** dashboard admin lebih lengkap, analytics operasional, dispute UX lanjut, sync lebih efisien, observability tambahan.
- **P3 — Future:** fitur lanjutan yang tidak krusial bagi kelulusan MVP, dengan alasan penundaan.

Untuk setiap backlog item tulis: `ID | Deskripsi | Evidence | Target repo | Deliverable | Prasyarat | Priority | Effort (S/M/L atau person-day bila berdasar) | Acceptance Criteria | Test method | Owner role`.

Usulkan **rencana bertahap dua pengembang** (misalnya backend/API/data dan Android/QA/docs, dengan review silang). Jangan mengasumsikan tenggat akademik tertentu jika belum diberikan; gunakan milestone relatif.

Jika audit menunjukkan backend implementasi hanya ada di feature branch, buat rekomendasi alur merge yang aman: review → fix → tests → PR → merge ke `main` → staging → E2E test. **Jangan merge otomatis.**

---

## 9. QUALITY GATES DAN DEFINITION OF DONE

Tandai dokumentasi selesai hanya bila:
1. Setiap klaim integrasi punya bukti atau status BLOCKED.
2. Semua repository dan branch yang relevan telah dipetakan dengan commit baseline.
3. Semua endpoint mobile dan backend diklasifikasi dan dibandingkan dengan OpenAPI.
4. Fitur online vs offline/mocked **tidak tertukar**.
5. Risiko Critical/High memiliki langkah penanganan yang dapat ditindaklanjuti.
6. PRD asli tetap menjadi basis dan memiliki pembaruan version/changelog.
7. Diagram inti sesuai source atau diberi label PROPOSED, dengan validasi sintaks Mermaid.
8. Data model MongoDB dan Room jelas, termasuk gap serta strategi migrasi.
9. Ada keputusan yang beralasan soal admin/moderator dan ruang lingkup MVP.
10. Backlog dapat dikerjakan oleh tim 2 orang dengan urutan dependensi yang tepat.
11. Semua dokumen Markdown dapat dirender GitHub dan tautan file tidak rusak.
12. Tidak ada secret atau data pribadi sungguhan di dokumen, log, contoh, atau commit.

---

## 10. URUTAN EKSEKUSI DAN FORMAT RESPONS TERAKHIR

**Kerjakan secara langsung, bertahap, dan profesional:**

**Fase 1 — Discovery:** baca keempat repo, semua branch relevan, baseline PRD/DESIGN/AGENTS, daftar modul, kontrak, model dan test.

**Fase 2 — Audit statis:** buat tabel implementasi, cross-repo integration matrix, feature gaps, risiko keamanan, dan daftar pertanyaan terbuka yang tidak dapat diselesaikan dari kode.

**Fase 3 — Verifikasi runtime bila lingkungan tersedia:** Go tests, MongoDB test, API tests, Android build/emulator/device, integration tests, offline tests; simpan bukti dan catat yang blocked.

**Fase 4 — Dokumentasi:** perbarui `PRD.md` yang sudah ada, lalu hasilkan `/docs/*.md` di atas dengan diagram Mermaid dan changelog. Pastikan seluruh diagram dan link dapat dirender.

**Fase 5 — Review dan delivery:** validasi inkonsistensi antar dokumen dan kode, susun backlog dan rekomendasi PR terpisah. Jika punya akses tulis, commit perubahan dokumentasi ke **branch baru**, lalu siapkan **draft pull request** (jangan merge tanpa persetujuan). Jangan mencampurkan refactor besar dengan perubahan dokumen.

**Respons terakhir WAJIB berisi dalam urutan berikut:**

1. **Executive verdict singkat:** `Apakah mobile dan backend sudah terhubung?` Jawab **YES_VERIFIED / PARTIALLY_VERIFIED / NOT_CONNECTED / UNVERIFIED**, beserta argumen berbasis bukti.
2. **Baseline audit:** repository, branch, commit yang diaudit, batasan akses.
3. **Top 10 temuan terpenting:** severity, lokasi kode, dampak.
4. **Integration readiness scorecard:** jangan mengarang persentase; gunakan status dan kriteria yang transparan.
5. **Keputusan fitur admin/moderator:** dibutuhkan atau tidak, minimum MVP vs pengembangan lanjut.
6. **Daftar file `.md` yang dibuat/diperbarui:** path dan ringkasan perubahan; link GitHub/PR bila benar-benar tersedia.
7. **Top 10 action items dan roadmap:** urutan P0→P3, first steps yang dapat dieksekusi.
8. **Ringkasan hasil uji:** PASS/FAIL/BLOCKED beserta command dan environment, bila benar-benar dijalankan.
9. **Open issues:** hal yang belum bisa diverifikasi dan informasi minimum yang diperlukan.

**Mulai dari membaca repository dan mengidentifikasi kondisi nyata; jangan langsung mengarang arsitektur atau menulis ulang PRD sebelum audit.**
