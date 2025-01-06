package org.elaastic.sequence.phase.result

import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationResponseStore
import org.elaastic.player.results.ResultsModelFactory
import org.elaastic.sequence.ILearnerSequence
import org.elaastic.sequence.State
import org.elaastic.sequence.phase.LearnerPhase
import org.elaastic.sequence.phase.LearnerPhaseExecution
import org.elaastic.sequence.phase.LearnerPhaseType
import org.elaastic.sequence.phase.PhaseTemplate

class LearnerResultPhase(
    learnerSequence: ILearnerSequence,
    index: Int,
    active: Boolean,
    state: State,
) : LearnerPhase(
    learnerSequence,
    index,
    active,
    state,
    PhaseTemplate(
        "${PhaseTemplate.TEMPLATE_PACKAGE}/result/_learner-result-phase.html",
        "resultPhase"
    )
) {
    override val phaseType = LearnerPhaseType.RESULT
    override var learnerPhaseExecution: LearnerResultPhaseExecution? = null

    override fun loadPhaseExecution(learnerPhaseExecution: LearnerPhaseExecution) {
        if (learnerPhaseExecution is LearnerResultPhaseExecution)
            this.learnerPhaseExecution = learnerPhaseExecution
        else throw IllegalArgumentException()
    }

    override fun getViewModel(): LearnerResultPhaseViewModel {

        // TODO : Fill with real data
        val explanationHasChatGPTEvaluationMap = ChatGptEvaluationResponseStore()


        return LearnerResultPhaseViewModel(
            learnerSequence.sequence.resultsArePublished,
            learnerPhaseExecution!!.myResultsModel,
            ResultsModelFactory.build(
                teacher = false,
                sequence = this.learnerSequence.sequence,
                responseSet = learnerPhaseExecution!!.responseSet,
                userCanRefreshResults = learnerPhaseExecution!!.userCanRefreshResults,
                featureManager = learnerPhaseExecution!!.featureManager,
                messageBuilder = learnerPhaseExecution!!.messageBuilder,
                chatGptEvaluationResponseStore = explanationHasChatGPTEvaluationMap
            ),
            learnerPhaseExecution!!.myChatGptEvaluationModel,
        )
    }

    // Note JT : results are displayed even when the sequence is closed
    override fun isVisible() = this.active

}