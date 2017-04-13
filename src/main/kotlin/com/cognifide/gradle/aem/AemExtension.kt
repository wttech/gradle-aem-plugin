package com.cognifide.gradle.aem

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil

open class AemExtension {

    companion object {
        val NAME = "aem"
    }

    val config = AemConfig()

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

}