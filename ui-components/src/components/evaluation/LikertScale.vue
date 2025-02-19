<script setup lang="ts">
import { computed } from 'vue'
import { useDisplay } from 'vuetify'
import type { LikertValue } from '@/components/evaluation/Likert'
import { useI18n } from 'vue-i18n'

export interface LikertScaleProps {
  /**
   * The selected value on the Likert scale
   */
  modelValue: LikertValue,
  /**
   * The number of value of the scale
   * @default 5
   */
  nbValues?: number,
  /**
   * Label for the lowest value of the scale
   */
  minLabel: string,
  /**
   * Label for the highest value of the scale
   */
  maxLabel: string,
  /**
   * Component color
   * @default 'default'
   */
  color?: string;
}

export interface LikertScaleEvents {
  /** Fired when the selected value changes */
  (event: 'update:modelValue', value: LikertValue): void;
}

const props = withDefaults(defineProps<LikertScaleProps>(), {
  modelValue: null,
  nbValues: 5,
  color: 'default'
})

const emit = defineEmits<LikertScaleEvents>()

const value = computed({
  get: () => props.modelValue,
  set: newValue => emit('update:modelValue', newValue)
})
const { xs } = useDisplay()
const { t } = useI18n()
</script>

<template>
  <v-radio-group class="d-inline-flex" v-model="value">
    <v-row dense>
      <v-col v-if="!xs" class="align-self-center pb-4 text-center">{{ minLabel }}</v-col>
      <v-col v-for="i in nbValues" :key="i" class="d-flex align-center flex-column">
        <p>{{ i }}</p>
        <v-radio :value="i" :color="color" />
      </v-col>
      <v-col v-if="!xs" class="align-self-center pb-4 text-center">{{ maxLabel }}</v-col>
    </v-row>
    <template v-if="xs">
      <p class="text-center">1 = {{ minLabel }}</p>
      <p class="text-center">{{ nbValues}} = {{ maxLabel }}</p>
    </template>
    <v-btn variant="plain"
           :class="{'hidden': !value}"
           @click="value = null"
           class="align-self-end mt-2 mr-12">
      {{ t('clear-selection') }}
    </v-btn>
  </v-radio-group>
</template>

<style scoped>
.hidden {
  visibility: hidden;
}
</style>

<i18n>
{
  "en": {
    "clear-selection": "Clear selection"
  },
  "fr": {
    "clear-selection": "Effacer la sélection"
  }
}
</i18n>
