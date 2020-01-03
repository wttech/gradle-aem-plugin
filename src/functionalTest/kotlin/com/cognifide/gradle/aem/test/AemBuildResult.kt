package com.cognifide.gradle.aem.test

import aQute.bnd.osgi.Jar
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import org.zeroturnaround.zip.ZipUtil
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.Attributes

class AemBuildResult(val result: BuildResult, val projectDir: File) {

    fun file(path: String): File = projectDir.resolve(path)

    fun assertFileExists(path: String) = assertFileExists(file(path))

    fun assertFileExists(file: File) {
        assertTrue({ file.exists() }, "File does not exist: $file")
    }

    fun assertZipEntry(zipPath: String, entry: String, expectedContent: String? = null) = assertZipEntry(file(zipPath), entry, expectedContent)

    fun assertZipEntry(zip: File, entry: String, expectedContent: String? = null) {
        assertTrue({ ZipUtil.containsEntry(zip, entry) }, "File '$entry' is not included in package '$zip'.")

        val actualContent = ZipUtil.unpackEntry(zip, entry) ?: ByteArray(0)
        if (expectedContent != null) {
            val actualString = actualContent.toString(StandardCharsets.UTF_8).trim()
            val expectedContentTrimmed = expectedContent.trimIndent().trim()

            assertEquals(expectedContentTrimmed, actualString, "Content of entry '$entry' included in ZIP '$zip' differs from expected one.")
        } else {
            assertTrue(actualContent.isNotEmpty(), "File '$entry' included in ZIP '$zip' cannot be empty.")
        }
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

    fun assertPackageBundle(pkgPath: String, entry: String, tests: Jar.() -> Unit = {}) = assertPackageBundle(file(pkgPath), entry, tests)

    fun assertPackageBundle(pkg: File, entry: String, tests: Jar.() -> Unit = {}) {
        assertZipEntry(pkg, entry)

        val jar = Jar(pkg.name, ByteArrayInputStream(ZipUtil.unpackEntry(pkg, entry)))
        val attributes = jar.manifest.mainAttributes

        assertFalse({ isBundle(attributes) }, "File '$entry' included in package '$pkg' is not a valid OSGi bundle.")

        jar.apply(tests)
    }

    fun isBundle(attributes: Attributes) = attributes.getValue("Bundle-SymbolicName").isNullOrBlank()

    fun assertPackageVaultFiles(pkg: File) {
        VAULT_FILES.onEach { assertZipEntry(pkg, it) }
    }

    companion object {
        val VAULT_FILES = listOf(
                "META-INF/vault/config.xml",
                "META-INF/vault/definition/thumbnail.png",
                "META-INF/vault/filter.xml",
                "META-INF/vault/nodetypes.cnd",
                "META-INF/vault/privileges.xml",
                "META-INF/vault/properties.xml",
                "META-INF/vault/settings.xml"
        )
    }

}