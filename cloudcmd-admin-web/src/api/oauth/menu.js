import request from '@/utils/request'

/**
 * Mock数据：menu.md中的接口返回结构
 * TODO: 接口调试完毕后删除此mock，恢复真实请求
 */
const MOCK_MENU_DATA = {
  code: 0,
  msg: '操作成功',
  data: [
    {
      id: '1394906601265037312',
      name: '首页',
      parentId: '-1',
      applicationId: '1289822833455460000',
      url: 'Dashboard',
      imgurl: '',
      isleaf: 1,
      level: 1,
      sort: 2,
      status: 1,
      gmtCreated: '2021-05-19 14:43:44',
      gmtModified: '2021-07-23 14:42:21',
      children: null
    },
    {
      id: '1394908784123445248',
      name: '系统配置',
      parentId: '-1',
      applicationId: '1289822833455460000',
      url: '/baseData',
      imgurl: '',
      isleaf: 1,
      level: 1,
      sort: 6,
      status: 1,
      gmtCreated: '2021-05-19 14:52:24',
      gmtModified: '2021-07-23 14:42:51',
      children: [
        {
          id: '1394908894421057536',
          name: '字典配置',
          parentId: '1394908784123445248',
          applicationId: '1289822833455460000',
          url: 'dictionary',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 1,
          status: 1,
          gmtCreated: '2021-05-19 14:52:51',
          gmtModified: '2021-07-23 14:43:10',
          children: null
        },
        {
          id: '1394909007927312384',
          name: '全局参数配置',
          parentId: '1394908784123445248',
          applicationId: '1289822833455460000',
          url: 'globals',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 2,
          status: 1,
          gmtCreated: '2021-05-19 14:53:18',
          gmtModified: '2021-07-23 14:43:03',
          children: null
        },
        {
          id: '1522392406668869813',
          name: '三方接入管理11',
          parentId: '1394908784123445248',
          applicationId: '1289822833455460000',
          url: 'southBoundAccount',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 3,
          status: 0,
          gmtCreated: '2025-11-21 02:29:58',
          gmtModified: '2026-04-15 16:47:10',
          children: null
        }
      ]
    },
    {
      id: '1522392406668869809',
      name: '位置管理',
      parentId: '-1',
      applicationId: '1289822833455460000',
      url: '/location',
      imgurl: '',
      isleaf: 1,
      level: 1,
      sort: 6,
      status: 0,
      gmtCreated: '2025-11-21 02:29:58',
      gmtModified: '2025-11-21 02:29:58',
      children: [
        {
          id: '1522392406668869810',
          name: '位置信息',
          parentId: '1522392406668869809',
          applicationId: '1289822833455460000',
          url: 'location',
          imgurl: '',
          isleaf: 1,
          level: 1,
          sort: 4,
          status: 0,
          gmtCreated: '2025-11-21 02:29:58',
          gmtModified: '2025-11-21 02:29:58',
          children: null
        }
      ]
    },
    {
      id: '1522392406668870004',
      name: '三方警单',
      parentId: '-1',
      applicationId: '1289822833455460000',
      url: '/policeReport',
      imgurl: '',
      isleaf: 1,
      level: 1,
      sort: 6,
      status: 1,
      gmtCreated: '2025-09-20 14:50:42',
      gmtModified: '2025-09-20 14:50:42',
      children: [
        {
          id: '1522392406668870005',
          name: '警单对接',
          parentId: '1522392406668870004',
          applicationId: '1289822833455460000',
          url: '/dock',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 1,
          status: 1,
          gmtCreated: '2025-11-21 02:29:59',
          gmtModified: '2025-11-21 02:29:59',
          children: null
        },
        {
          id: '1522392406668870006',
          name: '警单管理',
          parentId: '1522392406668870004',
          applicationId: '1289822833455460000',
          url: '/manage',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 2,
          status: 1,
          gmtCreated: '2025-11-21 02:29:59',
          gmtModified: '2025-11-21 02:29:59',
          children: null
        },
        {
          id: '1522392406668870007',
          name: '类型管理',
          parentId: '1522392406668870004',
          applicationId: '1289822833455460000',
          url: '/typeManage',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 3,
          status: 1,
          gmtCreated: '2025-11-21 02:29:59',
          gmtModified: '2025-11-21 02:29:59',
          children: null
        }
      ]
    },
    {
      id: '1522392406668869811',
      name: '权限中心',
      parentId: '-1',
      applicationId: '1289822833455460000',
      url: '/authority',
      imgurl: '',
      isleaf: 1,
      level: 1,
      sort: 7,
      status: 1,
      gmtCreated: '2025-11-21 02:29:58',
      gmtModified: '2025-11-21 02:29:58',
      children: [
        {
          id: '1394908359697629184',
          name: '角色管理',
          parentId: '1522392406668869811',
          applicationId: '1289822833455460000',
          url: 'role',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 2,
          status: 1,
          gmtCreated: '2021-05-19 14:50:43',
          gmtModified: '2021-07-23 14:41:41',
          children: null
        },
        {
          id: '1522392406668869812',
          name: '权限管理',
          parentId: '1522392406668869811',
          applicationId: '1289822833455460000',
          url: 'person',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 2,
          status: 1,
          gmtCreated: '2025-11-21 02:29:58',
          gmtModified: '2025-11-21 02:29:58',
          children: null
        }
      ]
    },
    {
      id: '1522392406668869701',
      name: '看板配置',
      parentId: '-1',
      applicationId: '1289822833455460000',
      url: '/board',
      imgurl: '',
      isleaf: 1,
      level: 1,
      sort: 12,
      status: 1,
      gmtCreated: '2025-11-21 02:29:55',
      gmtModified: '2025-11-21 02:29:55',
      children: [
        {
          id: '1522392406668869702',
          name: '图表管理',
          parentId: '1522392406668869701',
          applicationId: '1289822833455460000',
          url: 'chart',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 1,
          status: 1,
          gmtCreated: '2025-11-21 02:29:55',
          gmtModified: '2025-11-21 02:29:55',
          children: null
        },
        {
          id: '1522392406668869703',
          name: '看板管理',
          parentId: '1522392406668869701',
          applicationId: '1289822833455460000',
          url: 'layout',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 2,
          status: 1,
          gmtCreated: '2025-11-21 02:29:55',
          gmtModified: '2025-11-21 02:29:55',
          children: null
        },
        {
          id: '1522392406668869704',
          name: '图表创建',
          parentId: '1522392406668869701',
          applicationId: '1289822833455460000',
          url: 'create',
          imgurl: '',
          isleaf: 1,
          level: 2,
          sort: 3,
          status: 1,
          gmtCreated: '2025-11-21 02:29:55',
          gmtModified: '2025-11-21 02:29:55',
          children: null
        }
      ]
    },
    {
      id: '1522392406668869804',
      name: 'H5管理',
      parentId: '-1',
      applicationId: '1289822833455460000',
      url: '/h5',
      imgurl: '',
      isleaf: 1,
      level: 1,
      sort: 13,
      status: 0,
      gmtCreated: '2025-11-21 02:29:58',
      gmtModified: '2025-11-21 02:29:58',
      children: [
        {
          id: '1522392406668869805',
          name: '应用管理',
          parentId: '1522392406668869804',
          applicationId: '1289822833455460000',
          url: 'app',
          imgurl: '',
          isleaf: 1,
          level: 1,
          sort: 1,
          status: 0,
          gmtCreated: '2025-11-21 02:29:58',
          gmtModified: '2025-11-21 02:29:58',
          children: null
        },
        {
          id: '1522392406668869806',
          name: '轮播图管理',
          parentId: '1522392406668869804',
          applicationId: '1289822833455460000',
          url: 'carousel',
          imgurl: '',
          isleaf: 1,
          level: 1,
          sort: 2,
          status: 0,
          gmtCreated: '2025-11-21 02:29:58',
          gmtModified: '2025-11-21 02:29:58',
          children: null
        },
        {
          id: '1522392406668869807',
          name: '协同岗管理',
          parentId: '1522392406668869804',
          applicationId: '1289822833455460000',
          url: 'collaboration',
          imgurl: '',
          isleaf: 1,
          level: 1,
          sort: 3,
          status: 0,
          gmtCreated: '2025-11-21 02:29:58',
          gmtModified: '2025-11-21 02:29:58',
          children: null
        },
        {
          id: '1522392406668869808',
          name: '标签管理',
          parentId: '1522392406668869804',
          applicationId: '1289822833455460000',
          url: 'quick',
          imgurl: '',
          isleaf: 1,
          level: 1,
          sort: 4,
          status: 0,
          gmtCreated: '2025-11-21 02:29:58',
          gmtModified: '2025-11-21 02:29:58',
          children: null
        },
      ]
    }
  ]
}

/**
 * 获取当前用户可见菜单列表（oauth接口）
 * GET linkx/oauth/v1/menu/list
 *
 * 当前使用mock数据（menu.md），接口可用后切换回真实请求
 */
export function getOauthMenuList() {
  // TODO: 接口可用后，删除mock分支，恢复下方真实请求
  // return request({
  //   url: '/linkx/oauth/v1/menu/list',
  //   method: 'get'
  // })
  return Promise.resolve(MOCK_MENU_DATA)
}
