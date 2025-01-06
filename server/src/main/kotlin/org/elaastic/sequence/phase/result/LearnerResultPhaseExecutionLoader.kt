package org.elaastic.sequence.phase.result

import org.elaastic.activity.response.ResponseService
import org.elaastic.activity.results.ResultsService
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.common.web.MessageBuilder
import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.player.evaluation.chatgpt.ChatGptEvaluationModelFactory
import org.elaastic.player.results.learner.LearnerResultsModelFactory
import org.elaastic.player.results.learner.LearnerSequenceResponses
import org.elaastic.sequence.phase.LearnerPhase
import org.elaastic.sequence.phase.LearnerPhaseExecution
import org.elaastic.sequence.phase.LearnerPhaseExecutionLoader
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.togglz.core.manager.FeatureManager

@Service("LearnerResultPhaseExecutionLoader")
class LearnerResultPhaseExecutionLoader(
    @Autowired val resultsService: ResultsService,
    @Autowired val responseService: ResponseService,
    @Autowired val featureManager: FeatureManager,
    @Autowired val messageBuilder: MessageBuilder,
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService
) : LearnerPhaseExecutionLoader {

    override fun build(learnerPhase: LearnerPhase): LearnerPhaseExecution = run {
        val firstResponse = responseService.find(
            learnerPhase.learnerSequence.learner,
            learnerPhase.learnerSequence.sequence
        )

        val secondResponse = responseService.find(
            learnerPhase.learnerSequence.learner,
            learnerPhase.learnerSequence.sequence,
            2
        )

        val chatGptEvaluationResponseStore = chatGptEvaluationService.associateResponseToChatGPTEvaluationExistence(
            listOf(
                firstResponse?.id,
                secondResponse?.id
            )
        )


        // Get the "My results" data for the learner
        val myResultsModel = when (learnerPhase.learnerSequence.sequence.statement.questionType) {
            QuestionType.OpenEnded ->
                LearnerResultsModelFactory.buildOpenResult(
                    LearnerSequenceResponses(
                        firstResponse,
                        secondResponse,
                        chatGptEvaluationResponseStore,
                    )
                )

            QuestionType.ExclusiveChoice ->
                LearnerResultsModelFactory.buildExclusiveChoiceResult(
                    LearnerSequenceResponses(
                        firstResponse,
                        secondResponse,
                        chatGptEvaluationResponseStore,
                    ),
                    learnerPhase.learnerSequence.sequence.statement
                )

            QuestionType.MultipleChoice ->
                LearnerResultsModelFactory.buildMultipleChoiceResult(
                    LearnerSequenceResponses(
                        firstResponse,
                        secondResponse,
                        chatGptEvaluationResponseStore,
                    ),
                    learnerPhase.learnerSequence.sequence.statement
                )
        }

        // If both responses are null, then the chatGptEvaluation will be null
        val chatGptEvaluation = listOfNotNull(secondResponse, firstResponse)
            .firstOrNull()
            ?.let { chatGptEvaluationService.findEvaluationByResponse(it) }

        val myChatGptEvaluationModel = if (learnerPhase.learnerSequence.sequence.chatGptEvaluationEnabled) {
            ChatGptEvaluationModelFactory.build(chatGptEvaluation, learnerPhase.learnerSequence.sequence)
        } else null

        LearnerResultPhaseExecution(
            responseSet = responseService.findAll(learnerPhase.learnerSequence.sequence, excludeFakes = false),
            userCanRefreshResults = resultsService.canUpdateResults(
                learnerPhase.learnerSequence.learner,
                learnerPhase.learnerSequence.sequence
            ),
            myResultsModel = myResultsModel,
            featureManager = featureManager,
            messageBuilder = messageBuilder,
            myChatGptEvaluationModel = myChatGptEvaluationModel,
        )
    }
}