package com.cognifide.gradle.aem.pkg

import com.cognifide.gradle.aem.pkg.tasks.Compose
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

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

    constructor(compose: Compose) : this() {
        val project = compose.project

        this.group = compose.vaultGroup
        this.name = compose.vaultName
        this.version = compose.vaultVersion

        this.downloadName = "$name-$version.zip"
        this.conventionPaths = listOf(
                "/etc/packages/$group/${compose.archiveName}",
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
    val coordinates: String
        get() = "[group=$group][name=$name][version=$version]"

    val installed: Boolean
        get() = lastUnpacked?.let { it > 0 } ?: false
}