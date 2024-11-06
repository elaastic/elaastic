package org.elaastic.questions.assignment.sequence

import org.elaastic.questions.assignment.sequence.interaction.Interaction
import org.elaastic.questions.directory.User
import org.elaastic.questions.player.phase.LearnerPhase

interface ILearnerSequence : SequenceProgress {

    val learner: User
    val sequence: Sequence

    var activeInteraction: Interaction?

    fun loadPhase(learnerPhase: LearnerPhase)

    fun getPhase(index: Int): LearnerPhase

    val phaseList: Array<LearnerPhase?>
}