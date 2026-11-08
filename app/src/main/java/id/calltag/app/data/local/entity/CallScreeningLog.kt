package id.calltag.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_screening_logs")
data class CallScreeningLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "phone_e164")
    val phoneE164: String,

    @ColumnInfo(name = "tag_label")
    val tagLabel: String,

    @ColumnInfo(name = "category_code")
    val categoryCode: String,

    @ColumnInfo(name = "reputation_level")
    val reputationLevel: String,

    @ColumnInfo(name = "spam_score")
    val spamScore: Double,

    @ColumnInfo(name = "was_blocked")
    val wasBlocked: Boolean,

    @ColumnInfo(name = "detection_source")
    val detectionSource: String, // LOCAL_CACHE atau LIVE_API

    @ColumnInfo(name = "screened_at_epoch_ms")
    val screenedAtEpochMs: Long = System.currentTimeMillis()
)
