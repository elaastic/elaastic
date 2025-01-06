package org.elaastic.ai.evaluation.chatgpt

import org.elaastic.ai.evaluation.chatgpt.prompt.ChatGptPromptService
import org.elaastic.sequence.ExecutionContext
import org.elaastic.moderation.ReportReason
import org.elaastic.moderation.UtilityGrade
import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.activity.response.ResponseRepository
import org.elaastic.user.User
import org.elaastic.test.FunctionalTestingService
import org.elaastic.test.IntegrationTestingService
import org.elaastic.test.directive.tGiven
import org.elaastic.test.directive.tThen
import org.elaastic.test.directive.tWhen
import org.elaastic.test.interpreter.command.Phase
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.EnabledIf
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["no-async"])
@EnabledIf(value = "#{@featureManager.isActive(@featureResolver.getFeature('CHATGPT_EVALUATION'))}", loadContext = true)
@Transactional
internal class ChatGptEvaluationServiceIntegrationTest(
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService,
    @Autowired val integrationTestingService: IntegrationTestingService,
    @Autowired val chatGptEvaluationRepository: ChatGptEvaluationRepository,
    @Autowired val responseRepository: ResponseRepository,
    @Autowired val functionalTestingService: FunctionalTestingService,
    @Autowired val chatGptPromptService: ChatGptPromptService,

    ) {


    @BeforeEach
    @Transactional
    fun setup() {
        chatGptEvaluationRepository.deleteAll()
        // Precondition
        assertThat(chatGptEvaluationRepository.findAll(), `is`(empty()))
        // We want a reasonable good prompt for the test
        chatGptPromptService.updatePrompt(
            "Tu es un enseignant bienveillant qui doit évaluer la réponse donnée par un élève"
                    + " à une question. "
                    + "Tu dois donner une note comprise entre 0 et 5 à la réponse de l'élève et expliquer"
                    + " pourquoi tu as donné cette note. Tu dois fournir la réponse sous la forme d'un objet Json ayant " +
                    "la structure suivante : { \"grade\": \"\", \"annotation\": \"\" } . " +
                    "Merci de ne pas encapsuler l'objet json dans une enveloppe markdown." +
                    "La question est fournit dans le JSON suivant contenant la question et la réponse de l'élève et son score sur la base de ce qu'il a choisit comme item.",
            "fr"
        )
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `get a chatgpt evaluation - valid`() {

        val response = integrationTestingService.getAnyResponse()
        response.explanation =
            "Git est le meilleur système de gestion de version, il coche donc toutes les bonnes options."
        val promptFr = chatGptPromptService.getPrompt("fr")

        tWhen {
            chatGptEvaluationService.createEvaluation(response, "fr")
        }.tThen {
            assertThat(it.id, notNullValue())
            assertThat(it.dateCreated, notNullValue())
            assertThat(it.lastUpdated, notNullValue())

            assertThat(it.status, equalTo("DONE"))
            assertThat(it.annotation, notNullValue())
            assertEquals(promptFr, it.prompt)

            assertThat(it.reportReasons, nullValue())
            assertThat(it.reportComment, nullValue())
            assertThat(it.utilityGrade, nullValue())

            assertThat(it.hiddenByTeacher, equalTo(false))
            assertThat(it.removedByTeacher, equalTo(false))
        }
    }

    @Test
    fun `canHideGrading should return true if the user is the teacher of the sequence`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        val teacher: User = response.interaction.owner

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            )
        }.tThen("canHideGrading should return true") {
            assertTrue(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, teacher))
            it
        }.tThen("canHideGrading should return false") {
            val anotherUser = integrationTestingService.getTestStudent()
            assertNotEquals(teacher, anotherUser)
            assertFalse(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, anotherUser))
        }
    }

    @Test
    fun `canHideGrading should return false when the evaluation isn't DONE`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        val teacher: User = response.interaction.owner

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            )
        }.tWhen("The status isn't DONE") {
            it.status = ChatGptEvaluationStatus.PENDING.name
            assertNotEquals(ChatGptEvaluationStatus.DONE.name, it.status)
            it
        }.tThen("canHideGrading should return false") {
            assertFalse(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, teacher))
            it
        }.tWhen("The status is DONE") {
            it.status = ChatGptEvaluationStatus.DONE.name
            assertEquals(ChatGptEvaluationStatus.DONE.name, it.status)
            it
        }.tThen("canHideGrading should return true") {
            assertTrue(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, teacher))
        }
    }

    @Test
    fun `canHideGrading should return false when the user isn't the teacher of the sequence`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        val teacher: User = response.interaction.owner
        response.learner = integrationTestingService.getTestStudent()
        responseRepository.save(response)

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            )
        }.tThen("The user isn't the teacher of the sequence") {
            val anotherUser = integrationTestingService.getTestStudent()
            assertNotEquals(teacher, anotherUser)
            assertFalse(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, anotherUser))
            it
        }.tThen("the learner of the response isn't the teacher of the sequence") {
            assertNotEquals(teacher, response.learner)
            assertFalse(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, response.learner))
            it
        }.tThen("canHideGrading should return true with the teacher") {
            assertTrue(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, teacher))
        }
    }

    @Test
    fun `a teacher can hide a chatGPT evaluation in a sequence he own`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        val teacher: User = response.interaction.owner

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The teacher hide the evaluation") {
            assertDoesNotThrow {
                chatGptEvaluationService.markAsHidden(it, teacher)
            }
            it
        }.tThen("The evaluation is hidden") {
            assertTrue(it.hiddenByTeacher)
            it
        }
    }

    @Test
    fun `a student despit owning the response can't hide a chatGPT evaluation`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        response.learner = integrationTestingService.getTestStudent()
        responseRepository.save(response)
        val student: User = response.learner

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The student try to hide the evaluation") {
            assertFalse(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, student))
            assertThrows(IllegalAccessException::class.java) {
                chatGptEvaluationService.markAsHidden(it, student)
            }
            it
        }.tThen("The evaluation is not hidden") {
            assertFalse(it.hiddenByTeacher)
            it
        }
    }

    @Test
    fun `a teacher can unhide a chatGPT evaluation in a sequence he own`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        val teacher: User = response.interaction.owner

        tGiven("A chatGPT hidden evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = true,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The teacher unhide the evaluation") {
            assertTrue(it.hiddenByTeacher)
            assertDoesNotThrow {
                chatGptEvaluationService.markAsShown(it, teacher)
            }
            it
        }.tThen("The evaluation is unhidden") {
            assertFalse(it.hiddenByTeacher)
            it
        }
    }

    @Test
    fun `a student despit owning the response can't unhide a chatGPT evaluation`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        response.learner = integrationTestingService.getTestStudent()
        responseRepository.save(response)
        val student: User = response.learner

        tGiven("A chatGPT hidden evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = true,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The student try to unhide the evaluation") {
            assertTrue(it.hiddenByTeacher)
            assertFalse(chatGptEvaluationService.canUpdateVisibilityEvaluation(it, student))
            assertThrows(IllegalAccessException::class.java) {
                chatGptEvaluationService.markAsShown(it, student)
            }
            it
        }.tThen("The evaluation is not unhidden") {
            assertTrue(it.hiddenByTeacher)
            it
        }
    }

    @Test
    fun `nothing happend when we try to unhide an evalution that is visible`() {
        // Given
        val response = integrationTestingService.getAnyResponse()
        val teacher: User = response.interaction.owner

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The teacher try to unhide the evaluation") {
            assertFalse(it.hiddenByTeacher)
            assertDoesNotThrow {
                chatGptEvaluationService.markAsShown(it, teacher)
            }
            it
        }.tThen("The evaluation is not unhidden") {
            assertFalse(it.hiddenByTeacher)
            it
        }
    }

    @Test
    fun `test of associateResponseToChatGPTEvaluationExistence`() {
        // Given
        val response = integrationTestingService.getAnyResponse()

        var expected = ChatGptEvaluationResponseStore()
        assertEquals(
            expected,
            chatGptEvaluationService.associateResponseToChatGPTEvaluationExistence(listOf(response.id))
        )

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The teacher try to unhide the evaluation") {
            expected = ChatGptEvaluationResponseStore(listOf(response.id!!))
            assertEquals(
                expected,
                chatGptEvaluationService.associateResponseToChatGPTEvaluationExistence(listOf(response.id))
            )
        }
    }

    @Test
    fun `test of findAllReportedNotHidden`() {
        // Given
        val sequence = integrationTestingService.getAnySequence()
        functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)

        val learner = integrationTestingService.getNLearners(3).shuffled().first()

        val response = functionalTestingService.submitResponse(
            Phase.PHASE_1,
            learner,
            sequence,
            true,
            ConfidenceDegree.CONFIDENT,
            "explanation",
        )

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tThen("The evaluation exist but it's not reported") {
            assertEquals(
                0,
                chatGptEvaluationService.countAllReportedNotRemoved(response.interaction.sequence)
            )
            assertEquals(
                listOf(it),
                chatGptEvaluationService.findAllBySequence(response.interaction.sequence)
            )
            it
        }.tWhen("a learner report it") {
            chatGptEvaluationService.reportEvaluation(it, listOf(ReportReason.INCOHERENCE.name))
            it
        }.tThen("The evaluation is reported") {
            assertEquals(
                listOf(it).size,
                chatGptEvaluationService.countAllReportedNotRemoved(response.interaction.sequence)
            )
            it
        }
    }

    @Test
    fun `test of changeUtilityGrade`() {
        // Given
        val sequence = integrationTestingService.getAnySequence()
        functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)

        val learner = integrationTestingService.getNLearners(3).first()

        val response = functionalTestingService.submitResponse(
            Phase.PHASE_1,
            learner,
            sequence,
            true,
            ConfidenceDegree.CONFIDENT,
            "explanation",
        )

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The learner change the utility grade") {
            chatGptEvaluationService.changeUtilityGrade(it, UtilityGrade.AGREE)
            it
        }.tThen("The utility grade is changed") {
            assertEquals(UtilityGrade.AGREE, it.utilityGrade)
            assertNull(it.teacherUtilityGrade)
            it
        }.tWhen("The teacher change the utility grade") {
            chatGptEvaluationService.changeUtilityGrade(it, UtilityGrade.DISAGREE, true)
            it
        }.tThen("The utility grade isn't updated and the teacherUtilityGrade is set") {
            assertEquals(UtilityGrade.AGREE, it.utilityGrade)
            assertEquals(UtilityGrade.DISAGREE, it.teacherUtilityGrade)
        }
    }

    @Test
    fun `test of removeReport`() {
        // Given
        val sequence = integrationTestingService.getAnySequence()
        functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)

        val learner = integrationTestingService.getNLearners(3).shuffled().first()

        val response = functionalTestingService.submitResponse(
            Phase.PHASE_1,
            learner,
            sequence,
            true,
            ConfidenceDegree.CONFIDENT,
            "explanation",
        )

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("a learner report it") {
            chatGptEvaluationService.reportEvaluation(it, listOf(ReportReason.INCOHERENCE.name))
            it
        }.tThen("The evaluation is reported") {
            assertNotNull(it.reportReasons)
            it
        }.tWhen("The teacher remove the report") {
            assertThrows(IllegalAccessException::class.java, {
                chatGptEvaluationService.removeReport(learner, it.id!!)
            }, "The learner should not be able to remove the report")
            assertDoesNotThrow({
                chatGptEvaluationService.removeReport(sequence.owner, it)
            }, "The teacher should be able to remove the report")

            it
        }.tThen("The evaluation is not reported") {
            assertEquals(
                0,
                chatGptEvaluationService.countAllReportedNotRemoved(response.interaction.sequence)
            )
        }
    }

    @Test
    fun `test of markAsRemoved and markAsRestored`() {
        // Given
        val sequence = integrationTestingService.getAnySequence()
        functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)

        val learner = integrationTestingService.getNLearners(3).shuffled().first()

        val response = functionalTestingService.submitResponse(
            Phase.PHASE_1,
            learner,
            sequence,
            true,
            ConfidenceDegree.CONFIDENT,
            "explanation",
        )

        tGiven("A chatGPT evaluation") {
            ChatGptEvaluation(
                response = response,
                annotation = "annotation",
                grade = null,
                status = ChatGptEvaluationStatus.DONE.name,
                reportReasons = null,
                reportComment = null,
                utilityGrade = null,
                hiddenByTeacher = false,
                removedByTeacher = false,
            ).tWhen {
                chatGptEvaluationRepository.save(it)
                it
            }
        }.tWhen("The teacher remove the evaluation") {
            assertThrows(IllegalAccessException::class.java, {
                chatGptEvaluationService.markAsRemoved(learner, it)
            }, "The learner should not be able to remove the evaluation")
            assertDoesNotThrow({
                chatGptEvaluationService.markAsRemoved(sequence.owner, it)
            }, "The teacher should be able to remove the evaluation")
            it
        }.tThen("The evaluation is removed") {
            assertTrue(it.removedByTeacher)
            it
        }.tWhen("The teacher restore it") {
            assertThrows(IllegalAccessException::class.java, {
                chatGptEvaluationService.markAsRestored(learner, it)
            }, "The learner should not be able to restore the evaluation")
            assertDoesNotThrow({
                chatGptEvaluationService.markAsRestored(sequence.owner, it)
            }, "The teacher should be able to restore the evaluation")
            it
        }.tThen {
            assertFalse(it.removedByTeacher)
        }
    }
}