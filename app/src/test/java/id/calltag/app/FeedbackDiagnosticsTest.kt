package id.calltag.app
import id.calltag.app.util.FeedbackDiagnostics
import org.junit.Assert.*
import org.junit.Test
class FeedbackDiagnosticsTest {
    @Test fun templateIncludesOnlySafeMetadataAndRedactsPastedSecrets() {
        val text = FeedbackDiagnostics.create("Lapor Bug", "Gagal untuk +6281234567890; Bearer abc.def.secret", "1.2.0", "Pixel 5", "14")
        assertTrue(text.contains("Lapor Bug"))
        assertTrue(text.contains("Pixel 5"))
        assertTrue(text.contains("1.2.0"))
        assertFalse(text.contains("6281234567890"))
        assertFalse(text.contains("abc.def.secret"))
        assertTrue(text.contains("[nomor disamarkan]"))
    }
    @Test fun blanksProduceUsefulTemplateWithoutInventingRecipient() {
        val text = FeedbackDiagnostics.create("Saran Fitur", "  ", "1", "HP", "13")
        assertTrue(text.contains("Deskripsi: (belum diisi)"))
        assertFalse(text.contains("@"))
    }
}
