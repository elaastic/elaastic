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

package org.elaastic.activity.response

import org.elaastic.material.instructional.question.QuestionType

/**
 * Represents a response in Thymeleaf templates
 * @author John Tranier
 */
interface ResponseData {
    val id: Long
    val explanation: String
    val questionType: QuestionType

    /**
     * Defined for backward compatibility (the legacy templates was unique a single ResponseData class for every
     * type of response.
     * ResponseData is now an interface ; each concrete subtype has it own well-defined accessors.
     * @deprecated
     */
    fun getChoiceList() = listOf<Int>()
}