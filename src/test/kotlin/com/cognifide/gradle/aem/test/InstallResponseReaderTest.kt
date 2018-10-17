package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.pkg.deploy.CriticalInstallationError
import com.cognifide.gradle.aem.pkg.deploy.InstallResponse
import com.cognifide.gradle.aem.pkg.deploy.InstallResponseBuilder
import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.collections.HashMap

class InstallResponseReaderTest {

    companion object {
        private const val RESOURCE_PATH = "src/test/resources/com/cognifide/gradle/aem/test/response/"
        private const val UNEQUAL_ERROR_MESSAGE = " Install responses are not the same for file: "

        private val filenames = mapOf(
                "success-sk-1" to "success-starterkit-1.txt",
                "success-sk-2" to "success-starterkit-2.txt",
                "fail-sk-1" to "failure-starterkit-1.txt",
                "fail-sk-2" to "failure-starterkit-2.txt",
                "dependencyEx" to "failure-dependency-exception.txt"
        )

        val files = HashMap<String, File>()

        @BeforeClass
        @JvmStatic
        fun init() {
            for ((key, filename) in filenames) {
                files[key] = File("$RESOURCE_PATH$filename")
            }
        }

        fun readOldWay(file: File): InstallResponse {
            val stream = FileInputStream(file)
            val body = IOUtils.toString(stream)
            return InstallResponse(body)
        }

        fun compareResponses(createdByOldMethod: InstallResponse,
                             createdByNewMethod: InstallResponse): Boolean {
            if (createdByOldMethod.errors.size == createdByNewMethod.errors.size &&
                    createdByOldMethod.success == createdByNewMethod.success &&
                    createdByNewMethod.errors.containsAll(createdByOldMethod.errors)) {
                return true
            }
            return false
        }
    }

    @Before
    fun clean() {
        System.gc()
    }

    @Test
    fun shouldReceiveSameResponsesFromSuccessReport1() {
        val file = files["success-sk-1"]!!
        val stream = FileInputStream(file)

        val oldWayResponse = readOldWay(file)
        val newWayResponse = InstallResponseBuilder.readStreamPartially(stream)

        assert(compareResponses(oldWayResponse, newWayResponse))
        { "$UNEQUAL_ERROR_MESSAGE${file.name}" }
    }

    @Test
    fun shouldReceiveSameResponsesFromSuccessReport2() {
        val file = files["success-sk-2"]!!
        val stream = FileInputStream(file)

        val oldWayResponse = readOldWay(file)
        val newWayResponse = InstallResponseBuilder.readStreamPartially(stream)

        assert(compareResponses(oldWayResponse, newWayResponse))
        { "$UNEQUAL_ERROR_MESSAGE${file.name}" }
    }

    @Test
    fun shouldReceiveSameResponsesFromFailureReport1() {
        val file = files["fail-sk-1"]!!
        val stream = FileInputStream(file)

        val oldWayResponse = readOldWay(file)
        val newWayResponse = InstallResponseBuilder.readStreamPartially(stream)

        assert(compareResponses(oldWayResponse, newWayResponse))
        { "$UNEQUAL_ERROR_MESSAGE${file.name}" }
    }

    @Test
    fun shouldReceiveSameResponsesFromFailureReport2() {
        val file = files["fail-sk-2"]!!
        val stream = FileInputStream(file)

        val oldWayResponse = readOldWay(file)
        val newWayResponse = InstallResponseBuilder.readStreamPartially(stream)

        assert(compareResponses(oldWayResponse, newWayResponse))
        { "$UNEQUAL_ERROR_MESSAGE${file.name}" }
    }

    @Test
    fun shouldFindCriticalInstallationError() {
        val file = files["dependencyEx"]!!
        val stream = FileInputStream(file)

        val oldWayResponse = readOldWay(file)
        val newWayResponse = InstallResponseBuilder.readStreamPartially(stream)

        assert(CriticalInstallationError.findCriticalErrorsIn(oldWayResponse.errors).isNotEmpty())
        { " Critical installation errors not found in ${file.name}." }
        assert(CriticalInstallationError.findCriticalErrorsIn(newWayResponse.errors).isNotEmpty())
        { " Critical installation error not found in ${file.name}." }
    }

    @Test
    fun shouldFindConstraintViolationException() {
        val file = files["fail-sk-1"]!!
        val stream = FileInputStream(file)

        val newWayResponse = InstallResponseBuilder.readStreamPartially(stream)
        val foundErrors = CriticalInstallationError.findCriticalErrorsIn(newWayResponse.errors)

        assert(foundErrors.contains(CriticalInstallationError.CONSTRAINT_VIOLATION_EXCEPTION.className))
        { "Constraint violation exception not found in ${file.name}." }
    }

}