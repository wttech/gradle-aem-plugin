package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.common.instance.InstanceException
import net.minidev.json.JSONArray
import org.apache.http.HttpStatus

class Group(id: String, manager: AuthManager) : Authorizable(id, manager) {

    val members: List<User>?
        get() = determineMembers()

    override val type: String
        get() = "rep:Group"

    override val rootPath: String
        get() = "/home/groups"

    fun addMember(user: User) {
        node?.let {
            manager.http.postUrlencoded("${it.path}.rw.html", mapOf("addMembers" to user.id)) { response ->
                val status = response.statusLine.statusCode
                if (status == HttpStatus.SC_OK) {
                    manager.aem.logger.info("User ${user.id} added to $id on ${manager.instance}")
                } else throw ResponseException("Could not add ${user.id} to $id on ${manager.instance}. Response status: $status")
            }
        } ?: throw InstanceException("Could not add a member to non-existing group")
    }

    private fun determineMembers(): List<User> {
        var members = listOf<User>()
        node?.properties?.get("rep:members")?.let { prop ->
            members = processProperty(prop)
        }
        return members
    }

    private fun processProperty(property: Any): List<User> {
        val members = mutableListOf<User>()
            if (property is JSONArray) {
                property.forEach { uuid ->
                    findByUuid(uuid.toString())?.let { members.add(it) }
                }
            }
        return members
    }

    private fun findByUuid(uuid: String): User? {
        val node = manager.repository.node(rootPath)
                .traverse()
                .find { it.type == type && it.properties["jcr:uuid"] == uuid }
        return node?.properties?.get("rep:authorizableId")?.let { User(it as String, manager) }
    }
}
