package id.calltag.app

import com.google.gson.Gson
import com.google.gson.JsonParser
import id.calltag.app.data.remote.dto.AuthLoginResponseDto
import id.calltag.app.data.remote.dto.DisputeRequestDto
import id.calltag.app.data.remote.dto.DisputeResponseDto
import id.calltag.app.data.remote.dto.LookupDataDto
import id.calltag.app.data.remote.dto.MeResponseDto
import id.calltag.app.data.remote.dto.PhoneReportsDataDto
import id.calltag.app.data.remote.dto.PublicStatsDto
import id.calltag.app.data.remote.dto.ReportRequestDto
import id.calltag.app.data.remote.dto.ReportResponseDto
import id.calltag.app.data.remote.dto.ReputationLevels
import id.calltag.app.data.remote.dto.ScamCategories
import id.calltag.app.data.remote.dto.SyncChangesResponseDto
import id.calltag.app.data.repository.CallTagRepository
import id.calltag.app.data.repository.LookupAvailability
import id.calltag.app.data.repository.LookupDisplayStatus
import id.calltag.app.util.PhoneNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifikasi kontrak JSON lintas-repositori (T-602) serta kasus konservatif K-05 & K-06 (T-603).
 */
class ContractParityTest {

    private val gson = Gson()

    private fun locateOpenApiFile(): File {
        val candidates = listOf(
            File("../../apidocs/openapi.json"),
            File("../apidocs/openapi.json"),
            File("apidocs/openapi.json")
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("apidocs/openapi.json tidak ditemukan dari cwd=${File(".").absolutePath}")
    }

    @Test
    fun taxonomyAndReputationEnumsMatchOpenApiSpec() {
        val openApi = JsonParser.parseString(locateOpenApiFile().readText()).asJsonObject
        val schemas = openApi
            .getAsJsonObject("components")
            .getAsJsonObject("schemas")

        assertEquals("1.2.0", openApi.getAsJsonObject("info").get("version").asString)

        val openApiCategories = schemas
            .getAsJsonObject("CategoryCode")
            .getAsJsonArray("enum")
            .map { it.asString }
            .toSet()
        assertEquals(ScamCategories.ALL, openApiCategories)
        assertTrue(ScamCategories.INFO_TAG in openApiCategories)

        val openApiReputations = schemas
            .getAsJsonObject("ReputationLevel")
            .getAsJsonArray("enum")
            .map { it.asString }
            .toSet()
        assertEquals(ReputationLevels.ALL, openApiReputations)
    }

    @Test
    fun gsonDtosDeserializeActualBackendPayloads() {
        // 1. Lookup verified + INFO_TAG payload parity
        val lookupJson = """
            {
              "phone_e164": "+6221500111",
              "carrier": "PSTN Jakarta",
              "found": true,
              "reputation_level": "CLEAN",
              "risk_score": 0.0,
              "spam_score": 0.0,
              "is_danger": false,
              "is_official_blacklisted": false,
              "is_whitelisted_verified": true,
              "verified_label": "OFFICIAL",
              "verified_source_url": "https://resmi.example.test",
              "verified_at": "2026-11-16T00:00:00Z",
              "primary_category": "INFO_TAG",
              "primary_category_label": "Informasi Layanan / Peran",
              "top_community_label": "CS Resmi Bank",
              "tags": [{"tag": "CS Resmi Bank", "count": 4}],
              "category_breakdown": {"PINJOL_DC": 0},
              "unique_reporters": 2,
              "total_reports": 2,
              "recent_reports": [
                {
                  "id": "rep-1",
                  "phone_e164": "+6221500111",
                  "category_code": "INFO_TAG",
                  "custom_tag": "CS Resmi Bank",
                  "comment": "Konfirmasi layanan resmi",
                  "call_type": "WHATSAPP_CALL",
                  "created_at": "2026-11-16T01:00:00Z"
                }
              ],
              "last_reported_at": "2026-11-16T01:00:00Z"
            }
        """.trimIndent()
        val lookup = gson.fromJson(lookupJson, LookupDataDto::class.java)
        assertEquals("+6221500111", lookup.phoneE164)
        assertTrue(lookup.isWhitelistedVerified)
        assertEquals("OFFICIAL", lookup.verifiedLabel)
        assertEquals("INFO_TAG", lookup.primaryCategory)
        assertEquals("WHATSAPP_CALL", lookup.recentReports.first().callType)

        // 2. Auth + /me + Refresh + Dispute + Report + SyncChanges + PublicStats
        val authJson = """
            {
              "token": "jwt.token.abc",
              "access_token": "jwt.token.abc",
              "refresh_token": "rtok_xyz",
              "token_type": "Bearer",
              "expires_at": "2026-11-17T00:00:00Z",
              "user": {"id": "usr_1", "phone_hash": "hash_1", "display_name": "Warga", "role": "USER", "trust_weight": 1.0}
            }
        """.trimIndent()
        val auth = gson.fromJson(authJson, AuthLoginResponseDto::class.java)
        assertEquals("rtok_xyz", auth.refreshToken)
        assertEquals("usr_1", auth.user.id)

        val meJson = """
            {
              "id": "usr_1",
              "display_name": "Warga",
              "phone_mask": "+62812****7890",
              "role": "USER",
              "status": "ACTIVE",
              "trust_weight": 1.0,
              "quota_remaining": {"reports_5m_remaining": 3, "reports_24h_remaining": 10, "disputes_24h_remaining": 3},
              "created_at": "2026-11-16T00:00:00Z"
            }
        """.trimIndent()
        val me = gson.fromJson(meJson, MeResponseDto::class.java)
        assertEquals(3, me.quotaRemaining.reports5mRemaining)
        assertEquals(10, me.quotaRemaining.reports24hRemaining)

        val reportRespJson = """
            {
              "report_id": "672a10000000000000000001",
              "phone_tag": {
                "phone_e164": "+6281399990001",
                "carrier": "Telkomsel",
                "reputation_level": "SPAM",
                "risk_score": 48.0,
                "primary_category": "PINJOL_DC",
                "primary_category_label": "Pinjol Ilegal",
                "is_danger": true,
                "top_community_label": "DC Pinjol",
                "tags": [{"tag": "DC Pinjol", "count": 3}],
                "unique_reporters": 3,
                "total_reports": 3,
                "sync_version": 12
              },
              "recommended_official_hub": [
                {
                  "channel_slug": "ojk-kontak-157",
                  "title": "Kontak OJK 157",
                  "authority_name": "OJK",
                  "related_categories": ["PINJOL_DC"],
                  "description": "Pengaduan pinjol",
                  "action_links": {"website_url": "https://kontak157.ojk.go.id", "phone_dial": "157"},
                  "required_evidence_checklist": ["Screenshot tagihan"],
                  "step_by_step_guide": ["Catat nomor"]
                }
              ]
            }
        """.trimIndent()
        val reportResp = gson.fromJson(reportRespJson, ReportResponseDto::class.java)
        assertEquals(48.0, reportResp.phoneTag.spamScore, 0.001)
        assertEquals("ojk-kontak-157", reportResp.recommendedOfficialHub.first().channelSlug)

        val disputeJson = """
            {
              "dispute_id": "DSP_6afaf4bb3ca87333c090fba3",
              "phone_e164": "+6282144556677",
              "owner_name": "PT Logistik Nusantara",
              "reason": "Nomor resmi kantor cabang",
              "status": "UNDER_REVIEW",
              "tracking_token": "trk_abc123",
              "submitted_at": "2026-11-16T00:00:00Z"
            }
        """.trimIndent()
        val dispute = gson.fromJson(disputeJson, DisputeResponseDto::class.java)
        assertEquals("UNDER_REVIEW", dispute.status)
        assertEquals("trk_abc123", dispute.trackingToken)

        val syncJson = """
            {
              "generation": 2,
              "count": 1,
              "items": [
                {
                  "seq": 42,
                  "generation": 2,
                  "op": "UPSERT",
                  "kind": "PHONE_TAG",
                  "key": "+6281299887766",
                  "payload": {"reputation_level": "DANGER_SCAM"},
                  "at": "2026-11-16T00:00:00Z"
                }
              ],
              "next_cursor": "2:42",
              "has_more": true,
              "synced_at": "2026-11-16T00:00:00Z"
            }
        """.trimIndent()
        val sync = gson.fromJson(syncJson, SyncChangesResponseDto::class.java)
        assertEquals(2, sync.generation)
        assertTrue(sync.hasMore)
        assertEquals("2:42", sync.nextCursor)
        assertEquals("UPSERT", sync.items.first().op)

        val statsJson = """
            {
              "total_tracked_numbers": 12,
              "danger_scam_count": 3,
              "spam_count": 4,
              "suspicious_count": 5,
              "emergency_agencies": 23,
              "official_channels_count": 5,
              "category_totals": {"APK_SNIFFING": 6}
            }
        """.trimIndent()
        val stats = gson.fromJson(statsJson, PublicStatsDto::class.java)
        assertEquals(12, stats.totalTrackedNumbers)
        assertEquals(6, stats.categoryTotals["APK_SNIFFING"])

        val reportsJson = """
            {
              "phone_e164": "+6281299887766",
              "reputation_level": "DANGER_SCAM",
              "risk_score": 92.5,
              "top_community_label": "Penipu APK",
              "unique_reporters": 20,
              "total_reports": 20,
              "category_breakdown": {"APK_SNIFFING": 20},
              "reports": [{"phone_e164": "+6281299887766", "category_code": "APK_SNIFFING", "custom_tag": "Penipu APK", "comment": "[REDACTED_NIK]", "call_type": "WHATSAPP_CHAT", "created_at": "2026-11-16T00:00:00Z"}]
            }
        """.trimIndent()
        val phoneReports = gson.fromJson(reportsJson, PhoneReportsDataDto::class.java)
        assertEquals("[REDACTED_NIK]", phoneReports.reports.first().comment)

        // Serialisasi request DTO memakai snake_case sesuai kontrak
        val reqReportMap = JsonParser.parseString(
            gson.toJson(ReportRequestDto("08123456789", "INFO_TAG", "Kurir", "Mengantar", "WHATSAPP_CALL"))
        ).asJsonObject
        assertEquals("INFO_TAG", reqReportMap.get("category_code").asString)
        assertEquals("WHATSAPP_CALL", reqReportMap.get("call_type").asString)

        val reqDisputeMap = JsonParser.parseString(
            gson.toJson(DisputeRequestDto("08123456789", "PT Uji", "Resmi", "cs@uji.id"))
        ).asJsonObject
        assertEquals("PT Uji", reqDisputeMap.get("owner_name").asString)
    }

    @Test
    fun offlineCacheMissNeverClaimsCleanAndSeededOrSpamNeverAutoBlocks() {
        // K-05 (FR-002): Cache miss + API timeout/error harus menghasilkan OFFLINE, bukan CLEAN/AMAN
        val offlineMiss = CallTagRepository.resolveLookupResult(
            e164 = "+6281988887777",
            availability = LookupAvailability.UNAVAILABLE,
            entity = null
        )
        assertEquals(LookupDisplayStatus.OFFLINE, offlineMiss.displayStatus)
        assertEquals(LookupAvailability.UNAVAILABLE, offlineMiss.availability)
        assertNull(offlineMiss.verifiedLabel)

        // K-06 (FR-003): Level SPAM maupun cache FIXTURE tidak boleh memicu auto-block
        val now = 1_760_000_000_000L
        val spamBlock = PhoneNormalizer.shouldAutoBlock(
            reputationLevel = ReputationLevels.SPAM,
            autoBlockEnabled = true,
            source = "SERVER",
            fetchedAtEpochMs = now,
            nowEpochMs = now,
            phoneE164 = "+6281300001111"
        )
        assertFalse(spamBlock)

        val fixtureBlock = PhoneNormalizer.shouldAutoBlock(
            reputationLevel = ReputationLevels.DANGER_SCAM,
            autoBlockEnabled = true,
            source = "FIXTURE",
            fetchedAtEpochMs = now,
            nowEpochMs = now,
            phoneE164 = "+6281300001111"
        )
        assertFalse(fixtureBlock)
    }
}
