package org.elaastic.player.dashboard

import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.phase.LearnerPhaseType

/**
 * Model for the learners monitoring dashboard.
 *
 * The model is used to display the state of the learners in the dashboard.
 * It contains the following information:
 * - The execution context of the sequence
 * - The state of phase 1
 * - The state of phase 2
 * - The list of [LearnerMonitoringModel]
 *
 * @see DashboardPhaseState
 * @property executionContext Sequence execution context type
 * @property phase1State the state of phase 1
 * @property phase2State the state of phase 2
 * @property learners the learners' states on each phase
 * @property sequenceId the id of the sequence
 */
class SequenceMonitoringModel(
    val executionContext: ExecutionContext,
    val phase1State: DashboardPhaseState,
    val phase2State: DashboardPhaseState,
    val learners: MutableList<LearnerMonitoringModel> = mutableListOf(),
    val sequenceId: Long? = null
) {

    init {
        // Check coherence of the model
        when (executionContext) {
            ExecutionContext.FaceToFace -> {
                if (phase1State == DashboardPhaseState.IN_PROGRESS) require(phase2State == DashboardPhaseState.NOT_STARTED) { "In FaceToFace mode phase 2 must be not started when phase 1 is started" }
                if (phase2State == DashboardPhaseState.IN_PROGRESS) require(phase1State == DashboardPhaseState.STOPPED) { "In FaceToFace mode phase 1 must be completed when phase 2 is started" }
            }

            else -> require(phase1State == phase2State) { "In Remote mode phase 1 and 2 must have the same state" }
        }
    }

    /**
     * With the given list of learners, set the list of learners in the model.
     *
     * Replace the current list of learners with the new list. Before setting
     * the new list, sort the learners according to the execution context and
     * the current phase.
     *
     * @param newLearnersList the new list of learners
     * @see ExecutionContext
     * @see sortWithFaceToFaceBehavior
     * @see sortWithBlendedOrRemoteBehavior
     */
    fun setLearners(newLearnersList: MutableList<LearnerMonitoringModel>) {
        learners.clear()

        val newLearnersListSorted: MutableList<LearnerMonitoringModel> = when (this.executionContext) {
            ExecutionContext.FaceToFace -> this.sortWithFaceToFaceBehavior(newLearnersList)
            else -> this.sortWithBlendedOrRemoteBehavior(newLearnersList)
        }

        learners.addAll(newLearnersListSorted)
    }

    /**
     * Sort the learner when the sequence is in FaceToFace execution context
     *
     * If the Phase 1 is active, then we want the learner still writing their
     * answer at the top.
     *
     * If the Phase 2 is active, then we want the learner still evaluating at
     * first and more than the learner who didn't answer the question.
     *
     * If the Phase 3 is active, then we want the learner who didn't answer and
     * evaluate at first.
     */
    private fun sortWithFaceToFaceBehavior(
        newLearnersList: MutableList<LearnerMonitoringModel>
    ): MutableList<LearnerMonitoringModel> {

        newLearnersList.sortBy { it.learnerName }

        if (this.phase1State == DashboardPhaseState.IN_PROGRESS) {
            newLearnersList.sortByDescending { it.getLevelByStateCell(LearnerMonitoringModel.StateCell.IN_PROGRESS) }
        }

        if (this.phase2State == DashboardPhaseState.IN_PROGRESS) {
            newLearnersList.sortByDescending { it.getLevelByStateCell(LearnerMonitoringModel.StateCell.IN_PROGRESS) }
            newLearnersList.sortByDescending { it.getLevelByStateCell(LearnerMonitoringModel.StateCell.NOT_TERMINATED) }
            newLearnersList.sortByDescending { it.getStateCell(LearnerPhaseType.EVALUATION) == LearnerMonitoringModel.StateCell.IN_PROGRESS }
        } else {
            newLearnersList.sortByDescending { it.getStateCell(LearnerPhaseType.RESPONSE) == LearnerMonitoringModel.StateCell.NOT_TERMINATED }
            newLearnersList.sortByDescending { it.getLevelByStateCell(LearnerMonitoringModel.StateCell.NOT_TERMINATED) }
        }

        return newLearnersList
    }

    /**
     * Sort the learners when the sequence has Blended or Remote execution
     * context.
     *
     * Sort the learners alphabetically and by their "In Progress..." states
     * count.
     *
     * @param newLearnersList the list of learners to sort
     */
    private fun sortWithBlendedOrRemoteBehavior(
        newLearnersList: MutableList<LearnerMonitoringModel>
    ): MutableList<LearnerMonitoringModel> {
        // Sorted By name alphabetically
        newLearnersList.sortBy { it.learnerName }
        // Sort by number of states in progress descending
        newLearnersList.sortByDescending { it.getLevelByStateCell(LearnerMonitoringModel.StateCell.IN_PROGRESS) }

        return newLearnersList
    }
}

/**
 * Model for a learner's state on each phase.
 *
 * Represents a learner's state in each phase, a line in the table.
 * The model contains the following information:
 * - The id of the learner
 * - The name of the learner
 * - The [LearnerStateOnPhase] of the learner on phase 1
 * - The [LearnerStateOnPhase] of the learner on phase 2
 *
 * @property userId the user id of the learner
 * @property learnerName the learner's name
 * @property learnerStateOnPhase1 the learner's state on phase 1
 * @property learnerStateOnPhase2 the learner's state on phase 2
 * @property learnerStateOnPhase3 the learner's state on phase 3
 */
class LearnerMonitoringModel(
    val userId: Long,
    val learnerName: String,
    val learnerStateOnPhase1: LearnerStateOnPhase,
    val learnerStateOnPhase2: LearnerStateOnPhase = LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
    private val learnerStateOnPhase3: LearnerStateOnPhase = LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED,
    val sequenceMonitoringModel: SequenceMonitoringModel
) {

    /**
     * Return the StateCell from a given phase type and the learner's state
     *
     * @param phase the type of the phase
     * @return the StateCell of the phase
     * @see StateCell
     * @see LearnerPhaseType
     * @see LearnerStateOnPhase
     */
    fun getStateCell(phase: LearnerPhaseType): StateCell {
        val phaseState: DashboardPhaseState = this.getPhaseStateByType(phase)

        return when (this.getLearnerPhaseStateByType(phase)) {
            LearnerStateOnPhase.ACTIVITY_NOT_TERMINATED -> {
                when (phaseState) {
                    DashboardPhaseState.IN_PROGRESS -> {
                        StateCell.IN_PROGRESS
                    }

                    DashboardPhaseState.NOT_STARTED -> {
                        StateCell.LOCKED
                    }

                    else -> {
                        StateCell.NOT_TERMINATED
                    }
                }
            }

            LearnerStateOnPhase.ACTIVITY_TERMINATED -> StateCell.TERMINATED
            LearnerStateOnPhase.WAITING -> {
                if (phaseState == DashboardPhaseState.IN_PROGRESS) {
                    StateCell.IN_PROGRESS
                } else {
                    StateCell.LOCKED
                }
            }
        }
    }

    /**
     * Return the StateCell of the learner in the response phase
     *
     * Function used in the thymeleaf template
     *
     * @return the StateCell of the learner in the response phase
     * @see StateCell
     */
    fun getStateCellInResponsePhase(): StateCell {
        return getStateCell(LearnerPhaseType.RESPONSE)
    }

    /**
     * Return the StateCell of the learner in the evaluation phase
     *
     * Function used in the thymeleaf template
     *
     * @return the StateCell of the learner in the evaluation phase
     * @see StateCell
     */
    fun getStateCellInEvaluationPhase(): StateCell {
        return getStateCell(LearnerPhaseType.EVALUATION)
    }

    /**
     * Return the LearnerStateOnPhase from the type of the phase
     *
     * @param phase The type of the phase
     * @see LearnerPhaseType
     */
    private fun getLearnerPhaseStateByType(phase: LearnerPhaseType): LearnerStateOnPhase {
        return when (phase) {
            LearnerPhaseType.RESPONSE -> this.learnerStateOnPhase1
            LearnerPhaseType.EVALUATION -> this.learnerStateOnPhase2
            LearnerPhaseType.RESULT -> this.learnerStateOnPhase3
        }
    }

    /**
     * Return the PhaseState from the type of the phase
     *
     * @param phase The type of the phase
     * @see LearnerPhaseType
     */
    private fun getPhaseStateByType(phase: LearnerPhaseType): DashboardPhaseState {
        return when (phase) {
            LearnerPhaseType.RESPONSE -> this.sequenceMonitoringModel.phase1State
            else -> this.sequenceMonitoringModel.phase2State
        }
    }

    /**
     * @param stateCell the state of the cell we want to count
     * @return the number of states that is given
     * @see StateCell
     */
    fun getLevelByStateCell(stateCell: StateCell): Int {
        val states: List<StateCell> = listOf(
            this.getStateCell(LearnerPhaseType.RESPONSE),
            this.getStateCell(LearnerPhaseType.EVALUATION),
        )

        return states.count { it == stateCell }
    }

    /**
     * Enum defining states of a cell for a learner
     *
     * @property LOCKED the learner can't access the phase
     * @property IN_PROGRESS the learner is currently working on the phase
     * @property NOT_TERMINATED the learner has not terminated the phase
     * @property TERMINATED the learner has terminated the phase
     */
    enum class StateCell {
        LOCKED,
        IN_PROGRESS,
        NOT_TERMINATED,
        TERMINATED
    }

    fun hasAnswered(): Boolean {
        return this.learnerStateOnPhase1 == LearnerStateOnPhase.ACTIVITY_TERMINATED
    }
}

/**
 * Enum defining states of a phase
 *
 * @property NOT_STARTED the phase has not started
 * @property IN_PROGRESS the phase is in progress
 * @property STOPPED the phase has been stopped
 * @property COMPLETED the phase has been completed
 */
enum class DashboardPhaseState {
    /**
     * The phase has not started
     *
     * Applicable to all phases. During phase 3 (evaluation), the phase is not
     * started when the results are not displayed yet
     */
    NOT_STARTED,

    /**
     * The phase is in progress
     *
     * Applicable to all phases. During phase 3 (evaluation), the phase is in
     * progress when the results are being displayed
     */
    IN_PROGRESS,

    /**
     * The phase has been stopped
     *
     * Only applicable to phase 1 and 2.
     */
    STOPPED,                                // UNUSED

    /**
     * The phase has been completed
     *
     * Applicable to all phases.
     */
    COMPLETED
}

/**
 * Enum defining states of a learner on a phase
 *
 * @property ACTIVITY_NOT_TERMINATED the learner has not terminated the
 *     activity
 * @property ACTIVITY_TERMINATED the learner has terminated the activity
 * @property WAITING the learner is waiting for the next phase
 */
enum class LearnerStateOnPhase {
    /** The learner has not terminated the activity. */
    ACTIVITY_NOT_TERMINATED,

    /** The learner has terminated the activity. */
    ACTIVITY_TERMINATED,

    /** The learner is waiting for the next phase. */
    WAITING
}
