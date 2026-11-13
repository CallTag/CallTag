# 13. Diagram sistem

Status: **PROPOSED** (target). Diagram AS-IS yang sudah benar **tidak diduplikasi**: lihat [SYSTEM_ARCHITECTURE](../SYSTEM_ARCHITECTURE.md) (context/container/komponen/DFD/deployment as-is) dan [FLOWS_AND_DIAGRAMS](../FLOWS_AND_DIAGRAMS.md) (alur as-is dan sequence). Dokumen ini memuat diagram target yang belum ada dan indeks untuk yang sudah ditempatkan di dokumen topik.

## Indeks 17 diagram

| # | Diagram | Lokasi |
|---|---|---|
| 1 | System Context (target) | bagian 1 di bawah |
| 2 | High-Level Architecture | bagian 2 |
| 3 | Android Component | [05](05-ARSITEKTUR-SISTEM.md) bagian 1 |
| 4 | Backend Component | [05](05-ARSITEKTUR-SISTEM.md) bagian 2 |
| 5 | Role dan Permission | [02](02-ROLE-DAN-HAK-AKSES.md) bagian 3 |
| 6 | Authentication Flowchart | [02](02-ROLE-DAN-HAK-AKSES.md) bagian 4 |
| 7 | Phone Lookup Flowchart | [08](08-STRATEGI-PENCARIAN-NOMOR.md) bagian 2 |
| 8 | Phone Lookup Sequence | [08](08-STRATEGI-PENCARIAN-NOMOR.md) bagian 2 |
| 9 | Incoming Call Screening | bagian 3 |
| 10 | Community Reporting | bagian 4 |
| 11 | Dispute dan Moderation | [11](11-ADMIN-DAN-MODERASI.md) bagian 3 |
| 12 | Database ER | [06](06-DESAIN-DATABASE.md) bagian 3 |
| 13 | Offline Synchronisation | bagian 5 |
| 14 | Admin Workflow | [11](11-ADMIN-DAN-MODERASI.md) bagian 4 |
| 15 | Website Integration | bagian 6 |
| 16 | Deployment Architecture | bagian 7 |
| 17 | CI/CD | bagian 8 |

## 1. System Context (target)

```mermaid
flowchart LR
    W[Warga pengguna Android] --> APP[Aplikasi CallTag]
    V[Pengunjung web] --> WEB[Website CallTag]
    ST[Staf Moderator dan Admin] --> ADM[Panel admin]
    APP --> API[Backend CallTag]
    WEB --> API
    ADM --> API
    API --> DB[(MongoDB)]
    TEL[Android Telecom] --> APP
    APP --> OUT[Dialer dan kanal resmi eksternal]
    API -. opsional, belum dibangun .-> EXT[Penyedia data berizin]
```

Garis putus-putus menandai integrasi yang tidak dibangun pada MVP.

## 2. High-Level Architecture

```mermaid
flowchart TD
    subgraph Klien
        A1[Android Compose dan Room]
        A2[Website statis]
    end
    subgraph Server
        P[Reverse proxy HTTPS]
        G[Go modular monolith: middleware, controller, service, repository, job]
        AD[Halaman admin tertanam]
    end
    subgraph Data
        M[(MongoDB replica set)]
        B[(Backup terenkripsi)]
    end
    A1 --> P
    A2 --> P
    P --> G
    P --> AD
    AD --> G
    G --> M
    M --> B
```

## 3. Incoming Call Screening (target)

```mermaid
flowchart TD
    S[onScreenCall] --> D{Panggilan masuk?}
    D -- tidak --> AL[Izinkan]
    D -- ya --> N[Normalisasi E.164]
    N --> E{Valid?}
    E -- tidak --> AL
    E -- ya --> C{Cache ada?}
    C -- ya --> F{Segar ≤ 14 hari dan sumber SERVER?}
    C -- tidak --> Q[API timeout 800 ms]
    Q --> R{Berhasil?}
    R -- tidak --> AL2[Izinkan tanpa klaim aman]
    R -- ya --> F
    F -- tidak --> NT[Hanya notifikasi informatif bila level berisiko, tidak memblokir]
    F -- ya --> L{DANGER_SCAM, bukan verified, auto-block ON?}
    L -- ya --> BL[Tolak panggilan dan notifikasi dengan Bukan spam]
    L -- tidak --> NT
    NT --> AL
    BL --> LG[Catat log nomor ter-mask]
    AL2 --> LG
    AL --> LG
```

Aturan: seluruh jalur berakhir dengan respons ke Telecom dalam < 5 detik; `finally` menjamin `allow` bila terjadi kesalahan.

## 4. Community Reporting

```mermaid
flowchart TD
    U[Pengguna isi laporan] --> V{Validasi lokal: nomor, kategori, panjang}
    V -- gagal --> E[Tampilkan kesalahan]
    V -- lolos --> K[Buat Idempotency-Key]
    K --> O{Jaringan dan token tersedia?}
    O -- tidak --> P[Simpan pending_reports PENDING, skor lokal tidak berubah]
    P --> W[WorkManager coba ulang dengan backoff]
    W --> O
    O -- ya --> S[POST /reports]
    S --> R{Respons}
    R -- 201 ACCEPTED --> OK[Tampilkan diterima server]
    R -- 201 HELD_FOR_REVIEW --> HD[Tampilkan menunggu moderasi]
    R -- 200 ulangan kunci sama --> OK
    R -- 429 --> Q[Tampilkan kuota habis]
    R -- 401 --> T[Refresh token lalu ulangi sekali]
    R -- 400 --> X[Tandai FAILED dengan pesan]
```

## 5. Offline Synchronisation

```mermaid
sequenceDiagram
    participant WM as SyncWorker
    participant SR as SyncRepository
    participant DB as Room sync_state dan data
    participant API as Backend
    participant J as sync_changes
    WM->>SR: jalankan sinkronisasi unik
    SR->>DB: baca cursor dan generation
    loop selama has_more
        SR->>API: GET /sync/changes?cursor=...
        API->>J: baca seq lebih besar dari cursor, urut seq
        J-->>API: halaman perubahan
        API-->>SR: items, next_cursor, has_more, generation
        SR->>DB: transaksi: terapkan UPSERT dan DELETE lalu simpan cursor
    end
    alt generation berubah atau FULL_RESYNC_REQUIRED
        SR->>DB: kosongkan data sinkron lalu mulai dari cursor kosong
    end
    SR-->>WM: sukses dan last_success_at
```

## 6. Website Integration

```mermaid
flowchart LR
    U[Pengunjung] --> P[calltag.github.io statis]
    P -->|fetch HTTPS, CORS origin terdaftar| API[GET /phones, /emergency, /official-channels, /stats/public]
    P -->|tautan| REL[GitHub Releases]
    API --> DB[(MongoDB)]
    P -. gagal jaringan .-> ERR[Pesan galat, bukan data contoh sebagai nyata]
    DOC[apidocs Swagger UI] -->|mode live atau simulasi berlabel| API
```

## 7. Deployment Architecture

```mermaid
flowchart TD
    subgraph Dev
        D1[Emulator 10.0.2.2] --> D2[Backend lokal 8080]
        D2 --> D3[(Mongo lokal)]
    end
    subgraph Staging
        S1[APK staging] --> SP[Reverse proxy TLS]
        SW[Website dan apidocs staging] --> SP
        SP --> SB[Go instance]
        SB --> SM[(Mongo replica set staging)]
    end
    subgraph Production
        P1[APK release atau situs] --> PP[Reverse proxy TLS]
        PP --> PB[Go instance]
        PB --> PM[(Mongo replica set produksi)]
        PM --> BK[(Backup harian terenkripsi)]
        PB --> LG[Log JSON teredaksi]
    end
```

Host konkret (VPS/PaaS) belum diputuskan: OD-06.

## 8. CI/CD

```mermaid
flowchart LR
    PR[Pull request] --> L[Lint dan build]
    L --> T1[go vet dan go test]
    L --> T2[Gradle compile dan unit test]
    L --> T3[Node test website]
    L --> T4[Validasi OpenAPI dan contract test]
    T1 --> G{Semua lulus?}
    T2 --> G
    T3 --> G
    T4 --> G
    G -- tidak --> X[Blokir merge]
    G -- ya --> R[Review manusia dan merge]
    R --> ST[Deploy staging otomatis]
    ST --> SM[Smoke test readyz]
    SM --> TG{Tag rilis oleh manusia?}
    TG -- ya --> RL[Build release bertanda tangan, publish rilis]
    TG -- tidak --> END[Berhenti]
```

Catatan: workflow rilis Android saat ini terpicu setiap push `main` ([SYSTEM_ARCHITECTURE](../SYSTEM_ARCHITECTURE.md), Catatan CI). Target: rilis hanya dari tag atau persetujuan manual; perubahan dokumen saja tidak memicu rilis.
