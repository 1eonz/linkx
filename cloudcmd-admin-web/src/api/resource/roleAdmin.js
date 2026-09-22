// Mock 数据 - 接口暂时不可用，先写死固定数据
let mockRoleId = 6
const mockRoleList = [
  { id: 1, name: '超级管理员', status: 0, permissionIds: [] },
  { id: 2, name: '系统管理员', status: 0, permissionIds: [] },
  { id: 3, name: '普通用户', status: 0, permissionIds: [] },
  { id: 4, name: '运维人员', status: 1, permissionIds: [] },
  { id: 5, name: '审计员', status: 0, permissionIds: [] },
  { id: 6, name: '系统内置角色', status: 0, permissionIds: [] }
]

/**
 * 查询角色列表（Mock）
 * @param {Object} params - { applicationId, pageNo, pageSize, name }
 */
export function getRoleList(params) {
  const { pageNo = 1, pageSize = 10, name = '' } = params
  let filtered = mockRoleList
  if (name) {
    filtered = filtered.filter(item => item.name.includes(name))
  }
  const total = filtered.length
  const start = (pageNo - 1) * pageSize
  const records = filtered.slice(start, start + pageSize)

  return Promise.resolve({
    code: 0,
    data: { records, total }
  })
}

/**
 * 新增角色（Mock）
 * @param {Object} data - { applicationId, roleName, permissionIds }
 */
export function createRole(data) {
  mockRoleId++
  mockRoleList.push({
    id: mockRoleId,
    name: data.roleName,
    status: 0,
    permissionIds: data.permissionIds || []
  })
  return Promise.resolve({ code: 0, msg: '新增成功' })
}

/**
 * 更新角色（Mock）
 * @param {Object} data - { id, roleName, permissionIds }
 */
export function updateRole(data) {
  const idx = mockRoleList.findIndex(item => +item.id === +data.id)
  if (idx !== -1) {
    if (data.roleName !== undefined) mockRoleList[idx].name = data.roleName
    if (data.status !== undefined) mockRoleList[idx].status = data.status
    if (data.permissionIds !== undefined) mockRoleList[idx].permissionIds = data.permissionIds
  }
  return Promise.resolve({ code: 0, msg: '更新成功' })
}

/**
 * 删除角色（Mock）
 * @param {Object} data - 角色ID数组
 */
export function deleteRole(data) {
  data.forEach(id => {
    const idx = mockRoleList.findIndex(item => +item.id === +id)
    if (idx !== -1) mockRoleList.splice(idx, 1)
  })
  return Promise.resolve({ code: 0, msg: '删除成功' })
}
