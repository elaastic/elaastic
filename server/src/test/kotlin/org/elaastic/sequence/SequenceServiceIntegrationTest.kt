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

import org.elaastic.test.IntegrationTestingService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import javax.transaction.Transactional
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.*
import org.junit.jupiter.api.assertThrows
import org.springframework.security.access.AccessDeniedException
import javax.persistence.EntityNotFoundException


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
internal class SequenceServiceIntegrationTest(
    @Autowired val sequenceService: SequenceService,
    @Autowired val integrationTestingService: IntegrationTestingService
) {

    @Test
    fun `get a sequence - valid`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = assignment.owner

        assertThat(
            "The testing data are corrupted",
            assignment.sequences.size,
            equalTo(2)
        )

        val testingSequence = assignment.sequences.first()
        assertThat(
            sequenceService.get(user, testingSequence.id!!),
            equalTo(testingSequence)
        )
    }

    @Test
    fun `get a sequence - error unauthorized`() {
        val assignment = integrationTestingService.getTestAssignment()
        val user = integrationTestingService.getTestTeacher()

        assertThat(
            "The testing data are corrupted",
            assignment.sequences.size,
            equalTo(2)
        )

        val testingSequence = assignment.sequences.first()
        assertThrows<AccessDeniedException> {
            sequenceService.get(user, testingSequence.id!!)
        }
    }

    @Test
    fun `get a sequence - error invalid id`() {
        val user = integrationTestingService.getTestTeacher()

        assertThrows<EntityNotFoundException> {
            sequenceService.get(user, 12345678)
        }
    }
}
