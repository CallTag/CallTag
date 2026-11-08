package id.calltag.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Konstanta 4 Level Reputasi (`reputation_level`) sesuai PRD Bagian 4.2 & 4.3.
 */
object ReputationLevels {
    const val CLEAN = "CLEAN"
    const val SUSPICIOUS = "SUSPICIOUS"
    const val SPAM = "SPAM"
    const val DANGER_SCAM = "DANGER_SCAM"

    val ALL = setOf(CLEAN, SUSPICIOUS, SPAM, DANGER_SCAM)
}

/**
 * Konstanta 6 Kode Kategori Taksonomi Scam Khas Indonesia (`category_code`) sesuai PRD Tabel 4.1.
 */
object ScamCategories {
    const val PINJOL_DC = "PINJOL_DC"
    const val APK_SNIFFING = "APK_SNIFFING"
    const val IMPERSONATION = "IMPERSONATION"
    const val JUDOL = "JUDOL"
    const val SOCIAL_ENG = "SOCIAL_ENG"
    const val TELEMARKETING = "TELEMARKETING"

    val ALL = setOf(
        PINJOL_DC,
        APK_SNIFFING,
        IMPERSONATION,
        JUDOL,
        SOCIAL_ENG,
        TELEMARKETING
    )
}

data class ApiResponseDto<T>(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("code") val code: Int = 200,
    @SerializedName("message") val message: String = "",
    @SerializedName("data") val data: T? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class TagCountDto(
    @SerializedName("tag") val tag: String = "",
    @SerializedName("count") val count: Int = 0
)

data class SpamReportItemDto(
    @SerializedName("phone_e164") val phoneE164: String = "",
    @SerializedName("category_code") val categoryCode: String = ScamCategories.TELEMARKETING,
    @SerializedName(value = "custom_tag", alternate = ["tag_name"]) val tagName: String = "",
    @SerializedName("comment") val comment: String = "",
    @SerializedName("call_type") val callType: String = "CELLULAR_CALL",
    @SerializedName("created_at") val createdAt: String = ""
)

data class LookupDataDto(
    @SerializedName("phone_e164") val phoneE164: String = "",
    @SerializedName("carrier") val carrier: String = "",
    @SerializedName("found") val found: Boolean = false,
    @SerializedName("reputation_level") val reputationLevel: String = ReputationLevels.CLEAN,
    @SerializedName(value = "risk_score", alternate = ["spam_score"]) val spamScore: Double = 0.0,
    @SerializedName("is_danger") val isDanger: Boolean = false,
    @SerializedName("is_official_blacklisted") val isOfficialBlacklisted: Boolean = false,
    @SerializedName("is_whitelisted_verified") val isWhitelistedVerified: Boolean = false,
    @SerializedName("primary_category") val primaryCategory: String = "",
    @SerializedName("primary_category_label") val primaryCategoryLabel: String = "",
    @SerializedName("top_community_label") val topCommunityLabel: String = "",
    @SerializedName("tags") val tags: List<TagCountDto> = emptyList(),
    @SerializedName("category_breakdown") val categoryBreakdown: Map<String, Int> = emptyMap(),
    @SerializedName("unique_reporters") val uniqueReporters: Int = 0,
    @SerializedName("total_reports") val totalReports: Int = 0,
    @SerializedName("recent_reports") val recentReports: List<SpamReportItemDto> = emptyList(),
    @SerializedName("last_reported_at") val lastReportedAt: String? = null
)

data class PhoneReportsDataDto(
    @SerializedName("phone_e164") val phoneE164: String = "",
    @SerializedName("reputation_level") val reputationLevel: String = ReputationLevels.CLEAN,
    @SerializedName(value = "risk_score", alternate = ["spam_score"]) val spamScore: Double = 0.0,
    @SerializedName("top_community_label") val topCommunityLabel: String = "",
    @SerializedName("unique_reporters") val uniqueReporters: Int = 0,
    @SerializedName("total_reports") val totalReports: Int = 0,
    @SerializedName("category_breakdown") val categoryBreakdown: Map<String, Int> = emptyMap(),
    @SerializedName(value = "reports", alternate = ["recent_reports"]) val reports: List<SpamReportItemDto> = emptyList()
)

data class ReportRequestDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("category_code") val categoryCode: String,
    @SerializedName("custom_tag") val customTag: String,
    @SerializedName("comment") val comment: String,
    @SerializedName("call_type") val callType: String = "CELLULAR_CALL"
)

data class PhoneTagDto(
    @SerializedName("phone_e164") val phoneE164: String = "",
    @SerializedName("carrier") val carrier: String = "",
    @SerializedName("reputation_level") val reputationLevel: String = ReputationLevels.CLEAN,
    @SerializedName(value = "risk_score", alternate = ["spam_score"]) val spamScore: Double = 0.0,
    @SerializedName("primary_category") val primaryCategory: String = "",
    @SerializedName("primary_category_label") val primaryCategoryLabel: String = "",
    @SerializedName("is_danger") val isDanger: Boolean = false,
    @SerializedName("is_official_blacklisted") val isOfficialBlacklisted: Boolean = false,
    @SerializedName("is_whitelisted_verified") val isWhitelistedVerified: Boolean = false,
    @SerializedName("top_community_label") val topCommunityLabel: String = "",
    @SerializedName("tags") val tags: List<TagCountDto> = emptyList(),
    @SerializedName("unique_reporters") val uniqueReporters: Int = 0,
    @SerializedName("total_reports") val totalReports: Int = 0,
    @SerializedName("sync_version") val syncVersion: Long = 0L
)

data class OfficialChannelLinksDto(
    @SerializedName("website_url") val websiteUrl: String = "",
    @SerializedName("phone_dial") val phoneDial: String? = null,
    @SerializedName("whatsapp_url") val whatsappUrl: String? = null,
    @SerializedName("email") val email: String? = null
)

data class OfficialChannelDto(
    @SerializedName("channel_slug") val channelSlug: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("authority_name") val authorityName: String = "",
    @SerializedName("related_categories") val relatedCategories: List<String> = emptyList(),
    @SerializedName("description") val description: String = "",
    @SerializedName("action_links") val actionLinks: OfficialChannelLinksDto = OfficialChannelLinksDto(),
    @SerializedName("required_evidence_checklist") val requiredEvidenceChecklist: List<String> = emptyList(),
    @SerializedName("step_by_step_guide") val stepByStepGuide: List<String> = emptyList()
)

data class ReportResponseDto(
    @SerializedName("report_id") val reportId: String = "",
    @SerializedName("phone_tag") val phoneTag: PhoneTagDto = PhoneTagDto(),
    @SerializedName("recommended_official_hub") val recommendedOfficialHub: List<OfficialChannelDto> = emptyList()
)

data class DisputeRequestDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("owner_name") val ownerName: String,
    @SerializedName("reason") val reason: String,
    @SerializedName("contact_ref") val contactRef: String
)

data class DisputeResponseDto(
    @SerializedName("dispute_id") val disputeId: String = "",
    @SerializedName("phone_e164") val phoneE164: String = "",
    @SerializedName("owner_name") val ownerName: String = "",
    @SerializedName("status") val status: String = "UNDER_REVIEW",
    @SerializedName("submitted_at") val submittedAt: String = ""
)

data class EmergencyContactsDetailDto(
    @SerializedName("short_code") val shortCode: String? = null,
    @SerializedName("phone_numbers") val phoneNumbers: List<String> = emptyList(),
    @SerializedName("whatsapp_number") val whatsappNumber: String? = null,
    @SerializedName("whatsapp_template") val whatsappTemplate: String? = null
)

data class EmergencyDirectoryDto(
    @SerializedName("code") val code: String = "",
    @SerializedName("scope") val scope: String = "LOCAL",
    @SerializedName("agency_type") val agencyType: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("province") val province: String? = null,
    @SerializedName("city_regency") val cityRegency: String? = null,
    @SerializedName("contacts") val contacts: EmergencyContactsDetailDto = EmergencyContactsDetailDto(),
    @SerializedName("capabilities") val capabilities: List<String> = emptyList(),
    @SerializedName("address") val address: String? = null,
    @SerializedName("is_24_hours") val is24Hours: Boolean = true
)

data class EmergencyResponseDto(
    @SerializedName("filter_province") val filterProvince: String? = null,
    @SerializedName("filter_city") val filterCity: String? = null,
    @SerializedName("national_numbers") val nationalNumbers: List<EmergencyDirectoryDto> = emptyList(),
    @SerializedName("local_directory") val localDirectory: List<EmergencyDirectoryDto> = emptyList(),
    @SerializedName("official_channels") val officialChannels: List<OfficialChannelDto> = emptyList()
)

data class SpamSyncItemDto(
    @SerializedName("phone_e164") val phoneE164: String = "",
    @SerializedName("reputation_level") val reputationLevel: String = ReputationLevels.DANGER_SCAM,
    @SerializedName(value = "risk_score", alternate = ["spam_score"]) val spamScore: Double = 0.0,
    @SerializedName("is_danger") val isDanger: Boolean = true,
    @SerializedName("primary_category") val primaryCategory: String = "",
    @SerializedName("primary_category_label") val primaryCategoryLabel: String = "",
    @SerializedName("top_community_label") val topCommunityLabel: String = "",
    @SerializedName("sync_version") val syncVersion: Long = 0L
)

data class SpamSyncResponseDto(
    @SerializedName("latest_sync_version") val latestSyncVersion: Long = 0L,
    @SerializedName("count") val count: Int = 0,
    @SerializedName("items") val items: List<SpamSyncItemDto> = emptyList(),
    @SerializedName("national_emergency") val nationalEmergency: List<EmergencyDirectoryDto> = emptyList()
)

data class PublicStatsDto(
    @SerializedName("total_tracked_numbers") val totalTrackedNumbers: Int = 0,
    @SerializedName("danger_scam_count") val dangerScamCount: Int = 0,
    @SerializedName("spam_count") val spamCount: Int = 0,
    @SerializedName("suspicious_count") val suspiciousCount: Int = 0,
    @SerializedName("emergency_agencies") val emergencyAgencies: Int = 0,
    @SerializedName("official_channels_count") val officialChannelsCount: Int = 0,
    @SerializedName("category_totals") val categoryTotals: Map<String, Int> = emptyMap()
)

data class AuthLoginRequestDto(
    @SerializedName("phone") val phone: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("device_id") val deviceId: String
)

data class UserDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("phone_hash") val phoneHash: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("role") val role: String = "USER",
    @SerializedName("trust_weight") val trustWeight: Double = 1.0
)

data class AuthLoginResponseDto(
    @SerializedName("token") val token: String = "",
    @SerializedName("token_type") val tokenType: String = "Bearer",
    @SerializedName("expires_at") val expiresAt: String = "",
    @SerializedName("user") val user: UserDto = UserDto()
)
