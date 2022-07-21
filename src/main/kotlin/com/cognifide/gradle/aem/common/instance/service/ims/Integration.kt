package com.cognifide.gradle.aem.common.instance.service.ims

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class Integration(

    val privateKey: String,

    @JsonProperty("org")
    val orgId: String,

    @JsonProperty("id")
    val technicalAccountId: String,

    val technicalAccount: TechnicalAccount,

    @JsonProperty("imsEndpoint")
    val imsHost: String,

    val metascopes: String,
)
