package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.instance.service.repository.Node

abstract class Authorizable(val id: String, val manager: AuthManager) {

    protected abstract val type: String

    protected val node: Node? = find(id)

    abstract fun allow(path: String, permissions: List<Permission>)

    abstract fun deny(path: String, permissions: List<Permission>)

    protected fun find(id: String): Node? = manager.repository.node(GROUPS_ROOT)
            .traverse()
            .find { it.type == type && it.properties["rep:authorizableId"] == id }

    companion object {
        private const val GROUPS_ROOT = "/home/groups"

        private const val USERS_ROOT = "/home/users"
    }
}