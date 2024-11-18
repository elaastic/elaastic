package org.elaastic.player.activeinteraction

import org.elaastic.player.results.ResultsModel
import org.elaastic.player.statement.StatementInfo
import org.elaastic.player.statement.StatementPanelModel
import org.elaastic.sequence.phase.LearnerPhase

open class ActiveInteractionModel(
    val statementPanelModel: StatementPanelModel, // TODO merge with statementInfo
    val statementInfo: StatementInfo,
)

class TeacherActiveInteractionModel(
    statementPanelModel: StatementPanelModel,
    statementInfo: StatementInfo,
    val resultsModel: ResultsModel?,
    val showResults: Boolean,
) : ActiveInteractionModel(
    statementPanelModel = statementPanelModel,
    statementInfo = statementInfo,
)

class LearnerActiveInteractionModel(
    statementPanelModel: StatementPanelModel,
    statementInfo: StatementInfo,
    val phaseList: List<LearnerPhase>,
) : ActiveInteractionModel(
    statementPanelModel = statementPanelModel,
    statementInfo = statementInfo,
)