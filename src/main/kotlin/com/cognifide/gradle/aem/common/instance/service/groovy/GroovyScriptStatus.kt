package com.cognifide.gradle.aem.common.instance.service.groovy

import com.cognifide.gradle.aem.common.instance.Instance
import java.io.File

data class GroovyScriptStatus(val script: File, val instance: Instance, val success: Boolean)