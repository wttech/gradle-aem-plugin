package com.cognifide.gradle.aem.base.vlt

import com.cognifide.gradle.aem.api.AemConfig
import com.cognifide.gradle.aem.api.AemTask
import com.cognifide.gradle.aem.internal.PropertyParser
import com.cognifide.gradle.aem.internal.file.FileOperations
import com.cognifide.gradle.aem.pkg.PackagePlugin
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.Closeable
import java.io.File

class VltFilter(val file: File, private val temporary: Boolean = false) : Closeable {


    companion object {

        fun temporary(project: Project, paths: List<String>): VltFilter {
            val template = FileOperations.readResource("vlt/temporaryFilter.xml")!!
                    .bufferedReader().use { it.readText() }
            val content = PropertyParser(project).expand(template, mapOf("paths" to paths))
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

        fun of(project: Project): VltFilter {
            val logger = project.logger
            val props = PropertyParser(project)
            var config = AemConfig.of(project)
            // TODO: next major version -> refactor the property names to be more general (not aem.checkout)
            val cmdFilterRoots = props.list("aem.checkout.filterRoots")

            return if (cmdFilterRoots.isNotEmpty()) {
                logger.info("Using Vault filter roots specified as command line property: $cmdFilterRoots")
                VltFilter.temporary(project, cmdFilterRoots)
            } else {
                if (config.checkoutFilterPath.isNotBlank()) {
                    val configFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPath)
                            ?: throw VltException("Vault check out filter file does not exist at path: ${config.checkoutFilterPath} (or under directory: ${config.vaultPath}).")
                    VltFilter(configFilter)
                } else {
                    val conventionFilter = FileOperations.find(project, config.vaultPath, config.checkoutFilterPaths)
                            ?: throw VltException("None of Vault check out filter file does not exist at one of convention paths: ${config.checkoutFilterPaths}.")
                    VltFilter(conventionFilter)
                }
            }
        }

    }

    val rootElements: Set<Element>
        get() {
            val xml = file.bufferedReader().use { it.readText() }
            val doc = Jsoup.parse(xml, "", Parser.xmlParser())

            return doc.select("filter[root]").toSet()
        }

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

}