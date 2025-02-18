import type { Meta, StoryObj } from '@storybook/vue3'

import EvaluationCard from '@/stories/evaluation/EvaluationCard.vue'
import { ref } from 'vue'
import type { LikertValue } from '@/stories/evaluation/Likert'

const meta = {
  title: 'Evaluation/EvaluationCard',
  component: EvaluationCard,
  tags: ['autodocs']
} satisfies Meta<typeof EvaluationCard>

export default meta
type Story = StoryObj<typeof meta>

export const ExclusiveChoice: Story = {
  render: (args) => ({
    components: { EvaluationCard },
    setup() {
      const evaluationValue = ref<LikertValue>(args.modelValue || null)
      return { ...args, evaluationValue }
    },
    template: `
      <evaluation-card v-model="evaluationValue"
                       :evaluation-num="evaluationNum"
                       :choices="choices"
                       :response="response" />
    `
  }),
  args: {
    modelValue: null,
    evaluationNum: 3,
    response: {
      id: 1,
      questionType: 'ExclusiveChoice',
      choice: 1,
      explanation: 'Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry\'s standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen book. It has survived not only five centuries, but also the leap into electronic typesetting, remaining essentially unchanged. It was popularised in the 1960s with the release of Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software like Aldus PageMaker including versions of Lorem Ipsum.'
    }
  }
}

export const MultipleChoice: Story = {
  render: ExclusiveChoice.render,
  args: {
    ...ExclusiveChoice.args,
    response: {
      id: 2,
      questionType: 'MultipleChoice',
      choices: [1, 4],
      explanation: 'Another explanation'
    },
  }
}

export const OpenEnded: Story = {
  render: ExclusiveChoice.render,
  args: {
    ...ExclusiveChoice.args,
    response: {
      "id": 3,
      "questionType": "OpenEnded",
      "explanation": "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi hendrerit lorem imperdiet imperdiet blandit. Quisque efficitur, arcu vitae lobortis sodales, elit augue sollicitudin diam, id ullamcorper diam ipsum placerat felis. Vestibulum eu bibendum ante, id pellentesque dolor. Duis sit amet ipsum sit amet nulla facilisis tempor. Fusce eget metus nunc. Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Quisque id libero vitae ante faucibus pharetra eu sit amet elit. Sed auctor neque a justo pulvinar, vel rhoncus ex consequat. Maecenas gravida non velit semper eleifend. Maecenas eget eros mi.</p>\n      <p>Nullam vitae lobortis enim, nec elementum lorem. Maecenas auctor pharetra eros non dignissim. Suspendisse potenti. Donec fringilla urna vehicula, aliquet neque ac, congue lorem. Quisque vitae mauris quis velit accumsan cursus et vulputate leo. Quisque aliquet ullamcorper porttitor. Praesent enim metus, pharetra eget mi eu, cursus varius neque. Praesent sodales enim sed erat venenatis ultrices. In sit amet sapien eu lorem auctor bibendum quis nec libero. Nam malesuada aliquet viverra.</p>\n      <p>Phasellus convallis tempus sagittis. Maecenas malesuada laoreet tellus, vitae maximus leo rutrum eu. Quisque quis metus in arcu ullamcorper suscipit non a ante. Aenean laoreet malesuada mauris, vel pulvinar ipsum euismod at. Maecenas nec consequat purus. Integer id ultricies ipsum. Integer viverra sapien lectus, at feugiat nulla sodales fermentum.</p>\n      <p>Curabitur pulvinar erat sed magna dapibus, ac aliquet orci tincidunt. Aliquam fringilla, tellus nec ultricies convallis, eros tortor pretium justo, a dignissim nibh mi a ante. Ut ligula arcu, pellentesque sit amet tristique ac, efficitur vitae ex. Pellentesque tempus ligula sapien, at accumsan dolor aliquam sed. Phasellus imperdiet velit non dui placerat, finibus consequat mi tempor. Nulla blandit libero sed dui mollis sollicitudin. Cras maximus porta nunc. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed porttitor luctus tincidunt. Vivamus enim turpis, elementum vel nisi ut, imperdiet mollis dolor. Vestibulum mattis massa ac vulputate pellentesque. Nullam sit amet purus ex. Donec volutpat turpis nisl, a sodales odio consequat sed. Quisque at nunc in purus iaculis porttitor.</p>\n      <p>Vivamus porta, magna et malesuada sagittis, dolor metus fringilla nibh, ut congue nulla ex id nunc. Vivamus ultricies ultricies enim, vel varius libero rhoncus in. Curabitur rhoncus metus et condimentum lacinia. Duis sapien enim, auctor a velit a, lobortis facilisis massa. Etiam vehicula velit in quam feugiat, at feugiat enim rutrum. Sed vitae sem massa. Aenean maximus, lacus sit amet luctus laoreet, metus elit bibendum ante, sed euismod purus mauris in enim. In hac habitasse platea dictumst. Nullam suscipit, lorem in ornare dignissim, orci lorem rutrum sapien, et consectetur erat eros non dolor. Nam gravida faucibus cursus. Cras bibendum ante ut purus facilisis vulputate. In tristique augue non nunc <b>luctus faucibus</b>.</p>\n      "
    }
  }
}
