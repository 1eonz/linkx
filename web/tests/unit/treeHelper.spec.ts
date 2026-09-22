import { describe, it, expect } from 'vitest';
import { getAllIds } from '@/utils/treeHelper';

describe('treeHelper - getAllIds', () => {
  it('传入空对象 {} 应返回空数组', () => {
    expect(getAllIds({})).toEqual([]);
  });

  it('传入单个对象（无 children）应返回只含自身 id 的数组', () => {
    expect(getAllIds({ id: 1 })).toEqual([1]);
  });

  it('传入单对象带一层 children 应返回自身与子节点 id', () => {
    expect(getAllIds({ id: 1, children: [{ id: 2 }] })).toEqual([1, 2]);
  });

  it('传入数组应递归收集所有节点 id', () => {
    expect(
      getAllIds([
        { id: 1, children: [{ id: 2 }] },
        { id: 3 },
      ]),
    ).toEqual([1, 2, 3]);
  });

  it('传入深层嵌套对象应递归到最深层 id', () => {
    expect(
      getAllIds({
        id: 1,
        children: [
          {
            id: 2,
            children: [{ id: 3, children: [{ id: 4 }] }],
          },
        ],
      }),
    ).toEqual([1, 2, 3, 4]);
  });

  it('传入无 id 字段的对象应返回空数组', () => {
    expect(getAllIds({ name: 'a' })).toEqual([]);
  });

  it('传入空数组应返回空数组', () => {
    expect(getAllIds([])).toEqual([]);
  });

  it('传入 null 应返回空数组（源码对非对象/非数组直接返回 ids）', () => {
    expect(getAllIds(null)).toEqual([]);
  });

  it('传入 undefined 应返回空数组', () => {
    expect(getAllIds(undefined)).toEqual([]);
  });

  it('节点无 id 但有 children 时应继续向下递归子节点 id', () => {
    expect(
      getAllIds({
        name: 'root',
        children: [{ id: 5 }, { id: 6 }],
      }),
    ).toEqual([5, 6]);
  });

  it('children 非数组时应跳过子节点遍历，仅返回顶层 id', () => {
    expect(
      getAllIds({
        id: 1,
        children: { id: 2 },
      }),
    ).toEqual([1]);
  });

  it('应支持字符串类型的 id', () => {
    expect(getAllIds({ id: 'a', children: [{ id: 'b' }] })).toEqual(['a', 'b']);
  });
});
