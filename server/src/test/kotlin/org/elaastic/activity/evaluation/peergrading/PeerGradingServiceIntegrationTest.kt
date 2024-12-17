package org.elaastic.activity.evaluation.peergrading

import org.elaastic.activity.evaluation.peergrading.draxo.DraxoEvaluation
import org.elaastic.activity.evaluation.peergrading.draxo.DraxoPeerGrading
import org.elaastic.activity.evaluation.peergrading.draxo.criteria.Criteria
import org.elaastic.activity.evaluation.peergrading.draxo.option.OptionId
import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseService
import org.elaastic.assignment.LearnerAssignment
import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.material.instructional.statement.StatementRepository
import org.elaastic.material.instructional.subject.SubjectRepository
import org.elaastic.material.instructional.subject.SubjectService
import org.elaastic.moderation.ReportReason
import org.elaastic.moderation.UtilityGrade
import org.elaastic.player.dashboard.DashboardModelFactory
import org.elaastic.questions.assignment.sequence.peergrading.draxo.DraxoPeerGradingService
import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.test.FunctionalTestingService
import org.elaastic.test.IntegrationTestingService
import org.elaastic.test.directive.tGiven
import org.elaastic.test.directive.tThen
import org.elaastic.test.directive.tWhen
import org.elaastic.test.getAnyAssignment
import org.elaastic.test.getAnySequence
import org.elaastic.test.interpreter.command.Phase
import org.elaastic.user.RoleService
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.math.BigDecimal
import javax.persistence.EntityManager
import javax.transaction.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class PeerGradingServiceIntegrationTest(
    @Autowired val functionalTestingService: FunctionalTestingService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val integrationTestingService: IntegrationTestingService,
    @Autowired val responseService: ResponseService,
    @Autowired val entityManager: EntityManager,
    @Autowired val roleService: RoleService,
    @Autowired val interactionService: InteractionService,
    @Autowired val peerGradingRepository: PeerGradingRepository,
    @Autowired val draxoPeerGradingService: DraxoPeerGradingService,
    @Autowired val statementRepository: StatementRepository,
    @Autowired val subjectRepository: SubjectRepository,
    @Autowired val subjectService: SubjectService,
    @Autowired val dashboardModelFactory: DashboardModelFactory,
) {

    @Test
    fun `save a DRAXO Peer Grading`() {
        val teacher = integrationTestingService.getTestTeacher()
        val subject = functionalTestingService.generateSubjectWithQuestionsAndAssignmentsReadyToPratice(teacher)
        val sequence = subject.getAnyAssignment().getAnySequence()

        val learners = integrationTestingService.getNLearners(3)
        functionalTestingService.startSequence(sequence)
        functionalTestingService.submitRandomResponses(Phase.PHASE_1, learners, sequence)
        functionalTestingService.nextPhase(sequence)


        val grader = learners.first()
        val response = responseService.findAll(sequence).getWithoutFake(1).first()

        val explanation = "That do no really answer the question"
        DraxoEvaluation()
            .addEvaluation(Criteria.D, OptionId.YES)
            .addEvaluation(Criteria.R, OptionId.PARTIALLY, explanation)
            .tWhen { draxoEvaluation ->
                val peerGrading = draxoPeerGradingService.createOrUpdateDraxo(grader, response, draxoEvaluation, false)
                entityManager.flush()
                entityManager.clear()
                peerGrading
            }
            .tThen {
                assertThat(it.id, notNullValue())
                assertThat(it.version, equalTo(0L))
                assertThat(it.dateCreated, notNullValue())
                assertThat(it.lastUpdated, notNullValue())
                assertThat(it.grader, equalTo(grader))
                assertThat(it.response, equalTo(response))
                assertThat(it.criteriaD, equalTo(OptionId.YES))
                assertThat(it.criteriaR, equalTo(OptionId.PARTIALLY))
                assertThat(it.criteriaA, nullValue())
                assertThat(it.criteriaX, nullValue())
                assertThat(it.criteriaO, nullValue())
                assertThat(it.annotation, equalTo(explanation))
            }
    }


    @Test
    fun `a student can report a peerGrading`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val reporter = integrationTestingService.getTestStudent()
        response.learner = reporter
        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            peerGrading = LikertPeerGrading(
                grade = BigDecimal(2),
                annotation = "Reportable content",
                grader = grader,
                response = response
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We report it") {
            val reportReason: List<String> = listOf(ReportReason.INCOHERENCE.name)
            assertDoesNotThrow {
                peerGradingService.updateReport(reporter, peerGrading, reportReason)
            }
        }.tThen("the peerGrading is reported") {
            assertNotNull(peerGrading.reportReasons)
            assertTrue(peerGrading.reportReasons!!.contains(ReportReason.INCOHERENCE.name))
        }
    }

    @Test
    fun `can't report an draxo peerGrading where there is no content to report`() {
        val response = integrationTestingService.getAnyResponse()
        val reporter = integrationTestingService.getTestStudent()
        response.learner = reporter
        lateinit var peerGrading: DraxoPeerGrading
        tGiven("A draxo peerGrading without a content to report") {
            val grader = integrationTestingService.getAnyUser()
            // A peerGrading where there is no content to report, is a peerGrading where there is no comment
            // We can achieve the zero-comment peerGrading by giving all the criteria a positive option
            peerGrading = DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.YES)
                    .addEvaluation(Criteria.X, OptionId.YES)
                    .addEvaluation(Criteria.O, OptionId.NO),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
            // We check that the peerGrading has no comment
            assertNull(peerGrading.getDraxoEvaluation().getExplanation(), "The peerGrading should not have a comment")
        }.tWhen("We try report it") {
            val reportReason: List<String> = listOf(ReportReason.INCOHERENCE.name)
            reportReason
        }.tThen("We should get an exception") { reportReason ->
            assertThrows(IllegalStateException::class.java) {
                peerGradingService.updateReport(reporter, peerGrading, reportReason)
            }
        }.tThen("The peerGrading is not reported") {
            assertNull(peerGrading.reportReasons)
        }
    }

    @Test
    fun `report a Draxo peerGrading with an explanation`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val reporter = integrationTestingService.getTestStudent()
        response.learner = reporter
        lateinit var peerGrading: DraxoPeerGrading
        val comment = "This is a comment to explain the reason of the report"

        tGiven("A draxo peer grading") {
            peerGrading = DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.NO, "Reportable content"),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We report it") {
            val reportReason: List<String> = listOf(ReportReason.INCOHERENCE.name)
            assertDoesNotThrow {
                peerGradingService.updateReport(reporter, peerGrading, reportReason, comment)
            }
            reportReason
        }.tThen("the peerGrading is reported") { reportReason ->
            assertNotNull(peerGrading.reportReasons)
            assertEquals(comment, peerGrading.reportComment)
            assertTrue(peerGrading.reportReasons!!.contains(reportReason.first()))
        }
    }

    @Test
    fun `a report with the reason 'OTHER' and without comment should not be possible`() {
        val response = integrationTestingService.getAnyResponse()
        val reporter = integrationTestingService.getTestStudent()
        response.learner = reporter
        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            val grader = integrationTestingService.getAnyUser()
            peerGrading = LikertPeerGrading(
                grade = BigDecimal(2),
                annotation = "Reportable content",
                grader = grader,
                response = response
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We try report it with the reason 'OTHER' without a comment (e.i. empty comment or comment null)") {
            val reportReason: List<String> = listOf(ReportReason.OTHER.name)
            reportReason
        }.tThen("We should get an exception") { reportReason ->
            val emptyComment = ""
            assertThrows(IllegalArgumentException::class.java) {
                peerGradingService.updateReport(reporter, peerGrading, reportReason, emptyComment)
            }
            val nullComment = null
            assertThrows(IllegalArgumentException::class.java) {
                peerGradingService.updateReport(reporter, peerGrading, reportReason, nullComment)
            }
        }.tThen("The peerGrading is not reported") {
            assertNull(peerGrading.reportReasons)
        }
    }

    @Test
    fun `report a peerGrading with the reason 'OTHER' and a comment`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val reporter = integrationTestingService.getTestStudent()
        val comment = "This is a comment to explain the reason of the report"
        response.learner = reporter
        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            peerGrading = LikertPeerGrading(
                grade = BigDecimal(2),
                annotation = "Reportable content",
                grader = grader,
                response = response
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We report it with the reason 'OTHER' and a comment") {
            val reportReason: List<String> = listOf(ReportReason.OTHER.name)
            peerGradingService.updateReport(reporter, peerGrading, reportReason, comment)
        }.tThen("the peerGrading is reported") {
            assertNotNull(peerGrading.reportReasons)
            assertEquals(comment, peerGrading.reportComment)
            assertNotNull(peerGrading.reportReasons)
            assertTrue(peerGrading.reportReasons!!.contains(ReportReason.OTHER.name))
        }
    }

    @Test
    fun `a student can give to a peerGrading a UtilityGrade`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val learner = integrationTestingService.getTestStudent()
        response.learner = learner
        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            peerGrading = LikertPeerGrading(
                grade = BigDecimal(2),
                annotation = "Reportable content",
                grader = grader,
                response = response
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We give it a UtilityGrade") {
            val utilityGrade = UtilityGrade.AGREE
            peerGradingService.updateUtilityGrade(learner, peerGrading, utilityGrade)
        }.tThen("the peerGrading has a UtilityGrade") {
            assertNotNull(peerGrading.utilityGrade)
            assertEquals(UtilityGrade.AGREE, peerGrading.utilityGrade)
        }
    }

    @Test
    fun `a sudent who don't own the response can't report or give an UtilityGrade`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val learner = integrationTestingService.getTestStudent()
        val reporterWhoDontOwnTheResponse = integrationTestingService.getNLearners(1).first()
        response.learner = learner
        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            peerGrading = LikertPeerGrading(
                grade = BigDecimal(2),
                annotation = "Reportable content",
                grader = grader,
                response = response
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
            // We check that the reporterWhoDontOwnTheResponse is not the learner
            assertNotEquals(reporterWhoDontOwnTheResponse, learner)
        }.tWhen("We try to report it") {
            val reportReason: List<String> = listOf(ReportReason.INCOHERENCE.name)
            assertThrows(IllegalAccessException::class.java) {
                peerGradingService.updateReport(reporterWhoDontOwnTheResponse, peerGrading, reportReason)
            }
        }.tWhen("We try to give it a UtilityGrade") {
            val utilityGrade = UtilityGrade.AGREE
            assertThrows(IllegalAccessException::class.java) {
                peerGradingService.updateUtilityGrade(reporterWhoDontOwnTheResponse, peerGrading, utilityGrade)
            }
        }
    }

    @Test
    fun `a teacher can hide a peerGrading`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val teacher = integrationTestingService.getTestTeacher()
        response.interaction.sequence.assignment!!.owner = teacher

        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            peerGrading = LikertPeerGrading(
                grade = BigDecimal(2),
                annotation = "Reportable content",
                grader = grader,
                response = response
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We hide it") {
            assertDoesNotThrow({
                peerGradingService.markAsHidden(teacher, peerGrading)
            }, "A teacher can hide a peerGrading")
        }.tThen("the peerGrading is hidden") {
            assertTrue(peerGrading.hiddenByTeacher)
        }.tWhen("a student (or a teacher that doesn't own the sequence ) try hidding a peerGrading") {}
            .tThen("an exception is thrown") {
                assertThrows<IllegalAccessException> {
                    peerGradingService.markAsHidden(grader, peerGrading)
                }
                val anotherTeacher = integrationTestingService.getAnyUser()
                anotherTeacher.addRole(roleService.roleTeacher())
                assertNotEquals(teacher, anotherTeacher)
                assertThrows<IllegalAccessException> {
                    peerGradingService.markAsHidden(anotherTeacher, peerGrading)
                }
            }
    }

    @Test
    fun `a teacher unhide a peerGrading`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val teacher = response.interaction.sequence.assignment!!.owner
        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            peerGrading = DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation().addEvaluation(Criteria.D, OptionId.NO),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.save(it)
                    it
                }
        }.tWhen("A teacher try unhiding a peerGrading that is not hidden") {}.tThen("Nothing is thrown") {
            assertDoesNotThrow({
                peerGradingService.markAsShow(teacher, peerGrading)
            }, "A teacher can unhide a peerGrading that is not hidden")
        }.tWhen("We hide it") {
            peerGradingService.markAsHidden(teacher, peerGrading)
            assertTrue(peerGrading.hiddenByTeacher)
        }.tWhen("We unhide it") {
            assertDoesNotThrow({
                peerGradingService.markAsShow(teacher, peerGrading)
            }, "A teacher can unhide a peerGrading")
        }.tThen("the peerGrading is unhidden") {
            assertFalse(peerGrading.hiddenByTeacher)
        }
    }

    @Test
    fun `a student (or a student who doesn't own the sequence) can't unhide a peerGrading`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val teacher = integrationTestingService.getTestTeacher()
        response.interaction.sequence.assignment!!.owner = teacher
        lateinit var peerGrading: PeerGrading
        tGiven("A peer grading") {
            peerGrading = LikertPeerGrading(
                grade = BigDecimal(2),
                annotation = "Reportable content",
                grader = grader,
                response = response
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We hide it") {
            peerGradingService.markAsHidden(teacher, peerGrading)
            assertTrue(peerGrading.hiddenByTeacher)
        }.tWhen("A student try unhiding a peerGrading") {}.tThen("an exception is thrown") {
            assertNotEquals(teacher, grader)
            assertThrows<IllegalAccessException> {
                peerGradingService.markAsShow(grader, peerGrading)
            }
            val anotherStudent = integrationTestingService.getNLearners(1).first()
            anotherStudent.addRole(roleService.roleStudent())
            assertNotEquals(grader, anotherStudent)
            assertNotEquals(teacher, anotherStudent)
            assertThrows<IllegalAccessException> {
                peerGradingService.markAsShow(anotherStudent, peerGrading)
            }
        }.tWhen("a teacher that doesn't own the sequence try unhiding a peerGrading") {}
            .tThen("an exception is thrown") {
                val anotherTeacher = integrationTestingService.getAnyUser()
                anotherTeacher.addRole(roleService.roleTeacher())
                assertNotEquals(teacher, anotherTeacher)
                assertThrows<IllegalAccessException> {
                    peerGradingService.markAsShow(anotherTeacher, peerGrading)
                }
            }
    }

    @Test
    fun `test of getDraxoPeerGrading`() {
        val response = integrationTestingService.getAnyResponse()
        val grader = integrationTestingService.getAnyUser()
        val teacher = integrationTestingService.getTestTeacher()
        response.interaction.sequence.assignment!!.owner = teacher
        lateinit var peerGrading: DraxoPeerGrading
        tGiven("A draxo peer grading") {
            peerGrading = DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation()
                    .addEvaluation(Criteria.D, OptionId.YES)
                    .addEvaluation(Criteria.R, OptionId.YES)
                    .addEvaluation(Criteria.A, OptionId.YES)
                    .addEvaluation(Criteria.X, OptionId.YES)
                    .addEvaluation(Criteria.O, OptionId.NO),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.saveAndFlush(it)
                    entityManager.clear()
                    it
                }
        }.tWhen("We get the draxo peer grading") {
            val draxoPeerGrading = peerGrading.id?.let { it1 -> draxoPeerGradingService.getDraxoPeerGrading(it1) }
            draxoPeerGrading
        }.tThen("the draxo peer grading is returned") { draxoPeerGrading ->
            assertNotNull(draxoPeerGrading)
            assertEquals(peerGrading.id, draxoPeerGrading!!.id)
        }.tWhen("we get with an inexisting id") {}.tThen("we get an exception") {
            assertThrows<Exception> {
                draxoPeerGradingService.getDraxoPeerGrading(-1)
            }
        }
    }

    @Test
    fun `test of countEvaluationsMadeByUsers`() {
        // Given
        val learners = integrationTestingService.getNLearners(2)
        val grader = learners[0]
        val learner = learners[1]
        val subject = functionalTestingService.createSubject(integrationTestingService.getTestTeacher())
        functionalTestingService.addQuestion(subject, QuestionType.OpenEnded)
        val assignement = functionalTestingService.createAssignment(subject)
        val sequence = assignement.sequences.first()

        val learnersAssignementList: List<LearnerAssignment> = listOf(
            LearnerAssignment(grader, assignement),
            LearnerAssignment(learner, assignement),
        )
        val mapUserToEvaluatioCount: MutableMap<LearnerAssignment, Long> =
            learnersAssignementList.associateWith { 0.toLong() }.toMutableMap()
        assertEquals(
            mapUserToEvaluatioCount,
            peerGradingService.countEvaluationsMadeByUsers(learnersAssignementList, sequence),
            ""
        ) //No evaluations made by the users, because the submission interaction isn't initialized

        functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace) // Phase 1 (Start)

        val response = functionalTestingService.submitResponse(
            Phase.PHASE_1,
            learner,
            sequence,
            true,
            ConfidenceDegree.CONFIDENT,
            "response"
        )
        functionalTestingService.nextPhase(sequence) // Phase 2 (Evaluation)

        assertEquals(
            mapUserToEvaluatioCount,
            peerGradingService.countEvaluationsMadeByUsers(learnersAssignementList, sequence),
            "No evaluations made by the users"
        )
        assertEquals(
            emptyMap<LearnerAssignment, Long>(),
            peerGradingService.countEvaluationsMadeByUsers(emptyList<LearnerAssignment>(), sequence),
            "No user given"
        )

        tGiven("A peerGrading given by the grader") {
            DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation().addEvaluation(Criteria.D, OptionId.NO, "explanation"),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.save(it)
                    it
                }
        }.tWhen("We get all the evaluations for the response") {
            val evaluations = peerGradingService.findAllEvaluation(grader, response.interaction.sequence)
            evaluations
        }.tThen("the peerGrading is returned") { evaluations ->
            mapUserToEvaluatioCount[learnersAssignementList[0]] = 1
            assertEquals(
                mapUserToEvaluatioCount,
                peerGradingService.countEvaluationsMadeByUsers(learnersAssignementList, sequence)
            )
            assertEquals(1, evaluations.count())
            assertEquals(grader, evaluations.first().grader)
            assertEquals(response, evaluations.first().response)
        }
    }

    @Test
    fun `test of learnerToIfTheyAnswer`() {
        // Given
        val learners = integrationTestingService.getNLearners(3)
        val subject = functionalTestingService.createSubject(integrationTestingService.getTestTeacher())
        functionalTestingService.addQuestion(subject, QuestionType.OpenEnded)
        val assignement = functionalTestingService.createAssignment(subject)
        val sequence = assignement.sequences.first()

        tGiven("A started sequence") {
            functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)
            val learnersAssignementList: List<LearnerAssignment> = learners.map { LearnerAssignment(it, assignement) }

            assertEquals(
                learnersAssignementList.associateWith { false },
                peerGradingService.learnerToIfTheyAnswer(learnersAssignementList, sequence),
                "No response given by the users"
            )
            learnersAssignementList
        }.tWhen("Two learner answer") {
            functionalTestingService.submitResponse(
                Phase.PHASE_1,
                learners[0],
                sequence,
                true,
                ConfidenceDegree.CONFIDENT,
                "response"
            )
            functionalTestingService.submitResponse(
                Phase.PHASE_1,
                learners[1],
                sequence,
                true,
                ConfidenceDegree.CONFIDENT,
                "response"
            )
            it //learnersAssignementList
        }.tThen("the two learner who answer, are mark as so") { learnersAssignementList ->
            val mapUserToIfTheyAnswerExpected: MutableMap<LearnerAssignment, Boolean> =
                learnersAssignementList.associateWith { false }.toMutableMap()
            mapUserToIfTheyAnswerExpected[learnersAssignementList[0]] = true
            mapUserToIfTheyAnswerExpected[learnersAssignementList[1]] = true
            assertEquals(
                mapUserToIfTheyAnswerExpected,
                peerGradingService.learnerToIfTheyAnswer(learnersAssignementList, sequence)
            )
            learnersAssignementList
        }.tThen { learnersAssignementList ->
            val responses: List<Response> = interactionService.findAllResponsesBySequenceOrderById(sequence)
            assertTrue(dashboardModelFactory.learnerHasAnswer(responses, learnersAssignementList[0]))
        }

    }

    @Test
    fun `test of findAllDraxoPeerGradingReportedNotHidden`() {
        // Given
        val learners = integrationTestingService.getNLearners(2)
        val subject = functionalTestingService.createSubject(integrationTestingService.getTestTeacher())
        functionalTestingService.addQuestion(subject, QuestionType.OpenEnded)
        val assignement = functionalTestingService.createAssignment(subject)
        val sequence = assignement.sequences.first()

        tGiven("A started sequence") {
            functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)
            val learnersAssignementList: List<LearnerAssignment> = learners.map { LearnerAssignment(it, assignement) }

            assertEquals(
                0,
                draxoPeerGradingService.countAllDraxoPeerGradingReported(sequence, false),
                "No peerGrading reported"
            )
            assertEquals(
                emptyList<DraxoPeerGrading>(),
                draxoPeerGradingService.findAllDraxoPeerGradingReported(sequence),
                "No peerGrading reported"
            )
            learnersAssignementList
        }.tWhen("Two learner answer and report a peerGrading") { learnerAssignments ->
            val response = functionalTestingService.submitResponse(
                Phase.PHASE_1,
                learners[0],
                sequence,
                true,
                ConfidenceDegree.CONFIDENT,
                "response"
            )
            val grader = learners[1]
            val peerGrading = DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation().addEvaluation(Criteria.D, OptionId.NO, "explanation"),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.save(it)
                    it
                }
            peerGradingService.updateReport(learners[0], peerGrading, listOf(ReportReason.INCOHERENCE.name))
            learnerAssignments //learnersAssignementList
        }.tThen("the two learner who answer, are mark as so") {
            val peerGradingReportedNotHidden =
                draxoPeerGradingService.findAllDraxoPeerGradingReported(sequence)
            assertEquals(1, peerGradingReportedNotHidden.size)
            assertEquals(learners[1], peerGradingReportedNotHidden.first().grader)
            assertEquals(learners[0], peerGradingReportedNotHidden.first().response.learner)

            assertEquals(
                peerGradingReportedNotHidden.size,
                draxoPeerGradingService.countAllDraxoPeerGradingReported(sequence, false)
            )
        }
    }

    @Test
    fun `test of removereport`() {
        // Given
        val learners = integrationTestingService.getNLearners(2)
        val teacher = integrationTestingService.getTestTeacher()
        val subject = functionalTestingService.createSubject(teacher)
        functionalTestingService.addQuestion(subject, QuestionType.OpenEnded)
        val assignement = functionalTestingService.createAssignment(subject)
        val sequence = assignement.sequences.first()

        tGiven("A started sequence") {
            functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)
            val learnersAssignementList: List<LearnerAssignment> = learners.map { LearnerAssignment(it, assignement) }

            assertEquals(
                0,
                draxoPeerGradingService.countAllDraxoPeerGradingReported(sequence, false),
                "No peerGrading reported"
            )
            assertEquals(
                emptyList<DraxoPeerGrading>(),
                draxoPeerGradingService.findAllDraxoPeerGradingReported(sequence),
                "No peerGrading reported"
            )
            learnersAssignementList
        }.tWhen("Two learner answer and report a peerGrading") { learnerAssignments ->
            val response = functionalTestingService.submitResponse(
                Phase.PHASE_1,
                learners[0],
                sequence,
                true,
                ConfidenceDegree.CONFIDENT,
                "response"
            )
            val grader = learners[1]
            val peerGrading = DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation().addEvaluation(Criteria.D, OptionId.NO, "explanation"),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.save(it)
                    it
                }
            peerGradingService.updateReport(learners[0], peerGrading, listOf(ReportReason.INCOHERENCE.name))
            peerGrading
        }.tThen("the peerGrading is reported") {
            assertNotNull(it.reportReasons)
            it
        }.tWhen("We remove the report") {
            assertThrows(IllegalAccessException::class.java, {
                peerGradingService.removeReport(learners[0], it.id!!)
            }, "A student can't remove a report")
            assertDoesNotThrow({
                peerGradingService.removeReport(teacher, it)
            }, "A teacher can remove a report")
            it
        }.tThen("the peerGrading is not reported") {
            assertNull(it.reportReasons)
            assertNull(it.reportComment)
        }
    }

    @Test
    fun `test of markAsRestored`() {
        // Given
        val learners = integrationTestingService.getNLearners(2)
        val teacher = integrationTestingService.getTestTeacher()
        val subject = functionalTestingService.createSubject(teacher)
        functionalTestingService.addQuestion(subject, QuestionType.OpenEnded)
        val assignement = functionalTestingService.createAssignment(subject)
        val sequence = assignement.sequences.first()

        tGiven("A started sequence") {
            functionalTestingService.startSequence(sequence, ExecutionContext.FaceToFace)
        }.tWhen("A learner answer and another evaluate it") {
            val response = functionalTestingService.submitResponse(
                Phase.PHASE_1,
                learners[0],
                sequence,
                true,
                ConfidenceDegree.CONFIDENT,
                "response"
            )
            val grader = learners[1]
            val peerGrading = DraxoPeerGrading(
                grader = grader,
                response = response,
                draxoEvaluation = DraxoEvaluation().addEvaluation(Criteria.D, OptionId.NO, "explanation"),
                lastSequencePeerGrading = false
            )
                .tWhen {
                    peerGradingRepository.save(it)
                    it
                }
            peerGrading
        }.tWhen("The peergrading is removed") {
            assertThrows(IllegalAccessException::class.java, {
                peerGradingService.markAsRemoved(learners[0], it)
            }, "A student can't remove a report")

            assertDoesNotThrow({
                peerGradingService.markAsRemoved(teacher, it)
            }, "A teacher can remove a report")
            it
        }.tThen("The peergrading is removed") {
            assertTrue(it.removedByTeacher)
            it
        }.tWhen("The peergrading is restored") {
            assertThrows(IllegalAccessException::class.java, {
                peerGradingService.markAsRestored(learners[0], it)
            }, "A student can't remove a report")

            assertDoesNotThrow({
                peerGradingService.markAsRestored(teacher, it)
            }, "A teacher can remove a report")
            it
        }.tThen("The peergrading is restored") {
            assertFalse(it.removedByTeacher)
        }
    }
}