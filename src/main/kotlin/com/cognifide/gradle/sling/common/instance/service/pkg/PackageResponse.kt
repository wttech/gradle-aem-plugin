package com.cognifide.gradle.sling.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
abstract class PackageResponse {

    lateinit var operation: String

    lateinit var status: String

    lateinit var path: String

    abstract val success: Boolean

}