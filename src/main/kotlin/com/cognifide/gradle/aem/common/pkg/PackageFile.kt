package com.cognifide.gradle.aem.common.pkg

import com.cognifide.gradle.aem.common.instance.service.pkg.Package
import com.fasterxml.jackson.annotation.JsonIgnore
import java.io.File
import java.io.Serializable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.zeroturnaround.zip.ZipUtil

class PackageFile(val file: File) : Serializable {

    @JsonIgnore
    val properties: Document

    val group: String

    val name: String

    val version: String

    init {
        if (!file.exists()) {
            throw PackageException("File does not exist: $file!")
        }

        if (!ZipUtil.containsEntry(file, Package.VLT_PROPERTIES)) {
            throw PackageException("File is not a valid CRX package: $file!")
        }

        this.properties = ZipUtil.unpackEntry(file, Package.VLT_PROPERTIES).toString(Charsets.UTF_8).run {
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
