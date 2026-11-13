# 01. Visi, masalah, ruang lingkup, dan indikator keberhasilan

Status: **PROPOSED**. Melanjutkan [PRD](../../PRD.md) bagian 1 sampai 3; tidak mengganti visi yang sudah ada.

## 1. Evaluasi konsep

Konsep sepuluh kemampuan (identifikasi, reputasi, tag komunitas, deteksi spam, peringatan panggilan, blokir aman, laporan, sanggahan, direktori darurat, kanal lapor resmi) **masuk akal dan layak** sebagai produk, dengan tiga catatan:

1. Nilai terbesar dan paling dapat dipertanggungjawabkan adalah **peringatan berbasis laporan komunitas yang dimoderasi** plus **direktori darurat/kanal resmi yang dikurasi**, bukan identifikasi nama orang.
2. "Identifikasi nama pemilik nomor" ala Getcontact **tidak** dijadikan fitur: butuh data buku kontak massal yang berisiko melanggar UU PDP (lihat [09](09-ANALISIS-GETCONTACT-CLI.md)).
3. Kelayakan bergantung pada ketersediaan data. Tanpa jumlah pelapor yang cukup, hasil mayoritas adalah UNKNOWN. Produk harus jujur tentang itu.

## 2. Visi, masalah, pengguna

| Unsur | Rumusan |
|---|---|
| Visi | Perisai komunikasi dan asisten darurat warga Indonesia yang **jujur tentang apa yang diketahuinya** (PRD 1.2). |
| Masalah | Penipuan telepon (pinjol/DC, APK, impersonasi, judol, rekayasa sosial), serta nomor darurat/kanal lapor yang tersebar. |
| Target | Pengguna Android 8.0+ (`minSdk 26`), awalnya satu wilayah pilot. Persona Rina, Pak Hendra, Dito pada PRD 3.1. |
| Keunggulan | Taksonomi scam khas Indonesia; direktori darurat offline; kanal lapor resmi; moderasi dan sanggahan; tanpa unggah buku kontak. |
| Batasan | Tidak menyebut nama orang; tidak memverifikasi identitas; cakupan data kecil; bergantung kualitas laporan; Call Screening butuh role sistem. |

## 3. Perbandingan konseptual

| Aspek | Getcontact / Truecaller (observasi publik) | CallTag (rancangan) |
|---|---|---|
| Sumber data | Buku kontak pengguna dan data publik (praktik berisiko di UU PDP) | Laporan sukarela + kurasi staf |
| Hasil | Nama/tag yang ditampilkan | Kategori ancaman, skor, jumlah pelapor unik, sumber, kesegaran |
| Identitas pemilik | Ditampilkan | **Tidak ditampilkan**; hanya pemilik terverifikasi (P2) yang dapat mengoreksi |
| Sanggahan | Opsi hapus/unlist | Dispute tercatat, ditinjau, koreksi disinkronkan |
| Darurat | Tidak utama | Direktori darurat + kanal lapor resmi, offline |
| Model | Komersial, skala besar | Proyek akademik, tim 2 orang |

Yang diadaptasi secara konseptual: tag komunitas, lookup cepat, overlay panggilan, sanggahan. Tidak diadaptasi: unggah kontak, nama pemilik, data tanpa persetujuan. Tidak ada kode atau data proprietari yang disalin.

## 4. Pembagian cakupan

| Kelompok | Isi |
|---|---|
| **MVP wajib (P0/P1)** | Lookup jujur (dua dimensi status), laporan dengan identitas server, dispute tersimpan, auth perangkat + anti-abuse minimum, auto-block konservatif opt-in, sinkronisasi aman, direktori pilot terkurasi, panel staf minimum, staging HTTPS, test inti |
| **Pendukung (P1/P2)** | Riwayat laporan pribadi, outbox offline, WorkManager sync, landing page terhubung API, statistik valid, onboarding permission |
| **Ditunda (P3 / Won't)** | Verified Phone Owner dengan OTP, bisnis terverifikasi komersial, GPS terdekat, deteksi pesan/APK, ML, multi-wilayah, integrasi API pihak ketiga |
| **Production tambahan** | Backup/restore teruji, monitoring, rotasi secret, target SDK sesuai kanal (Play membutuhkan API 36 sejak 31 Agu 2026 menurut audit; sideload akademik terpisah), kebijakan privasi sesuai praktik nyata, SLA moderasi |

## 5. Indikator keberhasilan

KPI numerik di PRD 2.2 tetap berlaku sebagai **target, belum terukur**. Tambahan untuk fase pilot:

| ID | Indikator | Cara ukur |
|---|---|---|
| KPI-01 | Persentase nomor DANGER_SCAM yang lolos review moderator | Sampel audit moderator |
| KPI-02 | False positive blokir pada set uji berlabel | Suite FPR (target PRD < 0,5%) |
| KPI-03 | Waktu median dispute sampai keputusan | `moderation_cases` timestamp |
| KPI-04 | Laporan dengan pelapor unik ≥ 2 dibanding total | Agregasi |
| KPI-05 | Callback screening selesai < 5 detik | Instrumentasi tanpa nomor utuh |

Seluruh KPI menunggu baseline pengukuran; tidak ada angka pencapaian yang diklaim.
