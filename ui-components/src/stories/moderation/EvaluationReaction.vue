<script setup lang="ts">

import UtilityGrade from "@/stories/moderation/UtilityGrade.vue";
import {useI18n} from "vue-i18n";

const {t} = useI18n()

</script>

<template>
  <v-row id="evaluation-reaction-container">
    <v-col>
      <UtilityGrade :is-chat-g-p-t="false" :is-teacher="false"/>
    </v-col>
    <v-col align-self="center">
      <v-btn class="text-none" variant="outlined" color="#b7446f" prepend-icon="mdi-alert" id="report-btn">
        {{ t('report') }}
      </v-btn>
    </v-col>
  </v-row>
</template>

<style scoped>
@media (max-width: 600px) {
  #evaluation-reaction-container {
    flex-direction: column;
  }
  #report-btn {
    width: 100%;
  }
}

</style>

<i18n>
{
  "en": {
    "report": "Report"
  },
  "fr": {
    "report": "Signaler"
  }
}
</i18n>
