package ctrl.veyracore

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleIdTest {
    @Test fun acceptsConservativeModuleIds() { assertTrue(ModuleId.isSafe("zygisk-next_1.0")); assertTrue(ModuleId.isSafe("a")) }
    @Test fun rejectsPathAndShellSyntax() { listOf("../x", "x/y", "x;id", "", ".", "name with space").forEach { assertFalse(ModuleId.isSafe(it)) } }
}
