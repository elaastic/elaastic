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

package org.elaastic.player.statement

import org.elaastic.material.instructional.question.QuestionType
import org.elaastic.material.instructional.question.attachment.Attachment
import org.elaastic.material.instructional.statement.Statement

data class StatementInfo(
    val title: String,
    val questionType: QuestionType,
    val content: String,
    val attachment: Attachment? = null,
    val panelClosed: Boolean = false,
    val hideQuestionType: Boolean = false,
    val hideStatement: Boolean = false,
) {
    constructor(
        statement: Statement,
        panelClosed: Boolean = false,
        hideQuestionType: Boolean = false,
        hideStatement: Boolean = false
    ) : this(
        statement.title,
        statement.questionType,
        statement.content,
        statement.attachment,
        panelClosed,
        hideQuestionType,
        hideStatement,
    )
}
