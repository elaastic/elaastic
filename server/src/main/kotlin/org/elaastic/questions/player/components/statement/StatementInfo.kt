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

package org.elaastic.questions.player.components.statement

import org.elaastic.questions.assignment.QuestionType
import org.elaastic.questions.subject.statement.Statement
import org.elaastic.questions.attachment.Attachment

data class StatementInfo(
        val title: String,
        val questionType: QuestionType,
        val content: String,
        val attachment: Attachment? = null
) {
    constructor(statement: Statement) :
            this(
                    statement.title,
                    statement.questionType,
                    statement.content,
                    statement.attachment
            )
}
