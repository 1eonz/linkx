import { vi, describe, it, expect } from 'vitest';
import { validUsername, isExternal } from '@/utils/validate.js';

describe('Utils:validate', () => {
  it('validUsername 应识别 admin 和 editor', () => {
    expect(validUsername('admin')).toBe(true);
    expect(validUsername('editor')).toBe(true);
    expect(validUsername('xxxx')).toBe(false);
  });

  it('isExternal 应识别非外链路径', () => {
    expect(isExternal('/dashboard')).toBe(false);
    expect(isExternal('./dashboard')).toBe(false);
    expect(isExternal('dashboard')).toBe(false);
  });

  it('isExternal 应识别外链', () => {
    expect(isExternal('https://github.com')).toBe(true);
    expect(isExternal('http://example.com')).toBe(true);
    expect(isExternal('mailto:test@example.com')).toBe(true);
    expect(isExternal('tel:13800138000')).toBe(true);
  });
});
