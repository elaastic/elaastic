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

package org.elaastic.player

import com.nhaarman.mockitokotlin2.atLeastOnce
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.mockk.every
import io.mockk.mockkClass
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.ResponseService
import org.elaastic.ai.evaluation.chatgpt.ChatGptEvaluationService
import org.elaastic.analytics.lrs.EventLogService
import org.elaastic.assignment.Assignment
import org.elaastic.assignment.AssignmentService
import org.elaastic.common.web.MessageBuilder
import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.material.instructional.subject.Subject
import org.elaastic.player.dashboard.DashboardModelFactory
import org.elaastic.player.results.TeacherResultDashboardService
import org.elaastic.player.sequence.SequenceModelFactory
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.security.TestSecurityConfig
import org.elaastic.sequence.LearnerSequence
import org.elaastic.sequence.LearnerSequenceService
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.State
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.interaction.InteractionType
import org.elaastic.sequence.phase.LearnerPhaseService
import org.elaastic.test.FunctionalTestingService
import org.elaastic.test.IntegrationTestingService
import org.elaastic.user.AnonymousUserService
import org.elaastic.user.User
import org.elaastic.user.UserService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.test.context.support.WithUserDetails
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.togglz.core.manager.FeatureManager

@ExtendWith(SpringExtension::class)
@WebMvcTest(PlayerController::class)
@ContextConfiguration(classes = [TestSecurityConfig::class])
@WithUserDetails("teacher")
internal class PlayerControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val integrationTestingService: IntegrationTestingService,
    @Autowired val userDetailsService: UserDetailsService,
) {
    @MockBean
    lateinit var assignmentService: AssignmentService

    @MockBean
    lateinit var sequenceService: SequenceService

    @MockBean
    lateinit var learnerSequenceService: LearnerSequenceService

    @MockBean
    lateinit var interactionService: InteractionService

    @MockBean
    lateinit var responseService: ResponseService

    @MockBean
    lateinit var messageBuilder: MessageBuilder

    @MockBean
    lateinit var anonymousUserService: AnonymousUserService

    @MockBean
    lateinit var learnerPhaseService: LearnerPhaseService

    @MockBean
    lateinit var userService: UserService

    @MockBean
    lateinit var featureManager: FeatureManager

    @MockBean
    lateinit var teacherResultDashboardService: TeacherResultDashboardService

    @MockBean
    lateinit var chatGptEvaluationService: ChatGptEvaluationService

    @MockBean
    lateinit var eventLogService: EventLogService

    @MockBean
    lateinit var peerGradingService: PeerGradingService

    @MockBean
    lateinit var beanIntegrationTestingService: IntegrationTestingService

    @MockBean
    lateinit var functionalTestingService: FunctionalTestingService

    @MockBean
    lateinit var draxoPeerGradingService: DraxoPeerGradingService

    @MockBean
    lateinit var sequenceModelFactory: SequenceModelFactory

    @MockBean
    lateinit var dashboardModelFactory: DashboardModelFactory


    @Test
    fun `consultPlayer is called whenever a student accesses to the player`() {

        val teacher = User("firstName", "lastName", "teacher", "any")
        val user = userDetailsService.loadUserByUsername("teacher") as User

        val fakeAssignmentId = 383L
        val assignment = Assignment(
            title = "Test",
            owner = teacher,
            subject = Subject("Subject", teacher),
            scholarYear = "Any",
            audience = "Any",
            acceptAnonymousUsers = true
        )
        val sequence = org.elaastic.sequence.Sequence(
            owner = teacher,
            assignment = assignment,
            statement = Statement(teacher, "Title", "content", questionType = QuestionType.OpenEnded),
            state = State.afterStop,
        )
        sequence.id = 1L
        assignment.sequences.add(sequence)
        val interaction = Interaction(
            interactionType = InteractionType.Read,
            sequence = sequence,
            owner = user,
            rank = 1,
            state = State.afterStop
        )

        val learnerSequence = LearnerSequence(user, sequence, interaction)
        sequence.activeInteraction = interaction

        whenever(assignmentService.get(fakeAssignmentId, true)).thenReturn(assignment)
        whenever(sequenceService.get(sequence.id!!, fetchInteractions = true)).thenReturn(sequence)
        whenever(learnerSequenceService.getLearnerSequence(user, sequence)).thenReturn(learnerSequence)

        mockkObject(PlayerModelFactory)
        every { PlayerModelFactory.buildForLearner(any(), any()) } returns
                mockkClass(LearnerPlayerModel::class, relaxed = true)

        mockMvc.perform(
            MockMvcRequestBuilders.get("/player/assignment/{fakeAssignmentId}/play", fakeAssignmentId)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)

        verify(eventLogService, atLeastOnce()).consultPlayer(eq(sequence), eq(user), eq(learnerSequence), eq(null))

    }

    @AfterEach
    fun afterTests() {
        unmockkAll()
    }
}

