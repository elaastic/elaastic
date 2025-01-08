package org.elaastic.player.results.learner

import org.elaastic.activity.response.Response
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationResponseStore
import org.elaastic.material.instructional.question.ExclusiveChoiceSpecification
import org.elaastic.material.instructional.question.MultipleChoiceSpecification
import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.player.explanations.ExplanationDataFactory

object LearnerResultsModelFactory {

    /**
     * Get the LearnerResultsModel for the given responses and statement
     *
     * @param responseFirstAttempt the response of the learner for the first attempt
     * @param responseSecondAttempt the response of the learner for the second attempt
     * @param statement the statement of the sequence
     */
    fun builtLearnerResultsModel(
        learnerSequenceResponses: LearnerSequenceResponses,
        statement: Statement
    ): LearnerResultsModel {
        val learnerResultsModel = when (statement.questionType) {
            QuestionType.ExclusiveChoice -> buildExclusiveChoiceResult(
                learnerSequenceResponses,
                statement
            )

            QuestionType.MultipleChoice -> buildMultipleChoiceResult(
                learnerSequenceResponses,
                statement
            )

            QuestionType.OpenEnded -> buildOpenResult(
                learnerSequenceResponses,
            )
        }
        return learnerResultsModel
    }

    fun buildOpenResult(
        learnerSequenceResponses: LearnerSequenceResponses
    ): LearnerOpenResults = with(learnerSequenceResponses) {
        LearnerOpenResults(
            explanationFirstTry = if (responseFirstAttempt != null) ExplanationDataFactory.create(
                responseFirstAttempt,
                responseFirstTryHasChatGPTEvaluation
            ) else null,
            explanationSecondTry = if (responseSecondAttempt != null) ExplanationDataFactory.create(
                responseSecondAttempt,
                responseSecondTryHasChatGPTEvaluation
            ) else null
        )
    }

    fun buildMultipleChoiceResult(
        learnerSequenceResponses: LearnerSequenceResponses,
        statement: Statement
    ): LearnerMultipleChoiceResults = with(learnerSequenceResponses) {
        LearnerMultipleChoiceResults(
            explanationFirstTry = if (responseFirstAttempt != null) ExplanationDataFactory.create(
                responseFirstAttempt,
                responseFirstTryHasChatGPTEvaluation
            ) else null,
            explanationSecondTry = if (responseSecondAttempt != null) ExplanationDataFactory.create(
                responseSecondAttempt,
                responseSecondTryHasChatGPTEvaluation
            ) else null,
            choiceFirstTry = responseFirstAttempt?.learnerChoice,
            choiceSecondTry = responseSecondAttempt?.learnerChoice,
            scoreFirstTry = responseFirstAttempt?.score?.intValueExact(),
            scoreSecondTry = responseSecondAttempt?.score?.intValueExact(),
            expectedChoice = MultipleChoiceSpecification(
                nbCandidateItem = statement.choiceSpecification?.toLegacy()?.itemCount!!,
                expectedChoiceList = statement.choiceSpecification?.toLegacy()?.expectedChoiceList!!
            )
        )
    }

    fun buildExclusiveChoiceResult(
        learnerSequenceResponses: LearnerSequenceResponses,
        statement: Statement
    ): LearnerExclusiveChoiceResults = with(learnerSequenceResponses) {
        LearnerExclusiveChoiceResults(
            explanationFirstTry = if (responseFirstAttempt != null) ExplanationDataFactory.create(
                responseFirstAttempt,
                responseFirstTryHasChatGPTEvaluation
            ) else null,
            explanationSecondTry = if (responseSecondAttempt != null) ExplanationDataFactory.create(
                responseSecondAttempt,
                responseSecondTryHasChatGPTEvaluation
            ) else null,
            choiceFirstTry = responseFirstAttempt?.learnerChoice,
            choiceSecondTry = responseSecondAttempt?.learnerChoice,
            scoreFirstTry = responseFirstAttempt?.score?.intValueExact(),
            scoreSecondTry = responseSecondAttempt?.score?.intValueExact(),
            expectedChoice = ExclusiveChoiceSpecification(
                nbCandidateItem = statement.choiceSpecification?.toLegacy()?.itemCount!!,
                expectedChoice = statement.choiceSpecification?.toLegacy()?.expectedChoiceList!![0]
            )
        )
    }
}

data class LearnerSequenceResponses(
    val responseFirstAttempt: Response?,
    val responseSecondAttempt: Response?,
    val chatGptEvaluationResponseStore: ChatGptEvaluationResponseStore
) {
    val responseFirstTryHasChatGPTEvaluation: Boolean
        get() = responseFirstAttempt != null && chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(
            responseFirstAttempt.id!!
        )

    val responseSecondTryHasChatGPTEvaluation: Boolean
        get() = responseSecondAttempt != null && chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(
            responseSecondAttempt.id!!
        )
}