package com.deniscerri.ytdl.util

import org.junit.Assert.*
import org.junit.Test

class SolidReleaseVersionTest {
    @Test fun newerBuildUpgradesEveryAbi() {
        val old = SolidReleaseVersion.apkVersionCode("v1.9.0-solid.9.99")!!
        val next = SolidReleaseVersion.apkVersionCode("v1.9.0-solid.10.1")!!
        assertTrue(next > old + 4)
    }
    @Test fun rerunHasAnIncreasingCode() {
        assertTrue(SolidReleaseVersion.apkVersionCode("v1.9.0-solid.10.2")!! >
            SolidReleaseVersion.apkVersionCode("v1.9.0-solid.10.1")!! + 4)
    }
    @Test fun rejectsUpstreamTagsAndOverflow() {
        listOf("v1.9.0", "v1.9.0-beta", "v1.9.0-solid.1.100",
            "v1.9.0-solid.999999999999999999999.1", "v1.9.0-solid.190000.1")
            .forEach { assertNull(SolidReleaseVersion.apkVersionCode(it)) }
    }
    @Test fun oldRunIsOlderEvenWhenRerun() {
        assertTrue(SolidReleaseVersion.apkVersionCode("v1.9.0-solid.2.99")!! <
            SolidReleaseVersion.apkVersionCode("v1.9.0-solid.3.1")!!)
    }
}
