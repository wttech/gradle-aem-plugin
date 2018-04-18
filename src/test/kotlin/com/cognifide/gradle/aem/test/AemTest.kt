package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
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

}