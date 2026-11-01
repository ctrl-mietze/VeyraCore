package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RootResearchIndexTest {
    @Test fun allPortReferencesUseUniqueLinks() {
        val all = RootResearchIndex.ports
        assertEquals(18, all.size)
        assertEquals(all.size, all.map { it.url }.toSet().size)
        assertTrue(all.all { it.url.startsWith("https://github.com/") })
    }

    @Test fun theResearchIndexCannotExecuteRoot() {
        val guest = DeviceInfo("samsung", "SM-S938B", "Android 16", "kernel", "fingerprint")
        val routes = RootRouteCatalog.routes(guest, RootProbe())
        assertTrue(routes.filter { it.id != "existing-su" }.all { !it.canStart })
    }
}
