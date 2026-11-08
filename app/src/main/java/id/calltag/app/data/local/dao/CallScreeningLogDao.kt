package id.calltag.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.calltag.app.data.local.entity.CallScreeningLog
import kotlinx.coroutines.flow.Flow

@Dao
interface CallScreeningLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CallScreeningLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<CallScreeningLog>)

    @Query("SELECT COUNT(*) FROM call_screening_logs")
    suspend fun count(): Int

    @Query("SELECT * FROM call_screening_logs ORDER BY screened_at_epoch_ms DESC LIMIT :limit")
    fun observeRecentLogs(limit: Int = 50): Flow<List<CallScreeningLog>>
}
