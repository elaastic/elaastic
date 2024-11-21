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

package org.elaastic.player

import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseService
import org.elaastic.activity.results.AttemptNum
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.analytics.lrs.EventLogService
import org.elaastic.assignment.Assignment
import org.elaastic.assignment.AssignmentService
import org.elaastic.assignment.LearnerAssignment
import org.elaastic.common.web.ControllerUtil
import org.elaastic.common.web.MessageBuilder
import org.elaastic.material.instructional.course.Course
import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.player.dashboard.DashboardModel
import org.elaastic.player.dashboard.DashboardModelFactory
import org.elaastic.player.evaluation.chatgpt.ChatGptEvaluationModelFactory
import org.elaastic.player.results.TeacherResultDashboardService
import org.elaastic.player.results.learner.LearnerResultsModel
import org.elaastic.player.results.learner.LearnerResultsModelFactory
import org.elaastic.player.websocket.AutoReloadSessionHandler
import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.LearnerSequenceService
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.phase.LearnerPhaseService
import org.elaastic.sequence.phase.evaluation.EvaluationPhaseConfig
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.user.AnonymousUserService
import org.elaastic.user.User
import org.elaastic.user.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.ModelAndView
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import org.togglz.core.manager.FeatureManager
import java.util.*
import javax.persistence.EntityNotFoundException
import javax.servlet.http.HttpServletRequest
import javax.transaction.Transactional

@Controller
@RequestMapping("/player", "/elaastic-questions/player")
class PlayerController(
    @Autowired val anonymousUserService: AnonymousUserService,
    @Autowired val assignmentService: AssignmentService,
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService,
    @Autowired val draxoPeerGradingService: DraxoPeerGradingService,
    @Autowired val eventLogService: EventLogService,
    @Autowired val interactionService: InteractionService,
    @Autowired val learnerPhaseService: LearnerPhaseService,
    @Autowired val learnerSequenceService: LearnerSequenceService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val responseService: ResponseService,
    @Autowired val sequenceService: SequenceService,
    @Autowired val teacherResultDashboardService: TeacherResultDashboardService,
    @Autowired val userService: UserService,
    @Autowired val messageBuilder: MessageBuilder,
    @Autowired val featureManager: FeatureManager,
    @Autowired val messageSource: MessageSource,
) {

    private val autoReloadSessionHandler = AutoReloadSessionHandler

    @GetMapping(value = ["", "/", "/index"])
    fun index(
        authentication: Authentication,
        model: Model
    ): String {
        val user: User = authentication.principal as User

        // TODO N+1 SELECT (Assignment => Course)
        val assignments: List<Assignment> = assignmentService.findAllAssignmentsForLearner(user)
        val mapCourseAssignments: Map<Course, MutableList<Assignment>> =
            assignmentService.getCoursesAssignmentsMap(assignments)
        val assignmentsWithoutCourse: List<Assignment> =
            assignments.filter { assignment -> assignment.subject?.course == null }
        model.addAttribute("user", user)
        model.addAttribute("mapCourseAssignments", mapCourseAssignments)
        model.addAttribute("assignmentsWithoutCourse", assignmentsWithoutCourse)

        return "player/index"
    }

    private fun findAssignment(globalId: String?): Assignment {
        require(!(globalId == null || globalId == "")) {
            messageBuilder.message("assignment.register.empty.globalId")
        }

        return assignmentService.findByGlobalId(
            UUID.fromString(globalId)
        ) ?: throw EntityNotFoundException(
            messageBuilder.message("assignment.globalId.does.not.exist")
        )
    }

    @GetMapping("/register")
    fun register(
        authentication: Authentication?,
        model: Model,
        @RequestParam("globalId") globalId: String?
    ): ModelAndView {
        findAssignment(globalId).let {

            return if (authentication == null && it.acceptAnonymousUsers) {
                model.addAttribute("globalId", globalId)
                model.addAttribute("assignmentTitle", it.title)
                ModelAndView("/player/assignment/register")
            } else {
                ModelAndView(
                    "redirect:/player/authenticated-register",
                    mapOf("globalId" to globalId)
                )
            }
        }
    }

    /**
     * Register action for authenticated users (this URL is not accessible to
     * anonymous users)
     */
    @GetMapping("/authenticated-register")
    fun authenticatedOnlyRegister(
        authentication: Authentication,
        @RequestParam("globalId") globalId: String
    ): String {
        return doRegister(
            authentication.principal as User,
            findAssignment(globalId)
        )
    }

    /**
     * Start an anonymous session for a student :
     * - the user is only identified by a nickname
     * - a user entity will be created (with isAnonymous flag)
     * - the user won't be able to log anymore for this user ; it may just use
     *   the service during the session lifetime
     * - the session start by registering the on the assignment identified by
     *   globalId
     */
    @GetMapping("/start-anonymous-session")
    @Transactional
    fun startAnonymousSession(
        authentication: Authentication?,
        @RequestParam("nickname") nickname: String,
        @RequestParam("globalId") globalId: String,
        redirectAttributes: RedirectAttributes
    ): String {
        authentication == null ||
                throw IllegalStateException("You cannot start an anonymous session while being authenticated")

        if (nickname == "") {
            redirectAttributes.addAttribute("globalId", globalId)
            with(messageBuilder) {
                error(redirectAttributes, message("nickname.mandatory"))
            }

            return "redirect:/player/register"
        }

        val anonymousAuthentication =
            anonymousUserService.authenticateAnonymousUser(nickname)
        SecurityContextHolder.getContext().authentication = anonymousAuthentication

        return doRegister(
            anonymousAuthentication.principal as User,
            findAssignment(globalId)
        )
    }

    private fun doRegister(user: User, assignment: Assignment): String {
        assignmentService.registerUser(user, assignment)
        return "redirect:/player/assignment/${assignment.id}/play"
    }

    @GetMapping(
        "/assignment/{assignmentId}/play/sequence/{sequenceId}",
        "/assignment/{assignmentId}/play",
        "/assignment/{assignmentId}/{sequenceId}"
    )
    fun playAssignment(
        authentication: Authentication,
        httpServletRequest: HttpServletRequest,
        model: Model,
        @PathVariable assignmentId: Long,
        @PathVariable sequenceId: Long?,
        @RequestParam("currentPane", required = false) openedPane: String?,
    ): String {

        val user: User = authentication.principal as User
        val assignment: Assignment = assignmentService.get(assignmentId, true)
        model.addAttribute("user", user)

        if (assignment.sequences.isEmpty()) {
            return "redirect:/subject/${assignment.subject!!.id}"
        }

        val sequenceIdValid: Long = sequenceId ?: assignment.sequences.first().id!!

        // TODO Improve data fetching (should start from the assignment)
        sequenceService.get(sequenceIdValid, true).let { sequence ->
            model.addAttribute("user", user)
            val teacher = user == sequence.owner

            return if (teacher)
                playAssignmentForTeacher(
                    user,
                    model,
                    sequence,
                    openedPane ?: "assignments",
                    httpServletRequest
                )
            else
                playAssignmentForLearner(
                    user,
                    model,
                    sequence,
                    openedPane ?: "assignments",
                    httpServletRequest
                )
        }

    }

    private fun playAssignmentForTeacher(
        user: User,
        model: Model,
        selectedSequence: Sequence,
        openedPane: String,
        httpServletRequest: HttpServletRequest,
    ): String {
        val assignment: Assignment = selectedSequence.assignment!!
        val previousSequence: Sequence? = sequenceService.findPreviousSequence(selectedSequence)
        val nextSequence: Sequence? = sequenceService.findNextSequence(selectedSequence)
        val registeredUsers: List<LearnerAssignment> = assignmentService.getRegisteredUsers(assignment)
        val nbRegisteredUsers = registeredUsers.size
        val responses: List<Response> = interactionService.findAllResponsesBySequenceOrderById(selectedSequence)

        // Associate each learner with the number of evaluations he made
        val evaluationCountByUser = peerGradingService.countEvaluationsMadeByUsers(registeredUsers, selectedSequence)
        // Associate each learner with a boolean indicating if he answered the question
        val reponseAvailable = try {
            peerGradingService.learnerToIfTheyAnswer(registeredUsers, selectedSequence)
        } catch (_: IllegalStateException) {
            registeredUsers.associateWith { false }
        }
        // Count the number of responses gradable
        val countResponseGradable: Long = try {
            val responseStudent = responseService.findAllByAttemptNotFake(1, selectedSequence).size.toLong()
            val responseFake = responseService.findAllFakeResponses(selectedSequence).size.toLong()
            responseStudent + responseFake
        } catch (_: IllegalStateException) {
            0
        }


        val dashboardModel: DashboardModel =
            DashboardModelFactory.build(
                selectedSequence,
                previousSequence,
                nextSequence,
                registeredUsers,
                responses,
                openedPane,
                evaluationCountByUser,
                reponseAvailable,
                countResponseGradable,
            )
        model["dashboardModel"] = dashboardModel

        model["playerModel"] = PlayerModelFactory.buildForTeacher(
            user = user,
            sequence = selectedSequence,
            serverBaseUrl = ControllerUtil.getServerBaseUrl(httpServletRequest),
            nbRegisteredUsers = nbRegisteredUsers,
            messageBuilder = messageBuilder,
            sequenceStatistics = sequenceService.getStatistics(selectedSequence),
            teacherResultDashboardService = teacherResultDashboardService,
            nbReportBySequence = sequenceService.getNbReportBySequence(assignment.sequences, true),
        )

        return "player/assignment/sequence/play-teacher"
    }

    private fun playAssignmentForLearner(
        user: User,
        model: Model,
        sequence: Sequence,
        openedPane: String,
        httpServletRequest: HttpServletRequest,
    ): String {

        val assignment = sequence.assignment!!
        val nbRegisteredUsers = assignmentService.getNbRegisteredUsers(assignment)

        val learnerSequence = learnerSequenceService.getLearnerSequence(user, sequence)
        learnerPhaseService.loadPhaseList(learnerSequence)

        model.addAttribute(
            "playerModel",
            PlayerModelFactory.buildForLearner(
                sequence = sequence,
                nbRegisteredUsers = nbRegisteredUsers,
                openedPane = openedPane,
                messageBuilder = messageBuilder,
                activeInteraction = learnerSequenceService.getActiveInteractionForLearner(
                    user,
                    sequence
                ),
                learnerSequence = learnerSequence
            )
        )

        val userAgent = httpServletRequest.getHeader("User-Agent")
        eventLogService.consultPlayer(sequence, user, learnerSequence, userAgent)

        return "player/assignment/sequence/play-learner"
    }

    @GetMapping("/assignment/{id}/nbRegisteredUsers")
    @ResponseBody
    fun getNbRegisteredUsers(@PathVariable id: Long): Int {
        return assignmentService.getNbRegisteredUsers(id)
    }

    @GetMapping("/sequence/{id}/start")
    fun startSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long,
        @RequestParam executionContext: ExecutionContext,
        @RequestParam studentsProvideExplanation: Boolean?,
        @RequestParam responseToEvaluateCount: Int?,
        @RequestParam chatGptEvaluation: Boolean?,
        @RequestParam evaluationPhaseConfig: EvaluationPhaseConfig?,
    ): String {
        val user: User = authentication.principal as User
        var assignment: Assignment?

        sequenceService.get(user, id, true)
            .let {
                sequenceService.start(
                    user,
                    it,
                    executionContext,
                    studentsProvideExplanation ?: false,
                    responseToEvaluateCount ?: 0,
                    evaluationPhaseConfig,
                    chatGptEvaluation ?: false && studentsProvideExplanation ?: false
                )
                userService.updateUserActiveSince(user)
                autoReloadSessionHandler.broadcastReload(id)
                assignment = it.assignment!!
            }

        return "redirect:/player/assignment/${assignment!!.id}/play/sequence/${id}"
    }

    @GetMapping("/interaction/{id}/restart")
    fun restartInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User
        val interaction = interactionService.restart(user, id)
        autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
        return "redirect:/player/assignment/${interaction.sequence.assignment!!.id}/play/sequence/${interaction.sequence.id}"
    }

    @GetMapping("/interaction/{id}/start")
    fun startInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User
        val interaction = interactionService.start(user, id)
        autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
        return "redirect:/player/assignment/${interaction.sequence.assignment!!.id}/play/sequence/${interaction.sequence.id}"
    }

    @GetMapping("/interaction/{id}/startNext")
    fun startNextInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User

        interactionService.findById(id).let {
            sequenceService.loadInteractions(it.sequence)
            val interaction = interactionService.startNext(user, it)
            autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
            return "redirect:/player/assignment/${interaction.sequence.assignment!!.id}/play/sequence/${interaction.sequence.id}"
        }
    }

    @GetMapping("/interaction/{id}/skipNext")
    fun skipNextInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User

        interactionService.findById(id).let {
            sequenceService.loadInteractions(it.sequence)
            val interaction = interactionService.skipNext(user, it)
            autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
            return "redirect:/player/assignment/${interaction.sequence.assignment!!.id}/play/sequence/${interaction.sequence.id}"
        }
    }

    @GetMapping("/interaction/{id}/stop")
    fun stopInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User

        interactionService.findById(id).let {
            sequenceService.loadInteractions(it.sequence)
            interactionService.stop(user, id)
            autoReloadSessionHandler.broadcastReload(it.sequence.id!!)
            return "redirect:/player/assignment/${it.sequence.assignment!!.id}/play/sequence/${it.sequence.id}"
        }
    }

    @GetMapping("/sequence/{id}/stop")
    fun stopSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User
        var assignment: Assignment?

        sequenceService.get(user, id).let {
            sequenceService.stop(user, it)
            autoReloadSessionHandler.broadcastReload(id)
            assignment = it.assignment!!
        }

        return "redirect:/player/assignment/${assignment!!.id}/play/sequence/${id}"
    }

    @GetMapping("/sequence/{id}/reopen")
    fun reopenSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User
        var assignment: Assignment?

        sequenceService.get(user, id).let {
            sequenceService.reopen(user, it)
            autoReloadSessionHandler.broadcastReload(id)
            assignment = it.assignment!!
        }

        return "redirect:/player/assignment/${assignment!!.id}/play/sequence/${id}"
    }

    @GetMapping("/sequence/{id}/publish-results")
    fun publishResults(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User
        var assignment: Assignment?

        sequenceService.get(user, id, true).let {
            sequenceService.publishResults(user, it)
            autoReloadSessionHandler.broadcastReload(id)
            assignment = it.assignment!!
        }

        return "redirect:/player/assignment/${assignment!!.id}/play/sequence/${id}"
    }

    @GetMapping("/sequence/{id}/refresh-results")
    fun refreshResults(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User
        var assignment: Assignment?

        sequenceService.get(id, true).let {
            sequenceService.refreshResults(user, it)
            assignment = it.assignment!!
        }

        return "redirect:/player/assignment/${assignment!!.id}/play/sequence/${id}"
    }

    @GetMapping("/sequence/{id}/unpublish-results")
    fun unpublishResults(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ): String {
        val user: User = authentication.principal as User
        var assignment: Assignment?

        sequenceService.get(user, id, true).let {
            sequenceService.unpublishResults(user, it)
            autoReloadSessionHandler.broadcastReload(id)
            assignment = it.assignment!!
        }

        return "redirect:/player/assignment/${assignment!!.id}/play/sequence/${id}"
    }

    data class ResponseSubmissionData(
        val interactionId: Long,
        val attempt: AttemptNum,
        val choiceList: List<Int>?,
        val confidenceDegree: ConfidenceDegree?,
        val explanation: String?
    )

    @PostMapping("/sequence/{id}/submit-response")
    fun submitResponse(
        authentication: Authentication,
        model: Model,
        @ModelAttribute responseSubmissionData: ResponseSubmissionData,
        @PathVariable id: Long,
        locale: Locale
    ): String {
        val user: User = authentication.principal as User

        val sequence = sequenceService.get(id, true)
        val response = sequenceService.submitResponse(user, sequence, responseSubmissionData)
        if (sequence.chatGptEvaluationEnabled && !sequence.isSecondAttemptAllowed()) {
            chatGptEvaluationService.createEvaluation(response, locale.language)
        }

        return "redirect:/player/assignment/${sequence.assignment!!.id}/play/sequence/${id}"
    }

    @GetMapping("/response/{responseId}/hide-response")
    fun hideResponse(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long
    ): String {
        val user: User = authentication.principal as User

        // Get response from database
        var response = responseService.findById(responseId)

        println(response)
        // Update response visibility
        response = responseService.hideResponse(user, response)
        println("Response hidden by teacher: ${response.hiddenByTeacher}")

        return "redirect:/player/assignment/${response.interaction.sequence.assignment!!.id}/play/sequence/${response.interaction.sequence.id}"
    }

    @GetMapping("/response/{responseId}/unhide-response")
    fun unhideResponse(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long
    ): String {
        val user: User = authentication.principal as User

        // Get response from database
        var response = responseService.findById(responseId)
        // Update response visibility
        response = responseService.unhideResponse(user, response)
        println("Response hidden by teacher: ${response.hiddenByTeacher}")

        return "redirect:/player/assignment/${response.interaction.sequence.assignment!!.id}/play/sequence/${response.interaction.sequence.id}"
    }

    @GetMapping("/response/{responseId}/add-recommended-by-teacher")
    fun addRecommendedByTeacher(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long
    ): String {
        val user: User = authentication.principal as User

        // Get response from database
        var response = responseService.findById(responseId)
        // Update response favourite
        response = responseService.addRecommendedByTeacher(user, response)

        return "redirect:/player/assignment/${response.interaction.sequence.assignment!!.id}/play/sequence/${response.interaction.sequence.id}"
    }

    @GetMapping("/response/{responseId}/remove-recommended-by-teacher")
    fun removeRecommendedByTeacher(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long
    ): String {
        val user: User = authentication.principal as User

        // Get response from database
        var response = responseService.findById(responseId)
        // Update response not favourite
        response = responseService.removeRecommendedByTeacher(user, response)

        return "redirect:/player/assignment/${response.interaction.sequence.assignment!!.id}/play/sequence/${response.interaction.sequence.id}"
    }

    @GetMapping("/sequence/{sequenceId}/regenerate-chat-gpt-evaluation")
    @PreAuthorize("@featureManager.isActive(@featureResolver.getFeature('CHATGPT_EVALUATION'))")
    fun refreshChatGptEvaluation(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long,
        locale: Locale
    ): String {
        val user: User = authentication.principal as User
        val sequence = sequenceService.get(sequenceId, true)

        val response = responseService.find(user, sequence, 2) ?: responseService.find(user, sequence, 1)
        if (response != null) {
            val chatGptEvaluation = chatGptEvaluationService.findEvaluationByResponse(response)
            chatGptEvaluationService.createEvaluation(response, locale.language, chatGptEvaluation)
        }

        return "redirect:/player/assignment/${sequence.assignment!!.id}/play/sequence/${sequenceId}"
    }

    @GetMapping("sequence/{responseId}/chat-gpt-evaluation")
    @PreAuthorize("@featureManager.isActive(@featureResolver.getFeature('CHATGPT_EVALUATION'))")
    fun viewChatGptEvaluation(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long
    ): String {
        authentication.principal as User

        val response = responseService.findById(responseId)
        val chatGptEvaluation = chatGptEvaluationService.findEvaluationByResponse(response)
        model.addAttribute(
            "chatGptEvaluationModel",
            ChatGptEvaluationModelFactory.build(
                chatGptEvaluation,
                response.interaction.sequence,
                responseId = response.id
            )
        )
        return "player/assignment/sequence/components/chat-gpt-evaluation/_chat-gpt-evaluation-viewer"
    }


    /**
     * Get the result view for the given learner
     *
     * @param authentication the current authentication
     * @param model the model
     * @param userId the id of the learner to get the result view
     * @return the result view for the given learner
     */
    @GetMapping("/{sequenceId}/result/{userId}")
    fun result(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long,
        @PathVariable userId: Long
    ): String {
        val user: User = authentication.principal as User

        val sequence = sequenceService.get(sequenceId, true)
        val learner = userService.findById(userId)

        val responseFirstTry = responseService.find(learner, sequence, 1)
        val responseSecondTry = responseService.find(learner, sequence, 2)

        val longBooleanMap = chatGptEvaluationService.associateResponseToChatGPTEvaluationExistence(
            listOf(
                responseFirstTry?.id,
                responseSecondTry?.id
            )
        )

        val responseFirstTryHasChatGPTEvaluation: Boolean = longBooleanMap[responseFirstTry?.id] == true
        val responseSecondTryHasChatGPTEvaluation: Boolean = longBooleanMap[responseSecondTry?.id] == true

        model["studentResultsModel"] = builtLearnerResultsModel(
            responseFirstTry,
            responseSecondTry,
            responseFirstTryHasChatGPTEvaluation,
            responseSecondTryHasChatGPTEvaluation,
            sequence.statement
        )
        // The resultId is used to initialize the accordion in the view.
        // So to discriminate between all accordions in the page, we use the learnerId
        model["resultId"] = userId
        model["seenByTeacher"] = user == sequence.owner
        model["seenByOwner"] = user == learner

        return "player/assignment/sequence/components/my-results/_my-results::myResults"
    }

    /**
     * Get the LearnerResultsModel for the given responses and statement
     *
     * @param responseFirstAttempt the response of the learner for the first
     *    attempt
     * @param responseSecondAttempt the response of the learner for the second
     *    attempt
     * @param statement the statement of the sequence
     */
    private fun builtLearnerResultsModel(
        responseFirstAttempt: Response?,
        responseSecondAttempt: Response?,
        responseFirstTryHasChatGPTEvaluation: Boolean,
        responseSecondTryHasChatGPTEvaluation: Boolean,
        statement: Statement
    ): LearnerResultsModel {

        val learnerResultsModel = when (statement.questionType) {
            QuestionType.ExclusiveChoice -> LearnerResultsModelFactory.buildExclusiveChoiceResult(
                responseFirstAttempt,
                responseSecondAttempt,
                responseFirstTryHasChatGPTEvaluation,
                responseSecondTryHasChatGPTEvaluation,
                statement
            )

            QuestionType.MultipleChoice -> LearnerResultsModelFactory.buildMultipleChoiceResult(
                responseFirstAttempt,
                responseSecondAttempt,
                responseFirstTryHasChatGPTEvaluation,
                responseSecondTryHasChatGPTEvaluation,
                statement
            )

            QuestionType.OpenEnded -> LearnerResultsModelFactory.buildOpenResult(
                responseFirstAttempt,
                responseSecondAttempt,
                responseFirstTryHasChatGPTEvaluation,
                responseSecondTryHasChatGPTEvaluation,
            )
        }
        return learnerResultsModel
    }
}
