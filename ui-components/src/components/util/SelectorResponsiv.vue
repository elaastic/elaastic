<script setup lang="ts">

import {computed, ref} from "vue";

export type Selection = {
  label: string,
  value: string
}

interface SelectorResponsivProps {
  /**
   * The list of selections to display
   */
  selections: Selection[];
  /**
   * The selected value in the list
   */
  selected: string | null;
}
interface SelectorResponsivEmit {
  (event: 'changeSelection', newSelection: Selection): void;
}

const props = withDefaults(defineProps<SelectorResponsivProps>(), {
  selected: null
})
const emit = defineEmits<SelectorResponsivEmit>()

/**
 * Get the selected value from the props.
 * If the selected value is not in the selections, return null.
 * @returns the selected value or null
 */
const getSelectedFromProps = (): Selection | null => {
  const selected = props.selected

  console.log(selected)

  if (selected == null) {
    return null
  }

  return props.selections.find(selection => selection.value === selected) ?? null
}

const selected = ref<Selection | null>(getSelectedFromProps())

const setSelected = (newSelection: Selection) => {
  selected.value = newSelection
  onChangeSelection()
}

const onChangeSelection = () => {
  if (selected.value != null) {
    emit('changeSelection', selected.value)
  }
}

// Compute the button width based on the number of selections
const buttonWidth =`${100 / props.selections.length}%`;
</script>

<template>
  <div id="horizontal-grade-selector-container">
    <v-btn-toggle v-model="selected" variant="text" color="#0e6eb8" rounded="0" elevation="1"
                  style="margin-top: 10px;" id="horizontal-grade-selector">
      <v-btn v-for="(selection, index) in props.selections" :key="index" @click="setSelected(selection)"
             :value="selection" class="text-none text-subtitle-1" :style="{ width: buttonWidth}">
        {{ selection.label }}
      </v-btn>
    </v-btn-toggle>
  </div>
  <div id="vertical-grade-selector-container">
    <v-btn-toggle v-model="selected" variant="text" color="#0e6eb8" rounded="0" elevation="1"
                  style="margin-top: 10px;" id="vertical-grade-selector">
      <v-btn v-for="(selection, index) in props.selections" :key="index" @click="setSelected(selection)"
             :value="selection" class="text-none text-subtitle-1 btn-vertical-selector">
        {{ selection.label }}
      </v-btn>
    </v-btn-toggle>
  </div>
</template>

<style scoped>
@media (max-width: 600px) {
  #horizontal-grade-selector-container {
    display: none;
  }
}

@media not (max-width: 600px) {
  #vertical-grade-selector-container {
    display: none;
  }
}

#vertical-grade-selector {
  flex-direction: column;
  height: unset;
  width: 100%;

  .btn-vertical-selector {
    padding: 3%;
  }
}

#horizontal-grade-selector {
  flex-direction: row;
  width: 100%;
}
</style>
