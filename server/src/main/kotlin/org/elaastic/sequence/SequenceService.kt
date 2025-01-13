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

import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseService
import org.elaastic.activity.results.ResultsService
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.analytics.lrs.EventLogService
import org.elaastic.material.instructional.question.explanation.FakeExplanation
import org.elaastic.material.instructional.question.explanation.FakeExplanationRepository
import org.elaastic.material.instructional.question.legacy.LearnerChoice
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.moderation.ReportInformation
import org.elaastic.player.PlayerController
import org.elaastic.player.steps.SequenceStatistics
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.sequence.config.EvaluationSpecification
import org.elaastic.sequence.config.ReadSpecification
import org.elaastic.sequence.config.ResponseSubmissionSpecification
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.sequence.interaction.InteractionRepository
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.interaction.InteractionType
import org.elaastic.sequence.phase.evaluation.EvaluationPhaseConfig
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.util.*
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
@Transactional
class SequenceService(
    @Autowired val sequenceRepository: SequenceRepository,
    @Autowired val fakeExplanationRepository: FakeExplanationRepository,
    @Autowired val eventLogService: EventLogService,
    @Autowired val interactionService: InteractionService,
    @Autowired val interactionRepository: InteractionRepository,
    @Autowired val resultsService: ResultsService,
    @Autowired val learnerSequenceRepository: LearnerSequenceRepository,
    @Autowired val learnerSequenceService: LearnerSequenceService,
    @Autowired val responseService: ResponseService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val draxoPeerGradingService: DraxoPeerGradingService,
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService,
) {

    /**
     * Get a sequence by its id.
     *
     * If the user is not the owner of the sequence, an [AccessDeniedException] is thrown
     * @param user [User] the user requesting the sequence
     * @param id [Long] the id of the sequence
     * @param fetchInteractions [Boolean] if true, fetch the interactions of the sequence
     *
     * @throws [AccessDeniedException] if the user is not the owner of the sequence
     */
    fun get(user: User, id: Long, fetchInteractions: Boolean = false): Sequence =
        get(id, fetchInteractions).let {
            if (it.owner != user) throw AccessDeniedException("You are not authorized to access to this sequence")
            it
        }


    fun get(id: Long, fetchInteractions: Boolean = false): Sequence {
        return sequenceRepository.findOneById(id)?.let { sequence ->

            if (fetchInteractions) {
                loadInteractions(sequence)
            }

            sequence
        } ?: throw EntityNotFoundException("There is no sequence for id \"$id\"")
    }

    fun findPreviousSequence(sequence: Sequence): Sequence? = sequenceRepository.findPreviousSequence(
        sequence.rank,
        sequence.assignment!!,
        PageRequest.of(0, 1)
    ).firstOrNull()

    fun findNextSequence(sequence: Sequence): Sequence? = sequenceRepository.findNextSequence(
        sequence.rank,
        sequence.assignment!!,
        PageRequest.of(0, 1)
    ).firstOrNull()

    fun findByUuid(uuid: UUID, fetchInteractions: Boolean = false): Sequence {
        return sequenceRepository.findByUuid(uuid)?.let { sequence ->

            if (fetchInteractions) {
                loadInteractions(sequence)
            }

            sequence
        } ?: throw EntityNotFoundException("There is no sequence for uuid \"$uuid\"")
    }

    fun loadInteractions(sequence: Sequence): Sequence {
        interactionRepository.findAllBySequence(sequence).map {
            sequence.interactions[it.interactionType] = it
        }

        return sequence
    }

    fun findAllFakeExplanation(user: User, sequenceId: Long): List<FakeExplanation> {
        return fakeExplanationRepository.findAllByStatement(
            get(user, sequenceId).statement
        )
    }

    fun start(
        user: User,
        sequence: Sequence,
        executionContext: ExecutionContext,
        studentsProvideExplanation: Boolean,
        nbResponseToEvaluate: Int,
        evaluationPhaseConfig: EvaluationPhaseConfig?,
        chatGptEvaluationEnable: Boolean = false
    ): Sequence {

        require(user == sequence.owner) {
            "Only the owner of a sequence is allowed to start it"
        }

        require(sequence.interactions.isEmpty()) {
            "The sequence has already been started"
        }

        initializeInteractionsForSequence(sequence, studentsProvideExplanation, nbResponseToEvaluate, executionContext)

        eventLogService.saveActionsAfterClosingConfigurePopup(sequence)

        if (executionContext == ExecutionContext.FaceToFace)
            sequence.selectActiveInteraction(InteractionType.ResponseSubmission)
        else sequence.selectActiveInteraction(InteractionType.Read)

        sequence.let {
            it.state = State.show
            it.executionContext = executionContext
            it.resultsArePublished = (executionContext == ExecutionContext.Distance)
            it.evaluationPhaseConfig = evaluationPhaseConfig ?: EvaluationPhaseConfig.ALL_AT_ONCE
            it.chatGptEvaluationEnabled = chatGptEvaluationEnable
            sequenceRepository.save(it)
        }
        if (studentsProvideExplanation) {
            responseService.buildResponseBasedOnTeacherExpectedExplanationForASequence(
                sequence = sequence,
                teacher = sequence.owner
            )
            responseService.buildResponsesBasedOnTeacherFakeExplanationsForASequence(sequence)
        }
        return sequence
    }

    internal fun initializeInteractionsForSequence(
        sequence: Sequence,
        studentsProvideExplanation: Boolean,
        nbResponseToEvaluate: Int,
        executionContext: ExecutionContext,
    ): Sequence {
        sequence.interactions[InteractionType.ResponseSubmission] =
            interactionService.create(
                sequence,
                ResponseSubmissionSpecification(
                    studentsProvideExplanation,
                    studentsProvideConfidenceDegree = studentsProvideExplanation
                ),
                1,
                State.show
            )

        sequence.interactions[InteractionType.Evaluation] =
            interactionService.create(
                sequence,
                EvaluationSpecification(
                    nbResponseToEvaluate
                ),
                2,
                if (executionContext == ExecutionContext.FaceToFace)
                    State.beforeStart
                else State.show
            )

        sequence.interactions[InteractionType.Read] =
            interactionService.create(
                sequence,
                ReadSpecification(),
                3,
                when (executionContext) {
                    ExecutionContext.FaceToFace, ExecutionContext.Blended -> State.beforeStart
                    ExecutionContext.Distance -> State.show
                }

            )
        return sequence
    }

    fun stop(user: User, sequence: Sequence): Sequence {
        require(user == sequence.owner) {
            "Only the owner of the sequence is allowed to stop it"
        }

        sequence.let {
            it.state = State.afterStop
            sequenceRepository.save(it)
            eventLogService.stopSequence(sequence)
            return it
        }
    }

    fun reopen(user: User, sequence: Sequence): Sequence {
        require(user == sequence.owner) {
            "Only the owner of the sequence is allowed to reopen it"
        }
        require(sequence.isStopped()) {
            "This sequence is not stopped, it cannot be reopen"
        }

        sequence.let {
            it.state = State.show
            sequenceRepository.save(it)
            eventLogService.reopenSequence(sequence)
            return it
        }
    }

    fun submitResponse(
        user: User,
        sequence: Sequence,
        responseSubmissionData: PlayerController.ResponseSubmissionData
    ): Response {
        val choiceListSpecification = responseSubmissionData.choiceList?.let {
            LearnerChoice(it)
        }

        val userActiveInteraction = getActiveInteractionForLearner(sequence, user)

        val submitedResponse = responseService.save(
            userActiveInteraction
                ?: error("No active interaction, cannot submit a response"), // TODO we should provide a user-friendly error page for this
            Response(
                learner = user,
                interaction = sequence.getResponseSubmissionInteraction(),
                attempt = responseSubmissionData.attempt,
                confidenceDegree = responseSubmissionData.confidenceDegree,
                explanation = responseSubmissionData.explanation,  // TODO Sanitize
                learnerChoice = choiceListSpecification,
                score = choiceListSpecification?.let {
                    Response.computeScore(
                        it,
                        sequence.statement.choiceSpecification
                            ?: error("The choice specification is undefined")
                    )
                },
                statement = sequence.statement
            )
        )
        if (sequence.executionIsDistance() || sequence.executionIsBlended()) {
            nextInteractionForLearner(sequence, user)
        }

        return submitedResponse
    }

    fun refreshResults(user: User, sequence: Sequence): Sequence {
        sequence.let {
            resultsService.updateResults(user, it)
            eventLogService.refreshResults(it)
            return it
        }
    }

    fun publishResults(user: User, sequence: Sequence): Sequence {
        require(user == sequence.owner) {
            "Only the owner of the sequence is allowed to publish results"
        }
        require(sequence.resultsCanBePublished()) {
            "The results of this sequence cannot be published"
        }

        sequence.let {

            resultsService.updateResults(user, it)
            it.resultsArePublished = true

            // Set Read active and close all other interactions
            it.interactions.forEach { type, interaction ->
                interaction.state = when (type) {
                    InteractionType.Read -> State.show
                    else -> State.afterStop
                }
                interactionRepository.save(interaction)
            }
            sequence.activeInteraction = sequence.getReadInteraction()
            eventLogService.publishResults(it)
            sequenceRepository.save(it)
            return it
        }
    }

    fun unpublishResults(user: User, sequence: Sequence): Sequence {
        require(user == sequence.owner) {
            "Only the owner of the sequence is allowed to publish results"
        }
        require(sequence.resultsArePublished) {
            "The results of this sequence are not published"
        }

        sequence.let {
            it.resultsArePublished = false
            sequence.getReadInteraction().state = State.beforeStart
            eventLogService.unpublishResults(it)
            sequenceRepository.save(it)
            return it
        }
    }

    fun getActiveInteractionForLearner(sequence: Sequence, learner: User): Interaction? =
        if (sequence.executionIsFaceToFace())
            sequence.activeInteraction
        else learnerSequenceService.findOrCreateLearnerSequence(learner, sequence).activeInteraction

    fun nextInteractionForLearner(sequence: Sequence, learner: User) {
        learnerSequenceService.findOrCreateLearnerSequence(learner, sequence).let { learnerSequence ->
            learnerSequence.activeInteraction = sequence.getInteractionAt(
                (learnerSequence.activeInteraction
                    ?: error("No active interaction, cannot select the next one")).rank + 1
            )
            learnerSequenceRepository.save(learnerSequence)
        }
    }

    fun getStatistics(sequence: Sequence) = SequenceStatistics(
        if (sequence.isNotStarted()) 0 else responseService.count(sequence, 1),
        if (sequence.isNotStarted()) 0 else responseService.count(
            sequence,
            2
        ), // TODO should only compute this data if phase2 open or done
        if (sequence.isNotStarted()) 0 else peerGradingService.countEvaluations(sequence) // TODO should only compute this data if phase2 open or done
    )

    fun findAllNotTerminatedSequencesByStatement(statement: Statement): List<Sequence> {
        return sequenceRepository.findAllByStatementAndStateNot(statement, State.afterStop)
    }

    fun findAllSequencesByStatement(statement: Statement): List<Sequence> {
        return sequenceRepository.findAllByStatement(statement)
    }

    fun save(sequence: Sequence) {
        sequenceRepository.save(sequence)
    }

    fun existsById(id: Long): Boolean =
        sequenceRepository.existsById(id)

    /**
     * @param sequence [Sequence] to get the information
     * @param teacher [Boolean] to know if the user is a teacher
     * @param isRemoved [Boolean] false if we want the reported evaluation to moderate and false otherwise
     * @return [Int] for the reported evaluation
     * @throws IllegalStateException if the evaluation phase is not initialized
     */
    fun countReportedEvaluation(sequence: Sequence, teacher: Boolean, isRemoved: Boolean): Int {
        if (!teacher || !sequence.responseSubmissionInteractionIsInitialized()) {
            return 0
        }

        val nbDRAXOEvaluationReported: Int = if (sequence.evaluationPhaseConfig == EvaluationPhaseConfig.DRAXO) {
            draxoPeerGradingService.countAllDraxoPeerGradingReported(sequence, isRemoved)
        } else {
            0
        }

        val nbChatGPTEvaluationReported: Int = if (sequence.chatGptEvaluationEnabled) {
            chatGptEvaluationService.countAllReported(sequence, isRemoved)
        } else {
            0
        }

        return nbDRAXOEvaluationReported + nbChatGPTEvaluationReported
    }

    fun countReportBySequence(sequences: List<Sequence>, isTeacher: Boolean): Map<Sequence, ReportInformation> {
        return sequences.associateWith { countReportBySequence(it, isTeacher) }
    }

    fun countReportBySequence(sequence: Sequence, isTeacher: Boolean): ReportInformation {
        // Load interactions if not already loaded
        val sequenceInteractionsFetched = if (sequence.interactions.isEmpty()) {
            loadInteractions(sequence)
        } else {
            sequence
        }
        val nbRemovedReport = countReportedEvaluation(sequenceInteractionsFetched, isTeacher, true)
        val nbNotRemovedReport = countReportedEvaluation(sequenceInteractionsFetched, isTeacher, false)
        return ReportInformation(
                nbReportTotal = nbRemovedReport + nbNotRemovedReport,
                nbReportToModerate = nbNotRemovedReport
        )
    }
}
