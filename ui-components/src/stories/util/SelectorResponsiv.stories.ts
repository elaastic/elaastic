import type {Meta, StoryObj} from '@storybook/vue3';

import SelectorResponsiv from "@/components/util/SelectorResponsiv.vue";
import {fn} from "@storybook/test";

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Util/SelectorResponsiv',
  component: SelectorResponsiv,
  args: {
    onChangeSelection: fn(),
  },
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        story: 'Change this description'
      }
    }
  }
} satisfies Meta<typeof SelectorResponsiv>;

export default meta;
type Story = StoryObj<typeof meta>;


export const Primary: Story = {
  args: {
    selections: [
      {label: "First", value: "1"},
      {label: "Second", value: "2"},
      {label: "Last", value: "N"}
    ],
  },
};
