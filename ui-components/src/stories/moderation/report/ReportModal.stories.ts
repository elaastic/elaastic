import type { Meta, StoryObj } from '@storybook/vue3'
import ReportModal from '@/stories/moderation/report/ReportModal.vue'
import { fn } from '@storybook/test'

const lorem =
  'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus tincidunt elementum dapibus. Mauris sed auctor sem. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Ut egestas sapien nec feugiat consectetur. Proin hendrerit sem finibus, iaculis lectus non, consequat odio.'

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Moderation/ReportModal',
  component: ReportModal,
  tags: ['autodocs'],
  args: {
    onSubmitReport: fn(),
  },
  parameters: {
    docs: {
      description: {
        story:
          'ReportModal is a component that allows users to report content. It is used in the moderation process.',
      },
    },
  },
} satisfies Meta<typeof ReportModal>
export default meta

type Story = StoryObj<typeof meta>

export const Primary: Story = {
  args: {
    contentToReport: 'This is the content to report. ' + lorem,
    beADialog: true,
  },
}

export const PrimaryNotADialog: Story = {
  args: {
    contentToReport: 'This is the content to report. ' + lorem,
    beADialog: false,
  },
}
