package org.elaastic.player.activeinteraction

import org.elaastic.player.results.ResultsModel
import org.elaastic.player.results.TeacherResultDashboardService
import org.elaastic.player.statement.StatementInfoPanelModel
import org.elaastic.sequence.ILearnerSequence
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.State
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
object ActiveInteractionModelFactory {

    @Autowired
    lateinit var teacherResultDashboardService: TeacherResultDashboardService

    fun buildForTeacher(
        sequence: Sequence,
    ): ActiveInteractionModel = ActiveInteractionModel(
        statementInfoPanelModel = StatementInfoPanelModel(
            sequence.statement,
            hideStatement = false,
            panelClosed = !sequence.isNotStarted()
        ),
    )

    fun buildForLearner(
        learnerSequence: ILearnerSequence,
    ): LearnerActiveInteractionModel = LearnerActiveInteractionModel(
        statementInfoPanelModel = StatementInfoPanelModel(
            learnerSequence.sequence.statement,
            hideStatement = learnerSequence.sequence.state == State.beforeStart,
            panelClosed = false
        ),
        phaseList = learnerSequence.phaseList.filterNotNull(),
    )
}