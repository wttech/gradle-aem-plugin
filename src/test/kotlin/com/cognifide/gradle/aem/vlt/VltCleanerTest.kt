package com.cognifide.gradle.aem.vlt

import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VltCleanerTest {

    @Rule
    @JvmField
    var tmpDir = TemporaryFolder()

    @Test
    fun shouldFilterLinesProperly() {
        val sourceFile = File(javaClass.getResource("filterLines.xml").toURI())
        val testedFile = File(tmpDir.newFolder(), "filterLines.xml")

        FileUtils.copyFileToDirectory(sourceFile,testedFile )
        VltCleaner(testedFile, NopLogger()).cleanupDotContent(listOf(
            "jcr:lastModified",
            "jcr:created",
            "cq:lastModified",
            "cq:lastReplicat*",
            "jcr:uuid"
        ), "\n")
    }

}