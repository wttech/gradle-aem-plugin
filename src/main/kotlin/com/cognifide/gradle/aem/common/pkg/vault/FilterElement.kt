package com.cognifide.gradle.aem.common.pkg.vault

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class FilterElement(val root: String) {

    var mode: String? = null

    var type: FilterType = FilterType.UNKNOWN

    var excludes: Collection<String> = listOf()

    var includes: Collection<String> = listOf()

    val element: Element
        get() = Element(FILTER_TAG).apply {
            attr(ROOT_ATTR, root)

            if (!mode.isNullOrBlank()) {
                attr(MODE_ATTR, mode)
            }

            excludes.map { e -> Element(EXCLUDE_TAG).apply {
                attr(PATTERN_ATTR, e) }
            }.forEach { appendChild(it) }

            includes.map { e -> Element(INCLUDE_TAG).apply {
                attr(PATTERN_ATTR, e) }
            }.forEach { appendChild(it) }
        }

    companion object {

        fun of(root: String, definition: FilterElement.() -> Unit) = FilterElement(root).apply(definition)

        fun parse(xml: String): List<FilterElement> {
            val document = Jsoup.parse(xml, "", Parser.xmlParser())
            val elements = document.select(FILTER_TAG)

            return elements.map { element ->
                of(element.attr(ROOT_ATTR)) {
                    type = FilterType.UNKNOWN
                    mode = element.attr(MODE_ATTR)
                    excludes = element.select(EXCLUDE_TAG).map { it.attr(PATTERN_ATTR) }
                    includes = element.select(INCLUDE_TAG).map { it.attr(PATTERN_ATTR) }
                }
            }
        }

        const val FILTER_TAG = "filter"

        const val MODE_ATTR = "mode"

        const val ROOT_ATTR = "root"

        const val EXCLUDE_TAG = "exclude"

        const val INCLUDE_TAG = "include"

        const val PATTERN_ATTR = "pattern"
    }
}
