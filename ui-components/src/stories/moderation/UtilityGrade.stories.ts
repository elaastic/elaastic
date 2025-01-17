import {expect, fn, userEvent, waitFor} from '@storybook/test';
import type {Meta, StoryObj} from '@storybook/vue3';

import UtilityGrade from '@/components/moderation/UtilityGrade.vue';

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'Moderation/UtilityGrade',
  component: UtilityGrade,
  args: {
    isChatGPT: false,
    isTeacher: false,
    // Has to have the name of the event with `on` in front and in camelCase
    onSubmitUtilityGrade: fn(),
  },
  tags: ['autodocs'],
  parameters: {
    docs: {
      description: {
        story: 'Change this description'
      }
    }
  }
} satisfies Meta<typeof UtilityGrade>;

export default meta;
type Story = StoryObj<typeof meta>;


export const DraxoByLearner: Story = {
  parameters: {
    docs: {
      description: {
        story: 'A student can grade the utility of a peer grading (made with DRAXO) of their response'
      }
    }
  }
};

export const DraxoByLearnerWithSelectedGrade: Story = {
  args: {
    selectedGrade: "AGREE"
  },
  play: async ({canvas, step}: { canvas: any, step: any }) => {
    const findSubmitBtn = () => {
      const buttons = canvas.queryAllByRole('button')
      if (buttons.length < 4) {
        throw new Error('The number of button have changed. Please update the test');
      }
      // The submit button is the last button
      return buttons[4];
    };

    // Click on any button except the selected one
    await step('Select the first button', async () => {
      await userEvent.click(canvas.getAllByRole('button')[0]);
    });

    const submitBtn = findSubmitBtn();

    // We check if the submit button is visible
    await waitFor(() => expect(submitBtn).toBeInTheDocument());

    // Click on the selected grade.
    // As we set the selected grade to be "AGREE", we click on the second button
    await step('Select the selected grade', async () => {
      await userEvent.click(canvas.getAllByRole('button')[2]);
    });

    //We check if the submit button is invisible, because the grade selected is the same as at the beginning.
    await waitFor(() => expect(submitBtn).not.toBeInTheDocument());
  },
  parameters: {
    docs: {
      description: {
        story: 'A student can grade the utility of a peer grading (made with DRAXO) of their response. Here the student has already selected a grade'
      }
    }
  }
};

export const ChatGPTByLearner: Story = {
  args: {
    isChatGPT: true
  },
  parameters: {
    docs: {
      description: {
        story: 'A student can grade the utility of a peer grading (made by ChatGPT) of their response'
      }
    }
  }
}

export const ChatGPTByTeacher: Story = {
  args: {
    isChatGPT: true,
    isTeacher: true
  },
  parameters: {
    docs: {
      description: {
        story: 'The teacher can grade the utility of a peer grading (made by ChatGPT)'
      }
    }
  }
}
