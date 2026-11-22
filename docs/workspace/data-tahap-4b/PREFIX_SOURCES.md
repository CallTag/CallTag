# Sumber prefix T-208

Diperiksa 16 November 2026. Tabel lokal ada di `helper/carrier.go`; tidak ada panggilan API saat lookup. Informasi ini menunjukkan alokasi penomoran, bukan identitas, lokasi aktual, status aktif, hasil kueri HLR langsung, atau reputasi penelepon. Label merek lama dipertahankan sebagai konteks historis keluarga nomor.

| Cakupan | Sumber primer | Penggunaan |
| --- | --- | --- |
| Wilayah PSTN | [Permenkominfo 14/2018, FTP Nasional](https://jdih.komdigi.go.id/produk_hukum/unduh/id/620/t/peraturan%2Bmenteri%2Bkomunikasi%2Bdan%2Binformatika%2Bnomor%2B14%2Btahun%2B2018%2Btanggal%2B2%2Boktober%2B2018), Lampiran Bab II, Tabel II.2–II.8, halaman tercetak 21–27 | Seluruh sel alokasi terisi pada tujuh tabel; digit C dibaca dalam urutan 1–9 lalu 0. Kode dua digit 21/22/24/31/61 dibedakan dari kode tiga digit. Nol trunk tidak disimpan. Makassar memakai nama modern untuk Ujung Pandang. |
| Seluler nasional | [Data Penyelenggaraan Pos dan Informatika 2023, Tabel 3.3](https://djppi.komdigi.go.id/storage/file/file_publication/other/ADO%202023_ver.1_update29Jan24_final_TTE.pdf) | Daftar NDC pemerintah sebagai rujukan alokasi operator; indeks pencarian masih memuat tabel meski unduhan langsung saat pemeriksaan tidak tersedia. |
| Telkomsel | [Penjelasan prefix Telkomsel](https://www.telkomsel.com/jelajah/jelajah-lifestyle/ketahui-perbedaan-kode-area-telkomsel-di-nomor-hp-yuk) | Halo, Simpati, AS/Loop dan keluarga 0851 (by.U/AS). Prefix tidak memastikan produk pelanggan saat ini. |
| Indosat | [Dokumen interkoneksi Indosat](https://ioh.co.id/dataprod/portalcontent/portal/images/products/pdf/637403713615140419.pdf), skenario TE.M02 | Menegaskan 0814–0816 dan 0855–0858. |
| XL/AXIS | [Dokumen Pendukung A XL](https://staticxl.ext.xlaxiata.co.id/s3fs-public/media/documents/DokumenPendukungA.pdf), halaman 35; [penjelasan XL](https://www.xl.co.id/blog/0877-itu-kartu-apa) | Sepuluh NDC XL/AXIS; sumber terbaru menyebut merek berada dalam ekosistem XLSMART. |
| Smartfren | [Ketentuan penggunaan Smartfren](https://www.smartfren.com/terms-of-use/) | Menegaskan merek XLSMART dan prefix utama; alokasi historis 0881–0889 yang telah ada di aplikasi tetap dikenali. |
| Negara/wilayah | [ITU National Numbering Plans](https://www.itu.int/oth/t0202) | Subset kode geografis umum, termasuk seluruh negara Asia Tenggara, Jepang, Korea, Tiongkok, dan tujuan utama lain; kode belum tercantum mendapat label internasional tidak diketahui. +1 dipakai bersama NANP dan +7 oleh Rusia/Kazakhstan. |

Tabel PSTN memakai label `PSTN Indonesia`, karena kode wilayah sendiri tidak membuktikan operator Telkom. Nomor internasional selalu disertai penjelasan bahwa kode negara bukan bukti spam; tidak mengubah skor atau status reputasi. Nomor kosong/tidak dikenal tidak diberi label darurat. Kode darurat dipilih eksplisit, sedangkan nomor layanan `150xxxx` diberi konteks layanan nasional.

Normalisasi mempertahankan awalan `+` eksplisit sebelum aturan domestik `8xx`, sehingga `+855…`, `+81…`, dan `+82…` tidak diubah menjadi `+628…`. Nomor layanan nasional `150xxxx` dipertahankan tanpa kode negara sesuai format panggilan resmi; contoh [Halo BCA](https://www.bca.co.id/id/Individu/layanan/e-banking/Halo-BCA) `1500888`.
