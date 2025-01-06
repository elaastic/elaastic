package org.elaastic.assignment

import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseRepository
import org.elaastic.activity.response.ResponseService
import org.elaastic.material.instructional.question.legacy.LearnerChoice
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.material.instructional.statement.StatementRepository
import org.elaastic.material.instructional.statement.StatementService
import org.elaastic.material.instructional.subject.SubjectService
import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.interaction.InteractionType
import org.elaastic.test.FunctionalTestingService
import org.elaastic.test.IntegrationTestingService
import org.elaastic.test.directive.tExpect
import org.elaastic.test.directive.tThen
import org.elaastic.test.directive.tWhen
import org.elaastic.user.User
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.data.domain.PageRequest
import org.springframework.security.access.AccessDeniedException
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityNotFoundException
import javax.persistence.PersistenceUnitUtil
import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
@Profile("test")
internal class AssignmentServiceIntegrationTest(
    @Autowired val assignmentService: AssignmentService,
    @Autowired val integrationTestingService: IntegrationTestingService,
    @Autowired val entityManager: EntityManager,
    @Autowired val statementRepository: StatementRepository,
    @Autowired val subjectService: SubjectService,
    @Autowired val statementService: StatementService,
    @Autowired val responseService: ResponseService,
    @Autowired val interactionService: InteractionService,
    @Autowired val responseRepository: ResponseRepository,
    @Autowired val sequenceService: SequenceService,
    @Autowired val functionalTestingService: FunctionalTestingService,
) {

    val persistentUnitUtil: PersistenceUnitUtil by lazy {
        entityManager.entityManagerFactory.persistenceUnitUtil
    }

    @Test
    fun `findAllByOwner - no assignment`() {
        val teacher = integrationTestingService.getTestTeacher()

        assignmentService.findAllByOwner(teacher)
            .tExpect {
                MatcherAssert.assertThat(it.totalElements, CoreMatchers.equalTo(0L))
                MatcherAssert.assertThat(it.totalPages, CoreMatchers.equalTo(0))
            }
    }

    @Test
    fun `findAllByOwner - with assignments`() {
        val teacher = integrationTestingService.getTestTeacher()
        createTestingData(teacher)

        assignmentService.findAllByOwner(teacher)
            .tExpect {
                MatcherAssert.assertThat(it.totalElements, CoreMatchers.equalTo(10L))
                MatcherAssert.assertThat(it.totalPages, CoreMatchers.equalTo(1))
            }

        assignmentService.findAllByOwner(teacher, PageRequest.of(0, 5))
            .tExpect {
                MatcherAssert.assertThat(it.totalPages, CoreMatchers.equalTo(2))
            }
    }

    private fun createTestingData(owner: User, n: Int = 10) {
        (1..n).forEach {
            assignmentService.save(
                Assignment(
                    title = "Assignment n°$it",
                    owner = owner
                )
            )
        }
    }

    @Test
    fun `get an existing assignment without fetching sequences`() {
        val teacher = integrationTestingService.getTestTeacher()
        val assignmentTitle = "An assignment"
        val assignmentId = assignmentService.save(
            Assignment(title = assignmentTitle, owner = teacher)
        ).id!!

        entityManager.clear()

        assignmentService.get(assignmentId).let {
            MatcherAssert.assertThat(it.id, CoreMatchers.equalTo(assignmentId))
            MatcherAssert.assertThat(it.title, CoreMatchers.equalTo(assignmentTitle))
            MatcherAssert.assertThat(
                persistentUnitUtil.isLoaded(it, "sequences"),
                CoreMatchers.equalTo(false)
            )
        }
    }

    @Test
    fun `get an existing assignment fetching sequences`() {
        val teacher = integrationTestingService.getTestTeacher()
        val assignmentTitle = "An assignment"
        val assignmentId = assignmentService.save(
            Assignment(title = assignmentTitle, owner = teacher)
        ).id!!

        entityManager.clear()

        assignmentService.get(assignmentId, true).let {
            MatcherAssert.assertThat(it.id, CoreMatchers.equalTo(assignmentId))
            MatcherAssert.assertThat(it.title, CoreMatchers.equalTo(assignmentTitle))
            MatcherAssert.assertThat(
                persistentUnitUtil.isLoaded(it, "sequences"),
                CoreMatchers.equalTo(true)
            )
        }
    }

    @Test
    fun `get an assignment for a user - OK`() {
        val teacher = integrationTestingService.getTestTeacher()
        val assignmentTitle = "An assignment"
        val assignmentId = assignmentService.save(
            Assignment(title = assignmentTitle, owner = teacher)
        ).id!!

        entityManager.clear()

        MatcherAssert.assertThat(assignmentService.get(teacher, assignmentId).id, CoreMatchers.equalTo(assignmentId))
    }

    @Test
    fun `try to get an assignment for a user that is owned by another user`() {
        val teacher = integrationTestingService.getTestTeacher()
        val assignmentTitle = "An assignment"
        val assignmentId = assignmentService.save(
            Assignment(title = assignmentTitle, owner = teacher)
        ).id!!

        entityManager.clear()

        assertThrows<AccessDeniedException> {
            assignmentService.get(integrationTestingService.getAnotherTestTeacher(), assignmentId)
        }
    }

    @Test
    fun `try to get an assignment with an invalid id`() {
        assertThrows<EntityNotFoundException> {
            assignmentService.get(1234567L)
        }
    }

    @Test
    fun `save a valid assignment`() {
        val assignment = Assignment("title", integrationTestingService.getTestTeacher())
        tWhen { assignmentService.save(assignment) }
            .tThen {
                MatcherAssert.assertThat(it.id, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(it.version, CoreMatchers.equalTo(0L))
                MatcherAssert.assertThat(it.globalId, CoreMatchers.notNullValue())
                MatcherAssert.assertThat(it.sequences.size, CoreMatchers.equalTo(0))
                MatcherAssert.assertThat(it.owner, CoreMatchers.equalTo(integrationTestingService.getTestTeacher()))
            }
    }

    @Test
    fun `an assignment must have a not blank title`() {
        val exception = assertThrows<ConstraintViolationException> {
            assignmentService.save(Assignment("", integrationTestingService.getTestTeacher()))
        }

        MatcherAssert.assertThat(exception.constraintViolations.size, CoreMatchers.equalTo(1))
        MatcherAssert.assertThat(
            exception.constraintViolations.elementAt(0).propertyPath.toString(),
            CoreMatchers.equalTo("title")
        )
    }

    @Test
    fun `delete an assignment with results`() {
        val subject = integrationTestingService.getAnyTestSubject()
        val teacher = subject.owner
        val initialNbAssignment = assignmentService.count()
        val initialNbStatement = subjectService.countAllStatement(subject)
        val assignmentTitle = "An assignment"
        val assignment = subjectService.addAssignment(
            subject,
            Assignment(assignmentTitle, teacher)
        )
        val student = integrationTestingService.getTestStudent()

        sequenceService.start(
            teacher,
            assignment.sequences.first(),
            ExecutionContext.FaceToFace,
            false,
            0,
            null
        )

        val interaction =
            Interaction(
                interactionType = InteractionType.ResponseSubmission,
                rank = 1,
                owner = student,
                sequence = assignment.sequences.first()
            )
        interactionService.interactionRepository.saveAndFlush(interaction)
        interactionService.start(student, interaction.id!!)

        val initialNbResponse = responseService.count(assignment.sequences.first(), 1)

        val choiceListSpecification = LearnerChoice(listOf<Int>(1, 3))
        val response = Response(
            learner = student,
            interaction = interaction,
            attempt = 1,
            explanation = "explanation",
            confidenceDegree = ConfidenceDegree.CONFIDENT,
            meanGrade = BigDecimal(1),
            learnerChoice = choiceListSpecification,
            score = BigDecimal(2),
            statement = interaction.sequence.statement
        )
        responseRepository.save(response)

        tWhen {
            subjectService.removeAssignment(subject.owner, assignment)
        }.tThen {
            MatcherAssert.assertThat(assignmentService.count(), CoreMatchers.equalTo(initialNbAssignment))
            MatcherAssert.assertThat(
                subjectService.countAllStatement(subject),
                CoreMatchers.equalTo((initialNbStatement))
            )
            MatcherAssert.assertThat(
                responseService.count(assignment.sequences.first(), 1),
                CoreMatchers.equalTo(initialNbResponse)
            )
        }
    }

    @Test
    fun `delete an assignment without results`() {
        val subject = integrationTestingService.getAnyTestSubject()
        val teacher = subject.owner
        val initialNbAssignment = assignmentService.count()
        val initialNbStatement = subjectService.countAllStatement(subject)
        val assignmentTitle = "An assignment"

        val assignment = subjectService.addAssignment(
            subject,
            Assignment(assignmentTitle, teacher)
        )

        entityManager.clear()

        tWhen {
            subjectService.removeAssignment(teacher, assignment)
        }.tThen {
            MatcherAssert.assertThat(assignmentService.count(), CoreMatchers.equalTo(initialNbAssignment))
            MatcherAssert.assertThat(
                subjectService.countAllStatement(subject),
                CoreMatchers.equalTo((initialNbStatement))
            )
        }
    }

    @Test
    fun `try to delete an assignment of another user`() {
        val teacher = integrationTestingService.getTestTeacher()
        val assignment = assignmentService.save(
            Assignment(title = "An assignment", owner = teacher)
        )

        assertThrows<IllegalArgumentException> {
            assignmentService.delete(integrationTestingService.getAnotherTestTeacher(), assignment)
        }
    }

    @Test
    fun `delete an assignment`() {
        val teacher = integrationTestingService.getTestTeacher()
        val assignment = assignmentService.save(
            Assignment(title = "An assignment", owner = teacher)
        )

        assertDoesNotThrow {
            assignmentService.delete(teacher, assignment)
        }
        assertThrows<EntityNotFoundException> {
            assignmentService.get(assignment.id!!)
        }
    }

    @Test
    fun `count sequences of empty assignment`() {
        val teacher = integrationTestingService.getTestTeacher()
        val assignment = assignmentService.save(
            Assignment(title = "An assignment", owner = teacher)
        )

        MatcherAssert.assertThat(
            assignmentService.countAllSequence(assignment),
            CoreMatchers.equalTo(0)
        )
    }

    @Test
    fun `count sequences of the provided test assignment`() {
        MatcherAssert.assertThat(
            assignmentService.countAllSequence(
                assignmentService.get(382)
            ),
            CoreMatchers.equalTo(2)
        )
    }

    @Test
    fun `creating sequences for all statement upon assignment creation`() {
        val subject = integrationTestingService.getAnyTestSubject()
        val initialCount = subjectService.countAllStatement(subject)

        tWhen {
            subjectService.addAssignment(
                subject,
                Assignment(title = "An assignment", owner = subject.owner, subject = subject)
            )
        }.tThen {
            MatcherAssert.assertThat(it.id, CoreMatchers.notNullValue())
            MatcherAssert.assertThat(
                assignmentService.countAllSequence(it),
                CoreMatchers.equalTo(initialCount)
            )
            MatcherAssert.assertThat(
                subjectService.countAllStatement(subject),
                CoreMatchers.equalTo(initialCount)
            ) // Should not add statement to subject
        }
    }

    @Test
    fun `add a sequence when a new statement is added to subject`() {
        val subject = integrationTestingService.getAnyTestSubject()
        val assignment = subjectService.addAssignment(
            subject,
            Assignment(title = "An assignment", owner = subject.owner, subject = subject)
        )
        val initialCount = assignmentService.countAllSequence(assignment)


        tWhen {
            subjectService.addStatement(
                subject,
                Statement.createDefaultStatement(subject.owner)
                    .title("Sequence n°1")
                    .content("Content 1")
            )
        }.tThen {
            MatcherAssert.assertThat(it.id, CoreMatchers.notNullValue())
            MatcherAssert.assertThat(
                assignmentService.countAllSequence(assignment),
                CoreMatchers.equalTo(initialCount + 1)
            )
            MatcherAssert.assertThat(
                subjectService.countAllStatement(subject),
                CoreMatchers.equalTo(initialCount + 1)
            )
        }
    }

    //TODO
    @Test
    fun `remove a sequence without results when a statement is removed from subject`() {
        val subject = integrationTestingService.getAnyTestSubject()
        val statement1 = subjectService.addStatement(
            subject,
            Statement.createDefaultStatement(subject.owner)
                .title("Sequence n°1")
                .content("Content 1")
        )
        val assignment = subjectService.addAssignment(
            subject,
            Assignment(title = "An assignment", owner = subject.owner, subject = subject)
        )
        val initialCount = assignmentService.countAllSequence(assignment)

        tWhen {
            subjectService.removeStatement(subject.owner, statement1)
        }.tThen {
            MatcherAssert.assertThat(
                "1er",
                subjectService.countAllStatement(subject),
                CoreMatchers.equalTo(initialCount - 1)
            )
            MatcherAssert.assertThat(
                "2eme",
                assignmentService.countAllSequence(assignment),
                CoreMatchers.equalTo(initialCount - 1)
            )

        }
    }

    @Test
    fun `not removing a sequence with result when a statement is removed from subject`() {
        val subject = integrationTestingService.getAnyTestSubject()
        val statement1 = statementService.get(618) // A statement linked to a sequence with results related
        val assignment = assignmentService.get(382)
        subject.addAssignment(assignment)

        val initialCountStatements = subjectService.countAllStatement(subject)
        val initialCount = assignmentService.countAllSequence(assignment)
        val stmtId = statement1.id!!

        tWhen {
            MatcherAssert.assertThat(
                " Before ",
                assignmentService.countAllSequence(assignment),
                CoreMatchers.equalTo(initialCount)
            )
            subjectService.removeStatement(subject.owner, statement1)
        }.tThen {
            MatcherAssert.assertThat(statementRepository.existsById(stmtId), CoreMatchers.equalTo(true))
            MatcherAssert.assertThat(
                " First ",
                assignmentService.countAllSequence(assignment),
                CoreMatchers.equalTo(initialCount)
            ) // The sequence must be kept
            MatcherAssert.assertThat(
                " Second ",
                subjectService.countAllStatement(subject),
                CoreMatchers.equalTo(initialCountStatements - 1)
            )
        }
    }


    @Test
    fun `findByGlobalId - not existing value`() {
        MatcherAssert.assertThat(
            assignmentService.findByGlobalId(UUID.randomUUID()),
            CoreMatchers.nullValue()
        )
    }

    @Test
    fun `findByGlobalId - existing value`() {
        val teacher = integrationTestingService.getTestTeacher()
        assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        ).tExpect {
            MatcherAssert.assertThat(
                assignmentService.findByGlobalId(it.globalId),
                CoreMatchers.equalTo(it)
            )
        }
    }

    @Test
    fun `register a student to an assignment`() {
        val teacher = integrationTestingService.getTestTeacher()
        val student = integrationTestingService.getTestStudent()

        val assignment = assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        )

        tWhen {
            assignmentService.findAllAssignmentsForLearner(student)
        }.tExpect {
            MatcherAssert.assertThat(it.isEmpty(), CoreMatchers.equalTo(true))
        }

        tWhen {
            assignmentService.registerUser(student, assignment)
            assignmentService.findAllAssignmentsForLearner(student)
        }.tExpect {
            MatcherAssert.assertThat(it, CoreMatchers.equalTo(listOf(assignment)))
        }
    }

    @Test
    fun `register the teacher to his own assignment do nothing`() {
        val teacher = integrationTestingService.getTestTeacher()

        val assignment = assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        )

        assertNull(assignmentService.registerUser(teacher, assignment))
    }

    @Test
    fun `test of removeSequence when the user isn't the owner`() {
        // given an assignment and a sequence with statement with attachment
        val assignment = integrationTestingService.getAnyAssignment()

        assertThrows<IllegalArgumentException> {
            assignmentService.removeSequence(integrationTestingService.getAnyUser(), assignment.sequences[0])
        }
    }

    @Test
    fun `test of updateAllSequenceRank when there is no sequence`() {
        // given an assignment with no sequence
        val assignment = integrationTestingService.getAnyAssignment()
        assignment.sequences.clear()

        // when updating the rank of the sequences
        assignmentService.updateAllSequenceRank(assignment)
        // then nothing happens
    }

    @Test
    fun `register a student to an assignment and check it`() {
        val teacher = integrationTestingService.getTestTeacher()

        // Given an assignment and a student
        val student = integrationTestingService.getTestStudent()
        val assignment = assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        )
        assertFalse(assignmentService.userIsRegisteredInAssignment(student, assignment))

        // When the student is registered to the assignment
        assignmentService.registerUser(student, assignment)

        // Then the student is registered to the assignment
        assertTrue(assignmentService.userIsRegisteredInAssignment(student, assignment))
    }

    @Test
    fun `test of getRegisteredUsers`() {
        val teacher = integrationTestingService.getTestTeacher()

        // Given an assignment and a student
        val student = integrationTestingService.getTestStudent()
        val assignment = assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        )
        assertEquals(0, assignmentService.getRegisteredUsers(assignment).size)

        // When one student is registered to the assignment
        assignmentService.registerUser(student, assignment)

        // Then one student is registered to the assignment
        assertEquals(1, assignmentService.getRegisteredUsers(assignment).size)

        // When another student is registered to the assignment
        val student2: User = integrationTestingService.getNLearners(1).first()
        assignmentService.registerUser(student2, assignment)

        // Then two students are registered to the assignment
        assertEquals(2, assignmentService.getRegisteredUsers(assignment).size)
    }

    @Test
    fun `test of getRegisteredUser`() {
        val teacher = integrationTestingService.getTestTeacher()

        // Given an assignment and a student
        val student = integrationTestingService.getTestStudent()
        val assignment = assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        )
        assertNull(assignmentService.getRegisteredUser(assignment, student))

        // When the student is registered to the assignment
        val learnerAssignment = assignmentService.registerUser(student, assignment)

        // Then we can get the registered user
        assertEquals(learnerAssignment, assignmentService.getRegisteredUser(assignment, student))
    }

    @Test
    fun `test of countRegisteredUsers`() {
        val teacher = integrationTestingService.getTestTeacher()

        // Given an assignment and a student
        val student = integrationTestingService.getTestStudent()
        val assignment = assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        )
        assertEquals(0, assignmentService.countRegisteredUsers(assignment))
        assertEquals(0, assignmentService.countRegisteredUsers(assignment.id!!))

        // When one student is registered to the assignment
        assignmentService.registerUser(student, assignment)

        // Then one student is registered to the assignment
        assertEquals(1, assignmentService.countRegisteredUsers(assignment))
        assertEquals(1, assignmentService.countRegisteredUsers(assignment.id!!))

        // When another student is registered to the assignment
        val student2: User = integrationTestingService.getNLearners(1).first()
        assignmentService.registerUser(student2, assignment)

        // Then two students are registered to the assignment
        assertEquals(2, assignmentService.countRegisteredUsers(assignment))
        assertEquals(2, assignmentService.countRegisteredUsers(assignment.id!!))
    }

    @Test
    @Disabled("This test is not working because the assignment is not updated")
    fun `test of findAllAssignmentUpdatedSince`() {
        val teacher = integrationTestingService.getTestTeacher()

        val before = LocalDateTime.now()

        // Given an assignment
        val assignment = assignmentService.save(
            Assignment(
                title = "An assignment",
                owner = teacher
            )
        )
        Thread.sleep(1000) // To ensure, the assignment is updated before the after date
        val after = LocalDateTime.now()

        assertTrue(assignmentService.findAllAssignmentUpdatedSince(before).contains(assignment))
        assertFalse(assignmentService.findAllAssignmentUpdatedSince(after).contains(assignment))

        assignmentService.touch(assignment)
        Thread.sleep(1000) // To ensure, the assignment is updated before the check

        assertTrue(assignmentService.findAllAssignmentUpdatedSince(before).contains(assignment))
        assertTrue(assignmentService.findAllAssignmentUpdatedSince(after).contains(assignment))
    }

    @Test
    fun `test of getCoursesAssignmentsMap`() {
        val teacher = integrationTestingService.getTestTeacher()
        val course1 = functionalTestingService.createCourse(teacher, "Course 1")
        val course2 = functionalTestingService.createCourse(teacher, "Course 2")
        val subject1 = functionalTestingService.createSubject(teacher, course1, "Subject 1")
        val subject2 = functionalTestingService.createSubject(teacher, course2, "Subject 2")

        val assignment1 = functionalTestingService.createAssignment(subject1, "Assignment 1")
        val assignment2 = functionalTestingService.createAssignment(subject2, "Assignment 2")
        val assignment3 = functionalTestingService.createAssignment(subject1, "Assignment 3")
        assignment1.owner = teacher
        assignment2.owner = teacher
        assignment3.owner = teacher
        assignmentService.save(assignment1)
        assignmentService.save(assignment2)
        assignmentService.save(assignment3)


        val result = assignmentService.getCoursesAssignmentsMap(listOf(assignment1, assignment2, assignment3))

        assertEquals(2, result.size)
        assertEquals(2, result[course1]?.size)
        assertEquals(1, result[course2]?.size)
        assertTrue(result[course1]!!.containsAll(listOf(assignment1, assignment3)))
        assertEquals(assignment2, result[course2]?.get(0))
    }

}