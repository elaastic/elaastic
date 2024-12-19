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

package org.elaastic.material.instructional.course

import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.subject.Subject
import org.elaastic.material.instructional.subject.SubjectService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
@Transactional
class CourseService(
    @Autowired val courseRepository: CourseRepository,
    @Autowired val subjectService: SubjectService,
    @Autowired val entityManager: EntityManager
) {

    fun get(id: Long, fetchSubjects: Boolean = false): Course {
        return when (fetchSubjects) {
            true -> courseRepository.findOneWithSubjectsById(id)
            false -> courseRepository.findOneById(id)
        } ?: throw EntityNotFoundException("There is no course for id \"$id\"")
    }

    fun get(user: MaterialUser, id: Long): Course {
        courseRepository.findOneById(id).let {
            if (!user.isTeacher()) {
                throw AccessDeniedException("You are not authorized to access to this course")
            }
            if (user != it.owner) {
                throw AccessDeniedException("This course doesn't belong to you")
            }
            return it
        }
    }

    fun delete(user: MaterialUser, course: Course) {
        require(user == course.owner) {
            "Only the owner can delete an assignment"
        }
        require(course.subjects.isEmpty()) {
            "The course must be empty to be deleted"
        }
        courseRepository.delete(course)
    }

    fun touch(course: Course) {
        course.lastUpdated = Date()
        courseRepository.save(course)
    }

    fun removeSubject(user: MaterialUser, subject: Subject) {
        require(user == subject.owner) {
            "Only the owner can delete a subject"
        }

        if (subject.course != null) {
            val course = subject.course!!
            touch(course)
            course.subjects.remove(subject)
            entityManager.flush()
        }
        subjectService.delete(user, subject)
        entityManager.flush()
        entityManager.clear()
    }

    fun addSubjectToCourse(user: MaterialUser, subject: Subject, course: Course) {
        require(user == course.owner) {
            "Only the owner can add a subject"
        }
        subject.course?.let {
            touch(it)
            it.subjects.remove(subject)
        }
        subject.course = course
        course.subjects.add(subject)
        subjectService.save(subject)
    }

    fun save(course: Course): Course {
        return courseRepository.save(course)
    }

    fun count(): Long {
        return courseRepository.count()
    }

    fun findPageOfAllByOwner(
        owner: MaterialUser,
        pageable: Pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "lastUpdated"))
    )
            : Page<Course> {
        return courseRepository.findAllByOwner(owner, pageable)
    }

    fun findAllByOwner(
        owner: MaterialUser,
        sort: Sort = Sort.by(Sort.Direction.DESC, "lastUpdated")
    )
            : List<Course> {
        return courseRepository.findAllByOwner(owner, sort)
    }

    fun findAllWithSubjectsByOwner(
        owner: MaterialUser,
        pageable: Pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "lastUpdated"))
    )
            : Page<Course> {
        return courseRepository.findAllWithSubjectsByOwner(owner, pageable)
    }

    fun findFirstCourseByOwner(owner: MaterialUser) : Course? {
        return courseRepository.findFirstByOwner(owner)
    }
}