package org.elaastic.sequence.phase.evaluation.all_at_once

import org.elaastic.sequence.State
import org.elaastic.sequence.phase.evaluation.AbstractLearnerEvaluationPhaseViewModel
import org.elaastic.activity.response.ResponseData
import org.elaastic.sequence.phase.response.LearnerResponseFormViewModel

class AllAtOnceLearnerEvaluationPhaseViewModel(
    sequenceId: Long,
    interactionId: Long,
    phaseState: State,
    choices: Boolean,
    userHasCompletedPhase2: Boolean,
    val userHasPerformedEvaluation: Boolean,
    val responsesToGrade: List<ResponseData>,
    secondAttemptAllowed: Boolean,
    secondAttemptAlreadySubmitted: Boolean,
    responseFormModel: LearnerResponseFormViewModel
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