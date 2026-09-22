import Layout from '@/layout'

const nodeManageRouter = {
  path: '/nodeManage',
  component: Layout,
  alwaysShow: true,
  name: 'nodeManage',
  meta: {
    title: '多节点管理',
    icon: 'tree'
  },
  children: [
    {
      path: 'nodeManagement',
      component: () => import('@/views/nodeManage/nodeManagement'),
      name: 'nodeManagement',
      meta: { title: '节点管理' }
    },
    {
      path: 'dataManage',
      component: () => import('@/views/nodeManage/dataManage'),
      name: 'dataManage',
      meta: { title: '数据管理' }
    }
  ]
}

export default nodeManageRouter
