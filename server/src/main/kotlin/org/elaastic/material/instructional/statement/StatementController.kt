package org.elaastic.material.instructional.statement

import org.elaastic.assignment.AssignmentService
import org.elaastic.common.web.MessageBuilder
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.question.*
import org.elaastic.material.instructional.subject.SubjectService
import org.elaastic.material.instructional.question.explanation.FakeExplanation
import org.elaastic.material.instructional.question.explanation.FakeExplanationService
import org.elaastic.material.instructional.question.attachment.Attachment
import org.elaastic.material.instructional.question.attachment.AttachmentService
import org.elaastic.material.instructional.question.attachment.MimeType
import org.elaastic.sequence.FakeExplanationData
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import javax.servlet.http.HttpServletResponse
import javax.transaction.Transactional
import javax.validation.Valid
import javax.validation.constraints.Max
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Controller
@RequestMapping("/subject/{subjectId}/statement")
@Transactional
class StatementController(
    @Autowired val assignmentService: AssignmentService,
    @Autowired val statementService: StatementService,
    @Autowired val fakeExplanationService: FakeExplanationService,
    @Autowired val messageBuilder: MessageBuilder,
    @Autowired val subjectService: SubjectService,
    @Autowired val attachmentService: AttachmentService
) {

    @GetMapping("{id}/edit")
    fun edit(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {

        val user = MaterialUser.fromElaasticUser(authentication.principal as User)
        val statementBase = statementService.get(user, id)
        val subject = statementBase.subject!!
        val fakeExplanations = fakeExplanationService.findAllByStatement(statementBase)
        var newStatement = statementBase

        val statementAlreadyUsed = statementService.responsesExistForStatement(statementBase)
        var statementData = StatementData(statementBase, fakeExplanations)

        if (statementAlreadyUsed) {
            newStatement = subjectService.newVersionOfStatementInSubject(statementBase)
            statementService.assignStatementToSequences(newStatement)
            subjectService.removeStatementFromSubject(user, statementBase)
            statementData = StatementData(newStatement, fakeExplanations)
        }

        model.addAttribute("user", user)
        model.addAttribute("subject", subject)
        model.addAttribute("statementData", statementData)
        model.addAttribute("rank", statementBase.rank)

        return if (statementAlreadyUsed) {
            "redirect:/subject/" + subject.id + "/statement/${newStatement.id}/edit"
        } else {
            "/subject/statement/edit"
        }

    }

    @PostMapping("{id}/update")
    fun update(
        authentication: Authentication,
        @RequestParam("fileToAttached") fileToAttached: MultipartFile,
        @Valid @ModelAttribute statementData: StatementData,
        result: BindingResult,
        model: Model,
        @PathVariable subjectId: Long,
        @PathVariable id: Long,
        response: HttpServletResponse,
        redirectAttributes: RedirectAttributes
    ): String {

        val user = MaterialUser.fromElaasticUser(authentication.principal as User)
        val statementBase = statementService.get(user, id)
        val subject = statementBase.subject
        var newStatement = statementBase

        return if (result.hasErrors()) {
            response.status = HttpStatus.BAD_REQUEST.value()
            model.addAttribute("user", user)
            model.addAttribute("subject", subject)
            model.addAttribute("statementData", statementData)
            model.addAttribute("rank", statementBase.rank)
            "/subject/statement/edit"
        } else {
            // if there are responses for this statement
            val statementAlreadyUsed = statementService.responsesExistForStatement(statementBase)
            if (statementAlreadyUsed) {
                newStatement = subjectService.newVersionOfStatementInSubject(statementBase)
            }

            newStatement.let {
                newStatement.updateFrom(statementData.toEntity(user))
                statementService.save(it)
                statementService.updateFakeExplanationList(it, statementData.fakeExplanations)
                attachedFileIfAny(fileToAttached, it)
            }

            with(messageBuilder) {
                success(
                    redirectAttributes,
                    message(
                        "sequence.updated.message.variant",
                        statementBase.title
                    )
                )
            }

            if (statementAlreadyUsed) {
                statementService.assignStatementToSequences(newStatement)
                subjectService.removeStatementFromSubject(user, statementBase)
            }

            return if (statementData.returnOnSubject) {
                redirectAttributes.addAttribute("activeTab", "questions");
                "redirect:/subject/$subjectId"
            } else {
                "redirect:/subject/$subjectId/statement/$id/edit"
            }
        }
    }

    private fun attachedFileIfAny(fileToAttached: MultipartFile, it: Statement) {
        if (!fileToAttached.isEmpty) {
            attachmentService.saveStatementAttachment(
                it,
                createAttachment(fileToAttached),
                fileToAttached.inputStream
            )
        }
    }

    @GetMapping("{id}/delete")
    fun delete(
        authentication: Authentication,
        @PathVariable subjectId: Long,
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        val statement = statementService.get(user, id)
        subjectService.removeStatement(user, statement)

        with(messageBuilder) {
            success(
                redirectAttributes,
                message(
                    "sequence.deleted.message",
                    message("sequence.label"),
                    statement.title
                )
            )
        }
        redirectAttributes.addAttribute("activeTab", "questions");
        return "redirect:/subject/$subjectId"
    }

    @GetMapping("{id}/removeAttachment")
    fun removeAttachment(
        authentication: Authentication,
        @PathVariable subjectId: Long,
        @PathVariable id: Long
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)
        statementService.get(user, id).let {
            attachmentService.detachAttachmentFromStatement(user, it)
        }
        return "redirect:/subject/$subjectId/statement/$id/edit"
    }

    @GetMapping("{id}/up")
    fun up(
        authentication: Authentication,
        @PathVariable subjectId: Long,
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        val subject = subjectService.get(user, subjectId, true)
        subjectService.moveUpStatement(subject, id)
        redirectAttributes.addAttribute("activeTab", "questions");
        return "redirect:/subject/$subjectId#statement_${id}"
    }

    @GetMapping("{id}/down")
    fun down(
        authentication: Authentication,
        @PathVariable subjectId: Long,
        @PathVariable id: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)

        val subject = subjectService.get(user, subjectId, true)
        subjectService.moveDownStatement(subject, id)
        redirectAttributes.addAttribute("activeTab", "questions");
        return "redirect:/subject/$subjectId#statement_${id}"
    }

    @GetMapping("{id}/import/{newSubjectId}")
    fun import(
        authentication: Authentication, model: Model,
        @PathVariable subjectId: Long,
        @PathVariable id: Long,
        @PathVariable newSubjectId: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        val user = MaterialUser.fromElaasticUser(authentication.principal as User)
        val newSubject = subjectService.get(user, newSubjectId, false)
        var statement = statementService.get(user, id)
        statement = subjectService.importStatementInSubject(statement, newSubject)

        with(messageBuilder) {
            success(
                redirectAttributes,
                message(
                    "subject.import.statement.message",
                    message("statement.label"),
                    statement.title,
                    newSubject.title
                )
            )
        }

        redirectAttributes.addAttribute("activeTab", "questions");
        return "redirect:/subject/$subjectId"
    }

    // Note JT : would be nicer if we receive only the relevant data depending on QuestionType
    // Moreover, the gap between the view model and the entities representation is too important
    // The code below could be widely simplified by refactoring the view to use the same model as the backend
    data class StatementData(
        var id: Long? = null,
        var version: Long? = null,
        @field:NotBlank val title: String? = null,
        @field:NotBlank val content: String? = null,
        val attachment: Attachment? = null,
        val hasChoices: Boolean = true,
        val choiceInteractionType: ChoiceType? = null,
        @field:NotNull @field:Max(10) val itemCount: Int? = null,
        @field:NotNull var exclusiveChoice: Int = 1,
        @field:NotNull var expectedChoiceList: List<Int> = listOf(1),
        var returnOnSubject: Boolean = false,
        var expectedExplanation: String?,
        var fakeExplanations: List<FakeExplanationData> = ArrayList()
    ) {
        constructor(statement: Statement) : this(
            id = statement.id,
            version = statement.version,
            title = statement.title,
            content = statement.content,
            hasChoices = statement.questionType != QuestionType.OpenEnded,
            choiceInteractionType = statement.choiceSpecification?.getChoiceType(),
            itemCount = statement.choiceSpecification?.nbCandidateItem ?: 2,
            expectedChoiceList = listOf(1),
            exclusiveChoice = 1,
            expectedExplanation = statement.expectedExplanation,
            fakeExplanations = ArrayList(),
            attachment = statement.attachment

        ) {
            val choiceSpecification: ChoiceSpecification? = statement.choiceSpecification
            when (choiceSpecification) {
                is ExclusiveChoiceSpecification -> exclusiveChoice = choiceSpecification.expectedChoice.index
                is MultipleChoiceSpecification -> expectedChoiceList =
                    choiceSpecification.expectedChoiceList.map { it.index }
            }
        }

        constructor(statement: Statement, fakeExplanations: List<FakeExplanation>) : this(statement) {
            this.fakeExplanations = fakeExplanations.map { FakeExplanationData(it) }
        }

        fun toEntity(user: MaterialUser): Statement {
            val questionType: QuestionType =
                if (hasChoices) {
                    when (choiceInteractionType) {
                        ChoiceType.EXCLUSIVE -> QuestionType.ExclusiveChoice
                        ChoiceType.MULTIPLE -> QuestionType.MultipleChoice
                        null -> throw IllegalStateException("No choiceInteractionType defined")
                    }
                } else QuestionType.OpenEnded

            val choiceSpecification: ChoiceSpecification? =
                if (hasChoices) {
                    when (choiceInteractionType) {
                        null -> null

                        ChoiceType.MULTIPLE -> MultipleChoiceSpecification(
                            itemCount!!,
                            expectedChoiceList.map { n ->
                                ChoiceItem(n, 100f / expectedChoiceList.size)
                            },
                            listOf()
                        )

                        ChoiceType.EXCLUSIVE -> ExclusiveChoiceSpecification(
                            itemCount!!,
                            ChoiceItem(exclusiveChoice, 100f)
                        )
                    }
                } else null


            val statement = Statement(
                owner = user,
                title = title!!,
                content = content!!,
                questionType = questionType,
                choiceSpecification = choiceSpecification,
                expectedExplanation = expectedExplanation
            )
            statement.id = id
            statement.version = version

            return statement
        }
    }

    companion object {

        fun createAttachment(file: MultipartFile): Attachment {
            return Attachment(
                name = file.name,
                size = file.size,
                originalFileName = file.originalFilename,
                mimeType = if (file.contentType.isNullOrBlank()) MimeType() else MimeType(file.contentType!!)
            )
        }

    }
}