package com.cognifide.gradle.aem.common.pkg.vlt

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class VltFilterElement(val element: Element, val type: VltFilterType) {

    companion object {

        fun parse(xml: String): List<VltFilterElement> {
            val doc = Jsoup.parse(xml, "", Parser.xmlParser())
            val elements = doc.select("filter[root]")

            return elements.map { VltFilterElement(it, VltFilterType.UNKNOWN) }
        }

        fun of(
                root: String,
                mode: String? = null,
                type: VltFilterType = VltFilterType.UNKNOWN,
                excludes: Iterable<String> = listOf(),
                includes: Iterable<String> = listOf()
        ): VltFilterElement {
            val element = Element("filter").apply {
                attr("root", root)

                if (!mode.isNullOrBlank()) {
                    attr("mode", mode)
                }

                excludes.map { e -> Element("exclude").apply {
                    attr("pattern", e) }
                }.forEach { appendChild(it) }

                includes.map { e -> Element("include").apply {
                    attr("pattern", e) }
                }.forEach { appendChild(it) }
            }

            return VltFilterElement(element, type)
        }

    }

}