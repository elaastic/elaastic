package org.elaastic.ai.evaluation.chatgpt

import org.elaastic.test.IntegrationTestingService
import org.elaastic.test.directive.tThen
import org.elaastic.test.directive.tWhen
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.EnabledIf
import javax.persistence.EntityManager
import javax.transaction.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@EnabledIf(value = "#{@featureManager.isActive(@featureResolver.getFeature('CHATGPT_EVALUATION'))}", loadContext = true)
internal class ChatGptEvaluationRepositoryIntegrationTest(
    @Autowired val chatGptEvaluationRepository: ChatGptEvaluationRepository,
    @Autowired val integrationTestingService: IntegrationTestingService,
    @Autowired val entityManager: EntityManager
) {

    @BeforeEach
    fun setup() {
        chatGptEvaluationRepository.deleteAll()
        // Precondition
        assertTrue(chatGptEvaluationRepository.findAll().isEmpty())
    }

    @Test
    fun `save a valid ChatGPT evaluation`() {

        ChatGptEvaluation(
            response = integrationTestingService.getAnyResponse()
        )
            .tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
            .tThen {
                assertNotNull(it.id)
                assertNotNull(it.dateCreated)
                assertNotNull(it.lastUpdated)

                assertNull(it.annotation)
                assertNull(it.grade)
                assertEquals(it.status, ChatGptEvaluationStatus.UNKNOWN.name)

                assertNull(it.reportReasons)
                assertNull(it.reportComment)
                assertNull(it.utilityGrade)

                assertFalse(it.hiddenByTeacher)
                assertFalse(it.removedByTeacher)
            }
    }

}