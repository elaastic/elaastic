<script setup lang="ts">
import {useI18n} from 'vue-i18n'
import { computed, ref, onMounted, onUnmounted } from 'vue'

const {t} = useI18n()

class ReportReason {
  static FALSE_INFORMATION = new ReportReason('FALSE_INFORMATION');
  static INCOHERENCE = new ReportReason('INCOHERENCE');
  static PERSONAL_JUDGMENT = new ReportReason('PERSONAL_JUDGMENT');
  static OTHER = new ReportReason('OTHER');

  private constructor(public readonly key: string) {}

  shortLabel(): string {
    const { t } = useI18n();
    return t(`reportReason.${this.key}.short`);
  }
  longLabel(): string {
    const { t } = useI18n();
    return t(`reportReason.${this.key}.long`);
  }

  static values(): ReportReason[] {
    return [
      ReportReason.FALSE_INFORMATION,
      ReportReason.INCOHERENCE,
      ReportReason.PERSONAL_JUDGMENT,
      ReportReason.OTHER
    ];
  }
}

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

const isSmallScreen = ref(window.innerWidth < 600)

// Ajouter un écouteur pour mettre à jour isSmallScreen quand la fenêtre change de taille
onMounted(() => {
  window.addEventListener('resize', () => {
    isSmallScreen.value = window.innerWidth < 600
  })
})

// Nettoyer l'écouteur quand le composant est détruit
onUnmounted(() => {
  window.removeEventListener('resize', () => {
    isSmallScreen.value = window.innerWidth < 600
  })
})

</script>

<template>
  <v-dialog v-model="dialog" max-width="600" :fullscreen="isSmallScreen">
    <template v-slot:activator="{ props: activatorProps }">
      <v-btn class="text-none" variant="outlined" color="#b7446f"
             prepend-icon="mdi-alert" id="report-btn" v-bind="activatorProps">
        {{ t('report') }}
      </v-btn>
    </template>
    <v-card :title="t('title')">
      <v-card-text>
        <!-- Content to report -->
        <h4>{{ t('content-to-report') }}</h4>
        <p>
          {{ props.contentToReport }}
        </p>
        <br>

        <!-- Report reason -->
        <h4>{{ t('report-reason-title') }}</h4>
        <v-checkbox-btn v-model="selectedReportReason" v-for="reason in reportReasonsAvailable" :key="reason.key"
                        :value="reason.key" :label="reason.longLabel()"/>
        <br>

        <!-- Report detail -->
        <h4>{{ t('report-detail') }}</h4>
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
@media (max-width: 600px) {
  #report-btn {
    width: 100%;
  }
}

</style>
<i18n>
{
  "en": {
    "report": "Report",
    "title": "Report content",
    "content-to-report": "Content to report:",
    "report-reason-title": "Report reason:",
    "report-detail": "Report detail:",
    "report-detail-placeholder": "Details",
    "submit": "Send",
    "cancel": "Cancel",
    "reportReason": {
      "FALSE_INFORMATION": {
        "short": "False Information",
        "long": "The statement contains false information"
      },
      "INCOHERENCE": {
        "short": "Incoherence",
        "long": "The statement contains inconsistencies"
      },
      "PERSONAL_JUDGMENT": {
        "short": "Personal Judgment",
        "long": "The statement contains a judgment about personal characteristics"
      },
      "OTHER": {
        "short": "Other",
        "long": "Other reason"
      }
    }
  },
  "fr": {
    "report": "Signaler",
    "title": "Signaler le contenu",
    "content-to-report": "Contenu à signaler :",
    "report-reason-title": "Motif(s) du signalement :",
    "report-detail": "Préciser le motif :",
    "report-detail-placeholder": "Précisions",
    "submit": "Envoyer",
    "cancel": "Annuler",
    "reportReason": {
      "FALSE_INFORMATION": {
        "short": "Information Fausse",
        "long": "Le propos contient de fausses informations"
      },
      "INCOHERENCE": {
        "short": "Incohérence",
        "long": "Le propos contient des incohérences"
      },
      "PERSONAL_JUDGMENT": {
        "short": "Jugement Personnel",
        "long": "Le propos contient un jugement sur des caractéristiques personnelles"
      },
      "OTHER": {
        "short": "Autre",
        "long": "Autre raison"
      }
    }
  }
}
</i18n>
