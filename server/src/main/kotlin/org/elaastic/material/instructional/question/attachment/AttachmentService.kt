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

package org.elaastic.material.instructional.question.attachment

import org.elaastic.material.instructional.statement.Statement
import org.elaastic.filestore.DataIdentifier
import org.elaastic.filestore.FileDataStore
import org.elaastic.material.instructional.MaterialUser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import java.io.InputStream
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.transaction.Transactional

@Service
@Transactional
class AttachmentService(
    @Autowired val attachmentRepository: AttachmentRepository,
    @Autowired val dataStore: FileDataStore
) {

    val logger: Logger = Logger.getLogger(AttachmentService::class.java.name)

    /**
     * Get attachment by id
     */
    fun getAttachmentById(id: Long): Attachment {
        return attachmentRepository.findById(id).get()
    }

    fun getAttachmentByUuid(uuid: UUID): Attachment {
        return attachmentRepository.findByUuid(uuid).get()
    }

    /**
     * Add a statement to an attachment
     *
     * @param statement the statement
     * @param attachment the attachment
     * @return the modified attachment
     */
    fun addStatementToAttachment(statement: Statement, attachment: Attachment): Attachment {
        attachment.statement = statement
        statement.attachment = attachment
        attachment.toDelete = false
        return attachmentRepository.save(attachment)
    }

    /**
     * Save a statement attachment
     *
     * @param statement the concerning statement
     * @param attachment the attachment
     * @param inputStream the input stream corresponding to attachment file
     */
    fun saveStatementAttachment(statement: Statement, attachment: Attachment, inputStream: InputStream): Attachment {
        inputStream.use { iis ->
            try {
                val dataRecord = dataStore.addRecord(iis)
                attachment.path = dataRecord.identifier.toString()
                if (attachment.isDisplayableImage()) {
                    dataRecord.stream.use {
                        attachment.dimension = getDimensionFromInputStream(it)
                    }
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Message",e)
                e.cause?.let {
                    logger.log(Level.SEVERE, "Message",it)
                }
                throw Exception("A problem occurs when trying to save the attachment",e)
            }
        }
        return addStatementToAttachment(statement, attachment)
    }

    /**
     * Duplicate an attachement
     * @param original the attachment to duplicate
     * @return the duplicated attachment
     */
    fun duplicateAttachment(original: Attachment): Attachment {
        val attachment = Attachment(
                size = original.size,
                mimeType = original.mimeType,
                name = original.name,
                originalFileName = original.originalFileName,
                toDelete = original.toDelete
        )
        attachment.path = original.path
        original.dimension?.let {
            attachment.dimension = Dimension(
                    width = it.width,
                    height = it.height
            )
        }
        return attachment
    }


    /**
     * Detach an attachment
     *
     * @param statement the statement
     * @return the statement
     */
    fun detachAttachmentFromStatement(user: MaterialUser, statement: Statement): Statement {
        if (statement.owner != user) throw AccessDeniedException("You are not autorized to access to this sequence")
        statement.attachment?.let {
            statement.attachment = null
            it.statement = null
            it.toDelete = true
            attachmentRepository.saveAndFlush(it)
        }
        return statement
    }

    /**
     * Return input stream corresponding to the given Attachement
     *
     * @param attachment the attachement
     * @return the input stream
     */
    fun getInputStreamForAttachment(attachment: Attachment): InputStream =
        getInputStreamForPath(attachment.path!!)

    fun getInputStreamForPath(path: String): InputStream =
        dataStore.getRecord(DataIdentifier(path))!!.stream

    /**
     * Check if there are attachment to delete and delete them in this case.
     */
    fun deleteAttachmentAndFileInSystem() {
        val attachmentToRemoveList = attachmentRepository.findAllByToDelete(true)
        deleteAttachmentAndFileInSystem(attachmentToRemoveList)
    }


    internal fun deleteAttachmentAndFileInSystem(attachmentList: MutableList<Attachment>) {
        if (attachmentList.isEmpty()) return
        val attachmentToDelete = attachmentList[0]
        val newAttachmentList = if (attachmentList.size > 1) attachmentList.subList(1, attachmentList.size) else mutableListOf()
        processAttachmentToDelete(attachmentToDelete, newAttachmentList)
        deleteAttachmentAndFileInSystem(newAttachmentList)
    }

    private fun processAttachmentToDelete(attachmentToDelete: Attachment, newAttachmentList: MutableList<Attachment>) {
        var deleteInSystem = true
        attachmentRepository.findAllByPathAndIdNot(attachmentToDelete.path!!, attachmentToDelete.id!!).forEach {
            if (!it.toDelete) {
                deleteInSystem = false
            } else {
                newAttachmentList.remove(it)
                attachmentRepository.delete(it)
            }
        }
        if (deleteInSystem) {
            val attachmentPath = attachmentToDelete.path!!
            dataStore.getFile(DataIdentifier(attachmentPath)).delete()
        }
        attachmentRepository.delete(attachmentToDelete)
    }

    internal fun getDimensionFromInputStream(inputStream: InputStream): Dimension? {
        ImageIO.createImageInputStream(inputStream).let { iis ->
            ImageIO.getImageReaders(iis).let {
                while (it.hasNext()) {
                    val reader = it.next()
                    try {
                        reader.input = iis
                        return Dimension(
                                width = reader.getWidth(reader.minIndex),
                                height = reader.getHeight(reader.minIndex)
                        )
                    } catch (e: Exception) {
                        logger.severe(e.toString())
                    } finally {
                        reader.dispose()
                    }
                }
                return null
            }
        }
    }
}
