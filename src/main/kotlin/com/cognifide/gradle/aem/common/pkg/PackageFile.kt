package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.cognifide.gradle.common.zip.ZipFile
import com.fasterxml.jackson.annotation.JsonIgnore
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.io.File
import java.io.Serializable

class PackageFile(val file: File) : Serializable {

    @JsonIgnore
    val properties: Document

    val group: String

    val name: String

    val version: String

    init {
        if (!file.exists()) {
            throw PackageException("Package does not exist: $file!")
        }

        val zip = ZipFile(file)
        if (!zip.contains(Package.VLT_PROPERTIES)) {
            throw PackageException("Package is not a valid CRX package: $file!")
        }

        this.properties = zip.readFileAsText(Package.VLT_PROPERTIES).run {
            Jsoup.parse(this, "", Parser.xmlParser())
        }
        this.group = properties.select("entry[key=group]").text()
            ?: throw PackageException("CRX package '$file' does not have property 'group' specified.")
        this.name = properties.select("entry[key=name]").text()
            ?: throw PackageException("CRX package '$file' does not have property 'name' specified.")
        this.version = properties.select("entry[key=version]").text()
            ?: throw PackageException("CRX package '$file' does not have property 'version' specified.")
    }

    fun property(name: String): String? = properties.select("entry[key=$name]").text()

    override fun toString(): String {
        return "PackageFile(group='$group' name='$name', version='$version', path='$file')"
    }
}
