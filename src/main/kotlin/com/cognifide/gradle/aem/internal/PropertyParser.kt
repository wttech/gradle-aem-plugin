package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.base.vlt.SyncTask
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.lexer.Syntax
import com.mitchellbosecke.pebble.loader.StringLoader
import org.apache.commons.lang3.ClassUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class PropertyParser(val project: Project) {

    companion object {

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
                        .setEnableNewLineTrimming(false)
                        .setPrintOpenDelimiter(TEMPLATE_VAR_PREFIX)
                        .setPrintCloseDelimiter(TEMPLATE_VAR_SUFFIX)
                        .build()
                )
                .build()

        private val TEMPLATE_INTERPOLATOR: (String, Map<String, Any>) -> String = { source, props ->
            StrSubstitutor.replace(source, props, TEMPLATE_VAR_PREFIX, TEMPLATE_VAR_SUFFIX)
        }
    }

    private fun prop(name: String): String? {
        var value = project.properties[name] as String?
        if (value == null) {
            value = systemProperties[name]
        }
        if (value == null) {
            value = envProperties[name]
        }

        return value
    }

    fun flag(name: String): Boolean {
        val value = prop(name) ?: return false

        return if (!value.isBlank()) value.toBoolean() else true
    }

    fun list(name: String, delimiter: String = ","): List<String> {
        val raw = prop(name) ?: return emptyList()
        val between = StringUtils.substringBetween(raw, "[", "]") ?: raw

        return between.split(delimiter)
    }

    fun date(name: String, defaultValue: Date): Date {
        val timestamp = prop(name)

        return if (timestamp.isNullOrBlank()) {
            defaultValue
        } else {
            Date(timestamp!!.toLong())
        }
    }

    fun boolean(name: String, defaultValue: Boolean): Boolean {
        return prop(name)?.toBoolean() ?: defaultValue
    }

    fun long(name: String, defaultValue: Long): Long {
        return prop(name)?.toLong() ?: defaultValue
    }

    fun int(name: String, defaultValue: Int): Int {
        return prop(name)?.toInt() ?: defaultValue
    }

    fun string(name: String) = prop(name)

    fun string(name: String, defaultValue: String): String {
        return prop(name) ?: defaultValue
    }

    fun string(name: String, defaultValue: () -> String): String {
        return prop(name) ?: defaultValue()
    }

    fun expand(source: String, props: Map<String, Any>, context: String? = null): String {
        val interpolableProps = envProperties + systemProperties + filterInterpolableProps(props)

        return expand(source, interpolableProps, props, context)
    }

    // TODO move it to compose task
    fun expandPackage(source: String, overrideProps: Map<String, Any>, context: String? = null): String {
        val interpolableProps = envProperties + systemProperties + mvnProperties + filterInterpolableProps(configProperties + overrideProps)
        val templateProps = projectProperties + configProperties + overrideProps

        return expand(source, interpolableProps, templateProps, context)
    }

    private fun filterInterpolableProps(props: Map<String, Any>): Map<String, Any> {
        return props.filterValues { it is String || ClassUtils.isPrimitiveOrWrapper(it.javaClass) }
    }

    private fun expand(source: String, interpolableProps: Map<String, Any>, templateProps: Map<String, Any>, context: String? = null): String {
        try {
            val interpolated = TEMPLATE_INTERPOLATOR(source, interpolableProps)
            val expanded = StringWriter()

            TEMPLATE_ENGINE.getTemplate(interpolated).evaluate(expanded, templateProps)

            return expanded.toString()
        } catch (e: Throwable) {
            var msg = "Cannot expand properly all properties. Probably used non-existing field name or unescaped char detected. Source: '${source.trim()}'."
            if (!context.isNullOrBlank()) msg += " Context: $context"
            throw AemException(msg, e)
        }
    }

    val envProperties by lazy {
        System.getenv()
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

    val configProperties: Map<String, Any>
        get() {
            val config = AemConfig.of(project)

            return mapOf("config" to config) + config.fileProperties
        }

    val mvnProperties: Map<String, Any>
        get() = mapOf(
                "project.groupId" to project.group,
                "project.artifactId" to project.name,
                "project.build.finalName" to "${project.name}-${project.version}"
        )

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
