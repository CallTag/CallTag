# Keputusan arsitektur CallTag

Baseline audit 10 November 2026: Android `42e9ceb`, backend `8676314`. **Aktual** berarti terbukti dari source/bukti uji; **usulan** berarti arah perbaikan yang belum diimplementasikan atau disetujui sebagai release. Dokumen ini tidak mengubah aplikasi. Rincian storage ada di [DATABASE_DESIGN.md](DATABASE_DESIGN.md).

## ADR-001 — Sebut implementasi backend sesuai dependency aktual

**Status: deskripsi aktual; keputusan framework lanjutan terbuka.** PRD/README menyebut gocroot, tetapi [main.go](https://github.com/CallTag/backend/blob/8676314/main.go#L15) memakai `net/http.NewServeMux`, [router](https://github.com/CallTag/backend/blob/8676314/route/route.go#L23) switch path manual dan [go.mod](https://github.com/CallTag/backend/blob/8676314/go.mod) hanya memiliki dependency langsung Mongo driver. Tidak ada import/dependency gocroot. Karena itu arsitektur implementasi sekarang adalah REST API Go `net/http` dengan pola respons yang disebut bergaya gocroot dalam komentar.

Konsekuensi: jangan mengklaim middleware auth/rate limiter gocroot tersedia. Migrasi framework tidak diperlukan untuk mendokumentasikan fakta; jika kelak dipilih, buat keputusan terpisah berdasarkan kebutuhan. Middleware autentikasi, quota, body limit, timeout, error mapping dan request ID tetap harus dibangun/dikonfigurasi.

## ADR-002 — Room untuk screening cepat, Mongo untuk persistence daring

**Status: aktual, dengan keterbatasan.** [Repository](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L68) mencari Room lebih dulu untuk panggilan masuk, lalu API bertimeout 800 ms ketika cache miss. Cache hit tidak direvalidasi saat itu. Room menyimpan tiga tabel; DataStore menyimpan preferensi/kredensial. Lookup manual mencoba API lalu fallback lokal/seed. Laporan dan sengketa belum memiliki outbox lokal persisten.

Konsekuensi: panggilan bisa disaring tanpa jaringan untuk item yang sudah tersimpan, tetapi cakupan/freshness cache terbatas. Ini bukan bukti seluruh nomor diketahui. **Usulan:** source/freshness/expiry per item, freshness policy yang sesuai jalur telepon, dan checkpoint sync independen. Pertahankan batas waktu ketat di jalur panggilan; pekerjaan sinkronisasi/moderasi berjalan terpisah.

## ADR-003 — Offline harus menjelaskan apa yang benar-benar diketahui/disimpan

**Status: mitigasi UI sudah ada; koreksi model/persistence masih usulan.** [Fallback dispute Android](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L326) membentuk ID/status UNDER_REVIEW meski network gagal, tanpa antrean durable. Backend [SubmitDispute](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L126) juga mengklaim 201 berhasil dicatat tanpa persistence. Seed komentar/breakdown dapat dipakai pada hasil LIVE_API yang daftar laporannya kosong. Model repository masih dapat menyajikan contoh sebagai bukti komunitas dan membentuk respons domain seolah pengajuan tercatat, walaupun UI kini memitigasi sebagian dampaknya.

Mitigasi aktual di [ReportAndHubScreen](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L486): UI mendeteksi `reportId` berisi `_local_`, menampilkan “#Tag tersimpan di perangkat” dan menjelaskan bahwa layanan belum mengonfirmasi penerimaan. Untuk sengketa, UI mendeteksi teks `submittedAt` berawalan “Tersimpan di Antrean”, lalu menampilkan “Pengajuan belum terkirim” dan menyatakan belum masuk antrean peninjauan (baris 847–877). Ini mengurangi klaim sukses pada layar, tetapi tidak membuat outbox durable; teks laporan “tersimpan di perangkat” juga belum membuktikan pending request tersimpan. Deteksi berbasis string rapuh, dan respons sukses backend tetap tidak membuktikan persistence sengketa.

Keputusan usulan: server-acknowledged baru berarti “terkirim”; outbox lokal durable berarti “tersimpan di perangkat, belum terkirim”; bila keduanya tidak ada tampilkan gagal/belum terkirim dan sediakan retry. Data contoh ditandai demonstrasi, tidak dicampur dengan laporan nyata. Tidak mengarang jumlah reporter; sync mapper sekarang menetapkan `uniqueReporters=5` (repository baris 358), yang harus diganti nilai nyata atau state tidak tersedia. Client fallback tidak dapat membuktikan layanan pemerintah menerima laporan; CallTag hanya membuka panduan/link eksternal.

## ADR-004 — Pisahkan ketidaktahuan dari empat level reputasi

**Status: usulan; enum tetap empat level.** Backend lookup nomor tidak ditemukan membalas `found=false`, CLEAN dan score 0 ([lookup.go](https://github.com/CallTag/backend/blob/8676314/controller/lookup.go#L52)). Timeout/offline bukan bukti nomor aman, dan belum ada laporan bukan verifikasi keamanan. Jangan menambah string UNKNOWN ke `reputation_level` karena kontrak client/server menetapkan CLEAN/SUSPICIOUS/SPAM/DANGER_SCAM.

Keputusan usulan: model hasil terpisah, misalnya availability `AVAILABLE/NOT_FOUND/UNAVAILABLE` serta source/fetchedAt/isStale; reputation ada hanya saat data diketahui. UI menampilkan “belum ada data” atau “belum dapat diperiksa” sesuai sebab. Auto-block hanya memakai bukti level yang memenuhi kebijakan, bukan asumsi dari error. Whitelist terverifikasi juga harus memiliki provenance; short code bukan semua string <=6 digit.

## ADR-005 — Identitas pelapor dan pembatasan harus berasal dari server

**Status: usulan hardening; auth saat ini belum memadai.** [AuthLogin](https://github.com/CallTag/backend/blob/8676314/controller/auth.go#L32) menerima nomor tanpa bukti kepemilikan, memilih/memperbarui user dan menerbitkan JWT. [ExtractClaimsFromRequest](https://github.com/CallTag/backend/blob/8676314/helper/auth.go#L113) mengubah token invalid/hilang menjadi guest, dengan principal turunan `X-Device-Id` bebas atau RemoteAddr. Submit mengabaikan boolean auth dan semua report langsung VALIDATED. Tidak ada enforcement quota PRD 10/24 jam dan 3/5 menit; user counter hanya field.

Keputusan usulan: pemilik nomor/perangkat dibuktikan sebelum trust penuh, invalid bearer ditolak, status user dicek server-side dan credential dicabut saat perlu. Guest policy harus eksplisit; guest tidak otomatis setara kontribusi tervalidasi. JWT/hash key wajib dikonfigurasi di production dan dipisahkan; source memiliki fallback literal yang tidak boleh dipakai produksi. Jangan masukkan nilainya ke dokumen/log. Quota atomic per principal serta jaringan/perangkat dan anti-Sybil diterapkan bersama; unique index tidak cukup untuk mencegah identitas massal.

## ADR-006 — ID laporan adalah identitas persisten; idempotency adalah kontrak terpisah

**Status: usulan, belum diimplementasikan.** Saat ini config membuat `_id` baru dalam parameter by value; controller membalas `phone + "_" + uid` ([report.go](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L89)). ID balasan berulang untuk dua laporan dan bukan `_id` yang dapat ditelusuri. Index bernama dedup tidak unique; pengujian Mongo disposable membuktikan dua laporan persisten dari principal sama.

Keputusan usulan: response membawa ID canonical laporan tersimpan, opaque dan unik; idempotency key terikat principal + request intent, dipersist dengan unique constraint dan hasil respons yang dapat diulang. Retry timeout/500 tidak otomatis membuat kontribusi baru. Putuskan satu kontribusi aktif per pelapor/nomor atau window kejadian; koreksi/history terpisah. Deduplikasi harus sama untuk skor, kategori dan label supaya satu akun tidak dapat memompa vote tag lewat repeat submit.

Accepted report, quota, aggregate dan journal sync perlu transaction dengan retry atau workflow event idempoten yang dapat direkonsiliasi. Mongo saat ini insert→read-all→update tanpa transaction/CAS; writer lama dapat menimpa agregat baru, dan insert bisa berhasil sebelum 500. Error selain ErrNoDocuments tidak boleh dianggap “belum ada”, terutama karena flags whitelist/blacklist dapat hilang.

## ADR-007 — Delta sync memakai cursor urutan perubahan, bukan urutan risiko

**Status: usulan penggantian protokol, belum diimplementasikan.** [GetTopScammersForSync](https://github.com/CallTag/backend/blob/8676314/config/db.go#L521) memfilter `version>since`, sort risk desc, limit, lalu mengambil max version hasil. Dataset sintetis A(v10,risk90), B(v11,risk40), C(v12,risk80) dengan limit 2 mengirim A/C dan cursor12; B terlewat permanen. Mongo memakai Unix seconds sehingga dua perubahan satu detik bertabrakan; memory version reset setelah restart. CLEAN/deletion tidak dikirim sehingga cache tidak dapat mencabut tuduhan lama.

Keputusan usulan: journal sequence monotonik global durable, generation, cursor opaque/sequence ascending, halaman terikat urutan dengan `has_more`, event UPSERT/CLEAN/DELETE dan mekanisme full resync ketika cursor kedaluwarsa. Android menerapkan page+checkpoint secara atomic, terpisah dari MAX sync_version item. Lookup satu nomor tidak boleh memajukan checkpoint seluruh dataset. Mengganti timestamp ke nanodetik saja tidak menyelesaikan ordering, pagination, restart dan tombstone.

Konsekuensi: kontrak API/DTO/Room perlu migrasi kompatibel dan pengujian multi-page, tie, concurrent update, delete, restart dan resume. Jangan mengiklankan delta lengkap sampai seluruh rantai teruji. Skor decay/keputusan review juga harus menghasilkan event agar cache mengikuti perubahan.

## ADR-008 — Mongo wajib untuk produksi; memory hanya mode pengembangan eksplisit

**Status: perilaku fallback aktual, fail-closed masih usulan.** [InitDatabase](https://github.com/CallTag/backend/blob/8676314/config/db.go#L35) seed memory lebih dulu; URI kosong/connect/ping gagal tetap melanjutkan server dengan memory. [HealthCheck](https://github.com/CallTag/backend/blob/8676314/controller/sync.go#L83) selalu 200 dan mengandalkan flag startup, bukan ping runtime. Data write fallback hilang ketika proses mati. Runtime Mongo disconnect sesudah startup tetap dapat disebut connected.

Keputusan usulan: konfigurasi storage mode eksplisit; deployment produksi gagal startup atau readiness 503 ketika Mongo yang diwajibkan tidak tersedia, dan tidak mengakui write sebagai durable. Liveness hanya proses hidup, readiness mengecek dependency dalam waktu bounded. Index/seed error invariant harus terlapor/fatal, bukan diabaikan. Tutup client gagal, graceful shutdown, timeout http.Server, body limit dan error publik generik dengan request ID. Existing request context timeout 5 detik berguna, tetapi error cursor yang ditelan sekarang masih dapat menampilkan partial/empty data sebagai sukses.

## ADR-009 — Moderasi minimal untuk dua operator (usulan)

**Status: usulan, tidak ada admin/review API aktual.** Untuk dua pengelola awal, cukup antrean laporan/sengketa, detail bukti, keputusan approve/reject/resolve, override whitelist/blacklist beralasan dan audit log append-only. Hindari membangun sistem organisasi kompleks sebelum dibutuhkan. Dua operator masing-masing memiliki identitas terverifikasi dan izin server-side; jangan memakai akun bersama atau guest. Keputusan berisiko (override resmi/perubahan akses) memerlukan operator kedua, sementara review rutin dapat dilakukan satu operator sesuai policy yang dicatat.

Data minimal: collection disputes persisten dan moderation_audit_logs seperti [rancangan database](DATABASE_DESIGN.md#usulan-persistence-sengketa-dan-moderasi-minimal), status allowlist dan metadata reviewer/waktu/reason. API publik hanya memberi DTO teredaksi, bukan stable reporter uid, trust internal, kontak sengketa atau komentar belum termoderasi. Tidak perlu collection admin terpisah bila role/allowlist users memadai; JWT role lama tidak menggantikan pemeriksaan status aktual.

Konsekuensi: status UNDER_REVIEW hanya diterbitkan setelah write acknowledged; keputusan mengubah status kontribusi, menghitung ulang agregat dan menerbitkan sync event. Bukti pengajuan, audit dan retensi diperlukan agar pemilik nomor dapat menelusuri hasil. Workflow ini belum ada; jangan menyebut sengketa sekarang siap ditinjau.

## ADR-010 — Formula, seed dan provenance harus dapat diaudit

**Status: implementasi formula aktual memiliki deviasi; invariant target usulan.** [Scoring](https://github.com/CallTag/backend/blob/8676314/helper/scoring.go#L170) memakai base 8, bobot kategori dan decay 90 hari. Ada fallback SUSPICIOUS untuk satu reporter >=15 yang melonggarkan gate dua reporter; uji Mongo membuktikan satu reporter APK score16 menjadi SUSPICIOUS. Hanya status REJECTED diabaikan; trust <=0 malah diubah ke 1; trust tidak dibatasi atas; kategori tie bergantung iterasi map; kontribusi pertama reporter bergantung urutan history. AppliedWeight snapshot tidak dipakai dan decay hanya on-write.

Keputusan usulan: tetapkan acceptance matrix normatif bersama PRD, status VALIDATED saja, trust bounded, pilihan kontribusi/tie deterministik, formula_version dan snapshot policy eksplisit. Scheduled decay menerbitkan perubahan sync. Whitelist mengungguli blacklist saat menentukan level, tetapi policy menerima laporan atas nomor terlindungi harus dinyatakan dan diterapkan. Unique count, total submissions dan accepted contributions dibedakan.

Seed demo backend/Android harus dipisah dari data otoritatif dan ditandai sumbernya. Nomor yang tampak nyata dengan label tuduhan/blacklist tidak boleh dianggap terverifikasi hanya karena masuk fixture. Loader mengisi verified_at/updated_at dengan waktu boot, yang bukan verifikasi lapangan. Usulkan actual verified_at, source dan reviewer; verifikasi kanal resmi melalui sumber berwenang sebelum release. Dokumen ini tidak menyatakan kontak seed tertentu salah atau telah diverifikasi eksternal.

## ADR-011 — Migrasi menjaga riwayat; verifikasi harus menguji invariant

**Status: usulan.** Room v1 sekarang `exportSchema=false` dan destructive fallback: kenaikan versi tanpa migration dapat menghapus cache, direktori dan log. Re-seed mengembalikan fixture, bukan riwayat nyata. Aktifkan schema export, migration eksplisit dan uji jalur v1 sebelum menambah checkpoint/outbox/provenance. Mongo perlu backup + restore rehearsal, normalisasi/cleanup duplikat dan rollout validator/index bertahap; lihat [rencana migrasi](DATABASE_DESIGN.md#konsistensi-sync-retensi-dan-migrasi-yang-diusulkan).

Backend tests/vet lulus pada audit, tetapi suite sekarang satu happy-path endpoint test yang dapat memakai URI/DB environment proses. Uji berikutnya harus memakai database disposable terisolasi dan menegaskan persistence ID/sengketa, invalid bearer, Sybil/quota/dedup, concurrency/retry, index failure, score/decay, sync multi-page/same-second/CLEAN, restart, offline truthfulness dan redaksi publik. Tidak perlu mengulang build UI untuk edit dokumen ini; dokumen tidak membuktikan perubahan runtime telah dilakukan.
