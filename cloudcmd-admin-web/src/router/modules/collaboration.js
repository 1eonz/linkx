import Layout from '@/layout'

const collaborationRouter = {
  path: '/collaboration',
  component: Layout,
  alwaysShow: true,
  name: 'Collaboration',
  meta: {
    title: '协同岗管理',
    icon: 'chart'
  },
  children: [
    {
      path: 'index',
      component: () => import('@/views/h5/collaboration'),
      name: 'Collaboration',
      meta: { title: '协同岗管理' }
    },
    // {
    //   path: 'share',
    //   component: () => import('@/views/h5/collaborationShare'),
    //   name: 'CollaborationShare',
    //   meta: { title: '协同岗分享' }
    // },
    {
      path: 'quick',
      component: () => import('@/views/h5/quick'),
      name: 'Quick',
      meta: { title: '标签管理' }
    }
  ]
}

export default collaborationRouter
