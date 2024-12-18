package org.elaastic.ai.evaluation.chatgpt

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseRepository
import org.elaastic.activity.response.ResponseService
import org.elaastic.ai.evaluation.chatgpt.api.ChatGptApiMessageData
import org.elaastic.ai.evaluation.chatgpt.api.ChatGptCompletionService
import org.elaastic.ai.evaluation.chatgpt.prompt.ChatGptPrompt
import org.elaastic.ai.evaluation.chatgpt.prompt.ChatGptPromptService
import org.elaastic.common.util.requireAccess
import org.elaastic.moderation.ReportCandidateService
import org.elaastic.moderation.UtilityGrade
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.user.User
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.persistence.EntityManager

@Service
class ChatGptEvaluationService(
    val chatGptEvaluationRepository: ChatGptEvaluationRepository,
    val responseRepository: ResponseRepository,
    val chatGptPromptService: ChatGptPromptService,
    val reportCandidateService: ReportCandidateService,
    val responseService: ResponseService,
    val entityManager: EntityManager,
    val chatGptCompletionService: ChatGptCompletionService,
    val messageSource: MessageSource,
) {

    val logger = Logger.getLogger(ChatGptEvaluationService::class.java.name)
    val locale: Locale = LocaleContextHolder.getLocale()

    /**
     * Create a ChatGPT evaluation for a response. The evaluation is created
     * asynchronously.
     *
     * @param response the response to evaluate
     * @param language the language of the evaluation
     * @param chatGptExistingEvaluation the existing evaluation if it exists
     * @return the created evaluation
     */
    @Async
    @Transactional(propagation = Propagation.NEVER)
    fun createEvaluation(
        response: Response,
        language: String,
        chatGptExistingEvaluation: ChatGptEvaluation? = null
    ): ChatGptEvaluation {
        // get the default prompt for the language
        val chatGptDefaultPrompt = chatGptPromptService.getPrompt(language)
        // Initialization of the evaluation
        val chatGptEvaluation = chatGptExistingEvaluation ?: ChatGptEvaluation(response = response)
        chatGptEvaluation.prompt = chatGptDefaultPrompt
        markEvaluationAsPending(chatGptEvaluation)
        // build the prompt
        val prompt = buildThePrompt(chatGptDefaultPrompt, response)
        // build the evaluation
        try {
            // get the response from ChatGPT
            logger.info("Generating response with ChatGPT for response ${response.id}")
            logger.fine("Prompt: $prompt")
            val generatedResponse = chatGptCompletionService.getChatGptResponse(
                listOf(ChatGptApiMessageData(role = "user", content = prompt)),
            ).messageList.first().content
            logger.info("Response generated with ChatGPT for response ${response.id}")
            logger.fine("Generated response: $generatedResponse")
            // convert the generated response to a ChatGptEvaluationData object
            val chatGptEvaluationData = ObjectMapper().readValue(
                generatedResponse,
                ChatGptEvaluationData::class.java
            )
            // finalize the evaluation
            chatGptEvaluation.status = ChatGptEvaluationStatus.DONE.name
            chatGptEvaluation.grade = chatGptEvaluationData.grade
            chatGptEvaluation.annotation = chatGptEvaluationData.annotation
        } catch (e: Exception) {
            chatGptEvaluation.status = ChatGptEvaluationStatus.ERROR.name
            logger.log(Level.SEVERE, "Error while evaluating response with ChatGPT: ${e.message}",e)
        }
        return chatGptEvaluationRepository.save(chatGptEvaluation)
    }


    fun findEvaluationByResponse(response: Response): ChatGptEvaluation? =
        chatGptEvaluationRepository.findByResponse(response).takeIf { it?.removedByTeacher == false }

    fun findEvaluationById(id: Long): ChatGptEvaluation? {
        return chatGptEvaluationRepository.findById(id).get().takeIf { !it.removedByTeacher }
    }


    /**
     * Mark a chatGPT evaluation as hidden by a teacher.
     *
     * @param chatGptEvaluation the chatGPT evaluation to hide.
     */
    fun markAsHidden(chatGptEvaluation: ChatGptEvaluation) {
        reportCandidateService.markAsHidden(chatGptEvaluation, chatGptEvaluationRepository)
    }

    /**
     * Mark a chatGPT evaluation as removed by a teacher.
     *
     * @param chatGptEvaluation the chatGPT evaluation to remove.
     */
    fun markAsRemoved(user: User, chatGptEvaluation: ChatGptEvaluation) {
        requireAccess(user == chatGptEvaluation.response.interaction.owner) {
            "You don't have the permission to remove this evaluation"
        }

        reportCandidateService.markAsRemoved(chatGptEvaluation, chatGptEvaluationRepository)
    }

    /**
     * Update the utility grade associated with a chatGPT evaluation.
     *
     * @param chatGptEvaluation the chatGPT evaluation to update.
     * @param utilityGrade the utility grade.
     */
    fun changeUtilityGrade(chatGptEvaluation: ChatGptEvaluation, utilityGrade: UtilityGrade, isTeacher: Boolean = false) {
        if (isTeacher) {
            chatGptEvaluation.teacherUtilityGrade = utilityGrade
            chatGptEvaluationRepository.save(chatGptEvaluation)
        } else {
            reportCandidateService.updateGrade(chatGptEvaluation, utilityGrade, chatGptEvaluationRepository)
        }
    }

    /**
     * Update the report associated with a chatGPT evaluation.
     *
     * @param chatGptEvaluation the chatGPT evaluation to update.
     * @param reportReasons the reasons for the report.
     * @param reportComment the comment for the report.
     */
    fun reportEvaluation(
        chatGptEvaluation: ChatGptEvaluation,
        reportReasons: List<String>,
        reportComment: String? = null
    ) {
        reportCandidateService.updateReport(
            chatGptEvaluation,
            reportReasons,
            reportComment,
            chatGptEvaluationRepository
        )
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun markEvaluationAsPending(chatGptEvaluation: ChatGptEvaluation): ChatGptEvaluation {
        chatGptEvaluation.status = ChatGptEvaluationStatus.PENDING.name
        return chatGptEvaluationRepository.saveAndFlush(chatGptEvaluation)
    }

    /**
     * Check if the visibility of a chatGPT evaluation can be changed by the
     * given user
     *
     * A user can hide a chatGPT evaluation if the user is the teacher of the
     * sequence and if the evaluation is done.
     *
     * @param chatGptEvaluation the chatGPT evaluation to check.
     * @param user the user who wants to update the visibility of the
     *    evaluation.
     * @return true if the visibility of the chatGPT evaluation can be changed,
     *    false otherwise.
     */
    fun canUpdateVisibilityEvaluation(chatGptEvaluation: ChatGptEvaluation, user: User): Boolean {
        return responseService.canHidePeerGrading(
            user,
            chatGptEvaluation.response
        ) && chatGptEvaluation.status == ChatGptEvaluationStatus.DONE.name
    }

    /**
     * Hide a chatGPT evaluation.
     *
     * An user must have the permission to hide the evaluation. If the user
     * doesn't have the permission, an exception is thrown.
     *
     * @param chatGptEvaluation the chatGPT evaluation to hide.
     * @param user the user who wants to hide the evaluation.
     * @throws IllegalAccessException if the user doesn't have the permission
     *    to hide the evaluation.
     */
    @Throws(IllegalAccessException::class)
    fun markAsHidden(chatGptEvaluation: ChatGptEvaluation, user: User) {
        requireAccess(
            canUpdateVisibilityEvaluation(
                chatGptEvaluation,
                user
            )
        ) { messageSource.getMessage("chatGPT.error.markAsHidden.accessDenied", null, locale) }
        reportCandidateService.markAsHidden(chatGptEvaluation, chatGptEvaluationRepository)
    }

    /**
     * Unhide a chatGPT evaluation.
     *
     * An user must have the permission to unhide the evaluation. If the user
     * doesn't have the permission, an exception is thrown.
     *
     * @param chatGPTEvaluation the chatGPT evaluation to unhide
     * @param user the user who wants to unhide the evaluation.
     * @throws IllegalAccessException if the user doesn't have the permission
     *    to unhide the evaluation.
     */
    @Throws(IllegalAccessException::class)
    fun markAsShown(chatGPTEvaluation: ChatGptEvaluation, user: User) {
        requireAccess(
            canUpdateVisibilityEvaluation(
                chatGPTEvaluation,
                user
            )
        ) { messageSource.getMessage("chatGPT.error.markAsShown.accessDenied", null, locale) }
        reportCandidateService.markAsShown(chatGPTEvaluation, chatGptEvaluationRepository)
    }

    /**
     * With the given list of response ids, if the response has been evaluated
     * by ChatGPT, associate the evaluation to true. If the response has
     * not been evaluated by ChatGPT, associate the evaluation to false.
     *
     * @param listIdResponse the list of response ids to evaluate
     * @return a map with the response id as key and a boolean as value
     */
    fun associateResponseToChatGPTEvaluationExistence(listIdResponse: List<Long?>): Map<Long, Boolean> {
        val result =  entityManager.createQuery(
            """
            SELECT r.id, gpt.response.id
            FROM Response r
            LEFT JOIN ChatGptEvaluation gpt ON r.id = gpt.response.id
            WHERE r.id IN :listIdResponse AND gpt.removedByTeacher = false
        """.trimIndent()
        )
            .setParameter("listIdResponse", listIdResponse)
            .resultList.toList()

        return listIdResponse.associate { id ->
            id!! to result.any { it is Array<*> && it[0] == id && it[1] != null }
        }
    }

    /**
     * Find all the evaluations made on a sequence. We retrieve all the
     * responses of the sequence, and then we retrieve all the peer grading
     * that have been made on these responses.
     *
     * @param sequence the sequence.
     * @return the list of peer grading.
     */
    fun findAllBySequence(sequence: Sequence): List<ChatGptEvaluation> =
        chatGptEvaluationRepository.findAllByResponseIn(
            responseRepository.findAllByInteraction(
                sequence.getResponseSubmissionInteraction(),
            )
        ).filter { !it.removedByTeacher }

    /**
     * Find all the evaluations made on a sequence that have been reported and
     * not removed.
     *
     * @param sequence the sequence.
     * @return the list of ChatGPTEvaluation.
     */
    fun findAllReportedNotRemoved(sequence: Sequence): List<ChatGptEvaluation> {
        return findAllReported(sequence, removed = false)
    }

    /**
     * @see countAllReportedNotRemoved(Interaction)
     */
    fun countAllReportedNotRemoved(sequence: Sequence): Int {
        return countAllReportedNotRemoved(
            sequence.getResponseSubmissionInteraction(),
        )
    }

    /**
     * Count all the evaluations made on a sequence that have been reported and
     * not hidden.
     *
     * @param interaction the interaction.
     * @return the number of peer grading.
     */
    fun countAllReportedNotRemoved(interaction: Interaction): Int {
        return countAllReported(interaction, removed = false)
    }

    fun countAllReported(sequence: Sequence, removed: Boolean): Int {
        return countAllReported(sequence.getResponseSubmissionInteraction(), removed)
    }

    fun countAllReported(interction: Interaction, removed: Boolean): Int {
        return chatGptEvaluationRepository.countAllReported(
            interction,
            removed
        )
    }

    fun findAllReportedRemoved(sequence: Sequence): List<ChatGptEvaluation> {
        return findAllReported(sequence, removed = true)
    }

    fun findAllReported(sequence: Sequence, removed: Boolean): List<ChatGptEvaluation> {
        return chatGptEvaluationRepository.findAllReported(
            sequence.getResponseSubmissionInteraction(),
            removed = removed
        )
    }

    private fun buildThePrompt(
        chatGptPrompt: ChatGptPrompt,
        response: Response
    ): String {
        val questionTitle = response.statement.title
        val questionStatement = response.statement.content
        val teacherExplanation = response.statement.expectedExplanation
        val studentExplanation = response.explanation

        requireNotNull(teacherExplanation) { throw IllegalArgumentException("Error: You must define an expected explanation to create a ChatGPT evaluation") }
        requireNotNull(studentExplanation) { throw IllegalArgumentException("Error: No explanation to evaluate") }

        // add to the prompt the title, the question content, the teacher explanation and the student explanation as a json object
        val promptData = PromptData(
            questionTitle = questionTitle,
            questionStatement = questionStatement,
            teacherExplanation = teacherExplanation,
            studentExplanation = studentExplanation,
            studentChoices = response.learnerChoice,
            studentScoreBasedOnChoices = response.score,
        )

        val objectMapper = ObjectMapper()
        val jsonObject = objectMapper.writeValueAsString(promptData)

        return chatGptPrompt.content + "\n" + jsonObject
    }

    /**
     * Remove the report associated with a ChatGPT evaluation.
     *
     * @param user the user who wants to remove the report.
     * @param chatGptEvaluation the ChatGPT evaluation to update.
     * @throws IllegalAccessException if the user doesn't have the permission
     *    to remove the report.
     */
    fun removeReport(
        user: User,
        chatGptEvaluation: ChatGptEvaluation
    ) {
        requireAccess(user == chatGptEvaluation.response.interaction.owner) {
            "You don't have the permission to remove the report"
        }

        chatGptEvaluation.reportReasons = null
        chatGptEvaluation.reportComment = null

        chatGptEvaluationRepository.save(chatGptEvaluation)
    }

    fun removeReport(user: User, id: Long) {
        val chatGptEvaluation = chatGptEvaluationRepository.findById(id).orElseThrow()
        removeReport(user, chatGptEvaluation)
    }

    fun markAsRestored(user: User, chatGptEvaluation: ChatGptEvaluation) {
        requireAccess(user == chatGptEvaluation.response.interaction.owner) {
            "You don't have the permission to restore this evaluation"
        }
        reportCandidateService.markAsRestored(chatGptEvaluation, chatGptEvaluationRepository)
    }

    /**
     * Return the name of the IA use to generate the evaluation.
     */
    fun getAINameProvider(): String {
        return "ChatGPT"
    }
}


data class PromptData(
    val questionTitle: String,
    val questionStatement: String,
    val teacherExplanation: String,
    val studentExplanation: String,
    val studentChoices: List<Int>? = null,
    val studentScoreBasedOnChoices: BigDecimal? = null,
)

data class ChatGptEvaluationData(
    @JsonProperty("grade") val grade: BigDecimal,
    @JsonProperty("annotation") val annotation: String,
)