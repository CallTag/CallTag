# Data dan sumber Tahap 4B CallTag

Paket ini menyalin data yang dipakai proyek. Pemeriksaan sumber awal tercatat pada 16 November 2026.

| Data | Isi paket | Sumber asal |
| --- | --- | --- |
| Nomor layanan resmi | [CSV 18 entri](nomor-resmi.csv) dan [JSON backend](verified_numbers_seed.json). Kolom `source_url` menunjuk halaman atau dokumen resmi tiap nomor. | [Catatan kurasi](VERIFIED-SOURCES.md) dan [contoh laman Halo BCA](https://www.bca.co.id/id/halobca). |
| Prefix seluler Indonesia | [CSV 40 prefix](prefix-seluler.csv), diekspor dari tabel lookup CallTag. | [Daftar rujukan Komdigi dan operator](PREFIX_SOURCES.md). Label merek dapat mencerminkan alokasi historis. |
| Kode wilayah telepon tetap | [CSV 354 kode](kode-wilayah-pstn.csv), diekspor dari tabel lookup CallTag. | [PDF asli Permenkominfo 14/2018](sumber-asli/Permenkominfo-14-2018-FTP.pdf), Lampiran Bab II, Tabel II.2–II.8, halaman cetak 21–27. |
| Kode negara | [CSV 54 kode yang dipakai CallTag](kode-negara.csv). | [Daftar penomoran nasional ITU](https://www.itu.int/oth/t0202). |
| Sampel eksternal untuk uji importer | [Dua entri CallShield](callshield_sample.json), [lisensi repositori](CallShield-LICENSE), dan [atribusi](CALLSHIELD-SUMBER.md). | [Berkas JSON asli lengkap pada commit tetap](https://raw.githubusercontent.com/SysAdminDoc/CallShield/e4021815a10fb20a540994d53f98ba6a1ca839cc/data/spam_numbers.json). |

Untuk membandingkan nomor resmi, buka URL pada baris yang dipilih dan cari digit nomor pada halaman sumber. Nomor disimpan dengan format yang dipakai backend. Saat membuka CSV di Excel, impor kolom nomor dan prefix sebagai **Teks** agar awalan `+` dan `0` tetap terlihat.

JSON nomor resmi adalah hasil kurasi situs instansi. CSV prefix adalah salinan dari tabel lookup proyek. [Fixture 453 kasus](carrier_context.json) berisi contoh pengujian terhadap tabel tersebut. Prefix menunjukkan alokasi penomoran dan tidak mengidentifikasi pemilik nomor.

Kedua entri fixture CallShield dibandingkan kembali dengan berkas upstream dan cocok persis. Hash SHA-256 berkas upstream cocok dengan yang dicatat saat implementasi. Label spam upstream merupakan laporan eksternal untuk pengujian parser, tanpa konfirmasi kebenaran atau kepemilikan nomor. Berkas lengkap tersedia melalui tautan sumber agar atribusi dan konteks tiap entri dapat diperiksa.

Tokopedia Care WhatsApp tidak ada dalam seed karena nomor yang dapat dikonfirmasi lewat sumber resmi belum ditemukan saat kurasi.
