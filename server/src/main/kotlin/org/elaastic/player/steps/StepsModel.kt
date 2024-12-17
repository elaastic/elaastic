package org.elaastic.player.steps

import org.elaastic.player.dashboard.DashboardPhaseState
import org.elaastic.player.dashboard.SequenceMonitoringModel

data class StepsModel(
    val responseSubmissionState: PhaseState,
    val evaluationState: PhaseState,
    val readState: PhaseState,
    val showStatistics: Boolean = false,
    val studentsProvideExplanation: Boolean = true,
    val phase2Skipped: Boolean = false
) {
    enum class PhaseState {
        DISABLED,
        ACTIVE,
        COMPLETED;

        /**
         * Since The [StepsModel] use different state phase than the [SequenceMonitoringModel], we need to convert the
         * state.
         *
         * @return the converted state in DashboardPhaseState
         * @see DashboardPhaseState
         */
        fun getDashboardState(): DashboardPhaseState {
            return when (this) {
                DISABLED -> DashboardPhaseState.NOT_STARTED
                ACTIVE -> DashboardPhaseState.IN_PROGRESS
                COMPLETED -> DashboardPhaseState.STOPPED
            }
        }
    }
}