package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemExtension
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.Closeable
import java.io.File

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
        return rootPaths.map { File(contentDir, "${PackagePlugin.JCR_ROOT}/${it.removeSurrounding("/")}") }
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

        fun temporary(project: Project, paths: List<String>): VltFilter {
            val template = FileOperations.readResource("vlt/temporaryFilter.xml")!!
                    .bufferedReader().use { it.readText() }
            val content = AemExtension.of(project).props.expand(template, mapOf("paths" to paths))
            val file = AemTask.temporaryFile(project, VltTask.NAME, "temporaryFilter.xml")

            FileUtils.deleteQuietly(file)
            file.printWriter().use { it.print(content) }

            return VltFilter(file, true)
        }

        fun rootElement(xml: String): Element {
            return Jsoup.parse(xml, "", Parser.xmlParser()).select("filter[root]").first()
        }

        fun rootElementForPath(path: String): Element {
            return rootElement("<filter root=\"$path\"/>")
        }

        // TODO remove direct dependencies from here and move it task checkout (two actions, download and checkout)
        fun of(project: Project): VltFilter {
            val aem = AemExtension.of(project)
            val compose = aem.compose

            val cmdFilterRoots = aem.props.list("aem.filter.roots")

            return if (cmdFilterRoots.isNotEmpty()) {
                aem.logger.info("Using Vault filter roots specified as command line property: $cmdFilterRoots")
                VltFilter.temporary(project, cmdFilterRoots)
            } else {
                if (compose.checkoutFilterPath == AemConfig.AUTO_DETERMINED) {
                    val conventionFilter = FileOperations.find(project, compose.vaultPath, compose.checkoutFilterPaths)
                            ?: throw VltException("None of Vault check out filter file does not exist at one of convention paths: ${compose.checkoutFilterPaths}.")
                    VltFilter(conventionFilter)
                } else {
                    val configFilter = FileOperations.find(project, compose.vaultPath, compose.checkoutFilterPath)
                            ?: throw VltException("Vault check out filter file does not exist at path: ${compose.checkoutFilterPath} (or under directory: ${compose.vaultPath}).")
                    VltFilter(configFilter)
                }
            }
        }
    }

}