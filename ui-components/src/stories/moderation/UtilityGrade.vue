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
  possibleGrades: Grade[]
}

export interface UtilityGradeEvents {
  (event: 'submmitUtilityGrade', gradeSelected: string): void;
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
    emit("submmitUtilityGrade", selectedGrade.value.value)
  }
};

const {t} = useI18n()
</script>

<template>
  <!-- Grade buttons -->
  <v-row>
    <v-col>
      <v-btn-toggle v-model="selectedGrade" variant="text" color="#0e6eb8" rounded="0" elevation="1">
        <v-btn v-for="(grade, index) in props.possibleGrades" :key="index" @click="setSelectedUtilityGrade(grade)"
               :value="grade" class="text-none text-subtitle-1">
          {{ grade.label }}
        </v-btn>
      </v-btn-toggle>
    </v-col>
  </v-row>
  <!-- Submit button -->
  <v-row>
    <v-col>
      <v-btn v-if="selectedGrade != null" class="text-none text-white" @click="submitUtilityGrade" color="#95c155">
        {{ t('submit') }}
      </v-btn>
    </v-col>
  </v-row>
</template>

<style scoped>

</style>

<i18n>
{
  "en": {
    "submit": "Submit"
  },
  "fr": {
    "submit": "Soumettre"
  }
}
</i18n>
