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

package org.elaastic.moderation

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ModerationUIController(
    @Autowired val messageSource: MessageSource,
) {

    @GetMapping("/ui/utility-grade")
    fun utilityGrade(
        model: Model
    ): String {
        val locale = LocaleContextHolder.getLocale()
        val utilityGradeI18NKey = "player.sequence.chatGptEvaluation.utilityGrade"
        val gradeList = UtilityGrade.values().map {
            Grade(
                it.name,
                messageSource.getMessage("$utilityGradeI18NKey.${it.name}", null, locale)
            )
        }
        model["utilityGrade"] = gradeList
        return "moderation/components/utility-grade"
    }

    @GetMapping("/ui/utility-grade/test")
    fun utilityGradeTest() = "moderation/test-utility-grade"

    class Grade(val value: String, val label: String) {
        override fun toString(): String {
            return "{value='$value', label='$label'}"
        }
    }
}