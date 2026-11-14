# 03. Spesifikasi fitur, tag komunitas, dan reputation scoring

Status: **PROPOSED**. Basis: [PRD 4.1 sampai 4.3](../../PRD.md), [ADR-010](../ARCHITECTURE_DECISIONS.md), implementasi `backend/helper/scoring.go` menurut [API_CONTRACT_AUDIT](../API_CONTRACT_AUDIT.md).

## 1. Daftar fitur dan status

| ID | Fitur | Kelompok | Prioritas | Status as-is | Rujukan PRD |
|---|---|---|---|---|---|
| F-01 | Lookup nomor jujur (dua dimensi status) | Utama | P0 | PARTIAL | FR-002, FR-004 |
| F-02 | Caller ID overlay/notifikasi | Utama | P1 | IMPLEMENTED_NOT_E2E_TESTED | FR-003 |
| F-03 | Spam Guard + auto-block konservatif | Utama | P0 | PARTIAL (terlalu luas) | FR-003 |
| F-04 | Laporan + tag komunitas | Utama | P0 | PARTIAL | FR-005, FR-006 |
| F-05 | Outbox laporan offline | Pendukung | P1 | MISSING | FR-007 |
| F-06 | Dispute tersimpan dan terlacak | Utama | P0 | PARTIAL (tanpa persistensi) | FR-007 |
| F-07 | Moderasi laporan/dispute | Utama | P1 | MISSING | FR-010 |
| F-08 | Whitelist/nomor bisnis terkurasi | Pendukung | P1 | PARTIAL | FR-011 |
| F-09 | Direktori darurat + kanal resmi | Utama | P1 | PARTIAL | FR-009 |
| F-10 | Sinkronisasi delta | Utama | P0 | PARTIAL | FR-008 |
| F-11 | Akun perangkat + auth | Utama | P0 | PARTIAL | NFR-001 |
| F-12 | Riwayat laporan pribadi | Pendukung | P2 | MISSING | FR-014 |
| F-13 | Hak data (hapus/ekspor) | Pendukung | P1 | MISSING | FR-017 |
| F-14 | Verified Phone Owner | Ditunda | P3 | SPEC_ONLY | - |
| F-15 | Pesan/APK, GPS terdekat | Ditunda | P3 | SPEC_ONLY | FR-015, FR-016 |

## 2. Sistem tag komunitas

| Aspek | Rancangan |
|---|---|
| Pembuatan | Pengguna memilih **kategori baku** (6 kode) dan boleh memberi `custom_tag` bebas maksimal 40 karakter. Tag bebas **tidak** memengaruhi skor; hanya kategori. |
| Penyimpanan | Tag mentah di `spam_reports.custom_tag`; ringkasan teragregasi di `phone_tags.tags[]` (`tag`, `count`, `status`). |
| Normalisasi/penggabungan | `lowercase`, buang tanda baca dan spasi ganda, petakan sinonim dari kamus kecil milik admin (mis. "kurir palsu", "paket palsu" → `kurir paket mencurigakan`). Penggabungan hanya pada tampilan agregat; mentah tetap. |
| Moderasi | Tag baru tampil sebagai "dilaporkan" hanya jika ≥ 2 pelapor unik **atau** disetujui moderator. Tag mengandung PII (nomor, nama, NIK, rekening) ditolak otomatis dengan regex lalu masuk antrean. |
| Dukungan komunitas | Pengguna terdaftar dapat "konfirmasi serupa" pada laporan yang sudah ada (satu per principal per nomor). Konfirmasi dihitung sebagai laporan dengan komentar kosong, tunduk pada dedup yang sama. |
| Anti-manipulasi | Kuota 10 laporan/24 jam dan 3/5 menit per principal; satu kontribusi bernilai per (principal, nomor) per 30 hari; `trust_weight` mulai 0,5 untuk akun baru tanpa attestation; deteksi lonjakan (≥ 5 pelapor baru dalam 10 menit pada satu nomor menahan kenaikan skor sampai tinjauan). |
| Perlindungan identitas | Tidak ada nama orang pada tag. Komentar disaring dari PII sebelum tampil; reporter hanya pseudonim dan tidak pernah dikirim ke publik. |
| Tuduhan palsu | Alur dispute ([11](11-ADMIN-DAN-MODERASI.md)); laporan yang terbukti palsu menurunkan `trust_weight` pelapor. |
| Masa berlaku | Bobot laporan meluruh (half-life 90 hari). Laporan > 12 bulan tidak lagi dihitung dan nomor yang tidak diperbarui 12 bulan kembali ke `CLEAN`-tidak-diketahui (tombstone di sinkronisasi). |
| Nomor bisnis | Hanya `verified_numbers` (kurasi staf dengan bukti) yang dapat tampil sebagai "Bisnis terverifikasi". Tag komunitas **tidak pernah** menghasilkan status verifikasi. |

## 3. Reputation scoring

### 3.1 Evaluasi implementasi saat ini

| Temuan | Dampak | Usulan |
|---|---|---|
| Dedup hanya pada skor; dokumen duplikat tetap tersimpan (A08) | Hitungan label bisa naik | Unique index + idempotency key |
| Fallback `SUSPICIOUS` untuk satu pelapor ≥ 15 melonggarkan gerbang dua pelapor (ADR-010) | Satu laporan APK (16 poin) langsung SUSPICIOUS | Syarat `N ≥ 2` untuk SUSPICIOUS sesuai PRD; satu laporan menjadi "dalam pengamatan" |
| `trust ≤ 0` dijadikan 1; trust tanpa batas atas | Bobot anomali | Clamp `0,25 ≤ T ≤ 1,5` |
| Kategori utama bergantung iterasi map | Hasil tidak deterministik | Tie-break berdasarkan bobot lalu abjad |
| Decay dihitung saat tulis saja | Skor basi | Job `System` harian menghitung ulang |
| Android fallback memakai rumus lain | Skor lokal berbeda dari server | Hapus penghitungan skor di Android; pending report tidak memengaruhi skor |

### 3.2 Algoritma yang diusulkan

Parameter: `B = 8,0`; bobot kategori `W`: PINJOL_DC 1,5; APK_SNIFFING 2,0; IMPERSONATION 1,8; JUDOL 1,3; SOCIAL_ENG 1,8; TELEMARKETING 1,0. Decay `D(Δt) = 0,5^(Δt_hari / 90)`.

Kontribusi **satu per pelapor unik per nomor** (laporan terbaru dengan bobot tertinggi dalam jendela aktif):

```text
fungsi hitung_reputasi(nomor, sekarang):
    laporan = laporan_valid(nomor) dengan status = VALIDATED dan umur <= 365 hari
    per_pelapor = {}
    untuk setiap l dalam laporan:
        kontribusi = B * W[l.kategori] * clamp(l.trust, 0.25, 1.5) * 0.5 ** (umur_hari(l) / 90)
        per_pelapor[l.pelapor] = max(per_pelapor.get(l.pelapor, 0), kontribusi)
    S = min(100, jumlah(per_pelapor.nilai))
    N = jumlah_pelapor_unik(per_pelapor)

    jika nomor di whitelist_aktif: kembalikan CLEAN, S, "VERIFIED"
    jika ada dispute DITERIMA aktif: S = 0 (dikoreksi), tandai "DIKOREKSI"
    jika S >= 70 dan N >= 5 dan ada_pelapor_bukti_atau_moderator: DANGER_SCAM
    jika S >= 40 dan N >= 3: SPAM
    jika S >= 15 dan N >= 2: SUSPICIOUS
    selain itu: CLEAN dengan availability=NOT_FOUND atau "dalam pengamatan"
```

Catatan desain: syarat DANGER_SCAM tambahan "bukti atau konfirmasi moderator" **diusulkan** (PRD hanya mensyaratkan S ≥ 70 dan N ≥ 5); keputusan terbuka OD-03 di [17](17-KEPUTUSAN-ARSITEKTUR.md). Skor `S` selalu pecahan desimal; level tidak pernah turun dari SPAM ke CLEAN tanpa jalur whitelist/dispute atau kedaluwarsa.

### 3.3 Tabel keputusan

| S | N | Whitelist | Dispute diterima | Hasil |
|---|---|---|---|---|
| apa pun | apa pun | aktif | - | CLEAN + VERIFIED |
| apa pun | apa pun | tidak | ya | CLEAN (dikoreksi) |
| ≥ 70 | ≥ 5 | tidak | tidak | DANGER_SCAM (setelah gerbang konfirmasi) |
| ≥ 70 | < 5 | tidak | tidak | SPAM bila N ≥ 3; SUSPICIOUS bila N = 2; selain itu dalam pengamatan |
| 40 sampai < 70 | ≥ 3 | tidak | tidak | SPAM |
| 15 sampai < 40 | ≥ 2 | tidak | tidak | SUSPICIOUS |
| < 15 atau N < 2 | - | tidak | tidak | dalam pengamatan / NOT_FOUND |

### 3.4 Contoh perhitungan (dapat dijadikan unit test)

Semua laporan trust = 1,0, pelapor berbeda.

| Kasus | Masukan | Perhitungan | S | N | Hasil |
|---|---|---|---|---|---|
| U1 | 5 laporan APK_SNIFFING umur 0 hari | 5 × (8×2,0) = 80 | 80 | 5 | DANGER_SCAM (setelah gerbang konfirmasi) |
| U2 | 4 laporan IMPERSONATION umur 0 | 4 × 14,4 = 57,6 | 57,6 | 4 | SPAM |
| U3 | 3 laporan PINJOL_DC umur 90 hari | 3 × (8×1,5×0,5) = 18 | 18 | 3 | SUSPICIOUS |
| U4 | 1 laporan APK_SNIFFING umur 0 | 16 | 16 | 1 | dalam pengamatan (N < 2) |
| U5 | 2 laporan TELEMARKETING umur 0 | 2 × 8 = 16 | 16 | 2 | SUSPICIOUS |
| U6 | 1 pelapor mengirim 3 laporan APK_SNIFFING | maks per pelapor = 16 | 16 | 1 | dalam pengamatan |
| U7 | batas 14,99 vs 15 | 2 laporan, total 14,99 vs 15,00 | - | 2 | CLEAN vs SUSPICIOUS |
| U8 | 5 laporan APK umur 180 hari | 5 × (16×0,25) = 20 | 20 | 5 | SUSPICIOUS (decay) |

Verifikasi aritmetika U1 sampai U8 dilakukan pada saat implementasi melalui test tabel di backend; angka di atas dihitung manual dari rumus dan **belum dieksekusi**.

## 4. Business rules ringkas

- BR-01: tidak ada status hasil yang menyebut "aman" kecuali whitelist/bisnis terverifikasi.
- BR-02: data fixture/seed tidak pernah ditampilkan sebagai laporan nyata (`source` wajib `FIXTURE`/`SERVER`).
- BR-03: auto-block hanya pada `DANGER_SCAM`, data server ≤ 14 hari, bukan whitelist, bukan kontak tersimpan.
- BR-04: setiap perubahan level oleh manusia memiliki alasan, aktor, dan jejak audit.

