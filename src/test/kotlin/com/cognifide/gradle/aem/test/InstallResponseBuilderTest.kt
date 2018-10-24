package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.pkg.deploy.InstallResponse
import org.junit.Assert.assertTrue
import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.io.FileInputStream
import java.util.stream.Collectors


@RunWith(Parameterized::class)
class InstallResponseBuilderTest(filename: String, private val expectedError: String?) {
    var file: File = File("$RESOURCE_PATH$filename")

    companion object {
        private const val RESOURCE_PATH = "src/test/resources/com/cognifide/gradle/aem/test/response/"

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
            return InstallResponse(body)
        }

//        fun findPackageErrorsIn(errors: List<String>): Set<String> {
//            return errors.fold(mutableSetOf()) { results, error ->
//                values().forEach { exception ->
//                    if (error.contains(exception.className)) results.add(exception)
//                }; results
//            }
//        }

        fun compareResponses(readAtOnce: InstallResponse,
                             readPartially: InstallResponse): Boolean {
            val one = readAtOnce.errors
                    .stream()
                    .map { it.trim() }
                    .collect(Collectors.toList())
            val two = readPartially.errors
                    .stream()
                    .map { it.trim() }
                    .collect(Collectors.toList())

            println()

            return (readAtOnce.errors.size == readPartially.errors.size &&
                    readAtOnce.success == readPartially.success &&
                    two.containsAll(one))
        }
    }


    @Before
    fun clean() {
        System.gc()
    }

    @Test()
    fun shouldReceiveSameResponses() {
        val stream = FileInputStream(file)
        val oldWayResponse = readAtOnce(file)
        val newWayResponse = InstallResponse.from(stream)

        assertTrue(compareResponses(oldWayResponse, newWayResponse))
    }

//    @Test
//    fun shouldFindExpectedCriticalErrorIfDefined() {
//            val stream = FileInputStream(file)
//            val newWayResponse = InstallResponse.from(stream)
//            val criticalErrors = PackageError.findPackageErrorsIn(newWayResponse.errors)
//            expectedError?.let {
//                assertTrue(criticalErrors.contains(expectedError))
//            } ?: assertTrue(criticalErrors.isEmpty())
//    }
}