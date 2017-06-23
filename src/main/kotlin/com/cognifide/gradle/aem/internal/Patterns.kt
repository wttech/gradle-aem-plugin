package com.cognifide.gradle.aem.internal

import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOCase

object Patterns {

    fun wildcard(value: String, matcher: String): Boolean {
        return FilenameUtils.wildcardMatch(value, matcher, IOCase.INSENSITIVE)
    }

    fun wildcardSensitive(value: String, matcher: String): Boolean {
        return FilenameUtils.wildcardMatch(value, matcher, IOCase.SENSITIVE)
    }

}