package com.cognifide.gradle.aem.common.instance.service.ims

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Serves as a placeholder for secret json file
 * fetched from AEMaaCS instance
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class Secret(val integration: Integration)
