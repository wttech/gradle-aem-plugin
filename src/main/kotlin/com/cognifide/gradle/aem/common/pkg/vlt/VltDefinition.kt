package com.cognifide.gradle.aem.common.pkg.vlt

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.pkg.PackageException
import org.apache.commons.lang3.StringUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.io.File
import java.util.regex.Pattern

/**
 * Represents collection of metadata being a part of CRX package.
 */
open class VltDefinition(private val aem: AemExtension) {

    @Internal
    protected val common = aem.common

    /**
     * Name visible in CRX package manager
     */
    @Input
    var name: String = ""

    /**
     * Group for categorizing in CRX package manager
     */
    @Input
    var group: String = ""

    /**
     * Version visible in CRX package manager.
     */
    @Input
    var version: String = ""

    @Input
    @Optional
    var description: String? = null

    @Input
    @Optional
    var createdBy: String? = System.getProperty("user.name")

    @Internal
    var filterElements = mutableListOf<FilterElement>()

    fun filterElements(file: File) {
        if (!file.exists()) {
            throw PackageException("Cannot load Vault filter elements. File does not exist: '$file'!")
        }

        filterElements.addAll(FilterFile(file).elements)
    }

    @get:Internal
    val filterEffectives: Collection<FilterElement>
        get() = filterElements.asSequence()
                .filter { isFilterNeeded(it) }
                .sortedBy { it.type }
                .toList()

    @get:Internal
    val filterRoots: Collection<String>
        get() = filterEffectives.map { it.root }.toSet()

    @get:Input
    val filters: Collection<String>
        get() = filterEffectives.map { it.element.toString() }.toSet()

    fun filters(vararg roots: String) = filters(roots.asIterable())

    fun filters(roots: Iterable<String>) = roots.forEach { filter(it) }

    fun filter(root: String, definition: FilterElement.() -> Unit = {}) {
        filterElements.add(FilterElement.of(root, definition))
    }

    @Internal
    var nodeTypeLibs: MutableList<String> = mutableListOf()

    @Internal
    var nodeTypeLines: MutableList<String> = mutableListOf()

    @get:Input
    val nodeTypes: String
        get() = StringUtils.join(
                nodeTypeLibs.joinToString(aem.lineSeparatorString),
                aem.lineSeparatorString,
                nodeTypeLines.joinToString(aem.lineSeparatorString)
        )

    fun nodeTypes(file: File) {
        if (!file.exists()) {
            throw PackageException("Cannot load Vault node types. File does not exist: '$file'!")
        }

        nodeTypes(file.readText())
    }

    fun nodeTypes(text: String) {
        text.lineSequence().forEach { line ->
            if (NODE_TYPES_LIB.matcher(line.trim()).matches()) {
                nodeTypeLibs.add(line)
            } else {
                nodeTypeLines.add(line)
            }
        }
    }

    /**
     * Additional entries added to file 'META-INF/vault/properties.xml'.
     */
    @Input
    var properties: MutableMap<String, Any> = mutableMapOf(
            "acHandling" to "merge_preserve",
            "requiresRoot" to false
    )

    fun property(name: String, value: String) {
        properties[name] = value
    }

    fun ensureDefaults() {
        if (group.isBlank()) {
            group = if (aem.project == aem.project.rootProject) {
                aem.project.group.toString()
            } else {
                aem.project.rootProject.name
            }
        }

        if (name.isBlank()) {
            name = aem.baseName
        }

        if (version.isBlank()) {
            version = aem.project.version.toString()
        }
    }

    private fun isFilterNeeded(custom: FilterElement): Boolean {
        if (custom.type == FilterType.UNKNOWN) {
            return true
        }

        return isFilterDynamicAndNotRedundant(custom)
    }

    private fun isFilterDynamicAndNotRedundant(custom: FilterElement): Boolean {
        return filterElements.asSequence()
                .filter { custom != it }
                .none { general ->
                    custom != general && custom.root.startsWith("${general.root}/") &&
                            general.excludes.isEmpty() && general.includes.isEmpty()
                }
    }

    companion object {
        val NODE_TYPES_LIB: Pattern = Pattern.compile("<.+>")
    }
}
