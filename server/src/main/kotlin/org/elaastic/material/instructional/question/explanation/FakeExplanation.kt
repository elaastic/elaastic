/*
 * Elaastic - formative assessment system
 * Copyright (C) 2019. University Toulouse 1 Capitole, University Toulouse 3 Paul Sabatier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.elaastic.material.instructional.question.explanation

import org.elaastic.material.instructional.statement.Statement
import org.elaastic.common.persistence.AbstractJpaPersistable
import org.elaastic.material.instructional.MaterialUser
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank


/**
 * Fake Explanation of a statement The teacher can create multiple fake
 * explanations for a statement The student can't make the difference
 * between a fake explanation and a real explanation
 *
 * @see Statement
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
class FakeExplanation(

    /**
     * If the statement is a multiple choice question (or exclusive choice question),
     * the corresponding item is the index of an answer (correct or not) in the list of choices
     */
    var correspondingItem: Int? = null,

    /**
     * The fake explanation given by the teacher
     */
    @field:NotBlank
    var content: String,

    @field:ManyToOne
    var author: MaterialUser,

    @field:ManyToOne
    var statement: Statement
) : AbstractJpaPersistable<Long>() {

    @Version
    var version: Long? = null

    @Column(name = "date_created")
    @CreatedDate
    lateinit var dateCreated: Date

    @LastModifiedDate
    @Column(name = "last_updated")
    var lastUpdated: Date? = null
}
