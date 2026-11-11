# Rencana audit integrasi dan pembaruan PRD

**Goal:** Menghasilkan baseline faktual empat repo, bukti runtime lokal, PRD revisi terarah, dan draft PR dokumentasi.

**Architecture:** Audit as-is mendahului rancangan to-be. Bukti menggunakan commit tetap dan path:baris; MongoDB pengujian terisolasi, data sintetis, tanpa menyentuh deployment.

**Spec:** Instruksi audit lengkap dari pengguna pada lampiran 10 November 2026; output berada di `PRD.md` dan `docs/`.

## Batas kerja

- Tidak mengubah source aplikasi, endpoint, atau OpenAPI; tidak merge otomatis.
- Pertahankan `DESIGN.md`, `AGENTS.md`, visi, persona, dan taksonomi PRD.
- Rahasiakan kredensial, token, dan data pengguna; nomor uji hanya fixture lokal.
- Status VERIFIED_E2E dibatasi pada rantai yang benar-benar diuji; build bukan bukti integrasi.

## Langkah dan keluaran

- [x] Petakan main/feature, commit/tanggal, PR dan deployment; baca seluruh PRD/DESIGN/AGENTS.
- [x] Audit Android, backend/MongoDB, kontrak OpenAPI dan website; catat risiko Critical/High dengan bukti.
- [x] Jalankan Go tests/vet, API dengan MongoDB lokal dan fallback, Android build/emulator bila tersedia.
- [x] Tulis AUDIT_REPORT, INTEGRATION_MATRIX, FEATURE_GAP_ANALYSIS, SYSTEM_ARCHITECTURE, DATABASE_DESIGN, FLOWS_AND_DIAGRAMS, API_CONTRACT_AUDIT, QA_TEST_PLAN, IMPLEMENTATION_ROADMAP, ARCHITECTURE_DECISIONS, indeks docs.
- [x] Revisi PRD in-place menjadi 1.1.0; bedakan as-is, target MVP, post-MVP; tautkan detail, hasil uji, Mermaid, acceptance criteria dan backlog tim dua orang.
- [x] Validasi tautan Markdown dan sintaks Mermaid, review silang fakta, commit/push branch dokumentasi dan buat draft PR.

Penyerahan selesai pada 12 November 2026: [draft PR #1](https://github.com/CallTag/CallTag/pull/1), head `codex/integration-audit-prd`, base `codex/frontend-mobile-refresh`. Commit audit utama `373e38a`; scope hanya dokumentasi. Bukti runtime tetap bertanggal 10 November 2026; tidak diklaim diuji ulang pada tanggal penyerahan. Tidak dilakukan merge atau perbaikan source aplikasi.

## Fokus review

1. HTTP 201/Room fallback tidak otomatis berarti persistensi server.
2. UNKNOWN dan CLEAN tidak boleh disamakan dengan verifikasi nomor.
3. Semua fitur branch tidak dianggap sudah deploy.
4. Cursor sync, dedup, identitas pelapor dan auto-block perlu bukti spesifik.
5. Jalur API→Mongo tidak membuktikan mobile→Mongo maupun panggilan telepon nyata.
