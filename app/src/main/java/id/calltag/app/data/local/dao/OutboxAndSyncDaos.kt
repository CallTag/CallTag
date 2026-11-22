package id.calltag.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import id.calltag.app.data.local.entity.PendingDisputeEntity
import id.calltag.app.data.local.entity.PendingReportEntity
import id.calltag.app.data.local.entity.SyncStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: PendingReportEntity): Long

    @Update
    suspend fun update(report: PendingReportEntity)

    @Query("SELECT * FROM pending_reports WHERE local_id = :localId LIMIT 1")
    suspend fun getById(localId: Long): PendingReportEntity?

    @Query("SELECT * FROM pending_reports ORDER BY created_at_epoch_ms DESC")
    fun observeAll(): Flow<List<PendingReportEntity>>

    @Query(
        """
        SELECT * FROM pending_reports
        WHERE (status = 'PENDING' OR (status = 'FAILED' AND attempts < :maxAttempts))
        ORDER BY created_at_epoch_ms ASC
        """
    )
    suspend fun getRetryable(maxAttempts: Int = 8): List<PendingReportEntity>

    @Query("UPDATE pending_reports SET status = 'PENDING' WHERE status = 'SENDING'")
    suspend fun recoverSendingToPending()

    @Query("DELETE FROM pending_reports WHERE local_id = :localId")
    suspend fun deleteById(localId: Long)

    @Query("SELECT COUNT(*) FROM pending_reports WHERE status IN ('PENDING', 'SENDING')")
    suspend fun countPending(): Int
}

@Dao
interface PendingDisputeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dispute: PendingDisputeEntity): Long

    @Update
    suspend fun update(dispute: PendingDisputeEntity)

    @Query("SELECT * FROM pending_disputes WHERE owner_scope = :ownerScope ORDER BY created_at_epoch_ms DESC LIMIT 30")
    fun observeAll(ownerScope: String): Flow<List<PendingDisputeEntity>>

    @Query(
        """
        SELECT * FROM pending_disputes
        WHERE owner_scope = :ownerScope AND (status = 'PENDING' OR (status = 'FAILED' AND attempts < :maxAttempts))
        ORDER BY created_at_epoch_ms ASC
        """
    )
    suspend fun getRetryable(ownerScope: String, maxAttempts: Int = 8): List<PendingDisputeEntity>

    @Query("UPDATE pending_disputes SET status = 'PENDING' WHERE owner_scope = :ownerScope AND status = 'SENDING'")
    suspend fun recoverSendingToPending(ownerScope: String)

    @Query("DELETE FROM pending_disputes WHERE local_id = :localId")
    suspend fun deleteById(localId: Long)
}

@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state WHERE id = 1 LIMIT 1")
    suspend fun get(): SyncStateEntity?

    @Query("SELECT * FROM sync_state WHERE id = 1 LIMIT 1")
    fun observe(): Flow<SyncStateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)
}

