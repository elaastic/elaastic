import type {Meta, StoryObj} from '@storybook/vue3';
import DraxoGrid from "@/components/evaluation/draxo/DraxoGrid.vue";


// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'evaluation/draxo/DraxoGrid',
  component: DraxoGrid,
  args: {},
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        story: 'TODO'
      }
    }
  }
} satisfies Meta<typeof DraxoGrid >;

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
