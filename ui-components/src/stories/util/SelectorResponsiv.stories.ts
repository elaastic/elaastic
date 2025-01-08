import type {Meta, StoryObj} from '@storybook/vue3';

import SelectorResponsiv from "@/components/util/SelectorResponsiv.vue";
import {expect, fn, userEvent, waitFor, within} from "@storybook/test";

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

export const PrimaryWithTest: Story = {
  args: {
    selections: [
      {label: "First", value: "1"},
      {label: "Second", value: "2"},
      {label: "Last", value: "N"}
    ],
  },
  play: async ({args, canvasElement, step}) => {
    const canvas = within(canvasElement);

    // Click the first button
    await step('Select the first button', async () => {
      await userEvent.click(canvas.getAllByRole('button')[0]);
    });

    // Now we can assert that the onChangeSelection arg was called
    await waitFor(() => expect(args.onChangeSelection).toHaveBeenCalled());
  },
};
