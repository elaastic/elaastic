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

import org.apache.commons.lang3.time.DateUtils
import org.elaastic.questions.onboarding.OnboardingChapter
import org.elaastic.questions.terms.TermsService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import java.util.logging.Logger
import javax.annotation.PostConstruct
import javax.persistence.EntityManager
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer
import java.time.LocalDate
import java.util.regex.Pattern
import kotlin.jvm.Throws


@Service
class UserService(
    @Autowired val userRepository: UserRepository,
    @Autowired val passwordEncoder: PasswordEncoder,
    @Autowired val settingsRepository: SettingsRepository,
    @Autowired val unsubscribeKeyRepository: UnsubscribeKeyRepository,
    @Autowired val activationKeyRepository: ActivationKeyRepository,
    @Autowired val passwordResetKeyRepository: PasswordResetKeyRepository,
    @Autowired val termsService: TermsService,
    @Autowired val userConsentRepository: UserConsentRepository,
    @Autowired val onboardingStateRepository: OnboardingStateRepository,
    @Autowired val entityManager: EntityManager
) {

    val FAKE_USER_PREFIX = "John_Doe___"
    val logger = Logger.getLogger(UserService::class.java.name)
    var fakeUserList: List<User>? = null

    /**
     * Get user by id checking access authorization
     *
     * @param authUser the user triggering the get
     * @param id the id
     * @return the found user or null
     * @throws AccessDeniedException when authorization failed
     */
    fun get(authUser: User, id: Long): User {
        get(id).let {
            if (it != authUser) {
                logger.severe("Trying illegal access on user ${it?.id} from ${authUser.id}")
                throw AccessDeniedException("You are not autorized to access to this user")
            }
            return it
        }
    }

    /**
     * Get user by id
     *
     * @param id the id
     * @return the found user or null
     */
    fun get(id: Long): User? {
        userRepository.findById(id).let {
            return if (it.isPresent) {
                it.get()
            } else {
                null
            }
        }
    }


    /**
     * Find user by username
     *
     * @param username the username provided as input
     * @return the found user or null otherwise
     */
    fun findByUsername(username: String): User? {
        return userRepository.findByUsername(username)
    }

    /**
     * Get the default admin user
     *
     * @return the default admin user
     */
    fun getDefaultAdminUser(): User {
        val user = findByUsername("admin")!!
        assert(user.isAdmin())
        return user
    }

    /**
     * Find users by email
     *
     * @param email the email provided as input
     * @return the found users
     */
    fun findAllByEmail(email: String): List<User> {
        return userRepository.findAllByEmail(email)
    }

    /**
     * Save and initialize settings for a user newly created
     *
     * @param user the user to process
     * @param language the preferred language of the user
     * @param checkEmailAccount flag to indicates if mail checking must be
     *     perform by the system
     * @return the saved user
     */
    @Transactional
    fun addUser(
        user: User,
        language: String = "fr",
        checkEmailAccount: Boolean = false,
        enable: Boolean = true,
        addUserConsent: Boolean = true
    ): User {

        require(user.roles.isNotEmpty())

        with(user) {
            enabled = !checkEmailAccount && enable
            password = passwordEncoder.encode(plainTextPassword)
            userRepository.save(this).let {
                this.onboardingState = onboardingStateRepository.save(OnboardingState(user))
                initializeSettingsForUser(it, language)
                initializeUnsubscribeKeyForUser(it)
                if (checkEmailAccount) {
                    initializeActivationKeyForUser(it)
                }
                if (addUserConsent) {
                    addUserConsentToActiveTerms(user.username)
                }
                return it
            }
        }
    }

    @Transactional
    fun saveUser(authUser: User, user: User): User {
        if (authUser != user) {
            logger.severe("Trying illegal access on user ${user.id} from ${authUser.id}")
            throw AccessDeniedException("You are not authorized to access to this user")
        }
        if (user.plainTextPassword != null) {
            user.password = passwordEncoder.encode(user.plainTextPassword)
        }
        userRepository.saveAndFlush(user).let {
            return it
        }
    }

    /**
     * Change the password of a user
     *
     * @param user the processed user
     * @param newPlainTextPassword the new plain text password
     * @return the user with its new password
     */
    fun changePasswordForUser(user: User, newPlainTextPassword: String): User {
        user.plainTextPassword = newPlainTextPassword // required to get validation
        user.password = passwordEncoder.encode(newPlainTextPassword)
        userRepository.saveAndFlush(user).let {
            return it
        }
    }

    /**
     * Change the password of a user
     *
     * @param user the processed user
     * @param currentPassword the current password used to check current
     *     password is known by the user
     * @param newPlainTextPassword the new plain text password
     * @return the user with its new password
     * @throws AccessDeniedException if current password not valid
     */
    fun changePasswordForUserWithCurrentPasswordChecking(
        user: User,
        currentPassword: String,
        newPlainTextPassword: String
    ): User {
        passwordEncoder.encode(currentPassword).let {
            if (!passwordEncoder.matches(currentPassword, user.password)) {
                logger.severe("Trying illegal access on user ${user.id} with bad password")
                throw SecurityException("Bad.user.password")
            }
        }
        return changePasswordForUser(user, newPlainTextPassword)
    }

    /**
     * Initialize settings for a new user
     *
     * @param user the user
     * @return the settings object
     */
    fun initializeSettingsForUser(user: User, language: String): Settings {
        Settings(user = user, language = language).let {
            user.settings = it
            settingsRepository.save(it).let {
                return it
            }
        }
    }

    /**
     * Initialize unsubscribe key for a new user
     *
     * @param user the user
     * @return the unsubscribe key object
     */
    fun initializeUnsubscribeKeyForUser(user: User): UnsubscribeKey {
        UnsubscribeKey(
            user = user,
            unsubscribeKey = UUID.randomUUID().toString()
        ).let {
            unsubscribeKeyRepository.save(it)
        }.let {
            user.unsubscribeKey = it
            return it
        }
    }

    /**
     * Initialize activation key for a new user
     *
     * @param user the user
     * @return the activation key object
     */
    fun initializeActivationKeyForUser(user: User): ActivationKey {
        ActivationKey(
            user = user,
            activationKey = UUID.randomUUID().toString()
        ).let {
            activationKeyRepository.save(it)
        }.let {
            user.activationKey = it
            return it
        }
    }

    /**
     * Enable user with activation key
     *
     * @param activationKey the string value of the activation key
     * @return the enabled user or null if no activation key is found
     */
    fun enableUserWithActivationKey(activationKey: String): User? {
        activationKeyRepository.findByActivationKey(activationKey).let {
            return when (it) {
                null -> null
                else -> {
                    val user = it.user
                    user.enabled = true
                    userRepository.save(user)
                    activationKeyRepository.delete(it)
                    it.user
                }
            }
        }
    }

    /**
     * Find user by given activiation key
     *
     * @param activationKey the activiation key value
     * @return the user or null if not found
     */
    fun findUserByActivationKey(activationKey: String): User? {
        return activationKeyRepository.findByActivationKey(activationKey)?.user
    }

    /**
     * Disable user
     *
     * @param user the user to disable
     * @return the disabled user
     */
    fun disableUser(user: User): User {
        user.enabled = false
        userRepository.saveAndFlush(user)
        return user
    }

    /**
     * Initialize first activity of user user
     *
     * @param user the user whose first activity is to initialize
     * @return the updated user
     */
    fun updateUserActiveSince(user: User): User {
        if (user.activeSince == null) {
            user.activeSince = LocalDate.now()
            userRepository.saveAndFlush(user)
        }
        return user
    }

    /**
     * Generate password reset key for a user
     *
     * @param user the processed user
     * @param lifetime lifetime of a password key in hour, default set to 1
     * @return the password reset key object user
     */
    fun generatePasswordResetKeyForUser(user: User, lifetime: Int = 1): PasswordResetKey {
        val passwordResetKey = passwordResetKeyRepository.findByUser(user)
        when (passwordResetKey) {
            null -> {
                PasswordResetKey(
                    passwordResetKey = UUID.randomUUID().toString(),
                    user = user
                )
            }

            else -> {
                if (passwordResetKey.dateCreated < DateUtils.addHours(Date(), -lifetime)) {
                    passwordResetKey.passwordResetKey = UUID.randomUUID().toString()
                    passwordResetKey.dateCreated = Date()
                }
                passwordResetKey.passwordResetEmailSent = false
                passwordResetKey
            }
        }.let {
            passwordResetKeyRepository.saveAndFlush(it)
        }.let {
            return it
        }
    }


    /**
     * Find user by password reset key
     *
     * @param passwordResetKeyValue the string value of the password reset key
     * @return the found user or null otherwise
     */
    fun findByPasswordResetKeyValue(passwordResetKeyValue: String): User? {
        userRepository.findByPasswordResetKeyValue(passwordResetKeyValue).let {
            return it
        }
    }

    /**
     * Remove old activation keys and corresponding users who didn't activate
     * their accounts
     *
     * @param lifetime the lifetime in hours of activation keys, default set to
     *     3
     */
    fun removeOldActivationKeys(lifetime: Int = 3) {
        activationKeyRepository.findAllByDateCreatedLessThan(DateUtils.addHours(Date(), -lifetime)).let {
            activationKeyRepository.deleteAll(it)
            logger.info("${it.size} activation keys deleted")
            it
        }.map {
            it.user
        }.filter { user ->
            !user.enabled
        }.let {
            logger.info("${it.size} user(s) to delete")
            it
        }.forEach { user ->
            settingsRepository.findByUser(user).let { settings ->
                settingsRepository.delete(settings)
            }
            unsubscribeKeyRepository.findByUser(user).let { unsubscribeKey ->
                unsubscribeKeyRepository.delete(unsubscribeKey)
            }
            onboardingStateRepository.deleteAllByUser(user)
            userRepository.delete(user)
        }
    }

    /** Remove password reset keys older than lifetime hours, default to 1 */
    fun removeOldPasswordResetKeys(lifetime: Int = 1) {
        passwordResetKeyRepository.findAllByDateCreatedLessThan(DateUtils.addHours(Date(), -lifetime)).let {
            passwordResetKeyRepository.deleteAll(it)
            logger.info("${it.size} password reset keys deleted")
        }
    }

    /** Return true if user gave consent to active terms */
    fun userHasGivenConsentToActiveTerms(username: String): Boolean {
        return userConsentRepository.existsByUsernameAndTerms(
            username,
            termsService.getActive()
        )
    }

    /**
     * Store user consent to active terms if not already stored
     *
     * @param username the username of the user
     * @return the username
     */
    fun addUserConsentToActiveTerms(username: String): String {
        if (!userHasGivenConsentToActiveTerms(username)) {
            UserConsent(username, termsService.getActive()).let {
                userConsentRepository.save(it)
            }
        }
        return username
    }

    /**
     * Generate a password (not encoded)
     *
     * @return the password
     */
    fun generatePassword(): String {
        var password = ""
        val alphabet = "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789"
        val rand = Random()
        for (i in 0..7) {
            password += alphabet[rand.nextInt(alphabet.length)]
        }
        return password
    }

    @PostConstruct
    fun initializeFakeUserList() {
        fakeUserList = buildFakeUserList()
    }

    private fun buildFakeUserList(): List<User> {
        return mutableListOf<User>().also { fakeUserList ->
            for (i in 1..9) {
                fakeUserList.add(findByUsername("$FAKE_USER_PREFIX${i}")!!)
            }
        }
    }

    @Transactional
    fun updateOnboardingChapter(chapterToUpdate: OnboardingChapter, user: User?) {
        val onboardingState = user?.onboardingState
        if (onboardingState != null) {
            onboardingState.chaptersSeen.add(chapterToUpdate)
            onboardingStateRepository.save(onboardingState)
        }

    }

    fun getOnboardingState(userId: Long?): OnboardingState {
        return onboardingStateRepository.findFirstByUserId(userId)
    }


    /**
     * Generate username from firstname and lastname
     *
     * @param firstName the firstname
     * @param lastName the lastname
     * @return the username
     */
    fun generateUsername(firstName: String, lastName: String): String {
        val shortenedFirstname = firstName.replace("[\\s']".toRegex(), "")
        val shortenedLastname = lastName.replace("[\\s']".toRegex(), "")
        val indexLastname = MAX_INDEX_LASTNAME.coerceAtMost(shortenedLastname.length)
        val indexFirstName = MAX_INDEX_FIRSTNAME.coerceAtMost(shortenedFirstname.length)
        var username = replaceAccent(shortenedFirstname.lowercase().substring(0, indexFirstName)) +
                replaceAccent(shortenedLastname.lowercase().substring(0, indexLastname))
        val existingUsername = findMostRecentUsernameStartingWithUsername(username)
        if (existingUsername != null) {
            "[0-9]+".toRegex().findAll(existingUsername).let {
                username += if (it.count() == 0) {
                    2
                } else {
                    java.lang.Integer.parseInt(it.iterator().next().value) + 1
                }
            }
        }
        return username
    }

    private val MAX_INDEX_LASTNAME = 4
    private val MAX_INDEX_FIRSTNAME = 3

    /**
     * Replace accents in a string
     *
     * @param str the string to modify
     * @return
     */
    fun replaceAccent(str: String): String {
        val nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(nfdNormalizedString).replaceAll("");
    }

    /**
     * Get the most recent user who begin with username param
     *
     * @param username the username
     * @return a username if found else null
     */
    fun findMostRecentUsernameStartingWithUsername(username: String): String? {
        val userNameLike = "^$username[0-9]*$"
        val queryString = "SELECT username FROM user WHERE username RLIKE '$userNameLike' ORDER BY username DESC"
        logger.finest("Generated query string: $queryString")
        val query = entityManager.createNativeQuery(queryString)
        val result = query.resultList
        return when (result.isEmpty()) {
            true -> null
            false -> result.first().toString()
        }
    }

    /**
     * return the learner with the given id
     * @param learnerId id to find the learner
     * @return learner with the given id
     * @throws IllegalArgumentException if a learner with the given id doesn't exist
     */
    @Throws(IllegalArgumentException::class)
    fun findById(learnerId: Long): User {
        return userRepository.findById(learnerId).orElseThrow { IllegalArgumentException("Learner not found") }
    }

}

