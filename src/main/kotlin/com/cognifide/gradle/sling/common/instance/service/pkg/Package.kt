package com.cognifide.gradle.sling.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.text.SimpleDateFormat

@JsonIgnoreProperties(ignoreUnknown = true)
class Package private constructor() {

    lateinit var definition: Definition

    lateinit var path: String

    val installed: Boolean get() = definition.lastUnpacked != null

    @JsonIgnoreProperties(ignoreUnknown = true)
    class Definition {

        lateinit var group: String

        lateinit var name: String

        lateinit var version: String

        @JsonProperty("jcr:description")
        var description: String? = null

        @JsonProperty("jcr:lastModified")
        var lastModified: String? = null

        var lastUnpacked: String? = null

        @get:JsonIgnore
        val coordinates: String get() = coordinates(group, name, version)

        @get:JsonIgnore
        val dependencyNotation: String get() = "$group:$name:$version"

        val installed: Boolean get() = !lastUnpacked.isNullOrBlank()

        @get:JsonIgnore
        val installedTimestamp get() = timestamp(lastUnpacked)
    }

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

        fun timestamp(date: String?) = date?.let { SimpleDateFormat("yyyy-MM-dd hh:mm:ss").parse(it)?.time } ?: 0L
    }

    fun compare(group: String, name: String) = definition.group == group && definition.name == name

    fun compare(group: String, name: String, version: String) = compare(group, name) && definition.version == version

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Package

        return EqualsBuilder()
                .append(definition.group, other.definition.group)
                .append(definition.name, other.definition.name)
                .append(definition.version, other.definition.version)
                .isEquals
    }

    override fun hashCode() = HashCodeBuilder()
            .append(definition.group)
            .append(definition.name)
            .append(definition.version)
            .toHashCode()

    override fun toString(): String = "Package(path='$path', coordinates=${definition.coordinates})"
}
