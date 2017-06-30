package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemTask
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.TaskAction

open class DebugTask : DefaultTask(), AemTask {

    companion object {
        val NAME = "aemDebug"
    }

    @Nested
    final override val config = AemConfig.of(project)

    @Internal
    private val propertyParser = PropertyParser(project)

    init {
        group = AemTask.GROUP
        description = "Shows detailed AEM build configuration."
    }

    @TaskAction
    fun show() {
        printHeader("Gradle project details")
        println(projectAsJson())

        printHeader("Effective configuration")
        println(configAsJson())
    }

    private fun projectAsJson(): String? {
        return toJson(mapOf(
                "projectPath" to project.path,
                "projectName" to project.name,
                "projectDir" to project.projectDir.absolutePath,
                "aemName" to propertyParser.name
        ))
    }

    private fun toJson(value: Any): String? {
        return ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValueAsString(value)
    }

    private fun configAsJson(): String? {
        return toJson(config)
    }

    private fun printHeader(header: String) {
        println("=====< $header >=====")
    }

}
