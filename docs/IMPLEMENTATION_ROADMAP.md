# Roadmap implementasi — tim dua pengembang

Seluruh item di bawah **PROPOSED**, belum menjadi perubahan source dalam PR audit. [AUDIT_REPORT](AUDIT_REPORT.md) menetapkan bukti; [PRD](../PRD.md) menetapkan kebutuhan. Prioritas P0 berarti blocker keselamatan/integritas sebelum pilot publik, P1 minimum MVP, P2 peningkatan, P3 ditunda. Must tidak berarti sudah ada.

## Kapasitas dan cara kerja

- **Pengembang A:** backend/API/Mongo/security/data; **B:** Android/UI/cache/QA/docs. Review silang wajib untuk auth, auto-block dan privacy.
- S ≈ 0,5–1 person-day, M ≈ 2–4, L ≈ 5–8 adalah estimasi awal engineering, bukan jadwal akademik atau janji waktu; validasi lagi sesudah spike dan acceptance test. Effort tidak termasuk menunggu sumber data resmi/perangkat/persetujuan pengguna.
- Hanya satu alur vertikal aktif per pengembang. Bantu kontrak/test ketika perubahan A memblokir B; jangan membangun admin lengkap sebelum write/auth aman.
- Pertahankan `net/http`, Compose, Retrofit, Room dan Mongo driver; tidak perlu microservice/Kubernetes atau DI framework baru untuk lulus MVP.

## Backlog terurut dan deliverable

Evidence Axx mengikuti ID [temuan audit](AUDIT_REPORT.md#sepuluh-temuan-paling-penting); lokasi source tetap tersedia di sana. Kolom acceptance adalah target untuk patch terpisah.

| ID / prioritas / owner | Masalah, evidence, repo dan deliverable | Prasyarat / effort | Acceptance dan cara uji |
|---|---|---|---|
| B01 P0 A | A01/A02, `backend/helper/auth.go:27,113`: secret startup strict, invalid token → 401, identitas perangkat server-issued, batas trust | Environment test; L | Secret kosong pada production menolak startup; invalid/expired JWT → 401 tanpa write; perangkat tidak dapat mengaku uid lain lewat header. Unit/auth integration. |
| B02 P0 B | A04/A05, repository:244 dan PhoneNormalizer:56: status typed LOCAL_PENDING vs ACKED; hentikan kontribusi lokal terhadap auto-block otoritatif | B01 kontrak; L | Submit offline berulang tidak menambah reporter/skor otoritatif dan tidak memblokir nomor; UI jelas belum terkirim; test Room + reconnect. |
| B03 P0 A | A03, controller/report.go:98: model/collection dispute, ID unik, simpan reason/contact privat sebelum 201 | B01; M | 201→findById→restart→readback tetap ada; error Mongo tidak mengklaim UNDER_REVIEW. API→Mongo integration. |
| B04 P0 B | A05/A07, service:71, repository:77, overlay:75: opt-in DANGER-only, whitelist precedence, UNKNOWN/error, cache TTL | B02, schema source/provenance; L | UNKNOWN tidak pernah AMAN/verified; SPAM 60 dan kategori 50 allow+warn; stale/seed tidak auto-block; whitelist allow. Pure policy tests dan emulator/fisik. |
| B05 P0 A | A02/A08, db.go:148,344: ID laporan persisten, dedup window yang disepakati, idempotency, quota dan write-consistency | B01; L | Request identik/retry tidak menggandakan kontribusi; 11 request per principal/24 jam menghasilkan 429 setelah kuota 10; 3/5 menit ditegakkan; concurrent writer invariant terjaga. Unit+Mongo concurrent tests. |
| B06 P0 A+B | A06, db.go:521 dan repository:338: cursor server persisten terpisah, sort stabil, has_more, CLEAN/delete, generation | B05, desain migrasi; L per repo | Pada dataset >250 item dengan ties dan gap, termasuk setelah restart dan revocation, perubahan diterapkan tepat sekali secara efektif; cursor hanya maju setelah transaksi Room selesai. Contract test + uji 2 halaman + restart. |
| B07 P0 A | A09, db.go:35/sync.go:83: production persistence strict, readiness ping, error tidak bocor, server/body timeouts | Konfigurasi staging; M | Saat Mongo offline, readiness 503; kegagalan write dilaporkan sesuai kondisi; tanpa fallback memory produksi; tidak mengeluarkan URI/error internal; chaos lokal. |
| B08 P1 A+B | Matriks kontrak: envelope/null/201/ID/errors; sinkronkan OpenAPI, DTO dan fixture | B01–B07 kontrak stabil; M | Seluruh 11 endpoint tervalidasi schema + negative cases; Android parse actual JSON; APIdocs live/mock diberi label. CI contract test. |
| B09 P1 B | Auth method tanpa caller, static deviceID: onboarding principal, token expiry/logout/revocation dan penyimpanan aman | B01/B08; L | Instalasi baru mendapatkan identitas unik yang dibuktikan; API terproteksi memakai bearer; expired tidak menjadi guest diam-diam; no token in logs. Emulator+API. |
| B10 P1 B | Room v1 destructive; tanpa outbox/scheduler: migrasi additive, pending report durable, unique worker/retry/backoff | B02/B05/B06; L | Restart process/upgrade tidak kehilangan pending; reconnect tidak double-post; denied auth tetap error/draft; schema export dan migration test. |
| B11 P1 A+B | A10: data demo terpisah, direktori pilot terkurasi bersumber, verified_at nyata, per-region | Bukti sumber dan review manusia; M + kurasi | Setiap kontak pilot memiliki source URL, verification date/reviewer; seed tuduhan tidak masuk production; instalasi baru offline berisi kontak pilot dan tidak mengklaim cakupan seluruh Indonesia. Data QA + UI offline. |
| B12 P1 A+B | Moderasi minimal: endpoint internal RBAC + panel satu halaman, disputed report review, whitelist expiry, CRUD directory, audit event | B01/B03/B05/B11; L | User → 403; moderator hanya dapat memperbarui status/data yang diizinkan; audit keputusan immutable; false positive yang disetujui mengirim CLEAN/tombstone; kontak privat tidak publik. Role + integration tests. |
| B13 P1 B | Service deadline, permission/overlay/notif settings, simulation flags, PII log | B04/B09; M | Setiap callback respond <5 detik atau fail-open; toggle notifier dipatuhi; permission denied tidak crash; simulation tak dihitung panggilan riil; device matrix. |
| B14 P1 A+B | Release/staging, CI, SDK/signing/HTTPS, backup/restore, metrik jujur | B07–B13; L | APK staging pakai URL staging, release tidak memakai debug signing, SDK Play sesuai kanal, uji restore pada DB test sukses, E2E yang dapat direproduksi dan gate terlampir. |
| B15 P2 A | Dashboard backlog/latency, advanced anomaly, decay scheduler, efficient change journal | B12–B14; L | Latency P95 terukur dengan dataset dan perangkat jelas; decay menerbitkan revocation; no PII metric; trend bukan data demo. |
| B16 P3 A+B | GPS nearest, OTP bisnis terverifikasi secara massal, notifikasi pesan/APK detection, multi-region dan analytics skala besar | Privacy/data/capacity review; L | Spec terpisah dan keputusan produk baru; tidak menjadi syarat lulus MVP telepon/direktori. |

Kuota 10/24 jam dan 3/5 menit berasal dari PRD asal, tetapi jendela dedup 30 hari harus didefinisikan konsisten (rolling window vs bucket kalender, update/correction). Pilihan minimal yang disarankan: satu kontribusi aktif per principal/nomor; update koreksi diaudit. Bila mempertahankan rolling 30 hari, gunakan transaksi/conditional update dan test boundary; jangan membuat unique bucket yang mengizinkan dua vote di batas bucket.

## Milestone relatif

| Milestone | A | B | Exit gate |
|---|---|---|---|
| M0 audit/baseline | Review kontrak dan DB test | Review UX/status dan data fixture | Audit PRD ditinjau; keputusan principal/dedup/kanal distribusi dicatat. |
| M1 koneksi aman | B01/B03/B05/B07 | B02/B04, test kontrak | Auth-invalid ditolak; report/dispute durable; status unknown ditangani secara konservatif, tanpa auto-block palsu. |
| M2 sinkronisasi lengkap | Server journal/cursor B06 | Room cursor/outbox B06/B10 | Offline/reconnect/restart/revocation lulus. |
| M3 MVP terkurasi | Contract + admin/data B08/B11/B12 | Onboarding/permission/UI B09/B13 | Data wilayah pilot terverifikasi; moderator koreksi false positive; UI real vs demo jujur. |
| M4 staging/demo akademik | Readiness/backup/CI | Release/device/QA | Android→API→Mongo→UI/Room happy paths + failure paths tersimpan bukti; sideload vs Play eksplisit. |
| M5 pasca-MVP | B15 | Perbaikan UX dari pilot | P2/P3 hanya sesudah Must terpenuhi dan data aman. |

Tidak menetapkan tanggal kelulusan karena deadline akademik belum diberikan. Estimasi paralel bukan penjumlahan effort dibagi dua: dependensi, review, sumber direktori dan perangkat dapat mendominasi.

## Definition of Done patch dan release

Patch: issue dan FR/NFR terhubung, source review, test positive/negative relevan, migration/rollback plan, OpenAPI+DTO+docs sinkron, no secrets/PII log, CI lulus. UI success hanya untuk persistensi yang diakui dan retry tidak membuat duplicate.

Release: seluruh P0 ditutup dan Must P1 diterima, DB staging durable/readiness/restore diuji, dataset demo terisolasi, sumber direktori ditinjau, screening manual/perangkat sesuai kemampuan kanal distribusi, consent/privacy dan deletion tersedia, signing/HTTPS/BASE_API_URL diaudit. Mock demo tidak menggantikan bukti E2E.

## Tindakan pertama yang dapat dieksekusi

1. Review branch backend 8676314 dan apidocs 1ae8834 dengan matriks; main saat ini belum memiliki implementasinya.
2. Tetapkan principal anonymous-server-issued atau nomor terverifikasi; jangan menyebut OTP ada.
3. Tambah test invalid JWT → 401, dispute 201 → record, dan duplicate/idempotency sebelum patch.
4. Pisahkan laporan lokal berstatus pending dan auto-block, lalu patch prototype policy konservatif.
5. Kunci kontrak ID persisten, status unknown, source/freshness dan halaman sync sebelum migrasi klien.
6. Selesaikan B01–B07 di PR terpisah, merge hanya setelah review manusia.
7. Deploy staging dengan DB test dan URL eksplisit untuk ulang E2E; jangan memakai domain di OpenAPI sebagai bukti server aktif.

Langkah ini adalah rekomendasi patch, bukan izin merge otomatis dari PR dokumentasi.
