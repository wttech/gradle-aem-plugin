package com.cognifide.gradle.aem.test.json

import org.apache.commons.io.FilenameUtils
import org.skyscreamer.jsonassert.ValueMatcher

class PathValueMatcher : ValueMatcher<Any> {
    override fun equal(value: Any?, wildcardMatcher: Any?): Boolean {
        if (value !is String || wildcardMatcher !is String) {
            return false
        }

        if (value == wildcardMatcher) {
            return true
        }

        return FilenameUtils.wildcardMatch(value.replace("\\", "/"), wildcardMatcher)
    }
}