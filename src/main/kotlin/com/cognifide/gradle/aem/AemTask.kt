package com.cognifide.gradle.aem

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil

interface AemTask {

    val config: AemConfig

    fun config(closure: Closure<*>) {
        ConfigureUtil.configure(closure, config)
    }

}