package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RootIdentityTest {
    @Test fun parsesLeadingUidZeroOnly() {
        assertEquals(0, RootIdentity.parseUid("uid=0(root) gid=0(root) groups=0(root)"))
        assertTrue(RootProbe(Verification.VERIFIED, uid = 0, uidClass = UidClass.ROOT).isUid0)
    }

    @Test fun gidZeroDoesNotBecomeRoot() {
        assertEquals(1000, RootIdentity.parseUid("uid=1000(system) gid=0(root)"))
        assertFalse(RootProbe(Verification.VERIFIED, uid = 1000, uidClass = UidClass.SYSTEM).isUid0)
    }

    @Test fun laterUidZeroOrPrefixedWarningsAreRejected() {
        assertNull(RootIdentity.parseUid("warning: uid=0(root)"))
        assertNull(RootIdentity.parseUid("gid=0(root) uid=0(root)"))
    }

    @Test fun systemAndAdbShellAreDistinctIdentities() {
        assertEquals(UidClass.SYSTEM, RootIdentity.classify(1000))
        assertEquals(UidClass.ADB_SHELL, RootIdentity.classify(2000))
        assertEquals(UidClass.APP, RootIdentity.classify(10123))
    }
}
