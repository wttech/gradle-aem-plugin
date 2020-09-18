package com.cognifide.gradle.aem.common.instance.service.pkg

import com.cognifide.gradle.aem.pkg.tasks.PackageCompose
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder

@JsonIgnoreProperties(ignoreUnknown = true)
class Package private constructor() {

    lateinit var group: String

    lateinit var name: String

    lateinit var version: String

    lateinit var path: String

    lateinit var downloadName: String

    @get:JsonIgnore
    var conventionPaths = listOf<String>()

    var lastUnpacked: Long? = null

    var lastUnwrapped: Long? = null

    var lastWrapped: Long? = null

    var size: Long = 0

    lateinit var dependencies: List<PackageDependency>

    @JsonProperty("filter")
    lateinit var filters: List<PackageFilter>

    constructor(compose: PackageCompose) : this() {
        this.group = compose.vaultDefinition.group.get()
        this.name = compose.vaultDefinition.name.get()
        this.version = compose.vaultDefinition.version.get()

        this.downloadName = "$name-$version.zip"
        this.conventionPaths = listOf(
                "/etc/packages/$group/${compose.archiveFileName.get()}",
                "/etc/packages/$group/$name-$version.zip"
        )
    }

    constructor(group: String, name: String, version: String) : this() {
        this.group = group
        this.name = name
        this.version = version

        this.path = ""
        this.downloadName = ""
        this.conventionPaths = listOf("/etc/packages/$group/$name-$version.zip")
    }

    @get:JsonIgnore
    val coordinates: String get() = coordinates(group, name, version)

    @get:JsonIgnore
    val dependencyNotation: String get() = "$group:$name:$version"

    val installed: Boolean get() = lastUnpacked?.let { it > 0 } ?: false

    @get:JsonIgnore
    val installedTimestamp get() = lastUnpacked ?: 0L

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

    override fun toString(): String = "Package(path='$path', group='$group', name='$name', version='$version')"
}
