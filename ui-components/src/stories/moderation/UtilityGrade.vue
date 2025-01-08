<script setup lang="ts">

import {computed, ref} from 'vue'
import {useI18n} from "vue-i18n";
import SelectorResponsiv, {type Selection} from "@/components/util/SelectorResponsiv.vue";

export interface UtilityGradeProps {
  /**
   * The possible grades to select from
   *
   * A Grade is an object with a label and a value.
   * The label is displayed and the value is emitted when the grade is submitted
   */
  possibleGrades: Selection[],
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

const {t} = useI18n()
</script>

<template>
  <!-- Grade buttons -->
  <v-row>
    <v-col>
      <div v-if="!props.isChatGPT && !props.isTeacher" readonly>{{ t('peer-review-label') }}</div>
      <div v-if=" props.isChatGPT && !props.isTeacher" readonly>{{ t('chatGPT-review-student-label') }}</div>
      <div v-if=" props.isChatGPT &&  props.isTeacher" readonly>{{ t('chatGPT-review-teacher-label') }}</div>

      <SelectorResponsiv :selections="props.possibleGrades" @changeSelection="setSelectedUtilityGrade"/>
    </v-col>
  </v-row>
  <!-- Submit button -->
  <v-row>
    <v-col>
      <v-btn id="submit-btn" v-if="selectedGrade != null" class="text-none text-white" @click="submitUtilityGrade" color="#95c155">
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
    "chatGPT-review-teacher-label": "I consider the ChatGPT evaluation useful for the student's learning:"
  },
  "fr": {
    "peer-review-label": "Je considère que l'évaluation ci-dessus est utile pour mon apprentissage :",
    "chatGPT-review-student-label": "Je considère que l'évaluation de ChatGPT est utile pour mon apprentissage :",
    "chatGPT-review-teacher-label": "Je considère que l'évaluation de ChatGPT est utile pour l'apprentissage de l'étudiant :"
  }
}
</i18n>
