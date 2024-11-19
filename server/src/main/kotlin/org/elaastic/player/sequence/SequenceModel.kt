package org.elaastic.player.sequence

import org.elaastic.player.activeinteraction.ActiveInteractionModel
import org.elaastic.player.results.ResultsModel

/** Contain the data about the sequence currently being played. It's used for learners in the player template. */
open class SequenceModel(
    val sequenceProgressionModel: SequenceProgressionModel,
    val activeInteractionModel: ActiveInteractionModel,
)

/**
 * Contain the data about the sequence currently being played. It's used for teachers in the player template. In
 * addition to the data contained in [SequenceModel], it contains the results of the sequence who is used by the
 * Recommendations template.
 */
class TeacherSequenceModel(
    sequenceProgressionModel: SequenceOrchestrationModel,
    activeInteractionModel: ActiveInteractionModel,
    val resultsModel: ResultsModel?,
    val showResults: Boolean,
) : SequenceModel(
    sequenceProgressionModel = sequenceProgressionModel,
    activeInteractionModel = activeInteractionModel,
)