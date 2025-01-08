import {fn} from '@storybook/test';
import type {Meta, StoryObj} from '@storybook/vue3';

import UtilityGrade from '@/stories/moderation/UtilityGrade.vue';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Moderation/UtilityGrade',
  component: UtilityGrade,
  args: {
    // Has to have the name of the event with `on` in front and in camelCase
    onSubmitUtilityGrade: fn(),
  },
  tags: ['autodocs']
} satisfies Meta<typeof UtilityGrade>;

export default meta;
type Story = StoryObj<typeof meta>;


export const Primary: Story = {
  args: {
    possibleGrades: [
      {label: "Strongly disagree", value: "STRONGLY_DISAGREE"},
      {label: "Disagree", value: "DISAGREE"},
      {label: "Agree", value: "AGREE"},
      {label: "Strongly Agree", value: "STRONGLY_AGREE"}
    ],
  },
};
