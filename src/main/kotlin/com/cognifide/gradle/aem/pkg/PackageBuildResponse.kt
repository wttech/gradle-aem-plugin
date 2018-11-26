package com.cognifide.gradle.aem.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class PackageBuildResponse private constructor() {

    var isSuccess: Boolean = false

    lateinit var msg: String

    lateinit var path: String
}
