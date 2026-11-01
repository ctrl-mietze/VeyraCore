package ctrl.veyracore

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleZipVerifierTest {
    @Test fun previewsTextModulePropWithoutExecutingFiles() {
        val bytes = zip(
            "module.prop" to "id=demo.module\nname=Demo module\nversion=1.2\nauthor=Veyra\ndescription=Safe preview\n",
            "service.sh" to "#!/system/bin/sh\nrm -rf /\n"
        )
        val preview = ModuleZipVerifier.inspectBytes(bytes, "demo.zip").getOrThrow()
        assertEquals("demo.module", preview.moduleId)
        assertEquals("Demo module", preview.moduleName)
        assertTrue(preview.entries.contains("service.sh"))
    }

    @Test fun rejectsTraversalAndAbsolutePaths() {
        val traversal = ModuleZipVerifier.inspectBytes(zip("module.prop" to "id=demo\nname=Demo\n", "../escape" to "bad"), "bad.zip")
        val absolute = ModuleZipVerifier.inspectBytes(zip("module.prop" to "id=demo\nname=Demo\n", "/absolute" to "bad"), "bad.zip")
        assertFalse(traversal.isSuccess)
        assertFalse(absolute.isSuccess)
    }

    @Test fun rejectsUnsafeModuleIdAndMissingModuleProp() {
        assertFalse(ModuleZipVerifier.inspectBytes(zip("module.prop" to "id=../bad\nname=Bad\n"), "bad.zip").isSuccess)
        assertFalse(ModuleZipVerifier.inspectBytes(zip("payload/file" to "no metadata"), "bad.zip").isSuccess)
    }

    @Test fun rejectsUnixSymlinkMetadataInCentralDirectory() {
        val symlinkArchive = markFirstCentralEntryAsSymlink(zip("module.prop" to "id=demo\nname=Demo\n"))
        val result = ModuleZipVerifier.inspectBytes(symlinkArchive, "symlink.zip")
        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("symbolischen Link"))
        assertTrue(ModuleZipVerifier.isUnixSymlink(0xA000))
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, content) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    private fun markFirstCentralEntryAsSymlink(input: ByteArray): ByteArray {
        val output = input.copyOf()
        for (index in 0 until output.size - 46) {
            if (u32(output, index) == 0x02014b50L) {
                // version made by: upper byte 3 = Unix; external mode 0120000 = symlink.
                output[index + 4] = 0
                output[index + 5] = 3
                output[index + 38] = 0
                output[index + 39] = 0
                output[index + 40] = 0
                output[index + 41] = 0xA0.toByte()
                return output
            }
        }
        error("central directory not found")
    }

    private fun u32(bytes: ByteArray, offset: Int): Long =
        (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
}
