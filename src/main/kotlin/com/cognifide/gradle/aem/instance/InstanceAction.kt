package com.cognifide.gradle.aem.instance

import java.io.Serializable

interface InstanceAction : Serializable {

    fun perform()
}