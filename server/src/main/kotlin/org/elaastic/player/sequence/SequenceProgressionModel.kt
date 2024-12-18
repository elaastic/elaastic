package org.elaastic.player.sequence

import org.elaastic.player.command.CommandModel
import org.elaastic.player.sequence.status.SequenceInfoModel
import org.elaastic.player.steps.SequenceStatistics
import org.elaastic.player.steps.StepsModel

/**
 * Contain the data about the progression and the status of a sequence. It's used for learners as they only need this
 * information.
 *
 * @param stepsModel The model containing the progression of the sequence.
 * @param sequenceInfoModel The model containing the status of the sequence.
 */
open class SequenceProgressionModel(
    val stepsModel: StepsModel,
    val sequenceInfoModel: SequenceInfoModel,
)

/**
 * Contain the data about the progression, the status, the stats of a sequence and the command to manage it. It's used
 * for teachers as they need, in addition to see the progression, to manage the sequence.
 *
 * @param stepsModel The model containing the progression of the sequence.
 * @param sequenceStatistics The statistics of the sequence.
 * @param commandModel The model containing the command to manage the sequence.
 * @param sequenceInfoModel The model containing the status of the sequence.
 */
class SequenceOrchestrationModel(
    stepsModel: StepsModel,
    val sequenceStatistics: SequenceStatistics,
    val commandModel: CommandModel,
    sequenceInfoModel: SequenceInfoModel,
) : SequenceProgressionModel(
    stepsModel = stepsModel,
    sequenceInfoModel = sequenceInfoModel,
)