package org.elaastic.material.instructional.subject

import com.fasterxml.jackson.databind.ObjectMapper
import org.elaastic.common.util.ZipService
import org.elaastic.material.instructional.MaterialUser
import org.elaastic.material.instructional.question.attachment.AttachmentService
import org.elaastic.material.instructional.statement.StatementService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.stereotype.Service
import java.io.*
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipFile

@Service
class SubjectExporter(
    // Inject that bean to get the mapper used by Spring MVC (that is properly configured)
    @Qualifier("mappingJackson2HttpMessageConverter") @Autowired springMvcJacksonConverter: MappingJackson2HttpMessageConverter,
    @Autowired val zipService: ZipService,
    @Autowired val attachmentService: AttachmentService,
    @Autowired val subjectService: SubjectService,
    @Autowired val statementService: StatementService,
) {

    val mapper: ObjectMapper = springMvcJacksonConverter.objectMapper

    companion object {
        const val RESOURCE_FOLDER = "resources"
    }


    /**
     * Export the subject (including statements, attachments & fake explanations) to a POJO
     */
    fun exportToPojo(subject: Subject) =
        ExportSubjectData(subject, statementService)

    /**
     * Export the subject to JSON (which does not include attachments)
     */
    fun exportToJson(subject: Subject): String = exportToJson(exportToPojo(subject))

    /**
     * Export the subject to JSON (which does not include attachments)
     */
    fun exportToJson(exportSubjectData: ExportSubjectData): String =
        mapper.writeValueAsString(exportSubjectData)

    /**
     * Export the subject to a ZIP archive include a JSON file for the subject data and 1 file for each
     * attachment
     */
    fun exportToZip(subject: Subject, filename: String, outputStream: OutputStream) {
        val exportSubjectData = exportToPojo(subject)

        zipService.zip(
            outputStream,
            listOf(
                ZipService.ZipEntryData(
                    "${filename}.elaastic.json",
                    exportToJson(exportSubjectData).byteInputStream()
                )

            ) +
                    exportSubjectData.getAttachmentList().map { exportAttachment ->
                        ZipService.ZipEntryData(
                            "${RESOURCE_FOLDER}/${exportAttachment.path}",
                            attachmentService.getInputStreamForPath(exportAttachment.path)
                        )
                    }
        )
    }

    /**
     * Extract Subject data from JSON
     */
    fun parseFromJson(jsonReader: Reader): ExportSubjectData =
        mapper.readValue(jsonReader, ExportSubjectData::class.java)

    /**
     * Import a subject from JSON
     */
    fun importFromJson(user: MaterialUser, jsonReader: Reader): Subject =
        subjectService.createFromExportData(
            user,
            parseFromJson(jsonReader)
        )

    /**
     * Import a subject and its attachments from a ZIP archive
     */
    fun importFromZip(user: MaterialUser, inputStream: InputStream): Subject {
        val zip = File.createTempFile("elaastic-" + UUID.randomUUID().toString(), null)

        inputStream.use { input ->
            zip.outputStream().use { output ->
                input.copyTo(output, 16 * 1024)
            }
        }

        val extractedFiles = zipService.unzip(ZipFile(zip))

        // Extract the subject data
        val exportSubjectData = parseFromJson(
            InputStreamReader(extractedFiles.first().file.inputStream(), Charsets.UTF_8)
        )
        // Inject assignments
        exportSubjectData.getAttachmentList().forEach { exportAttachment ->
            exportAttachment.attachmentFile = extractedFiles.find { it.name == "${RESOURCE_FOLDER}/"+exportAttachment.path }?.file
        }

        // Import the subject
        val subject = subjectService.createFromExportData(
            user,
            exportSubjectData
        )

        // clean up all tmp files
        (zip.toPath() + extractedFiles.map { it.file.toPath() })
            .forEach { Files.deleteIfExists(it) }

        return subject
    }


}