package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator
import java.io.File

abstract class AemTest {

    @Rule
    @JvmField
    var tmpDir = TemporaryFolder()

    fun buildTask(rootProjectDir: String, taskName: String, callback: AemBuild.() -> Unit) {
        build(rootProjectDir, { it.withArguments(taskName, "-i", "-S") }, {
            assertTaskOutcome(taskName)
            callback()
        })
    }

    fun buildTasks(rootProjectDir: String, taskName: String, callback: AemBuild.() -> Unit) {
        build(rootProjectDir, { it.withArguments(taskName, "-i", "-S") }, {
            assertTaskOutcomes(taskName)
            callback()
        })
    }

    fun build(rootProjectDir: String, configurer: (GradleRunner) -> Unit, callback: AemBuild.() -> Unit) {
        val projectDir = File(tmpDir.newFolder(), rootProjectDir)

        GFileUtils.mkdirs(projectDir)
        FileOperations.copyResources("test/$rootProjectDir", projectDir)

        val runner = GradleRunner.create()
                .withProjectDir(projectDir)
                .apply(configurer)
        val result = runner.build()

        callback(AemBuild(result, projectDir))
    }

    fun readFile(fileName: String): String {
        return javaClass.getResourceAsStream(fileName).bufferedReader().use { it.readText() }
    }

    fun readFile(file: File): String {
        return file.bufferedReader().use { it.readText() }
    }

    fun assertJson(expected: String, actual: String) {
        assertJsonIgnored(expected, actual, listOf())
    }

    fun assertJsonIgnored(expected: String, actual: String, ignoredFields: List<String>) {
        val customizations = ignoredFields.map { Customization(it, { _, _ -> true }) }

        assertJsonCustomized(expected, actual, customizations)
    }

    fun assertJsonCustomized(expected: String, actual: String, customizations: List<Customization>) {
        val comparator = CustomComparator(JSONCompareMode.STRICT, *customizations.toTypedArray())

        JSONAssert.assertEquals(expected, actual, comparator)
    }

}