# Checklist Testing CallTag di HP Android

Gunakan APK debug dan HP kedua untuk menelepon. Ikuti [panduan menjalankan aplikasi di HP fisik](CARA_RUN_APLIKASI.md#4-jalankan-aplikasi-android-di-hp-fisik) untuk menyalakan backend dengan `GOOGLE_CLIENT_ID`, menghubungkan HP, dan memasang APK.

**Tanggal:** ___ · **Model HP / versi Android:** ___ · **Versi APK:** ___ · **Koneksi:** USB / Wi-Fi

Centang setelah hasil sesuai. Catat hasil yang berbeda atau langkah yang belum bisa dilakukan di bagian akhir.

## 1. Koneksi dan akses sebelum masuk

- [x] **Koneksi ke database.** Dari browser HP, buka `http://127.0.0.1:8080/readyz` untuk USB atau `http://IP_LAPTOP:8080/readyz` untuk Wi-Fi. Harapkan `status: ready` dan `storage_mode: mongodb-connected`. Di aplikasi, buka **Akun → Izin perlindungan → Periksa koneksi backend** dan pastikan backend terdeteksi.
- [x] **Cari sebelum masuk.** Buka **Cari → Coba contoh**, pilih contoh, dan periksa apakah hasil tampil. Coba juga nomor uji yang belum terdaftar saat backend aktif. Harapkan **Belum ada data**, tanpa label aman.
- [x] **Batas hak akses.** Sebelum masuk, buka **Beri #Tag → Laporkan nomor**. Aplikasi meminta masuk akun. Pastikan **Darurat** dan **Koreksi nomor → Lacak pengajuan** tetap bisa dibuka.

## 2. Caller ID dan panggilan masuk nyata (T-605)

Siapkan dua HP. Gunakan nomor uji yang kamu kendalikan dan belum disimpan di kontak HP penerima. Aktifkan peran penyaring panggilan, izin popup, dan notifikasi di **Akun → Izin perlindungan**. Di **Proteksi**, biarkan **Blokir otomatis penipu** nonaktif dan aktifkan **Notifikasi Caller ID**.

- [ ] **Pratinjau.** Buka **Proteksi → Pratinjau Caller ID** dan pilih salah satu tingkat risiko. Kartu simulasi tampil dan bisa ditutup.
- [ ] **Panggilan biasa.** Telepon HP penerima dari HP kedua. Ulangi saat layar terkunci. Panggilan tetap berdering. Catat apakah notifikasi atau popup muncul pada tiap kondisi.
- [ ] **Panggilan berlabel risiko.** Gunakan nomor pemanggil yang kamu kendalikan dan sudah punya data peringatan di lingkungan uji. Cek reputasinya di **Cari**, lalu telepon lagi. Cocokkan label Caller ID dengan hasil pencarian. Catat popup dan notifikasi secara terpisah. Jika belum ada data uji yang cocok, tandai **Tertunda**.
- [ ] **Backend tidak tersedia.** Hentikan backend sementara. Ulangi panggilan dari nomor uji yang belum ada di cache penerima. Panggilan tetap berdering. Buka **Darurat** dan pastikan kontak bawaan masih terlihat. Nyalakan backend kembali setelah selesai.
- [ ] **Callback ketika API lambat (K-07).** Pada pengujian terpisah dengan API uji yang sengaja merespons setelah 10 detik dan nomor yang belum ada di cache, ukur waktu dari callback screening masuk sampai aplikasi menjawabnya. Targetnya **kurang dari 5 detik** dan panggilan diizinkan. Jika pengukuran belum disiapkan, tandai **Tertunda**. [Kriteria K-07](CallTag/docs/planning/15-RENCANA-PENGUJIAN.md#2-kasus-kunci-traceability-ke-frnfr)

## 3. Akun, laporan, dan tampilan

- [ ] **Masuk dan keluar akun.** Gunakan koneksi USB atau API HTTPS. Buka **Akun → Masuk dengan Google** dan pilih akun di dialog Android. Halaman **Profil** menunjukkan nama dan email. Periksa akun di `calltag_db.users` di Compass. Coba keluar setelah pengujian laporan dan pastikan halaman Akun kembali ke **Belum masuk**.
- [ ] **Kirim #Tag.** Saat masuk akun dan backend aktif, kirim laporan untuk nomor uji. Jika aplikasi menyatakan laporan tersimpan di server, muat ulang koleksi `calltag_db.spam_reports` di Compass untuk melihatnya.
- [ ] **Direktori Darurat.** Pilih kota dan jenis layanan. Cocokkan pilihan dengan hasil daftar. Buka salah satu nomor di dialer dan tutup sebelum melakukan panggilan, terutama untuk nomor darurat resmi.
- [ ] **Koreksi nomor.** Buka **Beri #Tag → Koreksi nomor**. **Ajukan koreksi** membuka formulir, sedangkan **Lacak pengajuan** membuka kolom ID sanggahan. Jika sudah punya ID uji, coba pelacakan dan cocokkan statusnya.
- [ ] **Tema dan Back.** Ganti tema lewat **Akun → Tampilan & penyimpanan**. Cek keterbacaan tombol dan teks. Dari halaman detail Akun, Proteksi, dan Koreksi, coba panah kembali serta tombol Back Android.

## Catatan hasil

| Skenario | Status (Lulus / Gagal / Tertunda) | Hasil yang terlihat | Bukti |
|---|---|---|---|
| Koneksi backend | ___ | ___ | ___ |
| Panggilan biasa, layar terbuka / terkunci | ___ | ___ | ___ |
| Panggilan berlabel risiko | ___ | ___ | ___ |
| Panggilan saat backend mati | ___ | ___ | ___ |
| Callback API lambat (K-07) | ___ | Durasi: ___ ms | ___ |
| Fitur lain / temuan | ___ | ___ | ___ |

Untuk mengambil log saat melakukan panggilan uji, jalankan `adb -s SERIAL_HP logcat -s CallTagScreeningSvc AndroidRuntime` dengan serial dari `adb devices -l`. Samarkan nomor HP dan token sebelum menyimpan atau membagikan bukti. Ringkasan hasil T-605 bisa dicatat di [QA Test Plan](CallTag/docs/QA_TEST_PLAN.md#verifikasi-tahap-8--16-november-2026-wib).
