package com.cognifide.gradle.aem.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.zeroturnaround.zip.ZipUtil
import java.io.File

class AemBuild(val result: BuildResult, val projectDir: File) {

    fun file(path: String): File = File(projectDir, path)

    fun assertFileExists(message: String, path: String) {
        assertFileExists(message, file(path))
    }

    fun assertFileExists(message: String, file: File) {
        assertTrue(message, file.exists())
    }

    fun assertTaskOutcome(taskName: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        assertEquals(outcome, result.task(taskName)?.outcome)
    }

    fun assertTaskOutcomes(taskName: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        result.tasks.filter { it.path.endsWith(taskName) }.forEach { assertTaskOutcome(it.path, outcome) }
    }

    fun assertPackage(path: String) {
        assertPackage(file(path))
    }

    fun assertPackage(file: File) {
        assertTrue("Composed CRX package does not exist: $file", file.exists())
        assertPackageVaultFiles(file)
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

    companion object {
        val VAULT_FILES = listOf(
                "META-INF/vault/properties.xml",
                "META-INF/vault/filter.xml",
                "META-INF/vault/settings.xml"
        )
    }

}