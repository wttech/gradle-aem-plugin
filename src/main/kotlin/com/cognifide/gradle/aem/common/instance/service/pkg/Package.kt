package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.common.instance.Instance
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
class Package private constructor() {

    @JsonIgnore
    lateinit var instance: Instance

    lateinit var group: String

    lateinit var name: String

    lateinit var version: String

    lateinit var path: String

    lateinit var downloadName: String

    var created: Long? = null

    var lastModified: Long? = null

    var lastUnpacked: Long? = null

    var lastUnwrapped: Long? = null

    var lastWrapped: Long? = null

    var size: Long = 0

    lateinit var dependencies: List<PackageDependency>

    @JsonProperty("filter")
    lateinit var filters: List<PackageFilter>

    @get:JsonIgnore
    val coordinates: String get() = coordinates(group, name, version)

    @get:JsonIgnore
    val dependencyNotation: String get() = "$group:$name:$version"

    val built: Boolean get() = lastWrapped?.let { it > 0 } ?: false

    @get:JsonIgnore
    val buildDate: Date? get() = lastWrapped?.let { instance.date(it) }

    val installed: Boolean get() = lastUnpacked?.let { it > 0 } ?: false

    @get:JsonIgnore
    val installedDate: Date? get() = lastUnpacked?.let { instance.date(it) }

    @get:JsonIgnore
    val lastTouched: Long get() = listOfNotNull(created, lastModified, lastWrapped).maxOrNull() ?: 0L

    @get:JsonIgnore
    val touchDate: Date get() = instance.date(lastTouched)

    companion object {

        const val JCR_ROOT = "jcr_root"

        const val META_PATH = "META-INF"

        const val MANIFEST_FILE = "MANIFEST.MF"

        const val MANIFEST_PATH = "$META_PATH/$MANIFEST_FILE"

        const val OAKPAL_OPEAR_PATH = "OAKPAL_OPEAR"

        const val VLT_DIR = "vault"

        const val VLT_PATH = "$META_PATH/$VLT_DIR"

        const val VLT_HOOKS_PATH = "$VLT_PATH/hooks"

        const val VLT_PROPERTIES = "$VLT_PATH/properties.xml"

        const val VLT_NODETYPES_FILE = "nodetypes.cnd"

        fun conventionPath(group: String, name: String, version: String) = "/etc/packages/$group/$name-$version.zip"

        fun coordinates(group: String, name: String, version: String) = "[group=$group][name=$name][version=$version]"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Package

        return EqualsBuilder()
            .append(path, other.path)
            .append(group, other.group)
            .append(name, other.name)
            .append(version, other.version)
            .isEquals
    }

    override fun hashCode() = HashCodeBuilder()
        .append(path)
        .append(group)
        .append(name)
        .append(version)
        .toHashCode()

    override fun toString(): String = "Package(path='$path', group='$group', name='$name', version='$version', instance='${instance.name}')"
}
