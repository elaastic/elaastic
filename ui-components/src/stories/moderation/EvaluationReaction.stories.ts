import {expect, fn, waitFor} from '@storybook/test'
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
        story:
          'EvaluationReaction is a component that allows users to react to evaluations. It is used in the moderation process.',
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

export const TeacherReact: Story = {
  args: {
    isChatGPT: true,
    isTeacher: true,
    contentToReport: 'Content to report.',
  },
  parameters: {
    docs: {
      description: {
        story:
          'A teacher can give an Utility Grade to the evaluation. But the teacher cannot report the evaluation.',
      },
    },
  },
  play: async ({ canvas, step }: { canvas: any; step: any }) => {
    const countBtn = () => {
      return canvas.queryAllByRole('button').length
    }

    await step('Must have 4 button, because the report button is hidden', async () => {
        await expect(countBtn()).toBe(4)
    })

    await step('Click on the first button', async () => {
      await waitFor(() => canvas.getAllByRole('button')[0])
      await canvas.getAllByRole('button')[0].click()
    })

    await step('Must have 5 button, because the report button is hidden and the submit button should be visible', async () => {
        await expect(countBtn()).toBe(5)
    })
  }
}
