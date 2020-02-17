package com.cognifide.gradle.aem.common.pkg.vault

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
open class VaultDefinition(private val aem: AemExtension) {

    @Internal
    protected val common = aem.common

    /**
     * Name visible in CRX package manager
     */
    @Input
    val name = aem.obj.string { convention(aem.commonOptions.baseName) }

    /**
     * Group for categorizing in CRX package manager
     */
    @Input
    val group = aem.obj.string {
        convention(aem.obj.provider {
            if (aem.project == aem.project.rootProject) {
                aem.project.group.toString()
            } else {
                aem.project.rootProject.name
            }
        })
    }

    /**
     * Version visible in CRX package manager.
     */
    @Input
    val version = aem.obj.string {
        convention(aem.obj.provider { aem.project.version.toString() })
    }

    @Input
    @Optional
    val description = aem.obj.string()

    @Input
    @Optional
    val createdBy = aem.obj.string { convention(System.getProperty("user.name")) }

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
    val filterRoots: Collection<String> get() = filterEffectives.map { it.root }.toSet()

    @get:Input
    val filters: Collection<String> get() = filterEffectives.map { it.element.toString() }.toSet()

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
        get() {
            val ls = aem.commonOptions.lineSeparator.get().value
            return StringUtils.join(nodeTypeLibs.joinToString(ls), ls, nodeTypeLines.joinToString(ls))
        }

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
    val properties = aem.obj.map<String, Any> {
        convention(mapOf(
                "acHandling" to "merge_preserve",
                "requiresRoot" to false
        ))
    }

    fun property(name: String, value: String) {
        properties.put(name, value)
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
