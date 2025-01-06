package org.elaastic.ai.evaluation.chatgpt

/**
 * Store the ids of responses that received a ChatGPT evaluation.
 *
 * @author John Tranier
 * @author François de Saint Palais
 */
class ChatGptEvaluationResponseStore(responseIds: Collection<Long> = emptyList()) {

    private val responseIdSet: MutableSet<Long> = emptySet<Long>().toMutableSet()

    init {
        responseIdSet.addAll(responseIds)
    }

    fun addResponseId(responseId: Long) {
        responseIdSet.add(responseId)
    }

    fun addResponseIds(responseIds: List<Long>) {
        responseIdSet.addAll(responseIds)
    }

    /**
     * Check if ChatGPT has evaluated a response.
     *
     * @param responseId the id of the response to check
     * @return true if the response has been evaluated by ChatGPT, false otherwise
     */
    fun responseHasBeenEvaluatedByChatGpt(responseId: Long?) =
        responseId != null && responseIdSet.contains(responseId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ChatGptEvaluationResponseStore

        return responseIdSet == other.responseIdSet
    }

    override fun hashCode(): Int {
        return responseIdSet.hashCode()
    }
}