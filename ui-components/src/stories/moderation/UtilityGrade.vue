<script setup lang="ts">

import {computed, ref} from 'vue'
import {useI18n} from "vue-i18n";

type Grade = {
  label: string,
  value: string
}

export interface UtilityGradeProps {
  /**
   * The possible grades to select from
   *
   * A Grade is an object with a label and a value.
   * The label is displayed and the value is emitted when the grade is submitted
   */
  possibleGrades: Grade[],
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
  selectedGradeModel: null as Grade | null,
})

const selectedGrade = computed({
  get: () => modelValue.value.selectedGradeModel,
  set: newValue => {
    modelValue.value.selectedGradeModel = newValue
  }
});

function setSelectedUtilityGrade(itemClicked: Grade) {
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

      <div id="horizontal-grade-selector">
        <v-btn-toggle v-model="selectedGrade" variant="text" color="#0e6eb8" rounded="0" elevation="1"
                      style="margin-top: 10px;">
          <v-btn v-for="(grade, index) in props.possibleGrades" :key="index" @click="setSelectedUtilityGrade(grade)"
                 :value="grade" class="text-none text-subtitle-1">
            {{ grade.label }}
          </v-btn>
        </v-btn-toggle>
      </div>
      <div id="vertical-grade-selector-container">
        <v-btn-toggle v-model="selectedGrade" variant="text" color="#0e6eb8" rounded="0" elevation="1"
                      style="margin-top: 10px;" id="vertical-grade-selector">
          <v-btn v-for="(grade, index) in props.possibleGrades" :key="index" @click="setSelectedUtilityGrade(grade)"
                 :value="grade" class="text-none text-subtitle-1 btn-vertical-selector">
            {{ grade.label }}
          </v-btn>
        </v-btn-toggle>
      </div>
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
  #horizontal-grade-selector {
    display: none;
  }
  #submit-btn {
    width: 100%;
  }
}

@media not (max-width: 600px) {
  #vertical-grade-selector-container {
    display: none;
  }
}

#vertical-grade-selector  {
  flex-direction: column;
  height: unset;
  width: 100%;

  .btn-vertical-selector {
    padding: 3%;
  }
}

</style>

<i18n>
{
  "en": {
    "submit": "Submit",
    "peer-review-label": "I consider the above evaluation useful for my learning:",
    "chatGPT-review-student-label": "I consider the ChatGPT evaluation useful for my learning:",
    "chatGPT-review-teacher-label": "I consider the ChatGPT evaluation useful for the student's learning:"
  },
  "fr": {
    "submit": "Soumettre",
    "peer-review-label": "Je considère que l'évaluation ci-dessus est utile pour mon apprentissage :",
    "chatGPT-review-student-label": "Je considère que l'évaluation de ChatGPT est utile pour mon apprentissage :",
    "chatGPT-review-teacher-label": "Je considère que l'évaluation de ChatGPT est utile pour l'apprentissage de l'étudiant :"
  }
}
</i18n>
