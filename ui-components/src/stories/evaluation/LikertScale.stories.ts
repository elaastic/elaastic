import type { Meta, StoryObj } from '@storybook/vue3'

import LikertScale from '@/stories/evaluation/LikertScale.vue'
import { ref } from 'vue'
import type { LikertValue } from '@/stories/evaluation/Likert'

const meta = {
  title: 'Evaluation/Likert',
  component: LikertScale,
  tags: ['autodocs'],
  argTypes: {
    color: { control: 'text' },
  },
  decorators: [
    (story) => ({
      components: { story },
      template: '<v-container class="d-flex justify-center"><div><story /></div></v-container>',
    }),
  ],
} satisfies Meta<typeof LikertScale>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  render: (args) => ({
    components: { LikertScale },
    setup() {
      const value = ref<LikertValue>(args.modelValue || null);
      return { ...args, value };
    },
    template: `
        <likert-scale v-model="value" :nb-values="nbValues" :min-label="minLabel" :max-label="maxLabel" :color="color" />
    `,
  }),
  args: {
    modelValue: null,
    nbValues: 5,
    minLabel: "Strongly disagree",
    maxLabel: "Strongly agree",
    color: "primary",
  },
}
