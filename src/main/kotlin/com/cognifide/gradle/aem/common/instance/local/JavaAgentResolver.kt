package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.cognifide.gradle.common.utils.using

class JavaAgentResolver(private val aem: AemExtension) {

    private val common = aem.common

    private val fileResolver = FileResolver(common).apply {
        downloadDir.convention(aem.obj.buildDir("localInstance/javaAgent"))
        aem.prop.file("localInstance.javaAgent.downloadDir")?.let { downloadDir.set(it) }
        aem.prop.list("localInstance.javaAgent.urls")?.forEachIndexed { index, url ->
            val no = index + 1
            val fileName = url.substringAfterLast("/").substringBeforeLast(".")

            group("cmd.$no.$fileName") { get(url) }
        }
    }



    fun files(configurer: FileResolver.() -> Unit) = fileResolver.using(configurer)

    fun files(vararg values: Any) = fileResolver.getAll(values.asIterable())

    fun files(values: Iterable<Any>) = fileResolver.getAll(values)

    val files get() = fileResolver.files

    fun jacoco(version: String = "0.8.8") = files(aem.prop.string("localInstance.javaAgent.jacocoUrl")
        ?: "https://repo1.maven.org/maven2/org/jacoco/org.jacoco.agent/$version/org.jacoco.agent-$version-runtime.jar")

    fun newRelic() = files(aem.prop.string("localInstance.javaAgent.newRelicUrl")
        ?: "https://download.newrelic.com/newrelic/java-agent/newrelic-agent/current/newrelic-agent.jar")

    fun newRelic(version: String) = files("https://download.newrelic.com/newrelic/java-agent/newrelic-agent/$version/newrelic-agent-$version.jar")

    fun openTelemetry() = files(aem.prop.string("localInstance.javaAgent.openTelemetryUrl")
        ?: "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar")

    fun openTelemetry(version: String) = files("https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$version/opentelemetry-javaagent.jar")
}
