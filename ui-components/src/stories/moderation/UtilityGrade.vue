<script setup lang="ts">

import { computed, ref } from 'vue'

export interface UtilityGradeProps {
  possibleGrades: string[]
}

const props = withDefaults(defineProps<UtilityGradeProps>(), {
  possibleGrades: () => ['A', 'B', 'C', 'D', 'F']
})

const modelValue = ref({
  /**
   * @property {null | string} selectedGrade - Represents the currently selected grade.
   * Initially set to null, it can be updated to a string value representing a specific grade.
   */
  selectedGrade: null as null | string,
})

const selectedGrade = computed({
  get: () => modelValue.value.selectedGrade,
  set: newValue => {
    modelValue.value.selectedGrade = newValue
  }
});

function handleClick(itemClicked: string) {
  selectedGrade.value = itemClicked
  console.log(itemClicked);
}

</script>

<template>

  <v-col>
    <v-row>
      <div v-for="(grade, index) in props.possibleGrades" :key="index">
        <v-btn @click="handleClick(grade)" color="primary" class="mr-2">{{ grade }}</v-btn>
      </div>
    </v-row>
    <br>
    <p>{{ selectedGrade }}</p>
  </v-col>


</template>

<style scoped></style>
