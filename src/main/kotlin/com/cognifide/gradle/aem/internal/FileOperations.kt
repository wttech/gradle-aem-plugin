package com.cognifide.gradle.aem.internal

import org.apache.commons.io.IOUtils
import org.reflections.Reflections
import org.reflections.scanners.ResourcesScanner
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileOperations {

    fun eachResource(resourceRoot: String, targetDir: File, callback: (String, File) -> Unit) {
        for (resourcePath in Reflections(resourceRoot, ResourcesScanner()).getResources { true }) {
            val outputFile = File(targetDir, resourcePath.substringAfterLast("$resourceRoot/"))

            callback(resourcePath, outputFile)
        }
    }

    fun copyResources(resourceRoot: String, targetDir: File, skipExisting: Boolean = true,
                      transformer: (File, InputStream) -> InputStream = { _, input -> input }) {
        eachResource(resourceRoot, targetDir, { resourcePath, outputFile ->
            if (!skipExisting || !outputFile.exists()) {
                copyResource(resourcePath, outputFile, transformer)
            }
        })
    }

    fun copyResource(resourcePath: String, outputFile: File,
                     transformer: (File, InputStream) -> InputStream = { _, input -> input }) {
        val input = javaClass.getResourceAsStream("/" + resourcePath)
        val output = FileOutputStream(outputFile)

        try {
            IOUtils.copy(transformer(outputFile, input), output)
        } finally {
            IOUtils.closeQuietly(input)
            IOUtils.closeQuietly(output)
        }
    }


}