package id.calltag.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import id.calltag.app.BuildConfig
import id.calltag.app.data.local.dao.CallScreeningLogDao
import id.calltag.app.data.local.dao.EmergencyContactDao
import id.calltag.app.data.local.dao.PendingDisputeDao
import id.calltag.app.data.local.dao.PendingReportDao
import id.calltag.app.data.local.dao.PhoneTagDao
import id.calltag.app.data.local.dao.SyncStateDao
import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.data.local.entity.CallScreeningLog
import id.calltag.app.data.local.entity.EmergencyContact
import id.calltag.app.data.local.entity.PendingDisputeEntity
import id.calltag.app.data.local.entity.PendingReportEntity
import id.calltag.app.data.local.entity.SyncStateEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [
        CachedPhoneTag::class,
        EmergencyContact::class,
        CallScreeningLog::class,
        PendingReportEntity::class,
        PendingDisputeEntity::class,
        SyncStateEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class CallTagDatabase : RoomDatabase() {

    abstract fun phoneTagDao(): PhoneTagDao
    abstract fun emergencyContactDao(): EmergencyContactDao
    abstract fun callScreeningLogDao(): CallScreeningLogDao
    abstract fun pendingReportDao(): PendingReportDao
    abstract fun pendingDisputeDao(): PendingDisputeDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        @Volatile
        private var INSTANCE: CallTagDatabase? = null
        private val dbSeedScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_phone_tags ADD COLUMN availability TEXT NOT NULL DEFAULT 'AVAILABLE'")
                db.execSQL("ALTER TABLE cached_phone_tags ADD COLUMN source TEXT NOT NULL DEFAULT 'SERVER'")
                db.execSQL("ALTER TABLE cached_phone_tags ADD COLUMN fetched_at_epoch_ms INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_phone_tags ADD COLUMN expires_at_epoch_ms INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_phone_tags ADD COLUMN server_version INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE cached_phone_tags ADD COLUMN verified_label TEXT")

                db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN source_url TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN verified_at TEXT NOT NULL DEFAULT '2026-11-01'")
                db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN server_version INTEGER NOT NULL DEFAULT 1")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_reports` (
                        `local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `idempotency_key` TEXT NOT NULL,
                        `phone_e164` TEXT NOT NULL,
                        `category_code` TEXT NOT NULL,
                        `custom_tag` TEXT NOT NULL,
                        `comment` TEXT NOT NULL,
                        `call_type` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `last_error` TEXT NOT NULL,
                        `remote_report_id` TEXT NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL,
                        `updated_at_epoch_ms` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_pending_reports_idempotency_key ON pending_reports (idempotency_key)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_reports_status ON pending_reports (status)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `pending_disputes` (
                        `local_id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `phone_e164` TEXT NOT NULL,
                        `owner_name` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `contact_ref` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `dispute_id` TEXT NOT NULL,
                        `tracking_token` TEXT NOT NULL,
                        `attempts` INTEGER NOT NULL,
                        `last_error` TEXT NOT NULL,
                        `created_at_epoch_ms` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_disputes_status ON pending_disputes (status)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sync_state` (
                        `id` INTEGER NOT NULL,
                        `cursor` TEXT NOT NULL,
                        `generation` INTEGER NOT NULL,
                        `last_success_at_epoch_ms` INTEGER NOT NULL,
                        `last_error` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE call_screening_logs ADD COLUMN owner_scope TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_call_screening_logs_owner_scope_screened_at_epoch_ms ON call_screening_logs (owner_scope, screened_at_epoch_ms)")
                db.execSQL("ALTER TABLE pending_disputes ADD COLUMN owner_scope TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_disputes_owner_scope_created_at_epoch_ms ON pending_disputes (owner_scope, created_at_epoch_ms)")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE emergency_contacts ADD COLUMN longitude REAL")
            }
        }

        fun getInstance(context: Context): CallTagDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CallTagDatabase::class.java,
                    "calltag_offline.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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

                // Data scammer contoh hanya berlabel FIXTURE dan hanya diisikan pada build debug (T-306).
                if (BuildConfig.DEBUG) {
                    val phoneTagDao = database.phoneTagDao()
                    if (phoneTagDao.count() == 0) {
                        phoneTagDao.upsertAll(OfflineSeedData.getInitialTopScammers())
                    }
                }
            }
        }
    }
}
