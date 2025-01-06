package org.elaastic.player.results

import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.ResponseService
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.common.web.MessageBuilder
import org.elaastic.sequence.Sequence
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.togglz.core.manager.FeatureManager

@Service
class TeacherResultDashboardService(
    @Autowired val responseService: ResponseService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val messageBuilder: MessageBuilder,
    @Autowired val featureManager: FeatureManager,
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService,
) {

    fun buildModel(sequence: Sequence): ResultsModel {

        val responseSet = responseService.findAll(sequence, excludeFakes = false)

        val listIdResponse = responseSet[1].map { it.id } + responseSet[2].map { it.id }

        val chatGptEvaluationResponseStore =
            chatGptEvaluationService.associateResponseToChatGPTEvaluationExistence(listIdResponse)

        return ResultsModelFactory.build(
            true,
            sequence,
            featureManager,
            responseSet = responseSet,
            true,
            messageBuilder,
            peerGradings = peerGradingService.findAllByAttempt(sequence, 1),
            chatGptEvaluationResponseStore = chatGptEvaluationResponseStore
        )
    }

}
