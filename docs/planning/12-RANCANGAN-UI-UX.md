# 12. Rancangan UI/UX

Status: **PROPOSED**. Identitas **Nusantara Teal** dan aturan [DESIGN.md](../../DESIGN.md) dipertahankan (token warna, tipografi monospace untuk nomor, target sentuh 56/64 dp). Baseline: empat tab Cari / Darurat / Beri #Tag / Proteksi ([AGENTS.md](../../AGENTS.md)).

## 1. Navigasi

Baseline lama memakai 4 tab. Mulai **Tahap 5B (`T-308` s.d. `T-314`)**, navigasi ditingkatkan menjadi **lima tab** agar pengguna mudah menemukan profil, login opsional, pengaturan, tutorial, umpan balik, serta pengalaman pencarian nomor yang lebih informatif seperti aplikasi Caller ID modern:

| Tab | Isi | Perubahan |
|---|---|---|
| Cari | Cari nomor + Hasil + Riwayat pencarian lokal (*login untuk riwayat*) | Hasil menjadi layar rincian dengan status 2D & ajakan login bila tamu |
| Darurat | Direktori nasional/lokal + Kanal lapor resmi | Tampilkan `verified_at` |
| Beri #Tag | Lapor nomor & tambah tag info (*wajib login*) + Outbox + Sanggahan + Lacak Tiket | Dukung mode risiko spam maupun `#Tag` netral `INFO_TAG` (bobot 0); pembatasan untuk Tamu; lacak dispute via token tetap terbuka |
| Proteksi | Caller ID + Spam Guard + Sync Delta + Simulator Overlay | Fokus pada kontrol proteksi panggilan & sinkronisasi |
| Akun | Login/Logout Opsional + Kuota (`/me`) + Health Check Izin (`T-310`) + Whitelist Pribadi (`T-311`) + Tutorial (`T-309`) + Saran/Bug (`T-312`) | **Tab baru Tahap 5B** menggabungkan akun, pengaturan lengkap, & bantuan |

Navigation library tetap tidak diperlukan sampai ada lebih dari satu level dalam; evaluasi `navigation-compose` bila rincian bertingkat bertambah.

## 2. Halaman

| Halaman | Tujuan | Komponen utama | Aksi |
|---|---|---|---|
| Home/Cari | Memeriksa nomor cepat | Input nomor monospace, tombol cari, deteksi otomatis konteks operator/kota/negara asing, riwayat lokal (terkunci jika Guest) | Cari, tempel, masuk akun |
| Hasil pencarian | Memahami reputasi jujur & konteks nomor | Lencana status (termasuk `VERIFIED` + sumber resmi), info operator HLR/PSTN/negara (`T-313`), skor, kategori, jumlah pelapor unik, awan `#Tag (count)` langsung terbuka (`T-314`), waktu pembaruan, sumber | `+ Tambah #Tag`, `Lihat di WhatsApp` (`wa.me`), ajukan sanggahan, buka kanal resmi, salin |
| Caller ID (overlay/notifikasi) | Peringatan panggilan | Nomor, status, kategori, tombol "Bukan spam" | Tutup, buka aplikasi |
| Proteksi (Spam Guard) | Atur perlindungan | Status role, toggle notifikasi/overlay, toggle auto-block (default OFF) dengan penjelasan, log screening | Minta role, ubah toggle, sync delta |
| Lapor nomor | Berkontribusi (terkunci jika Guest) | Nomor, kategori, tag, komentar (hitung karakter), jenis kontak, kartu ajakan login | Kirim/simpan pending, masuk akun |
| Riwayat laporan | Transparansi (terkunci jika Guest) | Daftar dengan status `PENDING/SENT/HELD/VALIDATED/REJECTED` | Kirim ulang, hapus pending |
| Direktori darurat | Akses cepat | Grid kode nasional, filter wilayah | Panggil, WhatsApp |
| Sanggahan | Koreksi | Nomor, alasan, kontak balik, bukti tautan | Kirim, lacak status |
| Akun & Pengaturan *(Tahap 5B)* | Manajemen akun & bantuan | Kartu Login/Guest & Kuota `/me`, Health Check izin sistem, Whitelist pribadi, Tema/Cache, Tutorial aktivasi, Form Saran/Kritik/Bug | Login/Logout, Hapus data UU PDP, Tambah/Hapus whitelist, Buka Settings Android, Kirim feedback |

## 3. State per halaman

| Halaman | Loading | Empty | Error | Offline | Success |
|---|---|---|---|---|---|
| Cari/Hasil | Skeleton tidak dipakai; indikator progres + teks "memeriksa" | "Masukkan nomor." / UNKNOWN: "Belum ada data. Bukan jaminan aman." | "Tidak dapat memeriksa", tombol coba lagi | STALE/OFFLINE berlabel, bukan aman | Status + sumber |
| Lapor | Tombol disabled + progres | Guest: "Masuk akun untuk mulai melapor" | Pesan validasi/kuota/`429` | "Tersimpan di perangkat, belum terkirim" | "Diterima server" atau "Menunggu moderasi" |
| Riwayat | Progres | Guest: ajakan login / User: "Belum ada laporan." | Gagal muat + coba lagi | Data lokal pending tetap tampil | Daftar |
| Darurat | - | "Pilih wilayah" (nasional selalu ada) | Gagal sinkron, data tetap dari cache | Cache + tanggal verifikasi | Daftar |
| Proteksi | - | Role belum dipegang: panduan | Gagal minta role | Tidak bergantung jaringan | Status aktif |
| Sanggahan | Progres | - | Validasi/kuota | Antre di outbox, dengan kata "belum terkirim" | `dispute_id` + pelacak |
| Akun & Pengaturan | Progres muat `/me` atau `/health` | Guest: form login opsional; Whitelist kosong | Gagal login/hapus/kirim | Mode Tamu & Whitelist lokal tetap jalan; feedback bisa disalin | Profil aktif, izin sehat, template feedback tersalin |

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

