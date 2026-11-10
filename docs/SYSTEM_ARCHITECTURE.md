# Arsitektur sistem CallTag

Basis dan status mengikuti [AUDIT_REPORT](AUDIT_REPORT.md). Diagram AS-IS menggambarkan kode pada feature audit, bukan deployment publik. **PROPOSED** adalah target dan belum diimplementasikan.

## Context dan container AS-IS

```mermaid
flowchart LR
    U[Pengguna Android] --> A[CallTag Compose dan screening service]
    A <--> R[(Room cache dan log)]
    A -->|HTTP emulator atau URL override| B[Go net/http REST API]
    W[Website publik feature branch] -->|croot fetch| B
    D[API docs feature branch] -->|live atau simulasi otomatis| B
    B <--> M[(MongoDB bila startup berhasil)]
    B <--> F[(Memory fallback saat startup gagal)]
    A -->|ACTION_DIAL atau browser| E[Dialer dan kanal resmi eksternal]
    W --> E
```

Android tidak langsung mengakses MongoDB. Go adalah satu proses HTTP, bukan kumpulan microservice; gocroot hanya pendekatan penamaan router pada baseline. Outbound dialer/browser bukan integrasi laporan ke instansi. Runtime lokal memilih Mongo; fallback hanya pilihan startup, bukan failover durable.

## Komponen internal AS-IS

```mermaid
flowchart TD
    UI[Empat tab Compose] --> VM[CallTagMainViewModel]
    VM --> RP[CallTagRepository]
    SC[CallTagScreeningService] --> RP
    SC --> PF[DataStore preferences]
    SC --> NF[CallerIdNotifier notification dan overlay]
    RP --> PF
    RP --> DA[Room DAO tiga tabel]
    RP --> RT[Retrofit standard 8 s dan fast 800 ms]
    RT --> RO[route.URL]
    RO --> C[controller auth lookup report emergency sync]
    C --> H[helper auth scoring response]
    C --> P[config persistence dan aggregate]
    P --> MD[Mongo driver]
    P --> MM[Map memory dengan mutex]
    SD[OfflineSeedData] --> DA
```

Wiring ada pada `CallTagApplication.kt:20–35`; dua client `NetworkModule.kt:16–56`; backend router `route/route.go:23–69`, controller memanggil fungsi `config/db.go` langsung. Tidak ada dependency injection framework atau service layer terpisah yang wajib ditambah untuk tim dua orang. Mutex melindungi memory; itu tidak membuktikan atomisitas Mongo.

## DFD level 0 AS-IS

```mermaid
flowchart LR
    U[Warga] -->|nomor dan laporan| S((CallTag ecosystem))
    S -->|label sumber dan hasil lookup| U
    S <--> DB[(Penyimpanan server atau lokal)]
    OS[Android Telecom] -->|incoming call| S
    S -->|allow atau reject| OS
    S -->|buka URL atau dialer| X[Instansi dan telepon eksternal]
```

Boundary data sensitif meliputi nomor target, identitas pseudonim, komentar dan log panggilan. Diagram tidak menampilkan unggah buku kontak karena tidak ditemukan implementasinya; OS menyediakan screening sesuai perizinan.

## DFD level 1 AS-IS

```mermaid
flowchart TD
    IN[Input pengguna atau Telecom] --> L((Lookup))
    L <--> CACHE[(cached_phone_tags)]
    L --> API((REST API))
    API <--> PT[(phone_tags)]
    IN --> REP((Submit report))
    REP --> API
    API --> SR[(spam_reports)]
    SR --> AG((Aggregate score))
    AG --> PT
    REP -->|remote error hanya agregat lokal| CACHE
    IN --> DIS((Submit dispute))
    DIS --> API
    API -->|dispute 201 tanpa write| ACK[UNDER_REVIEW semu]
    SY((Sync manual)) --> API
    PT --> SY
    SY --> CACHE
    DIR((Direktori)) <--> EC[(emergency_contacts)]
    DIR --> API
    API <--> ED[(emergency_directory dan official_channels)]
```

Laporan offline tidak melewati outbox; komentar asli tidak ditulis sebagai raw report lokal. Dispute berhenti pada acknowledgment semu. DFD menjelaskan letak putus alur agar keberadaan endpoint tidak disamakan dengan penyimpanan.

## Deployment PROPOSED — minimum MVP

```mermaid
flowchart LR
    EM[Emulator 10.0.2.2 atau device LAN] --> DEV[Backend dev localhost:8080]
    DEV --> TEST[(Mongo test terisolasi)]
    APP[APK staging BASE_API_URL eksplisit] --> TLS[HTTPS reverse proxy]
    WEB[Website dan API docs staging] --> TLS
    TLS --> API[Go satu instance dengan readiness]
    API --> MDB[(MongoDB durable dengan backup)]
    MOD[Moderator internal] --> AUTH[Login internal dan RBAC PROPOSED]
    AUTH --> API
    API --> LOG[Log teredaksi dan audit tindakan]
```

Staging harus memakai DB tersendiri dan secret environment, bukan fallback publik. Satu proses + Mongo cukup untuk MVP; transaction lintas laporan/agregat membutuhkan replica set atau alternatif CAS/reconciliation yang terdokumentasi. Website statis boleh GitHub Pages; URL API wajib dikonfigurasi dan akses CORS dibatasi sesuai origin yang dipakai.

## Dependency, lifecycle dan failure boundary

| Area | AS-IS dan bukti | Target PROPOSED |
|---|---|---|
| Transport | `app/build.gradle.kts:60–63` default HTTP emulator; manifest:23 cleartext global | Build dev/staging/release eksplisit; production HTTPS, cleartext hanya dev. |
| Persistence | `config/db.go:35–67` fallback saat startup; health `controller/sync.go:83` flag saja | Production startup strict, liveness/readiness terpisah, bounded ping. |
| Screening | `CallTagScreeningService.kt:57–114` coroutine + network 800 ms | Deadline total <5 detik dan fail-open pada finally, lalu observability tanpa nomor lengkap. |
| Cache | repository:77 langsung return cache, seeding async `CallTagDatabase.kt:53` | Source/freshness/version terpisah, initial readiness, revocation event. |
| Background | VM:315 sync manual | WorkManager unik dengan constraint/retry; UI dapat sync manual dan melihat last-success. |
| Recovery | Room version 1 destructive; belum ada kode migration/backup Mongo | Export schema, non-destructive migration, index migration idempoten, uji restore di staging. |

## Rekomendasi urutan merge dan release

1. Review feature backend/apidocs, pisahkan patch auth/persistence/kontrak dari dokumen.
2. Jalankan test unit/contract/integration pada DB test; perbaiki P0.
3. Buat PR per repo, merge setelah review manusia; audit ini tidak melakukan merge.
4. Deploy staging dan konfigurasi URL APK/website/OpenAPI seragam, lalu jalankan E2E/perangkat.
5. Rilis hanya setelah gate keselamatan, sumber direktori, signing dan backup terpenuhi.

**Catatan CI:** `.github/workflows/release-android.yml:3–7` memicu release pada push main, termasuk merge docs. Reviewer harus menata filter path/approval release sebelum merge audit bila tidak ingin rilis APK dipicu perubahan dokumentasi. Audit tidak mengubah workflow tersebut.
