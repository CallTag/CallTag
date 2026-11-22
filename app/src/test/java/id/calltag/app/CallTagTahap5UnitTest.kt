package id.calltag.app

import id.calltag.app.data.local.entity.CachedPhoneTag
import id.calltag.app.data.remote.dto.ReputationLevels
import id.calltag.app.data.repository.CallTagRepository
import id.calltag.app.data.repository.ConfidenceTier
import id.calltag.app.data.repository.LookupAvailability
import id.calltag.app.data.repository.LookupDisplayStatus
import id.calltag.app.util.PhoneNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallTagTahap5UnitTest {

    @Test
    fun normalizeAndShortCodeDetectionWorkCorrectly() {
        assertEquals("+6281234567890", PhoneNormalizer.normalizeToE164("0812-3456-7890"))
        assertEquals("+6281234567890", PhoneNormalizer.normalizeToE164("tel:6281234567890"))
        assertEquals("112", PhoneNormalizer.normalizeToE164("112"))
        assertEquals("14080", PhoneNormalizer.normalizeToE164("14080"))

        assertTrue(PhoneNormalizer.isEmergencyOrShortCode("112"))
        assertTrue(PhoneNormalizer.isEmergencyOrShortCode("tel:119"))
        assertTrue(PhoneNormalizer.isEmergencyOrShortCode("14080"))
        assertFalse(PhoneNormalizer.isEmergencyOrShortCode("+6281234567890"))
        assertFalse(PhoneNormalizer.isEmergencyOrShortCode("081234567890"))
    }

    @Test
    fun maskPhoneAndUrlProtectRawPii() {
        assertEquals("+62812****7766", PhoneNormalizer.maskPhone("+6281299887766"))
        assertEquals("112", PhoneNormalizer.maskPhone("112"))

        val redactedUrl = PhoneNormalizer.maskPhoneInUrl(
            "https://api.calltag.id/api/v1/phones/+6281299887766/reports?limit=10"
        )
        assertTrue(redactedUrl.contains("+62812****7766"))
        assertFalse(redactedUrl.contains("9988"))
    }

    @Test
    fun shouldAutoBlockEnforcesConservativePolicy() {
        val now = 1_760_000_000_000L
        val threeDaysAgo = now - 3L * 24 * 60 * 60 * 1000L
        val fifteenDaysAgo = now - 15L * 24 * 60 * 60 * 1000L

        // Default opt-in OFF harus menolak blokir otomatis
        assertFalse(
            PhoneNormalizer.shouldAutoBlock(
                reputationLevel = "DANGER_SCAM",
                autoBlockEnabled = false,
                source = "SERVER",
                fetchedAtEpochMs = threeDaysAgo,
                nowEpochMs = now,
                phoneE164 = "+6281299887766"
            )
        )

        // FIXTURE dan LOCAL tidak boleh pernah diblokir otomatis
        assertFalse(
            PhoneNormalizer.shouldAutoBlock(
                reputationLevel = "DANGER_SCAM",
                autoBlockEnabled = true,
                source = "FIXTURE",
                fetchedAtEpochMs = threeDaysAgo,
                nowEpochMs = now,
                phoneE164 = "+6281299887766"
            )
        )

        // SPAM dan SUSPICIOUS hanya tampilkan notifikasi/overlay, bukan auto-block
        assertFalse(
            PhoneNormalizer.shouldAutoBlock(
                reputationLevel = "SPAM",
                autoBlockEnabled = true,
                source = "SERVER",
                fetchedAtEpochMs = threeDaysAgo,
                nowEpochMs = now,
                phoneE164 = "+6281299887766"
            )
        )

        // Whitelist atau kode darurat selalu aman dari blokir
        assertFalse(
            PhoneNormalizer.shouldAutoBlock(
                reputationLevel = "DANGER_SCAM",
                autoBlockEnabled = true,
                source = "SERVER",
                fetchedAtEpochMs = threeDaysAgo,
                nowEpochMs = now,
                isWhitelisted = true,
                phoneE164 = "+6281299887766"
            )
        )
        assertFalse(
            PhoneNormalizer.shouldAutoBlock(
                reputationLevel = "DANGER_SCAM",
                autoBlockEnabled = true,
                source = "SERVER",
                fetchedAtEpochMs = threeDaysAgo,
                nowEpochMs = now,
                phoneE164 = "112"
            )
        )

        // Cache > 14 hari kedaluwarsa untuk auto-block
        assertFalse(
            PhoneNormalizer.shouldAutoBlock(
                reputationLevel = "DANGER_SCAM",
                autoBlockEnabled = true,
                source = "SERVER",
                fetchedAtEpochMs = fifteenDaysAgo,
                nowEpochMs = now,
                phoneE164 = "+6281299887766"
            )
        )

        // Hanya DANGER_SCAM + SERVER + <= 14 hari + opt-in + non-whitelist
        assertTrue(
            PhoneNormalizer.shouldAutoBlock(
                reputationLevel = "DANGER_SCAM",
                autoBlockEnabled = true,
                source = "SERVER",
                fetchedAtEpochMs = threeDaysAgo,
                nowEpochMs = now,
                isWhitelisted = false,
                phoneE164 = "+6281299887766"
            )
        )
    }

    @Test
    fun resolveLookupResultHandles2DMatrixHonestly() {
        val now = 1_760_000_000_000L

        // 1. NOT_FOUND -> UNKNOWN (tanpa klaim Nomor Aman)
        val notFound = CallTagRepository.resolveLookupResult(
            e164 = "+6281100001111",
            availability = LookupAvailability.NOT_FOUND,
            entity = null,
            nowEpochMs = now
        )
        assertEquals(LookupAvailability.NOT_FOUND, notFound.availability)
        assertEquals(LookupDisplayStatus.UNKNOWN, notFound.displayStatus)
        assertEquals(ConfidenceTier.NONE, notFound.confidence)
        assertTrue(notFound.statusHeadline.contains("Belum ada data", ignoreCase = true))
        assertFalse(notFound.statusHeadline.contains("Nomor Aman", ignoreCase = true))

        // 2. UNAVAILABLE tanpa cache -> OFFLINE
        val offlineMiss = CallTagRepository.resolveLookupResult(
            e164 = "+6281100001111",
            availability = LookupAvailability.UNAVAILABLE,
            entity = null,
            nowEpochMs = now
        )
        assertEquals(LookupAvailability.UNAVAILABLE, offlineMiss.availability)
        assertEquals(LookupDisplayStatus.OFFLINE, offlineMiss.displayStatus)
        assertEquals("OFFLINE", offlineMiss.source)
        assertTrue(offlineMiss.statusHeadline.contains("offline", ignoreCase = true))

        // 3. Cache offline > 90 hari ditandai STALE
        val oldCacheEntity = sampleEntity(
            reputation = ReputationLevels.DANGER_SCAM,
            reporters = 6,
            source = "SERVER",
            fetchedAt = now - 100L * 24 * 60 * 60 * 1000L,
            expiresAt = now + 30L * 24 * 60 * 60 * 1000L
        )
        val staleCache = CallTagRepository.resolveLookupResult(
            e164 = "+6281299887766",
            availability = LookupAvailability.UNAVAILABLE,
            entity = oldCacheEntity,
            nowEpochMs = now
        )
        assertTrue(staleCache.isStale)
        assertEquals("CACHE", staleCache.source)
        assertEquals(LookupDisplayStatus.DANGER_SCAM, staleCache.displayStatus)

        // 4. Laporan tunggal N=1 -> Confidence LOW (bukan DANGER/Aman)
        val singleSuspicious = CallTagRepository.resolveLookupResult(
            e164 = "+6281299887766",
            availability = LookupAvailability.AVAILABLE,
            entity = sampleEntity(
                reputation = ReputationLevels.SUSPICIOUS,
                reporters = 1,
                source = "SERVER",
                fetchedAt = now,
                expiresAt = now + 90L * 24 * 60 * 60 * 1000L
            ),
            nowEpochMs = now
        )
        assertEquals(LookupDisplayStatus.SUSPICIOUS, singleSuspicious.displayStatus)
        assertEquals(ConfidenceTier.LOW, singleSuspicious.confidence)
        assertTrue(singleSuspicious.statusSubtext.contains("keyakinan rendah", ignoreCase = true))

        // 5. CLEAN dengan N=1 historis -> CLEAN_OBSERVED (tidak menyebut Nomor Aman)
        val cleanObserved = CallTagRepository.resolveLookupResult(
            e164 = "+6281299887766",
            availability = LookupAvailability.AVAILABLE,
            entity = sampleEntity(
                reputation = ReputationLevels.CLEAN,
                reporters = 1,
                source = "SERVER",
                fetchedAt = now,
                expiresAt = now + 90L * 24 * 60 * 60 * 1000L
            ),
            nowEpochMs = now
        )
        assertEquals(LookupDisplayStatus.CLEAN_OBSERVED, cleanObserved.displayStatus)
        assertEquals(ConfidenceTier.LOW, cleanObserved.confidence)
        assertFalse(cleanObserved.statusHeadline.contains("Nomor Aman", ignoreCase = true))

        // 6. CLEAN dengan N=0 -> UNKNOWN
        val cleanZero = CallTagRepository.resolveLookupResult(
            e164 = "+6281100001111",
            availability = LookupAvailability.AVAILABLE,
            entity = sampleEntity(
                reputation = ReputationLevels.CLEAN,
                reporters = 0,
                source = "SERVER",
                fetchedAt = now,
                expiresAt = now + 90L * 24 * 60 * 60 * 1000L
            ),
            nowEpochMs = now
        )
        assertEquals(LookupDisplayStatus.UNKNOWN, cleanZero.displayStatus)

        // 7. Verified Label -> VERIFIED + HIGH confidence
        val verified = CallTagRepository.resolveLookupResult(
            e164 = "+62211500888",
            availability = LookupAvailability.AVAILABLE,
            entity = sampleEntity(
                reputation = ReputationLevels.CLEAN,
                reporters = 0,
                source = "SERVER",
                fetchedAt = now,
                expiresAt = now + 90L * 24 * 60 * 60 * 1000L,
                verifiedLabel = "Halo BCA Resmi"
            ),
            nowEpochMs = now
        )
        assertEquals(LookupDisplayStatus.VERIFIED, verified.displayStatus)
        assertEquals(ConfidenceTier.HIGH, verified.confidence)
    }

    @Test
    fun expiredOrOfflineVerificationNeverShowsAnActiveShield() {
        val now = 1_760_000_000_000L
        for (availability in listOf(LookupAvailability.AVAILABLE, LookupAvailability.UNAVAILABLE)) {
            val expired = CallTagRepository.resolveLookupResult(
                e164 = "+6281299887766", availability = availability,
                entity = sampleEntity(ReputationLevels.CLEAN, 0, "SERVER", now - 1000, now - 1, "Nomor Resmi"),
                nowEpochMs = now
            )
            assertTrue(expired.isStale)
            assertFalse(expired.displayStatus == LookupDisplayStatus.VERIFIED)
            assertEquals(null, expired.verifiedLabel)
            assertEquals(null, expired.phoneTag.verifiedLabel)
        }
        val offlineFresh = CallTagRepository.resolveLookupResult(
            e164 = "+6281299887766", availability = LookupAvailability.UNAVAILABLE,
            entity = sampleEntity(ReputationLevels.CLEAN, 0, "SERVER", now, now + 1000, "Nomor Resmi"),
            nowEpochMs = now
        )
        assertFalse(offlineFresh.displayStatus == LookupDisplayStatus.VERIFIED)
    }

    @Test
    fun unknownAndOfflineMissesKeepPrefixContextWithoutChangingRiskStatus() {
        for (availability in listOf(LookupAvailability.NOT_FOUND, LookupAvailability.UNAVAILABLE)) {
            val result = CallTagRepository.resolveLookupResult("+85512345678", availability, null)
            assertTrue(result.phoneTag.carrier.contains("Kamboja"))
            assertFalse(result.displayStatus == LookupDisplayStatus.DANGER_SCAM)
        }
    }

    private fun sampleEntity(
        reputation: String,
        reporters: Int,
        source: String,
        fetchedAt: Long,
        expiresAt: Long,
        verifiedLabel: String? = null
    ) = CachedPhoneTag(
        phoneE164 = "+6281299887766",
        carrier = "Telkomsel",
        reputationLevel = reputation,
        spamScore = 30.0,
        isDanger = reputation == ReputationLevels.DANGER_SCAM,
        primaryCategory = "TELEMARKETING",
        primaryCategoryLabel = "Telemarketing",
        topCommunityLabel = "Contoh Tag",
        tagsSummary = "Contoh Tag ($reporters)",
        uniqueReporters = reporters,
        syncVersion = 1L,
        cachedAtEpochMs = fetchedAt,
        availability = "AVAILABLE",
        source = source,
        fetchedAtEpochMs = fetchedAt,
        expiresAtEpochMs = expiresAt,
        serverVersion = 1L,
        verifiedLabel = verifiedLabel
    )
}

