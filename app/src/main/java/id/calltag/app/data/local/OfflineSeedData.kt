package id.calltag.app.data.local

import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.local.entity.EmergencyContact
import id.calltag.app.data.remote.dto.OfficialChannelDto
import id.calltag.app.data.remote.dto.OfficialChannelLinksDto
import id.calltag.app.data.remote.dto.SpamReportItemDto

/**
 * Menyediakan data seed bawaan (Offline-First) agar Nomor Darurat Nasional (112, 110, 113, 118/119, 115, 123, 14080),
 * Damkar/Polisi/RS daerah, Pusat Lapor Resmi, serta daftar awal Top Scammer Indonesia 100% berfungsi meskipun pengguna belum pernah
 * terhubung ke internet sejak instalasi pertama.
 */
object OfflineSeedData {

    fun getInitialEmergencyContacts(): List<EmergencyContact> = listOf(
        EmergencyContact(
            code = "ID-NAT-112",
            scope = "NATIONAL",
            agencyType = "TERPADU",
            name = "Panggilan Darurat Terpadu Nasional (112)",
            shortCode = "112",
            primaryPhone = "112",
            capabilitiesSummary = "Koordinasi Darurat Terpadu Bebas Pulsa (Kebakaran, Medis, Keamanan)"
        ),
        EmergencyContact(
            code = "ID-NAT-110",
            scope = "NATIONAL",
            agencyType = "POLISI",
            name = "Kepolisian Negara Republik Indonesia (Polri 110)",
            shortCode = "110",
            primaryPhone = "110",
            capabilitiesSummary = "Laporan Tindak Kriminal, Gangguan Kamtibmas, & Kecelakaan Lalu Lintas"
        ),
        EmergencyContact(
            code = "ID-NAT-113",
            scope = "NATIONAL",
            agencyType = "DAMKAR",
            name = "Pemadam Kebakaran & Penyelamatan (Damkar 113)",
            shortCode = "113",
            primaryPhone = "113",
            capabilitiesSummary = "Pemadaman Kebakaran & Evakuasi Darurat Non-Kebakaran"
        ),
        EmergencyContact(
            code = "ID-NAT-119",
            scope = "NATIONAL",
            agencyType = "AMBULANS_RS",
            name = "Ambulans & Gawat Darurat Kemenkes (PSC 119 / 118)",
            shortCode = "119",
            primaryPhone = "119",
            secondaryPhone = "118",
            capabilitiesSummary = "Penjemputan Ambulans Gawat Darurat Medis & Informasi IGD RS"
        ),
        EmergencyContact(
            code = "ID-NAT-115",
            scope = "NATIONAL",
            agencyType = "SAR",
            name = "BASARNAS / Pencarian dan Pertolongan (115)",
            shortCode = "115",
            primaryPhone = "115",
            secondaryPhone = "+622165867510",
            capabilitiesSummary = "Evakuasi Bencana Alam, Banjir, Gempa, & Pencarian Orang Hilang"
        ),
        EmergencyContact(
            code = "ID-NAT-123",
            scope = "NATIONAL",
            agencyType = "PLN",
            name = "PLN Gangguan & Bahaya Kelistrikan (123)",
            shortCode = "123",
            primaryPhone = "123",
            secondaryPhone = "+6221123",
            capabilitiesSummary = "Korsleting Gardu/Tiang Listrik & Pemadaman Darurat Saat Banjir"
        ),
        EmergencyContact(
            code = "ID-NAT-14080",
            scope = "NATIONAL",
            agencyType = "JASA_MARGA",
            name = "Jasa Marga Darurat Jalan Tol (14080)",
            shortCode = "14080",
            primaryPhone = "14080",
            capabilitiesSummary = "Derek Resmi Jalan Tol, Patroli Jalan Raya (PJR), & Ambulans Tol"
        ),
        // Direktori Lokal Kota/Kabupaten
        EmergencyContact(
            code = "ID-JK-JKT-DAMKAR-01",
            scope = "LOCAL",
            agencyType = "DAMKAR",
            name = "Dinas Gulkarmat (Damkar) Provinsi DKI Jakarta",
            province = "DKI Jakarta",
            cityRegency = "Jakarta Pusat",
            shortCode = "112",
            primaryPhone = "+62216344215",
            secondaryPhone = "+62216344579",
            whatsappNumber = "+628990997113",
            whatsappTemplate = "Halo Posko Damkar DKI Jakarta, saya membutuhkan bantuan darurat kebakaran/evakuasi di lokasi: ",
            capabilitiesSummary = "Pemadaman Kebakaran, Evakuasi Ular/Sarang Tawon, Cincin Tersangkut",
            address = "Jl. KH. Zainul Arifin No.71, Gambir, Jakarta Pusat"
        ),
        EmergencyContact(
            code = "ID-JK-JKT-POLISI-01",
            scope = "LOCAL",
            agencyType = "POLISI",
            name = "SPKT Polda Metro Jaya & Polres Metro Jakarta Pusat",
            province = "DKI Jakarta",
            cityRegency = "Jakarta Pusat",
            shortCode = "110",
            primaryPhone = "+62215234000",
            secondaryPhone = "+62213909922",
            whatsappNumber = "+62811998110",
            whatsappTemplate = "Halo SPKT Kepolisian Jakarta, saya ingin melaporkan kejadian darurat di lokasi: ",
            capabilitiesSummary = "Patroli Cepat Kamtibmas & Laporan Polisi (LP) Kejahatan Siber",
            address = "Jl. Garut No.1, Menteng, Jakarta Pusat"
        ),
        EmergencyContact(
            code = "ID-JB-BDG-DAMKAR-01",
            scope = "LOCAL",
            agencyType = "DAMKAR",
            name = "Diskar PB (Damkar) Kota Bandung",
            province = "Jawa Barat",
            cityRegency = "Kota Bandung",
            shortCode = "113",
            primaryPhone = "+6222113",
            secondaryPhone = "+62227207113",
            whatsappNumber = "+62811207113",
            whatsappTemplate = "Halo Posko Diskar PB Kota Bandung, mohon bantuan darurat kebakaran/rescue di lokasi: ",
            capabilitiesSummary = "Pemadaman Kebakaran, Animal Rescue (Ular/Tawon), Pelepasan Cincin & Pohon Tumbang",
            address = "Jl. Sukabumi No.17, Batununggal, Kota Bandung"
        ),
        EmergencyContact(
            code = "ID-JB-BDG-POLISI-01",
            scope = "LOCAL",
            agencyType = "POLISI",
            name = "Polrestabes Bandung (SPKT & Tim Prabu)",
            province = "Jawa Barat",
            cityRegency = "Kota Bandung",
            shortCode = "110",
            primaryPhone = "+62224203500",
            secondaryPhone = "+62224244444",
            whatsappNumber = "+6282130201996",
            whatsappTemplate = "Halo Polrestabes Bandung, mohon bantuan kepolisian darurat di lokasi: ",
            capabilitiesSummary = "Patroli Cepat Kamtibmas & SPKT 24 Jam Kota Bandung",
            address = "Jl. Merdeka No.18-21, Sumur Bandung, Kota Bandung"
        ),
        EmergencyContact(
            code = "ID-JI-SBY-DAMKAR-01",
            scope = "LOCAL",
            agencyType = "DAMKAR",
            name = "DPKP Damkar & Command Center 112 Kota Surabaya",
            province = "Jawa Timur",
            cityRegency = "Kota Surabaya",
            shortCode = "112",
            primaryPhone = "+62313533843",
            secondaryPhone = "+62313533844",
            whatsappNumber = "+6281131112112",
            whatsappTemplate = "Halo Command Center 112 / Damkar Surabaya, mohon bantuan darurat di lokasi: ",
            capabilitiesSummary = "Respons Cepat Kebakaran < 7 Menit & Evakuasi Darurat Non-Kebakaran",
            address = "Jl. Pasar Turi No.21, Bubutan, Kota Surabaya"
        ),
        EmergencyContact(
            code = "ID-JT-SMG-DAMKAR-01",
            scope = "LOCAL",
            agencyType = "DAMKAR",
            name = "Dinas Pemadam Kebakaran Kota Semarang",
            province = "Jawa Tengah",
            cityRegency = "Kota Semarang",
            shortCode = "113",
            primaryPhone = "+62247605871",
            secondaryPhone = "+62247607076",
            whatsappNumber = "+628112700113",
            whatsappTemplate = "Halo Posko Damkar Kota Semarang, mohon bantuan pemadaman/evakuasi darurat di lokasi: ",
            capabilitiesSummary = "Pemadaman Kebakaran, Evakuasi Ular, Sarang Tawon, & Penyelamatan Warga",
            address = "Jl. Madukoro Raya No.6, Semarang Barat, Kota Semarang"
        ),
        EmergencyContact(
            code = "ID-YO-YGY-DAMKAR-01",
            scope = "LOCAL",
            agencyType = "DAMKAR",
            name = "Damkarmat Kota Yogyakarta",
            province = "DI Yogyakarta",
            cityRegency = "Kota Yogyakarta",
            shortCode = "113",
            primaryPhone = "+62274587101",
            whatsappNumber = "+628112700113",
            whatsappTemplate = "Halo Posko Damkarmat Kota Yogyakarta, mohon bantuan darurat di lokasi: ",
            capabilitiesSummary = "Pemadaman Kebakaran • Evakuasi Ular & Sarang Tawon • Penyelamatan Non-Kebakaran",
            address = "Jl. Kenari No.56, Umbulharjo, Kota Yogyakarta"
        ),
        EmergencyContact(
            code = "ID-JB-BDG-RS-01",
            scope = "LOCAL",
            agencyType = "AMBULANS_RS",
            name = "IGD RSUP Dr. Hasan Sadikin (RSHS) & PSC 119 Bandung",
            province = "Jawa Barat",
            cityRegency = "Kota Bandung",
            shortCode = "119",
            primaryPhone = "+62222551111",
            secondaryPhone = "+62222034953",
            whatsappNumber = "+628112442119",
            whatsappTemplate = "Halo PSC 119 / IGD RSHS Bandung, butuh ambulans darurat di lokasi: ",
            capabilitiesSummary = "IGD Rujukan Utama Jawa Barat 24 Jam • Penjemputan Ambulans Medis",
            address = "Jl. Pasteur No.38, Sukajadi, Kota Bandung"
        ),
        EmergencyContact(
            code = "ID-JK-JKT-RS-01",
            scope = "LOCAL",
            agencyType = "AMBULANS_RS",
            name = "IGD RSUPN Dr. Cipto Mangunkusumo (RSCM) & AGD DKI",
            province = "DKI Jakarta",
            cityRegency = "Jakarta Pusat",
            shortCode = "119",
            primaryPhone = "+62211500135",
            secondaryPhone = "+622165303118",
            whatsappNumber = "+628111922119",
            whatsappTemplate = "Halo AGD Dinkes DKI / IGD RSCM, butuh bantuan ambulans segera di lokasi: ",
            capabilitiesSummary = "IGD Rujukan Nasional 24 Jam • Trauma Center • Ambulans Gawat Darurat",
            address = "Jl. Pangeran Diponegoro No.71, Senen, Jakarta Pusat"
        ),
        EmergencyContact(
            code = "ID-JI-SBY-POLISI-01",
            scope = "LOCAL",
            agencyType = "POLISI",
            name = "SPKT Polrestabes Surabaya",
            province = "Jawa Timur",
            cityRegency = "Kota Surabaya",
            shortCode = "110",
            primaryPhone = "+62313523927",
            whatsappNumber = "+6281133370075",
            whatsappTemplate = "Halo SPKT Polrestabes Surabaya, saya ingin melaporkan kejadian darurat di lokasi: ",
            capabilitiesSummary = "Patroli Cepat Kamtibmas • Penerimaan Laporan Polisi (LP) & Siber",
            address = "Jl. Sikatan No.1, Krembangan, Kota Surabaya"
        ),
        EmergencyContact(
            code = "ID-JI-SBY-RS-01",
            scope = "LOCAL",
            agencyType = "AMBULANS_RS",
            name = "IGD RSUD Dr. Soetomo Surabaya & Ambulans TGC 112",
            province = "Jawa Timur",
            cityRegency = "Kota Surabaya",
            shortCode = "112",
            primaryPhone = "+62315501078",
            whatsappNumber = "+6281230257400",
            whatsappTemplate = "Halo IGD RSUD Dr. Soetomo / TGC Surabaya, butuh bantuan medis darurat di lokasi: ",
            capabilitiesSummary = "IGD & Pusat Trauma Terpadu • Tim Gerak Cepat (TGC) Ambulans Medis",
            address = "Jl. Mayjen Prof. Dr. Moestopo No.6-8, Gubeng, Kota Surabaya"
        )
    )

    fun getInitialTopScammers(): List<CachedPhoneTag> = listOf(
        CachedPhoneTag(
            phoneE164 = "+6281299887766",
            carrier = "Telkomsel (Simpati/Halo)",
            reputationLevel = "DANGER_SCAM",
            spamScore = 92.5,
            isDanger = true,
            primaryCategory = "APK_SNIFFING",
            primaryCategoryLabel = "Penipuan Modus File APK (Kurir/Undangan/Tilang)",
            topCommunityLabel = "Penipu Kurir Paket J&T File Resi.apk",
            tagsSummary = "Penipu Kurir Paket J&T File Resi.apk (14), Modus Lihat Foto Paket APK (6)",
            uniqueReporters = 24,
            syncVersion = 1L
        ),
        CachedPhoneTag(
            phoneE164 = "+6285711223344",
            carrier = "Indosat Ooredoo Hutchison (IM3)",
            reputationLevel = "DANGER_SCAM",
            spamScore = 88.0,
            isDanger = true,
            primaryCategory = "PINJOL_DC",
            primaryCategoryLabel = "Pinjol Ilegal & Debt Collector Meresahkan",
            topCommunityLabel = "DC Pinjol Ilegal Sebar Data & Maki-maki",
            tagsSummary = "DC Pinjol Ilegal Sebar Data & Maki-maki (12), Teror Pinjol Tidak Berizin OJK (7)",
            uniqueReporters = 19,
            syncVersion = 2L
        ),
        CachedPhoneTag(
            phoneE164 = "+6281377665544",
            carrier = "Telkomsel (Simpati/Halo)",
            reputationLevel = "DANGER_SCAM",
            spamScore = 84.2,
            isDanger = true,
            primaryCategory = "IMPERSONATION",
            primaryCategoryLabel = "Impersonasi / CS Palsu (Bank/E-Wallet/Pajak)",
            topCommunityLabel = "Penipu CS Bank BCA Palsu (Modus Tarif Rp150rb/bln)",
            tagsSummary = "Penipu CS Bank BCA Palsu (10), Minta OTP & PIN M-Banking (5)",
            uniqueReporters = 15,
            syncVersion = 3L
        ),
        CachedPhoneTag(
            phoneE164 = "+6289655443322",
            carrier = "Indosat Ooredoo Hutchison (Tri/3)",
            reputationLevel = "SPAM",
            spamScore = 56.4,
            isDanger = true,
            primaryCategory = "JUDOL",
            primaryCategoryLabel = "Promosi Judi Online (Judol)",
            topCommunityLabel = "Spam Robocall & Blast WA Situs Slot Judol",
            tagsSummary = "Spam Robocall & Blast WA Situs Slot Judol (6), Promosi Judi Online (3)",
            uniqueReporters = 9,
            syncVersion = 4L
        ),
        CachedPhoneTag(
            phoneE164 = "+6287812349876",
            carrier = "XL Axiata",
            reputationLevel = "DANGER_SCAM",
            spamScore = 79.6,
            isDanger = true,
            primaryCategory = "SOCIAL_ENG",
            primaryCategoryLabel = "Penipuan Sosial / Hipnotis Telepon",
            topCommunityLabel = "Penipu Modus Anak Kecelakaan Masuk IGD RS",
            tagsSummary = "Penipu Modus Anak Kecelakaan Masuk IGD RS (8), Minta Transfer Uang Operasi (4)",
            uniqueReporters = 12,
            syncVersion = 5L
        ),
        CachedPhoneTag(
            phoneE164 = "+6282144556677",
            carrier = "Telkomsel (Simpati)",
            reputationLevel = "SUSPICIOUS",
            spamScore = 26.5,
            isDanger = false,
            primaryCategory = "TELEMARKETING",
            primaryCategoryLabel = "Telemarketing / Spam Promosi Agresif",
            topCommunityLabel = "Sales Asuransi & KTA Jam Istirahat",
            tagsSummary = "Sales Asuransi & KTA Jam Istirahat (3)",
            uniqueReporters = 3,
            syncVersion = 6L
        )
    )

    fun getInitialScreeningLogs(): List<CallScreeningLog> = listOf(
        CallScreeningLog(
            phoneE164 = "+6281299887766",
            tagLabel = "Penipu Kurir Paket J&T File Resi.apk",
            categoryCode = "APK_SNIFFING",
            reputationLevel = "DANGER_SCAM",
            spamScore = 92.5,
            wasBlocked = true,
            detectionSource = "LOCAL_CACHE",
            screenedAtEpochMs = System.currentTimeMillis() - 18 * 60 * 1000L
        ),
        CallScreeningLog(
            phoneE164 = "+6285711223344",
            tagLabel = "DC Pinjol Ilegal Sebar Data & Maki-maki",
            categoryCode = "PINJOL_DC",
            reputationLevel = "DANGER_SCAM",
            spamScore = 88.0,
            wasBlocked = true,
            detectionSource = "LOCAL_CACHE",
            screenedAtEpochMs = System.currentTimeMillis() - 95 * 60 * 1000L
        ),
        CallScreeningLog(
            phoneE164 = "+6289655443322",
            tagLabel = "Spam Robocall & Blast WA Situs Slot Judol",
            categoryCode = "JUDOL",
            reputationLevel = "SPAM",
            spamScore = 56.4,
            wasBlocked = false,
            detectionSource = "LIVE_API",
            screenedAtEpochMs = System.currentTimeMillis() - 240 * 60 * 1000L
        ),
        CallScreeningLog(
            phoneE164 = "+6282144556677",
            tagLabel = "Sales Asuransi & KTA Jam Istirahat",
            categoryCode = "TELEMARKETING",
            reputationLevel = "SUSPICIOUS",
            spamScore = 26.5,
            wasBlocked = false,
            detectionSource = "LOCAL_CACHE",
            screenedAtEpochMs = System.currentTimeMillis() - 420 * 60 * 1000L
        )
    )

    fun getInitialOfficialChannels(): List<OfficialChannelDto> = listOf(
        OfficialChannelDto(
            channelSlug = "aduannomor-komdigi",
            title = "AduanNomor.id (Kementerian Komdigi)",
            authorityName = "Kementerian Komunikasi dan Digital RI",
            relatedCategories = listOf("APK_SNIFFING", "IMPERSONATION", "JUDOL", "SOCIAL_ENG", "TELEMARKETING"),
            description = "Lapor nomor seluler (MSISDN) penipu atau pengirim file APK berbahaya agar diblokir permanen oleh seluruh operator seluler Indonesia.",
            actionLinks = OfficialChannelLinksDto(
                websiteUrl = "https://aduannomor.id",
                email = "kontak@aduannomor.id"
            ),
            requiredEvidenceChecklist = listOf(
                "Nomor telepon pelaku terlihat jelas (format 08xx / +628xx)",
                "Tangkapan layar (screenshot) riwayat panggilan atau pesan WhatsApp/SMS",
                "Kronologi singkat waktu kejadian dan modus pelaku"
            ),
            stepByStepGuide = listOf(
                "Gunakan tombol 'Salin Nomor Pelaku' di bawah untuk menyalin nomor ke clipboard.",
                "Buka portal AduanNomor.id dan pilih kategori pelaporan Penipuan atau Spam.",
                "Unggah tangkapan layar bukti panggilan/chat untuk verifikasi pemblokiran MSISDN."
            )
        ),
        OfficialChannelDto(
            channelSlug = "cekrekening-iasc",
            title = "CekRekening.id & IASC (Indonesia Anti-Scam Centre)",
            authorityName = "Kementerian Komdigi & Otoritas Jasa Keuangan (OJK)",
            relatedCategories = listOf("IMPERSONATION", "APK_SNIFFING", "SOCIAL_ENG"),
            description = "Cek dan laporkan nomor rekening bank atau akun E-Wallet penipu untuk mempercepat pembekuan dana hasil kejahatan keuangan digital.",
            actionLinks = OfficialChannelLinksDto(
                websiteUrl = "https://iasc.ojk.go.id",
                phoneDial = "157"
            ),
            requiredEvidenceChecklist = listOf(
                "Nama bank atau penyedia E-Wallet beserta nomor rekening tujuan pelaku",
                "Bukti struk transfer atau mutasi rekening korban",
                "Tangkapan layar percakapan atau log telepon yang mengarahkan transfer"
            ),
            stepByStepGuide = listOf(
                "Segera hubungi Call Center bank Anda untuk meminta pemblokiran darurat.",
                "Laporkan rekening pelaku ke portal IASC (iasc.ojk.go.id) serta CekRekening.id (cekrekening.id).",
                "Simpan tanda terima laporan elektronik sebagai lampiran Laporan Polisi (LP)."
            )
        ),
        OfficialChannelDto(
            channelSlug = "satgas-pasti-ojk",
            title = "Satgas PASTI / Kontak OJK 157",
            authorityName = "Satuan Tugas Pemberantasan Aktivitas Keuangan Ilegal (OJK)",
            relatedCategories = listOf("PINJOL_DC", "IMPERSONATION"),
            description = "Kanal resmi pengaduan teror Debt Collector Pinjaman Online (Pinjol) ilegal, penyebaran data kontak darurat, dan investasi bodong.",
            actionLinks = OfficialChannelLinksDto(
                websiteUrl = "https://sipasti.ojk.go.id",
                phoneDial = "157",
                whatsappUrl = "https://wa.me/6281157157157",
                email = "waspadainvestasi@ojk.go.id"
            ),
            requiredEvidenceChecklist = listOf(
                "Nama aplikasi atau entitas Pinjol Ilegal / Investasi Bodong",
                "Daftar nomor telepon atau WhatsApp penagih utang (Debt Collector)",
                "Tangkapan layar ancaman intimidasi atau penyebaran data pribadi"
            ),
            stepByStepGuide = listOf(
                "Jangan melakukan pembayaran ke rekening pribadi penagih Pinjol Ilegal.",
                "Kirim aduan langsung ke WhatsApp resmi Kontak OJK 0811-5715-7157 atau portal sipasti.ojk.go.id.",
                "Aktifkan fitur Auto-Block DANGER_SCAM di tab Spam Guard CallTag."
            )
        ),
        OfficialChannelDto(
            channelSlug = "patroli-siber-polri",
            title = "Patroli Siber Polri (Dittipidsiber Bareskrim)",
            authorityName = "Kepolisian Negara Republik Indonesia (Polri)",
            relatedCategories = listOf("APK_SNIFFING", "IMPERSONATION", "PINJOL_DC", "JUDOL", "SOCIAL_ENG"),
            description = "Pelaporan tindak pidana siber termasuk peretasan perangkat via file APK, penipuan daring, pemerasan, dan pengancaman.",
            actionLinks = OfficialChannelLinksDto(
                websiteUrl = "https://patrolisiber.id",
                phoneDial = "110"
            ),
            requiredEvidenceChecklist = listOf(
                "Kartu identitas pelapor (KTP)",
                "Cetakan bukti transfer bank, tangkapan layar chat, dan nomor telepon pelaku",
                "File atau tautan berbahaya yang dikirim pelaku (jangan dibuka/diinstal)"
            ),
            stepByStepGuide = listOf(
                "Kumpulkan seluruh bukti digital tanpa menghapus riwayat chat asli.",
                "Buat aduan melalui Patroli Siber Polri atau datangi SPKT Polres terdekat untuk Laporan Polisi (LP).",
                "Gunakan surat tanda terima LP untuk pengajuan pemblokiran rekening pelaku di bank."
            )
        ),
        OfficialChannelDto(
            channelSlug = "lapor-go-id",
            title = "LAPOR! (lapor.go.id)",
            authorityName = "Kementerian PANRB, Ombudsman RI, & Kantor Staf Presiden",
            relatedCategories = listOf("IMPERSONATION", "TELEMARKETING"),
            description = "Sistem Pengelolaan Pengaduan Pelayanan Publik Nasional (SP4N) untuk pengaduan layanan publik dan pencatutan nama instansi pemerintah.",
            actionLinks = OfficialChannelLinksDto(
                websiteUrl = "https://www.lapor.go.id"
            ),
            requiredEvidenceChecklist = listOf(
                "Kronologi kejadian beserta lokasi dan waktu layanan",
                "Nama instansi terkait dan dokumen/foto bukti pendukung"
            ),
            stepByStepGuide = listOf(
                "Buka portal lapor.go.id dan pilih klasifikasi laporan Pengaduan.",
                "Tulis judul, isi laporan, tanggal, serta instansi tujuan secara jelas.",
                "Lampirkan bukti pendukung dan pantau tindak lanjut menggunakan nomor tiket."
            )
        )
    )

    fun getOfflineBreakdownForPhone(phoneTag: CachedPhoneTag): Map<String, Int> {
        val allCats = listOf("PINJOL_DC", "APK_SNIFFING", "IMPERSONATION", "JUDOL", "SOCIAL_ENG", "TELEMARKETING")
        val primaryCount = (phoneTag.uniqueReporters - 2).coerceAtLeast(1)
        val secondaryCount = (phoneTag.uniqueReporters - primaryCount).coerceAtLeast(0)
        return allCats.associateWith { cat ->
            when {
                cat == phoneTag.primaryCategory -> primaryCount
                secondaryCount > 0 && cat == "IMPERSONATION" && phoneTag.primaryCategory != "IMPERSONATION" -> secondaryCount
                else -> 0
            }
        }
    }

    fun getOfflineCommentsForPhone(phoneTag: CachedPhoneTag): List<SpamReportItemDto> {
        return when (phoneTag.phoneE164) {
            "+6281299887766" -> listOf(
                SpamReportItemDto(
                    phoneE164 = phoneTag.phoneE164,
                    categoryCode = "APK_SNIFFING",
                    tagName = "Penipu Kurir Paket J&T File Resi.apk",
                    comment = "Mengirim file Lihat_Foto_Paket.apk via WhatsApp dan mendesak agar dibuka karena alamat paket disebut luntur.",
                    callType = "WHATSAPP_CHAT",
                    createdAt = "2026-11-05 10:05 WIB"
                ),
                SpamReportItemDto(
                    phoneE164 = phoneTag.phoneE164,
                    categoryCode = "APK_SNIFFING",
                    tagName = "Modus Lihat Foto Paket APK",
                    comment = "Menelepon dulu mengaku kurir ekspedisi, lalu kirim file ekstensi .apk berbahaya.",
                    callType = "WHATSAPP_CALL",
                    createdAt = "2026-11-04 16:40 WIB"
                )
            )
            "+6285711223344" -> listOf(
                SpamReportItemDto(
                    phoneE164 = phoneTag.phoneE164,
                    categoryCode = "PINJOL_DC",
                    tagName = "DC Pinjol Ilegal Sebar Data & Maki-maki",
                    comment = "Menelepon berulang kali dengan kata kasar padahal saya tidak pernah meminjam, mengancam sebar foto ke kontak.",
                    callType = "CELLULAR_CALL",
                    createdAt = "2026-11-05 11:20 WIB"
                )
            )
            "+6281377665544" -> listOf(
                SpamReportItemDto(
                    phoneE164 = phoneTag.phoneE164,
                    categoryCode = "IMPERSONATION",
                    tagName = "Penipu CS Bank BCA Palsu (Modus Tarif Rp150rb/bln)",
                    comment = "Mengaku CS Halo BCA menginformasikan perubahan tarif transfer BI-Fast menjadi Rp150.000/bulan dan meminta OTP.",
                    callType = "CELLULAR_CALL",
                    createdAt = "2026-11-05 09:15 WIB"
                )
            )
            else -> listOf(
                SpamReportItemDto(
                    phoneE164 = phoneTag.phoneE164,
                    categoryCode = phoneTag.primaryCategory,
                    tagName = phoneTag.topCommunityLabel,
                    comment = "Dilaporkan oleh ${phoneTag.uniqueReporters} warga komunitas CallTag sebagai ${phoneTag.primaryCategoryLabel}.",
                    callType = "CELLULAR_CALL",
                    createdAt = "2026-11-05 WIB"
                )
            )
        }
    }
}
