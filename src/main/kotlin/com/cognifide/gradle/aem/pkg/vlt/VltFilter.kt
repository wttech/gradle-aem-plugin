package com.cognifide.gradle.aem.pkg.vlt

import com.cognifide.gradle.aem.common.AemExtension
import com.cognifide.gradle.aem.common.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.pkg.Package
import com.cognifide.gradle.aem.tooling.tasks.Vlt
import java.io.Closeable
import java.io.File
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser

class VltFilter(
    @InputFile
    val file: File,

    private val temporary: Boolean = false
) : Closeable {

    @get:Internal
    val rootElements: Set<Element>
        get() {
            val xml = file.bufferedReader().use { it.readText() }
            val doc = Jsoup.parse(xml, "", Parser.xmlParser())

            return doc.select("filter[root]").toSet()
        }

    @get:Internal
    val rootPaths: Set<String>
        get() {
            return rootElements.map { it.attr("root") }.toSet()
        }

    fun rootDirs(contentDir: File): List<File> {
        return rootPaths.map { File(contentDir, "${Package.JCR_ROOT}/${it.removeSurrounding("/")}") }
    }

    override fun close() {
        if (temporary) {
            file.delete()
        }
    }

    override fun toString(): String {
        return "VltFilter(file=$file, temporary=$temporary)"
    }

    companion object {

        const val BUILD_NAME = "filter.xml"

        const val CHECKOUT_NAME = "checkout.xml"

        const val TEMPORARY_NAME = "temporaryFilter.xml"

        fun temporary(project: Project, paths: List<String>): VltFilter {
            val template = FileOperations.readResource("vlt/$TEMPORARY_NAME")!!
                    .bufferedReader().use { it.readText() }
            val content = AemExtension.of(project).props.expand(template, mapOf("paths" to paths))
            val file = AemTask.temporaryFile(project, Vlt.NAME, TEMPORARY_NAME)

            FileUtils.deleteQuietly(file)
            file.printWriter().use { it.print(content) }

            return VltFilter(file, true)
        }

        fun parseElement(xml: String): Element {
            return Jsoup.parse(xml, "", Parser.xmlParser()).select("filter").first()
        }

        fun createElement(root: String, mode: String? = null): Element {
            return parseElement("<filter/>").apply {
                attr("root", root)
                if (!mode.isNullOrBlank()) {
                    attr("mode", mode)
                }
            }
        }
    }
}