package com.cognifide.gradle.aem.test.json

import org.apache.jackrabbit.util.ISO8601
import org.skyscreamer.jsonassert.ValueMatcher

class DateValueMatcher : ValueMatcher<Any> {

    override fun equal(d1: Any?, d2: Any?): Boolean {
        if (d1 !is String) {
            return false
        }

        return try {
            ISO8601.parse(d1) != null
        } catch (e: Exception) {
            false
        }
    }

}