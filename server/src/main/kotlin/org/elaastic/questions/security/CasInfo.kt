package org.elaastic.questions.security

data class CasInfo(
    val casKey: String,
    val label: String,
    val logoSrc: String,
    val serverUrl: String,
    val casProvider: String,
)