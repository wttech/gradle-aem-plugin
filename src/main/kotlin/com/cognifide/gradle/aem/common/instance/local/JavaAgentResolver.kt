package com.cognifide.gradle.aem.common.instance.local

import com.cognifide.gradle.aem.common.instance.LocalInstanceManager
import com.cognifide.gradle.common.file.resolver.FileResolver
import com.cognifide.gradle.common.utils.using

class JavaAgentResolver(private val manager: LocalInstanceManager) {

    private val aem = manager.aem

    private val common = manager.aem.common

    private val fileResolver = FileResolver(common).apply {
        downloadDir.convention(manager.rootDir.dir("javaAgent"))
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

    fun openTelemetry() =
        files("https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/latest/download/opentelemetry-javaagent.jar")

    fun openTelemetry(version: String) =
        files("https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v$version/opentelemetry-javaagent.jar")

    fun jacoco(version: String) =
        files("https://repo1.maven.org/maven2/org/jacoco/org.jacoco.agent/$version/org.jacoco.agent-$version-runtime.jar")
}
