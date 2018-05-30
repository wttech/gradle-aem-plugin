package com.cognifide.gradle.aem.test.json

import org.skyscreamer.jsonassert.ValueMatcher

class AnyValueMatcher : ValueMatcher<Any> {
    override fun equal(p1: Any?, p2: Any?): Boolean {
        return p1 != null
    }
}