package com.cognifide.gradle.aem.instance

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

import org.osgi.framework.Bundle as Base

@JsonIgnoreProperties(ignoreUnknown = true)
class Bundle {
    lateinit var id: String

    lateinit var name: String

    var stateRaw: Int = 0

    lateinit var symbolicName: String

    lateinit var version: String

    var fragment: Boolean = false

    val stable: Boolean
        get() = if (fragment) {
            stateRaw == Base.RESOLVED
        } else {
            stateRaw == Base.ACTIVE
        }

    val state: String
        get() = when (stateRaw) {
            Base.UNINSTALLED -> "uninstalled"
            Base.INSTALLED -> "installed"
            Base.RESOLVED -> "resolved"
            Base.STARTING -> "starting"
            Base.STOPPING -> "stopping"
            Base.ACTIVE -> "active"
            else -> "unknown"
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bundle

        return EqualsBuilder()
                .append(id, other.id)
                .append(name, other.name)
                .append(stateRaw, other.stateRaw)
                .append(symbolicName, other.symbolicName)
                .append(version, other.version)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder()
                .append(id)
                .append(name)
                .append(stateRaw)
                .append(symbolicName)
                .append(version)
                .toHashCode()
    }

    override fun toString(): String {
        return "Bundle(symbolicName='$symbolicName', state='$state', id='$id')"
    }

    companion object {

        const val ATTRIBUTE_NAME = "Bundle-Name"

        const val ATTRIBUTE_DESCRIPTION = "Bundle-Description"

        const val ATTRIBUTE_SYMBOLIC_NAME = "Bundle-SymbolicName"

        const val ATTRIBUTE_VERSION = "Bundle-Version"

        const val ATTRIBUTE_MANIFEST_VERSION = "Bundle-ManifestVersion"

        const val ATTRIBUTE_ACTIVATOR = "Bundle-Activator"

        const val ATTRIBUTE_CATEGORY = "Bundle-Category"

        const val ATTRIBUTE_VENDOR = "Bundle-Vendor"

        const val ATTRIBUTE_EXPORT_PACKAGE = "Export-Package"

        const val ATTRIBUTE_PRIVATE_PACKAGE = "Private-Package"

        const val ATTRIBUTE_IMPORT_PACKAGE = "Import-Package"

        const val ATTRIBUTE_SLING_MODEL_PACKAGES = "Sling-Model-Packages"

    }
}