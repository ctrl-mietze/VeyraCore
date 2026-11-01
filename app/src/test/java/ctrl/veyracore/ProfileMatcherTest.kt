package ctrl.veyracore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileMatcherTest {
    private val device = DeviceInfo("samsung", "SM-S901B", "Android 15", "6.1.0-test", "samsung/r0sxx/r0s:15/UP1A/ABC:user")
    @Test fun exactProfileMatches() = assertTrue(ProfileMatcher.matches(SupportedProfile("Samsung", "SM-S901B", device.fingerprint, "6.1.0-test"), device))
    @Test fun fingerprintMismatchIsNotCompatible() = assertFalse(ProfileMatcher.matches(SupportedProfile("Samsung", "SM-S901B", "other", "6.1.0-test"), device))
    @Test fun modelMismatchIsNotCompatible() = assertFalse(ProfileMatcher.matches(SupportedProfile("Samsung", "SM-S901U", device.fingerprint, "6.1.0-test"), device))
}
