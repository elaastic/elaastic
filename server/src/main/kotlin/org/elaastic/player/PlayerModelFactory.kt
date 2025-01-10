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

import org.elaastic.moderation.ReportInformation
import org.elaastic.player.assignmentview.AssignmentOverviewModelFactory
import org.elaastic.sequence.Sequence

object PlayerModelFactory {

    /**
     * @param nbReportBySequence Map of a sequence to the [ReportInformation] of the sequence
     */
    fun buildForTeacher(
        sequence: Sequence,
        serverBaseUrl: String,
        nbRegisteredUsers: Int,
        nbReportBySequence: Map<Sequence, ReportInformation>,
    ): TeacherPlayerModel {
        val assignment = sequence.assignment ?: error("The sequence must have an assignment to be played")

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
        )
    }

}                                                        