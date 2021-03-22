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

package org.elaastic.questions.subject

import org.elaastic.questions.assignment.AssignmentController
import org.elaastic.questions.assignment.AssignmentService
import org.elaastic.questions.attachment.AttachmentService
import org.elaastic.questions.controller.MessageBuilder
import org.elaastic.questions.course.Course
import org.elaastic.questions.course.CourseService
import org.elaastic.questions.directory.User
import org.elaastic.questions.persistence.pagination.PaginationUtil
import org.elaastic.questions.subject.statement.Statement
import org.elaastic.questions.subject.statement.StatementController
import org.elaastic.questions.subject.statement.StatementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import javax.persistence.EntityNotFoundException
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Controller
@RequestMapping("/subject")
@Transactional
class SubjectController(
    @Autowired val subjectService: SubjectService,
    @Autowired val courseService: CourseService,
    @Autowired val statementService: StatementService,
    @Autowired val attachmentService: AttachmentService,
    @Autowired val messageBuilder: MessageBuilder,
    @Autowired val assignmentService: AssignmentService,
    @Autowired val sharedSubjectService: SharedSubjectService
){

    @GetMapping(value = ["", "/", "/index"])
    fun index(authentication: Authentication,
              model: Model,
              @RequestParam("page") page: Int?,
              @RequestParam("size") size: Int?): String {
        val user: User = authentication.principal as User

        subjectService.findAllByOwner(
                user,
                PageRequest.of((page ?: 1) - 1, size ?: 10, Sort.by(Sort.Direction.DESC, "lastUpdated"))
        ).let {
            model.addAttribute("user", user)
            model.addAttribute("subjectPage", it)
            model.addAttribute(
                    "pagination",
                    PaginationUtil.buildInfo(
                            it.totalPages,
                            page,
                            size
                    )
            )
        }

        return "/subject/index"
    }

    @GetMapping(value = ["/{id}", "{id}/show"])
    fun show(authentication: Authentication, model: Model, @PathVariable id: Long,
             @RequestParam ("activeTab", defaultValue = "questions") activeTab: String,
             @RequestParam("page") page: Int?,
             @RequestParam("size") size: Int?): String {
        val user: User = authentication.principal as User
        model.addAttribute("user", user)

        var subject: Subject = subjectService.get(user, id, fetchStatementsAndAssignments = true)
        model.addAttribute("subject",subject)

        var statements: MutableList<Statement> = ArrayList()
        for (statement:Statement in subject.statements){
            if (!statements.contains(statement)) statements.add(statement)
        }
        model.addAttribute("statements",statements)
        model.addAttribute("listCourse", courseService.findAllByOwner(user).toList())
        model.addAttribute("alreadyImported", subjectService.isUsedAsParentSubject(user, subject))
        model.addAttribute("subjectData", SubjectData(owner = user))
        model.addAttribute("activeTab", activeTab)
        subjectService.findAllByOwner(
                user,
                PageRequest.of((page ?: 1) - 1, size ?: 10, Sort.by(Sort.Direction.DESC, "lastUpdated"))
        ).let {
            model.addAttribute("subjects",it.content)
            var firstSubject = Subject("NoSubject", user)
            if (it.content.size != 0)
                firstSubject = it.content.get(0)
            model.addAttribute("firstSubject",firstSubject)
        }

        return "/subject/show"
    }

    @GetMapping("create")
    fun create(authentication: Authentication, model: Model): String {
        val user: User = authentication.principal as User

        if (!model.containsAttribute("subjectData")) {
            model.addAttribute("subjectData", SubjectData(owner = user))
        }
        model.addAttribute("user", user)
        model.addAttribute("listCourse", courseService.findAllByOwner(user).toList())

        return "/subject/create"
    }

    @PostMapping("save")
    fun save(authentication: Authentication,
             @Valid @ModelAttribute subjectData: SubjectData,
             result: BindingResult,
             model: Model,
             response: HttpServletResponse,
             redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User

        return if (result.hasErrors()) {
            response.status = HttpStatus.BAD_REQUEST.value()
            model.addAttribute("user", user)
            model.addAttribute("subject", subjectData)
            "/subject/create"
        } else {
            val subject = subjectData.toEntity()
            subjectService.save(subject)
            redirectAttributes.addAttribute("activeTab", "questions");
            "redirect:/subject/${subject.id}"
        }
    }

    @PostMapping("{subjectId}/addStatement")
    fun addStatement(authentication: Authentication,
                     @RequestParam("fileToAttached") fileToAttached: MultipartFile,
                     @Valid @ModelAttribute statementData: StatementController.StatementData,
                     result: BindingResult,
                     model: Model,
                     @PathVariable subjectId: Long,
                     response: HttpServletResponse,
                     redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User

        val subject = subjectService.get(user, subjectId, fetchStatementsAndAssignments = true)

        if (result.hasErrors()) {
            response.status = HttpStatus.BAD_REQUEST.value()
            model.addAttribute("user", user)
            model.addAttribute("subject", subject)
            model.addAttribute("statement", statementData)
            model.addAttribute("nbStatement",subject.statements.size)
            return "/subject/statement/create"
        } else {
            val statementSaved = subjectService.addStatement(subject, statementData.toEntity(user))
            statementService.updateFakeExplanationList(
                    statementSaved,
                    statementData.fakeExplanations
            )
            attachedFileIfAny(fileToAttached, statementSaved)
            redirectAttributes.addAttribute("activeTab", "questions")
            return "redirect:/subject/${subject.id}"
        }
    }

    private fun attachedFileIfAny(fileToAttached: MultipartFile, it: Statement) {
        if (!fileToAttached.isEmpty) {
            attachmentService.saveStatementAttachment(
                    it,
                    StatementController.createAttachment(fileToAttached),
                    fileToAttached.inputStream)
        }
    }

    @GetMapping("{subjectId}/addStatement")
    fun addStatement(authentication: Authentication,
                    model: Model,
                    @PathVariable subjectId: Long): String {

        val user: User = authentication.principal as User
        val subject = subjectService.get(user, subjectId)

        model.addAttribute("user", user)
        model.addAttribute("subject", subject)
        model.addAttribute("nbStatement",subject.statements.size)
        model.addAttribute(
                "statementData",
                StatementController.StatementData(
                        Statement.createDefaultStatement(user)
                )
        )

        return "/subject/statement/create"
    }

    @PostMapping("{subjectId}/addAssignment")
    fun addAssignment(authentication: Authentication,
                      @Valid @ModelAttribute assignmentData: AssignmentController.AssignmentData,
                      result: BindingResult,
                      model: Model,
                      response: HttpServletResponse,
                      @PathVariable subjectId: Long,
                      redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User
        val subject = subjectService.get(user, subjectId)

        model.addAttribute("user", user)
        model.addAttribute("subject", subject)

        return if (result.hasErrors()) {
            response.status = HttpStatus.BAD_REQUEST.value()
            model.addAttribute("user", user)
            model.addAttribute("assignment", assignmentData)
            "redirect:/subject/${subject.id}/addAssignment"
        } else {
            val assignment = assignmentData.toEntity()
            if (assignment.audience.isNullOrBlank())
                assignment.audience = "na"
            assignmentService.save(assignment)
            subjectService.addAssignment(subject,assignment)
            redirectAttributes.addAttribute("activeTab", "assignments");
            "redirect:/subject/${subject.id}"
        }

    }

    @GetMapping("{subjectId}/addAssignment")
    fun addAssignment(authentication: Authentication,
                      model: Model,
                      @PathVariable subjectId: Long): String {

        val user: User = authentication.principal as User
        val subject = subjectService.get(user, subjectId)

        model.addAttribute("user", user)
        model.addAttribute("nbAssignments",subject.assignments.size)
        if (!model.containsAttribute("assignment")) {
            model.addAttribute("assignment", AssignmentController.AssignmentData(
                    owner = user,
                    subject = subject,
                    title = subject.title).toEntity())
        }

        return "/assignment/create"
    }

    @PostMapping("{id}/update")
    fun update(authentication: Authentication,
               @Valid @ModelAttribute subjectData: SubjectData,
               result: BindingResult,
               model: Model,
               @PathVariable id: Long,
               response: HttpServletResponse,
               redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User

        model.addAttribute("user", user)

        return if (result.hasErrors()) {
            response.status = HttpStatus.BAD_REQUEST.value()
            model.addAttribute("subject", subjectData)
            redirectAttributes.addAttribute("activeTab", "questions");
            "redirect:/subject/$id"
        } else {
            subjectService.get(user, id).let {
                it.updateFrom(subjectData.toEntity())
                subjectService.save(it)

                with(messageBuilder) {
                    success(
                        redirectAttributes,
                        message(
                            "subject.updated.message",
                            message("subject.label"),
                            it.title
                        )
                    )
                }
                model.addAttribute("subject", it)
                redirectAttributes.addAttribute("activeTab", "questions");
                "redirect:/subject/$id"
            }
        }
    }

    @GetMapping("{id}/delete")
    fun delete(authentication: Authentication,
               @PathVariable id: Long,
               redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User

        val subject = subjectService.get(user, id)
        courseService.removeSubject(user, subject)

        with(messageBuilder) {
            success(
                redirectAttributes,
                message(
                    "subject.deleted.message",
                    message("subject.label"),
                    subject.title
                )
            )
        }

        return "redirect:/subject"
    }

    @GetMapping("/shared")
    fun shared(authentication: Authentication,
                 model: Model,
                 @RequestParam("globalId") globalId: String?,
                 redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User

        if (globalId == null || globalId == "") {
            throw IllegalArgumentException(
                    messageBuilder.message("subject.share.empty.globalId")
            )
        }

        subjectService.findByGlobalId(globalId).let {
            if (it == null) {
                throw EntityNotFoundException(
                        messageBuilder.message("subject.globalId.does.not.exist")
                )
            }

            subjectService.sharedToTeacher(user, it)
            redirectAttributes.addAttribute("activeTab", "questions");
            return "redirect:/subject/${it.id}/show"
        }
    }


    @GetMapping(value = ["/shared_index"])
    fun shared_index(authentication: Authentication,
                     model: Model,
                     @RequestParam("page") page: Int?,
                     @RequestParam("size") size: Int?): String {
        val user: User = authentication.principal as User
        var sharedSubjectPage: Page<Subject>? = null
        subjectService.findAllSharedSubjects(
                user,
                PageRequest.of((page ?: 1) - 1, size ?: 10, Sort.by(Sort.Direction.DESC, "lastUpdated"))
        ).let {
            model.addAttribute("user", user)
            sharedSubjectPage = it
            model.addAttribute("sharedSubjectPage", sharedSubjectPage)
            model.addAttribute(
                    "pagination",
                    PaginationUtil.buildInfo(
                            it.totalPages,
                            page,
                            size
                    )
            )
        }
        val sharedInfos: MutableList<SharedSubject>? = ArrayList()
        for (subject: Subject in sharedSubjectPage!!.content){
             sharedInfos!!.add(sharedSubjectService.getSharedSubject(user,subject)!!)
        }
        model.addAttribute("sharedInfos", sharedInfos)

        return "/subject/shared_index"
    }

    @GetMapping(value = ["{id}/importSubject"])
    fun importSubject(authentication: Authentication, model: Model, @PathVariable id: Long,
                      redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User
        val sharedSubject = subjectService.get(user,id)
        val importedSubject = subjectService.import(user, sharedSubject)
        with(messageBuilder) {
            success(
                    redirectAttributes,
                    message(
                            "subject.imported.message",
                            message("subject.label"),
                            importedSubject.title
                    )
            )
        }
        redirectAttributes.addAttribute("activeTab", "questions");
        return "redirect:/subject/${importedSubject.id}/show"
    }

    @GetMapping(value = ["{id}/duplicateSubject"])
    fun duplicateSubject(authentication: Authentication, model: Model, @PathVariable id: Long,
                      redirectAttributes: RedirectAttributes): String {
        val user: User = authentication.principal as User
        val originalSubject = subjectService.get(user,id)
        val duplicatedSubject = subjectService.duplicate(user, originalSubject)
        with(messageBuilder) {
            success(
                    redirectAttributes,
                    message(
                            "subject.duplicated.message",
                            message("subject.label"),
                            duplicatedSubject.title
                    )
            )
        }
        redirectAttributes.addAttribute("activeTab", "questions");
        return "redirect:/subject/${duplicatedSubject.id}/show"
    }



    data class SubjectData(
        var id: Long? = null,
        var version: Long? = null,
        @field:NotBlank var title: String? = null,
        @field:NotNull var owner: User? = null,
        var course: Course? = null
    ) {
        fun toEntity(): Subject {
            return Subject(
                title = title!!,
                owner = owner!!,
                course = course
            ).let {
                it.id = id
                it.version = version
                it
            }
        }
    }

}
