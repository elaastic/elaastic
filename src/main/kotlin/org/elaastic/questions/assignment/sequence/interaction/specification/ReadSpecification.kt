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

package org.elaastic.questions.assignment.sequence.interaction.specification

import org.elaastic.questions.assignment.sequence.interaction.InteractionType

/**
 * ReadSpecification is a class that represents the specification of the
 * Read phase.
 */
class ReadSpecification : InteractionSpecification {
    override fun getType(): InteractionType =
        InteractionType.Read

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ReadSpecification) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }


}
