# Dokumentasi produk dan audit CallTag

Audit 10 November 2026 memperbarui [PRD 1.1.0](../PRD.md) berdasarkan empat repository pada commit tetap. Hasilnya **PARTIALLY_VERIFIED untuk lingkungan lokal**; dokumentasi ini tidak mengklaim aplikasi sudah siap produksi. [DESIGN](../DESIGN.md) dan [AGENTS](../AGENTS.md) tetap menjadi pedoman visual/engineering.

| Dokumen | Kegunaan |
|---|---|
| [AUDIT_REPORT](AUDIT_REPORT.md) | Verdict, branch/commit, bukti, 10 temuan, risk register dan keterbatasan |
| [INTEGRATION_MATRIX](INTEGRATION_MATRIX.md) | Seluruh 11 endpoint, konsumen, DTO, auth, response dan storage |
| [FEATURE_GAP_ANALYSIS](FEATURE_GAP_ANALYSIS.md) | Journey lintas modul, gap, keputusan admin/moderator minimum |
| [SYSTEM_ARCHITECTURE](SYSTEM_ARCHITECTURE.md) | Context/container/component, DFD0/1 dan deployment |
| [DATABASE_DESIGN](DATABASE_DESIGN.md) | Mongo aktual/usulan, Room, field/index, ERD dan migrasi/retention |
| [FLOWS_AND_DIAGRAMS](FLOWS_AND_DIAGRAMS.md) | Onboarding, lookup, screening, report, sync, directory, moderation, sequence/state |
| [API_CONTRACT_AUDIT](API_CONTRACT_AUDIT.md) | Mismatch OpenAPI–Android–backend–website dan rancangan koreksi |
| [QA_TEST_PLAN](QA_TEST_PLAN.md) | Command, environment, expected/actual, PASS/FAIL/BLOCKED, FR/NFR traceability |
| [IMPLEMENTATION_ROADMAP](IMPLEMENTATION_ROADMAP.md) | Backlog P0–P3, milestone dan pembagian dua orang |
| [ARCHITECTURE_DECISIONS](ARCHITECTURE_DECISIONS.md) | ADR aktual/usulan dan keputusan terbuka |
| [DOC_VALIDATION](DOC_VALIDATION.md) | Hasil pengecekan tautan file dan parse/render Mermaid |

Dokumen desain dan verifikasi frontend sebelumnya tetap tersedia: [Verifikasi frontend Android](frontend-refresh-verification.md), [rencana refresh](superpowers/plans/2026-11-10-frontend-refresh.md). Bukti audit menggunakan commit tetap; diagram bertanda PROPOSED tidak boleh dibaca sebagai kode yang sudah tersedia.
