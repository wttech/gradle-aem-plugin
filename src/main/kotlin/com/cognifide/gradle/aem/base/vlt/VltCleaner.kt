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

    private val filesDeletedRules by lazy {
        VltCleanRule.manyFrom(filesDeleted)
    }

    private val skipPropertiesRules by lazy {
        VltCleanRule.manyFrom(skipProperties)
    }

    private val skipMixinTypesRules by lazy {
        VltCleanRule.manyFrom(skipMixinTypes)
    }

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

    /**
     * Define here properties that will be skipped when pulling JCR content from AEM instance.
     *
     * After special delimiter '!' there could be specified one or many path patterns
     * (ANT style, delimited with ',') in which property shouldn't be removed.
     */
    var skipProperties: MutableList<String> = mutableListOf(
            "jcr:uuid!**/home/users/*,**/home/groups/*",
            "jcr:lastModified",
            "jcr:created",
            "cq:lastModified*",
            "cq:lastReplicat*",
            "*_x0040_Delete",
            "*_x0040_TypeHint"
    )

    /**
     * Define here mixin types that will be skipped when pulling JCR content from AEM instance.
     */
    var skipMixinTypes: MutableList<String> = mutableListOf(
            "cq:ReplicationStatus",
            "mix:versionable"
    )

    /**
     * Turn on/off namespace normalization after properties clean up.
     */
    var skipNamespaces: Boolean = true

    /**
     * Define hook method for customizing properties clean up.
     */
    var lineProcess: (File, String) -> String = { file, line -> normalizeLine(file, line) }

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
            val cleanLine = StringUtils.trimToEmpty(line)
            val processedLine = lineProcess(file, line)
            if (processedLine.isEmpty()) {
                when {
                    result.last().endsWith(">") -> {
                        // skip line
                    }
                    cleanLine.endsWith("/>") -> {
                        result.add(result.removeAt(result.size - 1) + "/>")
                    }
                    cleanLine.endsWith(">") -> {
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
        cleanNamespaces(result)
        return result
    }

    private fun cleanNamespaces(result: MutableList<String>) {
        if (!skipNamespaces) {
            return
        }

        val index = result.indexOfFirst { it.startsWith(JCR_ROOT_PREFIX) }
        if (index != -1) {
            val namespacesLine = result[1]
            val properNamespaces = namespacesLine.removePrefix(JCR_ROOT_PREFIX).split(" ").filter { isNamespaceUsed(it, result) }
            result.removeAt(index)
            result.add(index, JCR_ROOT_PREFIX + " " + properNamespaces.joinToString(" "))
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
        if (skipProperties.isEmpty()) {
            return line
        }

        return eachProp(line) { propOccurrence, _ ->
            var result = line
            if (matchAnyRule(propOccurrence, file, skipPropertiesRules)) {
                result = ""
            }
            result
        }
    }

    fun normalizeMixins(file: File, line: String): String {
        if (skipMixinTypes.isEmpty()) {
            return line
        }

        return eachProp(line) { propName, propValue ->
            var result = line
            if (propName == JCR_MIXIN_TYPES_PROP) {
                val normalizedValue = propValue.removePrefix("[").removeSuffix("]")
                val resultValues = normalizedValue.split(",").filter { !matchAnyRule(it, file, skipMixinTypesRules) }
                result = if (resultValues.isEmpty() || normalizedValue == "") {
                    ""
                } else {
                    val resultValue = resultValues.joinToString(",")
                    line.replace(normalizedValue, resultValue)
                }
            }
            result
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
            if (siblingFiles.any { it.name.equals(".vltcpy") }) {
                siblingFiles.filter { !it.name.equals(".cpy") }.forEach { it.delete() }
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

    companion object {
        const val JCR_CONTENT_FILE = ".content.xml"

        const val JCR_MIXIN_TYPES_PROP = "jcr:mixinTypes"

        const val JCR_ROOT_PREFIX = "<jcr:root"

        val CONTENT_PROP_PATTERN: Pattern = Pattern.compile("([^=]+)=\"([^\"]+)\"")

        val NAMESPACE_PATTERN: Pattern = Pattern.compile(".*:(.+)=.*")
    }
}
