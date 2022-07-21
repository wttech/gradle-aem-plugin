package com.cognifide.gradle.aem.common.instance.service.ims

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Serves as a placeholder for json response from
 * Adobe Identity Management Services
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class AccessObject(

    @JsonProperty("access_token")
    val accessToken: String,

)
