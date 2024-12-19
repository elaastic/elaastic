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

package org.elaastic.auth.lti

import org.elaastic.assignment.Assignment
import org.elaastic.assignment.AssignmentRepository
import org.elaastic.assignment.AssignmentService
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.course.CourseService
import org.elaastic.user.RoleService
import org.elaastic.user.User
import org.elaastic.user.UserService
import org.elaastic.material.instructional.subject.Subject
import org.elaastic.material.instructional.subject.SubjectService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.UUID
import java.util.logging.Logger
import javax.persistence.EntityManager
import javax.transaction.Transactional


@Service
@Transactional
class LmsService(
    @Autowired val ltiConsumerRepository: LtiConsumerRepository,
    @Autowired val lmsUserRepository: LmsUserRepository,
    @Autowired val assignmentRepository: AssignmentRepository,
    @Autowired val lmsAssignmentRepository: LmsAssignmentRepository,
    @Autowired val userService: UserService,
    @Autowired val roleService: RoleService,
    @Autowired val courseService: CourseService,
    @Autowired val assignmentService: AssignmentService,
    @Autowired val subjectService: SubjectService,
    @Autowired val lmsUserAccountCreationService: LmsUserAccountCreationService,
    @Autowired val entityManager: EntityManager
) {

    internal val logger = Logger.getLogger(LmsService::class.java.name)

    /**
     * Find lms user based on lti launch information
     *
     * @param ltiLmsKey the lti consumer key
     * @param ltiUserId the user id coming from lti consumer
     * @return the lms user or null if not find
     */
    fun findLmsUser(ltiLmsKey: String, ltiUserId: String): LmsUser? {
        val lms = ltiConsumerRepository.findById(ltiLmsKey).get()
        return lmsUserRepository.findByLmsUserIdAndAndLms(ltiUserId, lms)
    }

    /**
     * Get lms user based on lti launch information. Create a new one if required
     *
     * @param ltiUser the lti user built from consumer launch information
     * @return the lms user
     */
    fun getLmsUser(ltiUser: LtiUser): LmsUser {
        val lms = ltiConsumerRepository.findById(ltiUser.lmsKey).get()
        var lmsUser = lmsUserRepository.findByLmsUserIdAndAndLms(ltiUser.lmsUserId, lms)
        if (lmsUser == null) {
            lmsUserAccountCreationService.createUserFromLtiData(ltiUser).let { user ->
                lmsUser = createLmsUserFromLtiDataLmsAndUser(ltiUser.lmsUserId, lms, user)
            }
        }
        return lmsUser!!
    }

    /**
     * Create a new LMS User from lti data
     *
     * @param ltiUserId the data coming from lti launch
     * @param lms the lms
     * @param user the elaastic user correponding to the lms user
     */
    fun createLmsUserFromLtiDataLmsAndUser(
        ltiUserId: String,
        lms: LtiConsumer,
        user: User
    ): LmsUser {
        LmsUser(ltiUserId, lms, user).let {
            return lmsUserRepository.save(it)
        }
    }

    /**
     * Get lms assignment based on tool provider information. Create a new one if required
     *
     * @param lmsUser the lms user
     * @param ltiActivity activity built from lti consumer information
     * @return the lms assignment
     */
    fun getLmsAssignment(
        lmsUser: LmsUser,
        ltiActivity: LtiActivity
    ): LmsAssignment {
        var lmsAssignment = lmsAssignmentRepository.findByLmsActivityIdAndLmsCourseIdAndLms(
            ltiActivity.lmsActivityId,
            ltiActivity.lmsCourseId,
            lmsUser.lms
        )
        if (lmsAssignment == null) {
            if (!lmsUser.user.isTeacher()) {
                logger.severe("Try to create an assignment with no teacher role: ${lmsUser.user.username}")
                throw IllegalArgumentException("Only teacher can create an assignment")
            }
            findOrCreateAssignmentFromLtiData(
                lmsUser = lmsUser,
                ltiActivity = ltiActivity
            ).let {
                lmsAssignment = createLmsAssignment(
                    ltiActivity,
                    lmsUser.lms,
                    it
                )
            }
        }
        return lmsAssignment!!
    }

    private fun createLmsAssignment(
        ltiActivity: LtiActivity,
        lms: LtiConsumer,
        assignment: Assignment
    ): LmsAssignment {
        LmsAssignment(
            lms = lms,
            lmsActivityId = ltiActivity.lmsActivityId,
            lmsCourseId = ltiActivity.lmsCourseId,
            lmsCourseTitle = ltiActivity.lmsCourseTitle,
            assignment = assignment
        ).let {
            return lmsAssignmentRepository.save(it)
        }
    }

    private fun findOrCreateAssignmentFromLtiData(
        lmsUser: LmsUser,
        ltiActivity: LtiActivity
    ): Assignment {
        return if (ltiActivity.globalId != null) {
            findAssignmentWithIdFromLtiData(ltiActivity.globalId)
        } else {
            Subject(ltiActivity.title, MaterialUser.fromElaasticUser(lmsUser.user)).let {
                subjectService.save(it)
            }.let { subject ->
                Assignment(ltiActivity.title, lmsUser.user, subject = subject).let {
                    assignmentService.save(it)
                }
            }
        }
    }


    private fun findAssignmentWithIdFromLtiData(ltiGlobalId: String): Assignment {
        assignmentRepository.findByGlobalId(UUID.fromString(ltiGlobalId)).let {
            if (it == null) {
                logger.severe("No assignment found for global id $ltiGlobalId")
                throw IllegalArgumentException("No assignment found for global id $ltiGlobalId")
            }
            return it
        }
    }


}
