import type { InjectionKey } from 'vue';

export interface RadioProps {
  disabled?: boolean;
  hideLabel?: boolean;
  label: string;
  modelValue?: string;
  name?: string;
}

export interface RadioGroupProps {
  modelValue: string;
  name?: string;
}

export interface RadioGroupContext extends RadioGroupProps {
  changeEvent: (val: string) => void;
}

export const radioGroupKey: InjectionKey<RadioGroupContext> = Symbol('radioGroupKey');
