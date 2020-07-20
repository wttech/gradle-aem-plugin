package com.cognifide.gradle.sling.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class UploadResponse : PackageResponse() {

    override val success: Boolean get() =  (operation == "upload" && status == "successful")

    @JsonProperty("package")
    lateinit var pkg: Package
}