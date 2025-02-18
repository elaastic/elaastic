package org.elaastic.sequence.phase.evaluation.draxo

import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.ResponseService
import org.elaastic.sequence.phase.LearnerPhase
import org.elaastic.sequence.phase.LearnerPhaseExecution
import org.elaastic.sequence.phase.evaluation.AbstractLearnerEvaluationPhaseExecutionLoader
import org.elaastic.activity.response.ResponseDataFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("DraxoLearnerEvaluationPhaseExecutionLoader")
class DraxoLearnerEvaluationPhaseExecutionLoader(
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val responseService: ResponseService,
) : AbstractLearnerEvaluationPhaseExecutionLoader() {

    override fun build(learnerPhase: LearnerPhase): LearnerPhaseExecution = run {
        require(learnerPhase is DraxoLearnerEvaluationPhase) {
            "LearnerResponsePhaseExecutionService only handle DraxoLearnerEvaluationPhase interaction ; provided: ${learnerPhase.javaClass}"
        }

        val sequence = learnerPhase.learnerSequence.sequence
        val learner = learnerPhase.learnerSequence.learner

        val secondAttemptAlreadySubmitted = responseService.hasResponseForUser(learner, sequence, 2)

        val responseToEvaluateCount = sequence.getEvaluationSpecification().responseToEvaluateCount

        val responseIdAlreadyGradedList =
            peerGradingService.findAllEvaluation(learner, sequence).map { it.response.id!! }

        val nextResponseToGrade =
            if (responseIdAlreadyGradedList.size < responseToEvaluateCount) {
                responseService.findNextResponseToGrade(
                    sequence = sequence,
                    attempt = sequence.whichAttemptEvaluate(),
                    user = learner,
                    excludedIds = responseIdAlreadyGradedList,
                )?.let { ResponseDataFactory.build(it) }
            } else null

        return DraxoLearnerEvaluationPhaseExecution(
            userHasCompletedPhase2 = nextResponseToGrade == null,
            secondAttemptAlreadySubmitted = secondAttemptAlreadySubmitted,
            nextResponseToGrade = nextResponseToGrade,
            lastResponseToGrade = responseIdAlreadyGradedList.size == responseToEvaluateCount - 1,
            sequence = learnerPhase.learnerSequence.sequence,
            userActiveInteraction = learnerPhase.learnerSequence.activeInteraction,
            lastAttemptResponse = responseService.find(learner, sequence, 2) ?: responseService.find(
                learner,
                sequence,
                1
            ),
        )
    }


}