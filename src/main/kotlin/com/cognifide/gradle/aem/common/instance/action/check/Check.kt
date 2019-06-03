package com.cognifide.gradle.aem.common.instance.action.check

interface Check {

    fun check()

    val status: String

    val success: Boolean

    val failure: Boolean
        get() = !success
}