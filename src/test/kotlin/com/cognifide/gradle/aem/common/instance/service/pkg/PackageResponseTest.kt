package com.cognifide.gradle.aem.common.instance.service.pkg

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream

class PackageResponseTest {

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
        private const val DEPENDENCY_EXCEPTION = "org.apache.jackrabbit.vault.packaging.DependencyException"

        private fun importFileAsStream(fileName: String): InputStream {
            return this::class.java.getResource("package-response/$fileName").openStream()
        }
    }
}
