package com.cognifide.gradle.aem.common.build

import com.cognifide.gradle.aem.AemException
import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.utils.Formats
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.lexer.Syntax
import com.mitchellbosecke.pebble.loader.StringLoader
import java.io.File
import java.io.IOException
import java.io.StringWriter
import org.apache.commons.lang3.text.StrSubstitutor

class PropertyParser(private val aem: AemExtension) {

    private val project = aem.project

    private fun find(name: String): String? {
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

    private fun prop(name: String): String? {
        return find(name)?.ifBlank { null }
    }

    fun flag(vararg names: String) = names.any { flag(it) }

    fun flag(name: String): Boolean {
        val value = find(name) ?: return false

        return if (!value.isBlank()) value.toBoolean() else true
    }

    fun list(name: String, delimiter: String = ","): List<String>? {
        val value = prop(name) ?: return null

        return Formats.toList(value, delimiter)
    }

    fun map(name: String, valueDelimiter: String = ",", keyDelimiter: String = "="): Map<String, String>? {
        val value = prop(name) ?: return null

        return Formats.toMap(value, valueDelimiter, keyDelimiter)
    }

    fun boolean(name: String) = prop(name)?.toBoolean()

    fun long(name: String) = prop(name)?.toLong()

    fun int(name: String) = prop(name)?.toInt()

    fun string(name: String) = prop(name)

    fun expand(file: File, props: Map<String, Any?>) {
        file.writeText(expand(file.readText(), props, file.toString()))
    }

    fun expand(source: String, props: Map<String, Any?>, context: String? = null): String {
        return expand(source, envProps + systemProps + props, props, context)
    }

    fun expandPackage(source: String, props: Map<String, Any?>, context: String? = null): String {
        val interpolableProps = envProps + systemProps + props
        val templateProps = projectProps + commonProps + props

        return expand(source, interpolableProps, templateProps, context)
    }

    private fun expand(
        source: String,
        interpolableProps: Map<String, Any?>,
        templateProps: Map<String, Any?>,
        context: String? = null
    ): String {
        try {
            val interpolated = TEMPLATE_INTERPOLATOR(source, interpolableProps)
            val expanded = StringWriter()

            TEMPLATE_ENGINE.getTemplate(interpolated).evaluate(expanded, templateProps)

            return expanded.toString()
        } catch (e: IOException) {
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

    val commonProps: Map<String, Any>
        get() = mapOf("aem" to aem)

    fun isForce(): Boolean {
        return flag(FORCE_PROP)
    }

    fun checkForce() {
        if (!isForce()) {
            throw AemException("Unable to perform unsafe operation without param '-P$FORCE_PROP'")
        }
    }

    companion object {

        const val FORCE_PROP = "force"

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

        private val TEMPLATE_INTERPOLATOR: (String, Map<String, Any?>) -> String = { source, props ->
            StrSubstitutor.replace(source, props, TEMPLATE_VAR_PREFIX, TEMPLATE_VAR_SUFFIX)
        }
    }
}
