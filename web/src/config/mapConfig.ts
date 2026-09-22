export default {
  accountInfomation: '/passport/accountInfomation',
  accountStatus: '/passport/accountStatus', // 获取通信服务账号登录状态信息
  addCustomCatalogCamera: '/resource/resourcetocontainer/put', // 往自定义摄像头层级中添加摄像头
  addEventOperationRecord: '/event/addEventOperationRecord', // 添加历史修改记录
  addEventTargetByEventId: '/event/addEventTargetByEventId', // 新增干系对象
  // https请求配置
  ApiKey: 'CDC-1000', // Header参数校验值
  appendFacilityToCustomMonitorLevel: '/monitor/monitorToContainer/put', // 向自定义摄像头点层级中添加摄像头点
  batchQueryMonitoringPointInfo: '/monitor/monitorInfo/get', // 批量查询摄像头点信息，带设备
  bindAccount: '/passport/bindUserAccountWithSeat', // 绑定用户与账户关系
  cancelDownloald: '/mission/v1/terminalEvidenceDownload', // 取消证据下载
  completeTask: '/mission/completeTask', // 执行流程节点
  createActionGroup: '/resource/agroup/createAgroup', // 创建行动组
  createCommGroup: '/mission/v1/createCommunicationGroup', // 创建通信群组
  createCustomMonitorLevel: '/monitor/customizeResourceContainer/create', // 创建自定义摄像头点层级
  // 通信组
  createDycGroup: '/acm/group/create', // 创建通信群组
  createDynamicGroup: '/mission/v1/group/create', // 单独创建动态组
  // 任务接口
  createMission: '/mission/createMission', // 创建任务
  createMissionFeedBack: '/mission/createMissionFeedBack', // 创建任务反馈信息
  createMissionSpace: '/mission/createMissionExecuteSpace', // 创建任务执行空间Id
  createMissionUrl: '/mission/v1/createMission', // 创建任务
  createMonitorCatalog: '/resource/customizeResourceContainer/create', // 创建自定义摄像头层级
  creatEvent: '/event/createEvent', // 新建事件
  deleteActionGroup: '/resource/agroup/deleteDynamicAgroup', // 删除行动组
  deleteCustomCatalogCamera: '/resource/resourcetocontainer/delete', // 删除摄像头自定义层级中的摄像头
  deleteCustomMonitorLevel: '/monitor/customizeResourceContainer/delete', // 删除自定义摄像头点层级
  deleteDycGroup: '/acm/group/delete', // 删除群组
  deleteFacilityOfCustomMonitorLevel: '/monitor/monitorToContainer/delete', // 删除自定义摄像头点层级中的摄像头点
  deleteMonitorCatalog: '/resource/resourceContainer/delete', // 删除该登录账户自定义摄像头层级
  doActionGroupSearch: '/resource/agroup/search', // 行动组搜索
  doIndividualSearch: '/resource/individual/search', // 个体搜索
  doResourceSearch: '/resource/search', // 资源搜索
  downloadEvidenceFile: '/evidence/download', // 证据下载
  equipment: '/lbs/equipment', // 根据装备小类查询一定范围内人.车以及摄像头
  equipmentlocation: '/lbs/equipmentlocation', // 根据装备小类通过用户Id过滤所有下级及子集
  equipmentlocationPage: '/lbs/equipmentlocationPage', // 分页根据装备小类通过用户Id过滤所有下级及子集
  eventCategories: '/event/categories', // 事件分类
  eventDetail: '/event/detail', // 事件详情详情
  eventEvidences: '/mission/v1/eventEvidences', // 证据列表
  eventGeometryStatistics: '/jq/mission/geometry', // 一定时间段内的警情总数按辖区进行分类汇总统计
  eventLevelStatistics: '/jq/mission/level', // 一定时间段内的辖区警情数按警情级别进行分类汇总统计
  // 数据统计信息接口
  eventStatusStatistics: '/jq/mission/status', // 一定时间段内的辖区警情数按警情状态进行分类汇总统计
  eventtrendStatistics: '/jq/mission/trend', // 警情趋势
  evidencePercent: '/mission/v1/speedOfdEvidenceFileProgress', // 证据进度条
  // 勤务相关
  fileUploadUrl: '/das/v1/schduleFile/uploadFile',
  fuzzyQueryCamera: '/resource/camera/keywords/get', // 模糊查询摄像头
  geourl: '/egis/api/search?', // 地址搜索
  getAbility: '/config/category/getAbility', // 查询类目对应的能力
  // passport
  getAccountByRes: '/passport/accountInfomation', // 根据资源id获取通信isdn
  // 行动组
  getActionGroup: '/resource/individual/getActionGroup', // 根据登录人员的id查询登录人员所在的行动组
  getActionGroupCategory: '/resource/agroup/getCategory', // 获取行动组类目
  // 资源中心相关接口
  getActionGroupDetail: '/resource/agroup/getDetail', // 根据行动组id查询行动组详情
  getActionGroupTemplateById: '/resource/agroup/getMould', // 根据行动组模板的id，查询行动组模板
  // gis服务接口
  getAddressSearch: '/gis/getAddressSearch', // 地址搜索
  getAGroupModelByMissionCategoryId: '/mission/getAGroupModelByMissionCategoryId', // 根据任务类目查询行动组模板
  getAllAccountsByResIds: '/passport/getAllAccountsByResIds', // 批量查询资源isdn
  getAllResource: '/rm/v1/resource/query/allResource', // 没提供  人车模糊搜索
  getApplicationConfig: '/getApplicationConfig',
  getBusinessConfig: '/businessConfig/category/getConfig', // 根据个体类目id查询个体类目对应的图标URL(单个地图元素以及聚合地图元素都是该接口
  getByIds: '/resource/person/getByIds', // 获取地图人员状态
  getCameraByIds: '/resource/vehicle/getCameraByIds', // 获取车载摄像头数据
  getCategory: '/resource/individual/getCategory', // 获取所有个体类目
  getCategoryForFilter: '/resource/individual/getCategoryForFilter', // 查询个体类目，仅用来资源筛选
  getCategoryIcon: '/config/category/getConfig', // 根据个体类目id查询个体类目对应的图标URL(单个地图元素以及聚合地图元素都是该接口)
  getConfig: '/businessConfig/getConfig?appid:CDC-1000',
  getContainerContent: '/resource/container/getContent', // 根据容器id查询容器内容
  getCpnsHost: '/getCpnsHost', // 获取cpns服务器ip
  getDetail: '/individual/getDetail',
  getEvidenceById: '/evidence/getEvidenceById', // 证据Id查询证据详情
  getEvidenceFilesToken: '/mission/v1/getEvidenceFilesToken', // 获取证据token
  // 证据
  getEvidenceList: '/evidence/list',
  getEvidenceToken: '/evidence/token/get', // 获取证据下载token
  getFacility: '/resource/facility/get', // 查询设施详情
  // 重点人查询
  getFoucusPerson: '/zdry/getFocusPersonByParam',
  getGeoCode: '/gis/getGeoCode', // 地址编码  根据中文地址返回对应的经纬度
  getGisIpPort: '/gis/getGisIpPort', // 获取GIS地图服务的IP和端口
  getIndividualCategoryBrief: '/resource/individual/getCategoryBrief', // 根据个体类目id查询个体类目简要信息
  getIndividualCategoryDetail: '/resource/individual/getCategory', // 根据个体类目id查询个体类目详情
  getIndividualDetail: '/resource/individual/getDetail', // 根据个体id查询个体详情
  getIndividualRecursively: '/resource/agroup/getIndividualRecursively', // 根据行动组id  和 类目id，嵌套查询行动组下的所有个体
  // 一标三识
  getInformationByBuliding: '/ybss/building/list',
  getInformationByEmploy: '/ybss/building/sydw',
  getInformationByPerson: '/ybss/building/syrk',
  getIntegration: '/resource/individual/getIntegration', // 查询个体集成
  getMainDmInfo: '/maindm/getWorkdmBydmId', // 获取mainDm信息
  getMapCategory: '/config/map/individual/getCategroy', // 返回所有在地图需要展示的个体类目Id
  getMissionByResource: '/mission/getMissionByResource', // 根据资源id查询任务列表
  getMissionDictionnary: '/mission/getMissionDictionary', // 根据状态进行任务分类
  // 任务接口
  getMissionListByEventId: '/mission/missions/event/get', // 根据事件ID查询任务
  getMissionTaskId: '/mission/task/get', // 任务流程按钮数据
  getPermissionList: '/passport/getUserPermissionList', // 获取用户权限列表
  getProcessListAndFeedbackByMissionId: '/mission/v1/getProcessListAndFeedbackByMissionId', // 警情详情处警流程的所有信息
  getRegionByLocation: '/gis/getRegionByLocationAndCategory', // 通过经纬度查询辖区
  getResByAccountName: '/resource/individual/getResByAccountName',
  getResourceByDistance: '/lbs/resource', // 查询一定范围内人、车以及摄像头
  getResourceContainer: '/resource/resourcecontainer/get', // 查询资源组织
  // 资源容器
  getResourceContainerCategory: '/resource/resourcecontainercategory/get', // 查询资源容器类型
  getResourceContainerMember: '/resource/resourcecontainermember/get', // 查询容器中的资源
  getRootLevelOfResource: '/resource/resourcetocontainer/get', // 查找资源的根层级
  /* 通过id获取isdn*/
  // 系统域接口
  getSmmAddress: '/service/mainDm', // 获取smm地址
  getSSONewToken: '/passport/getNewToken', // 获取新token
  // 登录接口
  getSSOTokenUrl: '/passport/login', // 获取SSOTOKEN
  getState: '/resource/individual/getState', // 获取资源状态
  getTaskListByProcessId: '/mission/getTaskListByProcessId', // 获取当前流程节点
  getTrackLastByResourceId: '/lbs/getTrackLastByResourceId', // 获取资源的最新位置信息
  getUserAccountList: '/passport/getUserAccountList', // 获取用户账号列表
  getUserRoleList: '/passport/getUserRoleList', // 获取用户拥有角色
  // 人员
  getUserToResourceByAccountId: '/passport/getUserToResourceByAccountId', // 通过ISDN查人员
  getVehicleDetailInfoByIds: '/resource/getVehicleDetailInfoByIds', // 获取车载摄像头数据
  // 黎曼地图接口
  gishost: '',
  gisServerQuery: '/gis/getByRegionCategory', // lmgis数据表的查询接口---针对辖区、社区、社区、网格
  heartbeat: '/passport/heartbeat', // web端心跳
  // 需要删除的无用接口
  lbsResidLocateUrl: '/lbs/v1/resource/findbyResourceIds', // 依据警力资源的资源id请求位置地址
  logoutDCUrl: '/passport/logout', // 登出DC
  missionAbandonPath: '/mission/v1/abandonMission', // 任务作废
  missionCategories: '/mission/status/category', // 任务状态分类
  missionDetailByMissionId: '/mission/getMissionDetailById', // id查询任务详情
  missionTimeAxis: '/mission/getMissionRecordByMissionId', // 任务时间轴数据
  modifyMonitorCatalog: '/resource/resourcecontainer/update', // 修改该登录账户自定义摄像头层级
  modifyPassword: '/passport/changePassword', // 修改密码
  monitorEquipmentMember: '/monitor/monitorEquipmentMember/get', // 根据前台提供的范围（经纬度、半径距离），查询范围内的摄像头点、设备信息
  pushMessage: '/push/v1/pushMessage',
  queryActGrpByComGrpId: '/acm/actionId', // 通过通信组id获取行动组id
  // 警力相关
  queryActionGroup: '/resource/actiongroup/get', // 获取行动组
  queryAllComGroup: '/acm/groupList', // 查询群组列表
  queryComGroupDetail: '/acm/group', // 查询群组详情
  queryComGrpByActGrpId: '/acm/groupId', // 通过行动组id获取通信组id
  queryCustomMonitorLevel: '/monitor/customizeResourceContainer/get', // 查询自定义摄像头点层级
  // 群组
  queryDynamicGroup: '/acm/dynamicGroupList', // 查询动态组
  // 装备
  queryEquipmentDetail: '/resource/equipment/get', // 获取装备详情
  queryEquipmentDetailByName: '/resource/equipment/getByName', // 查询资源列表车辆
  queryEventById: '/ims/v1/event/queryEventById', // 根据事件id获取事件**带权限
  queryEventCategory: '/event/queryEventCategory', // 事件类目
  queryEventClass: '/event/queryEventClass', // 事件分类
  queryEventDetailById: '/event/queryEventDetailById', // 事件详情
  // 新版本事件接口
  queryEventList: '/event/queryEventList', // 根据条件查询事件列表
  queryEventOperationRecordByEventId: '/event/queryEventOperationRecordByEventId', // 修改事件历史
  queryEventReportCategory: '/event/queryEventReportCategory', // 事件报告类目
  queryEventReporterCategory: '/event/queryEventReporterCategory', // 事件报告者类目
  queryEventTargetCategory: '/event/queryEventTargetCategory', // 事件目标类目
  queryExecutor: '/resource/actiongroup/executor/get', // 获取执行者
  queryIsdnByResIds: '/passport/getOnlineAccountsByResIds', // 批量查询isdn
  queryMissionCategory: '/mission/queryMissionCategory', // 查询人物类目
  queryMissionClass: '/mission/queryMissionClass', // 查询任务分类
  queryMissionList: '/mission/v1/getMissionsByEventId', // 获取事件的所有任务；
  queryMissionListByEventId: '/event/queryMissionListByEventId', // 根据事件ID查询任务
  queryMissionListByParam: '/mission/queryMissionList', // 条件查询任务
  // 摄像头收藏的摄像头请求地址
  queryMonitorCatalog: '/resource/customizeResourceContainer/get', // 查询该登录账户的自定义摄像头层级
  querymonitorContainer: '/monitor/monitorToContainer/get', // 查询摄像头点所在层级关系
  querymonitorContainerMember: '/monitor/monitorContainerMember/get', // 查询摄像头点层级下成员，带设备
  queryMonitoringPoint: '/monitor/facility/get', // 查询摄像头点详细信息
  queryMonitoringPointByKeywords: '/monitor/facility/keywords/get', // 根据关键字查询摄像头点信息
  // 摄像头点特性请求地址
  queryMonitoringPointCategoryInfo: '/monitor/v1/categoryInfo', // 查询摄像头点類目
  queryMonitoringPointOfCurrentFacility: '/monitor/v1/monitorId', // 查询设备所在的摄像头点
  queryMonitoringPointRootHierarchy: '/monitor/monitorContainer/get', // 查询摄像头点信息根层级
  queryOrganization: '/resource/organization/get', // 查询组织
  queryPerson: '/resource/person/get', // 查询人员
  queryPersonKey: '/resource/person/keyword/get', // 查询人员
  queryPersonOutLineByPersonIds: '/rm/v1/person/queryPersonOutLineByPersonIds',
  queryProgressBar: '/evidence/queryProgressBar', // 下载进度查询
  queryStaticGroup: '/acm/staticGroupList', // 查询静态组
  queryTodayPolice: '/resource/policeforce/', // 查询今日警力
  queryTrackHistoryInfo: '/lbs/getHistory', // 获取资源的历史动向信息
  queryTrackHistorySummary: '/lbs/getHistorySummary', // 获取资源的历史动向汇总信息
  regeocode: '/gis/getGeoAddress', // 逆地理编码 根据经纬度返回对应的中文地址
  resourcelocation: '/lbs/resourcelocation', // 人、车（包括资源信息、资源状态、位置信息）通过用户Id过滤所有下级及子集
  // LBS接口
  resourcelocationPage: '/lbs/resourcelocationPage', // 分页查询人、车（包括资源信息、资源状态、位置信息）通过用户Id过滤所有下级及子集
  resourcePolicecarUrl: '/rm/v1/vehicle/queryOrganizationVehicleByUser', // 请求警力资源（警车)地址
  // 事件接口
  searchEvent: '/event/search', // 根据条件查询事件列表
  searchEventList: '/event/queryEventListByCondition', // 模糊搜索
  searchMissionList: '/mission/queryMissionListByCondition', // 模糊搜索任务列表
  searchPolice: '/resource/executor/keywords/get', // 警力搜索
  startProcessInstance: '/workflow/startProcessInstance', // 启动流程节点
  submitMission: '/mission/activity/post', // 执行任务
  tokenRenovate: true,
  totalEvent: '/event/total', // 今日警情、重大警情统计
  updatecationurl: '/ims/v1/event/location/update', // 更新业务数据位置
  updateCustomMonitorLevel: '/monitor/monitorToContainer/update', // 修改自定义摄像头点层级
  updateDycGroup: '/acm/group/update', // 更新群组
  updateDynamicGroup: '/mission/v1/group/updateByGroupIsdn', // 修改动态组
  updateEvent: '/event/post', // 更新事件信息
  updateEventContent: '/event/updateEventDetailById', // 更新事件信息
  updateMission: '/mission/post', // 更新事件信息
  updateMissionById: '/mission/updateMissionById', // 更新任务
};
