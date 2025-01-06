package org.elaastic.player.dashboard

import org.elaastic.test.FunctionalTestingService
import org.elaastic.test.IntegrationTestingService
import org.elaastic.test.directive.tGiven
import org.elaastic.test.directive.tThen
import org.elaastic.test.directive.tWhen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import javax.transaction.Transactional

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class DashboardModelFactoryIntegrationTest(
    @Autowired val integrationTestingService: IntegrationTestingService,
    @Autowired val functionalTestingService: FunctionalTestingService,
    @Autowired val dashboardModelFactory: DashboardModelFactory,
) {

    @Test
    fun `test of countGradableResponse with no fake response`() {
        tGiven("A sequence") {
            functionalTestingService.createSequence(integrationTestingService.getTestTeacher())
        }.tThen("there is no response gradable") { sequence ->
            assertEquals(0, dashboardModelFactory.countGradableResponse(sequence), "No response gradable")
            functionalTestingService.startSequence(sequence, nbResponseToEvaluate = 1)
            assertEquals(0, dashboardModelFactory.countGradableResponse(sequence), "No response gradable")
            sequence
        }.tWhen("we add a response gradable") { sequence ->
            functionalTestingService.createResponse(sequence, integrationTestingService.getTestStudent())
            sequence
        }.tThen("there is one response gradable") { sequence ->
            assertEquals(1, dashboardModelFactory.countGradableResponse(sequence), "One response gradable")
        }
    }

    @Test
    fun `test of countGradableResponse with fake response`() {
        tGiven("A sequence with a fake response") {
            val teacher = integrationTestingService.getTestTeacher()
            val sequence = functionalTestingService.createSequence(teacher, true)
            functionalTestingService.startSequence(sequence, nbResponseToEvaluate = 1)
        }.tThen("there is one response gradable") { sequence ->
            assertEquals(1, dashboardModelFactory.countGradableResponse(sequence), "The fake response is gradable")
        }
    }
}