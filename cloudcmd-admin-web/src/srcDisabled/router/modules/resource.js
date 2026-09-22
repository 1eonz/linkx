import Layout from '@/layout'
import menuBarCn from '@/locales/lang/cn'
import menuBarEn from '@/locales/lang/en'
import AppMain from '@/layout/components/AppMain.vue'
const language = localStorage.getItem('localLanguage')
let language_type
if (language === 'en') {
  language_type = menuBarEn.message.index
} else {
  language_type = menuBarCn.message.index
}

const resourceRouter = {
  path: '/resource',
  component: Layout,
  alwaysShow: true,
  name: 'Resource',
  meta: {
    title: language_type.menuBar.resource,
    icon: 'international',
  },
  children: [
    // {
    //   path: 'pdt',
    //   component: () => import('@/views/resource/pdt'),
    //   name: 'PDT',
    //   meta: {
    //     title: language_type.menuBar.pdt,
    //   },
    // },
    {
      path: 'device',
      component: () => import('@/views/resource/device'),
      name: 'Device',
      meta: {
        title: language_type.menuBar.device,
      },
    },
    {
      path: 'recorder',
      component: () => import('@/views/resource/recorder'),
      name: 'Recorder',
      meta: {
        title: language_type.menuBar.recorder,
      },
    },
    // {
    //   path: 'ballCamera',
    //   component: () => import('@/views/resource/ballCamera'),
    //   name: 'BallCamera',
    //   meta: {
    //     title: language_type.menuBar.ballCamera,
    //   },
    // },
    // {
    //   path: 'tripartite',
    //   component: AppMain,
    //   // standard recorder
    //   // alwaysShow: true,
    //   name: 'Tripartite',
    //   meta: {
    //     title: language_type.menuBar.tripartiteAccessDevices,
    //   },
    //   children: [
    //     {
    //       path: 'standardRecorder',
    //       component: () => import('@/views/resource/tripartite/tripartite'),
    //       name: 'TripartiteRecorder',
    //       meta: {
    //         title: language_type.menuBar.tripartite,
    //       },
    //     },
    //     {
    //       path: 'uav',
    //       component: () => import('@/views/resource/tripartite/uav'),
    //       name: 'Uav',
    //       meta: {
    //         title: language_type.menuBar.uav,
    //       },
    //     },
    //     {
    //       path: 'conferenceTerminal',
    //       component: () =>
    //         import('@/views/resource/tripartite/conferenceTerminal'),
    //       name: 'ConferenceTerminal',
    //       meta: {
    //         title: language_type.menuBar.conferenceTerminal,
    //       },
    //     },
    //     {
    //       path: 'southbound',
    //       component: () => import('@/views/resource/tripartite/southbound'),
    //       name: 'Southbound',
    //       meta: { title: language_type.menuBar.southbound },
    //     },
    //   ],
    // },
    // {
    //   path: 'car',
    //   component: () => import('@/views/resource/car'),
    //   name: 'Car',
    //   meta: {
    //     title: language_type.menuBar.car,
    //   },
    // },
    // {
    //   path: 'gateway',
    //   component: () => import('@/views/resource/gateway'),
    //   name: 'Gateway',
    //   meta: {
    //     title: language_type.menuBar.gateway,
    //   },
    // },
    // {
    //   path: 'vehicle',
    //   component: () => import('@/views/resource/vehicle'),
    //   name: 'Vehicle',
    //   meta: {
    //     title: language_type.menuBar.vehicle,
    //   },
    // },
    {
      path: 'seat',
      component: () => import('@/views/resource/seat'),
      name: 'Seat',
      meta: { title: language_type.menuBar.seat },
    },
    {
      path: 'executorEquipment',
      component: () => import('@/views/resource/executorEquipment'),
      name: 'ExecutorEquipment',
      meta: {
        title: language_type.menuBar.executorEquipment,
      },
    },
    {
      path: 'camera',
      component: () => import('@/views/resource/camera'),
      name: 'Camera',
      meta: {
        title: language_type.menuBar.camera,
      },
    },
    {
      path: 'cameraCatalog',
      component: () => import('@/views/resource/cameraCatalog'),
      name: 'CameraCatalog',
      meta: {
        title: language_type.menuBar.cameraCatalog,
      },
    },
    // {
    //   path: 'layer',
    //   component: () => import('@/views/resource/layer'),
    //   name: 'Layer',
    //   meta: {
    //     title: language_type.menuBar.layer
    //   }
    // },
    // {
    //   path: 'threeLinesIcon',
    //   component: () => import('@/views/resource/threeLines/threeLinesIcon'),
    //   name: 'ThreeLinesIcon',
    //   meta: {
    //     title: language_type.menuBar.threeIcon,
    //   },
    // },
    // {
    //   path: 'threeLinesLayer',
    //   component: () => import('@/views/resource/threeLines/threeLinesLayer'),
    //   name: 'ThreeLinesLayer',
    //   meta: {
    //     title: language_type.menuBar.threeLayer,
    //   },
    // },
    // {
    //   path: 'threeLinesPoint',
    //   component: () => import('@/views/resource/threeLines/threeLinesPoint'),
    //   name: 'ThreeLinesPoint',
    //   meta: {
    //     title: language_type.menuBar.threePoint,
    //   },
    // // },
    // {
    //   path: 'vehicleIllusion',
    //   component: () => import('@/views/resource/vehicleIllusion'),
    //   name: 'VehicleIllusion',
    //   meta: {
    //     title: language_type.menuBar.vehicleIllusion,
    //   },
    // },
    // {
    //   path: 'sandDraw',
    //   component: () => import('@/views/resource/sandDraw'),
    //   name: 'SandDraw',
    //   meta: {
    //     title: language_type.menuBar.sandDraw,
    //   },
    // },
    // {
    //   path: 'landmarkIcon',
    //   component: () => import('@/views/resource/landmark'),
    //   name: 'landmarkIcon',
    //   meta: {
    //     title: language_type.menuBar.landmark,
    //   },
    // },
  ],
}

export default resourceRouter
