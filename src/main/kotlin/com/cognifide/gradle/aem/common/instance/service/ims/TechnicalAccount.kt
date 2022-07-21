package com.cognifide.gradle.aem.common.instance.service.ims

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TechnicalAccount(val clientId: String, val clientSecret: String)
