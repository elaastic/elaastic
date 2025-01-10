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
import org.elaastic.activity.response.ResponseService
import org.elaastic.activity.results.AttemptNum
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationResponseStore
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.analytics.lrs.EventLogService
import org.elaastic.assignment.Assignment
import org.elaastic.assignment.AssignmentService
import org.elaastic.assignment.LearnerAssignment
import org.elaastic.common.web.ControllerUtil
import org.elaastic.common.web.MessageBuilder
import org.elaastic.material.instructional.course.Course
import org.elaastic.player.dashboard.DashboardModelFactory
import org.elaastic.player.dashboard.SequenceMonitoringModel
import org.elaastic.player.evaluation.chatgpt.ChatGptEvaluationModelFactory
import org.elaastic.player.results.TeacherResultDashboardService
import org.elaastic.player.results.learner.LearnerResultsModel
import org.elaastic.player.results.learner.LearnerResultsModelFactory
import org.elaastic.player.results.learner.LearnerSequenceResponses
import org.elaastic.player.sequence.SequenceModelFactory
import org.elaastic.player.steps.StepsModel
import org.elaastic.player.steps.StepsModelFactory
import org.elaastic.player.websocket.AutoReloadSessionHandler
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.LearnerSequenceService
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.phase.LearnerPhaseService
import org.elaastic.sequence.phase.evaluation.EvaluationPhaseConfig
import org.elaastic.user.AnonymousUserService
import org.elaastic.user.User
import org.elaastic.user.UserService
import org.slf4j.LoggerFactory
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
    @Autowired val sequenceModelFactory: SequenceModelFactory,
    @Autowired val dashboardModelFactory: DashboardModelFactory,
) {

    private val autoReloadSessionHandler = AutoReloadSessionHandler

    private val logger = LoggerFactory.getLogger(PlayerController::class.java)

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

    /** Register action for authenticated users (this URL is not accessible to anonymous users) */
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
     * - the user won't be able to log anymore for this user ; it may just use the service during the session lifetime
     * - the session start by registering the on the assignment identified by globalId
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
            val isTeacher = user == sequence.owner

            return if (isTeacher)
                playAssignmentForTeacher(user, model, sequence, httpServletRequest)
            else
                playAssignmentForLearner(user, model, sequence, httpServletRequest)
        }

    }

    private fun playAssignmentForTeacher(
        user: User,
        model: Model,
        selectedSequence: Sequence,
        httpServletRequest: HttpServletRequest,
    ): String {
        val assignment: Assignment = selectedSequence.assignment!!
        val registeredUsers: List<LearnerAssignment> = assignmentService.getRegisteredUsers(assignment)

        model["dashboardModel"] = dashboardModelFactory.build(selectedSequence)
        model["playerModel"] = PlayerModelFactory.buildForTeacher(
            user = user,
            sequence = selectedSequence,
            serverBaseUrl = ControllerUtil.getServerBaseUrl(httpServletRequest),
            nbRegisteredUsers = registeredUsers.size,
            messageBuilder = messageBuilder,
            sequenceStatistics = sequenceService.getStatistics(selectedSequence),
            teacherResultDashboardService = teacherResultDashboardService,
            nbReportBySequence = sequenceService.countReportBySequence(assignment.sequences, true),
        )

        return "player/assignment/sequence/play-teacher"
    }

    private fun playAssignmentForLearner(
        user: User,
        model: Model,
        sequence: Sequence,
        httpServletRequest: HttpServletRequest,
    ): String {

        val assignment = sequence.assignment!!
        val nbRegisteredUsers = assignmentService.countRegisteredUsers(assignment)

        val learnerSequence = learnerSequenceService.getLearnerSequence(user, sequence)
        learnerPhaseService.loadPhaseList(learnerSequence)

        model.addAttribute(
            "playerModel",
            PlayerModelFactory.buildForLearner(
                sequence = sequence,
                nbRegisteredUsers = nbRegisteredUsers,
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
        return assignmentService.countRegisteredUsers(id)
    }

    /**
     * Get the sequence view for the given user
     *
     * @param authentication the current authentication
     * @param model the model
     * @param sequenceId the id of the sequence to get the view
     */
    @GetMapping("/sequence/{sequenceId}")
    fun playSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long
    ): String {
        val user: User = authentication.principal as User
        val sequence = sequenceService.get(sequenceId, true)
        val isTeacher = user == sequence.owner

        return if (isTeacher) {
            playSequenceAsTeacher(user, sequence, model)
        } else {
            playSequenceAsLearner(user, sequence, model)
        }
    }

    private fun playSequenceAsTeacher(
        user: User,
        sequence: Sequence,
        model: Model,
    ): String {
        val sequenceModel = sequenceModelFactory.buildForTeacher(user, sequence)

        model["sequenceModel"] = sequenceModel
        return "player/assignment/sequence/play-sequence-teacher"
    }

    private fun playSequenceAsLearner(
        user: User,
        sequence: Sequence,
        model: Model,
    ): String {
        val learnerSequence = learnerSequenceService.getLearnerSequence(user, sequence)
        learnerPhaseService.loadPhaseList(learnerSequence)

        val sequenceModel =
            sequenceModelFactory.buildForLearner(user, learnerSequence, learnerSequence.activeInteraction)

        model["sequenceModel"] = sequenceModel
        return "player/assignment/sequence/play-sequence-learner"
    }


    @ResponseBody
    @GetMapping("/sequence/{sequenceId}/start")
    fun startSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long,
        @RequestParam executionContext: ExecutionContext,
        @RequestParam studentsProvideExplanation: Boolean?,
        @RequestParam responseToEvaluateCount: Int?,
        @RequestParam chatGptEvaluation: Boolean?,
        @RequestParam evaluationPhaseConfig: EvaluationPhaseConfig?,
    ) {
        val user: User = authentication.principal as User

        sequenceService.get(user, sequenceId, true)
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
                autoReloadSessionHandler.broadcastReload(sequenceId)
            }
    }

    @ResponseBody
    @GetMapping("/interaction/{interactionId}/restart")
    fun restartInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable interactionId: Long
    ) {
        val user: User = authentication.principal as User
        val interaction = interactionService.restart(user, interactionId)
        autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
    }

    @ResponseBody
    @GetMapping("/interaction/{id}/start")
    fun startInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable id: Long
    ) {
        val user: User = authentication.principal as User
        val interaction = interactionService.start(user, id)
        autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
    }

    @ResponseBody
    @GetMapping("/interaction/{interactionId}/startNext")
    fun startNextInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable interactionId: Long
    ) {
        val user: User = authentication.principal as User

        interactionService.findById(interactionId).let {
            sequenceService.loadInteractions(it.sequence)
            val interaction = interactionService.startNext(user, it)
            autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
        }
    }

    @ResponseBody
    @GetMapping("/interaction/{interactionId}/skipNext")
    fun skipNextInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable interactionId: Long
    ) {
        val user: User = authentication.principal as User

        interactionService.findById(interactionId).let {
            sequenceService.loadInteractions(it.sequence)
            val interaction = interactionService.skipNext(user, it)
            autoReloadSessionHandler.broadcastReload(interaction.sequence.id!!)
        }
    }

    @ResponseBody
    @GetMapping("/interaction/{interactionId}/stop")
    fun stopInteraction(
        authentication: Authentication,
        model: Model,
        @PathVariable interactionId: Long
    ) {
        val user: User = authentication.principal as User

        interactionService.findById(interactionId).let {
            sequenceService.loadInteractions(it.sequence)
            interactionService.stop(user, interactionId)
            autoReloadSessionHandler.broadcastReload(it.sequence.id!!)
        }
    }

    @ResponseBody
    @GetMapping("/sequence/{sequenceId}/stop")
    fun stopSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long
    ) {
        val user: User = authentication.principal as User

        sequenceService.get(user, sequenceId).let {
            sequenceService.stop(user, it)
            autoReloadSessionHandler.broadcastReload(sequenceId)
        }
    }

    @ResponseBody
    @GetMapping("/sequence/{sequenceId}/reopen")
    fun reopenSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long
    ) {
        val user: User = authentication.principal as User

        sequenceService.get(user, sequenceId).let {
            sequenceService.reopen(user, it)
            autoReloadSessionHandler.broadcastReload(sequenceId)
        }
    }

    @ResponseBody
    @GetMapping("/sequence/{sequenceId}/publish-results")
    fun publishResults(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long
    ) {
        val user: User = authentication.principal as User

        sequenceService.get(user, sequenceId, true).let {
            sequenceService.publishResults(user, it)
            autoReloadSessionHandler.broadcastReload(sequenceId)
        }
    }

    @ResponseBody
    @GetMapping("/sequence/{sequenceId}/refresh-results")
    fun refreshResults(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long
    ) {
        val user: User = authentication.principal as User

        sequenceService.get(sequenceId, true).let {
            sequenceService.refreshResults(user, it)
        }
    }

    @ResponseBody
    @GetMapping("/sequence/{sequenceId}/unpublish-results")
    fun unpublishResults(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long
    ) {
        val user: User = authentication.principal as User

        sequenceService.get(user, sequenceId, true).let {
            sequenceService.unpublishResults(user, it)
            autoReloadSessionHandler.broadcastReload(sequenceId)
        }
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

        // Get response from the database
        var response = responseService.findById(responseId)

        // Update response visibility
        response = responseService.hideResponse(user, response)

        logger.debug("Response hidden by teacher: ${response.hiddenByTeacher}")

        return "redirect:/player/assignment/${response.interaction.sequence.assignment!!.id}/play/sequence/${response.interaction.sequence.id}"
    }

    @GetMapping("/response/{responseId}/unhide-response")
    fun unhideResponse(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long
    ): String {
        val user: User = authentication.principal as User

        // Get response from the database
        var response = responseService.findById(responseId)

        // Update response visibility
        response = responseService.unhideResponse(user, response)

        logger.debug("Response unhidden by teacher: ${response.hiddenByTeacher}")

        return "redirect:/player/assignment/${response.interaction.sequence.assignment!!.id}/play/sequence/${response.interaction.sequence.id}"
    }

    @GetMapping("/response/{responseId}/add-recommended-by-teacher")
    fun addRecommendedByTeacher(
        authentication: Authentication,
        model: Model,
        @PathVariable responseId: Long
    ): String {
        val user: User = authentication.principal as User

        // Get response from the database
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

        // Get response from the database
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

        model["studentResultsModel"] = getLearnerResultsModel(learner, sequence)
        // The resultId is used to initialize the accordion in the view.
        // So to discriminate between all accordions in the page, we use the learnerId
        model["resultId"] = userId
        model["seenByTeacher"] = user == sequence.owner
        model["seenByOwner"] = user == learner

        return "player/assignment/sequence/components/my-results/_my-results::myResults"
    }

    /**
     * Get the result modal for the given learner
     *
     * @param authentication the current authentication
     * @param model the model
     * @param sequenceId the id of the sequence to get the result modal
     * @param userId the id of the learner to get the result modal
     * @return the result modal for the given learner
     */
    @GetMapping("/{sequenceId}/result/{userId}/modal")
    fun resultModal(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long,
        @PathVariable userId: Long
    ): String {
        val user: User = authentication.principal as User

        val sequence = sequenceService.get(sequenceId, true)
        val learner = userService.findById(userId)
        val isTeacher = user == sequence.owner
        val isOwnerOfResponse = user == learner

        val learnerStepsModel: StepsModel = StepsModelFactory.buildForTeacher(sequence)

        val sequenceMonitoringModel = SequenceMonitoringModel(
            sequence.executionContext,
            learnerStepsModel.responseSubmissionState.getDashboardState(),
            learnerStepsModel.evaluationState.getDashboardState(),
            sequenceId = sequence.id
        )

        val learnerAssignment: LearnerAssignment? = assignmentService.getRegisteredUser(sequence.assignment!!, learner)

        // If the learner a fake one, the learnerAssignment will be null.
        // Monitoring what the fake learner has done is not relevant.
        // So we don't display the monitoring model.
        if (learnerAssignment != null) {
            model["learnerMonitoringModel"] = dashboardModelFactory.buildLearnerMonitoringModel(
                learnerAssignment,
                (responseService.find(learner, sequence) ?: responseService.find(learner, sequence, 2)) != null,
                sequenceMonitoringModel,
                sequence,
                peerGradingService.countEvaluationsMadeByUsers(listOf(learnerAssignment), sequence)[learnerAssignment]
                    ?: 0,
                dashboardModelFactory.countGradableResponse(sequence)
            )
        }
        val learnerResultsModel = getLearnerResultsModel(learner, sequence)
        model["learnerResultsModel"] = learnerResultsModel
        // The accordionId is used to initialize the accordion in the view.
        // So to discriminate between all accordions in the page, we use the learnerId
        model["userId"] = userId
        model["seenByTeacher"] = isTeacher
        model["seenByOwner"] = isOwnerOfResponse
        model["learnerHasAnswered"] = learnerResultsModel.hasAnsweredPhase1() || learnerResultsModel.hasAnsweredPhase2()

        return "player/assignment/sequence/components/my-results/_my-results-modal.html :: myResultsModal"
    }

    /** Get the [LearnerResultsModel] for the given learner and sequence */
    private fun getLearnerResultsModel(
        learner: User,
        sequence: Sequence
    ): LearnerResultsModel {
        val responseFirstTry = responseService.find(learner, sequence, 1)
        val responseSecondTry = responseService.find(learner, sequence, 2)

        var chatGptEvaluationResponseStore = ChatGptEvaluationResponseStore()

        if (sequence.chatGptEvaluationEnabled) {
            chatGptEvaluationResponseStore = chatGptEvaluationService.associateResponseToChatGPTEvaluationExistence(
                listOf(
                    responseFirstTry?.id,
                    responseSecondTry?.id
                )
            )
        }


        return LearnerResultsModelFactory.builtLearnerResultsModel(
            LearnerSequenceResponses(
                responseFirstTry,
                responseSecondTry,
                chatGptEvaluationResponseStore
            ),
            sequence.statement
        )
    }
}
