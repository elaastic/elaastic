<script setup lang="ts">

import {useI18n} from "vue-i18n";

const {t} = useI18n()

class Option {
  static YES = new Option("yes");
  static NO = new Option("no");
  static PARTIALLY = new Option("partially");
  static DONT_KNOW = new Option("dontKnow");
  static NO_OPINION = new Option("noOpinion");

  private constructor(private readonly i18nCode: string) {
  }

  public label(): string {
    return t(`option.${this.i18nCode}`)
  }

  public get(type: string): Option {
    Option.values().forEach(option => {
      if (option.i18nCode === type) {
        return option;
      }
    });
    return Option.NO_OPINION;
  }

  public getCssClass(): string {
    switch (this) {
      case Option.YES:
        return "positive";
      case Option.PARTIALLY:
      case Option.NO:
        return "negative";
      case Option.DONT_KNOW:
      case Option.NO_OPINION:
      default:
        return "unknown";
    }
  }

  public static values(): Option[] {
    return [
      Option.YES,
      Option.NO,
      Option.PARTIALLY,
      Option.DONT_KNOW,
      Option.NO_OPINION
    ];
  }
}

class Criteria {
  static D = new Criteria("understandable", "D");
  static R = new Criteria("relevant", "R");
  static A = new Criteria("agreed", "A");
  static X = new Criteria("exhaustive", "X");
  static O = new Criteria("optimal", "O");

  constructor(public readonly i18nCode: string, public readonly capitalLetter: string) {
  }

  public header(): string {
    return t(`criteria.${this.i18nCode}.header`)
  }

  public question(): string {
    return t(`criteria.${this.i18nCode}.question`)
  }

  public static values(): Criteria[] {
    return [
      Criteria.D,
      Criteria.R,
      Criteria.A,
      Criteria.X,
      Criteria.O
    ];
  }
}


// map from criteria to a random Option
const criteriaOptions = new Map<Criteria, Option | null>()
criteriaOptions.set(Criteria.D, Option.YES)
criteriaOptions.set(Criteria.R, Option.YES)
criteriaOptions.set(Criteria.A, Option.NO)
criteriaOptions.set(Criteria.X, Option.DONT_KNOW)
criteriaOptions.set(Criteria.O, null)


</script>

<template>
  <div class="DRAXO-grid" id="expanded">
    <div class="custom-step" :class="criteriaOptions.get(criteria)?.getCssClass()" v-for="criteria in Criteria.values()"
         :key="criteria.capitalLetter">
      <div class="custom-step-content">
        <span>{{ criteria.header() }}</span>

        <v-icon v-if="criteriaOptions.get(criteria)?.getCssClass() === 'positive'" icon="mdi-check-bold"></v-icon>
        <v-icon v-else-if="criteriaOptions.get(criteria)?.getCssClass() === 'negative'" icon="mdi-close"></v-icon>
        <v-icon v-else-if="criteriaOptions.get(criteria)?.getCssClass() === 'unknown'" icon="mdi-help"></v-icon>
      </div>
    </div>
  </div>
  <div class="DRAXO-grid" id="collasped">
    <div class="custom-step" :class="criteriaOptions.get(criteria)?.getCssClass()" v-for="criteria in Criteria.values()"
         :key="criteria.capitalLetter">
      <div class="custom-step-content">
        <span class="capital-letter">{{ criteria.capitalLetter }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
:root {
  --border-color: #7A7A7A;
  --default-color: #757575;
  --default-bg-color: #F2F2F2;
}

.DRAXO-grid {
  display: flex;
  flex-direction: row;
  width: 100%;
  font-size: 1rem;
}

.custom-step {
  font-size: 1em;
  position: relative;
  display: flex;
  flex-direction: row;
  flex: 1 0 auto;
  flex-wrap: wrap;
  vertical-align: middle;
  align-items: center;
  justify-content: center;
  margin: 0 0;
  padding: 1em 0 1em 0;
  height: 3.5em;

  /* By default a step is disabled */
  background-color: #F2F2F2;
  border: 1px solid #D9D9D9;
  color: #757575;
}

.custom-step.positive {
  background-color: #99DBBB;
  border: 1px solid #7A7A7A;
  color: #01562d;
}

.custom-step.negative {
  background-color: #FCB5D0;
  border: 1px solid #7A7A7A;
  color: #802345;
}

.custom-step.unknown {
  background-color: white;
  border: 1px solid #7A7A7A;
  color: black;
}

.custom-step.negative, .custom-step.unknown {
  font-weight: bold;
}

.custom-step > .custom-step-content {
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  text-align: center;

  z-index: 3;

  font-size: 1rem;
}

.custom-step:first-child {
  border-radius: .28571429rem 0 0 .28571429rem;
}

.custom-step:last-child {
  border-radius: 0 .28571429rem .28571429rem 0;
}

#expanded {
  .custom-step::after {
    position: absolute;
    z-index: 2;
    content: '';
    top: 50.5%;
    right: -0.7px;
    background-color: inherit;
    width: 2.5em;
    height: 2.5em;

    /* We only keep the top and right border */
    border-width: 0 1px 1px 0;
    border-style: solid;
    border-color: inherit;
    border-radius: 1px;

    transform: translateY(-50%) translateX(50%) rotate(-45deg);
  }

  .custom-step:last-child::after {
    display: none;
  }

  .custom-step:not(:first-child) {
    border-left-width: 0 !important;
  }

  .custom-step:not(:last-child) {
    border-right-width: 0 !important;
  }

  .custom-step:not(:first-child) > .custom-step-content {
    margin-left: 1.1em;
  }

  /* If their is an icon next to the text (span), add a little margin */
  .custom-step-content span:has(~ .v-icon) {
    margin-right: 1.1em;
  }
}

#collasped {
  display: none;

  .custom-step {
    border-left: 1px solid #7A7A7A !important;
    border-right: 1px solid #7A7A7A !important;
    border-bottom: 1px solid #7A7A7A !important;
    border-top: 1px solid #7A7A7A !important;
  }
}

@media (max-width: 768px) {
  #expanded {
    display: none;
  }

  #collasped {
    display: flex;
  }
}
</style>

<i18n>
{
  "en": {
    "option": {
      "yes": "Yes",
      "no": "No",
      "partially": "Partially",
      "dont_know": "Don't know",
      "no_opinion": "No opinion"
    },
    "criteria": {
      "understandable": {
        "header": "unDerstandable",
        "question": "I understand what the answer says"
      },
      "relevant": {
        "header": "Relevant",
        "question": "I think the answer corresponds to the question asked"
      },
      "agreed": {
        "header": "Agreed",
        "question": "I agree with the proposed answer"
      },
      "exhaustive": {
        "header": "eXhaustive",
        "question": "I think the answer can be improved"
      },
      "optimal": {
        "header": "Optimal",
        "question": "I think the answer can be improved"
      }
    }
  },
  "fr": {
    "option": {
      "yes": "Oui",
      "no": "Non",
      "partially": "Partiellement",
      "dontKnow": "Ne sais pas",
      "noOpinion": "Pas d'opinion"
    },
    "criteria": {
      "understandable": {
        "header": "Déchiffrable",
        "question": "Je comprend ce qui est écrit dans la réponse"
      },
      "relevant": {
        "header": "Répond à la question",
        "question": "Je trouve que la réponse correspond à la question posée"
      },
      "agreed": {
        "header": "Acceptable",
        "question": "Je suis d'accord avec la réponse proposée"
      },
      "exhaustive": {
        "header": "eXhaustive",
        "question": "Je pense que la réponse est complète"
      },
      "optimal": {
        "header": "Optimale",
        "question": "Je pense que la réponse peut être améliorée"
      }
    }
  }
}
</i18n>
