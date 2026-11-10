# Penataan Ulang Frontend CallTag

Jenis: aplikasi utilitas keamanan konsumen; Android native, dilanjutkan website responsif.

## Arah yang dipilih

Pengguna memilih penataan ulang empat tab Android, kemudian website. Alternatif perapian font dan jarak saja tidak dipilih. Identitas Nusantara Teal, navigasi Cari / Darurat / Beri #Tag / Proteksi, dan fungsi inti tetap menjadi fondasi.

Tujuan: tampilan lebih menarik dan terasa dirancang sebagai satu produk, dengan prioritas jelas, teks terbaca, kontrol mudah disentuh, serta rincian yang tidak menenggelamkan aksi utama.

## Desain Android

- Shell: header ringkas dengan ikon tema, system bar mengikuti tema, navigasi bawah konsisten. Latar hangat dan kartu putih pada mode terang, tinta kehijauan pada mode gelap. Radius dan ruang memakai token bersama.
- Cari: judul kuat, bidang pencarian lebar, aksi cek nomor jelas, contoh ditandai sebagai contoh. Tampilan awal tidak otomatis memeriksa nomor demo. Hasil memprioritaskan nomor, status, skor dan aksi; rincian komunitas bisa diperluas.
- Darurat: pengantar di luar kartu, akses 112 utama dan direktori nasional mudah dipindai; filter serta posko daerah lebih dekat. Semua nomor dan tindakan panggil tetap berasal dari data yang ada.
- Beri #Tag: judul dan navigasi sekunder terbaca penuh, formulir dibagi menjadi kelompok nomor, kategori dan keterangan. Kartu kanal resmi dan koreksi memakai hierarki yang sama.
- Proteksi: status pengaturan dibedakan dari status izin; pengaturan, penyimpanan offline dan contoh Caller ID memiliki bagian jelas. Angka cache memakai label yang spesifik.

## Batasan

Jetpack Compose dan dependensi yang ada. Teks Indonesia. Monospace untuk nomor/skor. Target sentuh utama minimal 56dp, darurat 64dp. Warna semantik bahaya PRD dipertahankan. Data backend, algoritma reputasi dan kontrak API tidak diubah. Tidak menampilkan angka statistik atau klaim proteksi yang dibuat-buat.

Taste Skill diadaptasi untuk aplikasi native: hierarki, tepi sejajar, tipografi, komponen konsisten dan respons sentuh. Aturan landing page seperti shader, hover dan tinggi satu layar tidak diterapkan ke formulir native.

## Website tahap berikutnya

Selaraskan identitas teal dan tata letak layar kecil dengan Android. Pertahankan modul JS dan integrasi yang ada. Audit screenshot dahulu; perbaiki kepadatan, menu, kontrol dan overflow. Artefak yang sudah ada milik pengguna harus dipertahankan.

## Verifikasi

Baseline empat tab terang/gelap direkam sebelum perubahan. Artefak final berada di `docs/screenshots/frontend-refresh/`. Build baseline berhasil. Verifikasi sesudah perubahan: `assembleDebug --offline`, empat tab terang/gelap, layar 360dp, font diperbesar, keyboard formulir, tab sekunder, perluasan hasil dan navigasi. Tidak melakukan panggilan atau mengirim laporan ke layanan nyata. Website diperiksa dengan skrip Taste Skill dan screenshot responsif.
