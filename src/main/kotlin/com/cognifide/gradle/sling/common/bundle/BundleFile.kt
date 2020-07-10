package com.cognifide.gradle.sling.common.bundle

import aQute.bnd.osgi.Jar
import com.cognifide.gradle.sling.common.instance.service.osgi.Bundle
import java.io.File
import java.io.Serializable

class BundleFile(val file: File) : Serializable {

    val jar = Jar(file)

    val symbolicName: String = jar.manifest.mainAttributes.getValue(Bundle.ATTRIBUTE_SYMBOLIC_NAME)
            ?: throw BundleException("File is not a valid OSGi bundle: $file")

    val version: String = jar.manifest.mainAttributes.getValue(Bundle.ATTRIBUTE_VERSION)

    val description: String = jar.manifest.mainAttributes.getValue(Bundle.ATTRIBUTE_DESCRIPTION) ?: ""

    val group: String get() = symbolicName.substringBeforeLast(".")

    override fun toString(): String = "BundleFile(symbolicName='$symbolicName', version='$version', file='$file')"
}
