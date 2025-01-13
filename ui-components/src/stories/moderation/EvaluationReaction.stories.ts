import { fn } from '@storybook/test'
import type { Meta, StoryObj } from '@storybook/vue3'

import EvaluationReaction from '@/stories/moderation/EvaluationReaction.vue'

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Moderation/EvaluationReaction',
  component: EvaluationReaction,
  args: {
    onSubmitReport: fn(),
    onSubmitUtilityGrade: fn(),
  },
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        story: 'EvaluationReaction is a component that allows users to react to evaluations. It is used in the moderation process.',
      },
    },
  },
} satisfies Meta<typeof EvaluationReaction>

export default meta
type Story = StoryObj<typeof meta>

export const Primary: Story = {
  args: {
    isChatGPT: false,
    isTeacher: false,
    contentToReport: 'Content to report.',
  },
}
