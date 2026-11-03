package ctrl.veyracore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VeyraInternalProfilesTest {
    private val device = DeviceInfo(
        manufacturer = "samsung", model = "SM-S938B", android = "Android 16",
        kernel = "6.6.98-android15-8-pd6ff1cd-abogkiS938BXXSBCZG3-4k",
        fingerprint = "samsung/e3qxx/e3q:16/ABC/CZG3",
        codename = "e3q",
        buildId = "CZG3"
    )

    private fun json(models: String = """["SM-S938B"]""",
                     kernels: String = """["6.6.98"]""",
                     build: String = "[]",
                     sha: String? = null): ByteArray {
        val hash = sha?.let { ""","sha256":"$it"""" } ?: ""
        return """
            {"schemaVersion":3,"payloads":[{
                "payloadId":"target1","displayName":"Sample",
                "models":$models,"kernelVersions":$kernels,"buildIds":$build,
                "exploit":{"size":100$hash},
                "kernelsu":{"size":150}
            }]}
        """.trimIndent().toByteArray()
    }

    @Test fun evidenceFromSpecificVeyraFirmware() {
        val fullKernel = """["${device.kernel}"]"""
        val full = InternalRootProfileParser.parse("Veyra", json(
            kernels=fullKernel, build="""["CZG3"]"""))
        assertEquals(1, full.profiles.size)
        assertEquals(ProfileEvidence.EXACT_BUILD, full.profiles.single().evidence(device))
        assertTrue(full.profiles.single().buildVerified(device))
    }

    @Test fun threePartKernelIsOnlyCandidate() {
        val result = InternalRootProfileParser.parse("Veyra", json())
        assertEquals(ProfileEvidence.MODEL_KERNEL_CANDIDATE, result.profiles.single().evidence(device))
        assertFalse(result.profiles.single().buildVerified(device))
    }

    @Test fun dfWildcardProfilesAreNeverBuildVerified() {
        val profile = InternalRootProfileParser.parse("DF", json(
            models="""["*"]""", kernels="""["6.6"]""")).profiles.single()
        assertEquals(ProfileEvidence.SERIES_CANDIDATE, profile.evidence(device))
    }

    @Test fun wrongBuildAndModelAreRejected() {
        val wrongBuild = InternalRootProfileParser.parse("Veyra",
            json(build="""["DIFFERENT"]""")).profiles.single()
        val wrongModel = InternalRootProfileParser.parse("Veyra",
            json(models="""["SM-S928B"]""")).profiles.single()
        assertEquals(ProfileEvidence.NO_MATCH, wrongBuild.evidence(device))
        assertEquals(ProfileEvidence.NO_MATCH, wrongModel.evidence(device))
    }

    @Test(expected=IllegalArgumentException::class)
    fun invalidArtifactHashIsRejected() {
        InternalRootProfileParser.parse("Veyra", json(sha="not-a-valid-sha"))
    }

    @Test(expected=IllegalArgumentException::class)
    fun unknownManifestSchemaIsRejected() {
        InternalRootProfileParser.parse("Veyra",
            """{"schemaVersion":2,"payloads":[]}""".toByteArray())
    }

    @Test fun manifestDigestIsStable() {
        val data = json()
        val a = InternalRootProfileParser.parse("Veyra", data)
        val b = InternalRootProfileParser.parse("Veyra", data)
        assertEquals(64, a.manifestSha256.length)
        assertEquals(a.manifestSha256, b.manifestSha256)
    }
}