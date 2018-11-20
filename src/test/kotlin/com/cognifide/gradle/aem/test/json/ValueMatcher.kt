package com.cognifide.gradle.aem.test.json

import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.ValueMatcher

object ValueMatcher {

    fun customizationsOf(customizations: Map<String, ValueMatcher<Any>>): List<Customization> {
        return customizations.map { Customization(it.key, it.value) }
    }

}