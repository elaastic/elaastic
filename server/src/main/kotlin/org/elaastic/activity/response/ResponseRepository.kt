package org.elaastic.activity.response

import org.elaastic.activity.results.AttemptNum
import org.elaastic.material.instructional.statement.Statement
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.user.User
import org.springframework.data.jpa.repository.JpaRepository

interface ResponseRepository : JpaRepository<Response, Long> {

    fun getAllByIdIn(ids: List<Long>): List<Response>

    fun findAllByInteractionOrderByMeanGradeDesc(interaction: Interaction): List<Response>

    fun findAllByInteractionAndFakeIsFalseOrderByMeanGradeDesc(interaction: Interaction): List<Response>

    /**
     * Query for the API : retrive only the recommended responses
     */
    fun findAllByInteractionAndRecommendedByTeacherIsTrue(interaction: Interaction): List<Response>
    fun findAllByInteractionAndAttempt(interaction: Interaction, attempt: Int = 1): List<Response>

    fun findByInteractionAndAttemptAndLearner(interaction: Interaction, attempt: AttemptNum, learner: User): Response?

    fun countByLearnerAndInteractionAndAttempt(learner: User,
                                               interaction: Interaction,
                                               attempt: AttemptNum
    ): Int

    fun countByInteractionAndAttemptAndFakeIsFalse(interaction: Interaction,
                                                   attempt: AttemptNum
    ): Int

    // TODO replace by statement Uuid
    fun countByStatement(statement: Statement): Int

    fun findAllByAttemptAndInteractionAndFakeIsTrue(attempt: AttemptNum, interaction: Interaction): List<Response>

    fun findAllByInteraction(interaction: Interaction): List<Response>
}