package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.internal.Patterns
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.NameFileFilter
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.lang3.CharEncoding
import org.apache.commons.lang3.StringUtils
import org.gradle.api.Project
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class VltCleaner(val project: Project, val root: File) {

    private val logger = project.logger

    private val config = AemConfig.of(project)

    private val dotContentProperties by lazy {
        VltContentProperty.manyFrom(config.cleanSkipProperties)
    }

    private val cleanSkipMixinTypes = config.cleanSkipMixinTypes + ""

    private val dotContentFiles: Collection<File>
        get() = FileUtils.listFiles(root, NameFileFilter(JCR_CONTENT_FILE), TrueFileFilter.INSTANCE)
                ?: listOf()

    private val allFiles: Collection<File>
        get() = FileUtils.listFiles(root, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
                ?: listOf()

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
        if (!Patterns.wildcard(file, config.cleanFilesDeleted) || !file.exists()) {
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
            val filterLine = config.cleanLineProcess(this, file, line)
            if (filterLine.isEmpty()) {
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
                result.add(filterLine)
            }
        }
        cleanNamespaces(result)
        return result
    }

    private fun cleanNamespaces(result: MutableList<String>) {
        if (!config.cleanNamespaces) {
            return
        }

        val namespacesLine = result[1]
        if (namespacesLine.startsWith("<jcr:root ")) {
            val namespaces = namespacesLine.trim().removePrefix("<jcr:root ").split(" ")
            val properNamespaces = namespaces.filter { namespace -> isNamespaceUsed(namespace, result) }
            val properNamespacesLine = "<jcr:root " + properNamespaces.joinToString(" ")
            result.removeAt(1)
            result.add(1, properNamespacesLine)
        }
    }

    private fun isNamespaceUsed(namespace: String, lines: List<String>): Boolean {
        val namespaceName = namespace.substringBefore("=").substringAfter(":")
        return lines.any { it.contains("$namespaceName:") }
    }

    fun normalizeLine(file: File, line: String): String {
        return normalizeMixins(skipProperties(file, line))
    }

    fun skipProperties(file: File, line: String): String {
        if (config.cleanSkipProperties.isEmpty()) {
            return line
        }

        return eachProp(line) { propOccurrence, _ ->
            var result = line
            if (dotContentProperties.any { it.match(file, propOccurrence) }) {
                result = ""
            }
            result
        }
    }

    fun normalizeMixins(line: String): String {
        if (config.cleanSkipMixinTypes.isEmpty()) {
            return line
        }

        return eachProp(line) { propName, propValue ->
            var result = line
            if (propName == JCR_MIXIN_TYPES_PROP) {
                val normalizedValue = propValue.removePrefix("[").removeSuffix("]")
                val resultValues = normalizedValue.split(",") - cleanSkipMixinTypes
                val resultValue = resultValues.joinToString(",")
                result = if (resultValues.isEmpty()) {
                    ""
                } else {
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
            siblingFiles.forEach { removeFile(it) }
            siblingFiles.forEach { cleanDotContent(it) }

            if (parent.name == PackagePlugin.JCR_ROOT) {
                break
            }

            parent = parent.parentFile
        }
    }

    companion object {
        const val JCR_CONTENT_FILE = ".content.xml"

        const val JCR_MIXIN_TYPES_PROP = "jcr:mixinTypes"

        val CONTENT_PROP_PATTERN: Pattern = Pattern.compile("([^=]+)=\"([^\"]+)\"")
    }
}
