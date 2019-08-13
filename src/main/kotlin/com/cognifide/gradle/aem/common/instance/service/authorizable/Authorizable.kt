package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.instance.service.repository.Node

abstract class Authorizable(val id: String, val manager: AuthManager) {

    protected abstract val type: String

    protected abstract val rootPath: String

    protected val node: Node? = find(id)

    fun allow(path: String, permissions: List<Permission>) = addPermissions(path, permissions, "granted")

    fun deny(path: String, permissions: List<Permission>) = addPermissions(path, permissions, "denied")

    private fun addPermissions(path: String, permissions: List<Permission>, level: String) {
        val privileges = permissions.map { "privilege@${it.property}" to level }
                .toMap()

        manager.http.postUrlencoded("$path.modifyAce.json", privileges + mapOf(
                "principalId" to id
        )) { response ->
            manager.aem.logger.info("Permissions $permissions $level for $id to $path on ${manager.instance}.")
        }
    }

    protected fun find(id: String): Node? = manager.repository.node(rootPath)
            .traverse()
            .find { it.type == type && it.properties["rep:authorizableId"] == id }
}