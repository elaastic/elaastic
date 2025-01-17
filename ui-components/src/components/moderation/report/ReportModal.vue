<script setup lang="ts">
import {useI18n} from 'vue-i18n'
import {onMounted, onUnmounted, ref} from 'vue'
import ReportForm from "@/components/moderation/report/ReportForm.vue";

const {t} = useI18n()

export interface ReportModalProps {
  /**
   * The content to report
   */
  contentToReport: string
  /**
   * Wether the form should be a dialog or not
   */
  beADialog: boolean
}

export interface ReportModalEvents {
  (event: 'submitReport', reportReason: string[], reportDetail: string): void
}

const props = defineProps<ReportModalProps>()
const emit = defineEmits<ReportModalEvents>()

const showForm = ref<boolean>(false)

function submitReport(selectedReportReason: string[], reportDetail: string) {
  emit('submitReport', selectedReportReason, reportDetail)
  showForm.value = false
}

const isSmallScreen = ref(window.innerWidth < 600)

// Add a listener to update isSmallScreen when the window changes size
onMounted(() => {
  window.addEventListener('resize', () => {
    isSmallScreen.value = window.innerWidth < 600
  })
})

// Clean the listener when the component is destroyed
onUnmounted(() => {
  window.removeEventListener('resize', () => {
    isSmallScreen.value = window.innerWidth < 600
  })
})

</script>

<template>
  <!-- Dialog -->
  <v-dialog v-model="showForm" max-width="600" :fullscreen="isSmallScreen" v-if="props.beADialog">
    <template v-slot:activator="{ props: activatorProps }">
      <v-btn class="text-none" variant="outlined" color="#b7446f" prepend-icon="mdi-alert" id="report-btn"
             v-bind="activatorProps">
        {{ t('report') }}
      </v-btn>
    </template>
    <ReportForm content-to-report="props.contentToReport" @submitReport="submitReport" @cancel="showForm = false"/>
  </v-dialog>

  <!-- Not a dialog -->
  <div v-else>
    <v-expand-transition>
      <div v-if="!showForm">
        <v-btn class="text-none" variant="outlined" color="#b7446f" prepend-icon="mdi-alert" id="report-btn"
               @click="showForm = !showForm">
          {{ t('report') }}
        </v-btn>
      </div>
    </v-expand-transition>
    <v-expand-transition>
      <div v-if="showForm" id="report-form">
        <ReportForm :content-to-report="props.contentToReport" @submitReport="submitReport" @cancel="showForm = false"/>
      </div>
    </v-expand-transition>
  </div>
</template>

<style scoped>
#report-btn {
  width: 100%;
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
