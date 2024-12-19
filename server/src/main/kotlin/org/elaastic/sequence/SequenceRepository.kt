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

package org.elaastic.sequence

import org.elaastic.assignment.Assignment
import org.elaastic.material.instructional.statement.Statement
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID


interface SequenceRepository : JpaRepository<Sequence, Long> {

    fun findAllByResultsArePublishedIsTrue() : List<Sequence>

    @EntityGraph(value = "Sequence.statement", type = EntityGraph.EntityGraphType.LOAD)
    fun findOneById(id: Long) : Sequence?

    @EntityGraph(value = "Sequence.statement", type = EntityGraph.EntityGraphType.LOAD)
    fun findByUuid(uuid: UUID) : Sequence?

    fun countAllByAssignment(assignment: Assignment) : Int

    // TODO: Replace by statement UUID
    fun findAllByStatementAndStateNot(statement: Statement, state: State) : List<Sequence>

    // TODO: Replace by statement UUID
    fun findAllByStatement(statement: Statement) : List<Sequence>

    fun findSequenceByRankAndAssignment(rank: Int, assignment: Assignment): Sequence?

    @Query("SELECT s FROM Sequence s " +
           "WHERE s.assignment = :assignment " +
           "AND s.rank < :rank " +
           "ORDER BY s.rank DESC")
    fun findPreviousSequence(@Param("rank") rank: Int,
                             @Param("assignment") assignment: Assignment,
                             pageable: Pageable): List<Sequence>

    @Query("SELECT s FROM Sequence s " +
           "WHERE s.assignment = :assignment " +
           "AND s.rank > :rank " +
           "ORDER BY s.rank")
    fun findNextSequence(@Param("rank") rank: Int,
                         @Param("assignment") assignment: Assignment,
                         pageable: Pageable): List<Sequence>
}