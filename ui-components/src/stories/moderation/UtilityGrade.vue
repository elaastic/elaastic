<script setup lang="ts">

import {computed, ref} from 'vue'
import {useI18n} from "vue-i18n";
import SelectorResponsiv, {type Selection} from "@/components/util/SelectorResponsiv.vue";

const {t} = useI18n()

const gradeValue: string[] = [
  "STRONGLY_DISAGREE",
  "DISAGREE",
  "AGREE",
  "STRONGLY_AGREE"
]

const getSelection = (gradeValue: string): Selection => {
  return {label: t(`utility-grade.${gradeValue}`), value: gradeValue}
}

const possibleGrade: Selection[] = gradeValue.map(getSelection)

export interface UtilityGradeProps {
  /**
   * Wether the evaluation has been done by ChatGPT or not
   */
  isChatGPT: boolean
  /**
   * Whether the user is a teacher or not
   */
  isTeacher: boolean,
}

export interface UtilityGradeEvents {
  (event: 'submitUtilityGrade', gradeSelected: string): void;
}

const props = defineProps<UtilityGradeProps>();
const emit = defineEmits<UtilityGradeEvents>();

const modelValue = ref({
  selectedGradeModel: null as Selection | null,
})

const selectedGrade = computed({
  get: () => modelValue.value.selectedGradeModel,
  set: newValue => {
    modelValue.value.selectedGradeModel = newValue
  }
});

function setSelectedUtilityGrade(itemClicked: Selection) {
  selectedGrade.value = itemClicked;
}

const submitUtilityGrade = () => {
  if (selectedGrade.value != null) {
    emit("submitUtilityGrade", selectedGrade.value.value)
  }
};
</script>

<template>
  <!-- Grade buttons -->
  <v-row>
    <v-col>
      <div v-if="!props.isChatGPT && !props.isTeacher" readonly>{{ t('peer-review-label') }}</div>
      <div v-if=" props.isChatGPT && !props.isTeacher" readonly>{{ t('chatGPT-review-student-label') }}</div>
      <div v-if=" props.isChatGPT &&  props.isTeacher" readonly>{{ t('chatGPT-review-teacher-label') }}</div>

      <SelectorResponsiv :selections="possibleGrade" @changeSelection="setSelectedUtilityGrade"/>
    </v-col>
  </v-row>
  <!-- Submit button -->
  <v-row>
    <v-col>
      <v-btn id="submit-btn" v-if="selectedGrade != null" class="text-none text-white" @click="submitUtilityGrade"
             color="#95c155">
        {{ t('submit') }}
      </v-btn>
    </v-col>
  </v-row>
</template>

<style scoped>
@media (max-width: 600px) {
  #submit-btn {
    width: 100%;
  }
}
</style>

<i18n>
{
  "en": {
    "peer-review-label": "I consider the above evaluation useful for my learning:",
    "chatGPT-review-student-label": "I consider the ChatGPT evaluation useful for my learning:",
    "chatGPT-review-teacher-label": "I consider the ChatGPT evaluation useful for the student's learning:",
    "utility-grade": {
      "STRONGLY_DISAGREE": "Strongly disagree",
      "DISAGREE": "Disagree",
      "AGREE": "Agree",
      "STRONGLY_AGREE": "Strongly Agree"
    }
  },
  "fr": {
    "peer-review-label": "Je considère que l'évaluation ci-dessus est utile pour mon apprentissage :",
    "chatGPT-review-student-label": "Je considère que l'évaluation de ChatGPT est utile pour mon apprentissage :",
    "chatGPT-review-teacher-label": "Je considère que l'évaluation de ChatGPT est utile pour l'apprentissage de l'étudiant :",
    "utility-grade": {
      "STRONGLY_DISAGREE": "Pas du tout d'accord",
      "DISAGREE": "Pas d'accord",
      "AGREE": "D'accord",
      "STRONGLY_AGREE": "Tout à fait d'accord"
    }
  }
}
</i18n>
