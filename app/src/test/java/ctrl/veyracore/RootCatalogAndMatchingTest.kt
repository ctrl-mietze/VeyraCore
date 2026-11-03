package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootCatalogAndMatchingTest {
    @Test fun catalogIncludesEveryCuratedEntry() {
        assertEquals(57, RootPortRegistry.all.size)
        assertEquals(57, RootPortRegistry.all.map { it.url }.toSet().size)
        assertTrue(RootPortRegistry.all.all { it.url.startsWith("https://github.com/") })
    }
    @Test fun groupsAndFiltersWork() {
        assertTrue(RootPortRegistry.all.any { it.group == PortGroup.DIRTYFRAG })
        assertTrue(RootPortRegistry.search("DFRoot").isNotEmpty())
        assertTrue(RootPortRegistry.search("DirtyFrag", PortGroup.DIRTYFRAG).isNotEmpty())
        assertFalse(RootPortRegistry.search("dirtyfrag", PortGroup.PAYLOADS).isNotEmpty())
    }
    @Test fun kernelReleaseExtractionIsExactThreeComponents() {
        assertEquals("6.6.98", KernelVersion.extract("6.6.98-android15-8-pd6ff1cd"))
        assertEquals("6.1.145", KernelVersion.extract("6.1.145-android14-11"))
        assertEquals(null, KernelVersion.extract("kernel-has-no-version"))
    }
    @Test fun matchingRejectsWrongModelOrWrongKernel() {
        val profile = PayloadProfile("s25", "S25", "RMG", listOf("SM-S938B"), listOf("6.6.98"))
        val info = DeviceInfo("samsung", "SM-S938B", "Android 16", "6.6.98-android15-8",
            "samsung/e3q/fingerprint")
        assertTrue(profile.matches(info))
        assertFalse(profile.matches(info.copy(model = "SM-S928B")))
        assertFalse(profile.matches(info.copy(kernel = "6.6.99-android15")))
    }
    @Test fun fingerprintBoundProfilesRejectOtherBuilds() {
        val info = DeviceInfo("samsung", "SM-S938B", "Android 16",
            "6.6.98-android15-8", "new-fingerprint")
        val profile = PayloadProfile("s25", "S25", "RMG", listOf("SM-S938B"),
            listOf("6.6.98"), fingerprints = listOf("required-fingerprint"))
        assertFalse(profile.matches(info))
        assertTrue(profile.matches(info.copy(fingerprint = "required-fingerprint")))
    }
}