package com.cognifide.gradle.aem.common.instance.service.authorizable

class Group(manager: AuthManager) : Authorizable(manager) {

    override fun allow(path: String, permissions: List<Permission>) {
        TODO("not implemented")
    }

    override fun deny(path: String, permissions: List<Permission>) {
        TODO("not implemented")
    }

    fun addMember(user: User) {
        TODO("not implemented")
    }
}