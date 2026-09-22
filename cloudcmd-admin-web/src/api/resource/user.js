/**
 * 用户管理相关 API
 * 
 * 接口说明：
 * - getUserListByPage: 分页获取用户列表
 * - getUsersByRoleId: 获取角色下的用户列表
 * - bindUsersToRole: 为角色绑定用户
 * - unbindUsersFromRole: 从角色移除用户
 */

import request from '@/utils/request'

/**
 * 分页获取用户列表
 * @param {Object} params - 查询参数
 * @param {String} params.name - 用户名称（模糊搜索）
 * @param {String} params.account - 账号（模糊搜索）
 * @param {String} params.departmentCode - 组织编码
 * @param {Number} params.pageNum - 页码
 * @param {Number} params.pageSize - 每页数量
 * @returns {Promise} 用户列表数据
 */
export function getUserListByPage(params) {
  // TODO: 对接真实接口
  // return request({
  //   url: '/api/user/listByPage',
  //   method: 'post',
  //   data: params
  // })
  
  // ========== Mock 数据 ==========
  return new Promise((resolve) => {
    const { name = '', pageNum = 1, pageSize = 10 } = params
    
    // 模拟用户数据
    const allUsers = []
    for (let i = 1; i <= 100; i++) {
      allUsers.push({
        id: i,
        name: `测试用户${i}`,
        account: `user${i}`,
        departmentName: `组织${Math.ceil(i / 10)}`,
        departmentCode: `ORG${Math.ceil(i / 10)}`,
        status: i % 5 === 0 ? 1 : 0 // 每5个用户有一个禁用
      })
    }
    
    // 根据名称过滤
    let filteredUsers = allUsers
    if (name) {
      filteredUsers = allUsers.filter(user => 
        user.name.includes(name) || user.account.includes(name)
      )
    }
    
    // 分页
    const start = (pageNum - 1) * pageSize
    const records = filteredUsers.slice(start, start + pageSize)
    
    setTimeout(() => {
      resolve({
        code: 0,
        msg: 'success',
        data: {
          records,
          total: filteredUsers.length,
          pageNum,
          pageSize
        }
      })
    }, 300) // 模拟网络延迟
  })
}

/**
 * 获取角色下的用户列表
 * @param {String|Number} roleId - 角色ID
 * @returns {Promise} 用户ID列表
 */
export function getUsersByRoleId(roleId) {
  // TODO: 对接真实接口
  // return request({
  //   url: '/api/role/getUsers',
  //   method: 'get',
  //   params: { roleId }
  // })
  
  // ========== Mock 数据 ==========
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 0,
        msg: 'success',
        data: [] // 返回用户ID数组
      })
    }, 200)
  })
}

/**
 * 为角色绑定用户
 * @param {Object} data - 绑定数据
 * @param {String|Number} data.roleId - 角色ID
 * @param {Array} data.userIds - 用户ID数组
 * @returns {Promise}
 */
export function bindUsersToRole(data) {
  // TODO: 对接真实接口
  // return request({
  //   url: '/api/role/bindUsers',
  //   method: 'post',
  //   data
  // })
  
  // ========== Mock 数据 ==========
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 0,
        msg: '设置用户成功'
      })
    }, 300)
  })
}

/**
 * 从角色移除用户
 * @param {Object} data - 移除数据
 * @param {String|Number} data.roleId - 角色ID
 * @param {Array} data.userIds - 用户ID数组
 * @returns {Promise}
 */
export function unbindUsersFromRole(data) {
  // TODO: 对接真实接口
  // return request({
  //   url: '/api/role/unbindUsers',
  //   method: 'post',
  //   data
  // })
  
  // ========== Mock 数据 ==========
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve({
        code: 0,
        msg: '移除用户成功'
      })
    }, 300)
  })
}
