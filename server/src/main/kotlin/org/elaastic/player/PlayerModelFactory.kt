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
package org.elaastic.player

import org.elaastic.common.web.MessageBuilder
import org.elaastic.moderation.ReportInformation
import org.elaastic.player.assignmentview.AssignmentOverviewModelFactory
import org.elaastic.player.command.CommandModelFactory
import org.elaastic.player.results.TeacherResultDashboardService
import org.elaastic.player.sequence.status.SequenceInfoResolver
import org.elaastic.player.statement.StatementInfoPanelModel
import org.elaastic.player.steps.SequenceStatistics
import org.elaastic.player.steps.StepsModelFactory
import org.elaastic.sequence.ILearnerSequence
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.State
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.user.User

object PlayerModelFactory {

    /**
     * @param nbReportBySequence Map of sequence to (a pair of (total numbers of reports) and (number of reports to
     *    moderate))
     */
    fun buildForTeacher(
        user: User,
        sequence: Sequence,
        serverBaseUrl: String,
        nbRegisteredUsers: Int,
        messageBuilder: MessageBuilder,
        sequenceStatistics: SequenceStatistics,
        teacherResultDashboardService: TeacherResultDashboardService,
        nbReportBySequence: Map<Sequence, ReportInformation>,
    ): TeacherPlayerModel {
        val assignment = sequence.assignment ?: error("The sequence must have an assignment to be played")
        val showResults = sequence.state != State.beforeStart

        return TeacherPlayerModel(
            serverBaseUrl = serverBaseUrl,
            sequence = sequence,
            assignmentOverviewModel = AssignmentOverviewModelFactory.build(
                nbRegisteredUser = nbRegisteredUsers,
                assignment = assignment,
                selectedSequenceId = sequence.id,
                teacher = true,
                nbReportBySequence = nbReportBySequence,
            ),
            stepsModel = StepsModelFactory.buildForTeacher(sequence),
            sequenceStatistics = sequenceStatistics,
            commandModel = CommandModelFactory.build(user, sequence),
            sequenceInfoModel = SequenceInfoResolver.resolve(
                true,
                sequence,
                messageBuilder,
                nbReportBySequence[sequence] ?: ReportInformation.empty
            ),
            statementInfoPanelModel = StatementInfoPanelModel(
                sequence.statement,
                hideStatement = false,
                panelClosed = sequence.state != State.beforeStart
            ),
            showResults = showResults,
            resultsModel = if (showResults)
                teacherResultDashboardService.buildModel(sequence)
            else null,
            assignmentOverviewModelOneSequence = AssignmentOverviewModelFactory.buildOnSequence(
                teacher = true,
                assignment = assignment,
                nbRegisteredUser = nbRegisteredUsers,
                selectedSequence = sequence,
            )
        )
    }


    fun buildForLearner(
        sequence: Sequence,
        nbRegisteredUsers: Int,
        messageBuilder: MessageBuilder,
        activeInteraction: Interaction?,
        learnerSequence: ILearnerSequence,
    ): LearnerPlayerModel = run {
        val assignment = sequence.assignment ?: error("The sequence must have an assignment to be played")

        LearnerPlayerModel(
            sequence = sequence,
            assignmentOverviewModel = AssignmentOverviewModelFactory.build(
                nbRegisteredUser = nbRegisteredUsers,
                assignment = assignment,
                selectedSequenceId = sequence.id,
                teacher = false,
            ),
            stepsModel = StepsModelFactory.buildForLearner(sequence, activeInteraction),
            sequenceInfoModel = SequenceInfoResolver.resolve(false, sequence, messageBuilder),
            statementInfoPanelModel = StatementInfoPanelModel(
                sequence.statement,
                hideStatement = sequence.state == State.beforeStart,
                panelClosed = false
            ),
            phaseList = learnerSequence.phaseList.filterNotNull()
        )
    }

}                                                        