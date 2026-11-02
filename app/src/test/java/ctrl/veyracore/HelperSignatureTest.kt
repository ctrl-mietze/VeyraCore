package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HelperSignatureTest {
    @Test fun sha256IsDeterministicAndLowercase() {
        val expected = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
        assertEquals(expected, HelperSigner.hash("test".toByteArray()))
        assertEquals(64, HelperSigner.hash("hello".toByteArray()).length)
        assertNotEquals(HelperSigner.hash("test".toByteArray()),
            HelperSigner.hash("Test".toByteArray()))
    }
}