package com.cognifide.gradle.sling.common.instance.service.pkg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class UninstallResponse : PackageResponse() {

    override val success: Boolean get() =  (operation == "uninstallation" && status == "done")

    @JsonProperty("package")
    lateinit var pkg: Package
}
