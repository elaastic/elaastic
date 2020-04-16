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

package org.elaastic.questions.directory

import org.elaastic.questions.test.TestingService

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.logging.Logger
import javax.validation.Validation
import javax.validation.Validator
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import javax.transaction.Transactional
import javax.validation.ConstraintViolationException

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
internal class ActivationKeyIntegrationTest(
        @Autowired val testingService: TestingService,
        @Autowired val activationKeyRepository: ActivationKeyRepository
) {

    val logger = Logger.getLogger(ActivationKeyIntegrationTest::class.java.name)
    lateinit var validator: Validator

    @BeforeEach
    fun setUp() {
        val factory = Validation.buildDefaultValidatorFactory()
        validator = factory.validator
    }

    @Test
    fun `test validaton of a valid activation key`() {
        // given a valid activation key
        val actKey = ActivationKey(
                activationKey = "1234",
                user = testingService.getAnyUser()
        )
        actKey.dateCreated = Date()

        // expect validating the act key succeeds
        assertThat(validator.validate(actKey).isEmpty(), equalTo(true))
    }

    @Test
    fun `test validation of an invalid activation key`() {
        // given an activation key with a blank key
        val actKey = ActivationKey(
                activationKey = "",
                user = testingService.getAnyUser()
        )
        actKey.dateCreated = Date()

        // expect validating the act key fails
        assertThat(validator.validate(actKey).isEmpty(), equalTo(false))

    }

    @Test
    fun `test save of a valid activation key`() {
        // given a valid activation key
        val actKey = ActivationKey(
                activationKey = "1234",
                user = testingService.getAnyUser()
        )

        // when saving the act key
        activationKeyRepository.saveAndFlush(actKey)

        // then actKey has an id a version and a created date
        assertThat(actKey.id, notNullValue())
        assertThat(actKey.version, equalTo(0L))
        assertThat(actKey.dateCreated, notNullValue())
        assertFalse(actKey.activationEmailSent)
    }

    @Test
    fun `test save of a non valid activation key`() {
        // given a non valid activation key
        val actKey = ActivationKey(
                activationKey = "",
                user = testingService.getAnyUser()
        )

        // expect an exception is thrown when saving
        assertThrows<ConstraintViolationException> { activationKeyRepository.save(actKey) }
    }

}
