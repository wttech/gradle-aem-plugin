package com.cognifide.gradle.aem.launcher

import org.buildobjects.process.ProcBuilder
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.TimeUnit

class LauncherTest {

    @Test
    fun shouldDisplayInstanceStatus() = test("instance-status") {
        launch("instanceStatus")
    }

    private fun test(name: String, callback: File.() -> Unit) {
        File("build/functionalTest/$name").apply {
            deleteRecursively()
            mkdirs()
            callback()
        }
    }

    private fun File.launch(vararg args: String) {
        ProcBuilder("java", "-jar", File("build/libs/gap.jar").absolutePath, *args).apply {
            withWorkingDirectory(this@launch)
            withOutputStream(System.out)
            withErrorStream(System.err)
            withTimeoutMillis(TimeUnit.SECONDS.toMillis(30))
            run()
        }
    }
}