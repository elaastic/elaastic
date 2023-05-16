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

package org.elaastic.questions.player.components.explanationViewer

import org.elaastic.questions.assignment.choice.legacy.LearnerChoice
import org.elaastic.questions.assignment.sequence.ConfidenceDegree
import org.elaastic.questions.assignment.sequence.interaction.response.Response
import java.math.BigDecimal
import java.math.RoundingMode

open class ExplanationData(
    val content: String? = null,
    val author: String? = null,
    val nbEvaluations: Int = 0,
    meanGrade: BigDecimal? = null,
    val confidenceDegree: ConfidenceDegree? = null,
    val score: BigDecimal? = null,
    val correct: Boolean? = (score == BigDecimal(100)),
    val choiceList: LearnerChoice? = null,
) {
    constructor(response: Response) : this(
        content = response.explanation,
        author = response.learner.firstName + " " + response.learner.lastName + " (@" + response.learner.username + ")",
        nbEvaluations = response.evaluationCount,
        meanGrade = response.meanGrade,
        confidenceDegree = response.confidenceDegree,
        correct = response.score == BigDecimal(100),
        score = response.score,
        choiceList = response.learnerChoice,
    )

    val meanGrade = meanGrade
        ?.setScale(2, RoundingMode.CEILING)
        ?.stripTrailingZeros()

    open val fromTeacher = false
}
