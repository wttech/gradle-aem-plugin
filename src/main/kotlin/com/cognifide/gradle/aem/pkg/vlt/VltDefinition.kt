package com.cognifide.gradle.aem.pkg.vlt

import com.cognifide.gradle.aem.common.AemExtension
import org.apache.commons.lang3.StringUtils
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.jsoup.nodes.Element

// TODO handle skipping properties like createdBy (nulls allowed?)
class VltDefinition(val aem: AemExtension) {

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
    var artifactId: String = ""

    @Input
    var groupId: String = ""

    @Input
    var description: String = ""

    @Input
    var createdBy: String = ""

    @Internal
    var filterElements: MutableList<Element> = mutableListOf()

    @get:Internal
    val filterRoots: List<String>
        get() = filterElements.map { it.attr("root") }

    @get:Input
    val filters: String
        get() = filterElements.joinToString(aem.config.lineSeparatorString)

    @Internal
    var nodeTypeLibs: MutableList<String> = mutableListOf()

    @Internal
    var nodeTypeLines: MutableList<String> = mutableListOf()

    @get:Input
    val nodeTypes: String
        get() = StringUtils.join(
                nodeTypeLibs.joinToString(aem.config.lineSeparatorString),
                aem.config.lineSeparatorString,
                nodeTypeLines.joinToString(aem.config.lineSeparatorString)
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
}