# 07. Kontrak API

Status: **PROPOSED** untuk kolom "target"; kolom "as-is" merujuk [INTEGRATION_MATRIX](../INTEGRATION_MATRIX.md) dan [API_CONTRACT_AUDIT](../API_CONTRACT_AUDIT.md). Prinsip: **aditif dan kompatibel mundur** pada `/api/v1`; perubahan breaking hanya lewat `/api/v2` bila diperlukan.

## 1. Konvensi

- Envelope: `{success, code, message, data, timestamp}` dipertahankan; error memakai `data.error = {code, detail, request_id}`.
- Autentikasi: `Authorization: Bearer <access_token>`. Tidak valid → `401`; role kurang → `403`; kuota → `429` + `Retry-After`.
- POST yang membuat sumber daya wajib `Idempotency-Key` (UUID); ulangan dengan kunci sama mengembalikan respons semula (`200`/`201` yang sama, bukan duplikat).
- Batas body 16 KB, `comment` ≤ 500 karakter, `custom_tag` ≤ 40.
- Rate limit awal (nilai awal, perlu dikalibrasi): guest 30 lookup/menit/IP; user 60 lookup/menit, 10 laporan/24 jam, 3/5 menit; dispute 3/24 jam; login staf 5/15 menit.

## 2. Endpoint existing (11) dan target

| # | Method + URL | Fungsi | Role | Status as-is | Perubahan target |
|---|---|---|---|---|---|
| 1 | `GET /health` | Liveness | publik | PARTIAL (selalu 200) | Pecah: `GET /healthz` liveness, `GET /readyz` ping Mongo (503 bila gagal); `/health` tetap alias liveness |
| 2 | `POST /api/v1/auth/device` | Daftar akun perangkat | publik, kuota | PARTIAL (tanpa bukti, tidak dipanggil app) | Body `{install_id, display_name?, attestation?}`; `phone` opsional dan diabaikan; kembalikan `access_token`, `refresh_token`, `expires_at` |
| 3 | `GET /api/v1/phones/{e164}` | Lookup | guest+ | PARTIAL | Tambah `availability`, `source`, `fetched_at`, `confidence`, `verified_label`; `found=false` tidak lagi disertai `CLEAN` palsu (level `null`) |
| 4 | `GET /api/v1/phones/{e164}/reports` | Laporan teredaksi | guest+ | PARTIAL (raw `SpamReport` bocor, R11) | DTO publik: `category_code`, `tag`, `comment_sanitized`, `created_at` (dibulatkan ke hari); tanpa `reporter_user_id`; paginasi `limit/cursor` |
| 5 | `POST /api/v1/reports` | Kirim laporan | User | PARTIAL (guest diterima, tidak idempoten) | Wajib token valid + `Idempotency-Key`; `status` respons: `ACCEPTED` atau `HELD_FOR_REVIEW`; kembalikan `report_id` = `_id` persisten |
| 6 | `POST /api/v1/disputes` | Ajukan sanggahan | User (P1: guest dengan kontak) | PARTIAL (201 tanpa tulis) | Simpan ke `moderation_cases` sebelum `201`; kembalikan `dispute_id`, `tracking_token` |
| 7 | `GET /api/v1/emergency/national` | Nasional | publik | PARTIAL | Tambah `source_url`, `verified_at` |
| 8 | `GET /api/v1/emergency/local` | Lokal | publik | PARTIAL | Filter provinsi/kota/jenis; field sumber sama |
| 9 | `GET /api/v1/official-channels` | Kanal resmi | publik | PARTIAL | Tidak berubah selain `verified_at` |
| 10 | `GET /api/v1/sync/offline-bundle` | Sync lama | publik | PARTIAL (melewatkan data) | **Dipertahankan sementara** (deprecated); digantikan #N5 |
| 11 | `GET /api/v1/stats/public` | Statistik | publik | PARTIAL (seed bercampur) | Hanya angka nyata; sertakan `as_of` |

## 3. Endpoint tambahan (dengan alasan fungsional)

| ID | Method + URL | Alasan | Role | Request ringkas | Respons / error | Basis data | Prioritas |
|---|---|---|---|---|---|---|---|
| N1 | `POST /api/v1/auth/refresh` | Token pendek butuh pembaruan | token refresh | `{refresh_token}` | `200` token baru; `401` | `refresh_tokens` | P0 |
| N2 | `GET /api/v1/me` | Profil dan status akun | User | - | `200` `{id, role, status, quota_remaining}` | `users` | P1 |
| N3 | `DELETE /api/v1/me` | Hak hapus data (FR-017) | User | - | `202` permintaan tercatat | `users.status=DELETION_REQUESTED` | P1 |
| N4 | `GET /api/v1/me/reports` | Riwayat laporan pribadi | User | `limit,cursor` | daftar milik sendiri | `spam_reports` | P2 |
| N5 | `GET /api/v1/sync/changes?cursor=&limit=` | Delta aman | guest+ | cursor opak | `{items[{op,kind,key,payload}], next_cursor, has_more, generation}`; `410/409 FULL_RESYNC_REQUIRED` | `sync_changes` | P0 |
| N6 | `GET /api/v1/disputes/{id}` | Lacak status dispute | pemilik/token pelacak | header `X-Tracking-Token` | status, keputusan publik | `moderation_cases` | P1 |
| N7 | `POST /api/v1/admin/login`, `POST /api/v1/admin/logout` | Sesi staf | publik→staf | email, password, TOTP | cookie sesi | `users` staf | P1 |
| N8 | `GET /api/v1/admin/cases?status=&type=` | Antrean | MOD+ | | daftar kasus | `moderation_cases` | P1 |
| N9 | `GET /api/v1/admin/cases/{id}` | Detail + bukti | MOD+ | | kasus + laporan terkait | beberapa | P1 |
| N10 | `PATCH /api/v1/admin/cases/{id}` | Putuskan | MOD+ | `{outcome, reason, version}` | `200`; `409` konflik versi | cases, phone_tags, audit, sync | P1 |
| N11 | `GET /api/v1/admin/numbers/{e164}` | Lihat profil lengkap | MOD+ | | profil + riwayat | phone_tags, audit | P1 |
| N12 | `PUT/DELETE /api/v1/admin/verified-numbers/{e164}` | Whitelist/bisnis | ADMIN | `{kind,source_url,expires_at}` | | verified_numbers, audit, sync | P1 |
| N13 | `POST/PUT/DELETE /api/v1/admin/emergency`, `/admin/official-channels` | Kurasi direktori | ADMIN | entri + sumber | | direktori, audit, sync | P1 |
| N14 | `GET /api/v1/admin/audit?from=&to=&actor=` | Audit | ADMIN | | daftar | `audit_logs` | P1 |
| N15 | `POST /api/v1/owner/claims` | Verifikasi pemilik | User | | | `owner_claims` | **P3 ditunda** |

Tidak ada endpoint untuk "lookup nama pemilik" atau penyedia eksternal.

## 4. Pemetaan nama lintas lapisan (sumber kebenaran: OpenAPI)

| Konsep | JSON/OpenAPI | Go (`model`) | Mongo (BSON) | Kotlin DTO | Room |
|---|---|---|---|---|---|
| Nomor | `phone_e164` | `PhoneE164` | `phone_e164` | `phoneE164` | `phone_e164` |
| Skor | `risk_score` (alias lama `spam_score` hanya dibaca) | `RiskScore` | `risk_score` | `spamScore` (`alternate`) | `spam_score` → ganti nama `risk_score` pada migrasi |
| Level | `reputation_level` nullable | `ReputationLevel` | `reputation_level` | `reputationLevel: String?` | `reputation_level` nullable |
| Ketersediaan | `availability` | `Availability` (turunan) | tidak disimpan | `availability` | `availability` |
| Tag | `custom_tag` (alias `tag_name`) | `CustomTag` | `custom_tag` | `tagName` | - |

Enum `call_type` disatukan: `CELLULAR_CALL`, `WHATSAPP_CALL`, `WHATSAPP_CHAT`, `SMS` (website dikoreksi, menutup ketidakcocokan di INTEGRATION_MATRIX).

## 5. Matriks integrasi lintas repository (target)

| Endpoint | Backend | OpenAPI | Android | Website | Contract test |
|---|---|---|---|---|---|
| 1 sampai 11 | wajib sama | skema respons lengkap (bukan objek kosong) | DTO dari skema | modul fetch dari skema | Dredd/Schemathesis (PROPOSED) atau test Go yang memvalidasi respons terhadap `openapi.json` |
| N1 sampai N6 | ya | ya | ya | N5/N6 tidak | ya |
| N7 sampai N14 | ya | tag `Admin` terpisah | tidak | tidak | ya |

`apidocs` simulator (mode mock) wajib mengikuti bentuk respons nyata atau diberi label jelas "simulasi" (temuan A-docs di API_CONTRACT_AUDIT).

