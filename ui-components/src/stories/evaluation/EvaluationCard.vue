<script setup lang="ts">
import ChoiceChip from '@/stories/response/ChoiceChip.vue'
import LikertScale from '@/stories/evaluation/LikertScale.vue'
import { computed } from 'vue'
import type { LikertValue } from '@/stories/evaluation/Likert'
import { type AnyResponse } from '@/models/Response'
import { useI18n } from 'vue-i18n'

export interface EvaluationCardProps {
  /**
   * The evaluation value of this card
   */
  modelValue: LikertValue,
  /**
   * Evaluation number in the list of evaluations
   */
  evaluationNum?: number | null,
  /**
   * The alternative response to evaluate
   */
  response: AnyResponse
}

const props = withDefaults(defineProps<EvaluationCardProps>(), {
  evaluationNum: null
})

interface EvaluationCartEvents {
  /** Fires when the user changes its evaluation of this response */
  (event: 'update:modelValue', value: LikertValue): void;
}

const emit = defineEmits<EvaluationCartEvents>()
const { t } = useI18n()

const evaluationValue = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})
const explanationLabel = computed(() => props.response.questionType == 'OpenEnded' ? t('answer') : t('explanation'))
const explanation = props.response.explanation

const getChoices = (response: AnyResponse) => {
  switch (response.questionType) {
    case 'ExclusiveChoice':
      return [response.choice]

    case 'MultipleChoice':
      return response.choices

    case 'OpenEnded':
      return undefined
  }
}
const choices = getChoices(props.response)
</script>

<template>
  <v-card
    class="fill-height mx-auto border-bottom d-flex flex-column"
  >
    <v-card-item class="bg-surface-light">
      <div class="d-flex justify-space-between w-100 align-center">
        <div class="text-overline mb-1">
          {{ t('alternative-answer') }} <span v-if="evaluationNum">#{{ evaluationNum }}</span>
        </div>
        <v-chip v-if="!evaluationValue" class="ml-auto" color="red">{{ t('require-evaluation') }}</v-chip>
        <v-icon v-else class="ml-auto" color="green">mdi-checkbox-marked</v-icon>
      </div>

    </v-card-item>

    <v-card-item>
      <v-container>
        <v-text-field v-if="choices" :label="t('choices')" model-value=" " readonly variant="plain">
          <choice-chip v-for="choice in choices" :key="choice" :value="choice" color="grey" />
        </v-text-field>

        <v-text-field :label="explanationLabel" variant="plain" model-value=" " readonly>
          <div v-html="explanation" />
        </v-text-field>
      </v-container>
    </v-card-item>

    <v-spacer></v-spacer>
    <v-divider />
    <v-card-item>
      <v-label class="mb-2">{{ t('notice') }}</v-label>
      <div class="d-flex justify-center">
        <div>
          <likert-scale v-model="evaluationValue"
                        :min-label="t('min-label')"
                        :max-label="t('max-label')"
                        color="primary" />
        </div>
      </div>
    </v-card-item>
  </v-card>
</template>

<style scoped>

</style>

<i18n>
{
  "en": {
    "alternative-answer": "Alternative answer",
    "require-evaluation": "Require evaluation",
    "choices": "Choices",
    "explanation": "Explanation",
    "answer": "Answer",
    "notice": "To what extent do you agree with the explanation given?",
    "min-label": "Strongly disagree",
    "max-label": "Strongly agree"
  },
  "fr": {
    "alternative-answer": "Réponse alternative",
    "require-evaluation": "Évaluation requise",
    "choices": "Choix",
    "explanation": "Explication",
    "answer": "Réponse",
    "notice": "A quel point êtes-vous d'accord avec l'explication proposée ?",
    "min-label": "Pas du tout d'accord",
    "max-label": "Totalement d'accord"
  }
}
</i18n>
