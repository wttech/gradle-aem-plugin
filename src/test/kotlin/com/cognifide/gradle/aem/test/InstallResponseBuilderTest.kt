package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.pkg.deploy.InstallResponse
import org.junit.Assert.assertTrue
import org.apache.commons.io.IOUtils
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.FileInputStream


@RunWith(Parameterized::class)
class InstallResponseBuilderTest(filename: String, private val expectedError: String?) {
    var file: File = File("$RESOURCE_PATH$filename")

    companion object {
        private const val RESOURCE_PATH = "src/test/resources/com/cognifide/gradle/aem/test/response/"

        private val PACKAGE_ERRORS = mutableSetOf(

                "javax.jcr.nodetype.ConstraintViolationException",
                "org.apache.jackrabbit.vault.packaging.DependencyException",
                "org.xml.sax.SAXException")

        @JvmStatic
        @Parameterized.Parameters()
        fun files(): Collection<*> {
            return listOf(
                    arrayOf("success-starterkit-1.txt", null),
                    arrayOf("success-starterkit-2.txt", null),
                    arrayOf("failure-starterkit-1.txt", "javax.jcr.nodetype.ConstraintViolationException"),
                    arrayOf("failure-starterkit-2.txt", null),
                    arrayOf("failure-dependency-exception.txt", "org.apache.jackrabbit.vault.packaging.DependencyException"))
        }

        fun readAtOnce(file: File): InstallResponse {
            val stream = FileInputStream(file)
            val body = IOUtils.toString(stream)
            return InstallResponse(body, PACKAGE_ERRORS)
        }

        fun compareResponses(readAtOnce: InstallResponse,
                             readPartially: InstallResponse): Boolean {
            return (readAtOnce.errors.size == readPartially.errors.size &&
                    readAtOnce.success == readPartially.success)
        }
    }

    @Test()
    fun shouldReceiveSameResponses() {
        val stream = FileInputStream(file)
        val oldWayResponse = readAtOnce(file)
        val newWayResponse = InstallResponse.from(stream, PACKAGE_ERRORS)

        assertTrue(compareResponses(oldWayResponse, newWayResponse))
    }

    @Test
    fun shouldFindExpectedCriticalErrorIfDefined() {
            val stream = FileInputStream(file)
            val newWayResponse = InstallResponse.from(stream, PACKAGE_ERRORS)
            val criticalErrors = newWayResponse.encounteredPackageErrors
            expectedError?.let {
                assertTrue(criticalErrors.contains(expectedError))
            } ?: assertTrue(criticalErrors.isEmpty())
    }
}