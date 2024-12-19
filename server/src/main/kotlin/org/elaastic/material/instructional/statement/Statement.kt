package org.elaastic.material.instructional.statement

import org.elaastic.common.persistence.AbstractJpaPersistable
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.question.*
import org.elaastic.material.instructional.subject.Subject
import org.elaastic.material.instructional.question.attachment.Attachment
import org.elaastic.user.User
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * Statement is a class that represents a statement in a subject.
 *
 * A statement is a question, that can be open-ended, multiple choice or
 * exclusive choice.
 *
 * A statement can have a subject.
 *
 * @see Subject
 * @see QuestionType
 */
@Entity
@EntityListeners(AuditingEntityListener::class)
// TODO (+) We should define a interface so that a sequence can implement statement operations by delegation
class Statement(
    @field:NotNull
    @field:ManyToOne(fetch = FetchType.LAZY)
    var owner: MaterialUser,

    @field:NotBlank
    var title: String = "",

    @field:NotBlank
    var content: String = "",

    /**
     * The type of the question.
     *
     * @see QuestionType
     */
    @field:NotNull
    @Enumerated(EnumType.STRING)
    var questionType: QuestionType,

    /**
     * The specification of the choice. Only used if the question type is
     * MultipleChoice or ExclusiveChoice.
     *
     * @see ChoiceSpecification
     * @see MultipleChoiceSpecification
     * @see ExclusiveChoiceSpecification
     */
    @Convert(converter = ChoiceSpecificationConverter::class)
    @field:Column(name = "choice_specification")
    var choiceSpecification: ChoiceSpecification? = null,

    @field:ManyToOne(fetch = FetchType.LAZY)
    var parentStatement: Statement? = null,

    /** The expected explanation of the statement. */
    @field:Column(name = "expected_explanation")
    var expectedExplanation: String? = null,

    /**
     * The subject of the statement.
     *
     * @see Subject
     */
    @field:ManyToOne(fetch = FetchType.LAZY)
    var subject: Subject? = null,

    @Column(name = "`rank`")
    var rank: Int = 0

) : AbstractJpaPersistable<Long>(), Comparable<Statement> {

    @Version
    var version: Long? = null

    @field:NotNull
    @Column(columnDefinition = "BINARY(16)")
    var uuid: UUID = UUID.randomUUID()

    @Column(name = "date_created")
    @CreatedDate
    lateinit var dateCreated: Date

    @LastModifiedDate
    @Column(name = "last_updated")
    var lastUpdated: Date? = null

    /**
     * The attachment of the statement.
     *
     * @see Attachment
     */
    @OneToOne(mappedBy = "statement")
    var attachment: Attachment? = null

    @Transient
    fun isOpenEnded(): Boolean {
        return questionType == QuestionType.OpenEnded
    }

    @Transient
    fun isMultipleChoice(): Boolean {
        return questionType == QuestionType.MultipleChoice
    }

    @Transient
    fun isExclusiveChoice(): Boolean {
        return questionType == QuestionType.ExclusiveChoice
    }

    /**
     * @return True if the statement is a choice question (multiple or
     *    exclusive), false otherwise.
     */
    @Transient
    fun hasChoices() = isMultipleChoice() || isExclusiveChoice()

    override fun compareTo(other: Statement): Int {
        return rank.compareTo(other.rank)
    }

    /**
     * Update the statement's title with the given title
     *
     * @param value The new title of the statement.
     * @return The updated statement.
     */
    fun title(value: String): Statement {
        this.title = value
        return this
    }

    /**
     * Update the statement's content with the given content
     *
     * @param value The new content of the statement.
     * @return The updated statement.
     */
    fun content(value: String): Statement {
        this.content = value
        return this
    }

    /**
     * Update the statement's expected explanation with the given explanation
     *
     * @param value The new expected explanation of the statement.
     * @return The updated statement.
     */
    fun expectedExplanation(value: String?): Statement {
        this.expectedExplanation = value
        return this
    }

    /**
     * Update the statement's values with the values of the other statement.
     *
     * The other statement must have the same owner as this statement.
     *
     * If the id of the other statement is different from this statement, the
     * parent statement is updated.
     *
     * @param otherStatement The statement to update from.
     * @return The updated statement.
     * @throws OptimisticLockException If the version of the other statement is
     *    different from this statement.
     */
    fun updateFrom(otherStatement: Statement): Statement {
        require(owner == otherStatement.owner)
        if (id != otherStatement.id) {
            parentStatement = otherStatement
        } else if (version != otherStatement.version) {
            throw OptimisticLockException()
        }
        title = otherStatement.title
        content = otherStatement.content
        questionType = otherStatement.questionType
        choiceSpecification = otherStatement.choiceSpecification
        parentStatement = otherStatement.parentStatement
        expectedExplanation = otherStatement.expectedExplanation
        return this
    }

    companion object {
        /**
         * Create a default statement with the given user.
         *
         * The statement is an exclusive choice question with two candidate items,
         * and the first one is the expected choice.
         *
         * @param user The futur owner of the statement.
         * @return The created statement.
         */
        fun createDefaultStatement(user: MaterialUser): Statement {
            return Statement(
                owner = user,
                questionType = QuestionType.ExclusiveChoice,
                choiceSpecification = ExclusiveChoiceSpecification(
                    nbCandidateItem = 2,
                    expectedChoice = ChoiceItem(1, 1f)
                )
            )
        }

        fun createDefaultStatement(user: User) =
            createDefaultStatement(MaterialUser.fromElaasticUser(user))

        /**
         * Create an example statement with the given user.
         *
         * The statement is an exclusive choice question with two candidate items,
         * and the first one is the expected choice. The title is "Title" and the
         * content is "Blabla...".
         *
         * @param user The futur owner of the statement.
         * @return The created statement.
         */
        fun createExampleStatement(user: MaterialUser): Statement {
            return Statement(
                owner = user,
                questionType = QuestionType.ExclusiveChoice,
                choiceSpecification = ExclusiveChoiceSpecification(
                    nbCandidateItem = 2,
                    expectedChoice = ChoiceItem(1, 1f)
                ),
                content = "Blabla...",
                title = "Title"
            )
        }
    }
}