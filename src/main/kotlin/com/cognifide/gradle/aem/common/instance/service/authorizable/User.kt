package com.cognifide.gradle.aem.common.instance.service.authorizable

class User(id: String, manager: AuthManager) : Authorizable(id, manager) {
    override val type: String
        get() = "rep:User"

    override fun allow(path: String, permissions: List<Permission>) {
        TODO("not implemented")
    }

    override fun deny(path: String, permissions: List<Permission>) {
        TODO("not implemented")
    }
}