package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeyraLoadRuntimeTest {
    @Test fun rootVerificationRequiresZeroExitAndUidZero() {
        assertTrue(RuntimeReply.isRoot(ShellResult(0, "uid=0(root) gid=0(root)")))
        assertTrue(RuntimeReply.isRoot(ShellResult(0, "uid=0 gid=0")))
        assertFalse(RuntimeReply.isRoot(ShellResult(0, "uid=1000(system) gid=1000")))
        assertFalse(RuntimeReply.isRoot(ShellResult(1, "uid=0(root)")))
        assertFalse(RuntimeReply.isRoot(ShellResult(0, "uid=00(root)")))
        assertFalse(RuntimeReply.isRoot(ShellResult(0, "uid=0(root)", timedOut = true)))
    }

    @Test fun runtimeDataIsOnlyTakenFromExactKeys() {
        assertEquals("1", RuntimeReply.parseField("KSU=1\nMAGISK=0", "KSU"))
        assertEquals(null, RuntimeReply.parseField("UNRELATED_KSU=1", "KSU"))
    }

    @Test fun knownModuleTransportsAreRecognized() {
        assertEquals(ModuleTransport.KERNELSU,
            InstallerOutput.parse("KSUD=/data/adb/ksu/bin/ksud\nMAGISK=/sbin/magisk").transport)
        assertEquals(ModuleTransport.MAGISK,
            InstallerOutput.parse("MAGISK=/sbin/magisk").transport)
        assertEquals(ModuleTransport.UNAVAILABLE,
            InstallerOutput.parse("KSUD=/tmp/ksud;whoami").transport)
    }

    @Test fun noFakeRootOrInstallerInDefaultSnapshot() {
        val runtime = VeyraLoadSnapshot()
        assertFalse(runtime.authorized)
        assertFalse(runtime.moduleInstaller)
        assertEquals(LoadBackend.NONE, runtime.backend)
    }
}