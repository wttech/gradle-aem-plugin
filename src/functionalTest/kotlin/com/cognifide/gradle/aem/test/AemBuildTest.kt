package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import java.io.File

abstract class AemBuildTest {

    fun prepareProject(path: String, definition: File.() -> Unit) = File("build/functionalTest/$path").apply {
        deleteRecursively()
        mkdirs()
        definition()
    }

    fun File.file(path: String, text: String) {
        resolve(path).apply { parentFile.mkdirs() }.writeText(text.trimIndent())
    }

    fun File.buildSrc(text: String) = file("buildSrc/build.gradle.kts", text)

    fun File.settingsGradle(text: String) = file("settings.gradle.kts", text)

    fun File.buildGradle(text: String) = file("build.gradle.kts", text)

    fun File.gradleProperties(text: String) = file("gradle.properties", text)

    fun runBuild(projectDir: File, vararg arguments: String, asserter: AemBuildResult.() -> Unit) {
        runBuild(projectDir, { withArguments(*arguments, "-i", "-S") }, asserter)
    }

    fun runBuild(projectDir: File, runnerOptions: GradleRunner.() -> Unit, asserter: AemBuildResult.() -> Unit) {
        AemBuildResult(runBuild(projectDir, runnerOptions), projectDir).apply(asserter)
    }

    fun runBuild(projectDir: File, options: GradleRunner.() -> Unit) = GradleRunner.create().run {
        forwardOutput()
        withPluginClasspath()
        withProjectDir(projectDir)
        apply(options)
        build()
    }

    fun assertTask(result: BuildResult, taskPath: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        val task = result.task(taskPath)

        assertNotNull(task, "Build result does not contain task with path '$taskPath'")
        assertEquals(outcome, task?.outcome)
    }
}