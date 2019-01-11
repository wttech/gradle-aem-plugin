package com.cognifide.gradle.aem.tooling.clean

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.Patterns
import com.cognifide.gradle.aem.pkg.Package
import com.cognifide.gradle.aem.tooling.vlt.VltException
import java.io.File
import java.io.IOException
import java.util.regex.Pattern
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.EmptyFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

class Cleaner(project: Project) {

    @Internal
    private val aem = AemExtension.of(project)

    /**
     * Allows to control which files under each root root should be cleaned.
     */
    var filesDotContent: ConfigurableFileTree.() -> Unit = {
        include("**/$JCR_CONTENT_FILE")
    }

    /**
     * Determines which files will be deleted within running cleaning
     * (e.g after checking out JCR content).
     */
    @Input
    var filesDeleted: ConfigurableFileTree.() -> Unit = {
        include(listOf(
                "**/.vlt",
                "**/.vlt*.tmp",
                "**/install/*.jar"
        ))
    }

    /**
     * Determines which files will be flattened
     * (e.g /_cq_dialog/.content.xml will be replaced by _cq_dialog.xml).
     */
    @Input
    var filesFlattened: ConfigurableFileTree.() -> Unit = {
        include(listOf(
                "**/_cq_dialog/.content.xml",
                "**/_cq_htmlTag/.content.xml"
        ))
    }

    /**
     * Properties that will be skipped when pulling JCR content from AEM instance.
     *
     * After special delimiter '!' there could be specified one or many path patterns
     * (ANT style, delimited with ',') in which property shouldn't be removed.
     */
    @Input
    var propertiesSkipped: List<String> = listOf(
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

    /**
     * Mixin types that will be skipped when pulling JCR content from AEM instance.
     */
    @Input
    var mixinTypesSkipped: List<String> = listOf(
            "cq:ReplicationStatus",
            "mix:versionable"
    )

    /**
     * Controls unused namespaces skipping.
     */
    @Input
    var namespacesSkipped: Boolean = aem.props.boolean("aem.cleaner.namespacesSkipped") ?: true

    /**
     * Controls backups for parent nodes of filter roots for keeping them untouched.
     */
    @Input
    var parentsBackupEnabled: Boolean = aem.props.boolean("aem.cleaner.parentsBackup") ?: true

    /**
     * File suffix being added to parent node back up files.
     * Customize it only if really needed to resolve conflict with file being checked out.
     */
    @Internal
    var parentsBackupSuffix = ".bak"

    private val parentsBackupDirIndicator
        get() = "${parentsBackupSuffix}dir"

    /**
     * Hook for customizing particular line processing for '.content.xml' files.
     */
    @Internal
    var lineProcess: (File, String) -> String = { file, line -> normalizeLine(file, line) }

    /**
     * Hook for additional all lines processing for '.content.xml' files.
     */
    @Internal
    var contentProcess: (File, List<String>) -> List<String> = { file, lines -> normalizeContent(file, lines) }

    fun prepare(root: File) {
        if (parentsBackupEnabled) {
            doParentsBackup(root)
        }
    }

    fun clean(root: File) {
        if (parentsBackupEnabled) {
            doRootBackup(root)
            undoParentsBackup(root)
        } else {
            cleanParents(root)
        }

        cleanDotContents(root)
        flattenFiles(root)

        deleteFiles(root)
        deleteEmptyDirs(root)
    }

    private fun cleanDotContents(root: File) {
        if (root.isDirectory) {
            aem.project.fileTree(root, filesDotContent).forEach { cleanDotContentFile(it) }
        }
    }

    private fun flattenFiles(root: File) {
        if (root.isDirectory) {
            aem.project.fileTree(root, filesFlattened).forEach { flattenFile(it) }
        }
    }

    private fun flattenFile(file: File) {
        if (!file.exists()) {
            return
        }

        val dest = File(file.parentFile.path + ".xml")
        if (dest.exists()) {
            aem.logger.info("Overriding file by flattening $file")
            FileUtils.deleteQuietly(dest)
        } else {
            aem.logger.info("Flattening file $file")
        }

        file.renameTo(dest)
    }

    private fun deleteFiles(root: File) {
        if (root.isDirectory) {
            aem.project.fileTree(root, filesDeleted).forEach { deleteFile(it) }
        }
    }

    private fun deleteFile(file: File) {
        if (!file.exists()) {
            return
        }

        aem.logger.info("Deleting file {}", file.path)
        FileUtils.deleteQuietly(file)
    }

    private fun deleteEmptyDirs(root: File) {
        val siblingDirs = root.listFiles { file -> file.isDirectory } ?: arrayOf()
        siblingDirs.forEach { deleteEmptyDirs(it) }
        if (EmptyFileFilter.EMPTY.accept(root)) {
            aem.logger.info("Deleting empty directory {}", root.path)
            FileUtils.deleteQuietly(root)
        }
    }

    private fun cleanDotContentFile(file: File) {
        if (!file.exists() || file.name != JCR_CONTENT_FILE) {
            return
        }

        try {
            aem.logger.info("Cleaning file {}", file.path)

            val inputLines = FileUtils.readLines(file, CharEncoding.UTF_8)
            val filteredLines = filterLines(file, inputLines)

            FileUtils.writeLines(file, CharEncoding.UTF_8, filteredLines, aem.config.lineSeparatorString)
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

        val rules by lazy { CleanerRule.manyFrom(propertiesSkipped) }

        return eachProp(line) { propOccurrence, _ ->
            if (matchAnyRule(propOccurrence, file, rules)) {
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

        val rules by lazy { CleanerRule.manyFrom(mixinTypesSkipped) }

        return eachProp(line) { propName, propValue ->
            if (propName == JCR_MIXIN_TYPES_PROP) {
                val normalizedValue = StringUtils.substringBetween(propValue, "[", "]")
                val resultValues = normalizedValue.split(",").filter { !matchAnyRule(it, file, rules) }
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
                siblingFiles.filter { !it.name.endsWith(parentsBackupSuffix) }
                        .forEach { origin ->
                            val backup = File(parent, origin.name + parentsBackupSuffix)
                            aem.logger.info("Doing backup of parent file: $origin")
                            origin.copyTo(backup, true)
                        }
            }
        }
    }

    private fun doRootBackup(root: File) {
        if (root.isFile) {
            var parent = root.parentFile
            val backup = File(parent, root.name + parentsBackupSuffix)
            aem.logger.info("Doing backup of root file: $root")
            root.copyTo(backup, true)
        }
    }

    private fun undoParentsBackup(root: File) {
        val normalizedRoot = normalizeParentRoot(root)

        eachParentFiles(normalizedRoot) { _, siblingFiles ->
            if (siblingFiles.any { it.name == parentsBackupDirIndicator }) {
                siblingFiles.filter { !it.name.endsWith(parentsBackupSuffix) }.forEach { FileUtils.deleteQuietly(it) }
                siblingFiles.filter { it.name.endsWith(parentsBackupSuffix) }.forEach { backup ->
                    val origin = File(backup.path.removeSuffix(parentsBackupSuffix))
                    aem.logger.info("Undoing backup of parent file: $backup")
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
            siblingFiles.forEach { deleteFile(it) }
            siblingFiles.forEach { cleanDotContentFile(it) }
        }
    }

    private fun eachParentFiles(root: File, processFiles: (File, Array<File>) -> Unit) {
        var parent = root.parentFile
        while (parent != null) {
            val siblingFiles = parent.listFiles { file -> file.isFile } ?: arrayOf()
            processFiles(parent, siblingFiles)

            if (parent.name == Package.JCR_ROOT) {
                break
            }

            parent = parent.parentFile
        }
    }

    private fun matchAnyRule(value: String, file: File, rules: List<CleanerRule>): Boolean {
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
