package com.cognifide.gradle.aem.test

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.common.utils.Patterns
import net.lingala.zip4j.ZipFile
import org.apache.commons.io.FilenameUtils
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.Attributes

class AemBuildResult(val result: BuildResult, val projectDir: File) {

    fun file(path: String): File = projectDir.resolve(path)

    fun files(path: String, pattern: String) = projectDir.resolve(path)
            .walk().filter { Patterns.wildcard(it.absolutePath, pattern) }.toList()

    fun assertFileExists(path: String) = assertFileExists(file(path))

    fun assertFileNotExists(path: String) = assertFileNotExists(file(path))

    fun assertFileExists(file: File) {
        assertTrue({ file.exists() }, "File does not exist: $file")
    }

    fun assertFileNotExists(file: File) {
        assertFalse({ file.exists() }, "File exists: $file")
    }

    fun assertZipEntry(zip: File, entry: String, matcher: (String) -> Unit) {
        assertFileExists(zip)
        assertTrue({ ZipFile(zip).getFileHeader(entry) != null }, "File '$entry' is not included in ZIP '$zip'.")

        val actualContent = readyZipEntry(zip, entry)
        val actualString = actualContent.toString(StandardCharsets.UTF_8).trim()

        matcher(actualString)
    }

    fun assertZipEntry(zipPath: String, entry: String) = assertZipEntry(file(zipPath), entry)

    fun assertZipEntry(zip: File, entry: String) = assertZipEntry(zip, entry) { actualContent ->
        assertTrue(actualContent.isNotEmpty(), "File '$entry' included in ZIP '$zip' cannot be empty.")
    }

    fun assertZipEntryEquals(zipPath: String, entry: String, expectedContent: String) = assertZipEntryEquals(file(zipPath), entry, expectedContent)

    fun assertZipEntryEquals(zip: File, entry: String, expectedContent: String) = assertZipEntry(zip, entry) { actual ->
        val expectedNormalized = normalizeString(expectedContent)
        val actualNormalized = normalizeString(actual)

        assertEquals(expectedNormalized, actualNormalized, "Content of entry '$entry' included in ZIP '$zip' differs from expected one.")
    }

    fun assertZipEntryMatching(zipPath: String, entry: String, expectedContent: String) = assertZipEntryMatching(file(zipPath), entry, expectedContent)

    fun assertZipEntryMatching(zip: File, entry: String, expectedContent: String) = assertZipEntry(zip, entry) { actual ->
        val expectedNormalized = normalizeString(expectedContent)
        val actualNormalized = normalizeString(actual)

        assertTrue(FilenameUtils.wildcardMatch(actual, expectedNormalized),
                "Content of entry '$entry' included in ZIP '$zip' does not match expected pattern.\n\n" +
                        "==> expected content pattern:\n\n$expectedNormalized\n\n==> actual content:\n\n$actualNormalized\n\n")
    }

    fun assertTask(taskPath: String, outcome: TaskOutcome = TaskOutcome.SUCCESS) {
        val task = result.task(taskPath)
        assertNotNull(task, "Build result does not contain task with path '$taskPath'")
        assertEquals(outcome, task?.outcome)
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
        assertPackageMetaFiles(pkg)
    }

    fun assertPackageBundle(pkgPath: String, entry: String, tests: Jar.() -> Unit = {}) = assertPackageBundle(file(pkgPath), entry, tests)

    fun assertPackageBundle(pkg: File, entry: String, tests: Jar.() -> Unit = {}) {
        assertZipEntry(pkg, entry)

        val jar = Jar(pkg.name, ByteArrayInputStream(readyZipEntry(pkg, entry)))
        val attributes = jar.manifest.mainAttributes

        assertFalse({ isBundle(attributes) }, "File '$entry' included in package '$pkg' is not a valid OSGi bundle.")

        jar.apply(tests)
    }

    fun isBundle(attributes: Attributes) = attributes.getValue("Bundle-SymbolicName").isNullOrBlank()

    fun assertPackageMetaFiles(pkg: File) {
        PACKAGE_META_FILES.onEach { assertZipEntry(pkg, it) }
    }

    private fun readyZipEntry(zip: File, entry: String) = ZipFile(zip).run {
        getFileHeader(entry)?.let { h -> getInputStream(h).use { it.readBytes() } }
    } ?: ByteArray(0)

    private fun normalizeString(string: String) = string.trimIndent().replace("\r\n", "\n").trim()

    companion object {
        val PACKAGE_META_FILES = listOf(
                // Essentials
                "META-INF/MANIFEST.MF",
                "META-INF/vault/config.xml",
                "META-INF/vault/filter.xml",
                "META-INF/vault/properties.xml",
                "META-INF/vault/settings.xml",

                // Extra
                "META-INF/vault/definition/thumbnail.png"
        )
    }
}
