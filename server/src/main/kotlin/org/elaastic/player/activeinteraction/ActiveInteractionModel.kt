package org.elaastic.player.activeinteraction

import org.elaastic.player.results.ResultsModel
import org.elaastic.player.statement.StatementInfo
import org.elaastic.player.statement.StatementPanelModel
import org.elaastic.sequence.phase.LearnerPhase

open class ActiveInteractionModel(
    val statementPanelModel: StatementPanelModel, // TODO merge into statementInfo
    val statementInfo: StatementInfo,
)

/**
 * Contain the data about the current interaction of a teacher for the player template.
 */
class TeacherActiveInteractionModel(
    statementPanelModel: StatementPanelModel,
    statementInfo: StatementInfo,
    val resultsModel: ResultsModel?,
    val showResults: Boolean,
) : ActiveInteractionModel(
    statementPanelModel = statementPanelModel,
    statementInfo = statementInfo,
)

/**
 * Contain the data about the current interaction of a learner for the player template.
 */
class LearnerActiveInteractionModel(
    statementPanelModel: StatementPanelModel,
    statementInfo: StatementInfo,
    val phaseList: List<LearnerPhase>,
) : ActiveInteractionModel(
    statementPanelModel = statementPanelModel,
    statementInfo = statementInfo,
)