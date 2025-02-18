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

package org.elaastic.user.controller.command

import org.elaastic.user.HasPasswords
import org.elaastic.user.User
import org.elaastic.user.validation.PasswordsMustBeIdentical
import org.springframework.validation.BindingResult
import javax.validation.constraints.Size

@PasswordsMustBeIdentical
class PasswordData(
        val id: Long?,
        val password: String? = null,
        @field:Size(min = 4) override val password1: String? = null,
        override val password2: String? = null
) : HasPasswords {

    constructor(user: User) : this(
            user.id
    )

    fun catchSecurityException(e: SecurityException, result: BindingResult) {
        when {
            "Bad.user.password" == e.message -> result.rejectValue("password", "Bad")
            else -> throw e
        }
    }
}
