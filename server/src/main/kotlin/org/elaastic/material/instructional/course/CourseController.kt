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

import org.elaastic.common.web.MessageBuilder
import org.elaastic.user.User
import org.elaastic.common.persistence.pagination.PaginationUtil
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.subject.SubjectController
import org.elaastic.material.instructional.subject.SubjectService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Controller
@RequestMapping("/course")
@Transactional
class CourseController(
    @Autowired val courseService: CourseService,
    @Autowired val subjectService: SubjectService,
    @Autowired val messageBuilder: MessageBuilder
) {

    @GetMapping(value = ["", "/", "/index"])
    fun index(
        authentication: Authentication,
        model: Model,
        @RequestParam("page") page: Int?,
        @RequestParam("size") size: Int?
    ): String {

        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        courseService.findAllWithSubjectsByOwner(
            user,
            PageRequest.of((page ?: 1) - 1, size ?: 8, Sort.by(Sort.Direction.DESC, "lastUpdated"))
        )
            .let {
                model.addAttribute("user", user)
                model.addAttribute("coursePage", it)
                model.addAttribute("nbSubjectsWithoutCourse", subjectService.countWithoutCourse(user))
                model.addAttribute(
                    "pagination",
                    PaginationUtil.buildInfo(
                        it.totalPages,
                        page,
                        size
                    )
                )
            }
        return "course/index"
    }


    @GetMapping("create")
    fun create(authentication: Authentication, model: Model): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        if (!model.containsAttribute("course")) {
            model.addAttribute("course", CourseData(owner = user))
        }
        model.addAttribute("user", user)

        return "course/create"
    }

    @GetMapping(value = ["/{id}", "{id}/show"])
    fun show(
        authentication: Authentication, model: Model,
        @PathVariable id: Long,
        @RequestParam("page") page: Int?,
        @RequestParam("size") size: Int?
    ): String {

        val user = MaterialUser.fromElaasticUser(authentication.principal as User)
        model.addAttribute("user", user)

        if(id != -1L) {
            val course = courseService.get(id, fetchSubjects = true)
            model.addAttribute("course", course)
            model.addAttribute("subjects", course.subjects.toList())
            return "course/show"
        }
        else {
            subjectService.findAllWithoutCourseByOwner(
                user,
                PageRequest.of((page ?: 1) - 1, size ?: 10, Sort.by(Sort.Direction.DESC, "lastUpdated"))
            ).let {
                model.addAttribute("subjectsPage", it)
                model.addAttribute(
                    "pagination",
                    PaginationUtil.buildInfo(
                        it.totalPages,
                        page,
                        size
                    )
                )
            }


            return "course/show-without-course"
        }
    }

    @PostMapping("{id}/update")
    fun update(
        authentication: Authentication,
        @Valid @ModelAttribute courseData: CourseData,
        result: BindingResult,
        model: Model,
        @PathVariable id: Long,
        response: HttpServletResponse,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        model.addAttribute("user", user)

        return if (result.hasErrors()) {
            response.status = HttpStatus.BAD_REQUEST.value()
            model.addAttribute("course", courseData)
            "redirect:/course/$id"
        } else {
            courseService.get(user, id).let {
                it.updateFrom(courseData.toEntity())
                courseService.save(it)

                with(messageBuilder) {
                    success(
                        redirectAttributes,
                        message(
                            "course.updated.message",
                            message("course.label"),
                            it.title
                        )
                    )
                }
                model.addAttribute("course", it)
                "redirect:/course/$id"
            }
        }
    }

    @PostMapping("save")
    fun save(
        authentication: Authentication,
        @Valid @ModelAttribute courseData: CourseData,
        result: BindingResult,
        model: Model,
        response: HttpServletResponse,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        return if (result.hasErrors()) {
            response.status = HttpStatus.BAD_REQUEST.value()
            model.addAttribute("user", user)
            model.addAttribute("course", courseData)
            "/course/create"
        } else {
            val course = courseData.toEntity()
            courseService.save(course)
            "redirect:/course/${course.id}"
        }
    }

    @GetMapping("firstCourse")
    fun firstCourse(
            authentication: Authentication,
            model: Model,
            @RequestParam("page") page: Int?,
            @RequestParam("size") size: Int?
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)
        var firstCourse = courseService.findFirstCourseByOwner(user)
        if(firstCourse == null)
            firstCourse = createExampleCourse(user)
        return "redirect:/course/${firstCourse.id}"
    }

    private fun createExampleCourse(user: MaterialUser): Course =
        courseService.save(CourseData(title = "Example Course", owner = user).toEntity())

    @GetMapping("{id}/delete")
    fun delete(
        authentication: Authentication,
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        val course = courseService.get(user, id)
        courseService.delete(user, course)

        with(messageBuilder) {
            success(
                redirectAttributes,
                message(
                    "course.deleted.message",
                    message("course.label"),
                    course.title
                )
            )
        }

        return "redirect:/course"
    }

    @GetMapping("{courseId}/addSubject")
    fun addSubject(
        authentication: Authentication,
        model: Model,
        @PathVariable courseId: Long
    ): String {

        val user = MaterialUser.fromElaasticUser(authentication.principal as User)
        val course = courseService.get(user, courseId)

        model.addAttribute("user", user)
        model.addAttribute("course", course)
        model.addAttribute("listCourse", courseService.findAllByOwner(user))
        model.addAttribute("subjectData", SubjectController.SubjectData(owner = user, course = course))

        return "subject/create"
    }

    data class CourseData(
        var id: Long? = null,
        var version: Long? = null,
        @field:NotBlank var title: String? = null,
        @field:NotNull var owner: MaterialUser? = null
    ) {
        fun toEntity(): Course {
            return Course(
                title = title!!,
                owner = owner!!
            ).let {
                it.id = id
                it.version = version
                it
            }
        }
    }

}