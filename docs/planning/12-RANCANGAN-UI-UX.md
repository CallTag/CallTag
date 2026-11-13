# 12. Rancangan UI/UX

Status: **PROPOSED**. Identitas **Nusantara Teal** dan aturan [DESIGN.md](../../DESIGN.md) dipertahankan (token warna, tipografi monospace untuk nomor, target sentuh 56/64 dp). Baseline: empat tab Cari / Darurat / Beri #Tag / Proteksi ([AGENTS.md](../../AGENTS.md)).

## 1. Navigasi

Pertahankan **empat tab**; tidak menambah tab kelima.

| Tab | Isi | Perubahan |
|---|---|---|
| Cari | Cari nomor + Hasil + Riwayat pencarian lokal | Hasil menjadi layar rincian (sheet) dengan status baru |
| Darurat | Direktori nasional/lokal + Kanal lapor resmi | Tampilkan `verified_at` |
| Beri #Tag | Lapor nomor + Riwayat laporan (status pending/terkirim/ditinjau) + Sanggahan | Sanggahan menjadi sub-alur dari Hasil dan Riwayat |
| Proteksi | Caller ID + Spam Guard + Pengaturan + Profil singkat | Menggabungkan Pengaturan dan Profil (tidak halaman terpisah) |

Penggabungan: Caller ID + Spam Guard → satu halaman "Proteksi" dengan dua bagian; Profil dan Pengaturan → "Proteksi › Akun dan data"; Sanggahan → bukan tab, tetapi formulir yang dibuka dari Hasil atau Riwayat. Navigation library tetap tidak diperlukan sampai ada lebih dari satu level dalam; evaluasi `navigation-compose` bila rincian bertingkat bertambah.

## 2. Halaman

| Halaman | Tujuan | Komponen utama | Aksi |
|---|---|---|---|
| Home/Cari | Memeriksa nomor cepat | Input nomor monospace, tombol cari, riwayat lokal | Cari, tempel dari clipboard |
| Hasil pencarian | Memahami reputasi jujur | Lencana status, skor, kategori, jumlah pelapor unik, tag, waktu pembaruan, sumber, status verifikasi | Laporkan, ajukan sanggahan, buka kanal resmi, salin |
| Caller ID (overlay/notifikasi) | Peringatan panggilan | Nomor, status, kategori, tombol "Bukan spam" | Tutup, buka aplikasi |
| Proteksi (Spam Guard) | Atur perlindungan | Status role, toggle notifikasi/overlay, toggle auto-block (default OFF) dengan penjelasan, log screening | Minta role, ubah toggle |
| Lapor nomor | Berkontribusi | Nomor, kategori, tag, komentar (hitung karakter), jenis kontak | Kirim/simpan pending |
| Riwayat laporan | Transparansi | Daftar dengan status `PENDING/SENT/HELD/VALIDATED/REJECTED` | Kirim ulang, hapus pending |
| Direktori darurat | Akses cepat | Grid kode nasional, filter wilayah | Panggil, WhatsApp |
| Sanggahan | Koreksi | Nomor, alasan, kontak balik, bukti tautan | Kirim, lacak status |
| Akun dan data | Hak data | Status akun, kuota, versi kebijakan | Hapus data, ekspor (P2) |

## 3. State per halaman

| Halaman | Loading | Empty | Error | Offline | Success |
|---|---|---|---|---|---|
| Cari/Hasil | Skeleton tidak dipakai; indikator progres + teks "memeriksa" | "Masukkan nomor." / UNKNOWN: "Belum ada data. Bukan jaminan aman." | "Tidak dapat memeriksa", tombol coba lagi | STALE/OFFLINE berlabel, bukan aman | Status + sumber |
| Lapor | Tombol disabled + progres | - | Pesan validasi/kuota/`429` | "Tersimpan di perangkat, belum terkirim" | "Diterima server" atau "Menunggu moderasi" |
| Riwayat | Progres | "Belum ada laporan." | Gagal muat + coba lagi | Data lokal pending tetap tampil | Daftar |
| Darurat | - | "Pilih wilayah" (nasional selalu ada) | Gagal sinkron, data tetap dari cache | Cache + tanggal verifikasi | Daftar |
| Proteksi | - | Role belum dipegang: panduan | Gagal minta role | Tidak bergantung jaringan | Status aktif |
| Sanggahan | Progres | - | Validasi/kuota | Antre di outbox, dengan kata "belum terkirim" | `dispute_id` + pelacak |

Aksesibilitas: label TalkBack untuk semua ikon, kontras sesuai audit DESIGN, teks hingga 130%, fokus dan urutan baca logis, status tidak hanya dengan warna (ikon + kata). Tidak ada teks "AMAN" untuk status yang tidak terverifikasi.

## 4. Landing page (`calltag.github.io`)

Struktur mengikuti isi nyata, bukan templat:

1. **Pemeriksa nomor** (fungsi utama) dengan status jujur dan penjelasan "belum ada data bukan berarti aman".
2. **Cara kerja**: kategori ancaman, skor sebagai indikator, peran moderasi dan sanggahan.
3. **Direktori darurat dan kanal lapor resmi** (tersedia di web).
4. **Privasi**: apa yang dikumpulkan dan tidak (tautan ke `privacy.html`, harus sesuai implementasi final).
5. **FAQ khusus produk**: mengapa nomor saya muncul, bagaimana menyanggah, apakah aplikasi membaca kontak, apa itu skor.
6. **Unduhan**: tautan rilis dengan versi hasil verifikasi (perilaku test existing dipertahankan).

Pernyataan statistik hanya dari `GET /stats/public`; tanpa angka contoh dan tanpa klaim "terblokir" kecuali dihitung dari sumber nyata. Form lapor cepat web dinonaktifkan pada MVP ([05](05-ARSITEKTUR-SISTEM.md)).
