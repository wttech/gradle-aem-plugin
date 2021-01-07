package com.cognifide.gradle.aem.common.instance.oak

import com.cognifide.gradle.aem.common.cli.JarException
import java.io.File
import java.io.IOException
import java.util.*

class OakRunScript(private val oakRun: OakRun, private val content: String) {

    private val aem = oakRun.aem

    private val logger = aem.logger

    private val scriptFile = oakRun.instance.dir.resolve("tmp/oakrun/${UUID.randomUUID()}.groovy")

    private val storeDir = oakRun.instance.dir.resolve("crx-quickstart/repository/segmentstore")

    fun exec() = try {
        save()
        load()
    } finally {
        clean()
    }

    private fun save(): File = try {
        scriptFile.apply {
            parentFile.mkdirs()
            writeText(content)
        }
    } catch (e: IOException) {
        throw OakRunException("Cannot save script '$scriptFile' to be loaded by OakRun!", e)
    }

    private fun load() = try {
        oakRun.jarApp.exec("console", storeDir, "--read-write", ":load $scriptFile")
    } catch (e: JarException) {
        logger.debug("OakRun script '$scriptFile' contents:\n$content")
        throw OakRunException("Cannot load script '$scriptFile' using OakRun!", e)
    }

    private fun clean() {
        if (scriptFile.exists()) {
            scriptFile.delete()
        }
    }
}
