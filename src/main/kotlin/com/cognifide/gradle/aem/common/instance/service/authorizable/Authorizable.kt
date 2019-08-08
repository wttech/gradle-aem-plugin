package com.cognifide.gradle.aem.common.instance.service.authorizable

abstract class Authorizable(val manager: AuthManager) {

    abstract fun allow(path: String, permissions: List<Permission>)

    abstract fun deny(path: String, permissions: List<Permission>)
}