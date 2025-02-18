package org.elaastic.sequence.phase.evaluation.draxo

import org.elaastic.activity.evaluation.peergrading.draxo.DraxoEvaluation
import org.elaastic.sequence.State
import org.elaastic.sequence.phase.evaluation.AbstractLearnerEvaluationPhaseViewModel
import org.elaastic.activity.response.ResponseData
import org.elaastic.sequence.phase.response.LearnerResponseFormViewModel

class DraxoLearnerEvaluationPhaseViewModel(
    sequenceId: Long,
    interactionId: Long,
    phaseState: State,
    choices: Boolean,
    userHasCompletedPhase2: Boolean,
    val nextResponseToGrade: ResponseData?,
    val lastResponseToGrade: Boolean,
    secondAttemptAllowed: Boolean,
    secondAttemptAlreadySubmitted: Boolean,
    responseFormModel: LearnerResponseFormViewModel,
    val draxoEvaluation: DraxoEvaluation = DraxoEvaluation(),
) : AbstractLearnerEvaluationPhaseViewModel(
    sequenceId,
    interactionId,
    phaseState,
    choices,
    userHasCompletedPhase2,
    secondAttemptAllowed,
    secondAttemptAlreadySubmitted,
    responseFormModel
)