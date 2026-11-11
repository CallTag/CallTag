# Alur dan diagram

Baseline Android `42e9ceb`, backend `8676314`. **AS-IS** adalah alur source yang ditemukan; panah tidak berarti E2E telah diuji. **PROPOSED** adalah target implementasi. Bukti runtime dan batasnya ada di [AUDIT_REPORT](AUDIT_REPORT.md). Inventaris dan prioritas ada di [FEATURE_GAP_ANALYSIS](FEATURE_GAP_ANALYSIS.md).

## 1. AS-IS — onboarding dan izin

```mermaid
flowchart TD
  A[Buka aplikasi] --> B[Application: Room seed dan channel notifikasi]
  B --> C[Home: baca role dan runtime permissions]
  C --> D{Pengguna tekan Aktifkan}
  D --> E{Semua runtime permissions ada?}
  E -- Tidak --> F[Request runtime permissions]
  F --> C
  E -- Ya --> G{Intent role tersedia API29+?}
  G -- Ya --> H[Dialog ROLE_CALL_SCREENING]
  H --> C
  G -- Tidak --> F
  C --> I[Status UI aktif bila role dan permissions]
  I --> J[Belum ada pemanggilan auth device dari UI]
  J --> K[Grant overlay tidak dihubungkan ke alur ini]
```

Bukti: [Application:31](../app/src/main/java/id/calltag/app/CallTagApplication.kt#L31), [Home:134](../app/src/main/java/id/calltag/app/ui/screens/HomeScreen.kt#L134), [RolePermissionManager:24](../app/src/main/java/id/calltag/app/util/RolePermissionManager.kt#L24). Status aktif bukan pengujian Telecom/OEM. API26–28 mengandalkan cek permission, tanpa role intent modern.

## 2. AS-IS — pencarian nomor

```mermaid
flowchart TD
  A[Input nomor] --> B[VM: Loading]
  B --> C[Normalisasi dan baca Room]
  C --> D[GET phone: timeout1800ms]
  D --> E[GET reports: timeout1800ms]
  E --> F{Details ada dan found?}
  F -- Ya --> G[Upsert Room; gunakan reports API]
  G --> H{Reports atau breakdown kosong?}
  H -- Ya --> I[Isi dari seed sintetis]
  H -- Tidak --> J[Success LIVE_API]
  I --> J
  F -- Tidak --> K{Cache ada?}
  K -- Ya --> L[Success LOCAL_CACHE dengan seed comments]
  K -- Tidak --> M[Success synthetic CLEAN skor0 LOCAL_CACHE]
  J --> N[Home menampilkan hasil]
  L --> N
  M --> N
  N --> O[Untuk CLEAN tanpa pelapor: belum ada laporan dan disclaimer]
  O --> P[Preview CLEAN masih berlabel NOMOR AMAN]
```

Bukti: [repo:106](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L106), [Home:513](../app/src/main/java/id/calltag/app/ui/screens/HomeScreen.kt#L513), [preview:75](../app/src/main/java/id/calltag/app/ui/components/CallerIdOverlayCard.kt#L75). Tidak ada state UNKNOWN/OFFLINE_MISS terpisah; dua API saat ini sekuensial.

## 3. AS-IS — screening cache, API dan error

```mermaid
flowchart TD
  A[Android onScreenCall] --> B{Incoming dan nomor valid?}
  B -- Tidak --> C[Allow]
  B -- Ya --> D[Baca Room]
  D --> E{Cache hit?}
  E -- Ya --> F[Pakai cache tanpa cek TTL]
  E -- Tidak --> G[Fast GET phone: timeout800ms]
  G --> H{DTO tersedia?}
  H -- Tidak atau error --> C
  H -- Ya --> I[Map dan upsert Room]
  I --> F
  F --> J{Reputation CLEAN?}
  J -- Ya --> C
  J -- Tidak --> K[Baca auto-block DataStore]
  K --> L{Toggle ON dan policy block?}
  L -- Ya --> M[respondToCall reject]
  L -- Tidak --> N[respondToCall allow]
  M --> O[Insert log lokal]
  N --> O
  O --> P[Heads-up; overlay jika grant tersedia]
```

Bukti: [service:34](../app/src/main/java/id/calltag/app/service/CallTagScreeningService.kt#L34), [repo:68](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L68), [policy:56](../app/src/main/java/id/calltag/app/util/PhoneNormalizer.kt#L56). Timeout800ms mencakup request remote, bukan Room/DataStore/seluruh callback. Kegagalan DataStore setelah lookup belum dijaga fallback keseluruhan. Tidak ada lookup kontak aplikasi; cakupan callback untuk kontak tersimpan harus diuji menurut OS/perizinan. Toggle heads-up belum dibaca notifier.

## 4. AS-IS — report online, offline dan penolakan server

```mermaid
flowchart TD
  A[Form laporan] --> B[VM Loading dan validasi dasar]
  B --> C[Repo normalisasi; baca token jika ada]
  C --> D[POST report dengan deviceId default konstan]
  D --> E{Response data tersedia?}
  E -- Ya --> F[Cache phone_tag respons; Result Success]
  E -- Error atau null --> G[Agregasi lokal skor dan jumlah pelapor]
  G --> H[Upsert cache; DTO report_id lokal]
  H --> I[Result Success tanpa outbox raw report]
  I --> J[UI: tersimpan perangkat; layanan belum konfirmasi]
  F --> K[UI: Tag berhasil dikirim]
  J --> L[Tidak ada replay otomatis ke server]
  K --> M[Refresh stats dan lookup ulang]
```

Bukti: [repo:188](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L188), [VM:215](../app/src/main/java/id/calltag/app/ui/viewmodel/CallTagMainViewModel.kt#L215), [UI:486](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L486). “Offline” di fallback juga dapat berarti400/401/500 atau respons null; penyebab error hilang. Backend [report:63](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L63) tidak menolak invalid JWT; source dan runtime lokal mendukung batas ini.

## 5. AS-IS — sync offline bundle

```mermaid
flowchart TD
  A[Tombol sync Proteksi] --> B[since_version dari MAX row Room]
  B --> C[GET offline-bundle limit250]
  C --> D[Backend filter versi dan non-CLEAN; sort risiko]
  D --> E[Upsert batch ke Room]
  E --> F[Upsert national_emergency jika ada]
  F --> G[GET directory national dan local]
  G --> H[Baca statistik lokal]
  H --> I[Status berdasarkan Result bundle saja]
  I --> J[Tidak ada loop page atau cursor metadata terpisah]
```

Bukti: [repo:338](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L338), [VM:309](../app/src/main/java/id/calltag/app/ui/viewmodel/CallTagMainViewModel.kt#L309), [backend:521](https://github.com/CallTag/backend/blob/8676314/config/db.go#L521). Seed dan report lokal ikut memengaruhi MAX; latest_sync_version DTO tidak digunakan. Perubahan menjadi CLEAN tidak masuk feed ini; hasil directory di VM tidak menentukan pesan sukses sync.

## 6. AS-IS — direktori dan kanal resmi

```mermaid
flowchart TD
  A[Tab Darurat] --> B[Observe Room nasional dan lokal]
  B --> C[Filter kota dan jenis layanan di VM]
  C --> D{Aksi pengguna}
  D -- Telepon --> E[ACTION_DIAL nomor]
  D -- WhatsApp --> F[ACTION_VIEW wa.me dengan template]
  E --> G[Pengguna melanjutkan di aplikasi eksternal]
  F --> G
  H[Tab Hub resmi] --> I[API official channels atau seed]
  I --> J[Copy nomor; buka website atau dialer]
  J --> G
  G --> K[Tidak ada konfirmasi sambungan atau tiket lembaga di CallTag]
```

Bukti: [EmergencyScreen:524](../app/src/main/java/id/calltag/app/ui/screens/EmergencyScreen.kt#L524), [hub external:925](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L925). Pilihan kota hardcoded; tidak ada klaim pencarian lokasi terdekat berdasarkan GPS.

## 7. PROPOSED — moderasi minimum

```mermaid
flowchart TD
  A[Report atau dispute persisten] --> B[Antrean review]
  B --> C[Staf login dengan RBAC]
  C --> D[Panel: detail kasus dan bukti]
  D --> E{Keputusan dengan alasan}
  E -- Butuh info --> F[Status NEEDS_INFORMATION]
  F --> B
  E -- Tolak --> G[Status REJECTED; audit event]
  E -- Setujui koreksi --> H[Update reputasi atau whitelist versioned]
  H --> I[Audit actor dan before-after]
  I --> J[Terbitkan sync event termasuk demotion]
  J --> K[Mobile apply; invalidate stale cache]
  K --> L[Pemohon dapat melihat status kasus miliknya]
```

Panel dan endpoint admin **MISSING** pada baseline [router:23](https://github.com/CallTag/backend/blob/8676314/route/route.go#L23). Persistensi dispute/koreksi false positive adalah P0; panel + endpoint operasi minimum P1.

## 8. AS-IS — sequence lookup

```mermaid
sequenceDiagram
  actor User as Pengguna
  participant UI as Home
  participant VM as ViewModel
  participant Repo as Repository
  participant Room as Room
  participant API as Backend
  participant DB as Mongo atau memory
  User->>UI: Cari nomor
  UI->>VM: performPhoneLookup
  VM->>Repo: lookupPhoneFull
  Repo->>Room: getByPhone
  Room-->>Repo: cached atau null
  Repo->>API: GET phones/e164
  API->>DB: lookup tag dan laporan
  DB-->>API: data
  API-->>Repo: details atau gagal
  Repo->>API: GET phones/e164/reports
  API-->>Repo: reports atau gagal
  alt details found
    Repo->>Room: upsert mapped tag
    Repo-->>VM: LIVE_API, seed bila rincian kosong
  else cached tersedia
    Repo-->>VM: LOCAL_CACHE dan seed comments
  else tidak ada cache
    Repo-->>VM: synthetic CLEAN, skor0, LOCAL_CACHE
  end
  VM-->>UI: Success
  UI-->>User: Hasil dengan disclaimer untuk nol laporan
```

Sumber: [lookupPhoneFull:106](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L106). Rantai lookup emulator API34 → API lokal → MongoDB → UI/Room telah diuji memakai fixture sintetis non-seed (label AUDIT_SYNTHETIC, SUSPICIOUS, skor16, satu pelapor). Status VERIFIED_E2E terbatas pada jalur lokal ini; diagram tidak membuktikan seluruh cabang gagal maupun panggilan Telecom nyata.

## 9. AS-IS — sequence report dan sync

```mermaid
sequenceDiagram
  actor U as Pengguna
  participant M as Android
  participant R as Room
  participant A as Backend
  participant D as Mongo atau memory
  U->>M: Submit report
  M->>A: POST reports
  alt API dapat menyimpan
    A->>D: Insert raw report
    A->>D: Aggregate dan upsert PhoneTag
    A-->>M: 201 phone_tag
    M->>R: Upsert cache
  else error atau data null
    M->>R: Tambah skor/pelapor lokal dan sync_version
    M-->>U: Tersimpan perangkat dan belum dikonfirmasi
    Note over M,R: Tidak ada outbox raw report atau replay
  end
  U->>M: Tekan sync
  M->>R: MAX sync_version
  R-->>M: since_version
  M->>A: GET offline-bundle limit250
  A->>D: Cari non-CLEAN berubah dengan sort risiko
  D-->>A: Batch terbatas
  A-->>M: items dan latest_sync_version
  M->>R: Upsert items tanpa cursor metadata terpisah
  Note over M,A: Sync ini mengunduh bundle dan tidak mengunggah report offline
```

Sumber: [report:188](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L188), [sync:338](../app/src/main/java/id/calltag/app/data/repository/CallTagRepository.kt#L338), [DB:344](https://github.com/CallTag/backend/blob/8676314/config/db.go#L344). Insert report dan agregasi adalah operasi yang berbeda, bukan diagram transaksi atomik.

Satu submission form Android pada emulator lokal telah diverifikasi: tag AUDIT_MOBILE_E2E menampilkan “#Tag berhasil dikirim”, MongoDB menyimpan satu SpamReport dan Room memuat tag yang sama dengan status CLEAN, skor12.8. Bukti ini mencakup report online guest pada fixture lokal; bukan bukti autentikasi mobile, seluruh cabang report/offline/retry, atau pemblokiran telepon nyata.

## 10. AS-IS dan PROPOSED — state dispute

**AS-IS:** state berikut merupakan state UI/DTO; belum ada state machine kasus persisten di server.

```mermaid
stateDiagram-v2
  [*] --> Idle
  Idle --> Loading: Submit form valid
  Loading --> SuccessDTO: HTTP201 tanpa write dispute
  Loading --> LocalDTO: Error API ditelan
  Loading --> Error: Validasi lokal gagal
  SuccessDTO --> UnderReviewText: Status UNDER_REVIEW dari respons
  LocalDTO --> NotSentUI: UI mengenali fallback string
  note right of UnderReviewText
    Belum ada kasus persisten
    atau keputusan moderator
  end note
```

**PROPOSED P0/P1:** status hanya maju setelah write DB; pemohon hanya dapat mengakses kasusnya sendiri.

```mermaid
stateDiagram-v2
  [*] --> Draft
  Draft --> PendingLocal: Offline disimpan outbox
  Draft --> Submitted: API acknowledge setelah DB persist
  PendingLocal --> Submitted: Replay idempoten berhasil
  Submitted --> UnderReview: Moderator mengambil kasus
  UnderReview --> NeedsInformation: Bukti belum cukup
  NeedsInformation --> UnderReview: Pemohon melengkapi
  UnderReview --> Rejected: Alasan dan audit event
  UnderReview --> Accepted: Keputusan dan audit event
  Accepted --> CorrectionPublished: Reputasi direvisi dan sync event
  CorrectionPublished --> Resolved: Cache klien menerima koreksi
```

Sumber kondisi saat ini: [backend:98](https://github.com/CallTag/backend/blob/8676314/controller/report.go#L98), [UI:847](../app/src/main/java/id/calltag/app/ui/screens/ReportAndHubScreen.kt#L847).

## 11. AS-IS dan PROPOSED — state proteksi

**AS-IS:** status UI di Home hanya `role && permissions`; preferensi di Proteksi bukan pemeriksaan kapabilitas sistem.

```mermaid
stateDiagram-v2
  [*] --> CheckRoleAndPermissions
  CheckRoleAndPermissions --> UIInactive: Salah satu tidak terpenuhi
  CheckRoleAndPermissions --> UIActive: Keduanya terpenuhi
  UIInactive --> CheckRoleAndPermissions: Callback permintaan izin atau role
  UIActive --> PreferenceAutoBlockOff: Pengguna mematikan toggle
  UIActive --> PreferenceAutoBlockOn: Pengguna menyalakan toggle
  note right of UIActive
    Bukan bukti callback panggilan
    grant overlay atau cache fresh
  end note
```

**PROPOSED P0/P1:** bedakan kemampuan sistem, preferensi tindakan dan mutu data.

```mermaid
stateDiagram-v2
  [*] --> RoleMissing
  RoleMissing --> ReadyToScreen: Role diberikan
  ReadyToScreen --> RoleMissing: Role dicabut
  ReadyToScreen --> IdentificationOnly: Auto-block OFF
  ReadyToScreen --> BlockingEligible: Auto-block ON dan data fresh memenuhi policy
  BlockingEligible --> IdentificationOnly: Stale atau unknown atau whitelist
  IdentificationOnly --> BlockingEligible: Data fresh dan policy valid
  ReadyToScreen --> DegradedAlerts: Izin notifikasi atau overlay tidak tersedia
  DegradedAlerts --> ReadyToScreen: Grant tersedia
```

Sumber kondisi saat ini: [Home:414](../app/src/main/java/id/calltag/app/ui/screens/HomeScreen.kt#L414), [prefs:32](../app/src/main/java/id/calltag/app/data/preferences/CallTagPreferences.kt#L32), [notifier:62](../app/src/main/java/id/calltag/app/service/CallerIdNotifier.kt#L62). Target detail fail-open dan kelayakan blokir memerlukan keputusan policy serta pengujian OEM; diagram target tidak mengklaim kemampuan OS di luar callback screening.

## 12. AS-IS dan PROPOSED — use case

Mermaid tidak memiliki sintaks use-case UML native yang dipakai di sini; flowchart berikut menunjukkan aktor, batas sistem dan use case secara eksplisit.

```mermaid
flowchart LR
  U[Pengguna]
  O[Pemilik nomor]
  OS[Android Telecom]
  E[Aplikasi telepon atau kanal resmi]
  MOD[Moderator: PROPOSED]
  subgraph ASIS[CallTag AS-IS]
    LOOK([Cari nomor])
    SCR([Screen panggilan dan auto-block])
    REP([Lapor komunitas])
    DIR([Baca direktori offline])
    HUB([Buka panduan dan tautan resmi])
    SYN([Unduh bundle manual])
    DSP([Form dispute; belum persisten])
  end
  subgraph TARGET[PROPOSED P0 dan P1]
    AUTH([Onboarding identitas dan write auth])
    QUEUE([Report outbox idempoten])
    REVIEW([Review dispute persisten])
    FIX([Koreksi reputasi dan publikasi sync])
    AUDIT([Jejak keputusan staf])
  end
  U --> LOOK
  U --> REP
  U --> DIR
  U --> HUB
  U --> SYN
  OS --> SCR
  O --> DSP
  DIR --> E
  HUB --> E
  U -.-> AUTH
  U -.-> QUEUE
  O -.-> REVIEW
  MOD -.-> REVIEW
  REVIEW -.-> FIX
  MOD -.-> AUDIT
```

AS-IS ditelusuri dari [MainScaffold:204](../app/src/main/java/id/calltag/app/ui/MainScaffold.kt#L204), [ApiService](../app/src/main/java/id/calltag/app/data/remote/ApiService.kt#L37) dan [router backend](https://github.com/CallTag/backend/blob/8676314/route/route.go#L23). Use case yang putus-putus adalah backlog, bukan fitur tersedia.
