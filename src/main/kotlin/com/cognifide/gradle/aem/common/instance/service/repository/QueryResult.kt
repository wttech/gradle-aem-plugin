package com.cognifide.gradle.aem.common.instance.service.repository

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
class QueryResult {

    var success: Boolean = false

    var results: Long = 0

    var total: Long = 0

    val more: Boolean = true

    var offset: Long = 0

    var hits: List<Map<String, Any>> = listOf()

    override fun toString(): String = "QueryResult(success=$success, results=$results, total=$total, more=$more, offset=$offset)"
}