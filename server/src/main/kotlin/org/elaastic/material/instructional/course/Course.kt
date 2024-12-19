package org.elaastic.material.instructional.course

import org.elaastic.common.persistence.AbstractJpaPersistable
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.subject.Subject
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*
import javax.persistence.*
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * A course is a collection of subjects.
 *
 * For example :
 * a course can be `Français 5e` and have subjects like
 *  - `Le champ lexical`
 *  - `Les compléments d'objets`
 *  - `Accords du participe passé`
 *  - ...
 *
 *  A course in owned by a user, and have a title.
 *
 *  From here you can access the list of subjects.
 */
@Entity
@NamedEntityGraph(
        name = "Course_subjects",
        attributeNodes = [
            NamedAttributeNode(
                    value = "subjects"
            )
        ]
)
@EntityListeners(AuditingEntityListener::class)
class Course (

        @field:NotNull
        @field:NotBlank
        var title: String,

        @field:ManyToOne(fetch = FetchType.LAZY) // TODO Lazy ?
        var owner: MaterialUser

): AbstractJpaPersistable<Long>() {

    @Version
    var version: Long? = null

    @Column(name = "date_created")
    @CreatedDate
    lateinit var dateCreated: Date

    @LastModifiedDate
    @Column(name = "last_updated")
    var lastUpdated: Date? = null

    @OneToMany(fetch = FetchType.LAZY,
            mappedBy = "course",
            targetEntity = Subject::class)
    @OrderBy("lastUpdated DESC")
    var subjects: MutableSet<Subject> = mutableSetOf()


    @field:NotNull
    @Column(name="`uuid`", columnDefinition = "BINARY(16)")
    var globalId: UUID = UUID.randomUUID()

    fun updateFrom(otherCourse: Course) {
        require(id == otherCourse.id)
        if (this.version != otherCourse.version) {
            throw OptimisticLockException()
        }

        this.title = otherCourse.title
    }
}