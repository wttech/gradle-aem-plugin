package com.cognifide.gradle.aem.instance

import java.io.Serializable

class RemoteInstance(
        override val httpUrl: String,
        override val user: String,
        override val password: String,
        override val typeName: String,
        override val environment: String
) : Instance, Serializable {

    constructor(httpUrl: String) : this(httpUrl, LocalInstance.ENVIRONMENT)

    constructor(httpUrl: String, environment: String) : this(
            httpUrl,
            Instance.USER_DEFAULT,
            Instance.PASSWORD_DEFAULT,
            InstanceType.byUrl(httpUrl).name.toLowerCase(),
            environment
    )

    override fun toString(): String {
        return "RemoteInstance(httpUrl='$httpUrl', user='$user', password='$hiddenPassword', type='$typeName', environment='$environment')"
    }

}