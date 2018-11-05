package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.pkg.deploy.DeleteResponse
import com.cognifide.gradle.aem.pkg.deploy.HtmlResponse
import com.cognifide.gradle.aem.pkg.deploy.InstallResponse
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.io.InputStream

class HtmlResponseBuilderTest {

    companion object {
        private const val RESOURCE_PATH = "src/test/resources/com/cognifide/gradle/aem/test/response/"

        private const val DEPENDENCY_EXCEPTION = "org.apache.jackrabbit.vault.packaging.DependencyException"

        private fun importFileAsStream(filename: String): InputStream {
            return File("$RESOURCE_PATH$filename").inputStream()
        }

    }

    @Test
    fun shouldContainDependencyPackageException() {
        val response = InstallResponse.from(importFileAsStream("failure-dependency-exception.txt"), 4096)
        assertTrue(response.findPackageErrors(setOf(DEPENDENCY_EXCEPTION)).contains(DEPENDENCY_EXCEPTION))
    }

    @Test
    fun shouldFinishWithSuccessStatus() {
        val response = DeleteResponse.from(importFileAsStream("example-delete.txt"), 4096)
        assertEquals(response.status, HtmlResponse.Status.SUCCESS)
    }
}