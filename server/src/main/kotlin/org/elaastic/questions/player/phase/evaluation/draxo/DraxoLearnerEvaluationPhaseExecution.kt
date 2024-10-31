package org.elaastic.questions.player.phase.evaluation.draxo

import org.elaastic.questions.assignment.sequence.Sequence
import org.elaastic.questions.assignment.sequence.interaction.Interaction
import org.elaastic.questions.assignment.sequence.interaction.response.Response
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoEvaluation
import org.elaastic.questions.player.phase.evaluation.AbstractLearnerEvaluationPhaseExecution
import org.elaastic.questions.player.phase.evaluation.ResponseData

class DraxoLearnerEvaluationPhaseExecution(
    userHasCompletedPhase2: Boolean,
    secondAttemptAlreadySubmitted: Boolean,
    val nextResponseToGrade: ResponseData?,
    val lastResponseToGrade: Boolean,
    sequence: Sequence,
    userActiveInteraction: Interaction?,
    lastAttemptResponse: Response?,
    val draxoEvaluation: DraxoEvaluation = DraxoEvaluation()
): AbstractLearnerEvaluationPhaseExecution(
    userHasCompletedPhase2,
    secondAttemptAlreadySubmitted,
    sequence,
    userActiveInteraction,
    lastAttemptResponse
) {
}