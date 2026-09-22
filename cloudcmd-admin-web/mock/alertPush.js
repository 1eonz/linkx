import Mock from 'mockjs'

// 生成mock数据
const generateMockData = (classify) => {
  const baseData = {
    id: '@id',
    'alertNotifiers|1-3': [
      {
        label: '@cname',
        value: '@integer(1, 100)'
      }
    ],
    createTime: '@datetime'
  }

  if (classify === 'taskOverdue') {
    return Mock.mock({
      ...baseData,
      orgName: '@company',
      parentOrgName: '@company',
      classify: 'taskOverdue'
    })
  } else {
    return Mock.mock({
      ...baseData,
      coordinationPostName: '@cname',
      orgName: '@company',
      classify: 'unattended'
    })
  }
}

// 初始化数据
const taskOverdueData = []
const unattendedData = []

for (let i = 0; i < 10; i++) {
  taskOverdueData.push(generateMockData('taskOverdue'))
  unattendedData.push(generateMockData('unattended'))
}

// 合并所有数据
const allData = [...taskOverdueData, ...unattendedData]

// 组织相关数据
const orgData = Mock.mock({
  'orgList|10': [
    {
      label: '@company',
      value: '@integer(1, 100)',
      parent: `父组织${'@integer(1, 10)'}`
    }
  ],
  'parentOrgList|5': [
    {
      label: `父组织${'@integer(1, 10)'}`,
      value: `parent${'@integer(1, 10)'}`
    }
  ],
  'cooperationPostList|10': [
    {
      label: `协同岗${'@integer(1, 20)'}`,
      value: `post${'@integer(1, 20)'}`,
      org: '@company'
    }
  ],
  'unattendedOrgList|10': [
    {
      label: '@company',
      value: `org${'@integer(1, 100)'}`
    }
  ],
  'notifierList|15': [
    {
      label: '@cname',
      value: '@integer(1, 150)'
    }
  ],
  'notifierTypeList|5': [
    {
      label: '@ctitle(2, 4)',
      value: '@word(3, 8)'
    }
  ]
})

export default [
  // 分页查询预警推送列表
  {
    url: '/vue-admin-template/alertPush/list',
    type: 'get',
    response: config => {
      const { classify, pageNum = 1, pageSize = 10 } = config.query
      const parsedPageSize = parseInt(pageSize, 10)
      const currentPage = parseInt(pageNum, 10)

      // 根据classify筛选数据
      let filteredData = allData
      if (classify) {
        filteredData = allData.filter(item => item.classify === classify)
      }

      // 分页处理
      const start = (currentPage - 1) * parsedPageSize
      const end = start + parsedPageSize
      const items = filteredData.slice(start, end)

      return {
        code: 20000,
        data: {
          total: filteredData.length,
          items: items
        }
      }
    }
  },

  // 新增预警推送记录
  {
    url: '/vue-admin-template/alertPush/create',
    type: 'post',
    response: config => {
      const newItem = {
        ...config.body,
        id: Mock.mock('@id'),
        createTime: new Date().toISOString()
      }

      // 添加到对应的数据列表
      allData.push(newItem)

      return {
        code: 20000,
        message: '新增成功',
        data: newItem
      }
    }
  },

  // 更新预警推送记录
  {
    url: '/vue-admin-template/alertPush/update',
    type: 'put',
    response: config => {
      const { id, ...updateData } = config.body
      const index = allData.findIndex(item => item.id === id)

      if (index !== -1) {
        allData[index] = { ...allData[index], ...updateData }
        return {
          code: 20000,
          message: '更新成功',
          data: allData[index]
        }
      }

      return {
        code: 50000,
        message: '记录不存在'
      }
    }
  },

  // 获取组织列表
  {
    url: '/vue-admin-template/alertPush/orgList',
    type: 'get',
    response: config => {
      return {
        code: 20000,
        data: orgData.orgList
      }
    }
  },

  // 获取父组织列表
  {
    url: '/vue-admin-template/alertPush/parentOrgList',
    type: 'get',
    response: config => {
      return {
        code: 20000,
        data: orgData.parentOrgList
      }
    }
  },

  // 获取协同岗列表
  {
    url: '/vue-admin-template/alertPush/cooperationPostList',
    type: 'get',
    response: config => {
      return {
        code: 20000,
        data: orgData.cooperationPostList
      }
    }
  },

  // 获取无人值守组织列表
  {
    url: '/vue-admin-template/alertPush/unattendedOrgList',
    type: 'get',
    response: config => {
      return {
        code: 20000,
        data: orgData.unattendedOrgList
      }
    }
  },

  // 获取预警通知对象列表
  {
    url: '/vue-admin-template/alertPush/notifierList',
    type: 'get',
    response: config => {
      return {
        code: 20000,
        data: orgData.notifierList
      }
    }
  },
  // 获取预警通知对象类型列表
  {
    url: '/vue-admin-template/alertPush/notifierTypeList',
    type: 'get',
    response: config => {
      return {
        code: 20000,
        data: orgData.notifierTypeList
      }
    }
  }
]