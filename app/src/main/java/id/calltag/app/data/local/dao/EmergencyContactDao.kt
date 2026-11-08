package id.calltag.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.calltag.app.data.local.entity.EmergencyContact
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyContactDao {

    @Query("SELECT * FROM emergency_contacts WHERE scope = 'NATIONAL' ORDER BY short_code ASC")
    fun observeNationalContacts(): Flow<List<EmergencyContact>>

    @Query("SELECT * FROM emergency_contacts WHERE scope = 'NATIONAL' ORDER BY short_code ASC")
    suspend fun getNationalContacts(): List<EmergencyContact>

    @Query(
        """
        SELECT * FROM emergency_contacts
        WHERE scope = 'LOCAL'
        AND (:province = '' OR LOWER(province) LIKE '%' || LOWER(:province) || '%')
        AND (:city = '' OR LOWER(city_regency) LIKE '%' || LOWER(:city) || '%')
        AND (:agencyType = '' OR UPPER(agency_type) = UPPER(:agencyType))
        ORDER BY city_regency ASC, agency_type ASC
        """
    )
    fun observeLocalContacts(
        province: String = "",
        city: String = "",
        agencyType: String = ""
    ): Flow<List<EmergencyContact>>

    @Query(
        """
        SELECT * FROM emergency_contacts
        WHERE scope = 'LOCAL'
        AND (:province = '' OR LOWER(province) LIKE '%' || LOWER(:province) || '%')
        AND (:city = '' OR LOWER(city_regency) LIKE '%' || LOWER(:city) || '%')
        ORDER BY city_regency ASC, agency_type ASC
        """
    )
    suspend fun getLocalContacts(province: String = "", city: String = ""): List<EmergencyContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<EmergencyContact>)

    @Query("SELECT COUNT(*) FROM emergency_contacts")
    suspend fun count(): Int
}
