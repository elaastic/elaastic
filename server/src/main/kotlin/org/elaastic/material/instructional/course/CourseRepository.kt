package org.elaastic.material.instructional.course

import org.elaastic.material.instructional.MaterialUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<Course?, Long> {

    fun findAllByOwner(owner: MaterialUser, pageable: Pageable): Page<Course>

    fun findAllByOwner(owner: MaterialUser, sort: Sort): List<Course>

    @EntityGraph(value = "Course_subjects", type = EntityGraph.EntityGraphType.LOAD)
    fun findAllWithSubjectsByOwner(owner: MaterialUser, pageable: Pageable): Page<Course>

    fun findOneById(id: Long): Course

    @EntityGraph(value = "Course_subjects", type = EntityGraph.EntityGraphType.LOAD)
    fun findOneWithSubjectsById(id: Long): Course?

    fun findFirstByOwner(owner: MaterialUser): Course?

}