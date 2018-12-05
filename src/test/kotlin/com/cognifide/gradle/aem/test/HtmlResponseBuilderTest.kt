package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.pkg.DeleteResponse
import com.cognifide.gradle.aem.pkg.HtmlResponse
import com.cognifide.gradle.aem.pkg.InstallResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.io.InputStream

class HtmlResponseBuilderTest {

    @Test
    fun shouldContainDependencyPackageException() {
        val response = InstallResponse.from(importFileAsStream("failure-dependency-exception.txt"), 4096)
        assertTrue(response.hasPackageErrors(setOf(DEPENDENCY_EXCEPTION)))
    }

    @Test
    fun shouldFinishWithSuccessStatus() {
        val response = DeleteResponse.from(importFileAsStream("example-delete.txt"), 4096)
        assertEquals(response.status, HtmlResponse.Status.SUCCESS)
    }

    companion object {
        private const val RESOURCE_PATH = "src/test/resources/com/cognifide/gradle/aem/test/response/"

        private const val DEPENDENCY_EXCEPTION = "org.apache.jackrabbit.vault.packaging.DependencyException"

        private fun importFileAsStream(filename: String): InputStream {
            return File("$RESOURCE_PATH$filename").inputStream()
        }

    }
}