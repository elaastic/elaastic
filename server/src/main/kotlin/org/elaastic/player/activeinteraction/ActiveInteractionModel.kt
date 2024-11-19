package org.elaastic.player.activeinteraction

import org.elaastic.player.results.ResultsModel
import org.elaastic.player.statement.StatementInfo
import org.elaastic.sequence.phase.LearnerPhase

open class ActiveInteractionModel(
    val statementInfo: StatementInfo,
)

/**
 * Contain the data about the current interaction of a teacher for the player template.
 */
class TeacherActiveInteractionModel(
    statementInfo: StatementInfo,
    val resultsModel: ResultsModel?,
    val showResults: Boolean,
) : ActiveInteractionModel(
    statementInfo = statementInfo,
)

/**
 * Contain the data about the current interaction of a learner for the player template.
 */
class LearnerActiveInteractionModel(
    statementInfo: StatementInfo,
    val phaseList: List<LearnerPhase>,
) : ActiveInteractionModel(
    statementInfo = statementInfo,
)