package org.elaastic.moderation

import org.elaastic.activity.evaluation.peergrading.PeerGradingRepository
import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationRepository
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.assignment.AssignmentService
import org.elaastic.player.assignmentview.AssignmentOverviewModelFactory
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.sequence.SequenceService
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/report-manager")
class ReportManagerController(
    @Autowired val sequenceService: SequenceService,
    @Autowired val draxoPeerGradingService: DraxoPeerGradingService,
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val chatGptEvaluationRepository: ChatGptEvaluationRepository,
    @Autowired val peerGradingRepository: PeerGradingRepository,
    @Autowired val messageSource: MessageSource,
    @Autowired val assignmentService: AssignmentService,
) {

    @GetMapping("/{idSequence}")
    fun getAllReport(
        authentication: Authentication,
        model: Model,
        @PathVariable idSequence: Long
    ): String {
        val user = authentication.principal as User

        val sequence = sequenceService.get(user, idSequence, true)

        val draxoReportedNotRemoved = draxoPeerGradingService.findAllDraxoPeerGradingReported(sequence)
        val chatGPTReportedNotRemoved = chatGptEvaluationService.findAllReportedNotRemoved(sequence)
        val allReportedCandidateModelNotRemoved: List<ReportedCandidateModel> =
            (draxoReportedNotRemoved + chatGPTReportedNotRemoved)
                .mapNotNull { ReportedCandidateModelFactory.build(it) }

        val draxoReportedRemoved = draxoPeerGradingService.findAllDraxoPeerGradingReported(sequence, true)
        val chatGPTReportedRemoved = chatGptEvaluationService.findAllReportedRemoved(sequence)
        val allReportedCandidateModelRemoved: List<ReportedCandidateModel> =
            (draxoReportedRemoved + chatGPTReportedRemoved)
                .mapNotNull { ReportedCandidateModelFactory.build(it) }

        val registeredUsers: Int = assignmentService.countRegisteredUsers(sequence.assignment!!)

        val assignmentOverviewModel = AssignmentOverviewModelFactory.buildOnSequence(
            true,
            sequence.assignment!!,
            nbRegisteredUser = registeredUsers,
            selectedSequence = sequence,
        )


        model["user"] = user
        model["allReportedCandidateModel"] = allReportedCandidateModelNotRemoved
        model["allReportedCandidateModelRemoved"] = allReportedCandidateModelRemoved
        model["assignmentOverviewModel"] = assignmentOverviewModel

        return "moderation/report-manager"
    }

    @GetMapping("/detail/{type}/{idReportedCandidate}")
    @ResponseBody
    fun getReportedCandidateDetail(
        authentication: Authentication,
        @PathVariable type: ReportedCandidateType,
        @PathVariable idReportedCandidate: Long
    ): ReportCandidateDetail {
        val reportedCandidateDetail: ReportCandidateDetail = when (type) {
            ReportedCandidateType.PEER_GRADING -> {
                val peerGrading = peerGradingRepository.findById(idReportedCandidate).orElseThrow()
                val nbReport = draxoPeerGradingService.countAllReportedNotHiddenForGrader(
                    peerGrading.response.interaction,
                    peerGrading.grader
                )

                ReportCandidateDetail(
                    id = peerGrading.id!!,
                    contentReported = peerGrading.annotation!!,
                    reportReasons = peerGrading.reportReasons,
                    reportComment = peerGrading.reportComment,
                    type = ReportedCandidateType.PEER_GRADING,
                    reporter = peerGrading.response.learner.getDisplayName(),
                    reporterId = peerGrading.response.learner.id!!,
                    graderThatHaveBeenReported = peerGrading.grader.getDisplayName(),
                    messageSource = messageSource,
                    numberOfReport = nbReport,
                )
            }

            ReportedCandidateType.CHAT_GPT_EVALUATION -> {
                val chatGptEvaluation = chatGptEvaluationRepository.findById(idReportedCandidate).orElseThrow()
                val nbReport = chatGptEvaluationService.countAllReportedNotRemoved(
                    chatGptEvaluation.response.interaction,
                )

                ReportCandidateDetail(
                    id = chatGptEvaluation.id!!,
                    contentReported = chatGptEvaluation.annotation!!,
                    reportReasons = chatGptEvaluation.reportReasons,
                    reportComment = chatGptEvaluation.reportComment,
                    type = ReportedCandidateType.CHAT_GPT_EVALUATION,
                    reporter = chatGptEvaluation.response.learner.getDisplayName(),
                    reporterId = chatGptEvaluation.response.learner.id!!,
                    graderThatHaveBeenReported = chatGptEvaluationService.getAINameProvider(),
                    messageSource = messageSource,
                    numberOfReport = nbReport,
                )
            }
        }
        return reportedCandidateDetail
    }

    class ReportCandidateDetail(
        id: Long,
        contentReported: String,
        reportReasons: String?,
        reportComment: String?,
        type: ReportedCandidateType,
        val reporter: String,
        val reporterId: Long,
        val graderThatHaveBeenReported: String,
        val numberOfReport: Int = 0,
        private val messageSource: MessageSource,
    ) : ReportedCandidateModel(
        id,
        contentReported,
        reportReasons,
        reportComment,
        type
    ) {
        val reportReasonsI18N: List<String> = reasons.map { it.toHumanReadableString(messageSource) }
    }

    @GetMapping("/remove-report/{type}/{id}")
    @ResponseBody
    fun removeReport(
        authentication: Authentication,
        @PathVariable type: ReportedCandidateType,
        @PathVariable id: Long
    ): ResponseEntity<String> {
        val user = authentication.principal as User

        when (type) {
            ReportedCandidateType.PEER_GRADING -> {
                peerGradingService.removeReport(user, id)
            }

            ReportedCandidateType.CHAT_GPT_EVALUATION -> {
                chatGptEvaluationService.removeReport(user, id)
            }
        }
        return ResponseEntity.ok("Report removed")
    }

    @GetMapping("/remove-reported-evaluation/{type}/{id}")
    @ResponseBody
    fun removeReportedEvaluation(
        authentication: Authentication,
        @PathVariable type: ReportedCandidateType,
        @PathVariable id: Long
    ): ResponseEntity<String> {
        val user = authentication.principal as User

        when (type) {
            ReportedCandidateType.PEER_GRADING -> {
                val peerGrading = peerGradingRepository.findById(id).orElseThrow()
                peerGradingService.markAsRemoved(user, peerGrading)
            }

            ReportedCandidateType.CHAT_GPT_EVALUATION -> {
                val chatGptEvaluation = chatGptEvaluationRepository.findById(id).orElseThrow()
                chatGptEvaluationService.markAsRemoved(user, chatGptEvaluation)
            }
        }
        return ResponseEntity.ok("Evaluation completely hidden")
    }

    @GetMapping("/restore-reported-evaluation/{type}/{id}")
    @ResponseBody
    fun unremoveReportedEvaluation(
        authentication: Authentication,
        @PathVariable type: ReportedCandidateType,
        @PathVariable id: Long
    ): ResponseEntity<String> {
        val user = authentication.principal as User

        when (type) {
            ReportedCandidateType.PEER_GRADING -> {
                val peerGrading = peerGradingRepository.findById(id).orElseThrow()
                peerGradingService.markAsRestored(user, peerGrading)
            }
            ReportedCandidateType.CHAT_GPT_EVALUATION -> {
                val chatGptEvaluation = chatGptEvaluationRepository.findById(id).orElseThrow()
                chatGptEvaluationService.markAsRestored(user, chatGptEvaluation)
            }
        }
        return ResponseEntity.ok("Evaluation restored")
    }
}