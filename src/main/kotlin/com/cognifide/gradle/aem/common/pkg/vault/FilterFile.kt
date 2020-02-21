package com.cognifide.gradle.aem.common.pkg.vault

import com.cognifide.gradle.aem.AemExtension
import com.cognifide.gradle.aem.common.file.FileOperations
import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.aem.common.pkg.PackageException
import com.cognifide.gradle.aem.common.utils.JcrUtil
import com.cognifide.gradle.common.utils.Formats
import com.cognifide.gradle.aem.pkg.tasks.PackageVlt
import java.io.Closeable
import java.io.File
import org.apache.commons.io.FileUtils
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal

class FilterFile(@InputFile val file: File, private val temporary: Boolean = false) : Closeable {

    @get:Internal
    val elements: List<FilterElement>
        get() = file.run {
            if (!exists()) {
                throw PackageException("Cannot load Vault filter elements. File does not exist: '$this'!")
            }
            FilterElement.parse(readText())
        }

    @get:Internal
    val roots: Set<String> get() = elements.map { it.element.attr("root") }.toSet()

    fun rootDirs(contentDir: File): List<File> {
        return roots.asSequence()
                .map { absoluteRoot(contentDir, it) }
                .map { normalizeRoot(it) }
                .distinct()
                .toList()
    }

    private fun absoluteRoot(contentDir: File, root: String): File {
        return File(contentDir, "${Package.JCR_ROOT}/${root.removeSurrounding("/")}")
    }

    private fun normalizeRoot(root: File): File {
        return File(JcrUtil.manglePath(Formats.normalizePath(root.path).substringBefore("/jcr:content")))
    }

    override fun close() {
        if (temporary) {
            file.delete()
        }
    }

    override fun toString(): String = "FilterFile(file=$file, temporary=$temporary)"

    companion object {

        const val BUILD_NAME = "filter.xml"

        const val ORIGIN_NAME = "filter.origin.xml"

        const val SYNC_NAME = "filter.sync.xml"

        const val TEMPORARY_NAME = "filter.tmp.xml"

        fun default(aem: AemExtension): FilterFile = aem.run {
            val cmdFilterRoots = prop.list("filter.roots") ?: listOf()
            if (cmdFilterRoots.isNotEmpty()) {
                logger.debug("Using Vault filter roots specified as command line property: $cmdFilterRoots")
                return temporary(this, cmdFilterRoots)
            }

            val cmdFilterPath = prop.string("filter.path") ?: ""
            if (cmdFilterPath.isNotEmpty()) {
                val cmdFilter = FileOperations.find(project, packageOptions.vltDir.toString(), cmdFilterPath)
                        ?: throw VaultException("Vault check out filter file does not exist at path: $cmdFilterPath" +
                                " (or under directory: ${packageOptions.vltDir}).")
                logger.debug("Using Vault filter file specified as command line property: $cmdFilterPath")
                return FilterFile(cmdFilter)
            }

            val conventionFilterFiles = listOf(
                    "${packageOptions.vltDir}/$SYNC_NAME",
                    "${packageOptions.vltDir}/$BUILD_NAME"
            )
            val conventionFilterFile = FileOperations.find(project, packageOptions.vltDir.toString(), conventionFilterFiles)
            if (conventionFilterFile != null) {
                logger.debug("Using Vault filter file found by convention: $conventionFilterFile")
                return FilterFile(conventionFilterFile)
            }

            logger.debug("None of Vault filter files found by CMD properties or convention.")

            return temporary(this, listOf())
        }

        fun temporary(aem: AemExtension, paths: List<String>): FilterFile {
            val template = FileOperations.readResource("vlt/$TEMPORARY_NAME")!!
                    .bufferedReader().use { it.readText() }
            val content = aem.prop.expand(template, mapOf("paths" to paths))
            val file = aem.common.temporaryFile("${PackageVlt.NAME}/$TEMPORARY_NAME")

            FileUtils.deleteQuietly(file)
            file.apply {
                parentFile.mkdirs()
                printWriter().use { it.print(content) }
            }

            return FilterFile(file, true)
        }
    }
}
