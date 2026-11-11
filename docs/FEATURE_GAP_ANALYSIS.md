# Analisis kesenjangan fitur

Baseline audit 10 November 2026: Android `42e9ceb`, backend `8676314`, apidocs `1ae8834`, website `c3e071b`. Dokumen ini membedakan **AS-IS** yang ditemukan dari **PROPOSED** yang belum diimplementasikan. Bukti runtime terpusat di [AUDIT_REPORT](AUDIT_REPORT.md) dan [INTEGRATION_MATRIX](INTEGRATION_MATRIX.md); keberadaan kode bukan bukti E2E. Satu jalur lookup emulator API34 → API lokal → MongoDB → UI/Room telah dibuktikan dengan fixture sintetis non-seed; panggilan seluler nyata dan auto-block belum diuji.

## Arti status dan prioritas

| Status | Arti |
|---|---|
| VERIFIED_E2E | Hanya rantai dan lingkungan yang benar-benar diuji; harus disertai batas uji. |
| IMPLEMENTED_NOT_E2E_TESTED | Jalur implementasi ditemukan, integrasi lengkap belum dibuktikan. |
| PARTIAL | Sebagian jalur bekerja tetapi ada lapisan, kontrak, atau penanganan gagal yang belum lengkap. |
| MOCK_OR_LOCAL_ONLY | Simulasi, seed, penyimpanan lokal, atau respons sintetis; bukan bukti pengiriman server. |
| SPEC_ONLY | Ada di spesifikasi/rancangan tanpa implementasi yang ditemukan. |
| MISSING | Tidak ditemukan implementasi pada baseline yang diaudit. |

P0 menutup risiko kebenaran data, autentikasi, sengketa dan salah blokir sebelum klaim proteksi rilis. P1 melengkapi operasi MVP setelah P0. P2 adalah pasca-MVP. Prioritas adalah keputusan audit **PROPOSED**, bukan klaim perubahan source sudah dilakukan.

## AS-IS: inventaris per perjalanan pengguna

Path Android di tabel mengacu ke `app/src/main/java/id/calltag/app`. Tautan backend memakai commit tetap karena repository terpisah.

| ID / Journey / fitur | UI → ViewModel | Repository / API | Backend → DB | Status keseluruhan | Bukti / gap / prioritas |
|---|---|---|---|---|---|
| INV-01 — Aktivasi Caller ID | Home meminta runtime permission lalu role; role/permission disimpan sebagai state Compose | RolePermissionManager; belum ada onboarding identitas | Tidak memanggil auth dari UI; bukan write DB | PARTIAL | [HomeScreen:134](../app/src/main/java/id/calltag/app/ui/screens/HomeScreen.kt#L134), [role:24](../app/src/main/java/id/calltag/app/util/RolePermissionManager.kt#L24). Grant overlay helper belum digunakan; dukungan API26–28 tidak dibuktikan. P0 |
| INV-02 — Registrasi perangkat / JWT | Tidak ada pemanggil authenticateDevice dari ViewModel/UI | Metode auth dan penyimpanan token ada; report/dispute membaca Bearer bila token tersedia | Auth membuat/mencari user dan menerbitkan token; tanpa pembuktian kepemilikan nomor | PARTIAL | [repo:429](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L429), [backend auth:49](https://github.com/CallTag/backend/blob/8676314/controller/auth.go#L49). API→Mongo lokal diuji terpisah; onboarding mobile belum E2E. P0 |
| INV-03 — Cari nomor manual | Home → performPhoneLookup → Loading/Success | Room read, GET phone lalu reports sekuensial; fallback lokal | Lookup membaca PhoneTags/SpamReports Mongo atau fallback memory | PARTIAL | [VM:145](../app/src/main/java/id/calltag/app/ui/viewmodel/CallTagMainViewModel.kt#L145), [repo:106](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L106), [backend lookup](https://github.com/CallTag/backend/blob/8676314/controller/lookup.go#L16). Unknown/offline miss menjadi CLEAN; found=false dapat mempertahankan cache lama. P0 |
| INV-04 — Tampilkan status / bukti komunitas | Home menyebut belum ada laporan dan disclaimer, preview terpisah | Empty live reports/breakdown diisi seed; fallback source LOCAL_CACHE | Server bisa memberi found=false; data seed bukan laporan pengguna terverifikasi | PARTIAL | [Home:513](../app/src/main/java/id/calltag/app/ui/screens/HomeScreen.kt#L513), [repo:127](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L127), [seed:481](../app/src/main/java/id/calltag/app/data/local/OfflineSeedData.kt#L481). Preview masih NOMOR AMAN [overlay:75](../app/src/main/java/id/calltag/app/ui/components/CallerIdOverlayCard.kt#L75). P0 |
| INV-05 — Screening panggilan masuk | Callback service, bukan UI Compose | Cache-hit langsung; miss fast API timeout800ms | GET lookup; Room menyimpan hasil; tidak ada contact lookup aplikasi | IMPLEMENTED_NOT_E2E_TESTED | [service:34](../app/src/main/java/id/calltag/app/service/CallTagScreeningService.kt#L34), [repo:68](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L68). Belum membuktikan callback Telecom, kontak tersimpan, timing seluruh callback, atau semua OEM. P0 |
| INV-06 — Auto-block | Toggle default ON → DataStore; service membaca toggle | Policy DANGER selalu block, SPAM>=60, kategori berisiko>=50 | Whitelist/blacklist ada di DTO server tetapi hilang di entity mobile | PARTIAL | [policy:56](../app/src/main/java/id/calltag/app/util/PhoneNormalizer.kt#L56), [mapper:468](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L468). Policy lebih luas dari copy UI “Bahaya Penipuan”; freshness/whitelist perlu diselesaikan. P0 |
| INV-07 — Heads-up / overlay nyata | Toggle notifikasi tersimpan; helper grant overlay tidak dipanggil UI | Notifier selalu dipanggil pada non-CLEAN; tak membaca toggle | Lokal, tanpa DB server | PARTIAL | [notifier:55](../app/src/main/java/id/calltag/app/service/CallerIdNotifier.kt#L55), [VM:303](../app/src/main/java/id/calltag/app/ui/viewmodel/CallTagMainViewModel.kt#L303). Overlay perlu special permission; manifest bukan grant. P1 |
| INV-08 — Lapor online | Form kategori/tag/comment/callType → submitCommunityReport | POST report, cache respons; default deviceId konstan | Insert SpamReports + hitung PhoneTags; header invalid/tanpa auth tetap diterima | PARTIAL | [repo:188](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L188), [controller:63](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L63), [persist:344](https://github.com/CallTag/backend/blob/8676314/config/db.go#L344). Satu form emulator lokal guest → API → Mongo → UI/Room VERIFIED_E2E: tag AUDIT_MOBILE_E2E, satu report, CLEAN12.8. Auth mobile dan seluruh cabang report belum terbukti. P0 |
| INV-09 — Lapor offline / server menolak | Result tetap Success; UI membedakan tersimpan perangkat dan belum dikonfirmasi | Skor/pelapor lokal ditambah; raw komentar/callType tidak masuk outbox; DTO *_local_* sintetis | Tidak ada replay otomatis ke server | MOCK_OR_LOCAL_ONLY | [fallback:244](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L244), [UI:486](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L486). Jangan menyebut “laporan terantre dan akan terkirim”; perlu outbox/idempotensi. P0 |
| INV-10 — Koreksi / dispute | Form → submitNumberDispute; UI fallback menyebut belum terkirim | POST dispute atau synthetic UNDER_REVIEW tanpa persistence | Handler mengembalikan201 tanpa menulis koleksi dispute | PARTIAL | [repo:303](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L303), [UI:847](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L847), [handler:126](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L126). Runtime201 tidak membuktikan kasus tersimpan; review/status/audit trail belum ada. P0 |
| INV-11 — Offline bundle / refresh | Proteksi → runOfflineDeltaSync manual | MAX row sync_version sebagai cursor; satu batch250; latest_sync_version tidak digunakan | Filter non-CLEAN, sort risiko, limit; belum ada tombstone/demotion feed | PARTIAL | [repo:338](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L338), [DAO:22](../app/src/main/java/id/calltag/app/data/local/dao/PhoneTagDao.kt#L22), [server:521](https://github.com/CallTag/backend/blob/8676314/config/db.go#L521). Seed/local report mengotori cursor; stale block tak terselesaikan. P0 |
| INV-12 — Riwayat deteksi | Home mengamati logs; preview “Tolak & Blokir” memasukkan log | Room call_screening_logs | Tidak ada write server untuk history ini | MOCK_OR_LOCAL_ONLY | [seed log:330](../app/src/main/java/id/calltag/app/data/local/OfflineSeedData.kt#L330), [VM simulated:188](../app/src/main/java/id/calltag/app/ui/viewmodel/CallTagMainViewModel.kt#L188). Service nyata juga menulis log, tetapi seed/simulasi tidak dibedakan. P1 |
| INV-13 — Direktori darurat | Filter kota/jenis lokal; ACTION_DIAL / WhatsApp prefill | Room seed; directory sync dari tombol sync Proteksi | GET national/local → EmergencyDirectory | PARTIAL | [screen:80](../app/src/main/java/id/calltag/app/ui/screens/EmergencyScreen.kt#L80), [dial:524](../app/src/main/java/id/calltag/app/ui/screens/EmergencyScreen.kt#L524), [repo:407](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L407). Membuka dialer bukan sambungan; filter pilihan kota hardcoded; tak ada geolokasi otomatis. P1 |
| INV-14 — Kanal pengaduan resmi | Hub menyalin nomor dan membuka situs/dialer | GET official-channels, fallback seed | OfficialChannels | IMPLEMENTED_NOT_E2E_TESTED | [hub:528](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L528), [external:925](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L925). Tidak ada submit pemerintah otomatis, tiket lembaga atau sinkronisasi status perkara. P1 |
| INV-15 — Web publik / dokumentasi | Landing page dan Swagger UI, bukan panel moderator | Surface demo dokumentasi harus dibedakan dari request API nyata | Tidak ada route admin baseline | PARTIAL | [website](https://github.com/CallTag/calltag.github.io/blob/c3e071b/index.html), [mock interceptor](https://github.com/CallTag/apidocs/blob/1ae8834/app.js#L283), [router](https://github.com/CallTag/backend/blob/8676314/route/route.go#L23). Lihat audit kontrak untuk batas live/demo. P1 |
| INV-16 — Moderasi / administrasi | Tidak ada panel, login staf, review queue atau audit viewer | Tidak ada endpoint admin | Role field bukan RBAC operasional; tak ada dispute workflow persisten | MISSING | [route:23–69](https://github.com/CallTag/backend/blob/8676314/route/route.go#L23). Minimum panel+endpoint P1; persistensi dispute dan perbaikan false positive tetap P0. |

## AS-IS: risiko yang menghalangi klaim MVP selesai

| Severity / prioritas | Temuan dan akibat | Bukti kunci |
|---|---|---|
| High / P0 | Invalid JWT/tanpa auth menjadi guest, tidak ditolak; identitas mudah dimanipulasi. Mobile auth tak terhubung. | [ExtractClaimsFromRequest](https://github.com/CallTag/backend/blob/8676314/helper/auth.go#L113); report mengabaikan boolean [controller:63](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L63). Runtime lokal menerima201 pada kedua kasus. |
| High / P0 | Dispute201 tidak persisten; pemilik nomor tak memiliki jalan koreksi yang bisa ditelusuri. | [controller:98](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L98); tidak ada DB call sebelum WriteSuccess. |
| High / P0 | Cache tidak TTL dan demotion CLEAN tidak tersinkronisasi; nomor yang dibebaskan dapat tetap diblokir. | [cache-hit:77](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L77), [server sync:529](https://github.com/CallTag/backend/blob/8676314/config/db.go#L529). |
| High / P0 | Lokal, unavailable dan found=false tidak dibedakan dengan baik; preview menyatakan aman. | [synthetic CLEAN:156](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L156), [preview:78](../app/src/main/java/id/calltag/app/ui/components/CallerIdOverlayCard.kt#L78). Home sendiri sudah memasang disclaimer; masalah harus ditutup di semua permukaan. |
| High / P0 | Cursor berdasarkan MAX campuran seed/local/server + risk-sorted limited batch dapat melewatkan perubahan. | [local +1:261](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L261), [sync:339](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L339), [risk sort:535](https://github.com/CallTag/backend/blob/8676314/config/db.go#L535). Runtime fixture dua versi, limit1: versi lebih baru berisiko tinggi keluar dahulu; request berikutnya kosong, versi sebelumnya terlewat. |
| Medium / P0 | Duplikat tetap tersimpan walau agregator menghitung identitas unik; dedup skor bukan idempotensi request. | [DB insert:351](https://github.com/CallTag/backend/blob/8676314/config/db.go#L351), [aggregation:211](https://github.com/CallTag/backend/blob/8676314/helper/scoring.go#L211). Runtime dua report identik:2 dokumen, unique_reporters1, score16, SUSPICIOUS. |
| Medium / P1 | Angka latency hanya waktu baca Room, bukan total remote; seed/simulasi mencampur riwayat dan bukti komunitas. | [latency:97](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L97), [seed comments:494](../app/src/main/java/id/calltag/app/data/local/OfflineSeedData.kt#L494). Jangan pakai sebagai pembuktian KPI live. |

Uji abuse lokal juga membuktikan11 pengiriman tanpa JWT dengan `X-Device-Id` berbeda seluruhnya diterima201 dan menghasilkan DANGER_SCAM, skor100, unique_reporters11. Karena identitas perangkat dapat dipilih pemanggil, dedup identitas belum menjadi anti-brigading yang memadai. Ini bukti lokal memakai fixture sintetis, bukan klaim serangan pada deployment publik.

## PROPOSED: MVP minimum untuk tim dua orang

Urutan berikut menutup satu rantai vertikal sebelum memperluas fitur. Orang A memimpin backend/data; orang B memimpin Android/UI. Review kontrak dan skenario gagal dilakukan bersama. Estimasi relatif perlu dikalibrasi setelah P0 pertama, bukan janji tanggal rilis.

| Urutan | Scope | Orang A | Orang B | Kriteria selesai yang bisa diuji |
|---|---|---|---|---|
| P0.1 | Identitas & autentikasi | Wajib JWT untuk write; invalid/expired401; definisikan guest read-only; identitas perangkat bukan nomor terverifikasi | Onboarding perangkat, token lifecycle,401/expired UI, ID instalasi persisten | Form→API→Mongo dengan user sah; invalid token tidak membuat laporan. Tidak mengklaim kepemilikan nomor tanpa verifikasi. |
| P0.2 | Report dan data provenance | Idempotency key + indeks; rate limit dasar; satu identitas tak menambah unique_reporters | Status sent/pending/failed typed; outbox Room raw payload; retry terkontrol; hapus agregasi skor lokal sebagai kebenaran server | Offline tersimpan pending, restart tetap ada, replay sekali, server acknowledgment baru sent; penolakan400/401 tidak disulap menjadi laporan sukses. |
| P0.3 | False positives & dispute persisten | Koleksi Disputes dengan owner/requester, status, timestamp; koreksi/demotion versioned; whitelist precedence | Unknown berbeda dari CLEAN di semua tampilan; cache TTL; flag whitelist; fail-open callback dan status stale | Dispute diterima hanya setelah DB persist; nomor demoted tidak lagi terblokir setelah sync; unknown tidak pernah ditampilkan aman. |
| P0.4 | Sync kontrak benar | Change log/cursor monotonic, stable ordering, pagination, tombstone/demotion, atomisitas update | Metadata cursor terpisah dari cache rows, transaksi page+cursor, retry dan clear progress | Dataset lebih besar dari limit tersinkron penuh; seed/local tidak memajukan server cursor; replay page aman. |
| P1.1 | Minimum operasi admin: endpoint + panel | RBAC moderator/admin, list/detail reports/disputes, decision mutation, audit event | Panel web sederhana: login, tabel antrean/filter, detail bukti, aksi dengan alasan, audit view | Non-admin403; keputusan terlacak; revisi reputasi menghasilkan sync event; panel tidak memegang secret server. |
| P1.2 | Proteksi & direktori dapat diuji | Metadata freshness dan endpoint data direktori konsisten | Toggle notifikasi efektif, grant overlay, state izin akurat, label demo, dialer/WA failure feedback | Uji API29+/33+/34 dengan permission deny/revoke, offline/cache miss dan role tidak aktif; panggilan nyata diuji terpisah. |

### PROPOSED: minimum endpoint dan panel admin P1

Semua route berikut **belum ada**; nama adalah rancangan kontrak, bukan endpoint yang dapat dipanggil sekarang. Infrastruktur persistensi dispute P0 mendahului panel.

| Endpoint usulan | Hak / fungsi | Panel minimum |
|---|---|---|
| `GET /api/v1/admin/reports` dan `GET /api/v1/admin/reports/{id}` | Moderator; pagination, filter status/kategori/nomor; data pribadi dibatasi | Antrean laporan + detail dan provenance |
| `GET /api/v1/admin/disputes` dan `GET /api/v1/admin/disputes/{id}` | Moderator; kasus tersimpan dan riwayat | Antrean koreksi + bukti pemohon |
| `POST /api/v1/admin/disputes/{id}/decision` | Moderator; expected_version, keputusan, alasan wajib | Setujui/tolak/minta informasi; cegah aksi ganda |
| `POST /api/v1/admin/phones/{e164}/review` | Admin; koreksi label/whitelist dengan alasan, versi dan audit | Preview dampak dan konfirmasi perubahan konkret |
| `GET /api/v1/admin/audit-events` | Admin; event append-only berisi actor, target, before/after, alasan, timestamp | Pencarian jejak keputusan |
| `GET /api/v1/disputes/{id}` | Pemohon sah; hanya kasus miliknya | Status koreksi pada aplikasi |

Jangan menambah CRUD seluruh tabel, dashboard grafik kompleks atau integrasi setiap lembaga sebelum antrean inti, akses staf dan rollback reputasi teruji. Pembuktian kepemilikan nomor untuk dispute harus dirancang eksplisit; `phone` yang dikirim klien bukan bukti.

## PROPOSED: pasca-MVP yang realistis

### Keputusan biaya dan alternatif administrasi

Semua fungsi di bawah belum tersedia pada baseline. Estimasi S/M/L relatif pada roadmap, bukan janji tanggal. Panel minimum dapat satu halaman internal dengan tabel dan detail, tidak memerlukan aplikasi web admin besar.

| Kapabilitas | Keputusan / biaya | Dependensi dan risiko tanpa fitur | Alternatif sederhana |
|---|---|---|---|
| Login staf + RBAC user/moderator/admin | Wajib MVP P1; M | Auth P0; tanpa enforcement user dapat bertindak sebagai staf | Satu akun staf terkelola dengan credential aman dan role server, tanpa super_admin |
| Cari/filter laporan + review bukti | Wajib MVP P1; M | Report durable/idempotent; tanpa ini false report langsung memengaruhi reputasi | Antrean tabel paginated dan detail, bukan dashboard analytics |
| Dispute resolution + koreksi false positive | Persistence P0; operasi staf P1; L | Ownership dan sync revocation; tanpa ini pemilik nomor tak dapat memulihkan label | Satu action decision dengan alasan, audit event dan status pemohon |
| Whitelist/official number + bukti + expiry | Wajib MVP P1; M | Provenance, moderator dan revision; tanpa expiry nomor daur ulang bisa dianggap resmi | Data terkurasi kecil, review manual; business onboarding mandiri P2 |
| CRUD direktori/kanal dengan sumber dan last verified | Wajib MVP P1; M+kurasi | Pilot dataset dan hak akses; kontak stale dapat menghambat bantuan | Form internal atau import file tervalidasi dengan audit perubahan |
| Ringkasan report pending/dispute unresolved | Disarankan P1; S | Query status/index, data benar; tanpa ringkasan kasus dapat terlewat | Count dan filter di tabel antrean yang sama |
| Audit tindakan dan akses sensitif | Wajib MVP P1; M | Principal staf, redaksi dan retention; koreksi tanpa jejak sulit dipertanggungjawabkan | Append-only event minimal actor/action/target/reason/timestamp |
| Analytics lanjutan, super_admin, hierarki organisasi | Opsional P2/P3; L | Tidak dibutuhkan tim dua orang/pilot; menambah biaya dan attack surface | Gunakan metrik operasional sederhana sampai ada kebutuhan nyata |

Identitas pelapor, contact_ref pemohon dan bukti kepemilikan hanya untuk principal yang berhak/staf terbatas. Publik cukup melihat label agregat dan ringkasan komentar yang dimoderasi; dump raw report/user bukan fitur MVP.

| Prioritas | Fitur | Alasan / batas |
|---|---|---|
| P2 awal | Data direktori kota tambahan dan jadwal verifikasi sumber | Bernilai tanpa menambah permission sensitif; perlu penanggung jawab pembaruan. |
| P2 awal | Riwayat laporan sendiri dan status dispute | Gunakan data persisten/RBAC yang sudah diselesaikan; hindari realtime/push dulu. |
| P2 berikut | Sync terjadwal hemat baterai dan metrik latensi nyata | Setelah correctness cursor/TTL; ukur p50/p95 terpisah cache, API dan callback. |
| P2 berikut | Bantuan ringkas pengumpulan bukti untuk hub resmi | Tetap handoff eksternal; tidak mengklaim mengajukan atau melacak tiket lembaga. |
| Ditunda | Deteksi notifikasi pesan/APK, ML, browser extension, peta realtime, integrasi tiket pemerintah | Scope/perizinan/data operasional terlalu besar untuk dua orang sebelum proteksi telepon dan koreksi nomor terbukti. |

Diagram kondisi saat ini dan target ada di [FLOWS_AND_DIAGRAMS](FLOWS_AND_DIAGRAMS.md). Rencana pengujian dan sequencing lintas repo ada di [QA_TEST_PLAN](QA_TEST_PLAN.md) dan [IMPLEMENTATION_ROADMAP](IMPLEMENTATION_ROADMAP.md).
