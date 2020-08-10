package com.cognifide.gradle.aem.common.instance.check

import org.gradle.api.provider.Property

interface Check {

    val enabled: Property<Boolean>

    fun check()

    val status: String

    val success: Boolean

    val failure: Boolean get() = !success
}
