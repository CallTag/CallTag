package id.calltag.app

import androidx.sqlite.db.SupportSQLiteDatabase
import id.calltag.app.data.local.CallTagDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Verifikasi K-11 (NFR-004): Migrasi Room v1 -> v2 bersifat non-destruktif,
 * menambahkan kolom metadata cache, outbox pending_reports/pending_disputes, dan sync_state.
 */
class DatabaseMigrationTest {

    @Test
    fun migration1To2AppliesNonDestructiveAdditiveSchema() {
        val migration = CallTagDatabase.MIGRATION_1_2
        assertEquals(1, migration.startVersion)
        assertEquals(2, migration.endVersion)

        val executedSql = mutableListOf<String>()
        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedSql.add(args[0] as String)
                return@newProxyInstance null
            }
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                else -> null
            }
        } as SupportSQLiteDatabase

        migration.migrate(dbProxy)

        assertTrue("Migrasi harus menjalankan statement DDL", executedSql.size >= 12)
        // K-11: Dilarang melakukan DROP TABLE / kehilangan data v1
        for (sql in executedSql) {
            assertFalse("Statement destruktif dilarang saat migrasi: $sql", sql.contains("DROP TABLE", ignoreCase = true))
        }

        val joined = executedSql.joinToString("\n")
        for (col in listOf(
            "cached_phone_tags ADD COLUMN availability",
            "cached_phone_tags ADD COLUMN source",
            "cached_phone_tags ADD COLUMN fetched_at_epoch_ms",
            "cached_phone_tags ADD COLUMN expires_at_epoch_ms",
            "cached_phone_tags ADD COLUMN server_version",
            "cached_phone_tags ADD COLUMN verified_label",
            "emergency_contacts ADD COLUMN source_url",
            "emergency_contacts ADD COLUMN verified_at",
            "emergency_contacts ADD COLUMN server_version"
        )) {
            assertTrue("Kolom migrasi hilang: $col", joined.contains(col))
        }

        assertTrue(joined.contains("CREATE TABLE IF NOT EXISTS `pending_reports`"))
        assertTrue(joined.contains("CREATE UNIQUE INDEX IF NOT EXISTS index_pending_reports_idempotency_key ON pending_reports (idempotency_key)"))
        assertTrue(joined.contains("CREATE INDEX IF NOT EXISTS index_pending_reports_status ON pending_reports (status)"))
        assertTrue(joined.contains("CREATE TABLE IF NOT EXISTS `pending_disputes`"))
        assertTrue(joined.contains("CREATE INDEX IF NOT EXISTS index_pending_disputes_status ON pending_disputes (status)"))
        assertTrue(joined.contains("CREATE TABLE IF NOT EXISTS `sync_state`"))
    }
}

