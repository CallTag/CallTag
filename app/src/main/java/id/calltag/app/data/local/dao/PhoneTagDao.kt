package id.calltag.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.calltag.app.data.local.entity.CachedPhoneTag
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneTagDao {

    @Query("SELECT * FROM cached_phone_tags WHERE phone_e164 = :phoneE164 LIMIT 1")
    suspend fun getByPhone(phoneE164: String): CachedPhoneTag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(phoneTag: CachedPhoneTag)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<CachedPhoneTag>)

    @Query("SELECT COALESCE(MAX(sync_version), 0) FROM cached_phone_tags")
    suspend fun getLatestSyncVersion(): Long

    @Query("SELECT COUNT(*) FROM cached_phone_tags")
    suspend fun count(): Int

    @Query("SELECT * FROM cached_phone_tags WHERE is_danger = 1 ORDER BY spam_score DESC")
    fun observeDangerPhoneTags(): Flow<List<CachedPhoneTag>>
}
