import request from '@/utils/request'
const baseUrl = `/proxy/icp/v1`;
// 更新icp服务器配置信息
export const updateServerConfig = (data) => request({ url: `/api/icp/server/config`, method: 'put', data })
// 获取icp服务器配置信息
export const getServerConfig = () => request({ url: `/api/icp/server/config`, method: 'get'})

// 获取icp摄像头层级可选根节点的tree
export const getIcpCameraSelectTree = () => request({ url: `${baseUrl}/camera-level/tree/select`, method: 'get' })
// 获取icp组织部门可选根节点的tree
export const getIcpDeptSelectTree = () => request({ url: `${baseUrl}/department/tree/select`, method: 'get' })

// 批量摄像头权限回显
export const getDeptCameraPriv = (deptCode) => request({ url: `${baseUrl}/camera/priv/dept/${deptCode}`, method: 'get' })
// 批量设备组织部门权限回显
export const getDeptOrgPriv = (deptCode) => request({ url: `${baseUrl}/imuser/priv/dept/${deptCode}`, method: 'get' })

// 按部门分页查询用户列表
export const getUserPageByDept = (params) => request({ url: '/auth/v1/user/page/dept', method: 'get', params })