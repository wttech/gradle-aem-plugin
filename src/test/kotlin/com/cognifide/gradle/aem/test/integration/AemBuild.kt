package com.cognifide.gradle.aem.test.integration

import aQute.bnd.osgi.Jar
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File

class AemBuild(val result: BuildResult, val projectDir: File) {

    fun file(path: String): File = File(projectDir, path)

    fun assertFileExists(message: String, path: String) {
        assertFileExists(message, file(path))
    }

    fun assertFileExists(message: String, file: File) {
        assertTrue({ file.exists() }, message)
    }

    fun assertTaskOutcome(taskName: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        assertEquals(outcome, result.task(taskName)?.outcome)
    }

    fun assertTaskOutcomes(taskName: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        result.tasks.filter { it.path.endsWith(":$taskName") }.forEach { assertTaskOutcome(it.path, outcome) }
    }

    fun assertPackage(path: String) {
        assertPackage(file(path))
    }

    fun assertPackage(pkg: File) {
        assertTrue({ pkg.exists() }, "Package does not exist: $pkg")
        assertPackageVaultFiles(pkg)
    }

    fun assertPackageFile(pkg: File, entry: String) {
        assertTrue({ ZipUtil.containsEntry(pkg, entry) }, "File '$entry' is not included in package '$pkg'.")
        assertTrue({ ZipUtil.unpackEntry(pkg, entry).isNotEmpty() }, "File '$entry' included in package '$pkg' cannot be empty.")
    }

    fun assertPackageBundle(pkg: File, entry: String, tests: Jar.() -> Unit = {}) {
        assertPackageFile(pkg, entry)

        val jar = Jar(pkg.name, ByteArrayInputStream(ZipUtil.unpackEntry(pkg, entry)))
        val attributes = jar.manifest.mainAttributes

        assertFalse(
                { attributes.getValue("Bundle-SymbolicName").isNullOrBlank() },
                "File '$entry' included in package '$pkg' is not a valid OSGi bundle."
        )

        jar.apply(tests)
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