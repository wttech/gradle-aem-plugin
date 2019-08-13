package com.cognifide.gradle.aem.common.instance.service.authorizable

class User(id: String, manager: AuthManager) : Authorizable(id, manager) {

    override val type: String
        get() = "rep:User"

    override val rootPath: String
        get() = "/home/users"
}