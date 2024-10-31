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

package org.elaastic.questions.player.components.results

import ConfidenceDistributionChartModel
import EvaluationDistributionChartModel
import org.elaastic.questions.player.components.explanationViewer.ExplanationViewerModel
import org.elaastic.questions.player.components.recommendation.RecommendationModel
import org.elaastic.questions.player.components.responseDistributionChart.ResponseDistributionChartModel

data class ChoiceResultsModel(
        override val sequenceIsStopped: Boolean,
        override val sequenceId: Long,
        val responseDistributionChartModel: ResponseDistributionChartModel? = null,
        val confidenceDistributionChartModel: ConfidenceDistributionChartModel? = null,
        val evaluationDistributionChartModel: EvaluationDistributionChartModel? = null,
        override val userCanRefreshResults: Boolean = true,
        override val userCanDisplayStudentsIdentity: Boolean = false,
        override val explanationViewerModel: ExplanationViewerModel? = null,
        override val recommendationModel: RecommendationModel? = null
        ) : ResultsModel {
    val hasAnyResult = !responseDistributionChartModel?.results.isNullOrEmpty()
    override val hasExplanations = (explanationViewerModel?.nbExplanations ?: 0) > 0
    override fun getHasChoices() = true
}
