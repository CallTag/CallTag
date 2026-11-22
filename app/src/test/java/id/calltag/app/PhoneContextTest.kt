package id.calltag.app

import com.google.gson.Gson
import id.calltag.app.util.PhoneNormalizer
import org.junit.Assert.*
import org.junit.Test

class PhoneContextTest {
    @Test fun explicitCountriesAndNationalServicesKeepTheirIdentity() {
        for ((raw, expected) in listOf("+85512345678" to "+85512345678", "+819012345678" to "+819012345678", "1500888" to "1500888", "tel:+855 12345678" to "+85512345678", "08123456789" to "+628123456789")) {
            assertEquals(raw, expected, PhoneNormalizer.normalizeToE164(raw))
        }
    }
    @Test fun completeCarrierTablesMatchSharedBackendFixture() {
        // Salinan fixture backend/testdata/carrier_context.json disertakan agar repo Android mandiri.
        val raw = requireNotNull(javaClass.getResource("/carrier_context.json")).readText()
        val cases = Gson().fromJson(raw, Array<CarrierCase>::class.java)
        assertTrue(cases.size > 400)
        cases.forEach { assertEquals(it.phone, it.expected, PhoneNormalizer.detectCarrierContext(it.phone)) }
    }
    @Test fun whatsappUsesFullCanonicalNumberAndRejectsServiceOrInvalidNumbers() {
        assertEquals("https://wa.me/628123456789", PhoneNormalizer.whatsAppUrl("08123456789"))
        assertEquals("https://wa.me/85512345678", PhoneNormalizer.whatsAppUrl("+85512345678"))
        listOf("112", "1500888", "+112", "+999123456789", "+08123456789", "+62812****1234", "+6281234567890123456", "not a phone").forEach { assertNull(it, PhoneNormalizer.whatsAppUrl(it)) }
    }
    @Test fun verificationExpiryIsConservative() {
        val now = java.time.Instant.parse("2026-11-16T00:00:00Z").toEpochMilli()
        assertTrue(PhoneNormalizer.isVerificationUnexpired(null, now))
        assertTrue(PhoneNormalizer.isVerificationUnexpired("2026-11-17T00:00:00Z", now))
        assertFalse(PhoneNormalizer.isVerificationUnexpired("2026-11-16T00:00:00Z", now))
        assertFalse(PhoneNormalizer.isVerificationUnexpired("invalid", now))
    }
    private data class CarrierCase(val phone: String, val expected: String)
}
