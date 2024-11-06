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

package org.elaastic.questions.assignment

import org.elaastic.questions.directory.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.Date
import java.util.UUID


interface AssignmentRepository : JpaRepository<Assignment, Long> {

    fun findAllByOwner(owner: User, pageable: Pageable): Page<Assignment>

    @EntityGraph(value = "Assignment.sequences", type = EntityGraph.EntityGraphType.LOAD)
    fun findOneWithSequencesById(id: Long): Assignment?

    fun findOneById(id: Long): Assignment?

    fun findByGlobalId(globalId: UUID): Assignment?

    @EntityGraph(value = "Assignment.sequences", type = EntityGraph.EntityGraphType.LOAD)
    fun findWithSequenceByGlobalId(globalId: UUID): Assignment?

    @EntityGraph(value = "Assignment.sequences", type = EntityGraph.EntityGraphType.LOAD)
    fun findAllBySubjectIsNull(): List<Assignment>


    @Query("SELECT DISTINCT a FROM Assignment a LEFT JOIN FETCH a.sequences s " +
            "WHERE a.lastUpdated > :since OR s.lastUpdated > :since")
    fun findAllAssignmentUpdatedSince(since: Date): List<Assignment>

    @Modifying
    @Query("UPDATE assignment a SET a.last_updated = :now WHERE a.id = :assignmentId", nativeQuery = true)
    fun updateLastUpdated(assignmentId: Long, now: Date)
}
