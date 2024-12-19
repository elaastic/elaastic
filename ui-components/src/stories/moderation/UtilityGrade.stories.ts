import type { Meta, StoryObj } from '@storybook/vue3';

import UtilityGrade from '@/stories/moderation/UtilityGrade.vue';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta = {
  title: 'Moderation/UtilityGrade',
  component: UtilityGrade,
  tags: ['autodocs'],
  args: {
    possibleGrades: ["a", "b", "c"],
  },
} satisfies Meta<typeof UtilityGrade>;

export default meta;
type Story = StoryObj<typeof meta>;


export const Primary: Story = {
  args: {
    possibleGrades: [
      "STRONGLY_DISAGREE",
      "DISAGREE",
      "AGREE",
      "STRONGLY_AGREE"
    ],
  },
};
