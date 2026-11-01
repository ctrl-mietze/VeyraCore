package ctrl.veyracore

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleDirectoryEntryTest {
    @Test fun ordinaryDirectoryEntriesAreAccepted() {
        val stream = ByteArrayOutputStream()
        ZipOutputStream(stream).use { zip ->
            listOf("system/", "system/etc/").forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("system/etc/config.txt"))
            zip.write("safe content".toByteArray())
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("module.prop"))
            zip.write("id=veyradir\nname=Veyra directory module\nversion=1\n".toByteArray())
            zip.closeEntry()
        }
        assertTrue(ModuleZipVerifier.inspectBytes(stream.toByteArray(), "valid-directory.zip").isSuccess)
    }

    @Test fun repeatedSeparatorAndParentTraversalStayRejected() {
        listOf("system//", "system/../etc/", "/", "./assets/", "system/./").forEach { path ->
            val result = runCatching { ModuleZipVerifier.validateEntryPath(path) }
            assertFalse("Unsafe path accepted: $path", result.isSuccess)
        }
    }
}
