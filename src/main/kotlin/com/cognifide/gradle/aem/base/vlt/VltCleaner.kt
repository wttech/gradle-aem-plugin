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
import org.gradle.util.ConfigureUtil
import java.io.File
import java.io.IOException
import java.util.regex.Pattern

class VltCleaner(val project: Project, val root: File) {

    private val logger = project.logger

    private val config = AemConfig.of(project)

    private val dotContentProperties by lazy {
        VltContentProperty.manyFrom(skipProperties)
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
            // VLT tool internal files
            "**/.vlt",
            "**/.vlt*.tmp",

            // Top level nodes should remain untouched
            "**/jcr_root/.content.xml",
            "**/jcr_root/apps/.content.xml",
            "**/jcr_root/conf/.content.xml",
            "**/jcr_root/content/.content.xml",
            "**/jcr_root/content/dam/.content.xml",
            "**/jcr_root/etc/.content.xml",
            "**/jcr_root/etc/designs/.content.xml",
            "**/jcr_root/home/.content.xml",
            "**/jcr_root/home/groups/.content.xml",
            "**/jcr_root/home/users/.content.xml",
            "**/jcr_root/libs/.content.xml",
            "**/jcr_root/system/.content.xml",
            "**/jcr_root/tmp/.content.xml",
            "**/jcr_root/var/.content.xml"
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
        if (!Patterns.wildcard(file, filesDeleted) || !file.exists()) {
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
            val filterLine = lineProcess(file, line)
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
        if (!skipNamespaces) {
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
        if (skipProperties.isEmpty()) {
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
        if (skipMixinTypes.isEmpty()) {
            return line
        }

        return eachProp(line) { propName, propValue ->
            var result = line
            if (propName == JCR_MIXIN_TYPES_PROP) {
                val normalizedValue = propValue.removePrefix("[").removeSuffix("]")
                val resultValues = normalizedValue.split(",") - skipMixinTypes
                val resultValue = resultValues.joinToString(",")
                result = if (resultValues.isEmpty() || normalizedValue == "") {
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
