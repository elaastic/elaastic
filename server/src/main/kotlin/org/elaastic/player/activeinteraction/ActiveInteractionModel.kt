package org.elaastic.player.activeinteraction

import org.elaastic.player.results.ResultsModel
import org.elaastic.player.statement.StatementInfoPanelModel
import org.elaastic.sequence.phase.LearnerPhase

open class ActiveInteractionModel(
    val statementInfoPanelModel: StatementInfoPanelModel,
)

/**
 * Contain the data about the current interaction of a learner for the player template.
 */
class LearnerActiveInteractionModel(
    statementInfoPanelModel: StatementInfoPanelModel,
    val phaseList: List<LearnerPhase>,
) : ActiveInteractionModel(
    statementInfoPanelModel = statementInfoPanelModel,
)