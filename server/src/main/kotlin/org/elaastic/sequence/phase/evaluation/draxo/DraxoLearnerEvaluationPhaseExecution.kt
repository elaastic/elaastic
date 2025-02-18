package org.elaastic.sequence.phase.evaluation.draxo

import org.elaastic.activity.evaluation.peergrading.draxo.DraxoEvaluation
import org.elaastic.activity.response.Response
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.sequence.phase.evaluation.AbstractLearnerEvaluationPhaseExecution
import org.elaastic.activity.response.ResponseData

class DraxoLearnerEvaluationPhaseExecution(
    userHasCompletedPhase2: Boolean,
    secondAttemptAlreadySubmitted: Boolean,
    val nextResponseToGrade: ResponseData?,
    val lastResponseToGrade: Boolean,
    sequence: Sequence,
    userActiveInteraction: Interaction?,
    lastAttemptResponse: Response?,
    val draxoEvaluation: DraxoEvaluation = DraxoEvaluation()
) : AbstractLearnerEvaluationPhaseExecution(
    userHasCompletedPhase2,
    secondAttemptAlreadySubmitted,
    sequence,
    userActiveInteraction,
    lastAttemptResponse
) {
}