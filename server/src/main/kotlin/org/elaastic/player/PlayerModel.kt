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

import org.elaastic.sequence.Sequence
import org.elaastic.player.assignmentview.AssignmentOverviewModel
import org.elaastic.player.command.CommandModel
import org.elaastic.player.results.ResultsModel
import org.elaastic.player.sequence.status.SequenceInfoModel
import org.elaastic.player.statement.StatementInfoPanelModel
import org.elaastic.player.steps.SequenceStatistics
import org.elaastic.player.steps.StepsModel
import org.elaastic.sequence.phase.LearnerPhase

abstract class PlayerModel(
    val sequence: Sequence,
    val assignmentOverviewModel: AssignmentOverviewModel,
) {
    fun getAssignment() = sequence.assignment

    abstract fun isTeacher(): Boolean
}

class TeacherPlayerModel(
    val serverBaseUrl: String,
    sequence: Sequence,
    assignmentOverviewModel: AssignmentOverviewModel,
    val assignmentOverviewModelOneSequence: AssignmentOverviewModel,
) : PlayerModel(
    sequence = sequence,
    assignmentOverviewModel = assignmentOverviewModel,
) {
    override fun isTeacher() = true
}

class LearnerPlayerModel(
    sequence: Sequence,
    assignmentOverviewModel: AssignmentOverviewModel,
) : PlayerModel(
    sequence = sequence,
    assignmentOverviewModel = assignmentOverviewModel,
) {
    override fun isTeacher() = false
}