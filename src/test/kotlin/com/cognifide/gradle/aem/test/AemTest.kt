package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.common.file.FileOperations
import org.apache.commons.io.FileUtils
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import java.io.File
import java.nio.file.Files

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class AemTest {

    lateinit var tmpDir: File

    @BeforeAll
    fun prepare() {
        this.tmpDir = Files.createTempDirectory("aemTest").toFile()
    }

    @AfterAll
    fun clean() {
        FileUtils.deleteQuietly(tmpDir)
    }

    fun buildTask(rootProjectDir: String, taskName: String, vararg args: String = ARGS_DEFAULT, callback: AemBuild.() -> Unit) {
        build(rootProjectDir, { withArguments(listOf<String>() + taskName + args.toList()) }, {
            assertTaskOutcome(taskName)
            callback()
        })
    }

    fun buildTasks(rootProjectDir: String, taskName: String, vararg args: String = ARGS_DEFAULT, callback: AemBuild.() -> Unit) {
        build(rootProjectDir, { withArguments(listOf<String>() + taskName + args.toList()) }, {
            assertTaskOutcomes(taskName)
            callback()
        })
    }

    fun build(rootProjectDir: String, configurer: GradleRunner.() -> Unit, callback: AemBuild.() -> Unit) {
        val projectDir = File(tmpDir, rootProjectDir)

        GFileUtils.mkdirs(projectDir)
        FileOperations.copyResources("test/$rootProjectDir", projectDir)

        val runner = GradleRunner.create()
                .withProjectDir(projectDir)
                .apply(configurer)
        val result = runner.build()

        callback(AemBuild(result, projectDir))
        Thread.sleep(1000)
    }

    fun readFile(fileName: String): String {
        return javaClass.getResourceAsStream(fileName).bufferedReader().use { it.readText() }
    }

    fun readFile(file: File): String {
        return file.bufferedReader().use { it.readText() }
    }

    companion object {
        val ARGS_DEFAULT = arrayOf("-i", "-S", "-Poffline")
    }
}
