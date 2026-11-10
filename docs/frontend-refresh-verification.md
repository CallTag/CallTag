# Verifikasi frontend Android — 10 November 2026

Build: `./gradlew assembleDebug --offline --console=plain` berhasil. Verifikasi menggunakan emulator Android API 34 (`Pixel_5_Light`). Tidak ada dependensi baru.

## Diperiksa

- Empat tab pada mode terang dan gelap, termasuk warna system bar.
- Lebar 360dp dengan skala font 130%; label dan aksi utama dapat dibaca.
- Pencarian dari nomor contoh, hasil dan buka/tutup rincian komunitas.
- Hasil diteruskan ke formulir laporan dengan nomor yang tepat.
- Kembali ke Cari lalu melaporkan nomor yang sama membuka formulir dan menerapkan nomor kembali, meskipun sebelumnya berada pada subtab Koreksi.
- Isian formulir dan tab memakai state yang dapat disimpan saat berpindah tab.
- Reviewer independen memeriksa diff dan perbaikan prefill; tidak ada temuan penting tersisa.

## Artefak

- [Empat tab terang](screenshots/frontend-refresh/overview-light.png)
- [Empat tab gelap](screenshots/frontend-refresh/overview-dark.png)
- Screenshot individual, hasil pencarian serta 360dp/font besar berada di `screenshots/frontend-refresh/`.
- APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

## Batas pengujian

Tidak melakukan panggilan darurat, mengirim laporan nyata, atau menguji backend produksi. IME headless menerima fokus/permintaan input tetapi keyboard layar tidak tampil konsisten; perlu pemeriksaan lanjutan pada perangkat nyata. Masalah autentikasi dan scoring backend dari screening sebelumnya berada di luar pekerjaan visual ini.
