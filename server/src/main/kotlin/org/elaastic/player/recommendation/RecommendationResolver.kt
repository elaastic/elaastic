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

package org.elaastic.player.recommendation

import org.elaastic.activity.evaluation.peergrading.PeerGrading
import org.elaastic.activity.response.ResponseSet
import org.elaastic.common.web.MessageBuilder
import org.elaastic.sequence.Sequence
import java.math.BigDecimal

object RecommendationResolver {

    /**
     * The limit of response in the first phase to build the recommendation.
     */
    private const val LIMIT_TO_BUILD = 10

    /**
     * Resolve the recommendation for the given sequence.
     *
     * If the number of responses for the first phase is less than [LIMIT_TO_BUILD], the recommendation is null.
     *
     * @param responseSet the response set
     * @param peerGradings the peer gradings
     * @param sequence the sequence
     * @param messageBuilder the message builder
     * @return the recommendation model
     */
    fun resolve(
        responseSet: ResponseSet,
        peerGradings: List<PeerGrading>?,
        sequence: Sequence,
        messageBuilder: MessageBuilder
    ): RecommendationModel? {
        var result: RecommendationModel? = null
        val firstResponses = responseSet[1].filter { response -> !response.fake }
        if (firstResponses.size >= LIMIT_TO_BUILD) {
            if (sequence.recommendableAfterPhase1()) {
                val p1 = IndicatorCalculator.computeP(firstResponses)
                val noCorrectExplanations =
                    responseSet[1].filter { response -> response.score?.compareTo(BigDecimal(100.00)) == 0 }
                        .all { r -> r.explanation.isNullOrEmpty() }
                val pConf = IndicatorCalculator.computePConf(firstResponses)
                if (noCorrectExplanations) {
                    result = RecommendationModel(
                        noCorrectExplanation = true,
                        message = messageBuilder.message("player.sequence.recommendation.skipPhase2.message"),
                        popupDetailedExplanation = PopupDetailedExplanation.NO_EXPLANATION_FOR_CORRECT_ANSWERS
                    )
                } else {
                    val p1Prop = getP1Property(p1)
                    val pConfProp = getPConfProperty(pConf)
                    when (val pairToCompare = Pair(p1Prop, pConfProp)) {
                        Pair(ExplanationP1.VERY_HIGH, pairToCompare.second) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.skipPhase2.weak_benefits.message"),
                                explanationP1 = p1Prop,
                                popupDetailedExplanation = PopupDetailedExplanation.WEAK_BENEFITS
                            )

                        Pair(ExplanationP1.TOO_LOW, ExplanationPConf.PCONF_NEG) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.skipPhase2.message"),
                                explanationP1 = p1Prop,
                                explanationPConf = pConfProp,
                                popupDetailedExplanation = PopupDetailedExplanation.NON_SIGNIFICANT_BENEFITS
                            )

                        Pair(ExplanationP1.TOO_LOW, ExplanationPConf.PCONF_POS),
                        Pair(ExplanationP1.TOO_LOW, ExplanationPConf.PCONF_ZERO) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.provide_hint.message"),
                                explanationP1 = p1Prop,
                                explanationPConf = pConfProp,
                                popupDetailedExplanation = PopupDetailedExplanation.WEAK_BENEFITS
                            )

                        Pair(ExplanationP1.TOO_LOW, ExplanationPConf.PCONF_NULL) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.provide_hint.message"),
                                explanationP1 = p1Prop,
                                popupDetailedExplanation = PopupDetailedExplanation.WEAK_BENEFITS
                            )
                    }
                }
            } else if (sequence.recommendableAfterPhase2()) {
                if (phase2WasSkipped(peerGradings)) {
                    val p1 = IndicatorCalculator.computeP(firstResponses)
                    val noCorrectExplanations =
                        responseSet[1].filter { response -> response.score?.compareTo(BigDecimal(100.00)) == 0 }
                            .all { r -> r.explanation.isNullOrEmpty() }
                    val pConf = IndicatorCalculator.computePConf(firstResponses)
                    if (noCorrectExplanations) {
                        result = RecommendationModel(
                            noCorrectExplanation = true,
                            message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_detailed"),
                            popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT,
                            recommendedExplanationsComparator = IncorrectAndConfidenceDegreeComparator()
                        )
                    } else {
                        val p1Prop = getP1PropertySkip(p1)
                        val pConfProp = getPConfPropertySkip(pConf)
                        when (Pair(p1Prop, pConfProp)) {
                            Pair(ExplanationP1.VERY_HIGH_SKIP, ExplanationPConf.PCONF_NEG_SKIP) ->
                                result = RecommendationModel(
                                    message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_brief"),
                                    explanationP1 = p1Prop,
                                    explanationPConf = pConfProp,
                                    popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT,
                                    recommendedExplanationsComparator = IncorrectAndConfidenceDegreeComparator()
                                )

                            Pair(ExplanationP1.VERY_HIGH_SKIP, ExplanationPConf.PCONF_POS_SKIP),
                            Pair(ExplanationP1.VERY_HIGH_SKIP, ExplanationPConf.PCONF_ZERO_SKIP) ->
                                result = RecommendationModel(
                                    message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_brief"),
                                    explanationP1 = p1Prop,
                                    explanationPConf = pConfProp,
                                    popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT,
                                    recommendedExplanationsComparator = CorrectAndConfidenceDegreeComparator()
                                )

                            Pair(ExplanationP1.VERY_HIGH_SKIP, ExplanationPConf.PCONF_NULL_SKIP) ->
                                result = RecommendationModel(
                                    message = messageBuilder.message("player.sequence.recommendation.focus_on_correct"),
                                    explanationP1 = p1Prop,
                                    popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT,
                                    recommendedExplanationsComparator = CorrectAndConfidenceDegreeComparator()
                                )

                            Pair(ExplanationP1.TOO_LOW_SKIP, ExplanationPConf.PCONF_NEG_SKIP) ->
                                result = RecommendationModel(
                                    message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_detailed"),
                                    explanationP1 = p1Prop,
                                    explanationPConf = pConfProp,
                                    popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT,
                                    recommendedExplanationsComparator = IncorrectAndConfidenceDegreeComparator()
                                )

                            Pair(ExplanationP1.TOO_LOW_SKIP, ExplanationPConf.PCONF_POS_SKIP),
                            Pair(ExplanationP1.TOO_LOW_SKIP, ExplanationPConf.PCONF_ZERO_SKIP) ->
                                result = RecommendationModel(
                                    message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                                    explanationP1 = p1Prop,
                                    explanationPConf = pConfProp,
                                    popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT,
                                    recommendedExplanationsComparator = CorrectAndConfidenceDegreeComparator()
                                )

                            Pair(ExplanationP1.TOO_LOW_SKIP, ExplanationPConf.PCONF_NULL_SKIP) ->
                                result = RecommendationModel(
                                    message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect"),
                                    explanationP1 = p1Prop,
                                    popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT,
                                    recommendedExplanationsComparator = IncorrectAndConfidenceDegreeComparator()
                                )
                        }
                    }
                } else {
                    val pPeer = IndicatorCalculator.computePPeer(peerGradings)
                    val d = IndicatorCalculator.computeD(responseSet)
                    val dProp = getDProperty(d)
                    val pPeerProp = getPPeerProperty(pPeer)
                    when (Pair(pPeerProp, dProp)) {
                        Pair(ExplanationPPeer.PPEER_POS, ExplanationD.D_NEG),
                        Pair(ExplanationPPeer.PPEER_ZERO, ExplanationD.D_NEG),
                        Pair(ExplanationPPeer.PPEER_POS, ExplanationD.D_ZERO),
                        Pair(ExplanationPPeer.PPEER_ZERO, ExplanationD.D_ZERO) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.focus_on_correct_detailed"),
                                explanationD = dProp,
                                explanationPPeer = pPeerProp,
                                popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT,
                                recommendedExplanationsComparator = CorrectAndMeanGradeComparator()
                            )

                        Pair(ExplanationPPeer.PPEER_POS, ExplanationD.D_POS),
                        Pair(ExplanationPPeer.PPEER_ZERO, ExplanationD.D_POS) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.focus_on_correct"),
                                explanationD = dProp,
                                explanationPPeer = pPeerProp,
                                popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_CORRECT,
                                recommendedExplanationsComparator = CorrectAndMeanGradeComparator()
                            )

                        Pair(ExplanationPPeer.PPEER_NEG, ExplanationD.D_NEG),
                        Pair(ExplanationPPeer.PPEER_NEG, ExplanationD.D_ZERO) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect_detailed"),
                                explanationD = dProp,
                                explanationPPeer = pPeerProp,
                                popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT,
                                recommendedExplanationsComparator = IncorrectAndMeanGradeComparator()
                            )

                        Pair(ExplanationPPeer.PPEER_NEG, ExplanationD.D_POS) ->
                            result = RecommendationModel(
                                message = messageBuilder.message("player.sequence.recommendation.focus_on_incorrect"),
                                explanationD = dProp,
                                explanationPPeer = pPeerProp,
                                popupDetailedExplanation = PopupDetailedExplanation.POPULAR_ANSWERS_INCORRECT,
                                recommendedExplanationsComparator = IncorrectAndMeanGradeComparator()
                            )
                    }
                }
            }
        }
        return result
    }

    private fun phase2WasSkipped(peerGradings: List<PeerGrading>?) =
        peerGradings == null || peerGradings.isEmpty()

    private fun getP1Property(p1: Float?): ExplanationP1 =
        if (p1 == null) {
            ExplanationP1.NULL
        } else if (IndicatorCalculator.veryHigh(p1)) {
            ExplanationP1.VERY_HIGH
        } else if (IndicatorCalculator.tooLow(p1)) {
            ExplanationP1.TOO_LOW
        } else {
            ExplanationP1.ADEQUATE
        }

    private fun getP1PropertySkip(p1: Float?): ExplanationP1 =
        if (p1 == null) {
            ExplanationP1.NULL
        } else if (IndicatorCalculator.veryHigh(p1)) {
            ExplanationP1.VERY_HIGH_SKIP
        } else if (IndicatorCalculator.tooLow(p1)) {
            ExplanationP1.TOO_LOW_SKIP
        } else {
            ExplanationP1.ADEQUATE
        }

    private fun getPConfProperty(pConf: Float?): ExplanationPConf =
        if (pConf == null) ExplanationPConf.PCONF_NULL
        else if (pConf < 0f) ExplanationPConf.PCONF_NEG
        else if (pConf > 0f) ExplanationPConf.PCONF_POS
        else ExplanationPConf.PCONF_ZERO

    private fun getPConfPropertySkip(pConf: Float?): ExplanationPConf =
        if (pConf == null) ExplanationPConf.PCONF_NULL_SKIP
        else if (pConf < 0f) ExplanationPConf.PCONF_NEG_SKIP
        else if (pConf > 0f) ExplanationPConf.PCONF_POS_SKIP
        else ExplanationPConf.PCONF_ZERO_SKIP

    private fun getPPeerProperty(pPeer: Float?): ExplanationPPeer =
        if (pPeer == null) ExplanationPPeer.PPEER_NULL
        else if (pPeer < 0f) ExplanationPPeer.PPEER_NEG
        else if (pPeer > 0f) ExplanationPPeer.PPEER_POS
        else ExplanationPPeer.PPEER_ZERO

    private fun getDProperty(d: Float?): ExplanationD? =
        if (d == null) ExplanationD.D_NULL
        else if (d < 0f) ExplanationD.D_NEG
        else if (d > 0f) ExplanationD.D_POS
        else ExplanationD.D_ZERO

}
