import { vi, describe, it, expect } from 'vitest';
import { shallowMount } from '@vue/test-utils';
import SvgIcon from '@/components/SvgIcon/index.vue';

describe('SvgIcon.vue', () => {
  it('iconClass 应渲染 use 标签的 href', () => {
    const wrapper = shallowMount(SvgIcon, {
      propsData: {
        iconClass: 'test',
      },
    });
    expect(wrapper.find('use').attributes().href).toBe('#icon-test');
  });

  it('className 应添加自定义 class', () => {
    const wrapper = shallowMount(SvgIcon, {
      propsData: {
        iconClass: 'test',
      },
    });
    expect(wrapper.classes().length).toBe(1);
    wrapper.setProps({ className: 'test' });
    expect(wrapper.classes().includes('test')).toBe(true);
  });
});
