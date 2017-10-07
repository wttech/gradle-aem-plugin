package com.cognifide.gradle.aem.vlt

import org.apache.commons.io.FileUtils
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class VltCleanerTest {

    @Rule
    @JvmField
    var tmpDir = TemporaryFolder()

    @Test
    fun shouldCleanupDotContentWithDefaultClosing() {
        cleanUpDotContent("cleanupDotContent/defaultClosing")
    }

    @Test
    fun shouldCleanupDotContentWithSelfClosing() {
        cleanUpDotContent("cleanupDotContent/selfClosing")
    }

    private fun cleanUpDotContent(case: String) {
        val expectedFile = File(javaClass.getResource("$case-expected.xml").toURI())
        val sourceFile = File(javaClass.getResource("$case.xml").toURI())
        val testedFile = File(tmpDir.newFolder(), ".content.xml")

        FileUtils.copyFile(sourceFile, testedFile)
        VltCleaner(testedFile.parentFile, NopLogger()).cleanupDotContent(listOf(
                "jcr:lastModified",
                "jcr:created",
                "cq:lastModified",
                "cq:lastReplicat*",
                "jcr:uuid"
        ), "\n")

        val testedFileText = testedFile.bufferedReader().use { it.readText() }
        val expectedFileText = expectedFile.bufferedReader().use { it.readText() }

        assertEquals(expectedFileText, testedFileText)
    }

}