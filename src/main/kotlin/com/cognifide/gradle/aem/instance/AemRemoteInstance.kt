package com.cognifide.gradle.aem.instance

import java.io.Serializable

class AemRemoteInstance(
        override val httpUrl: String,
        override val user: String,
        override val password: String,
        override val typeName: String,
        override val environment: String
) : AemInstance, Serializable {

    constructor(httpUrl: String, environment: String) : this(
            httpUrl,
            AemInstance.USER_DEFAULT,
            AemInstance.PASSWORD_DEFAULT,
            AemInstanceType.byUrl(httpUrl).name,
            environment
    )

    override fun toString(): String {
        return "AemRemoteInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', typeName='$typeName', environment='$environment')"
    }

}