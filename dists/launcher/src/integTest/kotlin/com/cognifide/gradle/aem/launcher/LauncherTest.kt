package com.cognifide.gradle.aem.launcher

import org.buildobjects.process.ProcBuilder
import org.junit.jupiter.api.Test
import java.io.File

class LauncherTest {

    val jar = File("build/libs/launcher-13.0.0.jar")

    @Test
    fun shouldDisplayHelloWorld() {
        ProcBuilder("java", "-jar", jar.absolutePath).apply {
            withOutputStream(System.out)
            withErrorStream(System.err)
            run()
        }
    }
}