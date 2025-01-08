package org.elaastic.player.explanations

import org.elaastic.material.instructional.question.ChoiceSpecification
import org.elaastic.material.instructional.question.ExclusiveChoiceSpecification
import org.elaastic.material.instructional.question.MultipleChoiceSpecification
import org.elaastic.activity.response.Response
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationResponseStore

class ChoiceExplanationStore(choiceSpecification: ChoiceSpecification) : ExplanationStore,
    HashMap<ResponseData, MutableList<ExplanationData>>() {

    init {
        when (choiceSpecification) {
            is ExclusiveChoiceSpecification ->

                // Create all the entries for exclusive choice
                repeat(choiceSpecification.nbCandidateItem) {
                    this.put(
                        ResponseData(
                            choices = listOf(it + 1),
                            correct = (it + 1) == choiceSpecification.expectedChoice.index,
                            score = if ((it + 1) == choiceSpecification.expectedChoice.index) 100 else 0
                        ),
                        mutableListOf()
                    )
                }

            is MultipleChoiceSpecification ->

                // Create only the correct entry for multiple choices
                this.put(
                    ResponseData(
                        choices = choiceSpecification.expectedChoiceList.map { it.index },
                        correct = true,
                        score = 100
                    ),
                    mutableListOf()
                )

            else -> error("Unsupported type of ChoiceSpecification: ${choiceSpecification.javaClass}")
        }

    }

    constructor(
        choiceSpecification: ChoiceSpecification,
        responseList: List<Response>,
        chatGptEvaluationResponseStore: ChatGptEvaluationResponseStore
    ) : this(choiceSpecification) {
        responseList.forEach { add(it, chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(it.id!!)) }
    }

    fun add(response: Response, explanationHasChatGPTEvaluation: Boolean) {
        if (response.learnerChoice != null)
            add(
                ResponseData(response),
                ExplanationDataFactory.create(response, explanationHasChatGPTEvaluation)
            )
    }

    fun add(responseData: ResponseData, explanationData: ExplanationData) {
        this[responseData]?.add(explanationData)
            ?: this.put(responseData, mutableListOf(explanationData))

    }

}