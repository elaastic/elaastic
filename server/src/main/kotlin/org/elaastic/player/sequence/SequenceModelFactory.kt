package org.elaastic.player.sequence

import org.elaastic.player.activeinteraction.ActiveInteractionModelFactory
import org.elaastic.player.results.ResultsModel
import org.elaastic.player.results.TeacherResultDashboardService
import org.elaastic.sequence.ILearnerSequence
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class SequenceModelFactory (
    @Autowired val teacherResultDashboardService: TeacherResultDashboardService,
    @Autowired val sequenceProgressionModelFactory: SequenceProgressionModelFactory,
){

    fun buildForTeacher(
        teacher: User,
        sequence: Sequence,
    ): TeacherSequenceModel {
        return TeacherSequenceModel(
            sequenceProgressionModel = sequenceProgressionModelFactory.buildForTeacher(teacher, sequence),
            activeInteractionModel = ActiveInteractionModelFactory.buildForTeacher(sequence),
            resultsModel = getResultsModel(sequence),
            showResults = !sequence.isNotStarted(),
            sequenceId = sequence.id,
        )
    }

    fun buildForLearner(
        learner: User,
        learnerSequence: ILearnerSequence,
        learnerActiveInteraction: Interaction?,
    ): SequenceModel {
        return SequenceModel(
            sequenceProgressionModel = sequenceProgressionModelFactory.buildForLearner(
                learner,
                learnerSequence.sequence,
                learnerActiveInteraction
            ),
            activeInteractionModel = ActiveInteractionModelFactory.buildForLearner(learnerSequence),
            sequenceId = learnerSequence.sequence.id,
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