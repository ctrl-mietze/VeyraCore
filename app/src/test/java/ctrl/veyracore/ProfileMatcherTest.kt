package ctrl.veyracore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileMatcherTest {
    private val device = DeviceInfo(
        manufacturer = "samsung",
        model = "SM-S901B",
        android = "Android 15",
        kernel = "6.1.0-test",
        fingerprint = "samsung/r0sxx/r0s:15/UP1A/ABC:user",
        firmwareId = "UP1A.240905.003/1234567",
        abi = "arm64-v8a",
        pageSize = 4096,
        kmi = "android14-6.1"
    )

    @Test fun exactProfileMatches() = assertTrue(ProfileMatcher.matches(SupportedProfile("Samsung", "SM-S901B", device.fingerprint, "6.1.0-test"), device))
    @Test fun fingerprintMismatchIsNotCompatible() = assertFalse(ProfileMatcher.matches(SupportedProfile("Samsung", "SM-S901B", "other", "6.1.0-test"), device))
    @Test fun modelMismatchIsNotCompatible() = assertFalse(ProfileMatcher.matches(SupportedProfile("Samsung", "SM-S901U", device.fingerprint, "6.1.0-test"), device))

    @Test fun completeProfileMatchesAllHardwareDimensions() {
        val profile = SupportedProfile(
            manufacturer = "Samsung",
            model = device.model,
            fingerprint = device.fingerprint,
            kernel = device.kernel,
            firmwareId = device.firmwareId,
            abi = device.abi,
            pageSize = device.pageSize,
            kmi = device.kmi
        )
        assertTrue(ProfileMatcher.matchesExact(profile, device))
    }

    @Test fun kernelFirmwareAbiPageAndKmiMismatchAreRejected() {
        val fields = listOf<(SupportedProfile) -> SupportedProfile>(
            { it.copy(kernel = "6.1.1-other") },
            { it.copy(firmwareId = "different-firmware") },
            { it.copy(abi = "armeabi-v7a") },
            { it.copy(pageSize = 16384) },
            { it.copy(kmi = "android15-6.6") }
        )
        val profile = SupportedProfile(device.manufacturer, device.model, device.fingerprint, device.kernel, device.firmwareId, device.abi, device.pageSize, device.kmi)
        fields.forEach { mismatch -> assertFalse(ProfileMatcher.matchesExact(mismatch(profile), device)) }
    }

    @Test fun incompleteProfileCannotClaimExactCompatibility() {
        val incomplete = SupportedProfile(device.manufacturer, device.model, device.fingerprint, device.kernel)
        assertTrue(ProfileMatcher.matches(incomplete, device))
        assertFalse(ProfileMatcher.matchesExact(incomplete, device))
    }
}
