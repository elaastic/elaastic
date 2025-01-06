package org.elaastic.player.explanations

import org.elaastic.material.instructional.question.ChoiceSpecification
import org.elaastic.activity.response.Response
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationResponseStore

object ExplanationViewerModelFactory {

    fun buildOpen(
        teacher: Boolean,
        responseList: List<Response>,
        chatGptEvaluationResponseStore: ChatGptEvaluationResponseStore
    ) =
        OpenExplanationViewerModel(
            responseList.map { ExplanationDataFactory.create(it, chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(it.id)) },
            true,
            studentsIdentitiesAreDisplayable = teacher
        )

    fun buildChoice(
        teacher: Boolean,
        responseList: List<Response>,
        choiceSpecification: ChoiceSpecification,
        recommendedExplanationsComparator: Comparator<ExplanationData>? = null,
        chatGptEvaluationResponseStore: ChatGptEvaluationResponseStore
    ): ExplanationViewerModel =
        ChoiceExplanationViewerModel(
            // TODO I should simplify (merge ChoiceExplanationViewerModel & ChoiceExplanationStore)
            explanationsByResponse = ChoiceExplanationStore(
                choiceSpecification,
                responseList,
                chatGptEvaluationResponseStore,
            ),
            alreadySorted = true,
            studentsIdentitiesAreDisplayable = teacher,
            showOnlyCorrectResponse = !teacher,
            recommendedExplanationsComparator = recommendedExplanationsComparator
        )

}