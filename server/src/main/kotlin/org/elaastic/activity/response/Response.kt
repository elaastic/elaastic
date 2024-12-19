package org.elaastic.activity.response

import org.elaastic.common.persistence.AbstractJpaPersistable
import org.elaastic.material.instructional.question.ChoiceSpecification
import org.elaastic.material.instructional.question.ExclusiveChoiceSpecification
import org.elaastic.material.instructional.question.MultipleChoiceSpecification
import org.elaastic.material.instructional.question.legacy.LearnerChoice
import org.elaastic.material.instructional.question.legacy.LearnerChoiceConverter
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.moderation.ModerationCandidate
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.user.User
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotNull

/**
 * A response to a sequence
 *
 * In the database, the name of the table is `choice_interaction_response`
 *
 * To find the sequence to which this response is related, you can use the
 * `interaction` property
 *
 * A response can be hidden or removed by a teacher. That's why it
 * implements the [ModerationCandidate] interface
 *
 * A teacher can recommend a response.
 *
 * @see Interaction
 * @see Sequence
 * @see ConfidenceDegree
 * @see LearnerChoice
 */
@Entity
@Table(name = "choice_interaction_response")
@EntityListeners(AuditingEntityListener::class)
class Response(

    @field:ManyToOne
    var learner: User,

    @field:ManyToOne
    var interaction: Interaction,

    /**
     * if the learner changes his response in the second phase of the
     * interaction, this field will be set to 2 otherwise it will be set to 1
     */
    var attempt: Int = 1,

    var explanation: String? = null,

    /**
     * the confidence degree of the learner
     *
     * @see ConfidenceDegree
     */
    @field:Enumerated(EnumType.ORDINAL)
    var confidenceDegree: ConfidenceDegree? = null,

    /**
     * the mean grade of the response given by the PeerGrading
     *
     * @see org.elaastic.questions.assignment.sequence.peergrading.PeerGrading
     */
    var meanGrade: BigDecimal? = null,

    /**
     * the choice made by the learner, if the statement is a (multiple or
     * exclusive) choice statement
     *
     * @see LearnerChoice
     */
    @field:Convert(converter = LearnerChoiceConverter::class)
    @field:Column(name = "choiceListSpecification")
    var learnerChoice: LearnerChoice? = null,

    var score: BigDecimal? = null,

    /*
     * If the response is a fake response
     *
     * A fake response is a response not made by a learner but by the teacher at the creation of the statement
     */
    @field:Column(name = "is_a_fake")
    var fake: Boolean = false,

    var evaluationCount: Int = 0,

    /** Number of all evaluations made by Draxo */
    var draxoEvaluationCount: Int = 0,

    /**
     * the statement to which this response is related
     *
     * @see Statement
     */
    @field:ManyToOne // TODO : Remove this relationship to the statement (must be replaced by a reference to the statement UUID)
    var statement: Statement,

    @field:Column(name = "is_hidden_by_teacher")
    override var hiddenByTeacher: Boolean = false,

    @field:Column(name = "is_recommended_by_teacher")
    override var recommendedByTeacher: Boolean = false

) : AbstractJpaPersistable<Long>(), ModerationCandidate {
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

    /** Number of evaluations made by Draxo that are hidden to the learner */
    @Column(name = "draxo_evaluation_hidden_count")
    var draxoEvaluationHiddenCount: Int = 0

    /** Allow treating this response as an update of an existing response */
    fun makeAsUpdateOf(initialResponse: Response) {
        this.id = initialResponse.id
        this.uuid = initialResponse.uuid
        this.version = initialResponse.version
        this.dateCreated = initialResponse.dateCreated
    }

    companion object {
        /**
         * Compute the score of a learner choice
         *
         * Depending on the type of the statement, the score is computed
         * differently
         *
         * If the statement is an exclusive choice, the score is `100` if the
         * choice is correct, `0` otherwise.
         *
         * If the statement is a multiple choice, the score is computed as follows:
         * - for each correct choice, the learner earns `100 / nbCorrectChoices`
         *   points
         * - for each incorrect choice, the learner loses `100 /
         *   nbIncorrectChoices` points
         * - the score is the sum of the points earned minus the points lost
         * - the score is then rounded to the nearest integer
         * - the score is at least 0
         *
         * If the statement is not an exclusive or multiple choice, an error is
         * thrown
         *
         * @param learnerChoice the choice made by the learner
         * @param choiceSpecification the specification of the choice
         * @return the score of the learner choice
         * @exception IllegalStateException if the choice is not an exclusive or
         *    multiple choice
         * @see ExclusiveChoiceSpecification
         * @see MultipleChoiceSpecification
         * @see LearnerChoice
         */
        fun computeScore(
            learnerChoice: LearnerChoice,
            choiceSpecification: ChoiceSpecification
        ): BigDecimal =
            when (choiceSpecification) {
                is ExclusiveChoiceSpecification ->
                    run {
                        require(learnerChoice.size <= 1) { "Cannot select more than one item with exclusive choice" }
                        if (learnerChoice.isEmpty()
                            || learnerChoice.first() != choiceSpecification.expectedChoice.index
                        ) BigDecimal(0) else BigDecimal(100)
                    }

                is MultipleChoiceSpecification ->
                    run {
                        val zero = BigDecimal(0)
                        val oneHundred = BigDecimal(100)
                        val expectedIndexList = choiceSpecification.expectedChoiceList.map { it.index }
                        var positiveScore = zero
                        var negativeScore = zero
                        if (expectedIndexList.size == choiceSpecification.nbCandidateItem) { // limit case: all choices are correct
                            positiveScore = BigDecimal(learnerChoice.size * 100.0 / choiceSpecification.nbCandidateItem)
                            negativeScore = zero
                        } else { // nominal case: only some choices are correct
                            val nbCorrectLearnerChoices = learnerChoice.intersect(expectedIndexList).size
                            val nbCorrectChoices = expectedIndexList.size
                            val nbIncorrectLearnerChoices = learnerChoice.minus(expectedIndexList).size
                            val nbIncorrectChoices = choiceSpecification.nbCandidateItem - nbCorrectChoices
                            positiveScore = BigDecimal(nbCorrectLearnerChoices * (100.0 / nbCorrectChoices))
                            negativeScore = BigDecimal(nbIncorrectLearnerChoices * (100.0 / nbIncorrectChoices))
                        }

                        var score = (positiveScore - negativeScore).setScale(0, RoundingMode.HALF_UP)
                        if (score < zero) score = zero
                        score
                    }

                else -> error("Unsupported type of choice")
            }
    }
}