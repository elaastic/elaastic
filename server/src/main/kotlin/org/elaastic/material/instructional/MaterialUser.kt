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

package org.elaastic.material.instructional

import org.elaastic.user.validation.PlainTextPasswordIsTooShort
import org.elaastic.user.validation.ValidateHasEmailOrHasOwnerOrHasExternalSource
import org.elaastic.common.persistence.AbstractJpaPersistable
import org.elaastic.user.User
import java.io.Serializable
import java.util.*
import javax.persistence.*
import javax.validation.constraints.*
import kotlin.jvm.Transient

/**
 * User entity
 */
@Entity
@ValidateHasEmailOrHasOwnerOrHasExternalSource
@PlainTextPasswordIsTooShort
class MaterialUser(
    @field:NotBlank var firstName: String,
    @field:NotBlank var lastName: String,

    /**
     * The username.
     *
     * Can only contain letters, numbers, underscores and dashes.
     * It Must be between 1 and 31 characters long.
     */
    @field:NotBlank
    @field:Column(unique = true, length = 32)
    @field:Pattern(regexp = "^[a-zA-Z0-9_-]{1,31}$")
    var username: String,

    @Transient
    var plainTextPassword: String?,

    @field:Column(unique = true)
    @field:Email
    var email: String? = null,
) : AbstractJpaPersistable<Long>(), Serializable {

    companion object {
        fun fromElaasticUser(user: User): MaterialUser = TODO("not implemented")
    }

    @field:NotNull
    @Column(columnDefinition = "BINARY(16)")
    var uuid: UUID = UUID.randomUUID()

    fun isTeacher(): Boolean = TODO("not implemented")

    override fun toString(): String {
        return username
    }

}
