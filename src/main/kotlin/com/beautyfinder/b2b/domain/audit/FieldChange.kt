package com.beautyfinder.b2b.domain.audit

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

data class FieldChange(val field: String, val oldValue: Any?, val newValue: Any?)

data class ChangedFields(val changes: List<FieldChange>) {

    fun toJson(): String = objectMapper.writeValueAsString(
        changes.map {
            mapOf(
                "field" to it.field,
                "oldValue" to it.oldValue,
                "newValue" to it.newValue,
            )
        },
    )

    companion object {
        private val objectMapper = jacksonObjectMapper()

        fun of(vararg changes: Pair<String, Pair<Any?, Any?>>): ChangedFields =
            ChangedFields(
                changes.map { (field, values) ->
                    FieldChange(field, values.first, values.second)
                },
            )

        fun empty(): ChangedFields = ChangedFields(emptyList())
    }
}
