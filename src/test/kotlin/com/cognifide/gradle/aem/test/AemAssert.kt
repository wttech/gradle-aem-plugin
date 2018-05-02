package com.cognifide.gradle.aem.test

import org.junit.Assert
import org.skyscreamer.jsonassert.Customization
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.skyscreamer.jsonassert.comparator.CustomComparator

object AemAssert {

    fun assertEqualsIgnoringLineEndings(expected: String, actual: String) {
        Assert.assertEquals(normalizeLineEndings(expected), normalizeLineEndings(actual))
    }

    private fun normalizeLineEndings(text: String): String {
        return text.splitToSequence("\n").map { it.trim() }.joinToString("\n")
    }

    fun assertJson(expected: String, actual: String) {
        assertJsonIgnored(expected, actual, listOf())
    }

    fun assertJsonIgnored(expected: String, actual: String, ignoredFields: List<String>) {
        val customizations = ignoredFields.map { Customization(it, { _, _ -> true }) }

        assertJsonCustomized(expected, actual, customizations)
    }

    fun assertJsonCustomized(expected: String, actual: String, customizations: List<Customization>) {
        val comparator = CustomComparator(JSONCompareMode.STRICT, *customizations.toTypedArray())

        JSONAssert.assertEquals(expected, actual, comparator)
    }

}