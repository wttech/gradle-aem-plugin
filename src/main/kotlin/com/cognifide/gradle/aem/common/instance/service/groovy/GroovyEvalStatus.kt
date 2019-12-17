package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.common.instance.Instance
import java.io.File

data class GroovyEvalStatus(val script: File, val instance: Instance, val success: Boolean) {
    val fail: Boolean
        get() = !success
}
