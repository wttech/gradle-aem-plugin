package com.cognifide.gradle.aem.test

import com.cognifide.gradle.aem.internal.file.FileOperations
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GFileUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.zeroturnaround.zip.ZipUtil
import java.io.File

abstract class BuildTest {

    companion object {
        val VAULT_FILES = listOf(
                "META-INF/vault/properties.xml",
                "META-INF/vault/filter.xml",
                "META-INF/vault/settings.xml"
        )
    }

    @Rule
    @JvmField
    var tmpDir = TemporaryFolder()

    fun buildScript(scriptDir: String, configurer: (runner: GradleRunner, projectDir: File) -> Unit) {
        val projectDir = File(tmpDir.newFolder(), scriptDir)

        GFileUtils.mkdirs(projectDir)
        FileOperations.copyResources("test/$scriptDir", projectDir)

        val runner = GradleRunner.create().withProjectDir(projectDir)

        configurer(runner, projectDir)
    }

    fun assertTaskOutcomes(build: BuildResult, taskName: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        build.tasks.filter { it.path.endsWith(taskName) }.forEach { assertTaskOutcome(build, it.path, outcome) }
    }

    fun assertTaskOutcome(build: BuildResult, taskName: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        assertEquals(outcome, build.task(taskName)?.outcome)
    }

    fun assertPackage(projectDir: File, path: String): File {
        val pkg = File(projectDir, path)

        assertTrue("Composed CRX package does not exist: $pkg", pkg.exists())
        assertPackageVaultFiles(pkg)

        return pkg
    }

    fun assertPackageFile(pkg: File, entry: String) {
        assertPackageFile("Required file '$entry' is not included in CRX package '$pkg'", pkg, entry)
    }

    fun assertPackageFile(message: String, pkg: File, entry: String) {
        assertTrue(message, ZipUtil.containsEntry(pkg, entry))
    }

    fun assertPackageVaultFiles(pkg: File) {
        VAULT_FILES.onEach { assertPackageFile(pkg, it) }
    }

}