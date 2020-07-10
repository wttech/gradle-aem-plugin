package com.cognifide.gradle.sling.common.instance.service.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class RepositoryChange {

    lateinit var type: String

    lateinit var argument: String

    override fun toString(): String {
        return "RepositoryChange(type='$type', argument='$argument')"
    }
}
