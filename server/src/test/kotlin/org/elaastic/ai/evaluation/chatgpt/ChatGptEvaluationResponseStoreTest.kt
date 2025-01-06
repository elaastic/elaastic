package org.elaastic.ai.evaluation.chatgpt

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ChatGptEvaluationResponseStoreTest {

    @Test
    fun testAddResponseId() {
        // Given a ChatGptEvaluationResponseStore with no response id
        val chatGptEvaluationResponseStore = ChatGptEvaluationResponseStore()

        // When adding a response id
        val responseId: Long = 1
        chatGptEvaluationResponseStore.addResponseId(responseId)

        // Then the response id should be stored
        assertTrue(chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(responseId))
        assertFalse(chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(2))
    }

    @Test
    fun testAddResponseIds() {
        // Given a ChatGptEvaluationResponseStore with no response id
        val chatGptEvaluationResponseStore = ChatGptEvaluationResponseStore()

        // When adding a list of response ids
        chatGptEvaluationResponseStore.addResponseIds(listOf(1, 2, 3))

        // Then the response ids should be stored
        assertTrue(chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(1))
        assertTrue(chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(2))
        assertTrue(chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(3))
    }

    @Test
    fun testResponseHasBeenEvaluatedByChatGpt() {
        // Given a ChatGptEvaluationResponseStore with a response id
        val chatGptEvaluationResponseStore = ChatGptEvaluationResponseStore(listOf(1))

        // Then the response id should be stored
        assertTrue(chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(1))
        assertFalse(chatGptEvaluationResponseStore.responseHasBeenEvaluatedByChatGpt(2))
    }

    @Test
    fun testEquals() {
        // Given two ChatGptEvaluationResponseStore with the same response ids
        val chatGptEvaluationResponseStore1 = ChatGptEvaluationResponseStore()
        chatGptEvaluationResponseStore1.addResponseIds(listOf(1, 2, 3))
        val chatGptEvaluationResponseStore2 = ChatGptEvaluationResponseStore(listOf(1, 2, 3))

        // Then the two ChatGptEvaluationResponseStore should be equal
        assertEquals(chatGptEvaluationResponseStore1, chatGptEvaluationResponseStore2)
    }
}