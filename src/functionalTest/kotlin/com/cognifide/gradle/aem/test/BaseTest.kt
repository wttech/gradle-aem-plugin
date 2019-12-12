package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File

abstract class BaseTest {

    fun projectDir(path: String, definition: File.() -> Unit) = File("build/functionalTest/$path").apply {
        mkdirs()
        definition()
    }

    fun File.file(path: String, text: String) {
        resolve(path).apply { parentFile.mkdirs() }.writeText(text.trimIndent())
    }

    fun File.settingsGradle(text: String) = file("settings.gradle.kts", text)

    fun File.buildGradle(text: String) = file("build.gradle.kts", text)

    fun runBuild(projectDir: File, vararg arguments: String) = runBuild(projectDir) { withArguments(*arguments) }

    fun runBuild(projectDir: File, options: GradleRunner.() -> Unit) = GradleRunner.create().run {
        forwardOutput()
        withPluginClasspath()
        withDebug(true)
        withProjectDir(projectDir)
        apply(options)
        build()
    }

    fun assertTask(result: BuildResult, taskPath: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        assertEquals(result.task(taskPath)?.outcome, outcome)
    }
}