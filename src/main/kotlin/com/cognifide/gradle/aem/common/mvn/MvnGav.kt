package com.cognifide.gradle.aem.common.mvn

import groovy.util.*
import groovy.xml.XmlParser
import java.io.File

data class MvnGav(
    val groupId: String?,
    val artifactId: String,
    val version: String?
) {
    companion object {
        fun readDir(moduleDir: File) = readFile(moduleDir.resolve("pom.xml"))

        fun readFile(pomFile: File) = pomFile.takeIf { it.exists() }?.let { pom ->
            val xml = XmlParser().parse(pom)
            fun xmlProp(name: String) =
                (((xml.get(name) as NodeList).getOrNull(0) as Node?)?.value() as NodeList?)?.getOrNull(0)?.toString()
            MvnGav(
                xmlProp("groupId"),
                xmlProp("artifactId") ?: error("Artifact ID not found in '$pom'!"),
                xmlProp("version")
            )
        } ?: throw MvnException("Cannot read Maven GAV file '$pomFile!")
    }
}
