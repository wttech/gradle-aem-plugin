package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.common.instance.InstanceException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import org.apache.http.HttpStatus.SC_CREATED

class AuthManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    val http = repository.http

    fun createUser(id: String, password: String = generateRandomPassword()): User {
        createAuthorizable("user", id, mapOf("rep:password" to password))
        return User(id, this)
    }

    fun createGroup(id: String): Group {
        createAuthorizable("group", id, mapOf())
        return Group(id, this)
    }

    fun addMember(user: User, group: Group) = group.addMember(user)

    fun allow(authorizable: Authorizable, path: String, permissions: List<Permission>) = authorizable.allow(path, permissions)

    fun deny(authorizable: Authorizable, path: String, permissions: List<Permission>) = authorizable.deny(path, permissions)

    private fun createAuthorizable(type: String, id: String, params: Map<String, Any>) {
        val requestBody = mutableMapOf<String, Any>()
        when (type) {
            "user" -> requestBody["createUser"] = ""
            "group" -> requestBody["createGroup"] = ""
            else -> throw InstanceException("Could not find authorizable for given type: $type.")
        }
        requestBody["authorizableId"] = id
        http.postUrlencoded(AUTHORIZABLES_ENDPOINT, requestBody + params) { response ->
            val status = response.statusLine.statusCode
            if (status == SC_CREATED) {
                aem.logger.info("Authorizable ($type) $id created on $instance")
            } else throw ResponseException("Authorizable ($type) $id was not created on $instance. Response status: $status")
        }
    }

    private fun generateRandomPassword(): String = "password" // TODO

    companion object {

        const val AUTHORIZABLES_ENDPOINT = "/libs/granite/security/post/authorizables"
    }
}