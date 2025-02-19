import type {Meta, StoryObj} from '@storybook/vue3';
import type {Selection} from "@/components/util/SelectorResponsive.vue";

import SelectorResponsive from "@/components/util/SelectorResponsive.vue";
import {expect, fn, userEvent, waitFor, within} from "@storybook/test";

// Define the interface for args
interface Args {
  onChangeSelection: () => void;
  selections: Selection[];
}

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Util/SelectorResponsive',
  component: SelectorResponsive,
  args: {
    onChangeSelection: fn(),
  },
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        story: 'TODO'
      }
    }
  }
} satisfies Meta<typeof SelectorResponsive>;

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
  parameters: {
    docs: {
      description: {
        story: 'Default state'
      }
    }
  }
};

export const PrimaryClick: Story = {
  args: {
    selections: [
      {label: "First", value: "1"},
      {label: "Second", value: "2"},
      {label: "Last", value: "N"}
    ],
  },
  play: async ({args, canvas, step}: {args: Args, canvas: any, step: any}) => {
    // Click the first button
    await step('Select the first button', async () => {
      await userEvent.click(canvas.getAllByRole('button')[0]);
    });

    // Now we can assert that the onChangeSelection arg was called
    await waitFor(() => expect(args.onChangeSelection).toHaveBeenCalled());
  },
  parameters: {
    docs: {
      description: {
        story: 'Default state but with a click on the first button'
      }
    }
  }
};

export const WithSelectedValid: Story = {
  args: {
    selections: [
      {label: "First", value: "1"},
      {label: "Second", value: "2"},
      {label: "Last", value: "N"}
    ],
    selected: "N",
  },
  parameters: {
    docs: {
      description: {
        story: 'A selected value is provided'
      }
    }
  }
};

export const WithSelectedInvalid: Story = {
  args: {
    selections: [
      {label: "First", value: "1"},
      {label: "Second", value: "2"},
      {label: "Last", value: "N"}
    ],
    selected: "azertyuiop",
  },
  parameters: {
    docs: {
      description: {
        story: 'An selected value is provided but it is not in the list'
      }
    }
  }
};
