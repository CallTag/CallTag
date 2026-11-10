# Frontend Refresh Implementation Plan

> **For agentic workers:** Gunakan tugas terpisah di bawah; implementasi paralel hanya untuk file layar yang berbeda. Review integrasi dilakukan setelah build.

**Goal:** Menata ulang empat tab Android dan melanjutkan penyelarasan website untuk layar kecil.
**Architecture:** Komponen dan token Compose bersama, layar tetap memakai ViewModel/repository yang ada. Website mempertahankan modul vanilla JS.
**Tech Stack:** Kotlin, Jetpack Compose, Android SDK; HTML/CSS/JavaScript.
**Spec:** `docs/superpowers/specs/2026-11-10-frontend-refresh-design.md`.

## Global Constraints

- Bahasa Indonesia; kategori dan warna reputasi tetap mengikuti PRD.
- Sentuhan utama 56dp dan darurat 64dp; nomor/skor monospace.
- Tidak mengubah backend atau memanggil nomor darurat untuk pengujian.
- Sesuai AGENTS.md, verifikasi Android menggunakan compile/build dan emulator; tidak menambahkan tes yang sekadar mencerminkan tata letak.
- Perubahan dikerjakan pada branch `codex/frontend-mobile-refresh`; branch ini dipush untuk ditinjau sebelum merge.

## Review Focus

1. Layar 360dp dan font 1.3: label, tombol dan nomor tidak bertumpuk atau terpotong.
2. Tema gelap: teks tombol dan status memiliki kontras yang jelas.
3. Keyboard: field terakhir dan tombol formulir tetap dapat dijangkau.
4. Data kosong/offline: tidak menampilkan klaim aman atau proteksi aktif hanya dari asumsi UI.
5. Navigasi: input dan tab bertahan saat rotasi, aksi tetap menuju tujuan sebelumnya.

## Tasks

- [x] 1. Shell dan komponen: `MainScaffold.kt`, `MainActivity.kt`, `CallTagTokens.kt`, `ui/components/ScreenIntro.kt`. Sediakan `ScreenIntro(eyebrow: String, title: String, description: String, modifier: Modifier = Modifier)`. Perbaiki system bar, hierarki header dan state navigasi.
- [x] 2. Cari: `HomeScreen.kt`. Panel pencarian, contoh berlabel, hasil ringkas dengan rincian dapat dibuka. Root menyesuaikan nilai awal pencarian di ViewModel. Periksa pencarian, expand/collapse dan navigasi ke laporan.
- [x] 3. Darurat dan Proteksi: `EmergencyScreen.kt`, `SpamGuardScreen.kt`. Susunan pengantar, nomor, filter dan pengaturan lebih jelas. Periksa label panjang, tema gelap, filter dan toggle.
- [x] 4. Beri #Tag: `ReportAndHubScreen.kt`. Kelompok formulir, tab sekunder terbaca dan kartu kanal resmi. Periksa semua subtab, pilihan kategori, keyboard serta nomor yang diteruskan dari pencarian.
- [x] 5. Integrasi: build APK, pasang di emulator, screenshot terang/gelap dan ukuran kecil/font besar. Review kode independen lalu perbaiki temuan penting.
- [x] 6. Website: baca referensi Taste Skill dan Sites, rekam baseline, buat rencana lokal khusus web, terapkan identitas dan responsivitas, jalankan pemeriksaan visual.
- [x] 7. Catat bukti verifikasi dan batas pengujian; siapkan perubahan untuk ditinjau melalui branch fitur.

## Progress

- Baseline Android `assembleDebug --offline`: berhasil, sebelum perubahan.
- Keputusan: melanjutkan pilihan penataan ulang yang telah diberikan pengguna tanpa meminta ulang izin untuk perubahan lokal yang reversibel.

- Android final `assembleDebug --offline`: berhasil setelah seluruh perubahan (task Kotlin dijalankan ulang).
- Empat tab terang/gelap, 360dp/font 1.3, pencarian contoh, perluasan rincian dan pengulangan navigasi laporan diverifikasi lewat emulator.
- Review independen: regresi prefill diperbaiki dengan ID permintaan; pemeriksaan akhir tidak menemukan masalah penting.
- Pemeriksaan IME headless terbatas: field dapat difokuskan dan IME menerima permintaan, tetapi tampilan keyboard layar tidak konsisten. Uji keyboard perangkat nyata masih diperlukan.

- Website: layout final tanpa temuan pada 1440/1024/820/768/390px. Perf: 0 FAIL, 2 WARN. Enam tes perilaku lulus.

- Handoff: ringkasan lintas repo tersimpan di `../FRONTEND_REFRESH.md`; preview lokal HTTP 200 pada port 8000. Font web dilayani lokal beserta lisensi OFL.
