import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import suchi.morphe.extension.duchinese.DownloadDiagnostics
import java.net.UnknownHostException

class DownloadDiagnosticsTest {
    private val downloadMessageId = 0x7f13020b

    class Response(
        @JvmField val a: Int,
        @JvmField val b: ByteArray?,
        @JvmField val c: Map<String, String>?,
    )

    class Error(@JvmField val x: Response?, cause: Throwable? = null) :
        Exception("private URL and token", cause)

    @After
    fun clear() = DownloadDiagnostics.clear()

    @Test
    fun reportsHttpResponseWithoutExposingContent() {
        val body = "private token and response".toByteArray()
        DownloadDiagnostics.capture(
            Error(Response(403, body, mapOf(
                "content-type" to "application/json; charset=utf-8; token=private",
                "Set-Cookie" to "private cookie",
            ))),
        )
        val message = DownloadDiagnostics.appendToMessage("Download failed", downloadMessageId).toString()
        assertTrue(message.startsWith("Download failed\n\nVolley: "))
        assertTrue(message.contains("HTTP: 403"))
        assertTrue(message.contains("Response bytes: ${body.size}"))
        assertTrue(message.contains("Content-Type: application/json"))
        assertFalse(message.contains("private"))
        assertFalse(message.contains("Set-Cookie"))
    }

    @Test
    fun reportsTransportCauseWithoutInventingStatus() {
        DownloadDiagnostics.capture(Error(null, UnknownHostException("private hostname")))
        val message = DownloadDiagnostics.appendToMessage("Download failed", downloadMessageId).toString()
        assertTrue(message.contains("HTTP: unavailable (no attached response)"))
        assertTrue(message.contains("Cause: java.net.UnknownHostException"))
        assertFalse(message.contains("private hostname"))
        assertFalse(message.contains("Response bytes"))
    }

    @Test
    fun consumesDetailsOnceAndPreservesOtherMessages() {
        val original = StringBuilder("Original message")
        assertSame(original, DownloadDiagnostics.appendToMessage(original, downloadMessageId))
        DownloadDiagnostics.capture(Error(null))
        assertTrue(DownloadDiagnostics.appendToMessage(original, downloadMessageId).toString().contains("Volley: "))
        assertSame(original, DownloadDiagnostics.appendToMessage(original, downloadMessageId))
        DownloadDiagnostics.capture(Error(null))
        DownloadDiagnostics.clear()
        assertSame(original, DownloadDiagnostics.appendToMessage(original, downloadMessageId))
        DownloadDiagnostics.capture(Error(null))
        assertSame(original, DownloadDiagnostics.appendToMessage(original, 0x7f13020a))
        assertSame(original, DownloadDiagnostics.appendToMessage(original, downloadMessageId))
    }

    @Test
    fun toleratesMissingMetadataAndRejectsInvalidContentTypes() {
        DownloadDiagnostics.capture(Any())
        assertTrue(DownloadDiagnostics.appendToMessage("", downloadMessageId).toString().contains("HTTP: unavailable"))
        DownloadDiagnostics.capture(Error(Response(500, null, mapOf(
            "Content-Type" to "text/plain\r\nprivate-header",
        ))))
        val message = DownloadDiagnostics.appendToMessage("", downloadMessageId).toString()
        assertTrue(message.contains("HTTP: 500"))
        assertFalse(message.contains("Content-Type"))
        assertFalse(message.contains("private-header"))
        DownloadDiagnostics.capture(null)
        assertTrue(DownloadDiagnostics.appendToMessage("", downloadMessageId).toString().contains("Volley: unavailable"))
    }

    @Test
    fun keepsDiagnosticsOnTheCallbackThread() {
        DownloadDiagnostics.capture(Error(null))
        var otherThreadMessage: CharSequence? = null
        val thread = Thread { otherThreadMessage = DownloadDiagnostics.appendToMessage("Other", downloadMessageId) }
        thread.start()
        thread.join()
        assertEquals("Other", otherThreadMessage)
        assertTrue(DownloadDiagnostics.appendToMessage("Current", downloadMessageId).toString().contains("Volley: "))
    }
}
