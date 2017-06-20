package com.cognifide.gradle.aem

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil

/**
 * Intentionally contains only data class which cannot be proxied (Kotlin limitation), but the extension does.
 * It is also project independent by default to avoid serialization issues.
 */
open class AemExtension {

    companion object {
        val NAME = "aem"
    }

    val config = AemConfig()

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

}