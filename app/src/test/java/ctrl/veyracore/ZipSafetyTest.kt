package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipSafetyTest {
    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val data = ByteArrayOutputStream()
        ZipOutputStream(data).use { out ->
            entries.forEach { (name, value) ->
                out.putNextEntry(ZipEntry(name))
                out.write(value.toByteArray())
                out.closeEntry()
            }
        }
        return data.toByteArray()
    }

    @Test fun acceptsRootModuleProp() {
        val result = ZipSafety.inspect(ByteArrayInputStream(zip(
            "module.prop" to "id=sample\nname=Sample",
            "system/etc/example.conf" to "test"
        )))
        assertEquals(2, result.count)
        assertTrue(result.containsModuleProp)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTraversal() {
        ZipSafety.inspect(ByteArrayInputStream(zip("module.prop" to "ok", "../unsafe" to "x")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsMissingRootModuleProp() {
        ZipSafety.inspect(ByteArrayInputStream(zip("other/metadata.txt" to "not a module")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAbsoluteEntries() {
        ZipSafety.inspect(ByteArrayInputStream(zip("module.prop" to "id=a", "/tmp/escape" to "x")))
    }
}
