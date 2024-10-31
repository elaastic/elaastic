package org.elaastic.questions.player.phase.evaluation

import org.elaastic.questions.assignment.sequence.Sequence
import org.elaastic.questions.assignment.sequence.interaction.Interaction
import org.elaastic.questions.assignment.sequence.interaction.response.Response
import org.elaastic.questions.player.phase.LearnerPhaseExecution

abstract class AbstractLearnerEvaluationPhaseExecution(
    val userHasCompletedPhase2: Boolean,
    val secondAttemptAlreadySubmitted: Boolean,
    val sequence: Sequence,
    val userActiveInteraction: Interaction?,
    val lastAttemptResponse: Response?
) : LearnerPhaseExecution {
}