package com.cognifide.gradle.aem.common.pkg.vlt

import com.cognifide.gradle.aem.AemExtension
import org.apache.commons.lang3.StringUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jsoup.nodes.Element

/**
 * Represents collection of metadata being a part of CRX package.
 */
open class VltDefinition(private val aem: AemExtension) {

    /**
     * Name visible in CRX package manager
     */
    @Input
    var name: String = ""

    /**
     * Group for categorizing in qCRX package manager
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
    var filterElements: MutableList<Element> = mutableListOf()

    @get:Internal
    val filterRoots: List<String>
        get() = filterElements.map { it.attr("root") }

    @get:Input
    val filters: String
        get() = filterElements.map { it.toString() }.toSet().joinToString(aem.lineSeparatorString)

    fun filters(vararg roots: String) = filters(roots.asIterable())

    fun filters(roots: Iterable<String>) = roots.forEach { filter(it) }

    fun filter(
        root: String,
        mode: String? = null,
        excludes: Iterable<String> = listOf(),
        includes: Iterable<String> = listOf()
    ) {
        filterElements.add(VltFilter.createElement(root, mode, excludes, includes))
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
}