package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.service.repository.Node
import org.apache.http.HttpStatus.SC_OK

abstract class Authorizable(val id: String, val manager: AuthManager) {

    protected abstract val type: String

    protected abstract val rootPath: String

    protected val node: Node? = find(id)

    fun allow(path: String, permissions: List<Permission>) = addPermissions(path, permissions, "granted")

    fun deny(path: String, permissions: List<Permission>) = addPermissions(path, permissions, "denied")

    protected fun find(id: String): Node? = manager.repository.node(rootPath)
            .traverse()
            .find { it.type == type && it.properties["rep:authorizableId"] == id }

    private fun addPermissions(path: String, permissions: List<Permission>, level: String) {
        val privileges = permissions.map { "privilege@${it.property}" to level }
                .toMap()

        try {
            manager.http.postUrlencoded("$path.modifyAce.json", privileges + mapOf(
                    "principalId" to id
            )) { response ->
                val status = response.statusLine.statusCode
                if (status == SC_OK) {
                    manager.aem.logger.info("Permissions $permissions $level for $id to $path on ${manager.instance}.")
                } else {
                    throw ResponseException("Permissions could not be saved. Response status: $status")
                }
            }
        } catch (e: ResponseException) {
            throw InstanceException("Permissions for $id could not be saved on ${manager.instance}. Cause: ${e.message}", e)
        }
    }
}