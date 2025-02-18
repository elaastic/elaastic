import type { Meta, StoryObj } from '@storybook/vue3'

import ChoiceChip from '@/stories/response/ChoiceChip.vue'

const meta = {
  title: 'Response/ChoiceChip',
  component: ChoiceChip,
  tags: ['autodocs']
} satisfies Meta<typeof ChoiceChip>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    value: 4,
    color: 'primary'
  }
}
