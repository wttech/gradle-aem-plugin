package com.cognifide.gradle.aem.internal

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemException
import com.cognifide.gradle.aem.base.vlt.SyncTask
import com.mitchellbosecke.pebble.PebbleEngine
import com.mitchellbosecke.pebble.lexer.Syntax
import com.mitchellbosecke.pebble.loader.StringLoader
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import java.io.StringWriter
import java.util.*

class PropertyParser(val project: Project) {

    companion object {

        const val FORCE_PROP = "aem.force"

        val FORCE_MESSAGE = "Before continuing it is recommended to protect against potential data loss by checking out JCR content using '${SyncTask.NAME}' task then saving it in VCS."

        private val TEMPLATE_ENGINE = PebbleEngine.Builder()
                .autoEscaping(false)
                .cacheActive(false)
                .strictVariables(true)
                .newLineTrimming(false)
                .loader(StringLoader())
                .syntax(Syntax.Builder()
                        .setEnableNewLineTrimming(false)
                        .setPrintOpenDelimiter("{{")
                        .setPrintCloseDelimiter("}}")
                        .build()
                )
                .build()
    }

    private fun prop(name: String): String? {
        var value = project.properties[name] as String?
        if (value == null) {
            value = systemProperties["system"]?.get(name)
        }
        if (value == null) {
            value = envProperties["env"]?.get(name)
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

    fun expandEnv(source: String, overrideProps: Map<String, Any>, context: String? = null): String {
        val props = envProperties + systemProperties + overrideProps

        return expand(source, props, context)
    }

    fun expandPackage(source: String, overrideProps: Map<String, Any>, context: String? = null): String {
        val props = envProperties + systemProperties + projectProperties + configProperties + overrideProps

        return expand(source, props, context)
    }

    private fun expand(source: String, templateProps: Map<String, Any>, context: String? = null): String {
        try {
            val expanded = StringWriter()

            TEMPLATE_ENGINE.getTemplate(source).evaluate(expanded, templateProps)

            return expanded.toString()
        } catch (e: Throwable) {
            var msg = "Cannot expand properly all properties. Probably used non-existing field name or unescaped char detected. Source: '${source.trim()}'."
            if (!context.isNullOrBlank()) msg += " Context: $context"
            throw AemException(msg, e)
        }
    }

    val envProperties: Map<String, Map<String, String?>> by lazy {
        mapOf("env" to System.getenv())
    }

    val systemProperties: Map<String, Map<String, String?>> by lazy {
        mapOf("system" to System.getProperties().entries.fold(mutableMapOf<String, String>(), { props, prop ->
            props[prop.key.toString()] = prop.value.toString(); props
        }))
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
