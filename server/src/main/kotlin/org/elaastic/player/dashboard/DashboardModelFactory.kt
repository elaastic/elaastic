/*
 *
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
 *
 */

package org.elaastic.player.dashboard

import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseService
import org.elaastic.assignment.AssignmentService
import org.elaastic.assignment.LearnerAssignment
import org.elaastic.player.steps.StepsModel
import org.elaastic.player.steps.StepsModelFactory
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.config.EvaluationSpecification
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.interaction.InteractionType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * Factory to build the [DashboardModel].
 *
 * @see DashboardModel
 */
@Component
class DashboardModelFactory(
    @Autowired val sequenceService: SequenceService,
    @Autowired val assignmentService: AssignmentService,
    @Autowired val interactionService: InteractionService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val responseService: ResponseService
) {
    /**
     * Build the [DashboardModel] for the given sequence.
     *
     * @param sequence the sequence
     * @return the [DashboardModel]
     */
    fun build(
        sequence: Sequence,
    ): DashboardModel {
        val learnerStepsModel: StepsModel = StepsModelFactory.buildForTeacher(sequence)

        val sequenceMonitoringModel = SequenceMonitoringModel(
            sequence.executionContext,
            learnerStepsModel.responseSubmissionState.getDashboardState(),
            learnerStepsModel.evaluationState.getDashboardState(),
            sequenceId = sequence.id
        )

        val learners: MutableList<LearnerMonitoringModel> = getLearnerMonitoringModels(
            sequenceMonitoringModel,
            sequence,
        )
        sequenceMonitoringModel.setLearners(learners)

        val previousSequence: Sequence? = sequenceService.findPreviousSequence(sequence)
        val nextSequence: Sequence? = sequenceService.findNextSequence(sequence)

        return DashboardModel(
            sequence,
            previousSequence?.id,
            nextSequence?.id,
            sequenceMonitoringModel
        )
    }

    private fun getLearnerMonitoringModels(
        sequenceMonitoringModel: SequenceMonitoringModel,
        sequence: Sequence,
    ): MutableList<LearnerMonitoringModel> {
        val attendees: List<LearnerAssignment> = assignmentService.getRegisteredUsers(sequence.assignment!!)
        val responses: List<Response> = interactionService.findAllResponsesBySequenceOrderById(sequence)

        // Associate each learner with the number of evaluations he made
        val evaluationCountByUser = peerGradingService.countEvaluationsMadeByUsers(attendees, sequence)
        val countResponseGradable = getCountResponseGradable(sequence)

        val learners: MutableList<LearnerMonitoringModel> = mutableListOf()

        attendees.forEach {
            learners.add(
                buildLearnerMonitoringModel(
                    it,
                    learnerHasAnswer(responses, it),
                    sequenceMonitoringModel,
                    sequence,
                    evaluationCountByUser[it] ?: 0,
                    countResponseGradable
                )
            )
        }
        return learners
    }

    /**
     * Get the number of responses that are gradable.
     *
     * @param sequence the sequence
     * @return the number of responses that are gradable
     */
    fun getCountResponseGradable(sequence: Sequence): Long {
        return if (sequence.getResponseSubmissionInteractionOrNull() == null) {
            // If the sequence hasn't started yet, the ResponseSubmission interaction would be null
            // If the sequence hasn't started yet, no response has been produce,
            // so no response is gradable
            0
        } else {
            val responseStudent = responseService.findAllByAttemptNotFake(1, sequence).size.toLong()
            val responseFake = responseService.findAllFakeResponses(sequence).size.toLong()
            responseStudent + responseFake
        }
    }

    fun buildLearnerMonitoringModel(
        learnerAssignment: LearnerAssignment,
        learnerHasAnswered: Boolean,
        sequenceMonitoringModel: SequenceMonitoringModel,
        sequence: Sequence,
        nbEvaluationMade: Long,
        countResponseGradable: Long
    ) = LearnerMonitoringModel(
        learnerAssignment.learner.id!!,
        learnerAssignment.learner.getFullname(),
        getAttendeeStateOnResponsePhase(learnerHasAnswered, sequenceMonitoringModel.phase1State),
        getAttendeeStateOnEvaluationPhase(
            sequence,
            sequenceMonitoringModel.phase2State,
            nbEvaluationMade,
            learnerHasAnswered,
            countResponseGradable,
        ),
        sequenceMonitoringModel = sequenceMonitoringModel
    )

    /**
     * Get the LearnerStateOnPhase of the attendee on the Response phase.
     *
     * @param learnerHasAnswered if the learner has answered the question
     * @param responsePhaseState the state of the response phase
     * @return the state of the attendee on the response phase
     * @see LearnerStateOnPhase
     */
    private fun getAttendeeStateOnResponsePhase(
        learnerHasAnswered: Boolean,
        responsePhaseState: DashboardPhaseState
    ): LearnerStateOnPhase {
        return if (responsePhaseState == DashboardPhaseState.NOT_STARTED) {
            LearnerStateOnPhase.WAITING
        } else if (learnerHasAnswered) {
            LearnerStateOnPhase.ACTIVITY_TERMINATED
        } else {
            LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED
        }
    }

    /**
     * Get the LearnerStateOnPhase of the attendee on the Evaluation phase.
     *
     * A learner has finished the phase if he has evaluated exactly the number of responses required.
     *
     * @param sequence the sequence
     * @param evaluationPhaseState the state of the evaluation phase
     * @param nbEvaluationMade the number of evaluations made by the user
     * @param learnerHasAnswered if the learner has answered the question
     * @return the LearnerStateOnPhase base on the argument
     * @see EvaluationSpecification
     */
    private fun getAttendeeStateOnEvaluationPhase(
        sequence: Sequence,
        evaluationPhaseState: DashboardPhaseState,
        nbEvaluationMade: Long,
        learnerHasAnswered: Boolean,
        countResponseGradable: Long
    ): LearnerStateOnPhase {
        return if (evaluationPhaseState == DashboardPhaseState.NOT_STARTED) {
            LearnerStateOnPhase.WAITING
        } else if (evaluationHaveBeenFinished(
                sequence,
                nbEvaluationMade,
                learnerHasAnswered,
                countResponseGradable
            )
        ) {
            LearnerStateOnPhase.ACTIVITY_TERMINATED
        } else {
            LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED
        }
    }

    /**
     * Check if all the evaluations have been made for the sequence.
     *
     * Compare the number of evaluations made by the user with the number of evaluations to make specified in the
     * sequence.
     *
     * @param sequence the sequence
     * @param nbEvaluationMade the number of evaluations made by the user
     * @param learnerHaveAnswered if the learner has answered the question
     * @param countResponseGradable the number of responses available for evaluation
     * @return true if all the evaluations have been made, false otherwise
     * @see EvaluationSpecification
     */
    fun evaluationHaveBeenFinished(
        sequence: Sequence,
        nbEvaluationMade: Long?,
        learnerHaveAnswered: Boolean?,
        countResponseGradable: Long
    ): Boolean = try {
        sequence.getEvaluationSpecification().responseToEvaluateCount.toLong() == nbEvaluationMade
                // If the learner has answered, we need to remove one to total of responses gradable.
                // Because the learner can't evaluate his own response.
                || nbEvaluationMade == (countResponseGradable - (if (learnerHaveAnswered == true) 1 else 0))
    } catch (e: IllegalStateException) {
        false /* If the sequence isn't initialized an Exception his throw by the getEvaluationSpecification function */
    }

    /**
     * Check if among the responses of the sequence, one of them is from the learner and is the first attempt.
     *
     * @param attendeeResponses the list of responses of the attendee
     * @param attendee the attendee
     * @return true if the learner has answered the question, false otherwise
     */
    fun learnerHasAnswer(attendeeResponses: List<Response>, attendee: LearnerAssignment) = attendeeResponses.count {
        it.attempt == 1
                && it.interaction.interactionType == InteractionType.ResponseSubmission
                && it.learner == attendee.learner
    } == 1
}