import type { Meta, StoryObj } from '@storybook/vue3'

import ConfrontingViewpoint from '@/stories/evaluation/ConfrontingViewpoint.vue'

const meta = {
  title: 'Evaluation/ConfrontingViewpoints',
  component: ConfrontingViewpoint,
  tags: ['autodocs']
} satisfies Meta<typeof ConfrontingViewpoint>

export default meta
type Story = StoryObj<typeof meta>

export const Default: Story = {
  args: {
    responses: [
      {
        id: 1,
        questionType: 'ExclusiveChoice',
        choice: 2,
        explanation: `Lorem ipsum dolor sit amet, consectetur adipiscing elit. In in quam dignissim, mattis diam in, hendrerit nisl. Suspendisse potenti. Aenean massa lorem, placerat ut nibh nec, iaculis rutrum orci. Quisque et sollicitudin elit. Ut ornare ante quis mauris scelerisque pretium. Nunc pellentesque nec velit et tincidunt. Aliquam erat volutpat. Aenean justo ex, dignissim vulputate orci auctor, consectetur sagittis lacus.`
      } ,
      {
        id: 2,
        questionType: 'MultipleChoice',
        choices: [3, 4],
        explanation: `In iaculis eros et tortor malesuada, aliquet bibendum neque sollicitudin. Mauris sodales turpis finibus ante tempor semper. Nam quis ligula nec ligula iaculis egestas. Aliquam vestibulum finibus elit. Duis rhoncus orci convallis, auctor mauris non, dapibus nulla. Duis id ligula metus. Suspendisse posuere finibus nulla.`
      } ,
      {
        id: 3,
        questionType: 'OpenEnded',
        explanation: `Vivamus elit felis, tempor non tellus a, laoreet mattis lacus. Sed aliquam mauris ut sem vulputate, sit amet semper tellus volutpat. Proin accumsan erat at elit interdum accumsan. Quisque porta, lacus vitae varius facilisis, tellus ipsum auctor felis, quis ornare nisl lacus non diam. Vivamus dapibus lacus sapien. Ut fermentum lobortis diam, eu feugiat metus pulvinar ac. Quisque venenatis tincidunt tempus. Suspendisse sollicitudin nisi quis leo molestie facilisis.`
      }
    ]
  }
}

export const NoResponse: Story = {
  args: {
    responses: []
  }
}
