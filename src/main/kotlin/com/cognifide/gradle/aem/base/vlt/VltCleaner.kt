package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.EmptyFileFilter
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class VltCleaner(val project: Project) {

    private val logger = project.logger

    private val config = AemConfig.of(project)

    private val props = PropertyParser(project)

    /**
     * Determines which files will be deleted within running cleaning
     * (e.g after checking out JCR content).
     */
    var filesDeleted: MutableList<String> = mutableListOf(
            "**/.vlt",
            "**/.vlt*.tmp"
    )

    private val filesDeletedRules by lazy {
        VltCleanRule.manyFrom(filesDeleted)
    }

    /**
     * Properties that will be skipped when pulling JCR content from AEM instance.
     *
     * After special delimiter '!' there could be specified one or many path patterns
     * (ANT style, delimited with ',') in which property shouldn't be removed.
     */
    var propertiesSkipped: MutableList<String> = mutableListOf(
            pathRule("jcr:uuid", listOf("**/home/users/*", "**/home/groups/*")),
            "jcr:lastModified*",
            "jcr:created*",
            "jcr:isCheckedOut",
            "cq:lastModified*",
            "cq:lastReplicat*",
            "dam:extracted",
            "dam:assetState",
            "dc:modified",
            "*_x0040_*"
    )

    private val propertiesSkippedRules by lazy {
        VltCleanRule.manyFrom(propertiesSkipped)
    }

    /**
     * Mixin types that will be skipped when pulling JCR content from AEM instance.
     */
    var mixinTypesSkipped: MutableList<String> = mutableListOf(
            "cq:ReplicationStatus",
            "mix:versionable"
    )

    private val mixinTypesSkippedRules by lazy {
        VltCleanRule.manyFrom(mixinTypesSkipped)
    }

    /**
     * Controls unused namespaces skipping.
     */
    var namespacesSkipped: Boolean = props.boolean("aem.clean.namespacesSkipped", true)

    /**
     * Controls backups for parent nodes of filter roots for keeping them untouched.
     */
    var parentsBackupEnabled: Boolean = props.boolean("aem.clean.parentsBackup", true)

    /**
     * File suffix being added to parent node back up files.
     * Customize it only if really needed to resolve conflict with file being checked out.
     */
    var parentsBackupSuffix = ".bak"

    private val parentsBackupDirIndicator
        get() = "${parentsBackupSuffix}dir"

    /**
     * Hook for customizing particular line processing for '.content.xml' files.
     */
    var lineProcess: (File, String) -> String = { file, line -> normalizeLine(file, line) }

    /**
     * Hook for additional all lines processing for '.content.xml' files.
     */
    var contentProcess: (File, List<String>) -> List<String> = { file, lines -> normalizeContent(file, lines) }

    fun prepare(root: File) {
        if (parentsBackupEnabled) {
            doParentsBackup(root)
        }
    }

    fun clean(root: File) {
        removeFiles(root)
        removeEmptyDirs(root)
        cleanDotContents(root)

        if (parentsBackupEnabled) {
            undoParentsBackup(root)
        } else {
            cleanParents(root)
        }
    }

    private fun dotContentFiles(root: File): Collection<File> {
        return FileUtils.listFiles(root, NameFileFilter(JCR_CONTENT_FILE), TrueFileFilter.INSTANCE)
                ?: listOf()
    }

    private fun allFiles(root: File): Collection<File> {
        return FileUtils.listFiles(root, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                ?: listOf()
    }

    private fun cleanDotContents(root: File) {
        if (root.isDirectory) {
            dotContentFiles(root).forEach { cleanDotContentFile(it) }
        } else {
            cleanDotContentFile(root)
        }
    }

    private fun removeFiles(root: File) {
        if (root.isDirectory) {
            allFiles(root).forEach { removeFile(it) }
        } else {
            removeFile(root)
        }
    }

    private fun removeFile(file: File) {
        if (!file.exists() || !matchAnyRule(file.path, file, filesDeletedRules)) {
            return
        }

        logger.info("Deleting file {}", file.path)
        FileUtils.deleteQuietly(file)
    }

    private fun removeEmptyDirs(root: File) {
        val siblingDirs = root.listFiles { file -> file.isDirectory } ?: arrayOf()
        siblingDirs.forEach { removeEmptyDirs(it) }
        if (EmptyFileFilter.EMPTY.accept(root)) {
            logger.info("Removing empty directory {}", root.path)
            FileUtils.deleteQuietly(root)
        }
    }

    private fun cleanDotContentFile(file: File) {
        if (!file.exists() || file.name != JCR_CONTENT_FILE) {
            return
        }

        try {
            logger.info("Cleaning file {}", file.path)

            val inputLines = FileUtils.readLines(file, CharEncoding.UTF_8)
            val filteredLines = filterLines(file, inputLines)

            FileUtils.writeLines(file, CharEncoding.UTF_8, filteredLines, config.vaultLineSeparatorString)
        } catch (e: IOException) {
            throw VltException(String.format("Error opening %s", file.path), e)
        }
    }

    private fun filterLines(file: File, lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        for (line in lines) {
            val processedLine = lineProcess(file, line)
            if (processedLine.isEmpty()) {
                when {
                    result.last().endsWith(">") -> {
                        // skip line
                    }
                    line.trim().endsWith("/>") -> {
                        result.add(result.removeAt(result.size - 1) + "/>")
                    }
                    line.trim().endsWith(">") -> {
                        result.add(result.removeAt(result.size - 1) + ">")
                    }
                    else -> {
                        // skip line
                    }
                }
            } else {
                result.add(processedLine)
            }
        }

        return contentProcess(file, result)
    }

    @Suppress("UNUSED_PARAMETER")
    fun normalizeContent(file: File, lines: List<String>): List<String> {
        return cleanNamespaces(lines)
    }

    fun cleanNamespaces(lines: List<String>): List<String> {
        if (!namespacesSkipped) {
            return lines
        }

        return lines.map { line ->
            if (line.trim().startsWith(JCR_ROOT_PREFIX)) {
                line.split(" ")
                        .filter { part ->
                            val matcher = NAMESPACE_PATTERN.matcher(part)
                            if (matcher.matches()) {
                                lines.any { it.contains(matcher.group(1) + ":") }
                            } else {
                                true
                            }
                        }
                        .joinToString(" ")
            } else {
                line
            }
        }
    }

    fun normalizeLine(file: File, line: String): String {
        return normalizeMixins(file, skipProperties(file, line))
    }

    fun skipProperties(file: File, line: String): String {
        if (propertiesSkipped.isEmpty()) {
            return line
        }

        return eachProp(line) { propOccurrence, _ ->
            if (matchAnyRule(propOccurrence, file, propertiesSkippedRules)) {
                ""
            } else {
                line
            }
        }
    }

    fun normalizeMixins(file: File, line: String): String {
        if (mixinTypesSkipped.isEmpty()) {
            return line
        }

        return eachProp(line) { propName, propValue ->
            if (propName == JCR_MIXIN_TYPES_PROP) {
                val normalizedValue = StringUtils.substringBetween(propValue, "[", "]")
                val resultValues = normalizedValue.split(",").filter { !matchAnyRule(it, file, mixinTypesSkippedRules) }
                if (resultValues.isEmpty() || normalizedValue.isEmpty()) {
                    ""
                } else {
                    val resultValue = resultValues.joinToString(",")
                    line.replace(normalizedValue, resultValue)
                }
            } else {
                line
            }
        }
    }

    private fun eachProp(line: String, processProp: (String, String) -> String): String {
        val normalizedLine = line.trim().removeSuffix("/>").removeSuffix(">")
        val matcher = CONTENT_PROP_PATTERN.matcher(normalizedLine)
        return if (matcher.matches()) {
            processProp(matcher.group(1), matcher.group(2))
        } else {
            line
        }
    }

    private fun doParentsBackup(root: File) {
        val normalizedRoot = normalizeParentRoot(root)

        normalizedRoot.parentFile.mkdirs()
        eachParentFiles(normalizedRoot) { parent, siblingFiles ->
            parent.mkdirs()
            if (File(parent, parentsBackupDirIndicator).createNewFile()) {
                siblingFiles.filter { !it.name.endsWith(parentsBackupSuffix) && !matchAnyRule(it.path, it, filesDeletedRules) }
                        .forEach { origin ->
                            val backup = File(parent, origin.name + parentsBackupSuffix)
                            logger.info("Doing backup of parent file: $origin")
                            origin.copyTo(backup, true)
                        }
            }
        }
    }

    private fun undoParentsBackup(root: File) {
        val normalizedRoot = normalizeParentRoot(root)

        eachParentFiles(normalizedRoot) { _, siblingFiles ->
            if (siblingFiles.any { it.name == parentsBackupDirIndicator }) {
                siblingFiles.filter { !it.name.endsWith(parentsBackupSuffix) }.forEach { FileUtils.deleteQuietly(it) }
                siblingFiles.filter { it.name.endsWith(parentsBackupSuffix) }.forEach { backup ->
                    val origin = File(backup.path.removeSuffix(parentsBackupSuffix))
                    logger.info("Undoing backup of parent file: $backup")
                    backup.renameTo(origin)
                }
            }
        }
    }

    private fun normalizeParentRoot(root: File): File {
        return File(Patterns.normalizePath(root.path).substringBefore("/$JCR_CONTENT_NODE"))
    }

    private fun cleanParents(root: File) {
        eachParentFiles(root) { _, siblingFiles ->
            siblingFiles.forEach { removeFile(it) }
            siblingFiles.forEach { cleanDotContentFile(it) }
        }
    }

    private fun eachParentFiles(root: File, processFiles: (File, Array<File>) -> Unit) {
        var parent = root.parentFile
        while (parent != null) {
            val siblingFiles = parent.listFiles { file -> file.isFile } ?: arrayOf()
            processFiles(parent, siblingFiles)

            if (parent.name == PackagePlugin.JCR_ROOT) {
                break
            }

            parent = parent.parentFile
        }
    }

    private fun matchAnyRule(value: String, file: File, rules: List<VltCleanRule>): Boolean {
        return rules.any { it.match(file, value) }
    }

    fun pathRule(pattern: String, excludedPaths: List<String>): String {
        return pathRule(pattern, excludedPaths, listOf())
    }

    fun pathRule(pattern: String, excludedPaths: List<String>, includedPaths: List<String>): String {
        val paths = excludedPaths.map { "!$it" } + includedPaths
        return if (paths.isEmpty()) {
            pattern
        } else {
            pattern + "|" + paths.joinToString(",")
        }
    }

    companion object {
        const val JCR_CONTENT_FILE = ".content.xml"

        const val JCR_CONTENT_NODE = "jcr:content"

        const val JCR_MIXIN_TYPES_PROP = "jcr:mixinTypes"

        const val JCR_ROOT_PREFIX = "<jcr:root"

        val CONTENT_PROP_PATTERN: Pattern = Pattern.compile("([^ =]+)=\"([^\"]+)\"")

        val NAMESPACE_PATTERN: Pattern = Pattern.compile("\\w+:(\\w+)=\"[^\"]+\"")
    }
}
