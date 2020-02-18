package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.AemExtension
import org.gradle.api.provider.Provider
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
            aem.project.group.toString().ifBlank {
                when {
                    aem.project != aem.project.rootProject -> aem.project.rootProject.name
                    else -> throw VaultException("Cannot determine package group by convention!" +
                            " Please define project group property explicitly.")
                }
            }
        })
    }

    /**
     * Version visible in CRX package manager.
     */
    @Input
    val version = aem.obj.string { convention(aem.obj.provider { aem.project.version.toString() }) }

    @Input
    @Optional
    val description = aem.obj.string()

    @Input
    @Optional
    val createdBy = aem.obj.string { convention(System.getProperty("user.name")) }

    @Input
    val filterElements = aem.obj.list<FilterElement> { convention(listOf()) }

    fun filter(root: String, definition: FilterElement.() -> Unit = {}) {
        filterElements.add(aem.obj.provider { FilterElement.of(root, definition) })
    }

    fun filters(vararg roots: String) = filters(roots.asIterable())

    fun filters(roots: Iterable<String>) = roots.forEach { filter(it) }

    fun filters(file: File) {
        filterElements.addAll(aem.obj.provider { FilterFile(file).elements })
    }

    fun filters(provider: Provider<File>, optionallyExist: Boolean = false) {
        filterElements.addAll(provider.map { file ->
            when {
                file.exists() || !optionallyExist -> FilterFile(file).elements
                else -> listOf()
            }
        })
    }

    @get:Internal
    val filters get() = filterEffectives.map { it.toString() }.toSet()

    @get:Internal
    val filterRoots get() = filterEffectives.map { it.root }.toSet()

    private val filterEffectives: Collection<FilterElement>
        get() = filterElements.get().asSequence()
                .filter { isFilterNeeded(it) }
                .sortedBy { it.type }
                .toList()

    @Input
    val nodeTypeLibs = aem.obj.strings { convention(listOf()) }

    @Input
    var nodeTypeLines = aem.obj.strings { convention(listOf()) }

    fun nodeTypes(provider: Provider<File>, optionallyExist: Boolean = false) {
        nodeTypeLibs(provider, optionallyExist)
        nodeTypeLines(provider, optionallyExist)
    }

    fun nodeTypeLibs(provider: Provider<File>, optionallyExist: Boolean = false) {
        nodeTypeLibs.addAll(nodeTypeReader(provider, optionallyExist) { isNodeTypeLib(it) })
    }

    fun nodeTypeLines(provider: Provider<File>, optionallyExist: Boolean = false) {
        nodeTypeLines.addAll(nodeTypeReader(provider, optionallyExist) { !isNodeTypeLib(it) })
    }

    private fun nodeTypeReader(provider: Provider<File>, optionallyExist: Boolean = false, lineFilter: (String) -> Boolean) = provider.map { file ->
        if (!file.exists()) {
            if (optionallyExist) {
                return@map listOf<String>()
            } else {
                throw VaultException("Node types file does not exist: $file!")
            }
        }

        file.bufferedReader().use { reader ->
            reader.lineSequence().mapNotNull { it.takeIf(lineFilter) }.toList()
        }
    }

    private fun isNodeTypeLib(line: String) = NODE_TYPES_LIB.matcher(line.trim()).matches()

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
        return filterElements.get().asSequence()
                .filter { custom != it }
                .none { general ->
                    custom != general && custom.root.startsWith("${general.root}/") &&
                            general.excludes.isEmpty() && general.includes.isEmpty()
                }
    }

    /**
     * Any properties that could be used in any text file being a part of composed package.
     */
    @get:Internal
    val fileProperties get() = mapOf(
            "definition" to Delegate(this),
            "aem" to aem
    )

    /**
     * Provide nicer syntax for accessing Gradle lazy properties in Pebble template files.
     */
    class Delegate(private val base: VaultDefinition) {

        val group get() = base.group.get()

        val name get() = base.name.get()

        val version get() = base.version.get()

        val description get() = base.description.orNull

        val createdBy get() = base.createdBy.orNull

        val properties get() = base.properties.get()

        val filters get() = base.filters

        val nodeTypeLibs get() = base.nodeTypeLibs.get()

        val nodeTypeLines get() = base.nodeTypeLines.get()
    }

    companion object {
        val NODE_TYPES_LIB: Pattern = Pattern.compile("<.+>")
    }
}
