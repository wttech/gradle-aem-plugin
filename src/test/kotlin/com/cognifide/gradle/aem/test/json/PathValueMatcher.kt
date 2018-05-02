package com.cognifide.gradle.aem.test.json

import org.skyscreamer.jsonassert.ValueMatcher

class PathValueMatcher : ValueMatcher<Any> {
    override fun equal(p1: Any?, p2: Any?): Boolean {
        if (p1 !is String || p2 !is String) {
            return false
        }

        if (p1 == p2) {
            return true
        }

        return p1.replace("\\", "/").endsWith(p2)
    }
}