package org.elaastic.player

import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.player.command.CommandModelFactory
import org.elaastic.player.sequence.SequenceModelFactory
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.sequence.LearnerSequenceService
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.phase.LearnerPhaseService
import org.elaastic.sequence.phase.LearnerPhaseType
import org.elaastic.sequence.phase.descriptor.SequenceDescriptor
import org.elaastic.sequence.phase.result.LearnerResultPhaseViewModel
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
    private val sequenceService: SequenceService,
    private val sequenceModelFactory: SequenceModelFactory,
    private val learnerSequenceService: LearnerSequenceService,
    private val learnerPhaseService: LearnerPhaseService,
    private val sequenceDescriptor: SequenceDescriptor
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

    @GetMapping("/all-explanations/{sequenceId}")
    fun allExplanations(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long,
    ): String {
        val user = authentication.principal as User
        val sequence = sequenceService.get(sequenceId)
        val isTeacher = sequence.owner == user

        val explanationViewerModel = if (isTeacher) {
            sequenceModelFactory.buildForTeacher(user, sequence).resultsModel!!.explanationViewerModel
        } else {
            val learnerSequence = learnerSequenceService.getLearnerSequence(
                user,
                sequenceService.loadInteractions(sequence)
            )

            (learnerPhaseService.buildPhase(
                learnerSequence,
                sequenceDescriptor.phaseDescriptorList.find { it.type == LearnerPhaseType.RESULT }!!,
                1,
                active = true
            ).getViewModel() as LearnerResultPhaseViewModel).sequenceResultsModel.explanationViewerModel
        }

        //sequenceId,explanationViewerModel, isTeacher
        model["sequenceId"] = sequenceId
        model["explanationViewerModel"] = explanationViewerModel!!
        model["isTeacher"] = isTeacher
        return "player/assignment/sequence/components/explanation-viewer/_all-explanations-modal.html :: allExplanationsPopup"
    }
}