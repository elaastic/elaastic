package org.elaastic.assignment

import org.elaastic.activity.response.ResponseService
import org.elaastic.common.util.toDate
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceRepository
import org.elaastic.material.instructional.question.attachment.AttachmentService
import org.elaastic.material.instructional.course.Course
import org.elaastic.material.instructional.subject.Subject
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.material.instructional.statement.StatementService
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.EntityNotFoundException
import javax.transaction.Transactional

@Service
@Transactional
class AssignmentService(
    @Autowired val assignmentRepository: AssignmentRepository,
    @Autowired val sequenceRepository: SequenceRepository,
    @Autowired val learnerAssignmentRepository: LearnerAssignmentRepository,
    @Autowired val statementService: StatementService,
    @Autowired val attachmentService: AttachmentService,
    @Autowired val entityManager: EntityManager,
    @Autowired val responseService: ResponseService,
) {

    fun findAllByOwner(
        owner: User,
        pageable: Pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "lastUpdated"))
    )
            : Page<Assignment> {
        return assignmentRepository.findAllByOwner(owner, pageable)
    }

    fun get(id: Long, fetchSequences: Boolean = false): Assignment {
        // TODO (+) i18n error message
        return when (fetchSequences) {
            true -> assignmentRepository.findOneWithSequencesById(id)
            false -> assignmentRepository.findOneById(id)
        } ?: throw EntityNotFoundException("There is no assignment for id \"$id\"")
    }

    fun findByUuid(uuid: UUID, fetchSequences: Boolean = false): Assignment {
        // TODO (+) i18n error message
        return when (fetchSequences) {
            true -> assignmentRepository.findWithSequenceByGlobalId(uuid)
            false -> assignmentRepository.findByGlobalId(uuid)
        } ?: throw EntityNotFoundException("There is no assignment for id \"$uuid\"")
    }

    fun get(user: User, id: Long, fetchSequences: Boolean = false): Assignment {
        get(id, fetchSequences).let {
            if (it.owner != user) {
                throw AccessDeniedException("You are not authorized to access to this assignment")
            }
            return it
        }
    }

    fun delete(user: User, assignment: Assignment) {
        require(user == assignment.owner) {
            "Only the owner can delete an assignment"
        }
        assignmentRepository.delete(assignment) // all other linked entities are deletes by DB cascade
    }

    fun save(assignment: Assignment): Assignment {
        return assignmentRepository.save(assignment)
    }

    fun count(): Long {
        return assignmentRepository.count()
    }

    fun countAllSequence(assignment: Assignment): Int {
        return sequenceRepository.countAllByAssignment(assignment)
    }

    fun touch(assignment: Assignment) {
        assignment.lastUpdated = Date()
        assignmentRepository.save(assignment)
    }

    fun addSequence(assignment: Assignment, statement: Statement): Sequence {
        val sequence = Sequence(
            owner = assignment.owner,
            statement = statement,
            rank = statement.rank
        )

        assignment.addSequence(sequence)
        sequenceRepository.save(sequence)
        touch(assignment)

        return sequence
    }

    fun removeSequence(user: User, sequence: Sequence) {
        require(user == sequence.owner) {
            "Only the owner can delete a sequence"
        }
        val assignment = sequence.assignment!!
        touch(assignment)
        assignment.sequences.remove(sequence)
        sequenceRepository.delete(sequence) // all other linked entities are deletes by DB cascade
        entityManager.flush()
        updateAllSequenceRank(assignment)
    }

    fun moveUpSequence(assignment: Assignment, sequenceId: Long) {
        val idsArray = assignment.sequences.map { it.id }.toTypedArray()
        val pos = idsArray.indexOf(sequenceId)

        check(pos != -1) { "This sequence $sequenceId does not belong to assignment ${assignment.id}" }
        if (pos == 0)
            return  // Nothing to do

        entityManager.createNativeQuery(
            "UPDATE sequence SET `rank` = CASE " +
                    "WHEN id=${sequenceId} THEN ${pos} " +
                    "WHEN id=${idsArray[pos - 1]} THEN ${pos + 1} " +
                    " END " +
                    "WHERE id in (${idsArray[pos - 1]}, ${sequenceId})"
        ).executeUpdate()
    }

    fun moveDownSequence(assignment: Assignment, sequenceId: Long) {
        val idsArray = assignment.sequences.map { it.id }.toTypedArray()
        val pos = idsArray.indexOf(sequenceId)

        check(pos != -1) { "This sequence $sequenceId does not belong to assignment ${assignment.id}" }
        if (pos == assignment.sequences.size - 1)
            return  // Nothing to do

        entityManager.createNativeQuery(
            "UPDATE sequence SET `rank` = CASE " +
                    "WHEN id=${sequenceId} THEN ${pos + 1} " +
                    "WHEN id=${idsArray[pos + 1]} THEN ${pos} " +
                    " END " +
                    "WHERE id in (${idsArray[pos + 1]}, ${sequenceId})"
        ).executeUpdate()
    }

    fun updateAllSequenceRank(assignment: Assignment) {
        val sequenceIds = assignment.sequences.map { it.id }
        if (sequenceIds.isEmpty()) return // Nothing to do

        entityManager.createNativeQuery(
            "UPDATE sequence SET `rank` = CASE " +
                    sequenceIds.mapIndexed { index, id ->
                        "WHEN id=$id THEN $index"
                    }.joinToString(" ") +
                    " END " +
                    "WHERE id in (${sequenceIds.joinToString(",")})"
        ).executeUpdate()

        assignment.sequences.mapIndexed { index, sequence -> sequence.rank = index + 1 }
    }


    fun findAllAssignmentsForLearner(user: User): List<Assignment> {
        return learnerAssignmentRepository.findAllAssignmentsForLearnerWithoutPage(user)
    }


    fun findByGlobalId(globalId: UUID): Assignment? {
        return assignmentRepository.findByGlobalId(globalId)
    }

    fun registerUser(user: User, assignment: Assignment): LearnerAssignment? {
        if (assignment.owner == user) {
            return null
        }

        assignmentRepository.updateLastUpdated(assignment.id!!, Date())

        return learnerAssignmentRepository.findByLearnerAndAssignment(
            user,
            assignment
        ) ?: learnerAssignmentRepository.save(
            LearnerAssignment(user, assignment)
        )
    }

    fun userIsRegisteredInAssignment(user: User, assignment: Assignment): Boolean {
        return learnerAssignmentRepository.findByLearnerAndAssignment(
            user,
            assignment
        ) != null
    }

    fun getNbRegisteredUsers(assignment: Assignment): Int {
        return learnerAssignmentRepository.countAllByAssignment(assignment)
    }

    fun getRegisteredUsers(assignment: Assignment): List<LearnerAssignment>
        = learnerAssignmentRepository.findAllByAssignment(assignment)

    fun getRegisteredUser(assignment: Assignment, user: User): LearnerAssignment?
        = learnerAssignmentRepository.findByLearnerAndAssignment(user, assignment)

    fun countAllRegisteredUsers(assignment: Assignment): Int {
        return learnerAssignmentRepository.countAllByAssignment(assignment)
    }

    fun getNbRegisteredUsers(assignmentId: Long): Int {
        return learnerAssignmentRepository.countAllByAssignment(
            assignmentRepository.getReferenceById(assignmentId)
        )
    }

    fun buildFromSubject(assignment: Assignment, subject: Subject) {
        for (statement: Statement in subject.statements) {
            this.addSequence(assignment, statement)
            touch(assignment)
        }
    }

    fun getCoursesAssignmentsMap(assignments: List<Assignment>): MutableMap<Course, MutableList<Assignment>> {

        // TODO rewrite this code in a more functional way
        val mapResult: MutableMap<Course, MutableList<Assignment>> = mutableMapOf()
        for (assignment in assignments) {
            val course: Course? = assignment.subject?.course
            if (course != null) {
                if (!mapResult.containsKey(course)) {
                    mapResult[course] = mutableListOf(assignment)
                } else {
                    mapResult[course]!!.add(assignment)
                }
            }
        }
        return mapResult
    }

    fun findAllAssignmentUpdatedSince(since: LocalDateTime): List<Assignment> =
        assignmentRepository.findAllAssignmentUpdatedSince(since.toDate())

fun findAllLearnersRegisteredOnWithCasUser(assignment: Assignment) =
    learnerAssignmentRepository.findAllRegisteredLearnersOnAssignmentWithCasUser(assignment).toSet()
}