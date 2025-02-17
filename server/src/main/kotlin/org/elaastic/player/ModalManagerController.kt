package org.elaastic.player

import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.player.command.CommandModelFactory
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.sequence.SequenceService
import org.elaastic.user.User
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/modal-manager")
class ModalManagerController(
    private val draxoPeerGradingService: DraxoPeerGradingService,
    private val chatGptEvaluationService: ChatGptEvaluationService,
    private val sequenceService: SequenceService
) {

    /**
     * Get the report modal for a DRAXO evaluation
     *
     * @param authentication the current user authentication
     * @param model the model to add the report content
     * @param draxoEvaluationId the id of the DRAXO evaluation
     * @return the report modal
     */
    @GetMapping("/report/draxo/{draxoEvaluationId}")
    fun reportDRAXOEvaluation(
        authentication: Authentication,
        model: Model,
        @PathVariable draxoEvaluationId: Long,
    ): String {
        val draxoPeerGrading = draxoPeerGradingService.getDraxoPeerGrading(draxoEvaluationId)

        model["reportContent"] = draxoPeerGrading.annotation!!
        model["evaluationId"] = draxoPeerGrading.id!!
        model["responseId"] = draxoPeerGrading.response.id!!
        return "player/assignment/sequence/components/peer-grading-reaction/_peer-grading-draxo-reaction-report-modal.html :: reportModal"
    }


    /**
     * Get the report modal for a ChatGPT evaluation
     *
     * @param authentication the current user authentication
     * @param model the model to add the report content
     * @param iaEvaluationId the id of the ChatGPT evaluation
     * @return the report modal
     */
    @GetMapping("/report/ia/{iaEvaluationId}")
    fun reportIAEvaluation(
        authentication: Authentication,
        model: Model,
        @PathVariable iaEvaluationId: Long,
    ): String {
        val iaEvaluation = chatGptEvaluationService.findEvaluationById(iaEvaluationId)!!

        model["sequenceId"] = iaEvaluation.response.interaction.sequence.id!!
        model["reportContent"] = iaEvaluation.annotation!!
        model["evaluationId"] = iaEvaluation.id!!
        return "player/assignment/sequence/components/chat-gpt-evaluation/_chat-gpt-evaluation-report-modal.html :: reportModal"
    }

    @GetMapping("/config-sequence/{sequenceId}")
    fun configSequence(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long,
    ): String {
        val user = authentication.principal as User
        val sequence = sequenceService.get(sequenceId)

        val commandModel = CommandModelFactory.build(user, sequence)

        model["sequenceId"] = commandModel.sequenceId
        model["statementId"] = commandModel.statementId
        model["questionType"] = commandModel.questionType
        model["hasExpectedExplanation"] = commandModel.hasExpectedExplanation
        return "player/assignment/sequence/components/command/_config-sequence.html :: configSequence"
    }
}