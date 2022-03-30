package com.cognifide.gradle.aem.launcher

import org.buildobjects.process.ProcBuilder
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

class LauncherTest {

    @Test
    fun shouldRunProperly() = makeTestProject("tasks", null) { launch("tasks") }

    @Test
    fun shouldDisplayInstanceStatus() = makeTestProject("instance-status", "6.5.10") {
        launch("instanceStatus", "-Pinstance.local-author.httpUrl=http://localhost:8502") // some unavailable instance
    }

    private fun makeTestProject(name: String, aemVersion: String?, callback: File.() -> Unit) {
        File("build/functionalTest/$name").apply {
            deleteRecursively()
            mkdirs()
            if (aemVersion != null) {
                resolve("archetype.properties").let { f ->
                    f.outputStream().use { out ->
                        Properties().apply {
                            put("aemVersion", aemVersion)
                            store(out, null)
                        }
                    }
                }
            }
            callback()
        }
    }

    private fun File.launch(vararg args: String) {
        ProcBuilder("java", "-jar", File("build/libs/gap.jar").absolutePath, *args).apply {
            withWorkingDirectory(this@launch)
            withOutputStream(System.out)
            withErrorStream(System.err)
            withTimeoutMillis(TimeUnit.SECONDS.toMillis(120))
            run()
        }
    }
}
