import type { Meta, StoryObj } from '@storybook/vue3';

import UtilityGrade from '@/stories/moderation/UtilityGrade.vue';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta = {
  title: 'Moderation/UtilityGrade',
  component: UtilityGrade,
  tags: ['autodocs'],
  args: {
    possibleGrades: [{label: "bad", value: "STRONGLY_DISAGREE"}, {label: "ok", value: "DISAGREE"}, {label: "good", value: "AGREE"}, {label: "great", value: "STRONGLY_AGREE"}],
  },
} satisfies Meta<typeof UtilityGrade>;

export default meta;
type Story = StoryObj<typeof meta>;


export const Primary: Story = {
  args: {
    possibleGrades: [{label: "Bad", value: "STRONGLY_DISAGREE"}, {label: "Ok", value: "DISAGREE"}, {label: "Good", value: "AGREE"}, {label: "Great", value: "STRONGLY_AGREE"}],
  },
};
