package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstalledRootToolsTest {
    @Test fun knownToolsUseUniqueIdsAndPackages() {
        val all = InstalledRootTools.known
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.packageName }.toSet().size)
    }

    @Test fun unknownToolCannotBeLaunched() {
        val forged = InstalledRootTool("untrusted", "Unknown", "com.example.unsafe", "")
        assertFalse(InstalledRootTools.isAllowlisted(forged))
    }

    @Test fun VeyraPackageIsAllowlisted() {
        assertTrue(InstalledRootTools.isAllowlisted(InstalledRootTools.known.first()))
    }
}
