<script setup lang="ts">

import {ref} from "vue";

export type Selection = {
  label: string,
  value: string
}

interface SelectorResponsivProps {
  selections: Selection[];
}
interface SelectorResponsivEmit {
  (event: 'changeSelection', newSelection: Selection): void;
}

const props = defineProps<SelectorResponsivProps>()
const emit = defineEmits<SelectorResponsivEmit>()

const selected = ref<Selection | null>(null)

const setSelected = (newSelection: Selection) => {
  selected.value = newSelection
  onChangeSelection()
}

const onChangeSelection = () => {
  if (selected.value != null) {
    emit('changeSelection', selected.value)
  }
}

</script>

<template>
  <div id="horizontal-grade-selector">
    <v-btn-toggle v-model="selected" variant="text" color="#0e6eb8" rounded="0" elevation="1"
                  style="margin-top: 10px;">
      <v-btn v-for="(selection, index) in props.selections" :key="index" @click="setSelected(selection)"
             :value="selection" class="text-none text-subtitle-1">
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
  #horizontal-grade-selector {
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
</style>
