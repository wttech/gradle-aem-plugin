package com.cognifide.gradle.aem.common.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class UtilsTest {

    @ParameterizedTest
    @CsvSource(value = [
        "SomeClass,32,SomeClass",
        "com.company.SomeClass,32,com.company.SomeClass",
        "org.osgi.service.component.runtime.ServiceComponentRuntime,32,org.o.s.c.r.Serv*omponentRuntime",
        "org.osgi.service.component.runtime.ServiceComponentRuntime,40,org.o.s.c.r.ServiceComponentRuntime"
    ])
    fun `should shorten class properly`(input: String, maxLength: Int, expected: String) {
        assertEquals(expected, input.shortenClass(maxLength))
    }

}