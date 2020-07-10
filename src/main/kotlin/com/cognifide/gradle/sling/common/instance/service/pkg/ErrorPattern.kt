package com.cognifide.gradle.sling.common.instance.service.pkg

import java.util.regex.Pattern

data class ErrorPattern(val pattern: Pattern, val printStackTrace: Boolean, val message: String = "")
