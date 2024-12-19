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

package org.elaastic.bootstrap

import com.icegreen.greenmail.util.GreenMail
import com.icegreen.greenmail.util.ServerSetup
import org.elaastic.activity.evaluation.peergrading.PeerGradingService
import org.elaastic.activity.response.ConfidenceDegree
import org.elaastic.activity.response.Response
import org.elaastic.activity.response.ResponseService
import org.elaastic.assignment.Assignment
import org.elaastic.assignment.AssignmentService
import org.elaastic.assignment.ReadyForConsolidation
import org.elaastic.material.instructional.course.Course
import org.elaastic.material.instructional.course.CourseService
import org.elaastic.material.instructional.question.ChoiceItem
import org.elaastic.material.instructional.question.ExclusiveChoiceSpecification
import org.elaastic.material.instructional.question.MultipleChoiceSpecification
import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.material.instructional.question.legacy.LearnerChoice
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.material.instructional.statement.StatementService
import org.elaastic.material.instructional.subject.Subject
import org.elaastic.material.instructional.subject.SubjectService
import org.elaastic.material.instructional.question.attachment.Attachment
import org.elaastic.material.instructional.question.attachment.AttachmentService
import org.elaastic.material.instructional.question.attachment.MimeType
import org.elaastic.auth.lti.LtiConsumer
import org.elaastic.auth.lti.LtiConsumerRepository
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.sequence.ExecutionContext
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.interaction.InteractionService
import org.elaastic.sequence.phase.evaluation.EvaluationPhaseConfig
import org.elaastic.user.RoleService
import org.elaastic.user.User
import org.elaastic.user.UserService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.util.ResourceUtils
import java.math.BigDecimal
import java.util.*
import javax.transaction.Transactional


@Service
class BootstrapService(
    @Autowired val userService: UserService,
    @Autowired val roleService: RoleService,
    @Autowired val ltiConsumerRepository: LtiConsumerRepository,
    @Autowired val subjectService: SubjectService,
    @Autowired val courseService: CourseService,
    @Autowired val statementService: StatementService,
    @Autowired val attachmentService: AttachmentService,
    @Autowired val assignmentService: AssignmentService,
    @Autowired val sequenceService: SequenceService,
    @Autowired val responseService: ResponseService,
    @Autowired val interactionService: InteractionService,
    @Autowired val peerGradingService: PeerGradingService
) {

    var mailServer: GreenMail? = null

    @Transactional
    fun initializeDevUsers() {
        listOf(
            User(
                firstName = "Franck",
                lastName = "Sil",
                username = "fsil",
                plainTextPassword = "1234",
                email = "fsil@elaastic.org"
            ).addRole(roleService.roleTeacher()),
            User(
                firstName = "Rialy",
                lastName = "Andria",
                username = "rand",
                plainTextPassword = "1234",
                email = "rand@elaastic.org"
            ).addRole(roleService.roleTeacher()),
            User(
                firstName = "Albert",
                lastName = "Ein",
                username = "aein",
                plainTextPassword = "1234",
                email = "aein@elaastic.org"
            ).addRole(roleService.roleTeacher()),
            User(
                firstName = "Mary",
                lastName = "Sil",
                username = "msil",
                plainTextPassword = "1234",
                email = "msil@elaastic.org"
            ).addRole(roleService.roleStudent()),
            User(
                firstName = "Thom",
                lastName = "Sil",
                username = "tsil",
                plainTextPassword = "1234",
                email = "tsil@elaastic.org"
            ).addRole(roleService.roleStudent()),
            User(
                firstName = "John",
                lastName = "Tra",
                username = "jtra",
                plainTextPassword = "1234",
                email = "jtra@elaastic.org"
            ).addRole(roleService.roleStudent()),
            User(
                firstName = "Erik",
                lastName = "Erik",
                username = "erik",
                plainTextPassword = "1234",
                email = "erik@elaastic.org"
            ).addRole(roleService.roleStudent()),
            User(
                firstName = "admin",
                lastName = "root",
                username = "rootadmin",
                plainTextPassword = "1234",
                email = "admin@elaastic.org"
            ).addRole(roleService.roleAdmin())
        ).map {
            userService.findByUsername(it.username) ?: userService.addUser(it)
        }
    }

    fun startDevLocalSmtpServer() {
        mailServer = GreenMail(ServerSetup(10025, "localhost", "smtp"))
        try {
            with(mailServer!!) {
                setUser("elaastic", "elaastic")
                start()
            }
        } catch (e: Exception) {
        }
    }

    fun stopDevLocalSmtpServer() {
        try {
            mailServer?.stop()
        } catch (e: Exception) {
        }
    }

    fun initializeDevLtiObjects() {

        LtiConsumer( // a lti consumer aka an LMS
            consumerName = "Moodle",
            secret = "secret pass",
            key = "abcd1234"
        ).let {
            it.enableFrom = Date()
            if (!ltiConsumerRepository.existsById(it.key)) {
                ltiConsumerRepository.saveAndFlush(it)
            }
            it
        }

    }

    fun migrateTowardVersion400() {
        subjectService.migrateAssignmentsTowardSubjects()
    }

    @Transactional
    fun initializeDemoContent() {
        val demoUser = userService.findByUsername("aein")!!
        if (courseService.findAllByOwner(MaterialUser.fromElaasticUser(demoUser)).isEmpty()) {
            // if (courseService.count() == 0L) {
            initializeStepByStep()
        }
    }

    @Transactional
    private fun initializeStepByStep() {
        // Initialize courses - Step 1
        val demo = courseService.save(
            Course(
                title = "Demo",
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("aein")!!)
            )
        )

        val maths = courseService.save(
            Course(
                title = "Maths 6e",
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("aein")!!)
            )
        )

        val francais = courseService.save(
            Course(
                title = "Français 5e",
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("fsil")!!)
            )
        )

        val histoire = courseService.save(
            Course(
                title = "Histoire 4e",
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("rand")!!)
            )
        )

        // Initialize empty subjects - Step 2
        listOf(
            Subject(
                title = "Nombres décimaux",
                course = maths,
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("aein")!!)
            ),
            Subject(
                title = "Figures planes",
                course = maths,
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("aein")!!)
            ),
            Subject(
                title = "Le champ lexical",
                course = francais,
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("fsil")!!)
            ),
            Subject(
                title = "Les compléments d'objets",
                course = francais,
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("fsil")!!)
            ),
            Subject(
                title = "La révolution française",
                course = histoire,
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("rand")!!)
            ),
        ).forEach {
            subjectService.save(it)
        }

        // Initialize filled subjects - Step 3
        val accords = subjectService.save(
            Subject(
                title = "Accords du participe passé",
                course = francais,
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("fsil")!!)
            )
        )

        initializeStatementsAccords(accords) // Step 4
        initializeAssignment(accords) // Step 5


        val fractions = subjectService.save(
            Subject(
                title = "Fractions",
                course = maths,
                owner = MaterialUser.fromElaasticUser(userService.findByUsername("aein")!!)
            )
        )

        initializeStatementsFractions(fractions) // Step 4
        initializeAssignment(fractions) // Step 5
    }

    @Transactional
    private fun initializeStatementsAccords(accords: Subject) {
        val statements = listOf(
            // Statement Exclusive choice
            Statement(
                owner = accords.owner,
                questionType = QuestionType.ExclusiveChoice,
                choiceSpecification = ExclusiveChoiceSpecification(
                    nbCandidateItem = 4,
                    expectedChoice = ChoiceItem(2, 1f)
                ),
                content = "Complétez la phrase en choisissant la bonne orthographe du participe passé. Puis justifiez votre choix de manière rédigée.\n" +
                        "\n" +
                        "Le texte … en classe était très long et très difficile.\n" +
                        "\n" +
                        "    appri\n" +
                        "    appris\n" +
                        "    apprit\n" +
                        "    apprie\n",
                title = "Question 1"
            ),
            // Statement Open ended
            Statement(
                owner = accords.owner,
                questionType = QuestionType.OpenEnded,
                choiceSpecification = null,
                content = "À quoi sert le participe passé en français ? Dans quels cas l’utilise-t-on ?",
                title = "Question 2",
                expectedExplanation = "Le participe passé sert à former les temps composés d’un verbe (\"J’ai ouvert la porte\"). On peut aussi l'utiliser comme adjectif (\"le portail ouvert\")."
            )
        )
        statements.forEach { statementService.save(it) }
        accords.addStatement(statements[0])
        accords.addStatement(statements[1])
    }

    @Transactional
    private fun initializeStatementsFractions(fractions: Subject) {
        val statements = listOf(
            // Statement Multiple Choice with Latek
            Statement(
                owner = fractions.owner,
                questionType = QuestionType.MultipleChoice,
                choiceSpecification = MultipleChoiceSpecification(
                    nbCandidateItem = 3,
                    expectedChoiceList = listOf(ChoiceItem(1, 1f), ChoiceItem(3, 1f))
                ),
                content = "<p>Quel(s) symbole(s) repr&eacute;sente(nt) l&#39;inconnue dans l&#39;&eacute;quation suivante ? Expliquez ce que signifie r&eacute;soudre l&#39;&eacute;quation.</p>\n" +
                        "\n" +
                        "<p><span class=\"math-tex\">\\(7x + 4 = 25\\)</span></p>\n" +
                        "\n" +
                        "<ol>\n" +
                        "\t<li><span class=\"math-tex\">\\(x\\)</span></li>\n" +
                        "\t<li><span class=\"math-tex\">\\(+\\)</span></li>\n" +
                        "\t<li><span class=\"math-tex\">\\(=\\)</span></li>\n" +
                        "</ol>\n" +
                        "<p>&nbsp;</p>\n",
                title = "Équations simples",
                expectedExplanation = "L'inconnue est représentée par le symbole x. Résoudre l'équation signifie trouver la valeur de x qui vérifie l'égalité."
            ),
            // Statement with Attachment image
            Statement(
                owner = fractions.owner,
                questionType = QuestionType.OpenEnded,
                choiceSpecification = null,
                content = "<p>Comment tracer l&#39;&eacute;quation lin&eacute;aire &eacute;crite sur le graphique ?</p>\n",
                title = "Tracer une équation linéaire",
                expectedExplanation = "<p>Cette &eacute;quation est lin&eacute;aire, ce qui signifie qu&#39;elle sera repr&eacute;sent&eacute;e par une ligne droite sur le graphique. Il vous suffit de trouver 2 points par lesquels la droite est cens&eacute;e passer, puis vous pourrez la tracer. La r&eacute;solution de l&#39;&eacute;quation pour 2 valeurs diff&eacute;rentes de y permet de trouver 2 points diff&eacute;rents.</p>\n" +
                        "\n" +
                        "<p>Plus de d&eacute;tails :<br />\n" +
                        "Un premier point est d&eacute;j&agrave; &eacute;crit, il s&#39;agit de (0, -1). Un point est sur la droite si on peut &eacute;crire (x, y) comme un couple qui r&eacute;sout l&#39;&eacute;quation.<br />\n" +
                        "Avec le couple (x = 0, y = -1), l&#39;&eacute;quation y = 2x -1 devient 2*0 - 1 = -1, ce qui est vrai.<br />\n" +
                        "Un autre point que nous pouvons trouver est le point o&ugrave; y = 0 en r&eacute;solvant l&#39;&eacute;quation de cette fa&ccedil;on :<br />\n" +
                        "2*x - 1 = y = 0<br />\n" +
                        "2x = 1<br />\n" +
                        "x = 1/2</p>\n" +
                        "\n" +
                        "<p>Avec les deux points (0, -1) et (1/2, 0), vous pouvez tracer la ligne repr&eacute;sentant toutes les solutions possibles de l&#39;&eacute;quation.</p>\n"
            )
        )

        statements.forEach { statementService.save(it) }
        fractions.addStatement(statements[0])
        fractions.addStatement(statements[1])

        // Add attachment to the second statement
        val file = ResourceUtils.getFile("classpath:static/images/demonstrationContent/CreateQuestion.png");
        val graphMaths = Attachment(
            name = "fileToAttached",
            originalFileName = "CreateQuestion.png",
            size = file.length(),
            mimeType = MimeType("image/png"),
        )

        attachmentService.saveStatementAttachment(
            statement = statements[1],
            attachment = graphMaths,
            inputStream = file.inputStream()
        )

    }

    @Transactional
    private fun initializeAssignment(subject: Subject) {
        var assignments = listOf(
            Assignment(
                owner = User.fromMaterialUser(subject.owner),
                title = subject.title + " - Face à face - Finished",
                description = "Exercice en classe",
                audience = "groupe A1",
                readyForConsolidation = ReadyForConsolidation.NotAtAll
            ),
            Assignment(
                owner = User.fromMaterialUser(subject.owner),
                title = subject.title + " - Face à face",
                description = "Exercice en classe",
                audience = "groupe A2",
                readyForConsolidation = ReadyForConsolidation.AfterTeachings
            ),
            Assignment(
                owner = User.fromMaterialUser(subject.owner),
                title = subject.title + " - Distant",
                description = "Exercice en classe",
                audience = "groupe C",
                readyForConsolidation = ReadyForConsolidation.Immediately
            ),
            Assignment(
                owner = User.fromMaterialUser(subject.owner),
                title = subject.title + " - Blended",
                description = "Exercice en classe",
                audience = "groupe G",
                readyForConsolidation = ReadyForConsolidation.AfterTeachings
            )
        )

        assignments.forEach {
            subjectService.addAssignment(subject, it)
        }

        playAssignmentsWithLearners(assignments.get(0), ExecutionContext.FaceToFace)
        onlyStartSequences(assignments.get(1), ExecutionContext.FaceToFace)
        registerLearners(assignments.get(2))
        registerLearners(assignments.get(3))
    }

    @Transactional
    private fun playAssignmentsWithLearners(assignment: Assignment, mode: ExecutionContext) {
        var learners = registerLearners(assignment)
        startSequences(assignment, mode)
        var sequence0 = assignment.sequences.get(0)
        var sequence1 = assignment.sequences.get(1)

        var responses1 = firstAnswersForSequence(sequence0, learners)
        var responses2 = firstAnswersForSequence(sequence1, learners)

        // Teacher action to change phase
        interactionService.startNext(assignment.owner, sequence0.activeInteraction!!)
        interactionService.startNext(assignment.owner, sequence1.activeInteraction!!)

        feedbacksPhase2forSequence(sequence0, learners, responses1)
        feedbacksPhase2forSequence(sequence1, learners, responses2)

        // Teacher action to go in last phase
        interactionService.startNext(assignment.owner, sequence0.activeInteraction!!)
        interactionService.startNext(assignment.owner, sequence1.activeInteraction!!)
        sequenceService.stop(assignment.owner, sequence0)
        sequenceService.stop(assignment.owner, sequence1)
        sequenceService.publishResults(assignment.owner, sequence0)
        sequenceService.publishResults(assignment.owner, sequence1)
    }

    @Transactional
    private fun onlyStartSequences(assignment: Assignment, mode: ExecutionContext) {
        registerLearners(assignment)
        startSequences(assignment, mode)
    }

    @Transactional
    private fun registerLearners(assignment: Assignment): List<User> {
        val learners = listOf(
            userService.findByUsername("msil")!!,
            userService.findByUsername("tsil")!!,
            userService.findByUsername("jtra")!!,
            userService.findByUsername("erik")!!
        )

        learners.forEach {
            assignmentService.registerUser(it, assignment);
        }

        return learners
    }

    @Transactional
    private fun startSequences(assignment: Assignment, mode: ExecutionContext) {
        assignment.sequences.forEach {
            sequenceService.start(
                assignment.owner,
                it,
                mode,
                true,
                2,
                EvaluationPhaseConfig.ALL_AT_ONCE,
                false
            )
        }
    }

    @Transactional
    private fun firstAnswersForSequence(sequence: Sequence, learners: List<User>): List<Response> {

        // Learner 0

        val choiceListSpecification0: LearnerChoice? = when (sequence.statement.questionType) {
            QuestionType.ExclusiveChoice -> LearnerChoice(listOf(1))
            QuestionType.MultipleChoice -> LearnerChoice(listOf(2))
            QuestionType.OpenEnded -> null
        }

        val response0 = responseService.save(
            sequenceService.getActiveInteractionForLearner(sequence, learners.get(0))
                ?: error("No active interaction, cannot submit a response"),
            Response(
                learner = learners.get(0),
                interaction = sequence.getResponseSubmissionInteraction(),
                attempt = 1,
                confidenceDegree = ConfidenceDegree.CONFIDENT,
                explanation = "Une réponse correcte, mais pas complète",
                learnerChoice = choiceListSpecification0,
                score = choiceListSpecification0?.let {
                    Response.computeScore(
                        it,
                        sequence.statement.choiceSpecification
                            ?: error("The choice specification is undefined")
                    )
                },
                statement = sequence.statement
            )
        )

        if (sequence.executionIsDistance() || sequence.executionIsBlended()) {
            sequenceService.nextInteractionForLearner(sequence, learners.get(0))
        }

        /// Learner 1

        val choiceListSpecification1: LearnerChoice? = when (sequence.statement.questionType) {
            QuestionType.ExclusiveChoice -> LearnerChoice(listOf(1))
            QuestionType.MultipleChoice -> LearnerChoice(listOf(2))
            QuestionType.OpenEnded -> null
        }

        val response1 = responseService.save(
            sequenceService.getActiveInteractionForLearner(sequence, learners.get(1))
                ?: error("No active interaction, cannot submit a response"),
            Response(
                learner = learners.get(1),
                interaction = sequence.getResponseSubmissionInteraction(),
                attempt = 1,
                confidenceDegree = ConfidenceDegree.TOTALLY_CONFIDENT,
                explanation = "Une réponse excellente et correcte, qui couvre bien toute la question.",
                learnerChoice = choiceListSpecification1,
                score = choiceListSpecification1?.let {
                    Response.computeScore(
                        it,
                        sequence.statement.choiceSpecification
                            ?: error("The choice specification is undefined")
                    )
                },
                statement = sequence.statement
            )
        )

        if (sequence.executionIsDistance() || sequence.executionIsBlended()) {
            sequenceService.nextInteractionForLearner(sequence, learners.get(1))
        }

        // Learner 2

        val choiceListSpecification2: LearnerChoice? = when (sequence.statement.questionType) {
            QuestionType.ExclusiveChoice -> LearnerChoice(listOf(2))
            QuestionType.MultipleChoice -> LearnerChoice(listOf(1, 3))
            QuestionType.OpenEnded -> null
        }

        val response2 = responseService.save(
            sequenceService.getActiveInteractionForLearner(sequence, learners.get(2))
                ?: error("No active interaction, cannot submit a response"),
            Response(
                learner = learners.get(2),
                interaction = sequence.getResponseSubmissionInteraction(),
                attempt = 1,
                confidenceDegree = ConfidenceDegree.NOT_CONFIDENT_AT_ALL,
                explanation = "Une injure envers l'enseignant",
                learnerChoice = choiceListSpecification2,
                score = choiceListSpecification2?.let {
                    Response.computeScore(
                        it,
                        sequence.statement.choiceSpecification
                            ?: error("The choice specification is undefined")
                    )
                },
                statement = sequence.statement
            )
        )

        if (sequence.executionIsDistance() || sequence.executionIsBlended()) {
            sequenceService.nextInteractionForLearner(sequence, learners.get(2))
        }


        // Learner 3

        val choiceListSpecification3: LearnerChoice? = when (sequence.statement.questionType) {
            QuestionType.ExclusiveChoice -> LearnerChoice(listOf(2))
            QuestionType.MultipleChoice -> LearnerChoice(listOf(1, 3))
            QuestionType.OpenEnded -> null
        }

        val response3 = responseService.save(
            sequenceService.getActiveInteractionForLearner(sequence, learners.get(3))
                ?: error("No active interaction, cannot submit a response"),
            Response(
                learner = learners.get(3),
                interaction = sequence.getResponseSubmissionInteraction(),
                attempt = 1,
                confidenceDegree = ConfidenceDegree.NOT_REALLY_CONFIDENT,
                explanation = "Une explication bancale",
                learnerChoice = choiceListSpecification3,
                score = choiceListSpecification3?.let {
                    Response.computeScore(
                        it,
                        sequence.statement.choiceSpecification
                            ?: error("The choice specification is undefined")
                    )
                },
                statement = sequence.statement
            )
        )

        if (sequence.executionIsDistance() || sequence.executionIsBlended()) {
            sequenceService.nextInteractionForLearner(sequence, learners.get(3))
        }

        return listOf(response0, response1, response2, response3)
    }

    private fun feedbacksPhase2forSequence(sequence: Sequence, learners: List<User>, responses: List<Response>) {

        var learner0Eval = peerGradingService.createOrUpdateLikert(
            learners.get(0),
            responses.get(1),
            BigDecimal(5)
        )
        var learner1Eval = peerGradingService.createOrUpdateLikert(
            learners.get(1),
            responses.get(2),
            BigDecimal(1)
        )
        var learner2Eval = peerGradingService.createOrUpdateLikert(
            learners.get(2),
            responses.get(3),
            BigDecimal(2)
        )
        var learner3Eval = peerGradingService.createOrUpdateLikert(
            learners.get(3),
            responses.get(0),
            BigDecimal(4)
        )
    }

}
