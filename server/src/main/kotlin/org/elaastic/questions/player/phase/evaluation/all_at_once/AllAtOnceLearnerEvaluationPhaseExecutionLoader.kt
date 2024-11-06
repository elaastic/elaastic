package org.elaastic.questions.player.phase.evaluation.all_at_once

import org.elaastic.questions.assignment.sequence.interaction.response.ResponseService
import org.elaastic.questions.assignment.sequence.peergrading.PeerGradingService
import org.elaastic.questions.player.phase.LearnerPhaseExecution
import org.elaastic.questions.player.phase.LearnerPhase
import org.elaastic.questions.player.phase.evaluation.AbstractLearnerEvaluationPhaseExecutionLoader
import org.elaastic.questions.player.phase.evaluation.ResponseData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("AllAtOnceLearnerEvaluationPhaseExecutionLoader")
class AllAtOnceLearnerEvaluationPhaseExecutionLoader(
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val responseService: ResponseService,
) : AbstractLearnerEvaluationPhaseExecutionLoader() {

    override fun build(learnerPhase: LearnerPhase): LearnerPhaseExecution = run {
        require(learnerPhase is AllAtOnceLearnerEvaluationPhase) {
            "LearnerResponsePhaseExecutionService only handle AllAtOnceLearnerEvaluationPhase interaction ; provided: ${learnerPhase.javaClass}"
        }

        val sequence = learnerPhase.learnerSequence.sequence
        val learner = learnerPhase.learnerSequence.learner

        val userHasPerformedEvaluation =  peerGradingService.userHasPerformedEvaluation(learner, sequence)

        val secondAttemptAlreadySubmitted = responseService.hasResponseForUser(learner, sequence, 2)

        val responsesToGrade = if (!userHasPerformedEvaluation)
            responseService.findAllRecommandedResponsesForUser(
                sequence = sequence,
                attempt = sequence.whichAttemptEvaluate(),
                user = learner
            ).map { ResponseData(it) }
        else listOf()

        AllAtOnceLearnerEvaluationPhaseExecution(
            userHasCompletedPhase2 = (responsesToGrade.isEmpty() &&
                    (secondAttemptAlreadySubmitted || !sequence.isSecondAttemptAllowed())),
            userHasPerformedEvaluation = userHasPerformedEvaluation,
            secondAttemptAlreadySubmitted = secondAttemptAlreadySubmitted,
            responsesToGrade = responsesToGrade,
            sequence = learnerPhase.learnerSequence.sequence,
            userActiveInteraction = learnerPhase.learnerSequence.activeInteraction,
            firstAttemptResponse = responseService.find(learner, sequence),
        )
    }


}