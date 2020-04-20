package com.cognifide.gradle.aem.common.instance.service.osgi

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
        get() = when {
            fragment -> stateRaw == Base.RESOLVED
            else -> stateRaw == Base.ACTIVE
        }

    val state: String
        get() = when (stateRaw) {
            Base.UNINSTALLED -> STATE_UNINSTALLED
            Base.INSTALLED -> STATE_INSTALLED
            Base.RESOLVED -> STATE_RESOLVED
            Base.STARTING -> STATE_STARTING
            Base.STOPPING -> STATE_STOPPING
            Base.ACTIVE -> STATE_ACTIVE
            else -> STATE_UNKNOWN
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

    override fun hashCode(): Int = HashCodeBuilder()
            .append(id)
            .append(name)
            .append(stateRaw)
            .append(symbolicName)
            .append(version)
            .toHashCode()

    override fun toString(): String = "Bundle(symbolicName='$symbolicName', state='$state', id='$id')"

    companion object {

        const val STATE_UNINSTALLED = "uninstalled"

        const val STATE_INSTALLED = "installed"

        const val STATE_RESOLVED = "resolved"

        const val STATE_STARTING = "starting"

        const val STATE_STOPPING = "stopping"

        const val STATE_ACTIVE = "active"

        const val STATE_UNKNOWN = "unknown"

        const val ATTRIBUTE_NAME = "Bundle-Name"

        const val ATTRIBUTE_DESCRIPTION = "Bundle-Description"

        const val ATTRIBUTE_SYMBOLIC_NAME = "Bundle-SymbolicName"

        const val ATTRIBUTE_VERSION = "Bundle-Version"

        const val ATTRIBUTE_ACTIVATOR = "Bundle-Activator"

        const val ATTRIBUTE_CATEGORY = "Bundle-Category"

        const val ATTRIBUTE_LICENSE = "Bundle-License"

        const val ATTRIBUTE_COPYRIGHT = "Bundle-Copyright"

        const val ATTRIBUTE_DOC_URL = "Bundle-DocURL"

        const val ATTRIBUTE_DEVELOPERS = "Bundle-Developers"

        const val ATTRIBUTE_CONTRIBUTORS = "Bundle-Contributors"

        const val ATTRIBUTE_VENDOR = "Bundle-Vendor"

        const val ATTRIBUTE_FRAGMENT_HOST = "Fragment-Host"

        const val ATTRIBUTE_EXPORT_PACKAGE = "Export-Package"

        const val ATTRIBUTE_PRIVATE_PACKAGE = "Private-Package"

        const val ATTRIBUTE_IMPORT_PACKAGE = "Import-Package"

        const val ATTRIBUTE_SLING_MODEL_PACKAGES = "Sling-Model-Packages"
    }
}
