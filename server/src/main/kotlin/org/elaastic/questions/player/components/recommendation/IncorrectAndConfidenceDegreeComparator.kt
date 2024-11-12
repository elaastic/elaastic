package org.elaastic.questions.player.components.recommendation

import org.elaastic.player.explanations.ExplanationData

class IncorrectAndConfidenceDegreeComparator : Comparator<ExplanationData> {
    override fun compare(expl1: ExplanationData?, expl2: ExplanationData?): Int =

            /* better safe than sorry */
            if (expl1 == null && expl2 == null) 0
            else if (expl1 == null && expl2 != null) -1
            else if (expl1 != null && expl2 == null) 1
            else if (expl1!!.score == expl2!!.score) {
                    if (expl1.confidenceDegree == null && expl2.confidenceDegree == null) 0
                    else if (expl1.confidenceDegree == null && expl2.confidenceDegree != null) -1
                    else if (expl1.confidenceDegree != null && expl2.confidenceDegree == null) 1
                    else (expl1.confidenceDegree!!.ordinal.minus(expl2.confidenceDegree!!.ordinal))
            }
            else if (expl1.score == null && expl2.score == null) 0
            else if (expl1.score == null && expl2.score != null) -1
            else if (expl1.score != null && expl2.score == null) 1
            else (expl2.score!!.minus(expl1.score!!)).signum()



}