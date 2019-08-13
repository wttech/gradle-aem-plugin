package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.http.ResponseException
import org.apache.http.HttpStatus

class Group(id: String, manager: AuthManager) : Authorizable(id, manager) {

    override val type: String
        get() = "rep:Group"

    override val rootPath: String
        get() = "/home/groups"

    fun addMember(user: User) {
        node?.let {
            manager.http.postUrlencoded("${it.path}.rw.html", mapOf("addMembers" to user.id)) { response ->
                val status = response.statusLine.statusCode
                if (status == HttpStatus.SC_OK) {
                    manager.aem.logger.info("Group $id created on ${manager.instance}")
                } else throw ResponseException("Group was not created on ${manager.instance}. Response status: $status")
            }
        }
    }
}