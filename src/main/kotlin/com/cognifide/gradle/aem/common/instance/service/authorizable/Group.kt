package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.http.ResponseException
import org.apache.http.HttpStatus

class Group(id: String, manager: AuthManager) : Authorizable(id, manager) {

    override val type: String
        get() = "rep:Group"

    override fun allow(path: String, permissions: List<Permission>) {
        TODO("not implemented")
    }

    override fun deny(path: String, permissions: List<Permission>) {
        val changelogStr = mapOf(
                "path" to path,
                Permission.READ.property to permissions.contains(Permission.READ),
                Permission.MODIFY.property to permissions.contains(Permission.MODIFY),
                Permission.CREATE.property to permissions.contains(Permission.CREATE),
                Permission.DELETE.property to permissions.contains(Permission.DELETE),
                Permission.READ_ACL.property to permissions.contains(Permission.READ_ACL),
                Permission.EDIT_ACL.property to permissions.contains(Permission.EDIT_ACL),
                Permission.REPLICATE.property to permissions.contains(Permission.REPLICATE))
                .map { "${it.key}:${it.value}" }
                .joinToString(",")
        manager.http.postUrlencoded("/.cqactions.html", mapOf(
                "authorizableId" to id,
                "changelog" to changelogStr
        )) { response ->
            manager.aem.logger.info("Permissions $permissions denied for $id to $path on ${manager.instance}.")
        }
    }

    fun addMember(user: User) {
        node?.let {
            manager.http.postUrlencoded("${node!!.path}.rw.html", mapOf("addMembers" to user.id)) { response ->
                val status = response.statusLine.statusCode
                if (status == HttpStatus.SC_OK) {
                    manager.aem.logger.info("Group $id created on ${manager.instance}")
                } else throw ResponseException("Group was not created on ${manager.instance}. Response status: $status")
            }
        }
    }
}