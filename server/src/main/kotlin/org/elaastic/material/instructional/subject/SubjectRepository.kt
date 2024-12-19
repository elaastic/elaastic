package org.elaastic.material.instructional.subject

import org.elaastic.material.instructional.MaterialUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.*

interface SubjectRepository : JpaRepository<Subject?, Long> {

    fun findAllByOwner(owner: MaterialUser, pageable: Pageable): Page<Subject>

    fun findAllByOwnerAndCourseIsNull(owner: MaterialUser, pageable: Pageable): Page<Subject>

    @EntityGraph(value = "Subject.statements_assignments", type = EntityGraph.EntityGraphType.LOAD)
    fun findOneWithStatementsAndAssignmentsById(id: Long): Subject?

    fun findOneById(id: Long): Subject?

    fun findByGlobalId(globalId: UUID): Subject?

    @Query("select count(s.id) from Subject as s where s.owner=?1 AND s.parentSubject = ?2")
    fun countAllByParentSubject(owner: MaterialUser, parentSubject: Subject): Int

    @Query("select count(s.id) from Subject as s where s.owner=?1 AND s.title like ?2%")
    fun countAllStartingWithTitle(owner: MaterialUser, title: String): Int

    fun countByCourseIsNullAndOwner(owner: MaterialUser): Long

    fun findFirstByOwner(owner: MaterialUser): Subject?

}