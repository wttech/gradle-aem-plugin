package com.cognifide.gradle.sling.common.pkg.vault

import com.cognifide.gradle.sling.SlingExtension
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.jar.Attributes
import java.util.jar.Manifest
import java.util.regex.Pattern

/**
 * Represents collection of metadata being a part of CRX package.
 */
open class VaultDefinition(private val sling: SlingExtension) {

    @Internal
    protected val common = sling.common

    @Input
    val manifestProperties = sling.obj.map<String, String> {
        convention(sling.obj.provider {
            mapOf(
                    "Content-Package-Type" to "mixed",
                    "Content-Package-Id" to "${group.get()}:${name.get()}:${version.get()}",
                    "Content-Package-Roots" to filterRoots.joinToString(",")
            )
        })
    }

    /**
     * Name visible in CRX package manager
     */
    @Input
    val name = sling.obj.string { convention(sling.commonOptions.baseName) }

    /**
     * Group for categorizing in CRX package manager
     */
    @Input
    val group = sling.obj.string { convention(sling.obj.provider { sling.project.group.toString() }) }

    /**
     * Version visible in CRX package manager.
     */
    @Input
    val version = sling.obj.string { convention(sling.obj.provider { sling.project.version.toString() }) }

    @Input
    @Optional
    val description = sling.obj.string()

    @Input
    @Optional
    val createdBy = sling.obj.string { convention(System.getProperty("user.name")) }

    @Input
    val filterElements = sling.obj.list<FilterElement> { convention(listOf()) }

    fun filter(root: String, definition: FilterElement.() -> Unit = {}) {
        filterElements.add(sling.obj.provider { FilterElement.of(root, definition) })
    }

    fun filter(root: Provider<String>, definition: FilterElement.() -> Unit = {}) {
        filterElements.add(root.map { FilterElement.of(it, definition) })
    }

    fun filters(vararg roots: String) = filters(roots.asIterable())

    fun filters(roots: Iterable<String>) = roots.forEach { filter(it) }

    fun filters(file: File) {
        filterElements.addAll(sling.obj.provider { FilterFile(file).elements })
    }

    fun filters(file: RegularFileProperty, optionallyExist: Boolean = true) = filters(file.asFile, optionallyExist)

    fun filters(provider: Provider<File>, optionallyExist: Boolean = true) {
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
    val nodeTypeLibs = sling.obj.strings { convention(listOf()) }

    @Input
    val nodeTypeLines = sling.obj.strings { convention(listOf()) }

    fun nodeTypes(file: RegularFileProperty, optionallyExist: Boolean = true) = nodeTypes(file.asFile, optionallyExist)

    fun nodeTypes(provider: Provider<File>, optionallyExist: Boolean = true) {
        nodeTypeLibs(provider, optionallyExist)
        nodeTypeLines(provider, optionallyExist)
    }

    fun nodeTypeLibs(file: RegularFileProperty, optionallyExist: Boolean = true) = nodeTypeLibs(file.asFile, optionallyExist)

    fun nodeTypeLibs(provider: Provider<File>, optionallyExist: Boolean = true) {
        nodeTypeLibs.addAll(nodeTypeReader(provider, optionallyExist) { isNodeTypeLib(it) })
    }

    fun nodeTypeLines(file: RegularFileProperty, optionallyExist: Boolean = true) = nodeTypeLines(file.asFile, optionallyExist)

    fun nodeTypeLines(provider: Provider<File>, optionallyExist: Boolean = true) {
        nodeTypeLines.addAll(nodeTypeReader(provider, optionallyExist) { !isNodeTypeLib(it) })
    }

    private fun nodeTypeReader(provider: Provider<File>, optionallyExist: Boolean = true, lineFilter: (String) -> Boolean) = provider.map { file ->
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
    val properties = sling.obj.map<String, Any> {
        set(mapOf(
                AC_HANDLING_PROPERTY to "merge_preserve",
                REQUIRES_ROOT_PROPERTY to false
        ))
    }

    fun property(name: String, value: String) {
        properties.put(name, value)
    }

    fun acHandling(value: Boolean) {
        properties.put(AC_HANDLING_PROPERTY, value)
    }

    fun requiresRoot(value: Boolean) {
        properties.put(REQUIRES_ROOT_PROPERTY, value)
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
     *
     * Beware of reading this property earlier than in execution phase
     * as of it is lazy property to optimize performance.
     */
    @get:Internal
    val fileProperties by lazy {
        mapOf(
                "definition" to Delegate(this),
                "manifest" to Manifest().run {
                    mainAttributes.putValue(Attributes.Name.MANIFEST_VERSION.toString(), "1.0")
                    manifestProperties.get().forEach { (k, v) -> mainAttributes.putValue(k, v) }

                    val output = ByteArrayOutputStream()
                    write(output)
                    output.toString(StandardCharsets.UTF_8.displayName())
                },
                "sling" to sling
        )
    }

    /**
     * Provide nicer syntax for accessing Gradle lazy properties in Pebble template files.
     */
    class Delegate(private val base: VaultDefinition) {

        val group get() = base.group.orNull

        val name get() = base.name.orNull

        val version get() = base.version.orNull

        val description get() = base.description.orNull

        val createdBy get() = base.createdBy.orNull

        val nodeTypeLibs get() = base.nodeTypeLibs.orNull ?: listOf()

        val nodeTypeLines get() = base.nodeTypeLines.orNull ?: listOf()

        val properties get() = base.properties.orNull ?: mapOf()

        val filters get() = base.filters
    }

    override fun toString(): String = "VaultDefinition(group=$group, name=$name, version=$version, description=$description)"

    companion object {
        val NODE_TYPES_LIB: Pattern = Pattern.compile("<.+>")

        const val AC_HANDLING_PROPERTY = "acHandling"

        const val REQUIRES_ROOT_PROPERTY = "requiresRoot"
    }
}
