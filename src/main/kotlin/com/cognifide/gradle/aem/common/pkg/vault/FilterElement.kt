package com.cognifide.gradle.aem.common.pkg.vault

import org.gradle.api.tasks.Internal
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.Serializable

class FilterElement(val root: String) : Serializable {

    var mode: String? = null

    var type: FilterType = FilterType.UNKNOWN

    var rules: List<FilterRule> = listOf()

    val excludes get() = rules.filter { it.type == FilterRuleType.EXCLUDE }

    val includes get() = rules.filter { it.type == FilterRuleType.INCLUDE }

    @get:Internal
    val element: Element
        get() = Element(FILTER_TAG).apply {
            attr(ROOT_ATTR, root)
            if (!mode.isNullOrBlank()) {
                attr(MODE_ATTR, mode)
            }
            rules.forEach { appendChild(it.element) }
        }

    override fun toString() = element.toString().replace("></$FILTER_TAG>", "/>")

    companion object {

        fun of(root: String, definition: FilterElement.() -> Unit = {}) = FilterElement(root).apply(definition)

        fun parse(xml: String): List<FilterElement> {
            val document = Jsoup.parse(xml, "", Parser.xmlParser())
            val elements = document.select(FILTER_TAG)

            return elements.map { element ->
                of(element.attr(ROOT_ATTR)) {
                    type = FilterType.UNKNOWN
                    rules = FilterRule.manyOf(element)
                    mode = element.attr(MODE_ATTR)
                }
            }
        }

        const val FILTER_TAG = "filter"

        const val MODE_ATTR = "mode"

        const val ROOT_ATTR = "root"
    }
}
