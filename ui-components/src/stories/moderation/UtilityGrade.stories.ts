import {fn} from '@storybook/test';
import type {Meta, StoryObj} from '@storybook/vue3';

import UtilityGrade from '@/stories/moderation/UtilityGrade.vue';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Moderation/UtilityGrade',
  component: UtilityGrade,
  args: {
    isChatGPT: false,
    isTeacher: false,
    // Has to have the name of the event with `on` in front and in camelCase
    onSubmitUtilityGrade: fn(),
  },
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        story: 'Change this description'
      }
    }
  }
} satisfies Meta<typeof UtilityGrade>;

export default meta;
type Story = StoryObj<typeof meta>;


export const DraxoByLearner: Story = {
  args: {
    possibleGrades: [
      {label: "Strongly disagree", value: "STRONGLY_DISAGREE"},
      {label: "Disagree", value: "DISAGREE"},
      {label: "Agree", value: "AGREE"},
      {label: "Strongly Agree", value: "STRONGLY_AGREE"}
    ],
  },
  parameters: {
    docs: {
      description: {
        story: 'A student can grade the utility of a peer grading (made with DRAXO) of their response'
      }
    }
  }
};

export const ChatGPTByLearner: Story = {
  args: {
    possibleGrades: [
      {label: "Strongly disagree", value: "STRONGLY_DISAGREE"},
      {label: "Disagree", value: "DISAGREE"},
      {label: "Agree", value: "AGREE"},
      {label: "Strongly Agree", value: "STRONGLY_AGREE"}
    ],
    isChatGPT: true
  },
  parameters: {
    docs: {
      description: {
        story: 'A student can grade the utility of a peer grading (made by ChatGPT) of their response'
      }
    }
  }
}

export const ChatGPTByTeacher: Story = {
  args: {
    possibleGrades: [
      {label: "Strongly disagree", value: "STRONGLY_DISAGREE"},
      {label: "Disagree", value: "DISAGREE"},
      {label: "Agree", value: "AGREE"},
      {label: "Strongly Agree", value: "STRONGLY_AGREE"}
    ],
    isChatGPT: true,
    isTeacher: true
  },
  parameters: {
    docs: {
      description: {
        story: 'The teacher can grade the utility of a peer grading (made by ChatGPT)'
      }
    }
  }
}
