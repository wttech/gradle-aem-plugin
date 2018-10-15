package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.pkg.deploy.InstallResponse
import com.cognifide.gradle.aem.pkg.deploy.InstallResponseBuilder
import org.apache.commons.io.IOUtils
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.util.*

class InstallResponseReaderTest {

    companion object {
        val files = ArrayList<File>()
        private const val resourcePath = "src/test/resources/com/cognifide/gradle/aem/test/response/"

        @BeforeClass
        fun init() {
            val filenames = listOf("responseExampleLarge.txt", "responseExampleSmall.txt")
            for (filename in filenames) {
                files.add(File("${resourcePath}${filename}"))
            }
        }

        fun readOldWay(file: File): InstallResponse {
            val stream = FileInputStream(file)
            val body = IOUtils.toString(stream)
            return InstallResponse(body)
        }

        fun compareResponses(createdByOldMethod: InstallResponse,
                             createdByNewMethod: InstallResponse): Boolean {
            if (createdByOldMethod.errors.size == createdByNewMethod.errors.size) {
                if (createdByNewMethod.errors.containsAll(createdByOldMethod.errors)) {
                    return true
                }
            }
            return false
        }
    }

    @Before
    fun clean() {
        System.gc()
    }

    @Test
    fun shouldReceiveSameResponsesFromLargeResponseFile() {
        val stream = FileInputStream(files[0])

        val oldWayResponse = readOldWay(files[0])
        val newWayResponse = InstallResponseBuilder.buildFromStream(stream)

        assert(compareResponses(oldWayResponse, newWayResponse))
    }

    @Test
    fun shouldReceiveSameResponsesFromSmallResponseFile() {
        val stream = FileInputStream(files[1])

        val oldWayResponse = readOldWay(files[1])
        val newWayResponse = InstallResponseBuilder.buildFromStream(stream)

        assert(compareResponses(oldWayResponse, newWayResponse))
    }
}