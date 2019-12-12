package com.cognifide.gradle.aem.test

import aQute.bnd.osgi.Jar
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.util.jar.Attributes

class AemBuildResult(val result: BuildResult, val projectDir: File) {

    fun file(path: String): File = projectDir.resolve(path)

    fun assertFileExists(path: String) = assertFileExists(file(path))

    fun assertFileExists(file: File) {
        assertTrue({ file.exists() }, "File does not exist: $file")
    }

    fun assertTask(taskPath: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        val task = result.task(taskPath)
        assertNotNull(task, "Build result does not contain task with path '$taskPath'")
        assertEquals(outcome, task!!.outcome)
    }

    fun assertTasks(taskName: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        result.tasks.filter { it.path.endsWith(":$taskName") }.forEach { assertTask(it.path, outcome) }
    }

    fun assertBundle(path: String) = assertBundle(file(path))

    fun assertBundle(bundle: File, tests: Jar.() -> Unit = {}) {
        assertTrue({ bundle.exists() }, "OSGi bundle does not exist: $bundle")

        val jar = Jar(bundle)
        val attributes = jar.manifest.mainAttributes

        assertFalse({ isBundle(attributes) }, "File '$bundle' is not a valid OSGi bundle.")

        jar.apply(tests)
    }

    fun assertPackage(path: String) = assertPackage(file(path))

    fun assertPackage(pkg: File) {
        assertTrue({ pkg.exists() }, "Package does not exist: $pkg")
        assertPackageVaultFiles(pkg)
    }

    fun assertPackageFile(pkg: File, entry: String) {
        assertTrue({ ZipUtil.containsEntry(pkg, entry) }, "File '$entry' is not included in package '$pkg'.")
        assertTrue({ ZipUtil.unpackEntry(pkg, entry).isNotEmpty() }, "File '$entry' included in package '$pkg' cannot be empty.")
    }

    fun assertPackageBundle(pkgPath: String, entry: String, tests: Jar.() -> Unit = {}) = assertPackageBundle(file(pkgPath), entry, tests)

    fun assertPackageBundle(pkg: File, entry: String, tests: Jar.() -> Unit = {}) {
        assertPackageFile(pkg, entry)

        val jar = Jar(pkg.name, ByteArrayInputStream(ZipUtil.unpackEntry(pkg, entry)))
        val attributes = jar.manifest.mainAttributes

        assertFalse({ isBundle(attributes) }, "File '$entry' included in package '$pkg' is not a valid OSGi bundle.")

        jar.apply(tests)
    }

    fun isBundle(attributes: Attributes) = attributes.getValue("Bundle-SymbolicName").isNullOrBlank()

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