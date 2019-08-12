package com.cognifide.gradle.aem.common.instance.service.authorizable

import com.cognifide.gradle.aem.common.http.ResponseException
import com.cognifide.gradle.aem.common.instance.InstanceService
import com.cognifide.gradle.aem.common.instance.InstanceSync
import org.apache.http.HttpStatus.SC_CREATED

class AuthManager(sync: InstanceSync) : InstanceService(sync) {

    val repository = sync.repository

    val http = repository.http

    fun createUser(id: String, password: String = generateRandomPassword()): User {
        val user = User(id, this)
        http.postUrlencoded("/libs/granite/security/post/authorizables", mapOf(
                "createUser" to "",
                "authorizableId" to id,
                "rep:password" to password
        )) { response ->
            val status = response.statusLine.statusCode
            val reasonPhrase = response.statusLine.reasonPhrase
            if (status == SC_CREATED) {
                aem.logger.info("User $id created on $instance. Password: $password")
            } else throw ResponseException("User was not created on $instance. Response status: $status $reasonPhrase")
        }
        return user
    }

    fun createGroup(id: String): Group {
        val group = Group(id, this)
        http.postUrlencoded("/libs/granite/security/post/authorizables", mapOf(
                "createGroup" to "",
                "authorizableId" to id
        )) { response ->
            val status = response.statusLine.statusCode
            if (status == SC_CREATED) {
                aem.logger.info("Group $id created on $instance")
            } else throw ResponseException("Group was not created on $instance. Response status: $status")
        }
        return group
    }

    fun addMember(user: User, group: Group) = group.addMember(user)

    fun allow(authorizable: Authorizable, path: String, permissions: List<Permission>) = authorizable.allow(path, permissions)

    fun deny(authorizable: Authorizable, path: String, permissions: List<Permission>) = authorizable.deny(path, permissions)

    private fun generateRandomPassword(): String = "password"
}