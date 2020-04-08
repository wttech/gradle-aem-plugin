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

        fun default(aem: AemExtension): FilterFile = cmd(aem) ?: convention(aem)

        fun cmd(aem: AemExtension): FilterFile? = aem.run {
            val roots = prop.list("filter.roots") ?: listOf()
            if (roots.isNotEmpty()) {
                logger.debug("Using Vault filter roots specified as command line property: $roots")
                return temporary(this, roots)
            }

            val path = prop.string("filter.path") ?: ""
            if (path.isNotEmpty()) {
                val dir = packageOptions.vltDir.get().asFile
                val file = FileOperations.find(project, dir, path)
                        ?: throw VaultException("Vault check out filter file does not exist at path: $path (or under directory: $dir).")
                logger.debug("Using Vault filter file specified as command line property: $path")
                return FilterFile(file)
            }

            return null
        }

        fun convention(aem: AemExtension): FilterFile = aem.run {
            val dir = packageOptions.vltDir.get().asFile
            val files = listOf("$dir/$SYNC_NAME", "$dir/$BUILD_NAME")
            val file = FileOperations.find(project, dir, files)
            if (file != null) {
                logger.debug("Using Vault filter file found by convention: $file")
                return FilterFile(file)
            }

            logger.debug("None of Vault filter files found by CMD properties or convention.")

            return temporary(this, listOf())
        }

        fun temporary(aem: AemExtension, paths: List<String>): FilterFile {
            val template = aem.assetManager.file("vlt/$TEMPORARY_NAME").get().bufferedReader().use { it.readText() }
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
