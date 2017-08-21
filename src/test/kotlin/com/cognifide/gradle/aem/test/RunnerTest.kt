package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GFileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File

abstract class RunnerTest {

    @Rule
    @JvmField
    var tmpDir = TemporaryFolder()

    fun buildScript(scriptDirName: String, configurer: (runner: GradleRunner, projectDir: File) -> Unit) {
        val projectDir = File(tmpDir.newFolder(), scriptDirName)

        GFileUtils.mkdirs(projectDir)
        FileOperations.copyResources("test/$scriptDirName", projectDir)

        val runner = GradleRunner.create().withProjectDir(projectDir)

        configurer(runner, projectDir)
    }

}