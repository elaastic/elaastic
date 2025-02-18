package org.elaastic.consolidation.subject

import com.toedter.spring.hateoas.jsonapi.JsonApiId
import com.toedter.spring.hateoas.jsonapi.JsonApiType
import org.elaastic.assignment.Assignment
import java.util.UUID

open class SummaryPracticeSubject(
    @JsonApiId
    val id: UUID,
    val title: String,
) {
    @JsonApiType
    val type = "practice-subject"

    constructor(assignment: Assignment) : this(
        id = assignment.globalId,
        title = assignment.title,
    )
}