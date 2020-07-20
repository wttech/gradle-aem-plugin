package com.cognifide.gradle.sling.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class InstallResponse : PackageResponse() {

    override val success: Boolean get() =  (operation == "installation" && status == "done")

    @JsonProperty("package")
    lateinit var pkg: Package
}