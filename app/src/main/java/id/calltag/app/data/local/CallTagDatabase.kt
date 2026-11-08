package id.calltag.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import id.calltag.app.data.local.dao.CallScreeningLogDao
import id.calltag.app.data.local.dao.EmergencyContactDao
import id.calltag.app.data.local.dao.PhoneTagDao
import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.local.entity.EmergencyContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        CachedPhoneTag::class,
        EmergencyContact::class,
        CallScreeningLog::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CallTagDatabase : RoomDatabase() {

    abstract fun phoneTagDao(): PhoneTagDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun callScreeningLogDao(): CallScreeningLogDao

    companion object {
        @Volatile
        private var INSTANCE: CallTagDatabase? = null
        private val dbSeedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun getInstance(context: Context): CallTagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CallTagDatabase::class.java,
                    "calltag_offline.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                seedInitialOfflineDataIfEmpty(instance)
                instance
            }
        }

        private fun seedInitialOfflineDataIfEmpty(database: CallTagDatabase) {
            dbSeedScope.launch {
                val emergencyDao = database.emergencyContactDao()
                if (emergencyDao.count() == 0) {
                    emergencyDao.upsertAll(OfflineSeedData.getInitialEmergencyContacts())
                }

                val phoneTagDao = database.phoneTagDao()
                if (phoneTagDao.count() == 0) {
                    phoneTagDao.upsertAll(OfflineSeedData.getInitialTopScammers())
                }

                val logDao = database.callScreeningLogDao()
                if (logDao.count() == 0) {
                    logDao.insertAll(OfflineSeedData.getInitialScreeningLogs())
                }
            }
        }
    }
}
