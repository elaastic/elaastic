package org.elaastic.player.sequence

import org.elaastic.common.web.MessageBuilder
import org.elaastic.player.command.CommandModelFactory
import org.elaastic.player.sequence.status.SequenceInfoResolver
import org.elaastic.player.steps.SequenceStatistics
import org.elaastic.player.steps.StepsModelFactory
import org.elaastic.sequence.Sequence
import org.elaastic.sequence.SequenceService
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
object SequenceProgressionModelFactory {

    @Autowired
    lateinit var sequenceService: SequenceService

    @Autowired
    lateinit var messageBuilder: MessageBuilder

    fun buildForTeacher(
        teacher: User,
        sequence: Sequence,
    ): SequenceOrchestrationModel = SequenceOrchestrationModel(
        stepsModel = StepsModelFactory.buildForTeacher(sequence),
        sequenceInfoModel = getSequenceInfoModel(sequence, teacher),
        sequenceStatistics = getSequenceStatistics(sequence),
        commandModel = CommandModelFactory.build(teacher, sequence),
    )

    fun buildForLearner(
        learner: User,
        sequence: Sequence,
        learnerActiveInteraction: Interaction?,
    ): SequenceProgressionModel = SequenceProgressionModel(
        stepsModel = StepsModelFactory.buildForLearner(sequence, learnerActiveInteraction),
        sequenceInfoModel = getSequenceInfoModel(sequence, learner)
    )

    private fun getSequenceInfoModel(
        sequence: Sequence,
        user: User
    ) = SequenceInfoResolver.resolve(
        isTeacher = sequence.owner == user,
        sequence = sequence,
        nbReportedEvaluation = getNbRerportedEvaluation(sequence, user),
        messageBuilder = messageBuilder,
    )

    private fun getNbRerportedEvaluation(sequence: Sequence, user: User): Pair<Int, Int> {
        return sequenceService.getNbReportBySequence(sequence, sequence.owner == user)
    }

    private fun getSequenceStatistics(sequence: Sequence): SequenceStatistics = sequenceService.getStatistics(sequence)
}