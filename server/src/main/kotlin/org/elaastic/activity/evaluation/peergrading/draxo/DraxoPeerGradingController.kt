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
package org.elaastic.activity.evaluation.peergrading.draxo

import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.ResponseService
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.assignment.AssignmentService
import org.elaastic.moderation.UtilityGrade
import org.elaastic.player.evaluation.EvaluationModel
import org.elaastic.player.evaluation.chatgpt.ChatGptEvaluationModelFactory
import org.elaastic.player.evaluation.draxo.DraxoEvaluationModel
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import java.util.*

@Controller
@RequestMapping("/peer-grading/draxo")
class DraxoPeerGradingController(
    @Autowired val responseService: ResponseService,
    @Autowired val assignmentService: AssignmentService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val messageSource: MessageSource,
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService,
    @Autowired val draxoPeerGradingService: DraxoPeerGradingService,
) {

    /**
     * Get all the DRAXO evaluations of a response
     *
     * @param authentication the current user authentication
     * @param model the model
     * @param responseId the id of the response
     * @param hideName if the name of the grader should be hidden
     * @return the view of the list of DRAXO evaluations
     */
    @GetMapping("/{responseId}")
    fun getAll(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long,
        @RequestParam hideName: Boolean?
    ): String {
        val user: User = authentication.principal as User

        val response = responseService.findById(responseId)
        val assignment = response.interaction.sequence.assignment
        val chatGptEvaluationEnabled = response.interaction.sequence.chatGptEvaluationEnabled

        // Check authorizations
        if (
            assignment?.owner != user &&
            (assignment == null || !assignmentService.userIsRegisteredInAssignment(user, assignment))
        ) {
            throw AccessDeniedException("You are not authorized to access to those feedbacks")
        }
        val teacher = assignment.owner
        val isTeacher = user == teacher
        val isResponseAuthor = user == response.learner
        val isTeacherResponseAuthor = isTeacher && isResponseAuthor

        var draxoPeerGradingList = draxoPeerGradingService.findAllDraxo(response)

        // The student can't see the hidden feedbacks
        if (!isTeacher) draxoPeerGradingList = draxoPeerGradingList.filter { !it.hiddenByTeacher }

        val draxoEvaluationModels = draxoPeerGradingList.mapIndexed { index, draxoPeerGrading ->
            DraxoEvaluationModel(
                index,
                draxoPeerGrading,
                isTeacher,
                responseService.canReactOnFeedbackOfResponse(user, response),
                responseService.canHidePeerGrading(user, response)
            )
        }
        val chatGptEvaluation = chatGptEvaluationService.findEvaluationByResponse(response)
        val evaluationIsHidden = chatGptEvaluation?.hiddenByTeacher == true

        val chatGptEvaluationModel =
            if (chatGptEvaluationEnabled && (isTeacher || !evaluationIsHidden) && !isTeacherResponseAuthor && chatGptEvaluation != null) {
                ChatGptEvaluationModelFactory.build(
                    evaluation = chatGptEvaluation,
                    sequence = response.interaction.sequence,
                    canHideGrading = responseService.canHidePeerGrading(user, response),
                    responseId = response.id
                )
            } else null

        val evaluationModel = EvaluationModel(
            draxoEvaluationModels,
            chatGptEvaluationModel,
            hideName ?: false,
            canSeeChatGPTEvaluation = isTeacher || isResponseAuthor, // Only the teacher and the learner of the question can see the ChatGPT evaluation
            isTeacher = isTeacher
        )

        model["user"] = user
        model["evaluationModel"] = evaluationModel

        return "player/assignment/sequence/phase/evaluation/method/draxo/_draxo-show-list::draxoShowList"
    }


    /**
     * Handle the submission of a DRAXO evaluation utility grade through asynchronous request
     *
     * @param authentication the current user authentication
     * @param evaluationId the id of the evaluation to update
     * @param utilityGrade the utility grade to set
     * @return [ResponseSubmissionAsynchronous] the response of the submission in JSON format
     * @see ResponseSubmissionAsynchronous
     */
    @ResponseBody
    @PostMapping("/submit-utility-grade")
    fun submitDRAXOEvaluationUtilityGrade(
        authentication: Authentication,
        @RequestParam(required = true) evaluationId: Long,
        @RequestParam(required = true) utilityGrade: UtilityGrade
    ): ResponseSubmissionAsynchronous {
        val user = authentication.principal as User
        val evaluation: DraxoPeerGrading = draxoPeerGradingService.getDraxoPeerGrading(evaluationId)
        val locale: Locale = LocaleContextHolder.getLocale()

        val responseSubmissionAsynchronous = run {
            try {
                peerGradingService.updateUtilityGrade(user, evaluation, utilityGrade)

                ResponseSubmissionAsynchronous(
                    success = true,
                    header = messageSource.getMessage("evaluation.submitUtilityGrade.success.header", null, locale),
                    content = messageSource.getMessage("evaluation.submitUtilityGrade.success.content", null, locale)
                )
            } catch (e: Exception) {
                ResponseSubmissionAsynchronous(
                    success = false,
                    header = messageSource.getMessage("evaluation.submitUtilityGrade.error.header", null, locale),
                    content = messageSource.getMessage("evaluation.submitUtilityGrade.error.content", null, locale)
                )
            }
        }

        return responseSubmissionAsynchronous
    }


    /**
     * Handle the submission of a DRAXO evaluation report through asynchronous request
     *
     * @param authentication the current user authentication
     * @param model the model
     * @param evaluationId the id of the evaluation to update
     * @param reasons the reasons to report the evaluation
     * @param otherReasonComment the comment of the other reason
     * @return [ResponseSubmissionAsynchronous] the response of the submission in JSON format
     */
    @ResponseBody
    @PostMapping("/report-draxo-evaluation")
    fun reportDRAXOEvaluation(
        authentication: Authentication,
        model: Model,
        @RequestParam(required = true) evaluationId: Long,
        @RequestParam(value = "reason", required = true) reasons: List<String>,
        @RequestParam(value = "other-reason-comment", required = false) otherReasonComment: String
    ): ResponseSubmissionAsynchronous {
        val user: User = authentication.principal as User
        val evaluation: DraxoPeerGrading = draxoPeerGradingService.getDraxoPeerGrading(evaluationId)
        val reasonComment = otherReasonComment.ifEmpty { null }
        val locale: Locale = LocaleContextHolder.getLocale()

        val responseSubmissionAsynchronous = kotlin.run {
            try {
                peerGradingService.updateReport(user, evaluation, reasons, reasonComment)

                ResponseSubmissionAsynchronous(
                    success = true,
                    header = messageSource.getMessage("evaluation.reportEvaluation.success.header", null, locale),
                    content = messageSource.getMessage("evaluation.reportEvaluation.success.content", null, locale)
                )
            } catch (e: Exception) {
                ResponseSubmissionAsynchronous(
                    success = false,
                    header = messageSource.getMessage("evaluation.reportEvaluation.error.header", null, locale),
                    content = messageSource.getMessage("evaluation.reportEvaluation.error.content", null, locale)
                )
            }
        }

        return responseSubmissionAsynchronous
    }

    /**
     * Handle the submission of a hiding request for a DRAXO evaluation through asynchronous request
     *
     * @param authentication the current user authentication
     * @param model the model
     * @param id the id of the evaluation to hide
     * @return [ResponseSubmissionAsynchronous] the response of the submission in JSON format
     * @see ResponseSubmissionAsynchronous
     */
    @ResponseBody
    @GetMapping("/hide/{id}")
    fun hide(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): ResponseSubmissionAsynchronous {
        val user: User = authentication.principal as User
        val evaluation: DraxoPeerGrading = draxoPeerGradingService.getDraxoPeerGrading(id)
        val locale: Locale = LocaleContextHolder.getLocale()

        val responseSubmissionAsynchronous = kotlin.run {
            try {
                if (!responseService.canHidePeerGrading(user, evaluation.response)) {
                    throw AccessDeniedException("You are not authorized to hide this feedback")
                }

                peerGradingService.markAsHidden(user, evaluation)

                ResponseSubmissionAsynchronous(
                    success = true,
                    header = messageSource.getMessage("evaluation.hideEvaluation.success.header", null, locale),
                    content = messageSource.getMessage("evaluation.hideEvaluation.success.content", null, locale)
                )
            } catch (e: AccessDeniedException) {
                ResponseSubmissionAsynchronous(
                    success = false,
                    header = messageSource.getMessage("evaluation.accesDenied.header", null, locale),
                    content = messageSource.getMessage("evaluation.hideEvaluation.accesDenied.content", null, locale)
                )
            } catch (e: Exception) {
                ResponseSubmissionAsynchronous(
                    success = false,
                    header = messageSource.getMessage("evaluation.hideEvaluation.error.header", null, locale),
                    content = messageSource.getMessage("evaluation.hideEvaluation.error.content", null, locale)
                )
            }
        }

        return responseSubmissionAsynchronous
    }

    /**
     * Handle the submission of an unhiding request for a DRAXO evaluation through asynchronous request
     *
     * @param authentication the current user authentication
     * @param model the model
     * @param id the id of the evaluation to unhide
     * @return [ResponseSubmissionAsynchronous] the response of the submission in JSON format
     * @see ResponseSubmissionAsynchronous
     */
    @ResponseBody
    @GetMapping("/unhide/{id}")
    fun unhide(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): ResponseSubmissionAsynchronous {
        val user: User = authentication.principal as User
        val evaluation: DraxoPeerGrading = draxoPeerGradingService.getDraxoPeerGrading(id)
        val locale: Locale = LocaleContextHolder.getLocale()

        val responseSubmissionAsynchronous = kotlin.run {
            try {

                if (!responseService.canHidePeerGrading(user, evaluation.response)) {
                    throw AccessDeniedException("You are not authorized to unhide this feedback")
                }

                peerGradingService.markAsShow(user, evaluation)

                ResponseSubmissionAsynchronous(
                    success = true,
                    header = messageSource.getMessage("evaluation.unhideEvaluation.success.header", null, locale),
                    content = messageSource.getMessage("evaluation.unhideEvaluation.success.content", null, locale)
                )
            } catch (e: AccessDeniedException) {
                ResponseSubmissionAsynchronous(
                    success = false,
                    header = messageSource.getMessage("evaluation.accesDenied.header", null, locale),
                    content = messageSource.getMessage("evaluation.unhideEvaluation.accesDenied.content", null, locale)
                )
            } catch (e: Exception) {
                ResponseSubmissionAsynchronous(
                    success = false,
                    header = messageSource.getMessage("evaluation.unhideEvaluation.error.header", null, locale),
                    content = messageSource.getMessage("evaluation.unhideEvaluation.error.content", null, locale)
                )
            }
        }

        return responseSubmissionAsynchronous
    }

    /**
     * Response for submission through asynchronous request
     *
     * @param success true if the submission was successful, false otherwise
     * @param header the header of the response (Meant to be used in a semantic-ui message)
     * @param content the content of the response (Meant to be used in a semantic-ui message)
     */
    data class ResponseSubmissionAsynchronous(val success: Boolean, val header: String, val content: String)
}