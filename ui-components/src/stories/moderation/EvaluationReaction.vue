<script setup lang="ts">

import UtilityGrade from "@/stories/moderation/UtilityGrade.vue";
import {useI18n} from "vue-i18n";
import ReportModal from '@/stories/moderation/report/ReportModal.vue'

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
  /**
   * Wether the form should be a dialog or not
   */
  beADialog: boolean
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
      <UtilityGrade :is-chat-g-p-t="props.isChatGPT" :is-teacher="props.isTeacher" :selected-grade="props.selectedGrade"
                    @submitUtilityGrade="submitUtilityGrade"/>
    </v-col>
    <v-col v-if="!props.isTeacher">
      <ReportModal :content-to-report="contentToReport" :be-a-dialog="beADialog" @submitReport="submitReport"/>
    </v-col>
  </v-row>
</template>

<style scoped>
  #evaluation-reaction-container {
    flex-direction: column;
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
