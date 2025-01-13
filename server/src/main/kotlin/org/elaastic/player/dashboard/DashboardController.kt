package org.elaastic.player.dashboard

import org.elaastic.sequence.SequenceService
import org.elaastic.user.User
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/dashboard")
class DashboardController(
    private val sequenceService: SequenceService,
    private val dashboardModelFactory: DashboardModelFactory
) {

    @GetMapping("/{sequenceId}/attendees")
    fun getAttendees(
        authentication: Authentication,
        model: Model,
        @PathVariable sequenceId: Long
    ): String {
        val user = authentication.principal as User
        val sequence = sequenceService.get(user, sequenceId, true)

        model["dashboardModel"] = dashboardModelFactory.build(sequence)

        return "player/assignment/sequence/components/_attendees-overview.html :: attendeesOverview"
    }
}