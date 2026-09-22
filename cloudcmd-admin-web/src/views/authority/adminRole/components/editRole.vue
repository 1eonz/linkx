<!--
  角色编辑弹窗组件
  
  功能说明：
  - 新增/编辑角色
  - 角色名称编辑
  - 权限树选择（父子联动）
  
  Props:
  - 无（通过 init 方法传入数据）
  
  Events:
  - success: 保存成功后触发
  
  使用方式：
  this.$refs.editRole.init(row) // 编辑
  this.$refs.editRole.init() // 新增
-->
<template>
  <el-dialog
    :title="dialogTitle"
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    width="800px"
    custom-class="edit-role-dialog"
    @close="handleClose"
    @opened="handleDialogOpened"
  >
    <!-- 角色信息区域 -->
    <div class="role-info-section">
      <el-form
        ref="roleForm"
        :model="roleForm"
        :rules="rules"
        label-width="80px"
        label-position="left"
      >
        <el-form-item :label="$t('index.list.roleName')" prop="name">
          <el-input 
            v-model="roleForm.name" 
            :placeholder="$t('index.operations.inputContent')"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 权限设置区域 -->
    <div class="permission-section">
      <div class="section-header">
        <div class="section-title-wrapper">
          <i class="el-icon-s-check section-icon"></i>
          <span class="section-title">{{ $t('index.messageText.permissionSetting') }}</span>
        </div>
        <div class="section-tools">
          <!-- 权限搜索 -->
          <el-input
            v-model="filterText"
            :placeholder="$t('index.messageText.searchPermissionPlaceholder')"
            prefix-icon="el-icon-search"
            clearable
            size="small"
            style="width: 200px"
            class="search-input"
          />
        </div>
      </div>
      
      <!-- 权限树 -->
      <div class="permission-tree-wrapper">
        <el-tree
          ref="permissionTree"
          v-loading="treeLoading"
          class="permission-tree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :props="treeProps"
          :data="permissionTree"
          :default-checked-keys="checkedKeys"
          :filter-node-method="filterNode"
          @check="handleCheckChange"
        >
          <!-- 自定义节点内容 -->
          <span slot-scope="{ node, data }" class="custom-tree-node">
            <!-- 菜单图标 -->
            <i 
              v-if="data.icon" 
              :class="data.icon" 
              class="node-icon"
            />
            <!-- 节点名称 -->
            <span class="node-label" :title="data.name">{{ data.name }}</span>
            <!-- 节点类型标签 -->
            <el-tag 
              v-if="data.url" 
              size="mini" 
              type="info"
              class="node-tag"
            >
              {{ data.url }}
            </el-tag>
          </span>
        </el-tree>
        
        <!-- 空状态 -->
        <div v-if="!treeLoading && permissionTree.length === 0" class="empty-state">
          <div class="empty-icon-wrapper">
            <i class="el-icon-folder-opened"></i>
          </div>
          <p>{{ $t('index.messageText.noPermissionData') }}</p>
        </div>
      </div>
    </div>
    
    <!-- 底部操作按钮 -->
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button 
        type="primary" 
        :loading="submitLoading"
        @click="handleSubmit"
      >
        {{ $t('index.operations.save') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
// 引入角色API
import { createRole, updateRole } from '@/api/resource/roleAdmin'
// 引入菜单API
import { getMenuList } from '@/api/permission/menu'
// 引入应用配置
import { APPLICATION_ID } from '@/appConfig'

export default {
  name: 'EditRole',
  
  data() {
    return {
      // 弹窗显示状态
      dialogVisible: false,
      // 是否为新增模式
      isAdd: true,
      // 提交加载状态
      submitLoading: false,
      // 树加载状态
      treeLoading: false,
      // 权限过滤文本
      filterText: '',
      
      // 角色表单数据
      roleForm: {
        id: '',
        name: '',
        permissionIds: [] // 权限ID列表
      },
      
      // 表单验证规则
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.messageText.roleNameCannotBeEmpty'),
            trigger: 'blur'
          },
          {
            min: 2,
            max: 50,
            message: this.$t('index.messageText.roleNameLengthRange'),
            trigger: 'blur'
          }
        ]
      },
      
      // 树属性配置
      treeProps: {
        children: 'children',
        label: 'name'
      },
      
      // 权限树数据
      permissionTree: [],
      // 默认选中的权限ID
      checkedKeys: []
    }
  },
  
  computed: {
    /**
     * 弹窗标题
     */
    dialogTitle() {
      return this.isAdd 
        ? this.$t('index.operations.newsRole') 
        : this.$t('index.operations.editRole')
    }
  },
  
  watch: {
    /**
     * 监听权限过滤文本变化
     */
    filterText(val) {
      this.$refs.permissionTree && this.$refs.permissionTree.filter(val)
    }
  },
  
  created() {
  },
  
  methods: {
    /**
     * 初始化弹窗
     * @param {Object} row - 角色数据（编辑时传入，新增时不传）
     */
    async init(row) {
      // 判断是新增还是编辑
      this.isAdd = !row
      
      // 重置表单
      this.resetForm()
      
      if (row) {
        // 编辑模式：填充数据
        this.roleForm.id = row.id
        this.roleForm.name = row.name
        this.roleForm.permissionIds = row.permissionIds || []
        
        // 设置默认选中
        this.checkedKeys = this.roleForm.permissionIds
      } else {
        // 新增模式
        this.checkedKeys = []
      }
      
      // 每次打开弹窗时重新加载权限树，确保数据最新
      await this.loadPermissionTree()
      
      // 显示弹窗
      this.dialogVisible = true
    },
    
    /**
     * 弹窗打开后恢复选中状态
     * 使用 opened 事件确保 DOM 已渲染完成
     */
    handleDialogOpened() {
      if (this.$refs.permissionTree && this.checkedKeys.length > 0) {
        this.$refs.permissionTree.setCheckedKeys(this.checkedKeys)
      }
    },
    
    /**
     * 加载权限树数据
     * 使用与后台权限管理页面一致的逻辑
     */
    async loadPermissionTree() {
      this.treeLoading = true
      
      try {
        // 获取后台菜单树（applicationId: '' 表示后台权限）
        const { code, data } = await getMenuList({ applicationId: '' })
        
        if (code === 0) {
          // 过滤并处理数据
          this.permissionTree = this.filterPermissionData(data)
        }
      } catch (error) {
        console.error('加载权限树失败:', error)
        this.$message.error(this.$t('index.messageText.loadPermissionFailed'))
      } finally {
        this.treeLoading = false
      }
    },
    
    /**
     * 过滤权限数据
     * 只保留 status 为 0（启用）或 2（系统内置）的菜单
     * @param {Array} data - 原始数据
     * @returns {Array} 过滤后的数据
     */
    filterPermissionData(data) {
      if (!data || !Array.isArray(data)) return []
      
      return data.reduce((result, item) => {
        // 过滤掉禁用状态
        if (item.status !== 0 && item.status !== 2) {
          return result
        }
        
        // 浅拷贝节点，避免修改入参
        const cloned = { ...item }
        
        // 递归处理子节点
        if (item.children && item.children.length > 0) {
          const filteredChildren = this.filterPermissionData(item.children)
          if (filteredChildren.length > 0) {
            cloned.children = filteredChildren
          } else {
            delete cloned.children
          }
        }
        
        result.push(cloned)
        return result
      }, [])
    },
    
    /**
     * 树节点过滤方法
     * @param {String} value - 过滤文本
     * @param {Object} data - 节点数据
     * @returns {Boolean} 是否匹配
     */
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    
    /**
     * 处理权限树勾选变化
     * 父子联动已通过移除 check-strictly 实现
     * @param {Object} data - 当前节点数据
     * @param {Object} checkStatus - 勾选状态
     */
    handleCheckChange(data, { checkedKeys }) {
      // 更新选中的权限ID（包含所有选中的节点，父子联动自动处理）
      this.roleForm.permissionIds = checkedKeys
    },
    
    /**
     * 提交表单
     */
    handleSubmit() {
      this.$refs.roleForm.validate(async valid => {
        if (!valid) return
        
        this.submitLoading = true
        
        try {
          // 构建提交参数
          const permissionIds = this.$refs.permissionTree 
            ? this.$refs.permissionTree.getCheckedKeys() 
            : []

          let params = {}

          if (this.isAdd) {
            // 新增：applicationId + roleName + permissionIds
            params = {
              applicationId: APPLICATION_ID.ADMIN,
              roleName: this.roleForm.name,
              permissionIds
            }
          } else {
            // 编辑：id + roleName + permissionIds
            params = {
              id: this.roleForm.id,
              roleName: this.roleForm.name,
              permissionIds
            }
          }
          
          // 调用对应的API
          const api = this.isAdd ? createRole : updateRole
          const result = await api(params)
          
          if (result.code === 0) {
            this.$message({
              message: result.msg || this.$t('index.messageText.operationSuccess'),
              type: 'success'
            })
            // 触发成功事件
            this.$emit('success')
            // 关闭弹窗
            this.handleClose()
          } else {
            this.$message({
              message: result.msg,
              type: 'error'
            })
          }
        } catch (error) {
          console.error('提交失败:', error)
          this.$message.error(this.$t('index.messageText.operationFailed'))
        } finally {
          this.submitLoading = false
        }
      })
    },
    
    /**
     * 重置表单
     */
    resetForm() {
      this.roleForm = {
        id: '',
        name: '',
        permissionIds: []
      }
      this.checkedKeys = []
      this.filterText = ''
      
      // 清空表单验证
      this.$nextTick(() => {
        this.$refs.roleForm && this.$refs.roleForm.resetFields()
        this.$refs.permissionTree && this.$refs.permissionTree.setCheckedKeys([])
      })
    },
    
    /**
     * 关闭弹窗
     */
    handleClose() {
      this.resetForm()
      this.dialogVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
// 角色信息区域
.role-info-section {
  padding-bottom: 16px;
  border-bottom: 1px solid #F0F2F5;
  margin-bottom: 16px;
}

// 权限设置区域
.permission-section {
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .section-title-wrapper {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .section-icon {
      font-size: 16px;
      color: #409EFF;
    }
    
    .section-title {
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }

    .search-input {
      ::v-deep .el-input__inner {
        border-radius: 16px;
      }
    }
  }
  
  .permission-tree-wrapper {
    max-height: 400px;
    border: 1px solid #E8ECF1;
    border-radius: 6px;
    padding: 12px;
    overflow-y: auto;
    background: #FAFBFC;
    transition: border-color 0.2s ease;

    &:hover {
      border-color: #C0C4CC;
    }
  }
}

// 权限树样式
.permission-tree {
  ::v-deep .el-tree-node__content {
    height: 34px;
    border-radius: 4px;
    margin-bottom: 1px;
    transition: background-color 0.2s ease;
    
    &:hover {
      background-color: #F5F7FA;
    }
  }
  
  ::v-deep .el-tree-node.is-current > .el-tree-node__content {
    background-color: #ECF5FF;
    font-weight: 500;
  }
}

// 自定义树节点样式
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  font-size: 14px;
  
  .node-icon {
    margin-right: 8px;
    color: #409EFF;
    font-size: 15px;
  }
  
  .node-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #303133;
  }
  
  .node-tag {
    margin-left: 10px;
    max-width: 150px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    border-radius: 10px;
  }
}

// 空状态样式
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #909399;

  .empty-icon-wrapper {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    background: linear-gradient(135deg, #e8f4fd, #f0f7ff);
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 16px;

    i {
      font-size: 28px;
      color: #409EFF;
    }
  }
  
  p {
    margin: 0;
    font-size: 14px;
    color: #909399;
  }
}

// 底部按钮样式
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 4px;
}
</style>

<style lang="scss">
// 弹窗全局样式（不使用 scoped）
.edit-role-dialog {
  border-radius: 10px;

  .el-dialog__header {
    padding: 20px 24px 16px;
    border-bottom: 1px solid #F0F2F5;
  }

  .el-dialog__body {
    padding: 20px 24px;
  }

  .el-dialog__footer {
    padding: 16px 24px 20px;
    border-top: 1px solid #F0F2F5;
  }
}
</style>
