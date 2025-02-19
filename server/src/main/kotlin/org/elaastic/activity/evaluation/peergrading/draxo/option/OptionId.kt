package org.elaastic.activity.evaluation.peergrading.draxo.option

/**
 * OptionId is an enumeration of the possible values for a DraxoPeerGrading criteria.
 *
 * @property YES
 * @property NO
 * @property PARTIALLY
 * @property DONT_KNOW
 * @property NO_OPINION
 *
 * @see org.elaastic.activity.evaluation.peergrading.draxo.DraxoPeerGrading
 */
enum class OptionId(val codeI18n: String) {
    YES("common.yes"),
    NO("common.no"),
    PARTIALLY("common.partially"),
    DONT_KNOW("draxo.value.dontKnow"),
    NO_OPINION("draxo.value.noOpinion"),

}