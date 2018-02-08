package com.cognifide.gradle.aem.internal.jsoup

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

object JsoupUtil {

    fun selfClosingTag(xml: String, tag: String): Element {
        return Jsoup.parse(xml, "", Parser.xmlParser()).select(tag).first()
    }

}