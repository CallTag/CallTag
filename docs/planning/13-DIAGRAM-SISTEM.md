# 13. Katalog Lengkap Diagram Sistem CallTag (17 Diagram Arsitektur & Alur)

| Metadata | Nilai |
|---|---|
| Status | **DISETUJUI (14 November 2026)** sesuai keputusan [17-KEPUTUSAN-ARSITEKTUR](17-KEPUTUSAN-ARSITEKTUR.md) |
| Cakupan | 17 diagram sistem target (Mermaid) mencakup konteks, arsitektur, komponen, RBAC, autentikasi, lookup, screening, pelaporan, moderasi, database, sinkronisasi, admin, website, deployment, dan CI/CD |
| Baseline AS-IS | Diagram kondisi awal kode sebelum perubahan tetap tersimpan di [SYSTEM_ARCHITECTURE](../SYSTEM_ARCHITECTURE.md) dan [FLOWS_AND_DIAGRAMS](../FLOWS_AND_DIAGRAMS.md) |

---

## Daftar Isi 17 Diagram Sistem

1. [Diagram Konteks Sistem (System Context Diagram)](#1-diagram-konteks-sistem-system-context-diagram)
2. [Arsitektur Sistem Tingkat Tinggi (High-Level System Architecture)](#2-arsitektur-sistem-tingkat-tinggi-high-level-system-architecture)
3. [Diagram Komponen Android (Android Component Diagram)](#3-diagram-komponen-android-android-component-diagram)
4. [Diagram Komponen Backend (Backend Component Diagram)](#4-diagram-komponen-backend-backend-component-diagram)
5. [Diagram Peran dan Hak Akses (Role and Permission Diagram)](#5-diagram-peran-dan-hak-akses-role-and-permission-diagram)
6. [Diagram Alir Autentikasi (Authentication Flowchart)](#6-diagram-alir-autentikasi-authentication-flowchart)
7. [Diagram Alir Pencarian Nomor (Phone Lookup Flowchart)](#7-diagram-alir-pencarian-nomor-phone-lookup-flowchart)
8. [Diagram Urutan Pencarian Nomor (Phone Lookup Sequence Diagram)](#8-diagram-urutan-pencarian-nomor-phone-lookup-sequence-diagram)
9. [Diagram Alir Penyaringan Panggilan Masuk (Incoming Call Screening Flowchart)](#9-diagram-alir-penyaringan-panggilan-masuk-incoming-call-screening-flowchart)
10. [Diagram Alir Pelaporan Komunitas (Community Reporting Flowchart)](#10-diagram-alir-pelaporan-komunitas-community-reporting-flowchart)
11. [Diagram Alir Sanggahan dan Moderasi (Dispute and Moderation Flowchart)](#11-diagram-alir-sanggahan-dan-moderasi-dispute-and-moderation-flowchart)
12. [Diagram Hubungan Entitas Basis Data (Database ER Diagram)](#12-diagram-hubungan-entitas-basis-data-database-er-diagram)
13. [Diagram Sinkronisasi Offline (Offline Synchronisation Diagram)](#13-diagram-sinkronisasi-offline-offline-synchronisation-diagram)
14. [Diagram Alur Kerja Admin (Admin Workflow Diagram)](#14-diagram-alur-kerja-admin-admin-workflow-diagram)
15. [Diagram Integrasi Website (Website Integration Diagram)](#15-diagram-integrasi-website-website-integration-diagram)
16. [Arsitektur Deployment (Deployment Architecture)](#16-arsitektur-deployment-deployment-architecture)
17. [Diagram Alur CI/CD (CI/CD Diagram)](#17-diagram-alur-cicd-cicd-diagram)

---

## 1. Diagram Konteks Sistem (System Context Diagram)

Menggambarkan batas ekosistem CallTag, aktor eksternal (Pengguna Android, Pengunjung Web, Pemilik Nomor, Moderator/Admin), sistem operasi Android Telecom, serta layanan instansi/darurat eksternal.

```mermaid
flowchart LR
    W["Warga Pengguna Android"] -->|"Cari nomor, lapor scam, akses darurat"| APP["Aplikasi Android CallTag"]
    OWN["Pemilik Nomor (Guest / User)"] -->|"Ajukan sanggahan (dispute) & lacak status"| APP
    OWN -->|"Ajukan sanggahan via Web"| WEB["Portal Web CallTag"]
    V["Pengunjung Publik"] -->|"Cek reputasi nomor & baca panduan"| WEB
    ST["Staf Moderator & Admin"] -->|"Tinjau laporan, dispute, kurasi data"| ADM["Panel Admin Internal (/admin)"]
    TEL["Android Telecom OS"] -->|"Callback panggilan masuk (< 5 detik)"| APP
    APP -->|"Izinkan atau tolak panggilan"| TEL
    APP -->|"HTTPS / REST JSON"| API["Backend Go CallTag"]
    WEB -->|"HTTPS / CORS Terdaftar"| API
    ADM -->|"Same-Origin Session + TOTP"| API
    API <-->|"Driver Resmi MongoDB"| DB[("MongoDB Database")]
    APP -->|"Intent ACTION_DIAL / Browser"| EXT["Nomor Darurat 112/113 & Kanal Resmi"]
    WEB -->|"Tautan Resmi"| EXT
```

**Penjelasan:** Aplikasi Android dan Portal Web berkomunikasi dengan Backend Go melalui REST API. Panggilan ke nomor darurat dan kanal pelaporan pemerintah dilakukan melalui *Intent* telepon/peramban perangkat, tanpa mengunggah buku kontak pengguna.

---

## 2. Arsitektur Sistem Tingkat Tinggi (High-Level System Architecture)

Menunjukkan hubungan antarkontainer pada empat repositori (`CallTag`, `backend`, `calltag.github.io`, `apidocs`) dan penyimpanan data lokal maupun server.

```mermaid
flowchart TD
    subgraph Klien["Lapisan Klien"]
        A1["Android App (Kotlin, Compose, Room v2, WorkManager)"]
        A2["Website Publik (calltag.github.io)"]
        A3["Dokumentasi Interaktif (apidocs Swagger UI)"]
    end

    subgraph Server["Lapisan Backend (Modular Monolith Go)"]
        RP["Reverse Proxy / Lapisan HTTP"]
        MW["Middleware: RequestID, CORS, RateLimit, Authn, RBAC"]
        API["REST API Handler (/api/v1/*)"]
        ADM["Panel Admin Tertanam (/admin/*)"]
        SVC["Lapisan Service: Lookup, Report, Dispute, Scoring, Sync"]
        JOB["Job Terjadwal: Decay Skor, Retensi, Rekonsiliasi"]
    end

    subgraph Penyimpanan["Lapisan Penyimpanan Data"]
        ROOM[("SQLite Room v2: Cache, Outbox, SyncState")]
        MONGO[("MongoDB: 11 Koleksi Operasional & Audit")]
        BK[("Arsip Backup Terenkripsi")]
    end

    A1 <-->|"Baca/Tulis Lokal"| ROOM
    A1 -->|"Bearer JWT + Idempotency-Key"| RP
    A2 -->|"Public Fetch"| RP
    A3 -->|"Live / Mock Mode"| RP
    RP --> MW
    MW --> API
    MW --> ADM
    API --> SVC
    ADM --> SVC
    JOB --> SVC
    SVC <--> MONGO
    MONGO --> BK
```

**Penjelasan:** Arsitektur mempertahankan pendekatan *modular monolith* pada satu proses Go untuk kemudahan operasional tim 2 orang, dilengkapi *Room v2* di sisi Android sebagai penyangga *offline-first*.

---

## 3. Diagram Komponen Android (Android Component Diagram)

Merinci arsitektur internal aplikasi Android di repositori `CallTag/CallTag` dengan pemisahan *UI Layer*, *Domain/Policy*, *Repository*, *WorkManager*, dan *Local/Remote Data Sources*.

```mermaid
flowchart TD
    subgraph UI["Lapisan Antarmuka (Jetpack Compose)"]
        SC["MainScaffold (4 Tab: Cari, Darurat, Beri #Tag, Proteksi)"]
        OV["CallerIdNotifier (Heads-Up Notification & Window Overlay)"]
        VM["CallTagMainViewModel (StateFlow & UiLoadState)"]
    end

    subgraph Service["Layanan Sistem & Latar Belakang"]
        CS["CallTagScreeningService (CallScreeningService)"]
        WM["WorkManager: SyncWorker & PendingReportWorker"]
    end

    subgraph Domain["Lapisan Kebijakan & Utilitas"]
        POL["ScreeningPolicy (Aturan Auto-Block Konservatif)"]
        NRM["PhoneNormalizer (Normalisasi E.164)"]
        RPM["RolePermissionManager"]
    end

    subgraph Data["Lapisan Data & Repository"]
        LR["LookupRepository"]
        RR["ReportRepository (Laporan & Dispute)"]
        SR["SyncRepository"]
        AR["AuthRepository"]
        DAO[("Room v2 DAO: CachedPhoneTag, PendingReports, SyncState, Emergency")]
        PREF["CallTagPreferences & SecureTokenStore"]
        NET["Retrofit: StandardClient (8s) & FastScreeningClient (800ms)"]
        AI["AuthInterceptor (Bearer Token & Refresh)"]
    end

    SC --> VM
    VM --> LR
    VM --> RR
    VM --> SR
    VM --> AR
    CS --> NRM
    CS --> LR
    CS --> POL
    CS --> OV
    WM --> RR
    WM --> SR
    LR --> DAO
    LR --> NET
    RR --> DAO
    RR --> NET
    SR --> DAO
    SR --> NET
    AR --> PREF
    AR --> NET
    NET --> AI
    AI --> PREF
```

**Penjelasan:** Jalur panggilan masuk (`CallTagScreeningService`) wajib melewati `FastScreeningClient` (batas waktu 800 ms) dan `ScreeningPolicy` agar keputusan kembali ke sistem operasi dalam waktu di bawah 5 detik.

---

## 4. Diagram Komponen Backend (Backend Component Diagram)

Menggambarkan lapisan internal server Go di repositori `CallTag/backend` sesuai keputusan ADR-021.

```mermaid
flowchart LR
    subgraph HTTP["Lapisan Transport HTTP"]
        MUX["Router net/http (ServeMux)"]
        MW1["RequestID & Panic Recovery"]
        MW2["CORS & Body Size Limit (16 KB)"]
        MW3["Token Bucket Rate Limiter"]
        MW4["Authn JWT / Session & RBAC Middleware"]
    end

    subgraph Handler["Lapisan Controller"]
        HC["Health & Readiness (/healthz, /readyz)"]
        AC["AuthController (/auth/device, /auth/refresh)"]
        LC["LookupController (/phones/*)"]
        RC["ReportDisputeController (/reports, /disputes)"]
        EC["EmergencyHubController (/emergency/*, /official-channels)"]
        SC["SyncStatsController (/sync/changes, /stats/public)"]
        ADC["AdminController (/admin/*)"]
    end

    subgraph Bisnis["Lapisan Service"]
        LSV["LookupService"]
        RSV["ReportService & PII Filter"]
        DSV["DisputeModerationService"]
        SSV["ScoringService (Decay & Threshold)"]
        YSV["SyncJournalService"]
        ASV["AuditLogService"]
    end

    subgraph Repo["Lapisan Repository"]
        MREP["MongoRepository (Idempotent Upsert & Reconcile)"]
        MEM["DevMemoryRepository (Hanya APP_ENV=dev)"]
    end

    MUX --> MW1 --> MW2 --> MW3 --> MW4
    MW4 --> HC & AC & LC & RC & EC & SC & ADC
    LC --> LSV
    RC --> RSV & DSV
    SC --> YSV
    ADC --> DSV & YSV & ASV
    RSV --> SSV & YSV
    DSV --> SSV & YSV & ASV
    LSV & RSV & DSV & YSV & ASV --> MREP
    MREP -. "Fallback khusus dev" .-> MEM
```

**Penjelasan:** Semua aksi penulisan reputasi dan moderasi terpusat di lapisan `Service` yang secara konsisten memperbarui agregat `phone_tags`, menulis jurnal `sync_changes`, dan mencatat `audit_logs`.

---

## 5. Diagram Peran dan Hak Akses (Role and Permission Diagram)

Memvisualisasikan 4 role aktif pada MVP (`Guest`, `Registered User`, `Staf Moderator/Admin`, `System Service`) dan 1 role yang ditunda ke P2 (`Verified Phone Owner`) sesuai OD-01.

```mermaid
flowchart TD
    subgraph Peran["Hierarki Peran (RBAC CallTag)"]
        G["Guest (Tanpa Token)"]
        U["Registered User (Akun Perangkat JWT)"]
        V["Verified Phone Owner (Ditunda ke P2)"]
        M["Moderator (Sesi Staf)"]
        A["Administrator (Sesi Staf + TOTP)"]
        SYS["System Service (Internal Worker)"]
    end

    subgraph Hak["Kelompok Hak Akses"]
        P1["Lookup Nomor, Baca Tag Teredaksi, Direktori Darurat, Ajukan Dispute (Kuota Ketat)"]
        P2["Kirim Laporan Scam, Konfirmasi Tag, Lihat Riwayat Pribadi, Hapus Akun"]
        P3["Klaim Kepemilikan Nomor dengan Bukti OTP/WhatsApp (P2)"]
        P4["Tinjau Laporan Tertahan, Putuskan Dispute, Konfirmasi Gerbang DANGER_SCAM"]
        P5["Kelola Whitelist/Bisnis, CRUD Direktori Darurat, Baca Audit Log, Kelola Staf"]
        P6["Jalankan Decay Skor Harian, Kedaluwarsa 12 Bulan, Rekonsiliasi Agregat"]
    end

    G --> P1
    U --> P1
    U --> P2
    U -. "P2" .-> V
    V -.-> P3
    M --> P4
    A --> P4
    A --> P5
    SYS --> P6
```

**Penjelasan:** `Guest` tidak dapat mengirim laporan spam (mencegah manipulasi skor anonim), namun tetap dapat melakukan pencarian nomor, mengakses nomor darurat, dan mengajukan sanggahan (*dispute*) dengan batas kuota ketat (OD-10).

---

## 6. Diagram Alir Autentikasi (Authentication Flowchart)

Menjelaskan alur pendaftaran perangkat tanpa nomor telepon (OD-02), verifikasi token pada request API, serta login staf dengan TOTP.

```mermaid
flowchart TD
    START(["Permintaan Masuk ke Backend"]) --> IS_ADM{"Jalur /admin/* ?"}

    IS_ADM -- "Ya" --> CHK_COOKIE{"Sesi Cookie Staf & TOTP Valid?"}
    CHK_COOKIE -- "Tidak" --> E401_ADM["401 / Redirect ke /admin/login"]
    CHK_COOKIE -- "Ya" --> CHK_ROLE_ADM{"Role MOD atau ADMIN Sesuai?"}
    CHK_ROLE_ADM -- "Tidak" --> E403_ADM["403 Forbidden"]
    CHK_ROLE_ADM -- "Ya" --> OK_ADM["Izinkan Akses & Catat Audit Log bila Mutasi"]

    IS_ADM -- "Tidak (API /api/v1/*)" --> HAS_HDR{"Ada Header Authorization Bearer?"}
    HAS_HDR -- "Tidak Ada" --> REQ_AUTH{"Endpoint Wajib Login? (mis. POST /reports)"}
    REQ_AUTH -- "Ya" --> E401["401 Unauthorized (Wajib Akun Perangkat)"]
    REQ_AUTH -- "Tidak" --> GUEST["Konteks Guest (Terapkan Rate Limit IP/Perangkat)"]

    HAS_HDR -- "Ada" --> VERIFY_JWT{"Signature, Exp, dan token_version Valid?"}
    VERIFY_JWT -- "Tidak Valid / Kedaluwarsa" --> E401_TOKEN["401 Unauthorized (Jangan Turunkan Jadi Guest)"]
    VERIFY_JWT -- "Valid" --> CHK_STATUS{"Status User ACTIVE?"}
    CHK_STATUS -- "BANNED" --> E403_BAN["403 Akun Diblokir"]
    CHK_STATUS -- "ACTIVE / RESTRICTED" --> OK_USER["Teruskan ke Handler dengan Principal Server"]
```

**Penjelasan:** Menutup celah temuan A02: token yang rusak atau kedaluwarsa langsung ditolak dengan `401 Unauthorized` dan tidak pernah diturunkan menjadi `Guest`.

---

## 7. Diagram Alir Pencarian Nomor (Phone Lookup Flowchart)

Menggambarkan logika pencarian nomor 2 dimensi (`availability` + `reputation_level`) sehingga nomor yang belum dikenal tidak pernah diklaim "Aman" (ADR-015).

```mermaid
flowchart TD
    IN(["Pengguna Memasukkan Nomor Telepon"]) --> VAL{"Format Valid?"}
    VAL -- "Tidak" --> ERR_IN["Tampilkan Pesan Kesalahan Format Nomor"]
    VAL -- "Ya" --> NORM["Normalisasi ke E.164 (+62... atau Kode Pendek)"]
    NORM --> CACHE{"Ada di Room Cache & Belum Kedaluwarsa?"}

    CACHE -- "Ya (Cache Segar)" --> SHOW_CACHE["Tampilkan Data Cache (Sumber: CACHE) & Segarkan di Latar"]
    CACHE -- "Tidak / Kedaluwarsa" --> CALL_API["Panggil GET /api/v1/phones/{e164} (Timeout 1800 ms)"]

    CALL_API --> API_OK{"Respons Server Berhasil?"}
    API_OK -- "Ya" --> FOUND{"availability == AVAILABLE?"}
    FOUND -- "Ya, Ada di Whitelist" --> SHOW_VER["Tampilkan VERIFIED OFFICIAL / BUSINESS (Teal)"]
    FOUND -- "Ya, Ada Laporan Komunitas" --> SHOW_REP["Simpan ke Room & Tampilkan Level (SUSPICIOUS / SPAM / DANGER_SCAM)"]
    FOUND -- "Tidak (NOT_FOUND)" --> DEL_STALE["Hapus Cache Usang bila Ada"] --> SHOW_UNK["Tampilkan Status UNKNOWN: Belum Ada Data, Bukan Jaminan Aman"]

    API_OK -- "Gagal / Offline" --> HAS_OLD{"Ada Cache Lama (Stale)?"}
    HAS_OLD -- "Ya" --> SHOW_STALE["Tampilkan Cache dengan Peringatan: Data Lama & Sedang Offline"]
    HAS_OLD -- "Tidak" --> SHOW_OFF["Tampilkan Status OFFLINE / UNAVAILABLE: Tidak Dapat Memeriksa"]
```

**Penjelasan:** Memisahkan secara tegas antara nomor resmi (`VERIFIED`), nomor belum terlapor (`UNKNOWN`), dan kegagalan jaringan (`OFFLINE/UNAVAILABLE`).

---

## 8. Diagram Urutan Pencarian Nomor (Phone Lookup Sequence Diagram)

Menunjukkan interaksi berurutan antara pengguna, UI Compose, `LookupRepository`, database Room, Backend Go, dan MongoDB.

```mermaid
sequenceDiagram
    actor U as Pengguna
    participant UI as HomeScreen / Cari
    participant VM as CallTagMainViewModel
    participant LR as LookupRepository
    participant RM as Room (cached_phone_tags)
    participant API as Backend Go (/api/v1/phones)
    participant DB as MongoDB

    U->>UI: Ketik "081299887766" & tekan Cari
    UI->>VM: performPhoneLookup("081299887766")
    VM->>LR: lookupPhoneFull("+6281299887766")
    LR->>RM: getByPhone("+6281299887766")
    RM-->>LR: cachedEntity (atau null)
    par Ambil Detail Reputasi & Laporan Teredaksi
        LR->>API: GET /api/v1/phones/%2B6281299887766
        API->>DB: Baca verified_numbers & phone_tags
        DB-->>API: Dokumen Reputasi
        API-->>LR: 200 OK (availability, reputation_level, risk_score, confidence)
    and Ambil Riwayat Komentar Aman
        LR->>API: GET /api/v1/phones/%2B6281299887766/reports
        API->>DB: Baca spam_reports (status=VALIDATED, tanpa reporter_user_id)
        DB-->>API: Daftar Laporan Teredaksi
        API-->>LR: 200 OK (reports[])
    end
    alt availability == AVAILABLE
        LR->>RM: upsert(CachedPhoneTag dengan expires_at)
        LR-->>VM: LookupResult.Found(data, source="SERVER")
        VM-->>UI: Render Kartu Reputasi, Tag, & Tombol Aksi
    else availability == NOT_FOUND
        LR->>RM: deleteIfUnverified("+6281299887766")
        LR-->>VM: LookupResult.NotFound
        VM-->>UI: Render Kartu UNKNOWN ("Belum ada laporan")
    end
```

**Penjelasan:** Respons `/reports` telah disanitasi di server sehingga tidak pernah mengirimkan `reporter_user_id` ke klien publik (menutup risiko R11).

---

## 9. Diagram Alir Penyaringan Panggilan Masuk (Incoming Call Screening Flowchart)

Menggambarkan alur kritis `CallTagScreeningService` dengan batas waktu ketat `< 5 detik`, kebijakan *auto-block* konservatif (`OD-03` & `ADR-018`), dan mekanisme *fail-open*.

```mermaid
flowchart TD
    CALL(["Panggilan Masuk Diterima Android Telecom"]) --> DIR{"Arah Panggilan == INCOMING?"}
    DIR -- "Tidak" --> ALLOW_NOW["respondToCall: Izinkan Panggilan"]
    DIR -- "Ya" --> NORM["Normalisasi Nomor ke E.164"]
    NORM --> VALID{"Nomor Valid?"}
    VALID -- "Tidak" --> ALLOW_NOW

    VALID -- "Ya" --> GUARD["Mulai Coroutine dengan Batas Total < 4500 ms & Blok finally Fail-Open"]
    GUARD --> RM_CHK{"Cek Room Cache"}
    RM_CHK -- "Cache Miss" --> FAST_API["Panggil FastScreeningApiService (Timeout 800 ms)"]
    FAST_API --> API_RES{"Ditemukan di Server?"}
    API_RES -- "Tidak / Timeout" --> ALLOW_SILENT["respondToCall: Izinkan (Tanpa Klaim Aman)"]
    API_RES -- "Ya" --> EVAL

    RM_CHK -- "Cache Hit" --> EVAL{"Sumber == SERVER & Umur Cache <= 14 Hari?"}
    EVAL -- "Tidak (Seed / Stale / Local)" --> NOTIF_ONLY["respondToCall: Izinkan + Tampilkan Notifikasi Peringatan bila Berisiko"]
    EVAL -- "Ya (Data Server Segar)" --> IS_VER{"Apakah Verified / Whitelist?"}
    IS_VER -- "Ya" --> ALLOW_VER["respondToCall: Izinkan + Banner Resmi"]
    IS_VER -- "Tidak" --> CHK_BLOCK{"Auto-Block ON & Level == DANGER_SCAM?"}

    CHK_BLOCK -- "Ya" --> REJECT["respondToCall: Tolak Panggilan (setDisallowCall & setRejectCall)"]
    CHK_BLOCK -- "Tidak (SUSPICIOUS / SPAM / Toggle OFF)" --> ALLOW_WARN["respondToCall: Izinkan + Tampilkan Overlay/Heads-Up Peringatan"]

    REJECT --> LOG["Simpan CallScreeningLog (Nomor Ter-Mask) & Notifikasi Tombol Bukan Spam"]
    ALLOW_WARN --> LOG
    NOTIF_ONLY --> LOG
```

**Penjelasan:** Hanya nomor berstatus `DANGER_SCAM` (yang telah melewati ambang skor $\ge 70$, $\ge 5$ pelapor unik, dan gerbang konfirmasi moderator sesuai OD-03) dengan cache server segar ($\le 14$ hari) yang dapat diblokir otomatis bila pengguna mengaktifkan *toggle* Auto-Block.

---

## 10. Diagram Alir Pelaporan Komunitas (Community Reporting Flowchart)

Menggambarkan pengiriman laporan oleh pengguna terdaftar, perlindungan *idempotency*, penahanan otomatis pada lonjakan laporan, dan penyimpanan *outbox* saat offline.

```mermaid
flowchart TD
    START(["Pengguna Mengisi Form Laporan Spam/Scam"]) --> V_LOC{"Validasi Lokal: Nomor E.164, Kategori, Komentar <= 500 Karakter"}
    V_LOC -- "Gagal" --> ERR_UI["Tampilkan Pesan Validasi di Form"]
    V_LOC -- "Lolos" --> GEN_KEY["Buat UUID Idempotency-Key"]

    GEN_KEY --> HAS_NET{"Ada Koneksi & Token Perangkat?"}
    HAS_NET -- "Tidak (Offline)" --> SAVE_OUTBOX["Simpan ke Tabel Room pending_reports (Status: PENDING)"]
    SAVE_OUTBOX --> NO_SCORE["Jangan Ubah Skor Reputasi Lokal di cached_phone_tags"]
    NO_SCORE --> ENQ_WM["Jadwalkan PendingReportWorker (WorkManager Backoff)"]
    ENQ_WM --> MSG_OFF["Tampilkan: Tersimpan di Perangkat, Akan Dikirim Saat Online"]

    HAS_NET -- "Ya (Online)" --> POST_API["POST /api/v1/reports (Bearer JWT + Idempotency-Key)"]
    POST_API --> RESP{"Kode Respons Server"}
    RESP -- "401 Unauthorized" --> REFRESH["POST /auth/refresh Sekali Lalu Ulangi POST /reports"]
    RESP -- "429 Too Many Requests" --> MSG_QUOTA["Tampilkan: Kuota Laporan Harian Tercapai"]
    RESP -- "400 Bad Request / PII Terdeteksi" --> MSG_FAIL["Tandai Gagal & Tampilkan Alasan Validasi"]
    RESP -- "200 / 201 Berhasil" --> SRV_PROC["Server: Dedup 30 Hari, Rekonsiliasi Skor, Cek Gerbang Moderator"]
    SRV_PROC --> UPD_CACHE["Perbarui Room cached_phone_tags dari Respons Server"]
    UPD_CACHE --> MSG_OK["Tampilkan Status: Diterima Server / Menunggu Tinjauan Moderator"]
```

**Penjelasan:** Saat offline, laporan disimpan di `pending_reports` tanpa memalsukan kenaikan skor lokal (menutup temuan A04).

---

## 11. Diagram Alir Sanggahan dan Moderasi (Dispute and Moderation Flowchart)

Menggambarkan alur sanggahan pemilik nomor (termasuk *Guest* sesuai OD-10), antrean kasus di `moderation_cases`, gerbang konfirmasi `DANGER_SCAM` (OD-03), dan penyebaran koreksi ke klien.

```mermaid
flowchart TD
    subgraph Masuk["Sumber Kasus Moderasi"]
        D_IN["Pemilik Nomor / Guest Mengajukan Dispute (Alasan + Kontak Balik)"]
        R_SPIKE["Laporan dengan Lonjakan Mencurigakan / Kandidat DANGER_SCAM (OD-03)"]
    end

    D_IN --> RL_CHK{"Lolos Rate Limit (Maks 3/24 Jam) & Tidak Ada Dispute Aktif?"}
    RL_CHK -- "Tidak" --> D_REJECT["429 / 409 Tolak Pengajuan Duplikat"]
    RL_CHK -- "Ya" --> SAVE_CASE["Simpan ke Koleksi moderation_cases (Status: OPEN) Sebelum Balas 201"]
    SAVE_CASE --> RET_TOKEN["Kembalikan dispute_id & tracking_token ke Pemohon"]

    R_SPIKE --> HOLD_CASE["Simpan Kasus REPORT_REVIEW di moderation_cases (Level Tertahan di SPAM)"]

    RET_TOKEN --> QUEUE["Antrean Panel Moderator (/admin/cases) dengan Indikator SLA (OD-09)"]
    HOLD_CASE --> QUEUE

    QUEUE --> MOD_ACT{"Keputusan Moderator (Wajib Alasan & Optimistic Lock version)"}
    MOD_ACT -- "Butuh Bukti Tambahan" --> ST_INFO["Status: NEEDS_INFO (Terlihat Saat Pemohon Lacak Dispute)"]
    MOD_ACT -- "Dispute Diterima (False Positive)" --> ST_ACC["Status: ACCEPTED -> Reset Skor / Tandai CORRECTED / Masuk Whitelist Opsional"]
    MOD_ACT -- "Dispute Ditolak" --> ST_REJ["Status: REJECTED -> Reputasi Tetap, Alasan Dicatat"]
    MOD_ACT -- "Setujui Kandidat DANGER_SCAM" --> ST_DNG["Status: ACCEPTED -> Promosikan Nomor ke DANGER_SCAM"]

    ST_ACC & ST_REJ & ST_DNG --> AUDIT["Tulis Entri Append-Only ke audit_logs"]
    AUDIT --> SYNC_EVT["Tulis Nomor Urut Baru ke sync_changes (UPSERT / DELETE)"]
    SYNC_EVT --> CLIENT_SYNC["Aplikasi Android Menarik Perubahan via GET /sync/changes"]
```

**Penjelasan:** Menutup temuan A03 (dispute wajib tersimpan di MongoDB sebelum merespons `201`) dan OD-03 (status `DANGER_SCAM` memerlukan pengesahan moderator).

---

## 12. Diagram Hubungan Entitas Basis Data (Database ER Diagram)

Menampilkan hubungan antar-11 koleksi MongoDB di backend dan 6 tabel Room v2 di Android.

```mermaid
erDiagram
    USERS ||--o{ DEVICES : "memiliki"
    USERS ||--o{ REFRESH_TOKENS : "menerbitkan"
    USERS ||--o{ SPAM_REPORTS : "melaporkan"
    PHONE_TAGS ||--o{ SPAM_REPORTS : "direkonsiliasi_dari"
    PHONE_TAGS ||--o{ MODERATION_CASES : "ditinjau_pada"
    VERIFIED_NUMBERS ||--o| PHONE_TAGS : "menimpa_sebagai_resmi"
    USERS ||--o{ AUDIT_LOGS : "mencatat_aksi_staf"
    MODERATION_CASES ||--o{ AUDIT_LOGS : "menghasilkan_jejak"
    PHONE_TAGS ||--o{ SYNC_CHANGES : "menerbitkan_event"
    EMERGENCY_DIRECTORY ||--o{ SYNC_CHANGES : "menerbitkan_event"
    OFFICIAL_CHANNELS ||--o{ SYNC_CHANGES : "menerbitkan_event"

    USERS {
        ObjectId _id PK
        string role "USER | MOD | ADMIN"
        string status "ACTIVE | RESTRICTED | BANNED"
        double trust_weight
        int token_version
    }
    DEVICES {
        ObjectId _id PK
        ObjectId user_id FK
        string install_id_hash UK
    }
    PHONE_TAGS {
        ObjectId _id PK
        string phone_e164 UK
        string reputation_level
        double risk_score
        int unique_reporters
        string dispute_state
        date expires_at
    }
    SPAM_REPORTS {
        ObjectId _id PK
        string phone_e164 FK
        ObjectId reporter_user_id FK
        string idempotency_key
        string category_code
        string status "PENDING_MOD | VALIDATED | REJECTED"
    }
    MODERATION_CASES {
        ObjectId _id PK
        string type "DISPUTE | REPORT_REVIEW"
        string phone_e164 FK
        string status "OPEN | NEEDS_INFO | ACCEPTED | REJECTED"
        int version
    }
    VERIFIED_NUMBERS {
        ObjectId _id PK
        string phone_e164 UK
        string kind "OFFICIAL | BUSINESS"
        string source_url
        date expires_at
    }
    SYNC_CHANGES {
        int64 seq PK
        int generation
        string op "UPSERT | DELETE"
        string kind "PHONE_TAG | EMERGENCY"
        string key
    }
    AUDIT_LOGS {
        ObjectId _id PK
        string action
        string actor_id
        string reason
        date at
    }
```

**Penjelasan:** Skema menggunakan *Idempotent Upsert* + rekonsiliasi dari `spam_reports` (OD-08) sehingga tetap konsisten baik di MongoDB *standalone* maupun *Replica Set*.

---

## 13. Diagram Sinkronisasi Offline (Offline Synchronisation Diagram)

Menunjukkan mekanisme sinkronisasi berbasis jurnal perubahan (`sync_changes`) dengan *cursor* opak (`generation:seq`) yang menutup bug kehilangan versi pada temuan A06.

```mermaid
sequenceDiagram
    participant WM as WorkManager (SyncWorker)
    participant SR as SyncRepository
    participant RM as Room v2 (sync_state & cached_phone_tags)
    participant API as Backend Go (/api/v1/sync/changes)
    participant DB as MongoDB (sync_changes)

    WM->>SR: Jalankan Sinkronisasi Terjadwal / Manual
    SR->>RM: Baca cursor terakhir ("gen:seq") dari tabel sync_state
    RM-->>SR: cursor = "1:1040"
    loop Selama has_more == true
        SR->>API: GET /api/v1/sync/changes?cursor=1:1040&limit=200
        API->>DB: Cari dokumen dengan seq > 1040 ORDER BY seq ASC LIMIT 200
        DB-->>API: Daftar event UPSERT & DELETE
        alt Generation Cocok
            API-->>SR: 200 OK (items[], next_cursor="1:1240", has_more, generation=1)
            SR->>RM: Transaksi Room: Terapkan UPSERT, Hapus untuk DELETE, Simpan next_cursor
        else Generation Berubah / Cursor Kedaluwarsa (> 90 Hari)
            API-->>SR: 409 Conflict (FULL_RESYNC_REQUIRED, generation=2)
            SR->>RM: Transaksi Room: Bersihkan Cache Server & Reset cursor ke "2:0"
        end
    end
    SR-->>WM: Sinkronisasi Selesai (Simpan last_success_at)
```

**Penjelasan:** Urutan halaman didasarkan pada nomor urut `seq` yang selalu naik, bukan `risk_score`, sehingga tidak ada nomor yang terlewat dan pencabutan status berbahaya (`DELETE`/tombstone) ikut tersinkronisasi ke perangkat.

---

## 14. Diagram Alur Kerja Admin (Admin Workflow Diagram)

Menggambarkan alur kerja Staf (`Moderator` dan `Administrator`) pada panel internal `/admin` untuk kurasi direktori wilayah pilot (OD-05), pengelolaan whitelist, dan peninjauan kasus.

```mermaid
flowchart LR
    LOGIN(["Staf Buka /admin/login"]) --> AUTH["Verifikasi Email, Password (Argon2id), & Kode TOTP"]
    AUTH --> DASH["Dashboard Ringkas: Antrean Kasus, Peringatan SLA (OD-09), Statistik 24 Jam"]

    DASH --> T_MOD["Tugas Moderator & Admin"]
    DASH --> T_ADM["Tugas Khusus Admin"]

    subgraph ModFlow["Alur Moderasi (MOD & ADMIN)"]
        T_MOD --> Q_LIST["Pilih Kasus: Dispute atau Kandidat DANGER_SCAM"]
        Q_LIST --> INSPECT["Periksa Riwayat Laporan, Bukti Pemohon, & Skor Pelapor"]
        INSPECT --> DECIDE["Simpan Keputusan + Alasan Wajib"]
    end

    subgraph AdmFlow["Alur Kurasi & Konfigurasi (Khusus ADMIN)"]
        T_ADM --> DIR_EDIT["CRUD Direktori Darurat (7 Nasional + 5 Kota Pilot OD-05)"]
        T_ADM --> WL_EDIT["CRUD Nomor Terverifikasi / Whitelist Resmi"]
        T_ADM --> AUD_VIEW["Tinjau & Cari Audit Log"]
        DIR_EDIT & WL_EDIT --> CHK_SRC{"Ada source_url & Tanggal Verifikasi?"}
        CHK_SRC -- "Tidak" --> REJ_SRC["Tolak Penyimpanan (Sumber Wajib)"]
        CHK_SRC -- "Ya" --> SAVE_CUR["Simpan ke MongoDB"]
    end

    DECIDE --> WRITE_AUD["Tulis audit_logs (Append-Only) & Terbitkan Event sync_changes"]
    SAVE_CUR --> WRITE_AUD
```

**Penjelasan:** Setiap perubahan data direktori darurat dan whitelist mewajibkan pencantuman `source_url` resmi (OD-05) dan tercatat di `audit_logs`.

---

## 15. Diagram Integrasi Website (Website Integration Diagram)

Menunjukkan bagaimana portal publik `calltag.github.io` dan `apidocs` berinteraksi dengan backend dan GitHub Releases.

```mermaid
flowchart TD
    USER["Pengunjung Website"] --> PORTAL["Portal Web Statis (calltag.github.io)"]
    DEV["Pengembang / Penguji"] --> SWAGGER["Dokumentasi API (apidocs Swagger UI)"]

    PORTAL -->|"1. Cek Nomor: GET /api/v1/phones/{e164}"| API["Backend Go REST API"]
    PORTAL -->|"2. Direktori Pilot: GET /api/v1/emergency/*"| API
    PORTAL -->|"3. Kanal Resmi: GET /api/v1/official-channels"| API
    PORTAL -->|"4. Statistik Nyata: GET /api/v1/stats/public"| API
    PORTAL -->|"5. Ajukan Dispute Guest (OD-10): POST /api/v1/disputes"| API
    PORTAL -->|"6. Cek Rilis APK"| GH["GitHub Releases API (CallTag/CallTag)"]

    API -->|"Sukses: JSON Envelope"| RENDER_LIVE["Render Data Server dengan Label Sumber SERVER"]
    API -. "Gagal Jaringan / Backend Mati" .-> RENDER_ERR["Tampilkan Status OFFLINE / Contoh Lokal Berlabel Jelas (Tanpa Klaim Aman)"]

    SWAGGER -->|"Mode Live"| API
    SWAGGER -. "Mode Simulasi Berlabel" .-> MOCK["Simulator Lokal app.js (Sesuai Skema OpenAPI)"]
```

**Penjelasan:** Pengiriman laporan spam cepat dari web dinonaktifkan pada MVP untuk mencegah spam tanpa identitas perangkat, tetapi pengajuan sanggahan (*dispute*) oleh *Guest* tetap dibuka dengan *rate limit* ketat (OD-10).

---

## 16. Arsitektur Deployment (Deployment Architecture)

Menggambarkan skema deployment **Zero-Cost (Rp 0)** sesuai keputusan OD-06 untuk lingkungan pengembangan/demo lokal dan lingkungan staging/cloud opsional.

```mermaid
flowchart TD
    subgraph ModeLokal["1. Lingkungan Demo Akademik & Pengembangan (Lokal / LAN - Rp 0)"]
        EMU["Emulator Android (Pixel_5_Light - 10.0.2.2:8080)"]
        HP["HP Android Fisik (USB adb reverse tcp:8080 / Wi-Fi LAN)"]
        WEB_LOC["Server Web Lokal (:8082) & API Docs (:8081)"]
        GO_LOC["Backend Go Lokal (:8080, APP_ENV=dev)"]
        M_LOC[("MongoDB Lokal Standalone :27017 (calltag_db)")]

        EMU & HP & WEB_LOC --> GO_LOC
        GO_LOC --> M_LOC
    end

    subgraph ModeCloud["2. Lingkungan Staging / Demo Cloud Opsional (Free Tier - Rp 0)"]
        APK_STG["APK Sideload GitHub Releases (targetSdk 34 - OD-04)"]
        GHP["GitHub Pages: calltag.github.io & apidocs"]
        TUN["Reverse Proxy / HTTPS Endpoint"]
        GO_STG["Backend Go (APP_ENV=prod, Fail-Closed, Tanpa Memory Fallback)"]
        ATLAS[("MongoDB Atlas Free Tier M0 + Backup Snapshot")]

        APK_STG & GHP -->|"HTTPS"| TUN
        TUN --> GO_STG
        GO_STG -->|"TLS MongoDB URI"| ATLAS
    end
```

**Penjelasan:** Skema ini memastikan demo sidang kuliah 100% andal di jaringan lokal tanpa biaya sewa server, sekaligus mendukung koneksi cloud melalui MongoDB Atlas Free Tier dan GitHub Pages.

---

## 17. Diagram Alur CI/CD (CI/CD Diagram)

Menggambarkan pipeline pengujian otomatis pada setiap *Pull Request* dan pemisahan pembuatan rilis APK hanya ketika *git tag* versi diterbitkan (mencegah commit dokumentasi memicu rilis APK).

```mermaid
flowchart LR
    DEV["Push Branch / Buka Pull Request"] --> CI_START["GitHub Actions: Pipeline Verifikasi"]

    subgraph Checks["Gerbang Kualitas Otomatis (Wajib Lulus)"]
        G_TEST["Backend: go vet & go test ./..."]
        A_TEST["Android: ./gradlew compileDebugKotlin & testDebugUnitTest"]
        W_TEST["Website: node tests/frontend-state.test.mjs"]
        C_TEST["Validasi Skema openapi.json & Link Docs"]
    end

    CI_START --> G_TEST & A_TEST & W_TEST & C_TEST
    G_TEST & A_TEST & W_TEST & C_TEST --> GATE{"Semua Pemeriksaan Lulus?"}

    GATE -- "Tidak" --> BLOCK["Tolak Merge PR & Laporkan Galat"]
    GATE -- "Ya" --> REVIEW["Review Silang Pengembang A & B -> Merge ke main"]

    REVIEW --> IS_TAG{"Apakah Push Berupa Git Tag Versi (v*.*.*)?"}
    IS_TAG -- "Tidak (Hanya Kode / Docs)" --> DEPLOY_PAGES["Update GitHub Pages (Website & API Docs) Tanpa Rilis APK"]
    IS_TAG -- "Ya (Tag Rilis Resmi)" --> BUILD_APK["Build Signed Release APK/AAB (targetSdk 34) & Terbitkan ke GitHub Releases"]
```

**Penjelasan:** Memperbaiki perilaku `.github/workflows/release-android.yml` agar pembaruan dokumentasi tidak lagi memicu pembuatan rilis APK baru secara tidak sengaja.
