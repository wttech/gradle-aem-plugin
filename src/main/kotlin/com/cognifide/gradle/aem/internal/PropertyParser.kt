package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.lexer.Syntax
import com.mitchellbosecke.pebble.loader.StringLoader
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.text.StrSubstitutor
import org.gradle.api.Project
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class PropertyParser(val project: Project) {

    companion object {

        const val FORCE_PROP = "aem.force"

        private const val TEMPLATE_VAR_PREFIX = "{{"

        private const val TEMPLATE_VAR_SUFFIX = "}}"

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
        if (project.hasProperty(name)) {
            return project.property(name).toString()
        }

        val systemValue = System.getProperty(name)
        if (systemValue != null) {
            return systemValue
        }

        val envValue = System.getenv(name)
        if (envValue != null) {
            return envValue
        }

        return null
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
            Date(timestamp.toLong())
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
        return expand(source, envProps + systemProps + props, props, context)
    }

    fun expandPackage(source: String, props: Map<String, Any>, context: String? = null): String {
        val interpolableProps = envProps + systemProps + props
        val templateProps = projectProps + configProps + props

        return expand(source, interpolableProps, templateProps, context)
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

    val envProps by lazy {
        System.getenv()
    }

    val systemProps: Map<String, String> by lazy {
        System.getProperties().entries.fold(mutableMapOf<String, String>()) { props, prop ->
            props.put(prop.key.toString(), prop.value.toString()); props
        }
    }

    val projectProps: Map<String, Any>
        get() = mapOf(
                // Gradle objects
                "rootProject" to project.rootProject,
                "project" to project,

                // Maven fallbacks
                "project.groupId" to project.group,
                "project.artifactId" to project.name,
                "project.build.finalName" to "${project.name}-${project.version}"
        )

    val configProps: Map<String, Any>
        get() = mapOf("config" to AemConfig.of(project))

    fun isForce(): Boolean {
        return flag(FORCE_PROP)
    }

    fun checkForce() {
        if (!isForce()) {
            throw AemException("Unable to perform unsafe operation without param '-P$FORCE_PROP'")
        }
    }

}
