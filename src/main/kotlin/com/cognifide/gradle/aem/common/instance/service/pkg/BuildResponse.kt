package com.cognifide.gradle.aem.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class BuildResponse private constructor() {

    var success: Boolean = false

    lateinit var msg: String

    lateinit var path: String
}
