import {fn} from '@storybook/test';
import type {Meta, StoryObj} from '@storybook/vue3';

import EvaluationReaction from "@/stories/moderation/EvaluationReaction.vue";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Moderation/EvaluationReaction',
  component: EvaluationReaction,
  args: {

  },
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        story: 'TODO'
      }
    }
  }
} satisfies Meta<typeof EvaluationReaction>;

export default meta;
type Story = StoryObj<typeof meta>;


export const Primary: Story = {
  parameters: {
    docs: {
      description: {
        story: 'TODO'
      }
    }
  }
};
