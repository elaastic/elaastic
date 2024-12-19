package org.elaastic.test

import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseService
import org.elaastic.assignment.Assignment
import org.elaastic.assignment.AssignmentService
import org.elaastic.assignment.ReadyForConsolidation
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.course.Course
import org.elaastic.material.instructional.course.CourseService
import org.elaastic.material.instructional.question.*
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.material.instructional.subject.Subject
import org.elaastic.material.instructional.subject.SubjectService
import org.elaastic.player.PlayerController
import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.State
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.phase.evaluation.EvaluationPhaseConfig
import org.elaastic.test.interpreter.command.*
import org.elaastic.user.User
import org.elaastic.user.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import kotlin.random.Random


private const val THE_SEQUENCE_HAS_NO_ACTIVE_INTERACTION = "The sequence has no active interaction"

@Service
@Transactional
class FunctionalTestingService(
    @Autowired val courseService: CourseService,
    @Autowired val subjectService: SubjectService,
    @Autowired val sequenceService: SequenceService,
    @Autowired val assignmentService: AssignmentService,
    @Autowired val userRepository: UserRepository,
    @Autowired val responseService: ResponseService,
    @Autowired val peerGradingService: PeerGradingService,
    @Autowired val interactionService: InteractionService,
) {

    fun createCourse(user: User, title: String = "Default course title") =
        courseService.save(
            Course(
                owner = MaterialUser.fromElaasticUser(user),
                title = title,
            )
        )

    fun createSubject(
        user: User,
        title: String = "Default subject title"
    ) =
        subjectService.save(
            Subject(title, MaterialUser.fromElaasticUser(user))
        )

    fun createSubject(user: User, course: Course, title: String = "Default subject title") =
        createSubject(user, title)
            .also { courseService.addSubjectToCourse(MaterialUser.fromElaasticUser(user), it, course) }

    fun createAssignment(subject: Subject, title: String = "Default assignment title") =
        subjectService.addAssignment(
            subject,
            Assignment(
                owner = User.fromMaterialUser(subject.owner),
                title = title,
            )
        )

    fun createAssignmentReadyToPractice(subject: Subject, title: String = "Default assignment title") =
        subjectService.addAssignment(
            subject,
            Assignment(
                owner = User.fromMaterialUser(subject.owner),
                title = title,
                readyForConsolidation = ReadyForConsolidation.AfterTeachings
            )
        )

    fun createAssignmentReadyImmediatelyForPractice(subject: Subject, title: String = "Default assignment title") =
        subjectService.addAssignment(
            subject,
            Assignment(
                owner = User.fromMaterialUser(subject.owner),
                title = title,
                readyForConsolidation = ReadyForConsolidation.Immediately
            )
        )

    fun addQuestion(subject: Subject, statement: Statement) =
        subjectService.addStatement(subject, statement)

    fun addQuestion(subject: Subject, questionType: QuestionType) =
        subject.statements.size.let { questionIndex ->
            addQuestion(
                subject,
                generateStatement(
                    owner = User.fromMaterialUser(subject.owner),
                    questionType = questionType,
                    questionIndex = questionIndex
                )
            )

        }

    fun generateStatement(
        owner: User,
        questionType: QuestionType,
        questionIndex: Int
    ): Statement {
        val title = "Question ${questionIndex + 1} - $questionType"
        val content = "Content of question ${questionIndex + 1}"
        val expectedExplanation = "Expected answer to question ${questionIndex + 1}"

        return Statement(
            title = title,
            content = content,
            owner = MaterialUser.fromElaasticUser(owner),
            questionType = questionType,
            rank = questionIndex,
            expectedExplanation = expectedExplanation,
            choiceSpecification = when (questionType) {
                QuestionType.OpenEnded -> null

                QuestionType.ExclusiveChoice ->
                    ExclusiveChoiceSpecification(
                        nbCandidateItem = 4,
                        expectedChoice = ChoiceItem(3, 100f)
                    )

                QuestionType.MultipleChoice ->
                    MultipleChoiceSpecification(
                        nbCandidateItem = 3,
                        expectedChoiceList = listOf(ChoiceItem(1, 50f), ChoiceItem(2, 50f))
                    )
            }
        )
    }

    /**
     * Generate a subject with questions and assignments ready to practice
     */
    fun generateSubjectWithQuestionsAndAssignmentsReadyToPratice(user: User) =
        generateSubjectWithQuestionsAndAssignments(user, ReadyForConsolidation.AfterTeachings)

    fun generateSubjectWithQuestionsAndAssignments(user: User, praticeStatus: ReadyForConsolidation = ReadyForConsolidation.NotAtAll) =
        createSubject(user, "Test subject ${LocalDate.now()}")
            // Questions
            .also { subject ->
                addQuestion(subject, QuestionType.OpenEnded)
                addQuestion(subject, QuestionType.ExclusiveChoice)
                addQuestion(subject, QuestionType.MultipleChoice)
            }
            // Assignments
            .also { subject ->
                listOf("Face-to-face", "Blended", "Distant").forEach {
                    when (praticeStatus) {
                        ReadyForConsolidation.NotAtAll -> createAssignment(subject, "$it Test Assignment")
                        ReadyForConsolidation.Immediately -> createAssignmentReadyImmediatelyForPractice(subject, "$it Test Assignment")
                        ReadyForConsolidation.AfterTeachings -> createAssignmentReadyToPractice(subject, "$it Test Assignment")
                    }
                }
            }

    /**
     * Generate an Assignment ready to use
     */
    fun generateAssignement(user: User): Assignment {
        val subject = generateSubjectWithQuestionsAndAssignments(user)
        return subject.assignments.first()
    }

    /**
     * Generate a sequence ready to use
     */
    fun generateSequence(user: User): Sequence {
        val assignment = generateAssignement(user)
        return assignment.sequences.first()
    }

    fun startSequence(
        sequence: Sequence,
        executionContext: ExecutionContext = ExecutionContext.FaceToFace,
        studentsProvideExplanation: Boolean = true,
        nbResponseToEvaluate: Int = 3
    ) =
        sequenceService.start(
            sequence.owner,
            sequence,
            executionContext,
            studentsProvideExplanation,
            nbResponseToEvaluate,
            EvaluationPhaseConfig.ALL_AT_ONCE,
        )

    fun submitResponse(
        phase: Phase,
        user: User,
        sequence: Sequence,
        correct: Boolean,
        confidenceDegree: ConfidenceDegree,
        explanation: String? = null
    ): Response {
        registerUserIfNeeded(user, sequence)

        // Check preconditions depending on phase
        val interaction = sequenceService.getActiveInteractionForLearner(sequence, user)
        require(interaction != null) { "The interaction must not be null after registration" }

        when (phase) {
            Phase.PHASE_1 -> {
                require(interaction.isResponseSubmission()) {
                    "The active interaction for this sequence and this learner is not a response submission"
                }
                require(
                    !responseService.hasResponseForUser(
                        user,
                        sequence,
                        1
                    )
                ) { "This user has already submitted its 1st attempt " }
            }

            Phase.PHASE_2 -> {
                require(interaction.isEvaluation()) {
                    "The active interaction for this sequence and this learner is not evaluation"
                }
                require(sequence.isSecondAttemptAllowed()) { "This sequence does not allow a 2nd attempt" }
                require(
                    !responseService.hasResponseForUser(
                        user,
                        sequence,
                        2
                    )
                ) { "This user has already submitted its 2nd attempt " }
            }
        }

        return sequenceService.submitResponse(
            user,
            sequence,
            PlayerController.ResponseSubmissionData(
                interactionId = interaction.id!!,
                attempt = phase.index,
                choiceList = generateChoiceResponse(sequence.statement.choiceSpecification, correct),
                confidenceDegree = confidenceDegree,
                explanation = explanation ?: "Random explanation on ${LocalDate.now()} by ${user.username}",
            )
        )


    }


    fun submitRandomResponses(
        phase: Phase,
        learners: List<User>,
        sequence: Sequence,
    ) =
        learners.forEachIndexed { index, learner ->
            submitResponse(
                phase,
                learner,
                sequence,
                Random.nextBoolean(),
                ConfidenceDegree.values().random(),
                "Random explanation n°$index by ${learner.username}"
            )
        }

    fun curriedSubmitRandomResponses(
        phase: Phase,
        learners: List<User>
    ) =
        { sequence: Sequence -> submitRandomResponses(phase, learners, sequence) }

    fun evaluate(
        user: User,
        sequence: Sequence,
        evaluationStrategy: EvaluationStrategy,
    ) {

        // Evaluate other responses
        responseService.findAllRecommandedResponsesForUser(
            sequence = sequence,
            attempt = sequence.whichAttemptEvaluate(),
            user = user
        ).forEach { response: Response ->
            peerGradingService.createOrUpdateLikert(user, response, evaluationStrategy.evaluate(response))
        }
    }

    fun randomEvaluate(
        learners: List<User>,
        sequence: Sequence
    ) =
        learners.forEach {
            evaluate(it, sequence, EvaluationStrategy.RANDOM)
        }

    fun curriedRandomEvaluate(learners: List<User>) =
        { sequence: Sequence -> randomEvaluate(learners, sequence) }


    fun publishResults(sequence: Sequence) =
        sequenceService.publishResults(sequence.owner, sequence)

    fun stopSequence(sequence: Sequence) =
        sequenceService.stop(sequence.owner, sequence)

    fun reopenSequence(sequence: Sequence) =
        sequenceService.reopen(sequence.owner, sequence)

    fun nextPhase(sequence: Sequence) =
        sequence.activeInteraction.let { activeInteraction ->
            checkNotNull(activeInteraction) { THE_SEQUENCE_HAS_NO_ACTIVE_INTERACTION }
            check(activeInteraction.rank != sequence.interactions.size) { "The active interaction is the last one" }

            interactionService.stop(sequence.owner, activeInteraction)
            interactionService.startNext(sequence.owner, activeInteraction)
        }

    /**
     * Stop the active interaction of the sequence
     */
    fun stopPhase(sequence: Sequence) =
        sequence.activeInteraction.let { activeInteraction ->
            checkNotNull(activeInteraction) { THE_SEQUENCE_HAS_NO_ACTIVE_INTERACTION }
            interactionService.stop(sequence.owner, activeInteraction)
        }

    /**
     * Start the next interaction of the sequence
     * The active interaction must be stopped
     */
    fun startNextPhase(sequence: Sequence) =
        sequence.activeInteraction.let { activeInteraction ->
            checkNotNull(activeInteraction) { THE_SEQUENCE_HAS_NO_ACTIVE_INTERACTION }
            check(activeInteraction.rank != sequence.interactions.size) { "The active interaction is the last one" }
            check(activeInteraction.state == State.afterStop) { "The active interaction is not stopped" }

            interactionService.startNext(sequence.owner, activeInteraction)
        }


    private fun generateChoiceResponse(
        choiceSpecification: ChoiceSpecification?,
        correct: Boolean
    ): List<Int>? =
        when (choiceSpecification) {
            null -> null
            is ExclusiveChoiceSpecification -> generateExclusiveChoiceResponse(choiceSpecification, correct)
            is MultipleChoiceSpecification -> generateMultipleChoiceResponse(choiceSpecification, correct)
            else -> error("Hmm... That shouldn't happened")
        }


    /**
     * Generate a user choice depending on the question specification & the
     * response must be correct or not
     */
    private fun generateExclusiveChoiceResponse(
        choiceSpecification: ExclusiveChoiceSpecification,
        correct: Boolean,
    ): List<Int> {
        return listOf(
            if (correct) {
                choiceSpecification.expectedChoice.index
            } else {
                (1 until choiceSpecification.nbCandidateItem + 1)
                    .filter {
                        it != choiceSpecification.expectedChoice.index
                    }[Random.nextInt(choiceSpecification.nbCandidateItem - 1)]
            }
        )
    }

    /**
     * Generate a user choice depending on the question specification & the
     * response must be correct or not
     */
    private fun generateMultipleChoiceResponse(
        choiceSpecification: MultipleChoiceSpecification,
        correct: Boolean,
    ): List<Int> =
        if (correct) {
            choiceSpecification.expectedChoiceList.map { it.index }
        } else {
            var candidates: List<Int>
            do {
                candidates = (1 until choiceSpecification.nbCandidateItem).toList().filter {
                    Random.nextInt(100) < 40
                }
            } while (candidates == choiceSpecification.expectedChoiceList.map { it.index })

            candidates
        }

    fun randomlyPlaySequence(learners: List<User>, sequence: Sequence) =
        startSequence(sequence)
            .also(curriedSubmitRandomResponses(Phase.PHASE_1, learners))
            .also(::nextPhase)
            .also(curriedRandomEvaluate(learners))
            .also(::nextPhase)
            .also(::publishResults)
            .also(::stopSequence)

    fun curriedRandomlyPlaySequence(learners: List<User>): (sequence: Sequence) -> Unit =
        { sequence: Sequence -> randomlyPlaySequence(learners, sequence) }

    fun randomlyPlayAllSequences(learners: List<User>, assignment: Assignment) =
        assignment.sequences.forEach(curriedRandomlyPlaySequence(learners))

    fun curriedRandomlyPlayAllSequences(learners: List<User>): (assignment: Assignment) -> Unit =
        { assignment -> randomlyPlayAllSequences(learners, assignment) }


    fun executeScript(sequenceId: Long, script: List<Command>) {
        val sequence = sequenceService.get(sequenceId, true)

        script.forEach { command ->
            when (command) {
                is StartSequence -> startSequence(
                    sequence,
                    command.executionContext,
                    command.studentsProvideExplanation,
                    command.nbResponseToEvaluate
                )

                is SubmitResponse -> submitResponse(
                    phase = command.phase,
                    user = userRepository.getByUsername(command.username),
                    sequence = sequence,
                    correct = command.correct,
                    confidenceDegree = command.confidenceDegree,
                    explanation = command.explanation
                )

                is Evaluate -> evaluate(
                    userRepository.getByUsername(command.username),
                    sequence,
                    evaluationStrategy = command.strategy,
                )

                is NextPhase -> nextPhase(sequence)

                is PublishResults -> publishResults(sequence)

                is StopSequence -> stopSequence(sequence)

                else -> error("Unsupported command")
            }

        }
    }

    private fun registerUserIfNeeded(user: User, sequence: Sequence) {
        val assignment = sequence.assignment!!

        // Register on the fly if needed
        if (!assignmentService.userIsRegisteredInAssignment(user, assignment)) {
            assignmentService.registerUser(user, assignment)
        }
    }

    fun unpublishResults(sequence: Sequence) {
        sequenceService.unpublishResults(sequence.owner, sequence)
    }
}