package id.calltag.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Antrean laporan spam offline (outbox) sesuai T-301 / T-304.
 * Laporan tertunda TIDAK memengaruhi skor pada `cached_phone_tags`.
 */
@Entity(
    tableName = "pending_reports",
    indices = [
        Index(value = ["idempotency_key"], unique = true),
        Index(value = ["status"])
    ]
)
data class PendingReportEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Long = 0,

    @ColumnInfo(name = "idempotency_key")
    val idempotencyKey: String,

    @ColumnInfo(name = "phone_e164")
    val phoneE164: String,

    @ColumnInfo(name = "category_code")
    val categoryCode: String,

    @ColumnInfo(name = "custom_tag")
    val customTag: String,

    @ColumnInfo(name = "comment")
    val comment: String = "",

    @ColumnInfo(name = "call_type")
    val callType: String = "CELLULAR_CALL",

    @ColumnInfo(name = "status")
    val status: String = "PENDING", // PENDING, SENDING, SENT, FAILED

    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String = "",

    @ColumnInfo(name = "remote_report_id")
    val remoteReportId: String = "",

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Antrean & riwayat pengajuan sanggahan (dispute) beserta token pelacak lokal.
 */
@Entity(
    tableName = "pending_disputes",
    indices = [
        Index(value = ["status"]),
        Index(value = ["owner_scope", "created_at_epoch_ms"])
    ]
)
data class PendingDisputeEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "local_id")
    val localId: Long = 0,

    @ColumnInfo(name = "owner_scope")
    val ownerScope: String? = null,

    @ColumnInfo(name = "phone_e164")
    val phoneE164: String,

    @ColumnInfo(name = "owner_name")
    val ownerName: String,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "contact_ref")
    val contactRef: String = "",

    @ColumnInfo(name = "status")
    val status: String = "PENDING", // PENDING, SENDING, UNDER_REVIEW, FAILED

    @ColumnInfo(name = "dispute_id")
    val disputeId: String = "",

    @ColumnInfo(name = "tracking_token")
    val trackingToken: String = "",

    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,

    @ColumnInfo(name = "last_error")
    val lastError: String = "",

    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long = System.currentTimeMillis()
)

/**
 * Status cursor sinkronisasi delta terpisah dari tabel data (T-301).
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "cursor")
    val cursor: String = "",

    @ColumnInfo(name = "generation")
    val generation: Int = 1,

    @ColumnInfo(name = "last_success_at_epoch_ms")
    val lastSuccessAtEpochMs: Long = 0L,

    @ColumnInfo(name = "last_error")
    val lastError: String = ""
)

