<script setup lang="ts">
import ReportReason from '@/models/moderation/ReportReason'
import { useI18n } from 'vue-i18n'
import { ref } from 'vue'

const { t } = useI18n()

export interface ReportModalProps {
  /**
   * The content to report
   */
  contentToReport: string
}

export interface ReportModalEvents {
  (event: 'submitReport', reportReason: string[], reportDetail: string): void
}

const props = defineProps<ReportModalProps>()
const emit = defineEmits<ReportModalEvents>()

const reportReasonsAvailable: ReportReason[] = ReportReason.values()
let selectedReportReason = ref<string[]>([])
const reportDetail = ref<string>()
const dialog = ref<boolean>(false)

const detailMandatory = () => {
  return selectedReportReason.value.includes(ReportReason.OTHER.key)
}
const canSubmit = () => {
  return selectedReportReason.value.length > 0 && (!detailMandatory() || reportDetail.value)
}

function submitReport() {
  emit('submitReport', selectedReportReason.value, reportDetail.value ?? '')
  dialog.value = false
}

</script>

<template>
  <v-dialog v-model="dialog" max-width="600">
    <template v-slot:activator="{ props: activatorProps }">
      <v-btn class="text-none" variant="outlined" color="#b7446f"
             prepend-icon="mdi-alert" id="report-btn" v-bind="activatorProps">
        {{ t('report') }}
      </v-btn>
    </template>
    <v-card :title="t('title')">
      <v-card-text>
        <!-- Content to report -->
        <h4>{{ t('contentToReport') }}</h4>
        <p>
          {{ props.contentToReport }}
        </p>
        <br>

        <!-- Report reason -->
        <h4>{{ t('reportReason') }}</h4>
        <v-checkbox-btn v-model="selectedReportReason" v-for="reason in reportReasonsAvailable" :key="reason.key"
                        :value="reason.key" :label="reason.longLabel()" />
        <br>

        <!-- Report detail -->
        <h4>{{ t('reportDetail') }}</h4>
        <v-textarea v-model="reportDetail" variant="outlined" rows="2"
                    :placeholder="t('report-detail-placeholder')"></v-textarea>
      </v-card-text>

      <v-divider></v-divider>

      <v-card-actions>
        <v-spacer></v-spacer>

        <v-btn :text="t('submit')" @click="submitReport" :disabled="!canSubmit()" variant="flat"
               class="text-none text-white"
               color="#95c155"></v-btn>
        <v-btn :text="t('cancel')" @click="dialog = false" variant="flat" class="text-none"></v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style scoped>

</style>
<i18n>
{
  "en": {
    "report": "Report",
    "title": "Report content",
    "contentToReport": "Content to report:",
    "reportReason": "Report reason:",
    "reportDetail": "Report detail:",
    "report-detail-placeholder": "Details",
    "submit": "Send",
    "cancel": "Cancel"
  },
  "fr": {
    "report": "Signaler",
    "title": "Signaler le contenu",
    "contentToReport": "Contenu à signaler :",
    "reportReason": "Motif(s) du signalement :",
    "reportDetail": "Préciser le motif :",
    "report-detail-placeholder": "Précisions",
    "submit": "Envoyer",
    "cancel": "Annuler"
  }
}
</i18n>
