import type {Meta, StoryObj} from '@storybook/vue3';
import DraxoGrid from "@/components/evaluation/draxo/DraxoGrid.vue";
import {OptionType} from "@/components/evaluation/draxo/OptionType";

const getOptionsType = () => {
  return [null, ...Object.values(OptionType)];
}
function getCriteriaControl() {
  return {options: getOptionsType(), control: {type: 'select'}};
}

// More on how to set up stories at: https://storybook.js.org/docs/writing-stories
const meta: any = {
  title: 'evaluation/draxo/DraxoGrid',
  component: DraxoGrid,
  args: {
    criteriaD: null,
    criteriaR: null,
    criteriaA: null,
    criteriaX: null,
    criteriaO: null,
  },
  argTypes: {
    criteriaD: getCriteriaControl(),
    criteriaR: getCriteriaControl(),
    criteriaA: getCriteriaControl(),
    criteriaX: getCriteriaControl(),
    criteriaO: getCriteriaControl(),
  },
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
  },
  args: {
    criteriaD: OptionType.YES,
    criteriaR: OptionType.YES,
    criteriaA: OptionType.YES,
    criteriaX: OptionType.NO,
    criteriaO: null,
  }
};
