import { describe, it, expect, vi, beforeEach } from 'vitest';

// 顶部 mock Layout，避免加载真实 .vue 组件
vi.mock('@/layout', () => ({
  default: { name: 'Layout' },
}));

import { buildRoutesFromOauthMenu, printMatchedInfo } from '@/utils/menuRouteMapper';

describe('Utils:menuRouteMapper - buildRoutesFromOauthMenu', () => {
  beforeEach(() => {
    vi.spyOn(console, 'warn').mockImplementation(() => {});
  });

  describe('入参校验', () => {
    it('非数组入参应返回空结构并触发 warn', () => {
      const result = buildRoutesFromOauthMenu(null);
      expect(result.routes).toEqual([]);
      expect(result.matchedInfo).toEqual({
        matched: [],
        placeholder: [],
        disabled: [],
        routeOnly: [],
      });
      expect(console.warn).toHaveBeenCalled();
    });

    it('undefined 入参应返回空结构', () => {
      const result = buildRoutesFromOauthMenu(undefined);
      expect(result.routes).toEqual([]);
      expect(result.matchedInfo.matched).toEqual([]);
    });

    it('对象入参应返回空结构', () => {
      const result = buildRoutesFromOauthMenu({ foo: 'bar' });
      expect(result.routes).toEqual([]);
    });

    it('空数组应返回空 routes（routeOnly 含全部映射表项）', () => {
      const result = buildRoutesFromOauthMenu([]);
      expect(result.routes).toEqual([]);
      // routeOnly 应包含映射表中所有 url（接口未返回的）
      expect(result.matchedInfo.routeOnly.length).toBeGreaterThan(0);
    });
  });

  describe('status=0 菜单过滤', () => {
    it('一级菜单 status=0 应被过滤并记录到 disabled', () => {
      const menuData = [
        {
          id: 1,
          name: '禁用菜单',
          url: '/disabled',
          status: 0,
          children: [
            { id: 11, name: '子菜单', url: 'child', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(0);
      expect(result.matchedInfo.disabled.length).toBe(1);
      expect(result.matchedInfo.disabled[0].name).toBe('禁用菜单');
      expect(result.matchedInfo.disabled[0].reason).toBe('一级菜单 status=0');
    });

    it('二级菜单 status=0 应被过滤并记录到 disabled', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '禁用子菜单', url: 'carousel', status: 0, sort: 1 },
            { id: 12, name: '启用子菜单', url: 'GroupTags', status: 1, sort: 2 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(1);
      expect(result.routes[0].children.length).toBe(1);
      expect(result.routes[0].children[0].name).toBe('GroupTags');
      expect(result.matchedInfo.disabled.length).toBe(1);
      expect(result.matchedInfo.disabled[0].name).toBe('禁用子菜单');
      expect(result.matchedInfo.disabled[0].reason).toBe('二级菜单 status=0');
      expect(result.matchedInfo.disabled[0].parentName).toBe('父菜单');
    });
  });

  describe('二级菜单按 sort 排序', () => {
    it('应按 sort 升序排列子菜单', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 12, name: '菜单B', url: 'GroupTags', status: 1, sort: 2 },
            { id: 11, name: '菜单A', url: 'carousel', status: 1, sort: 1 },
            { id: 13, name: '菜单C', url: 'ArchivedTable', status: 1, sort: 3 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes[0].children.length).toBe(3);
      expect(result.routes[0].children[0].name).toBe('carousel');
      expect(result.routes[0].children[1].name).toBe('GroupTags');
      expect(result.routes[0].children[2].name).toBe('ArchivedTable');
    });

    it('sort 缺失应按 0 处理', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 12, name: '菜单B', url: 'GroupTags', status: 1, sort: 5 },
            { id: 11, name: '菜单A', url: 'carousel', status: 1 }, // sort 缺失
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes[0].children[0].name).toBe('carousel'); // sort=0 排在前
      expect(result.routes[0].children[1].name).toBe('GroupTags');
    });

    it('sort 为 0 时应正确参与排序', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 12, name: '菜单B', url: 'GroupTags', status: 1, sort: 2 },
            { id: 11, name: '菜单A', url: 'carousel', status: 1, sort: 0 },
            { id: 13, name: '菜单C', url: 'ArchivedTable', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes[0].children[0].name).toBe('carousel'); // sort=0
      expect(result.routes[0].children[1].name).toBe('ArchivedTable'); // sort=1
      expect(result.routes[0].children[2].name).toBe('GroupTags'); // sort=2
    });
  });

  describe('Dashboard 特殊路径处理', () => {
    it('Dashboard 一级菜单应特殊处理，不生成 Layout 路由', () => {
      const menuData = [
        {
          id: 1,
          name: '首页',
          url: '/Dashboard',
          status: 1,
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(0);
      expect(result.matchedInfo.matched.length).toBe(1);
      expect(result.matchedInfo.matched[0].routePath).toBe('/dashboard');
      expect(result.matchedInfo.matched[0].component).toBe('views/dashboard');
    });

    it('Dashboard url 不带前导斜杠也应识别', () => {
      const menuData = [
        {
          id: 1,
          name: '首页',
          url: 'Dashboard',
          status: 1,
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.matchedInfo.matched.length).toBe(1);
      expect(result.matchedInfo.matched[0].routePath).toBe('/dashboard');
    });
  });

  describe('ROUTE_COMPONENT_MAP 匹配', () => {
    it('命中映射表的 url 应使用真实组件', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '轮播', url: 'carousel', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(1);
      const childRoute = result.routes[0].children[0];
      expect(childRoute.path).toBe('carousel');
      expect(childRoute.name).toBe('carousel');
      expect(typeof childRoute.component).toBe('function');
      expect(childRoute.meta._matched).toBe(true);
      expect(childRoute.meta._placeholder).toBeUndefined();
      expect(childRoute.meta.title).toBe('轮播');
      expect(childRoute.meta._oauthMenuId).toBe(11);

      expect(result.matchedInfo.matched.length).toBe(1);
      expect(result.matchedInfo.matched[0].component).toBe('real');
      expect(result.matchedInfo.matched[0].routePath).toBe('/parent/carousel');
    });

    it('命中映射表的 url 应从 routeOnly 中排除', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '轮播', url: 'carousel', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      const routeOnlyUrls = result.matchedInfo.routeOnly.map((r) => r.url);
      expect(routeOnlyUrls).not.toContain('carousel');
    });

    it('未命中映射表的 url 应使用 placeholder 组件', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '未知页面', url: 'unknownUrl', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(1);
      const childRoute = result.routes[0].children[0];
      expect(childRoute.path).toBe('unknownUrl');
      expect(childRoute.name).toBe('placeholder_unknownUrl');
      expect(typeof childRoute.component).toBe('function');
      expect(childRoute.meta._matched).toBe(false);
      expect(childRoute.meta._placeholder).toBe(true);

      expect(result.matchedInfo.placeholder.length).toBe(1);
      expect(result.matchedInfo.placeholder[0].name).toBe('未知页面');
      expect(result.matchedInfo.placeholder[0].normalizedUrl).toBe('unknownUrl');
      expect(result.matchedInfo.placeholder[0].parentName).toBe('父菜单');
      expect(result.matchedInfo.placeholder[0].routePath).toBe(
        '/parent/unknownUrl',
      );
    });

    it('url 带前导斜杠应先 normalize 再匹配映射表', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '轮播', url: '/carousel', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes[0].children[0].meta._matched).toBe(true);
    });

    it('子菜单 url 为空字符串时应走 placeholder 分支', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '空url菜单', url: '', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      // normalizeUrl('') 返回 ''，'' 不在 ROUTE_COMPONENT_MAP 中，走 placeholder
      expect(result.routes[0].children[0].name).toBe('placeholder_');
      expect(result.routes[0].children[0].meta._placeholder).toBe(true);
      expect(result.matchedInfo.placeholder[0].normalizedUrl).toBe('');
    });

    it('子菜单 url 为 null 时应走 placeholder 分支', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: 'null url菜单', url: null, status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      // normalizeUrl(null) 返回 ''，走 placeholder
      expect(result.routes[0].children[0].name).toBe('placeholder_');
    });
  });

  describe('children 为空时不生成父路由', () => {
    it('无 children 属性应不生成路由', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(0);
    });

    it('children 为空数组应不生成路由', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(0);
    });

    it('children 全部被 status=0 过滤后应不生成父路由', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '禁用1', url: 'carousel', status: 0, sort: 1 },
            { id: 12, name: '禁用2', url: 'GroupTags', status: 0, sort: 2 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes.length).toBe(0);
      expect(result.matchedInfo.disabled.length).toBe(2);
    });
  });

  describe('父路由结构', () => {
    it('应正确构建父路由的 path、component、meta', () => {
      const menuData = [
        {
          id: 100,
          name: '父菜单',
          url: '/parent',
          imgurl: 'parent-icon',
          status: 1,
          children: [
            { id: 11, name: '轮播', url: 'carousel', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      const route = result.routes[0];
      expect(route.path).toBe('/parent');
      expect(route.component).toEqual({ name: 'Layout' });
      expect(route.alwaysShow).toBe(true);
      expect(route.name).toBe('parent'); // normalizeUrl('/parent') = 'parent'
      expect(route.meta.title).toBe('父菜单');
      expect(route.meta.icon).toBe('parent-icon');
      expect(route.meta._oauthMenuId).toBe(100);
    });

    it('imgurl 缺失时 icon 应默认为 "component"', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '轮播', url: 'carousel', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      expect(result.routes[0].meta.icon).toBe('component');
    });
  });

  describe('routeOnly 统计', () => {
    it('应收集映射表中存在但接口未返回的 url', () => {
      const menuData = [
        {
          id: 1,
          name: '父菜单',
          url: '/parent',
          status: 1,
          children: [
            { id: 11, name: '轮播', url: 'carousel', status: 1, sort: 1 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);
      // carousel 命中，应不在 routeOnly
      const routeOnlyUrls = result.matchedInfo.routeOnly.map((r) => r.url);
      expect(routeOnlyUrls).not.toContain('carousel');
      // 但其他映射表中的项应在 routeOnly 中
      expect(routeOnlyUrls).toContain('GroupTags');
      expect(routeOnlyUrls).toContain('ArchivedTable');
      expect(
        result.matchedInfo.routeOnly[0].reason,
      ).toBe('路由映射表中存在，但接口未返回此url');
    });
  });

  describe('综合场景', () => {
    it('应正确处理包含多种情况的菜单数据', () => {
      const menuData = [
        // Dashboard 首页
        { id: 0, name: '首页', url: '/Dashboard', status: 1 },
        // 禁用的一级菜单
        {
          id: 99,
          name: '禁用模块',
          url: '/disabled',
          status: 0,
          children: [{ id: 991, name: '子', url: 'carousel', status: 1, sort: 1 }],
        },
        // 正常父菜单 + 混合 children
        {
          id: 1,
          name: 'H5管理',
          url: '/h5',
          status: 1,
          children: [
            { id: 11, name: '轮播图', url: 'carousel', status: 1, sort: 2 },
            { id: 12, name: '分组标签', url: 'GroupTags', status: 1, sort: 1 },
            { id: 13, name: '归档表', url: 'ArchivedTable', status: 1, sort: 3 },
            { id: 14, name: '禁用项', url: 'collaboration', status: 0, sort: 4 },
            { id: 15, name: '未知页', url: 'unknownH5', status: 1, sort: 5 },
          ],
        },
      ];
      const result = buildRoutesFromOauthMenu(menuData);

      // 只有一个父路由生成（H5管理）
      expect(result.routes.length).toBe(1);
      expect(result.routes[0].path).toBe('/h5');

      // children 应按 sort 排序，且过滤掉 status=0
      // 未命中 ROUTE_COMPONENT_MAP 的组件名会加 placeholder_ 前缀
      const childNames = result.routes[0].children.map((c) => c.name);
      expect(childNames).toEqual(['GroupTags', 'carousel', 'ArchivedTable', 'placeholder_unknownH5']);

      // matched: Dashboard + 3 个真实组件
      expect(result.matchedInfo.matched.length).toBe(4);
      // placeholder: 1 个未知页
      expect(result.matchedInfo.placeholder.length).toBe(1);
      // disabled: 1 个一级 + 1 个二级
      expect(result.matchedInfo.disabled.length).toBe(2);
    });
  });
});

describe('Utils:menuRouteMapper - printMatchedInfo', () => {
  let groupSpy;
  let groupEndSpy;
  let tableSpy;
  let logSpy;

  beforeEach(() => {
    groupSpy = vi.spyOn(console, 'group').mockImplementation(() => {});
    groupEndSpy = vi.spyOn(console, 'groupEnd').mockImplementation(() => {});
    tableSpy = vi.spyOn(console, 'table').mockImplementation(() => {});
    logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
  });

  function createMatchedInfo({
    matched = [],
    placeholder = [],
    disabled = [],
    routeOnly = [],
  } = {}) {
    return { matched, placeholder, disabled, routeOnly };
  }

  it('应调用 console.group 5 次（外层 + 4 个分组）', () => {
    printMatchedInfo(createMatchedInfo());
    // 外层 1 + 4 个子分组 = 5
    expect(groupSpy).toHaveBeenCalledTimes(5);
  });

  it('应调用 console.groupEnd 5 次（外层 + 4 个分组）', () => {
    printMatchedInfo(createMatchedInfo());
    expect(groupEndSpy).toHaveBeenCalledTimes(5);
  });

  it('外层 group 应包含 "[OAuth菜单映射结果]" 标识', () => {
    printMatchedInfo(createMatchedInfo());
    expect(groupSpy).toHaveBeenCalledWith(
      '%c[OAuth菜单映射结果]',
      'color: #409EFF; font-weight: bold;',
    );
  });

  it('matched 非空时应调用 console.table', () => {
    const matched = [{ name: '轮播', url: 'carousel', routePath: '/h5/carousel' }];
    printMatchedInfo(createMatchedInfo({ matched }));
    expect(tableSpy).toHaveBeenCalledWith(matched);
  });

  it('matched 为空时也应调用 console.table（传入空数组）', () => {
    printMatchedInfo(createMatchedInfo({ matched: [] }));
    expect(tableSpy).toHaveBeenCalledWith([]);
  });

  it('placeholder 非空时应调用 console.table', () => {
    const placeholder = [
      { name: '未知', url: 'unknown', normalizedUrl: 'unknown' },
    ];
    printMatchedInfo(createMatchedInfo({ placeholder }));
    expect(tableSpy).toHaveBeenCalledWith(placeholder);
  });

  it('placeholder 为空时应调用 console.log("无")', () => {
    printMatchedInfo(createMatchedInfo({ placeholder: [] }));
    expect(logSpy).toHaveBeenCalledWith('无');
  });

  it('routeOnly 非空时应调用 console.table', () => {
    const routeOnly = [{ url: 'carousel', reason: '路由映射表中存在，但接口未返回此url' }];
    printMatchedInfo(createMatchedInfo({ routeOnly }));
    expect(tableSpy).toHaveBeenCalledWith(routeOnly);
  });

  it('routeOnly 为空时应调用 console.log("无")', () => {
    printMatchedInfo(createMatchedInfo({ routeOnly: [] }));
    expect(logSpy).toHaveBeenCalledWith('无');
  });

  it('disabled 非空时应调用 console.table', () => {
    const disabled = [
      { name: '禁用菜单', url: '/disabled', reason: '一级菜单 status=0' },
    ];
    printMatchedInfo(createMatchedInfo({ disabled }));
    expect(tableSpy).toHaveBeenCalledWith(disabled);
  });

  it('disabled 为空时应调用 console.log("无")', () => {
    printMatchedInfo(createMatchedInfo({ disabled: [] }));
    expect(logSpy).toHaveBeenCalledWith('无');
  });

  it('全部为空时 console.log 应被调用 3 次（placeholder/routeOnly/disabled 各一次"无"）', () => {
    printMatchedInfo(createMatchedInfo());
    expect(logSpy).toHaveBeenCalledTimes(3);
    expect(logSpy).toHaveBeenNthCalledWith(1, '无');
    expect(logSpy).toHaveBeenNthCalledWith(2, '无');
    expect(logSpy).toHaveBeenNthCalledWith(3, '无');
  });

  it('全部非空时 console.log 应被调用 0 次', () => {
    printMatchedInfo(
      createMatchedInfo({
        matched: [{ name: 'a' }],
        placeholder: [{ name: 'b' }],
        routeOnly: [{ url: 'c' }],
        disabled: [{ name: 'd' }],
      }),
    );
    expect(logSpy).not.toHaveBeenCalled();
  });

  it('应正确处理完整数据（综合场景）', () => {
    const matchedInfo = createMatchedInfo({
      matched: [{ name: '轮播', url: 'carousel' }],
      placeholder: [{ name: '未知', url: 'unknown' }],
      routeOnly: [{ url: 'GroupTags' }],
      disabled: [{ name: '禁用', url: '/disabled' }],
    });
    printMatchedInfo(matchedInfo);
    // 4 个分组都应调用 console.table
    expect(tableSpy).toHaveBeenCalledTimes(4);
    // 不应调用 console.log（因为都不为空）
    expect(logSpy).not.toHaveBeenCalled();
  });
});
