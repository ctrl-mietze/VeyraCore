package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderRegistryTest {
    @Test fun defaultRegistryHasDistinctHonestMethodIdsAndStatuses() {
        val device = DeviceInfo("samsung", "SM-S938B", "Android 16", "6.12.23-test", "samsung/test", "BUILD/1", "arm64-v8a", 4096, "android16-6.12")
        val states = RootProviderRegistry.default().evaluateAll(device, RootProbe())
        assertEquals(listOf("ghostlock", "dirtyfrag", "vx-cert-reroot", "authorized-su"), states.map { it.methodId })
        assertEquals(ProviderStatus.UNAVAILABLE, states.first { it.methodId == "ghostlock" }.status)
        assertEquals(ProviderStatus.BLOCKED, states.first { it.methodId == "dirtyfrag" }.status)
        assertEquals(ProviderStatus.REQUIRES_EXTERNAL_RUNTIME, states.first { it.methodId == "vx-cert-reroot" }.status)
        assertEquals(ProviderStatus.UNAVAILABLE, states.first { it.methodId == "authorized-su" }.status)
        assertTrue(states.all { it.provenance.sourceUrl.startsWith("https://") })
        assertTrue(states.none { it.exactProfileMatched })
    }

    @Test fun authorizedSuIsSupportedOnlyAfterUidZeroProof() {
        val device = DeviceInfo("test", "model", "Android", "kernel", "fingerprint")
        val registry = RootProviderRegistry.default()
        val nonRoot = registry.evaluateAll(device, RootProbe()).first { it.methodId == "authorized-su" }
        val root = registry.evaluateAll(device, RootProbe(Verification.VERIFIED, uid = 0, uidClass = UidClass.ROOT)).first { it.methodId == "authorized-su" }
        assertEquals(ProviderStatus.UNAVAILABLE, nonRoot.status)
        assertEquals(ProviderStatus.SUPPORTED, root.status)
        assertFalse(nonRoot.exactProfileMatched)
        assertTrue(root.exactProfileMatched)
    }
}
