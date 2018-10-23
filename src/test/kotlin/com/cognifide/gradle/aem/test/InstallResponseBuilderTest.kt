package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.pkg.deploy.PackageError
import com.cognifide.gradle.aem.pkg.deploy.InstallResponse
import com.cognifide.gradle.aem.pkg.deploy.InstallResponseBuilder
import org.junit.Assert.assertTrue
import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.FileInputStream


@RunWith(Parameterized::class)
class InstallResponseBuilderTest(filename: String, private val expectedError: PackageError?) {
    var file: File = File("$RESOURCE_PATH$filename")

    companion object {
        private const val RESOURCE_PATH = "src/test/resources/com/cognifide/gradle/aem/test/response/"

        @JvmStatic
        @Parameterized.Parameters()
        fun files(): Collection<*> {
            return listOf(
                    arrayOf("success-starterkit-1.txt", null),
                    arrayOf("success-starterkit-2.txt", null),
                    arrayOf("failure-starterkit-1.txt", PackageError.CONSTRAINT_VIOLATION_EXCEPTION),
                    arrayOf("failure-starterkit-2.txt", null),
                    arrayOf("failure-dependency-exception.txt", PackageError.DEPENDENCY_EXCEPTION))
        }

        fun readAtOnce(file: File): InstallResponse {
            val stream = FileInputStream(file)
            val body = IOUtils.toString(stream)
            return InstallResponse.from(body)
        }

        fun compareResponses(readAtOnce: InstallResponse,
                             readPartially: InstallResponse): Boolean {
            return (readAtOnce.errors.size == readPartially.errors.size &&
                    readAtOnce.success == readPartially.success &&
                    readPartially.errors.containsAll(readAtOnce.errors))
        }
    }


    @Before
    fun clean() {
        System.gc()
    }

    @Test
    fun a(){

    }

    @Test()
    fun shouldReceiveSameResponses() {
        val stream = FileInputStream(file)
        val oldWayResponse = readAtOnce(file)
        val newWayResponse = InstallResponseBuilder.buildFrom(stream)
        assertTrue(compareResponses(oldWayResponse, newWayResponse))
    }

    @Test
    fun shouldFindExpectedCriticalErrorIfDefined() {
            val stream = FileInputStream(file)
            val newWayResponse = InstallResponseBuilder.buildFrom(stream)
            val criticalErrors = PackageError.findPackageErrorsIn(newWayResponse.errors)
            expectedError?.let {
                assertTrue(criticalErrors.contains(expectedError))
            } ?: assertTrue(criticalErrors.isEmpty())
    }
}