package org.elaastic.player.results.learner

import org.elaastic.activity.response.Response
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
        responseFirstAttempt: Response?,
        responseSecondAttempt: Response?,
        responseFirstTryHasChatGPTEvaluation: Boolean,
        responseSecondTryHasChatGPTEvaluation: Boolean,
        statement: Statement
    ): LearnerResultsModel {
        val learnerResultsModel = when (statement.questionType) {
            QuestionType.ExclusiveChoice -> buildExclusiveChoiceResult(
                responseFirstAttempt,
                responseSecondAttempt,
                responseFirstTryHasChatGPTEvaluation,
                responseSecondTryHasChatGPTEvaluation,
                statement
            )

            QuestionType.MultipleChoice -> buildMultipleChoiceResult(
                responseFirstAttempt,
                responseSecondAttempt,
                responseFirstTryHasChatGPTEvaluation,
                responseSecondTryHasChatGPTEvaluation,
                statement
            )

            QuestionType.OpenEnded -> buildOpenResult(
                responseFirstAttempt,
                responseSecondAttempt,
                responseFirstTryHasChatGPTEvaluation,
                responseSecondTryHasChatGPTEvaluation,
            )
        }
        return learnerResultsModel
    }

    fun buildOpenResult(
        responseFirstTry: Response?,
        responseSecondTry: Response?,
        responseFirstTryHasChatGPTEvaluation: Boolean,
        responseSecondTryHasChatGPTEvaluation: Boolean,
    ): LearnerOpenResults =
        LearnerOpenResults(
            explanationFirstTry = if (responseFirstTry != null) ExplanationDataFactory.create(
                responseFirstTry,
                responseFirstTryHasChatGPTEvaluation
            ) else null,
            explanationSecondTry = if (responseSecondTry != null) ExplanationDataFactory.create(
                responseSecondTry,
                responseSecondTryHasChatGPTEvaluation
            ) else null
        )

    fun buildMultipleChoiceResult(
        responseFirstTry: Response?,
        responseSecondTry: Response?,
        responseFirstTryHasChatGPTEvaluation: Boolean,
        responseSecondTryHasChatGPTEvaluation: Boolean,
        statement: Statement
    ): LearnerMultipleChoiceResults =
        LearnerMultipleChoiceResults(
            explanationFirstTry = if (responseFirstTry != null) ExplanationDataFactory.create(
                responseFirstTry,
                responseFirstTryHasChatGPTEvaluation
            ) else null,
            explanationSecondTry = if (responseSecondTry != null) ExplanationDataFactory.create(
                responseSecondTry,
                responseSecondTryHasChatGPTEvaluation
            ) else null,
            choiceFirstTry = responseFirstTry?.learnerChoice,
            choiceSecondTry = responseSecondTry?.learnerChoice,
            scoreFirstTry = responseFirstTry?.score?.intValueExact(),
            scoreSecondTry = responseSecondTry?.score?.intValueExact(),
            expectedChoice = MultipleChoiceSpecification(
                nbCandidateItem = statement.choiceSpecification?.toLegacy()?.itemCount!!,
                expectedChoiceList = statement.choiceSpecification?.toLegacy()?.expectedChoiceList!!
            )
        )

    fun buildExclusiveChoiceResult(
        responseFirstTry: Response?,
        responseSecondTry: Response?,
        responseFirstTryHasChatGPTEvaluation: Boolean,
        responseSecondTryHasChatGPTEvaluation: Boolean,
        statement: Statement
    ): LearnerExclusiveChoiceResults =
        LearnerExclusiveChoiceResults(
            explanationFirstTry = if (responseFirstTry != null) ExplanationDataFactory.create(
                responseFirstTry,
                responseFirstTryHasChatGPTEvaluation
            ) else null,
            explanationSecondTry = if (responseSecondTry != null) ExplanationDataFactory.create(
                responseSecondTry,
                responseSecondTryHasChatGPTEvaluation
            ) else null,
            choiceFirstTry = responseFirstTry?.learnerChoice,
            choiceSecondTry = responseSecondTry?.learnerChoice,
            scoreFirstTry = responseFirstTry?.score?.intValueExact(),
            scoreSecondTry = responseSecondTry?.score?.intValueExact(),
            expectedChoice = ExclusiveChoiceSpecification(
                nbCandidateItem = statement.choiceSpecification?.toLegacy()?.itemCount!!,
                expectedChoice = statement.choiceSpecification?.toLegacy()?.expectedChoiceList!![0]
            )
        )
}