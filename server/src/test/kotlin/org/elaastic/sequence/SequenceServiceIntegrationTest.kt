/*
 * Elaastic - formative assessment system
 * Copyright (C) 2019. University Toulouse 1 Capitole, University Toulouse 3 Paul Sabatier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.elaastic.sequence

import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationRepository
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.moderation.ReportCandidateService
import org.elaastic.moderation.ReportInformation
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingRepository
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.sequence.interaction.InteractionType
import org.elaastic.sequence.phase.evaluation.EvaluationPhaseConfig
import org.elaastic.test.FunctionalTestingService
import org.elaastic.test.IntegrationTestingService
import org.elaastic.test.directive.tThen
import org.elaastic.test.directive.tWhen
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.access.AccessDeniedException
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
internal class SequenceServiceIntegrationTest(
    @Autowired val sequenceService: SequenceService,
    @Autowired val integrationTestingService: IntegrationTestingService,
    @Autowired val sequenceRepository: SequenceRepository,
    @Autowired val functionalTestingService: FunctionalTestingService,
    @Autowired val chatGptEvaluationRepository: ChatGptEvaluationRepository,
    @Autowired val chatGptEvaluationService: ChatGptEvaluationService,
    @Autowired val draxoPeerGradingRepository: DraxoPeerGradingRepository,
    @Autowired val draxoPeerGradingService: DraxoPeerGradingService,
    @Autowired val reportCandidateService: ReportCandidateService,
) {

    @Test
    fun `get a sequence - valid`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = assignment.owner

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")

        val testingSequence = assignment.sequences.first()
        assertEquals(testingSequence, sequenceService.get(user, testingSequence.id!!))
        assertEquals(testingSequence, sequenceService.get(user, testingSequence.id!!, true))
    }

    @Test
    fun `get a sequence - error unauthorized`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = integrationTestingService.getTestTeacher()

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")

        val testingSequence = assignment.sequences.first()
        assertThrows<AccessDeniedException> {
            sequenceService.get(user, testingSequence.id!!)
        }
    }

    @Test
    fun `get a sequence - error invalid id`() {
        val user = integrationTestingService.getTestTeacher()

        assertThrows<EntityNotFoundException> {
            sequenceService.get(user, 12345678)
        }
    }

    @Test
    fun findPreviousSequenceTest() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = assignment.owner

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")

        val testingSequence = assignment.sequences.last()
        val previousSequence = sequenceService.findPreviousSequence(testingSequence)
        assertNotNull(previousSequence)
        assertEquals(assignment.sequences.first(), previousSequence)
    }

    @Test
    fun findNextSequenceTest() {
        val assignment = integrationTestingService.getTestAssignment()

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")

        val testingSequence = assignment.sequences.first()
        val nextSequence = sequenceService.findNextSequence(testingSequence)
        assertNotNull(nextSequence)
        assertEquals(assignment.sequences.last(), nextSequence)
    }

    @Test
    fun `start a sequence without the owner`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = integrationTestingService.getTestStudent()

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")
        assertNotEquals(assignment.owner, user, "For this test the user must be different from the owner")

        val testingSequence = assignment.sequences.first()
        assertThrows<IllegalArgumentException> {
            sequenceService.start(
                user,
                testingSequence,
                ExecutionContext.Distance,
                true,
                1,
                EvaluationPhaseConfig.DRAXO,
                false,
            )
        }
    }

    @Test
    fun `start a sequence when the interaction already exists`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = assignment.owner

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")

        val testingSequence = assignment.sequences.first()
        // Add an interaction to the sequence
        testingSequence.interactions[InteractionType.ResponseSubmission] = Interaction(
            InteractionType.ResponseSubmission,
            rank = 0,
            owner = user,
            sequence = testingSequence,
            state = State.beforeStart,
        )
        sequenceRepository.save(testingSequence)

        assertThrows<IllegalArgumentException> {
            sequenceService.start(
                user,
                testingSequence,
                executionContext = ExecutionContext.Distance,
                studentsProvideExplanation = true,
                nbResponseToEvaluate = 1,
                evaluationPhaseConfig = EvaluationPhaseConfig.DRAXO,
                chatGptEvaluationEnable = false,
            )
        }
    }

    @Test
    fun `stop a sequence without the owner`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = integrationTestingService.getTestStudent()

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")
        assertNotEquals(assignment.owner, user, "For this test the user must be different from the owner")

        val testingSequence = assignment.sequences.first()
        assertThrows<IllegalArgumentException> {
            sequenceService.stop(user, testingSequence)
        }
    }

    @Test
    fun `reopen a sequence without the owner`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = integrationTestingService.getTestStudent()

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")
        assertNotEquals(assignment.owner, user, "For this test the user must be different from the owner")

        val testingSequence = assignment.sequences.first()
        sequenceService.stop(assignment.owner, testingSequence)
        assertThrows<IllegalArgumentException> {
            sequenceService.reopen(user, testingSequence)
        }
    }

    @Test
    fun `reopen a sequence when the sequence is not closed`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = assignment.owner
        val testingSequence = assignment.sequences.first()

        // Precondition
        assertEquals(2, assignment.sequences.size, "The testing data are corrupted")
        assertNotEquals(State.afterStop, testingSequence.state, "For this test the sequence must not be closed")

        assertThrows<IllegalArgumentException> {
            sequenceService.reopen(user, testingSequence)
        }
    }

    @Test
    fun `save a sequence`() {
        val sequence = Sequence(
            integrationTestingService.getTestTeacher(),
            integrationTestingService.getAnyStatement(),
            integrationTestingService.getTestAssignment(),
        )

        assertFalse(sequenceRepository.findAll().contains(sequence))

        sequenceService.save(sequence)

        assertTrue(sequenceRepository.findAll().contains(sequence))
    }

    @Test
    fun `countReportedEvaluation and countReportBySequence test`() {
        // Given a sequence freshly generated
        val teacher = integrationTestingService.getTestTeacher()
        val students = integrationTestingService.getNLearners(3)
        val grader = students[0]
        val sequence: Sequence = functionalTestingService.createSequence(
            teacher,
            evaluationPhaseConfig = EvaluationPhaseConfig.DRAXO,
            chatGptEvaluationEnabled = true
        )

        /** Check that the count of reported evaluation is 0 for the given sequence when the teacher is false */
        val checkAlways0WhenIsntTeacher = {
            assertEquals(0, sequenceService.countReportedEvaluation(sequence, teacher = false, isRemoved = true))
            assertEquals(0, sequenceService.countReportedEvaluation(sequence, teacher = false, isRemoved = false))
            assertEquals(ReportInformation(0, 0), sequenceService.countReportBySequence(sequence, isTeacher = false))
        }

        // Then the count of reported evaluation should be 0
        assertEquals(0, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = true))
        assertEquals(0, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = false))
        assertEquals(ReportInformation(0, 0), sequenceService.countReportBySequence(sequence, isTeacher = true))
        checkAlways0WhenIsntTeacher()

        functionalTestingService.startSequence(sequence)

        tWhen("We add a reported evaluation") {
            val student = students[1]
            functionalTestingService.createResponse(sequence, student).let { response ->
                functionalTestingService.createDRAXOEvaluation(response, grader).let { draxoPeerGrading ->
                    functionalTestingService.reportReportCandidate(student, draxoPeerGrading)
                }
            }
        }.tThen {
            assertEquals(0, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = true))
            assertEquals(1, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = false))
            assertEquals(ReportInformation(1, 1), sequenceService.countReportBySequence(sequence, isTeacher = true))
            checkAlways0WhenIsntTeacher()

        }.tWhen("We add a ChatGPT reported evaluation") {
            val student = students[2]
            functionalTestingService.createResponse(sequence, student).let { response ->
                functionalTestingService.createChatGPTEvaluation(response).let { draxoPeerGrading ->
                    functionalTestingService.reportReportCandidate(student, draxoPeerGrading)
                }
            }
        }.tThen {
            assertEquals(0, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = true))
            assertEquals(2, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = false))
            assertEquals(ReportInformation(2, 2), sequenceService.countReportBySequence(sequence, isTeacher = true))
            checkAlways0WhenIsntTeacher()

        }.tWhen("The teacher removed a DRAXOEvaluation") {
            draxoPeerGradingService.findAllDraxo(sequence).first { it.reportReasons != null }.let { draxoPeerGrading ->
                reportCandidateService.markAsRemoved(draxoPeerGrading, draxoPeerGradingRepository)
            }
        }.tThen {
            assertEquals(1, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = true))
            assertEquals(1, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = false))
            assertEquals(ReportInformation(2, 1), sequenceService.countReportBySequence(sequence, isTeacher = true))
            checkAlways0WhenIsntTeacher()
        }.tWhen("The teacher removed the ChatGPT evaluation") {
            chatGptEvaluationService.findAllBySequence(sequence).first { it.reportReasons != null }
                .let { chatGptEvaluation ->
                    reportCandidateService.markAsRemoved(chatGptEvaluation, chatGptEvaluationRepository)
                }
        }.tThen {
            assertEquals(2, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = true))
            assertEquals(0, sequenceService.countReportedEvaluation(sequence, teacher = true, isRemoved = false))
            assertEquals(ReportInformation(2, 0), sequenceService.countReportBySequence(sequence, isTeacher = true))
            checkAlways0WhenIsntTeacher()

        }
    }


}
