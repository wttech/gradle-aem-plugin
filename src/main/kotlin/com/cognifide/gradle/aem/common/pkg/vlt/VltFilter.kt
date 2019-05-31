package com.cognifide.gradle.aem.common.pkg.vlt

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.AemTask
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.tooling.vlt.Vlt
import java.io.Closeable
import java.io.File
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.jsoup.select.Elements

class VltFilter(
    @InputFile
    val file: File,

    private val temporary: Boolean = false
) : Closeable {

    @get:Internal
    val rootElements: Set<Element>
        get() = parseElements(file.bufferedReader().use { it.readText() }).toSet()

    @get:Internal
    val rootPaths: Set<String>
        get() = rootElements.map { it.attr("root") }.toSet()

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

        const val SYNC_NAME = "sync.xml"

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

        fun parseElement(xml: String) = parseElements(xml).first()

        fun parseElements(xml: String): Elements {
            val doc = Jsoup.parse(xml, "", Parser.xmlParser())

            return doc.select("filter[root]")
        }

        fun createElement(
            root: String,
            mode: String? = null,
            excludes: Iterable<String> = listOf(),
            includes: Iterable<String> = listOf()
        ): Element {
            return Element("filter").apply {
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
        }
    }
}