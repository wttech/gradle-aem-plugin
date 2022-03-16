package com.cognifide.gradle.aem.common.pkg.vault

import org.gradle.api.tasks.Internal
import org.jsoup.nodes.Element
import java.io.Serializable

class FilterRule(val type: FilterRuleType, val pattern: String) : Serializable {

    private val tag get() = type.name.lowercase()

    @get:Internal
    val element get() = Element(tag).apply { attr(ATTR_PATTERN, pattern) }

    override fun toString() = element.toString().replace("></$tag>", "/>")

    companion object {
        const val ATTR_PATTERN = "pattern"

        fun manyOf(filter: Element) = filter.select(FilterRuleType.tags().joinToString(",")).map {
            FilterRule(FilterRuleType.of(it.tagName()), it.attr(ATTR_PATTERN))
        }
    }
}
