<script setup lang="ts">

import UtilityGrade from "@/stories/moderation/UtilityGrade.vue";
import {useI18n} from "vue-i18n";
import ReportModal from '@/stories/moderation/ReportModal.vue'

const {t} = useI18n()

export interface EvaluationReactionProps {
  /**
   * Wether the evaluation has been done by ChatGPT or not
   */
  isChatGPT: boolean
  /**
   * Whether the user is a teacher or not
   */
  isTeacher: boolean,
  /**
   * The selected grade if any
   */
  selectedGrade: string | null
  /**
   * The content to report
   */
  contentToReport: string
}
export interface EvaluationReactionEvents {
  (event: 'submitUtilityGrade', gradeSelected: string): void;
  (event: 'submitReport', reportReason: string[], reportDetail: string): void
}

const props = defineProps<EvaluationReactionProps>()
const emit = defineEmits<EvaluationReactionEvents>()

function submitUtilityGrade(gradeSelected: string) {
  emit('submitUtilityGrade', gradeSelected)
}
function submitReport(reportReason: string[], reportDetail: string) {
  emit('submitReport', reportReason, reportDetail)
}

</script>

<template>
  <v-row id="evaluation-reaction-container">
    <v-col>
      <UtilityGrade :is-chat-g-p-t="props.isChatGPT" :is-teacher="props.isTeacher" :selected-grade="props.selectedGrade" @submitUtilityGrade="submitUtilityGrade"/>
    </v-col>
    <v-col align-self="center">
      <ReportModal :content-to-report="contentToReport" @submitReport="submitReport"/>
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
