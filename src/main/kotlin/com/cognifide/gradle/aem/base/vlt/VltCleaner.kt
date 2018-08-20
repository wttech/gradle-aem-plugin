package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class VltCleaner(val project: Project, val root: File) {

    private val logger = project.logger

    private val config = AemConfig.of(project)

    private val dotContentFiles: Collection<File>
        get() = FileUtils.listFiles(root, NameFileFilter(JCR_CONTENT_FILE), TrueFileFilter.INSTANCE)
                ?: listOf()

    private val allFiles: Collection<File>
        get() = FileUtils.listFiles(root, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                ?: listOf()

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
     * Define here properties that will be skipped when pulling JCR content from AEM instance.
     *
     * After special delimiter '!' there could be specified one or many path patterns
     * (ANT style, delimited with ',') in which property shouldn't be removed.
     */
    var propertiesSkipped: MutableList<String> = mutableListOf(
            rule("jcr:uuid", listOf("**/home/users/*", "**/home/groups/*"), listOf()),
            "jcr:lastModified",
            "jcr:created",
            "jcr:isCheckedOut",
            "cq:lastModified*",
            "cq:lastReplicat*",
            "*_x0040_Delete",
            "*_x0040_TypeHint"
    )

    private val propertiesSkippedRules by lazy {
        VltCleanRule.manyFrom(propertiesSkipped)
    }

    /**
     * Define here mixin types that will be skipped when pulling JCR content from AEM instance.
     */
    var mixinTypesSkipped: MutableList<String> = mutableListOf(
            "cq:ReplicationStatus",
            "mix:versionable"
    )

    private val mixinTypesSkippedRules by lazy {
        VltCleanRule.manyFrom(mixinTypesSkipped)
    }

    /**
     * Turn on/off namespace normalization after properties clean up.
     */
    var namespacesSkipped: Boolean = true

    /**
     * Define hook method for customizing properties clean up.
     */
    var lineProcess: (File, String) -> String = { file, line -> normalizeLine(file, line) }

    /**
     * Define hook method for customizing content clean up.
     */
    var contentProcess: (List<String>) -> List<String> = { lines -> cleanNamespaces(lines) }

    init {
        ConfigureUtil.configure(config.cleanConfig, this)
    }

    fun clean() {
        removeFiles()
        cleanDotContent()
        cleanParents()
    }

    private fun cleanDotContent() {
        if (root.isDirectory) {
            dotContentFiles.forEach { cleanDotContent(it) }
        } else {
            cleanDotContent(root)
        }
    }

    private fun removeFiles() {
        if (root.isDirectory) {
            allFiles.forEach { removeFile(it) }
        } else {
            removeFile(root)
        }
    }

    private fun removeFile(file: File) {
        if (!matchAnyRule(file.path, file, filesDeletedRules) || !file.exists()) {
            return
        }

        logger.info("Deleting file {}", file.path)
        FileUtils.deleteQuietly(file)
    }

    private fun cleanDotContent(file: File) {
        if (file.name != JCR_CONTENT_FILE || !file.exists()) {
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
        return contentProcess(result)
    }

    fun cleanNamespaces(lines: List<String>): List<String> {
        if (!namespacesSkipped) {
            return lines
        }

        return lines.map { line ->
            if (line.trim().startsWith(JCR_ROOT_PREFIX)) {
                line.split(" ")
                        .filter { it == JCR_ROOT_PREFIX || isNamespaceUsed(it, lines) }
                        .joinToString(" ")
            } else {
                line
            }
        }
    }

    private fun isNamespaceUsed(namespace: String, lines: List<String>): Boolean {
        val matcher = NAMESPACE_PATTERN.matcher(namespace)
        return if (matcher.matches()) {
            lines.any { it.contains(matcher.group(1) + ":") }
        } else {
            false
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

    private fun cleanParents() {
        var parent = root.parentFile
        while (parent != null) {
            val siblingFiles = parent.listFiles { file: File -> file.isFile } ?: arrayOf<File>()
            if (siblingFiles.any { it.name == ".vltcpy" }) {
                siblingFiles.filter { !it.name.endsWith(".cpy") }.forEach { it.delete() }
                siblingFiles.filter { it.name.endsWith(".cpy") }.forEach { it.renameTo(File(it.path.removeSuffix(".cpy"))) }
            }

            if (parent.name == PackagePlugin.JCR_ROOT) {
                break
            }

            parent = parent.parentFile
        }
    }

    private fun matchAnyRule(value: String, file: File, rules: List<VltCleanRule>): Boolean {
        return rules.any { it.match(file, value) }
    }

    fun rule(pattern: String, excludedPaths: List<String>): String {
        return rule(pattern, excludedPaths, listOf())
    }

    fun rule(pattern: String, excludedPaths: List<String>, includedPaths: List<String>): String {
        val paths = excludedPaths.map { "!$it" } + includedPaths
        return if (paths.isEmpty()) {
            pattern
        } else {
            pattern + "|" + paths.joinToString(",")
        }
    }

    companion object {
        const val JCR_CONTENT_FILE = ".content.xml"

        const val JCR_MIXIN_TYPES_PROP = "jcr:mixinTypes"

        const val JCR_ROOT_PREFIX = "<jcr:root"

        val CONTENT_PROP_PATTERN: Pattern = Pattern.compile("([^=]+)=\"([^\"]+)\"")

        val NAMESPACE_PATTERN: Pattern = Pattern.compile(".*:(.+)=.*")
    }
}
