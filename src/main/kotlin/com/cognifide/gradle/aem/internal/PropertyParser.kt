package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.base.api.AemConfig
import com.cognifide.gradle.aem.base.api.AemException
import com.cognifide.gradle.aem.base.vlt.SyncTask
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.lexer.Syntax
import com.mitchellbosecke.pebble.loader.StringLoader
import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.ClassUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import java.io.StringWriter
import java.text.SimpleDateFormat

class PropertyParser(val project: Project) {

    companion object {
        const val FILTER_DEFAULT = "*"

        const val FORCE_PROP = "aem.force"

        val FORCE_MESSAGE = "Before continuing it is recommended to protect against potential data loss by checking out JCR content using '${SyncTask.NAME}' task then saving it in VCS."

        private val TEMPLATE_VAR_PREFIX = "{{"

        private val TEMPLATE_VAR_SUFFIX = "}}"

        private val TEMPLATE_ENGINE = PebbleEngine.Builder()
                .autoEscaping(false)
                .cacheActive(false)
                .strictVariables(true)
                .newLineTrimming(false)
                .loader(StringLoader())
                .syntax(Syntax.Builder()
                        .setPrintOpenDelimiter(TEMPLATE_VAR_PREFIX)
                        .setPrintCloseDelimiter(TEMPLATE_VAR_SUFFIX)
                        .build()
                )
                .build()

        private val TEMPLATE_INTERPOLATOR: (String, Map<String, Any>) -> String = { source, props ->
            StrSubstitutor.replace(source, props, TEMPLATE_VAR_PREFIX, TEMPLATE_VAR_SUFFIX)
        }
    }

    fun prop(name: String): String? {
        var value = project.properties[name] as String?
        if (value == null) {
            value = systemProperties[name]
        }

        return value
    }

    fun flag(name: String): Boolean {
        return project.properties.containsKey(name) && BooleanUtils.toBoolean(project.properties[name] as String?)
    }

    fun list(name: String, delimiter: String = ","): List<String> {
        val raw = prop(name) ?: return emptyList()
        val between = StringUtils.substringBetween(raw, "[", "]") ?: raw

        return between.split(delimiter)
    }

    fun prop(name: String, defaultValue: () -> String): String {
        return prop(name) ?: defaultValue()
    }

    fun filter(value: String, propName: String, propDefault: String = FILTER_DEFAULT): Boolean {
        val filters = project.properties.getOrElse(propName, { propDefault }) as String

        return filters.split(",").any { group -> Patterns.wildcard(value, group) }
    }

    fun expand(source: String, properties: Map<String, Any> = mapOf(), context: String? = null): String {
        try {
            val interpolableProperties = systemProperties + mvnProperties + configProperties.filterValues {
                it is String || ClassUtils.isPrimitiveOrWrapper(it.javaClass)
            }
            val interpolated = TEMPLATE_INTERPOLATOR(source, interpolableProperties)

            val templateProperties = projectProperties + aemProperties + configProperties + properties
            val expanded = StringWriter()

            TEMPLATE_ENGINE.getTemplate(interpolated).evaluate(expanded, templateProperties)

            return expanded.toString()
        } catch (e: Throwable) {
            var msg = "Cannot expand properly all properties. Probably used non-existing field name or unescaped char detected. Source: '${source.trim()}'."
            if (!context.isNullOrBlank()) msg += " Context: $context"
            throw AemException(msg, e)
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
                    "requiresRoot" to "false",
                    "buildCount" to SimpleDateFormat("yDDmmssSSS").format(config.buildDate),
                    "created" to Formats.date(config.buildDate)
            )
        }

    val configProperties: Map<String, Any>
        get() = AemConfig.of(project).fileProperties

    val mvnProperties: Map<String, Any>
        get() = mapOf(
                "project.groupId" to project.group,
                "project.artifactId" to project.name,
                "project.build.finalName" to "${project.name}-${project.version}"
        )

    val namePrefix: String = if (isUniqueProjectName()) {
        project.name
    } else {
        "${project.rootProject.name}${project.path}"
                .replace(":", "-")
                .replace(".", "-")
                .substringBeforeLast("-")
    }

    val name: String
        get() = if (isUniqueProjectName()) {
            project.name
        } else {
            "$namePrefix-${project.name}"
        }

    private fun isUniqueProjectName() = project == project.rootProject || project.name == project.rootProject.name

    fun isForce(): Boolean {
        return flag(FORCE_PROP)
    }

    fun checkForce(message: String = FORCE_MESSAGE) {
        if (!isForce()) {
            throw AemException("Warning! This task execution must be confirmed by specifying explicitly parameter '-P$FORCE_PROP=true'. $message")
        }
    }

    fun checkOffline(): Boolean {
        return project.gradle.startParameter.isOffline
    }

}
