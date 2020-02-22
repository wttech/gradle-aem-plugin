package com.cognifide.gradle.aem.common.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UtilsTest {

    @ParameterizedTest
    @CsvSource(value = [
        "SomeClass,SomeClass",
        "com.company.SomeClass,com.company.SomeClass",
        "org.osgi.service.component.runtime.ServiceComponentRuntime,org.o.s.c.r.ServiceComponentRuntime"
    ])
    fun `should shorten class properly`(input: String, expected: String) {
        assertEquals(expected, input.shortenClass())
    }

}