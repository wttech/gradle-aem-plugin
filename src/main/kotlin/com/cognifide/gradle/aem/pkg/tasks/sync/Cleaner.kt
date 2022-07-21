package com.cognifide.gradle.aem.pkg.tasks.sync

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.vault.VaultException
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.EmptyFileFilter
import org.apache.commons.lang3.StringUtils
import org.gradle.api.tasks.util.PatternFilterable
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.regex.Pattern

@Suppress("TooManyFunctions")
class Cleaner(private val aem: AemExtension) {

    /**
     * Allows to control which files under each root root should be cleaned.
     */
    fun filesDotContent(filter: PatternFilterable.() -> Unit) {
        this.filesDotContent = filter
    }

    private var filesDotContent: PatternFilterable.() -> Unit = {
        include("**/$JCR_CONTENT_FILE")
    }

    /**
     * Determines which files will be deleted within running cleaning
     * (e.g after checking out JCR content).
     */
    fun filesDeleted(filter: PatternFilterable.() -> Unit) {
        this.filesDeleted = filter
    }

    private var filesDeleted: PatternFilterable.() -> Unit = {
        include(
            listOf(
                "**/.vlt",
                "**/.vlt*.tmp",
                "**/install/*.jar"
            )
        )
    }

    /**
     * Determines which files will be flattened
     * (e.g /_cq_dialog/.content.xml will be replaced by _cq_dialog.xml).
     */
    fun filesFlattened(filter: PatternFilterable.() -> Unit) {
        this.filesFlattened = filter
    }

    private var filesFlattened: PatternFilterable.() -> Unit = {
        include(
            listOf(
                "**/_cq_design_dialog/.content.xml",
                "**/_cq_dialog/.content.xml",
                "**/_cq_htmlTag/.content.xml",
                "**/_cq_template/.content.xml"
            )
        )
    }

    /**
     * Properties that will be skipped when pulling JCR content from AEM instance.
     *
     * After special delimiter '!' there could be specified one or many path patterns
     * (ANT style, delimited with ',') in which property shouldn't be removed.
     */
    val propertiesSkipped = aem.obj.strings {
        set(
            listOf(
                pathRule("jcr:uuid", listOf("**/home/users/*", "**/home/groups/*")),
                pathRule("cq:lastModified*", listOf("**/content/experience-fragments/*")),
                "jcr:lastModified*",
                "jcr:created*",
                "jcr:isCheckedOut",
                "cq:lastReplicat*",
                "cq:lastRolledout*",
                "dam:extracted",
                "dam:assetState",
                "dc:modified",
                "*_x0040_*"
            )
        )
    }

    /**
     * Mixin types that will be skipped when pulling JCR content from AEM instance.
     */
    val mixinTypesSkipped = aem.obj.strings {
        set(
            listOf(
                "cq:ReplicationStatus",
                "mix:versionable"
            )
        )
    }

    /**
     * Controls unused namespaces skipping.
     */
    val namespacesSkipped = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.sync.cleaner.namespacesSkipped")?.let { set(it) }
    }

    /**
     * Controls backups for parent nodes of filter roots for keeping them untouched.
     */
    val parentsBackupEnabled = aem.obj.boolean {
        convention(true)
        aem.prop.boolean("package.sync.cleaner.parentsBackup")?.let { set(it) }
    }

    /**
     * File suffix being added to parent node back up files.
     * Customize it only if really needed to resolve conflict with file being checked out.
     */
    val parentsBackupSuffix = aem.obj.string { convention(".bak") }

    private val parentsBackupDirIndicator = parentsBackupSuffix.map { "${it}dir" }

    /**
     * Hook for customizing particular line processing for '.content.xml' files.
     */
    fun lineProcess(callback: (File, String) -> String) {
        this.lineProcess = callback
    }

    private var lineProcess: (File, String) -> String = { file, line -> normalizeLine(file, line) }

    /**
     * Hook for additional all lines processing for '.content.xml' files.
     */
    fun contentProcess(callback: (File, List<String>) -> List<String>) {
        this.contentProcess = callback
    }

    private var contentProcess: (File, List<String>) -> List<String> = { file, lines -> normalizeContent(file, lines) }

    // ===== [Implementation] =====

    fun prepare(root: File) {
        if (parentsBackupEnabled.get()) {
            doParentsBackup(root)
        }
    }

    fun beforeClean(root: File) {
        if (parentsBackupEnabled.get()) {
            doRootBackup(root)
        }
    }

    fun clean(root: File) {
        flattenFiles(root)

        when {
            parentsBackupEnabled.get() -> undoParentsBackup(root)
            else -> cleanParents(root)
        }

        cleanDotContents(root)

        deleteFiles(root)
        deleteBackupFiles(root)
        deleteEmptyDirs(root)
    }

    private fun eachFiles(root: File, filter: PatternFilterable.() -> Unit, action: (File) -> Unit) {
        val rootFilter: PatternFilterable.() -> Unit = if (root.isDirectory) {
            { include("${root.name}/**") }
        } else {
            { include(root.name) }
        }
        aem.project.fileTree(root.parent).matching(rootFilter).matching(filter).forEach(action)
    }

    private fun cleanDotContents(root: File) = eachFiles(root, filesDotContent) { cleanDotContentFile(it) }

    private fun cleanDotContentFile(file: File) {
        if (!file.exists()) {
            return
        }

        try {
            aem.logger.info("Cleaning file {}", file.path)

            val inputLines = FileUtils.readLines(file, StandardCharsets.UTF_8.name())
            val filteredLines = filterLines(file, inputLines)

            FileUtils.writeLines(file, StandardCharsets.UTF_8.name(), filteredLines, aem.commonOptions.lineSeparator.get().value)
        } catch (e: IOException) {
            throw VaultException(String.format(Locale.ENGLISH, "Error opening %s", file.path), e)
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

    @Suppress("UnusedPrivateMember", "unused_parameter")
    fun normalizeContent(file: File, lines: List<String>): List<String> {
        return mergeSinglePropertyLines(cleanNamespaces(lines))
    }

    @Suppress("NestedBlockDepth")
    fun mergeSinglePropertyLines(lines: List<String>) = mutableListOf<String>().apply {
        val it = lines.listIterator()
        while (it.hasNext()) {
            val line = it.next()
            val lineTrimmed = line.trim()
            if (lineTrimmed.startsWith(JCR_ROOT_PREFIX) || !it.hasNext()) {
                add(line)
            } else if (lineTrimmed.startsWith("<") && !lineTrimmed.endsWith(">")) {
                val nextLine = it.next()
                val nextLineTrimmed = nextLine.trim()
                if (!nextLineTrimmed.startsWith("<") && nextLineTrimmed.endsWith(">")) {
                    add("$line $nextLineTrimmed")
                } else {
                    add(line)
                    add(nextLine)
                }
            } else {
                add(line)
            }
        }
    }

    fun cleanNamespaces(lines: List<String>): List<String> {
        if (!namespacesSkipped.get()) {
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
        if (propertiesSkipped.get().isEmpty()) {
            return line
        }

        val rules by lazy { CleanerRule.manyFrom(propertiesSkipped.get()) }

        return eachProp(line) { propOccurrence, _ ->
            when {
                matchAnyRule(propOccurrence, file, rules) -> ""
                else -> line
            }
        }
    }

    fun normalizeMixins(file: File, line: String): String {
        if (mixinTypesSkipped.get().isEmpty()) {
            return line
        }

        val rules by lazy { CleanerRule.manyFrom(mixinTypesSkipped.get()) }

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
        return when {
            matcher.matches() -> processProp(matcher.group(1), matcher.group(2))
            else -> line
        }
    }

    private fun flattenFiles(root: File) = eachFiles(root, filesFlattened) { flattenFile(it) }

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
        eachParentFiles(root) { parent ->
            aem.project.fileTree(parent)
                .matching { it.include("*") }
                .matching(filesDeleted)
                .forEach { deleteFile(it) }
        }
        eachFiles(root, filesDeleted) { deleteFile(it) }
    }

    private fun deleteBackupFiles(root: File) = eachFiles(root, {
        include(
            listOf(
                "**/${parentsBackupDirIndicator.get()}",
                "**/*${parentsBackupSuffix.get()}"
            )
        )
    }) { deleteFile(it) }

    private fun deleteFile(file: File) {
        if (!file.exists()) {
            return
        }

        aem.logger.info("Deleting file {}", file.path)
        FileUtils.deleteQuietly(file)
    }

    private fun deleteEmptyDirs(root: File) {
        if (!root.exists() || root.isFile) {
            return
        }

        val siblingDirs = root.listFiles { file -> file.isDirectory } ?: arrayOf()
        siblingDirs.forEach { deleteEmptyDirs(it) }
        if (EmptyFileFilter.EMPTY.accept(root)) {
            aem.logger.info("Deleting empty directory {}", root.path)
            FileUtils.deleteQuietly(root)
        }
    }

    private fun doParentsBackup(root: File) {
        root.parentFile.mkdirs()
        eachParentFiles(root) { parent ->
            val siblingFiles = parent.listFiles { file -> file.isFile } ?: arrayOf()
            parent.mkdirs()
            if (File(parent, parentsBackupDirIndicator.get()).createNewFile()) {
                siblingFiles.filter { !it.name.endsWith(parentsBackupSuffix.get()) }
                    .forEach { origin ->
                        val backup = File(parent, origin.name + parentsBackupSuffix.get())
                        aem.logger.info("Doing backup of parent file: $origin")
                        origin.copyTo(backup, true)
                    }
            }
        }
    }

    private fun doRootBackup(root: File) {
        if (root.isFile) {
            val backup = File(root.parentFile, root.name + parentsBackupSuffix.get())
            aem.logger.info("Doing backup of root file: $root")
            root.copyTo(backup, true)
        }

        eachFiles(root, filesFlattened) { file ->
            val backup = File(file.parentFile.path + ".xml" + parentsBackupSuffix.get())
            aem.logger.info("Doing backup of file: $file")
            file.copyTo(backup, true)
        }
    }

    private fun undoParentsBackup(root: File) = eachParentFiles(root) { parent ->
        val siblingFiles = parent.listFiles { file -> file.isFile } ?: arrayOf()
        if (siblingFiles.any { it.name == parentsBackupDirIndicator.get() }) {
            siblingFiles.filter { !it.name.endsWith(parentsBackupSuffix.get()) }.forEach { FileUtils.deleteQuietly(it) }
            siblingFiles.filter { it.name.endsWith(parentsBackupSuffix.get()) }.forEach { backup ->
                val origin = File(backup.path.removeSuffix(parentsBackupSuffix.get()))
                aem.logger.info("Undoing backup of parent file: $backup")
                backup.renameTo(origin)
            }
        }
    }

    private fun cleanParents(root: File) = eachParentFiles(root) { parent ->
        val siblingFiles = parent.listFiles { file -> file.isFile } ?: arrayOf()
        siblingFiles.forEach { deleteFile(it) }
        siblingFiles.forEach { cleanDotContentFile(it) }
    }

    private fun eachParentFiles(root: File, processFiles: (File) -> Unit) {
        var parent = root.parentFile
        while (parent != null) {
            processFiles(parent)

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
        return when {
            paths.isEmpty() -> pattern
            else -> pattern + "|" + paths.joinToString(",")
        }
    }

    companion object {
        const val JCR_CONTENT_FILE = ".content.xml"

        const val JCR_MIXIN_TYPES_PROP = "jcr:mixinTypes"

        const val JCR_ROOT_PREFIX = "<jcr:root"

        val CONTENT_PROP_PATTERN: Pattern = Pattern.compile("([^ =]+)=\"([^\"]+)\"")

        val NAMESPACE_PATTERN: Pattern = Pattern.compile("\\w+:(\\w+)=\"[^\"]+\"")
    }
}
