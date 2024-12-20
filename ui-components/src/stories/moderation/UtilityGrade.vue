<script setup lang="ts">

import {computed, ref} from 'vue'
import {useI18n} from "vue-i18n";
import utilityGradeStories from "@/stories/moderation/UtilityGrade.stories";

type Grade = {
  label: string,
  value: string
}

export interface UtilityGradeProps {
  possibleGrades: Grade[]
}

/** Fired when the selected value changes */
export interface UtilityGradeEvents {
  (event: 'update:submmitUtilityGrade', value: Grade): void;
}
const props = defineProps<UtilityGradeProps>()
const emit = defineEmits<UtilityGradeEvents>()

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

function submitUtilityGrade() {
  if (selectedGrade.value != null) {
    emit('update:submmitUtilityGrade', selectedGrade.value);
  }
}

const {t} = useI18n()
</script>

<template>
  <v-row>
    <v-col>
      <v-btn-toggle v-model="selectedGrade" v-for="(grade, index) in props.possibleGrades" :key="index" variant="tonal"
                    color="light-blue-darken-1" rounded="0">
        <v-btn @click="setSelectedUtilityGrade(grade)" :value="grade" class="text-none text-subtitle-1">
          {{ grade.label }}
        </v-btn>
      </v-btn-toggle>
    </v-col>
  </v-row>
  <v-row>
    <v-col>
      <v-btn v-if="selectedGrade != null" class="text-none text-subtitle-1" @click="submitUtilityGrade()">
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
