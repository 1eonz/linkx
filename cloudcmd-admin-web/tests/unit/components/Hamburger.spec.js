import { vi, describe, it, expect } from 'vitest';
import { shallowMount } from '@vue/test-utils';
import Hamburger from '@/components/Hamburger/index.vue';

describe('Hamburger.vue', () => {
  it('toggle click 应触发 toggleClick 事件', () => {
    const wrapper = shallowMount(Hamburger);
    const mockFn = vi.fn();
    wrapper.vm.$on('toggleClick', mockFn);
    wrapper.find('.hamburger').trigger('click');
    expect(mockFn).toBeCalled();
  });

  it('prop isActive 控制 is-active class', () => {
    const wrapper = shallowMount(Hamburger);
    wrapper.setProps({ isActive: true });
    expect(wrapper.contains('.is-active')).toBe(true);
    wrapper.setProps({ isActive: false });
    expect(wrapper.contains('.is-active')).toBe(false);
  });
});
