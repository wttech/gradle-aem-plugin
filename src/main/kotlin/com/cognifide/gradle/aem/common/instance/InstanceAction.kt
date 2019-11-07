package com.cognifide.gradle.aem.common.instance

import java.io.Serializable

interface InstanceAction : Serializable {

    fun perform()
}
