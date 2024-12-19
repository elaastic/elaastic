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

import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.question.explanation.FakeExplanation
import org.elaastic.material.instructional.question.attachment.AttachmentService
import org.elaastic.user.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.logging.Logger
import javax.transaction.Transactional

@RestController
@RequestMapping("/api/assignment/{assignmentId}/sequence")
@Transactional
class RestSequenceController(
    @Autowired val sequenceService: SequenceService,
    @Autowired val attachmentService: AttachmentService
) {

    val logger:Logger = Logger.getLogger(RestSequenceController::class.java.name)

    @GetMapping("{id}/findAllFakeExplanation")
    fun findAllFakeExplanation(authentication: Authentication,
                               @PathVariable id: Long): List<FakeExplanationData> {
        val user: User = authentication.principal as User

        return sequenceService.findAllFakeExplanation(user, id).map {
            FakeExplanationData(it.correspondingItem ?: 1, it.content)
        }
    }

    @GetMapping("{id}/removeAttachment")
    fun removeAttachment(authentication: Authentication,
                         @PathVariable id: Long) {
        val user: User = authentication.principal as User
        sequenceService.get(user, id).let {
            attachmentService.detachAttachmentFromStatement(MaterialUser.fromElaasticUser(user), it.statement)
        }

    }
}

open class FakeExplanationData() {

    constructor(correspondingItem: Int?, content: String?) : this() {
        this.correspondingItem = correspondingItem
        this.content = content
    }

    constructor(fakeExplanation: FakeExplanation) :
            this(fakeExplanation.correspondingItem, fakeExplanation.content)

    var correspondingItem: Int? = null
    var content: String? = null
}
