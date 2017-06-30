package com.cognifide.gradle.aem.deploy

import java.util.regex.Pattern

data class ErrorPattern(val pattern: Pattern, val printStackTrace: Boolean, val message: String = "")