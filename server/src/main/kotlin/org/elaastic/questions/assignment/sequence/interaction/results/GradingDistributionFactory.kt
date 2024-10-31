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

package org.elaastic.questions.assignment.sequence.interaction.results

import org.elaastic.questions.assignment.choice.ChoiceSpecification
import org.elaastic.questions.assignment.sequence.peergrading.PeerGrading
import java.math.BigDecimal

object GradingDistributionFactory {

    fun build(choiceSpecification: ChoiceSpecification,
              peerGradings: List<PeerGrading>?): GradingDistribution{
        val gradingDistributionList = mutableListOf<GradingDistributionOnResponse>()
        if(peerGradings != null){
            for(choiceIndex in 1..choiceSpecification.nbCandidateItem){
                gradingDistributionList.add(GradingDistributionOnResponse(
                        peerGradings.filter{pg -> pg.grade?.compareTo(BigDecimal(-1)) != 0 },
                        choiceIndex
                ))
            }
        }
        return GradingDistribution(gradingDistributionList)
    }
}
