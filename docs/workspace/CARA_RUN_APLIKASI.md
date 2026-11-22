# Panduan Cepat: Cara Run & Revisi Aplikasi CallTag (VS Code / Antigravity)

Gunakan terminal **Fish** di VS Code atau Antigravity. Buka dua terminal. Terminal 1 menjalankan backend dan Terminal 2 menjalankan perintah Android. Untuk tes di HP, mulai dari [langkah 1](#1-mongodb-compass-dan-backend-terminal-1), lalu ke [langkah 4](#4-jalankan-aplikasi-android-di-hp-fisik).

Untuk penjelasan arsitektur dan fungsi setiap folder secara mendalam, lihat [PANDUAN_MENJALANKAN.md](PANDUAN_MENJALANKAN.md).

---

## 1. MongoDB Compass dan Backend (Terminal 1)

**Untuk pemakaian sehari-hari, cukup jalankan backend.** Di laptop ini, service `mongodb` sudah aktif dan diatur untuk mulai saat laptop menyala. Compass bisa dibuka kapan saja untuk melihat database yang sama. MongoDB Server menyimpan data, Compass menampilkannya, dan HP mengakses data lewat backend CallTag.

### Buka database di Compass

1. Buka **MongoDB Compass → Add New Connection**.
2. Masukkan URI `mongodb://127.0.0.1:27017`. Simpan sebagai **CallTag Lokal** lewat **Save & Connect**.
3. Setelah backend terhubung dan mengisi data awal, pilih database **`calltag_db`** di panel kiri. Buka **Documents** pada koleksi `phone_tags` untuk melihat reputasi nomor, `users` untuk akun, atau `spam_reports` untuk laporan yang tersimpan. Jika koleksi atau dokumen baru belum terlihat, muat ulang tampilan Compass.

Compass boleh ditutup ketika tidak diperlukan. Jika koneksi ditolak, cek service dengan `systemctl is-active mongodb`. Jika hasilnya `inactive`, jalankan `sudo systemctl start mongodb`. Agar mulai otomatis di komputer lain yang memakai service ini, gunakan `sudo systemctl enable --now mongodb` sekali saja. [Panduan koneksi Compass](https://www.mongodb.com/docs/compass/connect/)

### Jalankan backend

```bash
cd ~/Documents/Kuliah/Semester5/CallTAG/backend &&
  env APP_ENV=dev MONGOSTRING="mongodb://127.0.0.1:27017" DB_NAME=calltag_db PORT=8080 \
    GOOGLE_CLIENT_ID="23321394343-9vp7211b0tplpro2upuo0pvoj8s31tl7.apps.googleusercontent.com" go run .
```

Biarkan Terminal 1 terbuka. Jika backend sudah berjalan, cek respons di Terminal 2 sebelum membuka proses kedua. Jika muncul `address already in use`, port 8080 sudah digunakan. Jika backend lama masih memakai mode memori, tekan **Ctrl+C di terminal proses lama**, lalu jalankan blok backend di atas kembali. Mulai dengan mengecek database dari Terminal 2:

```bash
curl -fsS http://127.0.0.1:8080/readyz
```

Cari `"storage_mode":"mongodb-connected"` pada respons. Jika muncul `in-memory-seeded`, backend memakai penyimpanan sementara dan data baru tidak muncul di Compass. Jika `/readyz` gagal, cek bahwa service MongoDB aktif, URI di perintah benar, dan backend sudah menyala. `/health` hanya memeriksa apakah backend merespons.

---

### Buat akun admin web dari Fish

Jalankan sekali di Terminal 2. Contoh ini membuat username `admin-ridwan` di database `calltag_db`. Siapkan kata sandi dengan **minimal 8 karakter** jika memakai huruf, angka, dan simbol ASCII. Batas kata sandi adalah 72 byte.

```fish
cd ~/Documents/Kuliah/Semester5/CallTAG/backend
read --silent --prompt-str 'Kata sandi admin: ' CALLTAG_ADMIN_PASSWORD
printf '\n'
printf '%s\n' admin-ridwan "$CALLTAG_ADMIN_PASSWORD" ADMIN | env MONGOSTRING=mongodb://127.0.0.1:27017 DB_NAME=calltag_db go run ./cmd/seedstaff -enroll
set -e CALLTAG_ADMIN_PASSWORD
```

Jika muncul `staff creation rejected` setelah memasukkan kata sandi kurang dari 8 karakter, ulangi blok di atas dengan kata sandi yang lebih panjang. Setelah akun berhasil disimpan, perintah menampilkan **TOTP enrollment** dan **Staff account created**.

1. Pada output **TOTP enrollment**, ambil nilai `secret` setelah `secret=` hingga tanda `&` berikutnya atau akhir tautan. Di aplikasi autentikator, pilih tambah akun dengan **kunci penyiapan**, beri nama **CallTag admin-ridwan**, lalu pilih **berbasis waktu**. Simpan kunci hanya di autentikator.
2. Buka [Panel staf CallTag](http://127.0.0.1:8080/admin/). Gunakan username `admin-ridwan`, kata sandi yang tadi dibuat, dan kode 6 digit dari autentikator.
3. Di Compass, muat ulang `calltag_db` dan buka `staff_accounts` untuk melihat username dan role. Koleksi ini juga berisi data autentikasi sensitif. Akun Google Android tetap ada di `users`.

Jika pernah membuat akun staf dengan email, gunakan [panduan migrasi username](backend/STAFF-AUTH.md#migrating-an-existing-staff-account) untuk memakai kembali akun lama.


### Cari data dan tambahkan posko dari panel staf

- **Reputasi Nomor** menampilkan seluruh entri reputasi per halaman. Cari dengan nomor (misalnya awalan `0812`), tag, atau kategori. Pilih **Lihat detail** untuk melihat tag, jumlah suara, serta laporan dan kasus terkait.
- **Direktori Publik** menyediakan pilihan **Nomor resmi**, **Posko dan nomor darurat**, serta **Kanal pengaduan resmi**. Cari entri untuk melihat atau mengedit datanya.
- Untuk menambah posko, pilih **Posko dan nomor darurat → Tambah posko atau nomor**. Isi cakupan **Daerah**, provinsi, kota/kabupaten, nomor kontak, sumber resmi, dan alasan perubahan. Isi koordinat dalam urutan **bujur, lintang** agar posko bisa diurutkan berdasarkan jarak. Pakai koordinat dari lokasi posko resmi. Tekan **Pratinjau publikasi** untuk memeriksa kontak, wilayah, dan koordinat. Setelah sesuai, tekan **Terbitkan ke Darurat**.
- Di APK yang terbaru, buka ulang **Darurat** saat HP tersambung ke backend yang sama. Pilih provinsi atau kota/kabupaten baru dari daftar. Tekan **Urutkan dari lokasi saya** dan berikan izin lokasi jika ingin melihat urutan jarak garis lurus.


## 2. Jalankan Website & API Docs (Terminal 2 & 3 - Opsional)

Jika ingin membuka Website atau Swagger API Docs bersamaan dengan backend:

```bash
# Terminal 2: Swagger UI API Docs -> buka http://localhost:8081
cd ~/Documents/Kuliah/Semester5/CallTAG/apidocs
python3 -m http.server 8081
```

```bash
# Terminal 3: Website CallTag -> buka http://localhost:8082
cd ~/Documents/Kuliah/Semester5/CallTAG/calltag.github.io
python3 -m http.server 8082
```

---

## 3. Jalankan Aplikasi Android di Emulator (`Pixel_5_Light`)

Kamu cukup memakai **emulatornya saja** tanpa membuka project di Android Studio.

### Langkah A: Nyalakan Emulator (Pilih Salah Satu)

- **Langsung dari Terminal VS Code / Antigravity (Paling Cepat)**:
  ```bash
  nohup emulator -avd Pixel_5_Light >/dev/null 2>&1 &
  ```
- **Atau lewat jendela awal Android Studio**:
  Buka Android Studio &rarr; di layar *Welcome*, klik **More Actions (`...`) &rarr; Virtual Device Manager** &rarr; klik tombol **Play** pada **`Pixel_5_Light`**.

Tunggu sampai layar HP emulator menyala sempurna. Pastikan terdeteksi dengan:

```bash
adb devices
# Output: emulator-5554   device
```

### Langkah B: Build, Install, dan Buka Aplikasi (1 Baris Perintah)

Di **Terminal 4** VS Code / Antigravity:

```bash
cd ~/Documents/Kuliah/Semester5/CallTAG/CallTag &&
  ./gradlew assembleDebug -PbaseApiUrl="http://10.0.2.2:8080/" &&
  adb -e install -r app/build/outputs/apk/debug/app-debug.apk &&
  adb -e shell am start -n id.calltag.app/.MainActivity
```

> [!NOTE]
> Kamu **tidak perlu** mengetik `export JAVA_HOME` atau `export ANDROID_HOME` lagi karena JDK 17 (`/home/ridwan/.local/jdk17`) sudah dikunci otomatis di `~/.gradle/gradle.properties` dan SDK Android sudah diatur di `CallTag/local.properties`.

---

## 4. Jalankan Aplikasi Android di HP Fisik

**Bisa.** HP perlu menjangkau backend di laptop. MongoDB tetap di `127.0.0.1:27017` pada laptop, dan port database tidak perlu dibuka. Biarkan backend dan laptop menyala selama pengujian.

### Opsi A: USB dengan adb reverse (disarankan)

1. Aktifkan **USB Debugging** di menu **Opsi pengembang** HP. Sambungkan dengan kabel data, buka kunci HP, lalu setujui permintaan izin USB Debugging.
2. Di Terminal 2, jalankan `adb devices -l`. Pastikan HP berstatus `device`. Jika tertulis `unauthorized`, setujui izin di layar HP. Jika daftarnya kosong, coba kabel data atau port USB lain.
3. Sambungkan satu HP USB dan tempel blok perintah berikut. Opsi `adb -d` memilih HP USB walaupun emulator juga terbuka. Jika beberapa HP USB tersambung, cabut HP lain terlebih dahulu.

```fish
cd ~/Documents/Kuliah/Semester5/CallTAG/CallTag &&
  adb -d reverse tcp:8080 tcp:8080 &&
  ./gradlew assembleDebug -PbaseApiUrl="http://127.0.0.1:8080/" &&
  adb -d install -r app/build/outputs/apk/debug/app-debug.apk &&
  adb -d shell am start -n id.calltag.app/.MainActivity
```

`assembleDebug` membuat APK dengan alamat API untuk USB. `adb install -r` memperbarui APK dan mempertahankan data aplikasi jika tanda tangannya cocok. Tunggu **BUILD SUCCESSFUL** dan **Success** dari ADB. CallTag akan terbuka di HP.

Buka `http://127.0.0.1:8080/readyz` dari browser HP. Jika muncul `mongodb-connected`, HP sudah dapat menjangkau backend dan backend sudah tersambung ke MongoDB. Di aplikasi, buka **Akun → Izin perlindungan → Periksa koneksi backend**.

#### Kalau APK USB sudah terpasang

Setelah mencabut kabel atau merestart HP, sambungkan kembali dan jalankan blok ini. Pakai blok instalasi di atas jika kode Android atau alamat API berubah.

```fish
adb -d reverse tcp:8080 tcp:8080 &&
  adb -d shell am start -n id.calltag.app/.MainActivity
```

### Opsi B: Wi-Fi yang sama

Hubungkan HP dan laptop ke jaringan Wi-Fi tepercaya yang sama. Di laptop, jalankan `ip -br -4 addr` dan catat alamat IPv4 pada antarmuka Wi-Fi, misalnya `192.168.1.24`. Sambungkan HP ke USB untuk instalasi awal. Tempel blok berikut dan masukkan IP laptop saat diminta.

```fish
read --prompt-str "IPv4 laptop: " CALLTAG_LAN_IP
cd ~/Documents/Kuliah/Semester5/CallTAG/CallTag &&
  ./gradlew assembleDebug -PbaseApiUrl="http://$CALLTAG_LAN_IP:8080/" &&
  adb -d install -r app/build/outputs/apk/debug/app-debug.apk &&
  adb -d shell am start -n id.calltag.app/.MainActivity
```

Dari browser HP, buka `http://IP_LAPTOP:8080/readyz` dengan IP laptop yang tadi dimasukkan. Setelah muncul `mongodb-connected`, kabel bisa dicabut. Jika IP laptop berubah, build ulang dengan IP baru. Jika HP tidak bisa menjangkau backend lewat Wi-Fi, cek pengaturan firewall TCP 8080 atau gunakan USB. Port MongoDB 27017 tetap lokal.

### Coba aplikasi dan lihat hasilnya di Compass

- Untuk cek koneksi dalam aplikasi, buka **Akun → Izin perlindungan → Periksa koneksi backend**.
- Untuk login, gunakan APK debug melalui jalur **USB**, buka **Akun → Masuk dengan Google**, lalu pilih akunmu di dialog Android. **Profil** menampilkan nama dan email. Muat ulang koleksi `calltag_db.users` di Compass untuk melihat akun baru. Untuk APK rilis, arahkan aplikasi ke API HTTPS dan daftarkan SHA-1 sertifikat rilis pada proyek OAuth.
- Untuk uji Caller ID pada panggilan masuk nyata, aktifkan peran penyaring panggilan melalui **Akun → Izin perlindungan**, kemudian gunakan HP lain untuk menelepon. Tes koneksi API dan tes panggilan masuk merupakan dua pemeriksaan berbeda.

Setelah koneksi siap, lanjut ke [Checklist Testing HP](CHECKLIST_TESTING_HP.md).

### Kalau ada masalah

| Gejala | Yang perlu dilakukan |
|---|---|
| `device 'SERIAL_HP' not found` | Itu teks contoh dari panduan lama. Pakai blok USB di atas dengan `adb -d`. |
| `Unsupported use of '='` di Fish | Gunakan blok panduan ini. Perintah backend meneruskan variabel lewat `env`. |
| Laptop bisa membuka `/readyz`, HP USB gagal | Cek `adb -d reverse --list`. Jika pemetaan 8080 belum ada, jalankan `adb -d reverse tcp:8080 tcp:8080`. Jika browser HP bisa membuka `/readyz` tetapi aplikasi gagal, pasang ulang APK dengan alamat API sesuai jalur yang dipakai. |
| `in-memory-seeded` atau data tes tidak muncul di Compass | Cek service `mongodb`, lalu restart backend memakai perintah yang mengisi `MONGOSTRING`. Cek lagi `/readyz` dan muat ulang `calltag_db` di Compass. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | APK yang sudah terpasang punya tanda tangan berbeda. Pakai APK dengan tanda tangan yang sama. Menghapus aplikasi akan menghapus data lokalnya. |

---

## 5. Alur Kerja Saat Revisi Kode (Ngoding di VS Code / Antigravity)

### A. Saat Revisi Aplikasi Android (`CallTag/`)

Lokasi file penting yang paling sering diedit saat revisi:
- **Warna, Font Monospace, & Tema**: [CallTagTheme.kt](CallTag/app/src/main/java/id/calltag/app/ui/theme/CallTagTheme.kt)
- **Navigasi Bawah (5 Tab)**: [MainScaffold.kt](CallTag/app/src/main/java/id/calltag/app/ui/MainScaffold.kt)
- **Tab 1 (Cek Nomor & Riwayat)**: [HomeScreen.kt](CallTag/app/src/main/java/id/calltag/app/ui/screens/HomeScreen.kt)
- **Tab 2 (Direktori Darurat 112/Damkar/Polisi)**: [EmergencyScreen.kt](CallTag/app/src/main/java/id/calltag/app/ui/screens/EmergencyScreen.kt)
- **Tab 3 (Form Lapor, Sanggah, & Kanal Resmi)**: [ReportAndHubScreen.kt](CallTag/app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt)
- **Tab 4 (Spam Guard & Sync Offline)**: [SpamGuardScreen.kt](CallTag/app/src/main/java/id/calltag/app/ui/screens/SpamGuardScreen.kt)
- **Tab 5 (Akun & Pengaturan)**: [AccountSettingsScreen.kt](CallTag/app/src/main/java/id/calltag/app/ui/screens/AccountSettingsScreen.kt)
- **Kartu Peringatan Penelepon (Overlay)**: [CallerIdOverlayCard.kt](CallTag/app/src/main/java/id/calltag/app/ui/components/CallerIdOverlayCard.kt)
- **State UI & Aksi Tombol**: [CallTagMainViewModel.kt](CallTag/app/src/main/java/id/calltag/app/ui/viewmodel/CallTagMainViewModel.kt)
- **Koneksi API & Cache Room**: [CallTagRepository.kt](CallTag/app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt)
- **Deteksi Telepon Masuk**: [CallTagScreeningService.kt](CallTag/app/src/main/java/id/calltag/app/service/CallTagScreeningService.kt)

Simpan perubahan, lalu ulangi langkah build dan pemasangan untuk [emulator](#langkah-b-build-install-dan-buka-aplikasi-1-baris-perintah) atau [HP fisik](#4-jalankan-aplikasi-android-di-hp-fisik). Jika memakai Wi-Fi, gunakan kembali IP laptop yang benar. Setelah mengubah kode Go saja, cukup restart backend.

### B. Saat Revisi Backend Go (`backend/`)

1. Edit file di `backend/controller/`, `backend/model/`, atau `backend/route/`.
2. Hentikan backend dengan **Ctrl+C** di Terminal 1, lalu ulangi perintah di [Jalankan backend](#jalankan-backend) agar URI dan nama database tetap sama.
3. Untuk mencoba perubahan seed pada database kosong, hentikan backend dan jalankan dengan nama uji terpisah seperti `DB_NAME="calltag_seed_sandbox"` pada perintah yang sama. Buka database uji tersebut di Compass. Seed awal umumnya dimuat ketika koleksi target masih kosong.

### C. Saat Revisi Website (`calltag.github.io/`) atau API Docs (`apidocs/`)

1. Edit file `.html`, `.css`, atau `.js`.
2. Simpan (`Ctrl + S`), lalu langsung tekan **`Ctrl + Shift + R`** (Hard Reload) di browser. Tidak perlu me-restart `python3 -m http.server`.

---

## 6. Perintah Terminal Berguna Saat Testing & Debugging

| Keperluan | Perintah Terminal |
| :--- | :--- |
| **Simulasi telepon masuk di Emulator** (tanpa klik menu emulator) | `adb emu gsm call +6281299887766` |
| **Matikan simulasi telepon masuk di Emulator** | `adb emu gsm cancel +6281299887766` |
| **Lihat log panggilan atau crash di HP USB** | `adb -d logcat -s CallTagScreeningSvc AndroidRuntime` |
| **Reset / hapus data aplikasi di HP/Emulator** (seperti baru install) | `adb shell pm clear id.calltag.app` |
| **Tutup paksa (Force Stop) aplikasi dari terminal** | `adb shell am force-stop id.calltag.app` |
| **Uninstall aplikasi dari HP/Emulator** | `adb uninstall id.calltag.app` |
| **Hanya cek apakah kode Android ada error kompilasi** | `./gradlew compileDebugKotlin` |
| **Jalankan unit test Backend Go** | `cd ~/Documents/Kuliah/Semester5/CallTAG/backend && go test -v ./...` |

