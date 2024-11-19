package org.elaastic.player.sequence

import org.elaastic.player.activeinteraction.ActiveInteractionModelFactory
import org.elaastic.player.activeinteraction.ActiveInteractionModelFactory.teacherResultDashboardService
import org.elaastic.player.results.ResultsModel
import org.elaastic.sequence.ILearnerSequence
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.user.User

object SequenceModelFactory {

    fun buildForTeacher(
        teacher: User,
        sequence: Sequence,
    ): TeacherSequenceModel {
        return TeacherSequenceModel(
            sequenceProgressionModel = SequenceProgressionModelFactory.buildForTeacher(teacher, sequence),
            activeInteractionModel = ActiveInteractionModelFactory.buildForTeacher(sequence),
            resultsModel = getResultsModel(sequence),
            showResults = !sequence.isNotStarted(),
        )
    }

    fun buildForLearner(
        learner: User,
        learnerSequence: ILearnerSequence,
        learnerActiveInteraction: Interaction?,
    ): SequenceModel {
        return SequenceModel(
            sequenceProgressionModel = SequenceProgressionModelFactory.buildForLearner(
                learner,
                learnerSequence.sequence,
                learnerActiveInteraction
            ),
            activeInteractionModel = ActiveInteractionModelFactory.buildForLearner(learnerSequence),
        )
    }

    private fun getResultsModel(
        sequence: Sequence
    ): ResultsModel? {
        var resultsModel: ResultsModel? = null

        if (!sequence.isNotStarted()) {
            resultsModel = teacherResultDashboardService.buildModel(sequence)
        }

        return resultsModel
    }
}