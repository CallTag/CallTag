package id.calltag.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cached_phone_tags",
    indices = [
        Index(value = ["reputation_level", "spam_score"]),
        Index(value = ["sync_version"])
    ]
)
data class CachedPhoneTag(
    @PrimaryKey
    @ColumnInfo(name = "phone_e164")
    val phoneE164: String,

    @ColumnInfo(name = "carrier")
    val carrier: String,

    @ColumnInfo(name = "reputation_level")
    val reputationLevel: String, // CLEAN, SUSPICIOUS, SPAM, DANGER_SCAM

    @ColumnInfo(name = "spam_score")
    val spamScore: Double,

    @ColumnInfo(name = "is_danger")
    val isDanger: Boolean,

    @ColumnInfo(name = "primary_category")
    val primaryCategory: String, // PINJOL_DC, APK_SNIFFING, IMPERSONATION, JUDOL, SOCIAL_ENG, TELEMARKETING

    @ColumnInfo(name = "primary_category_label")
    val primaryCategoryLabel: String,

    @ColumnInfo(name = "top_community_label")
    val topCommunityLabel: String,

    @ColumnInfo(name = "tags_summary")
    val tagsSummary: String,

    @ColumnInfo(name = "unique_reporters")
    val uniqueReporters: Int,

    @ColumnInfo(name = "sync_version")
    val syncVersion: Long,

    @ColumnInfo(name = "cached_at_epoch_ms")
    val cachedAtEpochMs: Long = System.currentTimeMillis()
)
