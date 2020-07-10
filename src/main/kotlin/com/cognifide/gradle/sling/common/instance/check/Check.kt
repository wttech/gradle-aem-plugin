package com.cognifide.gradle.sling.common.instance.check

interface Check {

    fun check()

    val status: String

    val success: Boolean

    val failure: Boolean
        get() = !success
}
