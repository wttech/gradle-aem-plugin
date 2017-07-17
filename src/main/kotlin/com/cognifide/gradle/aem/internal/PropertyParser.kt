package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.AemConfig
import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.deploy.DeployException
import com.cognifide.gradle.aem.vlt.SyncTask
import com.fasterxml.jackson.databind.util.ISO8601Utils
import groovy.text.SimpleTemplateEngine
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import java.text.SimpleDateFormat
import java.util.*

class PropertyParser(val project: Project) {

    companion object {
        const val FILTER_DEFAULT = "*"

        const val FORCE_PROP = "aem.force"
    }

    fun filter(value: String, propName: String, propDefault: String = FILTER_DEFAULT): Boolean {
        val filters = project.properties.getOrElse(propName, { propDefault }) as String

        return filters.split(",").any { group -> Patterns.wildcard(value, group) }
    }

    fun expand(source: String, properties: Map<String, Any> = mapOf()): String {
        try {
            val interpolated = StrSubstitutor.replace(source, systemProperties)
            val allProperties = projectProperties + aemProperties + configProperties + properties
            val template = SimpleTemplateEngine().createTemplate(interpolated).make(allProperties)

            return template.toString()
        } catch (e: Throwable) {
            throw AemException("Cannot expand properly all properties. Probably used non-existing field name. Source: '$source'", e)
        }
    }

    val systemProperties: Map<String, String> by lazy {
        System.getProperties().entries.fold(mutableMapOf<String, String>(), { props, prop ->
            props.put(prop.key.toString(), prop.value.toString()); props
        })
    }

    val projectProperties: Map<String, Any>
        get() = mapOf(
                "rootProject" to project.rootProject,
                "project" to project
        )

    val aemProperties: Map<String, Any>
        get() {
            val config = AemConfig.of(project)

            return mapOf(
                    "name" to name,
                    "config" to config,
                    "instances" to config.instancesByName,
                    "buildCount" to SimpleDateFormat("yDDmmssSSS").format(config.buildDate),
                    "created" to ISO8601Utils.format(config.buildDate)
            )
        }

    val configProperties: Map<String, Any>
        get() = AemConfig.of(project).fileProperties

    val namePrefix: String = if (isUniqueProjectName()) {
        project.name
    } else {
        "${project.rootProject.name}${project.path}".replace(":", "-").substringBeforeLast("-")
    }

    val name: String
        get() = if (isUniqueProjectName()) {
            project.name
        } else {
            "$namePrefix-${project.name}"
        }

    private fun isUniqueProjectName() = project == project.rootProject || project.name == project.rootProject.name

    fun checkForce() {
        if (!project.properties.containsKey(FORCE_PROP) || !BooleanUtils.toBoolean(project.properties[FORCE_PROP] as String?)) {
            throw DeployException(
                    "Warning! This task execution must be confirmed by specyfing explicitly parameter '-P$FORCE_PROP=true'. " +
                            "Before continuing it is recommended to protect against potential data loss by checking out JCR content using '${SyncTask.NAME}' task."
            )
        }
    }

}
