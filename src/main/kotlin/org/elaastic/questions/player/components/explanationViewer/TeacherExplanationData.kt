package org.elaastic.questions.player.components.explanationViewer

import org.elaastic.questions.assignment.choice.legacy.LearnerChoice
import org.elaastic.questions.assignment.sequence.ConfidenceDegree
import java.math.BigDecimal

class TeacherExplanationData(
    content: String? = null,
    author: String? = null,
    nbEvaluations: Int = 0,
    meanGrade: BigDecimal? = null,
    confidenceDegree: ConfidenceDegree? = null,
    score: BigDecimal? = null,
    correct: Boolean? = (score == BigDecimal(100)),
    choiceList: LearnerChoice? = null,
) : ExplanationData(
    content, author, nbEvaluations, meanGrade, confidenceDegree, score, correct, choiceList
) {
    constructor(explanationData: ExplanationData) : this(
        content = explanationData.content,
        author = explanationData.author,
        nbEvaluations = explanationData.nbEvaluations,
        meanGrade = explanationData.meanGrade,
        confidenceDegree = explanationData.confidenceDegree,
        correct = explanationData.score == BigDecimal(100),
        score = explanationData.score,
        choiceList = explanationData.choiceList,
    )

    override val fromTeacher = true
}