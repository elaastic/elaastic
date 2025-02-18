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

package org.elaastic.sequence

import org.elaastic.material.instructional.statement.Statement
import org.elaastic.sequence.interaction.Interaction
import org.elaastic.sequence.interaction.InteractionType
import org.elaastic.user.User

object SequenceGenerator {

    var idSequence: Long = 999999
    var idInteractionGenerator: Long = 999999

    fun generateAllTypes(user: User): List<Sequence> =
            ExecutionContext.values().flatMap { executionContext ->
                // sequenceState: beforeStart
                listOf(
                        SequenceInfoSetup(
                                executionContext = executionContext,
                                sequenceState = State.beforeStart
                        )
                ) +

                        // sequenceState: show
                        InteractionType.values().flatMap { interactionType ->
                            State.values().flatMap { interactionState ->
                                when (interactionType) {
                                    InteractionType.Read ->
                                        listOf(true, false).map { resultsArePublished ->
                                            SequenceInfoSetup(
                                                    executionContext = executionContext,
                                                    sequenceState = State.show,
                                                    interactionType = interactionType,
                                                    interactionState = interactionState,
                                                    resultsArePublished = resultsArePublished
                                            )
                                        }
                                    else ->
                                        listOf(
                                                SequenceInfoSetup(
                                                        executionContext = executionContext,
                                                        sequenceState = State.show,
                                                        interactionType = interactionType,
                                                        interactionState = interactionState
                                                )
                                        )
                                }
                            }
                        } +
                        // sequenceState: afterStop
                        SequenceInfoSetup(
                                executionContext = executionContext,
                                sequenceState = State.afterStop
                        )
            }.map { setup ->
                Sequence(
                        owner = user,
                        rank = 0,
                        state = setup.sequenceState,
                        statement = Statement.createDefaultStatement(user).also { it.id = (++idSequence) },
                        assignment = null,
                        executionContext = setup.executionContext,
                        resultsArePublished = setup.resultsArePublished
                ).let {
                    it.id = (++idSequence)
                    if (setup.interactionType != null) {
                        val interaction = Interaction(
                                owner = user,
                                interactionType = setup.interactionType,
                                rank = when (setup.interactionType) {
                                    InteractionType.ResponseSubmission -> 1
                                    InteractionType.Evaluation -> 2
                                    InteractionType.Read -> 3
                                },
                                sequence = it,
                                state = setup.interactionState!!
                        )
                        interaction.id = (++idInteractionGenerator)

                        it.activeInteraction = interaction
                    }

                    it
                }
            }


    data class SequenceInfoSetup(
        val executionContext: ExecutionContext,
        val sequenceState: State,
        val resultsArePublished: Boolean = false,
        val interactionType: InteractionType? = null,
        val interactionState: State? = null
    )
}
