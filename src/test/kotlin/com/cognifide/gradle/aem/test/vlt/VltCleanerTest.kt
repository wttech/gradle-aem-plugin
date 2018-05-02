package com.cognifide.gradle.aem.test.vlt

import com.cognifide.gradle.aem.base.vlt.VltCleaner
import com.cognifide.gradle.aem.test.AemAssert.assertEqualsIgnoringLineEndings
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
    fun shouldCleanupDotContentWithDefaultClosing() {
        cleanupDotContent("cleanupDotContent/defaultClosing")
    }

    @Test
    fun shouldCleanupDotContentWithSelfClosing() {
        cleanupDotContent("cleanupDotContent/selfClosing")
    }

    private fun cleanupDotContent(case: String) {
        val expectedFile = File(javaClass.getResource("$case-expected.xml").toURI())
        val sourceFile = File(javaClass.getResource("$case.xml").toURI())
        val testedFile = File(tmpDir.newFolder(), ".content.xml")

        FileUtils.copyFile(sourceFile, testedFile)
        VltCleaner(testedFile.parentFile, NopLogger()).cleanupDotContent(listOf(
                "jcr:lastModified",
                "jcr:created",
                "cq:lastModified*",
                "cq:lastReplicat*",
                "jcr:uuid",
                "*_x0040_Delete",
                "*_x0040_TypeHint"
        ), "\r\n")

        val testedFileText = testedFile.bufferedReader().use { it.readText() }
        val expectedFileText = expectedFile.bufferedReader().use { it.readText() }

        assertEqualsIgnoringLineEndings(expectedFileText, testedFileText)
    }

}