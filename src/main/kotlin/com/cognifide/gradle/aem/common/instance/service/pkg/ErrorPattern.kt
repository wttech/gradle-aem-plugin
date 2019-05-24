package com.cognifide.gradle.aem.common.instance.service.pkg

import java.util.regex.Pattern

data class ErrorPattern(val pattern: Pattern, val printStackTrace: Boolean, val message: String = "")