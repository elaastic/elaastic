package org.elaastic.activity.evaluation.peergrading

import org.elaastic.activity.response.Response
import java.math.BigDecimal

class ResponseInfo(
        val id: Long,
        val correct: Boolean,
        val evaluable: Boolean = true,
        var nbSelection: Int = 0
) : Comparable<ResponseInfo> {

    constructor(response: Response) : this(
            response.id!!,
            response.score?.compareTo(BigDecimal(100)) == 0,
        (response.explanation?.length ?: 0) > ResponseRecommendationService.MIN_SIZE_OF_EXPLANATION_TO_BE_EVALUATED
    )

    override fun compareTo(other: ResponseInfo): Int =
            this.nbSelection.compareTo(other.nbSelection)

    override fun toString(): String =
            "<$id, $correct, $nbSelection>"
}
