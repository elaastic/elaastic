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

package org.elaastic.common.web

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.servlet.ModelAndView
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpServletRequest
import org.springframework.web.multipart.MaxUploadSizeExceededException
import javax.persistence.EntityNotFoundException

/**
 * Controller advice to add common attributes to all views.
 */
@ControllerAdvice
class ControllerAdvice {

    @Value("\${elaastic.questions.version}")
    private lateinit var applicationVersion: String

    @Value("\${ui.components.version}")
    private lateinit var uiComponentsVersion: String

    @ModelAttribute("applicationVersion")
    fun getApplicationVersion(): String {
        return applicationVersion
    }

    @ModelAttribute("uiComponentsVersion")
    fun getUiComponentVersion(): String {
        return uiComponentsVersion
    }

    @ModelAttribute("logoutUrl")
    fun getLogoutUrl(): String {
        return "logout"
    }


    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleMaxSizeException(
        exc: MaxUploadSizeExceededException,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ModelAndView {

        val modelAndView = ModelAndView("error")

        modelAndView.model["message"] = exc.message
        return modelAndView
    }

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleEntityNotFoundException(e: EntityNotFoundException) =
        ResponseEntity(e.message ?: "Entity not found", HttpStatus.NOT_FOUND)
}
