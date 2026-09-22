<template>
  <el-dialog
    :title="$t('index.authority.setPermission')"
    :visible.sync="visible"
    :close-on-click-modal="false"
    @close="closePermissionDialog"
  >
    <el-tabs
      v-model="activeTab"
      type="card"
      style="min-height: 400px;"
      @tab-click="handleTabClick"
    >
      <el-tab-pane
        key="700001"
        :label="$t('index.authority.menuPermissions')"
        name="700001"
        style="max-height: 400px;overflow-y:auto;"
      >
        <el-tree
          ref="menuTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :props="treeProps"
          :data="menuTree"
          :default-checked-keys="menuCheckKeys"
        />
      </el-tab-pane>

      <!-- <el-tab-pane
        key="700002"
        :label="$t('index.authority.functionalAuthority')"
        name="700002"
      >
        <el-container style="height: 100%">
          <el-aside style="width: 25%;max-height: 400px;overflow-y:auto;">
            <br />
            <el-tree
              :props="treeProps"
              :data="actionMenuTree"
              node-key="id"
              default-expand-all
              highlight-current
              @node-click="actionMenuNodeClick"
            />
          </el-aside>
          <div
            style="margin-top: 10px;margin-bottom: 10px;width: 1px;background-color: #99a9bf"
          ></div>
          <el-main style="max-height: 400px;overflow-y:auto;">
            <el-table
              ref="actionTable"
              :data="actionList"
              node-key="id"
              size="mini"
              highlight-current-row
              @selection-change="handleActionSelected"
            >
              <el-table-column
                v-if="hasMenuSelected"
                :selectable="canActionSelect"
                type="selection"
                align="center"
                width="55"
              />
              <el-table-column
                :label="$t('index.list.functionName')"
                :show-overflow-tooltip="true"
                min-width="200"
                align="center"
              >
                <template slot-scope="scope">
                  <span>{{ scope.row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column
                :label="$t('index.list.condition')"
                class-name="status-col"
                min-width="80"
                align="center"
              >
                <template slot-scope="scope">
                  <el-tag :type="scope.row.status | statusFilter">
                    {{ scope.row.status === 0 ? $t('index.list.normal') : '' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-main>
        </el-container>
      </el-tab-pane>

      <el-tab-pane
        key="700000"
        :label="$t('index.authority.groupPermissions')"
        name="700000"
        style="max-height: 400px;overflow-y:auto;"
      >
        <el-tree
          ref="orgTree"
          :data="organizationTree"
          show-checkbox
          node-key="id"
          default-expand-all
          :default-checked-keys="organizationCheckKeys"
          highlight-current
          :props="treeProps"
          @check-change="changeOrganizationCheckKeys"
        />
      </el-tab-pane> -->
    </el-tabs>

    <div slot="footer" class="dialog-footer">
      <el-button type="primary" @click="submitPermissionConfig">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  getPermissionListByParam,
  submitPermissionConfig
} from '@/api/permission/permission'
import { getMenuList } from '@/api/permission/menu'
import { getActionList } from '@/api/permission/action'
import { getOrganizationList } from '@/api/resource/organization'

export default {
  name: 'PermissionDialog',
  filters: {
    statusFilter(status) {
      const statusMap = {
        '0': 'success',
        '1': 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      visible: false,
      activeTab: '700001',
      roler: {},
      menuTree: [],
      menuCheckKeys: [],
      menuAllCheckKeys: [],
      treeProps: {
        children: 'children',
        label: 'name'
      },
      actionMenuTree: [],
      actionList: [],
      actionCheckKeys: [],
      actionMenuCurrentId: '',
      organizationTree: [],
      organizationCheckKeys: [],
      menuIdList: [],
      saveActionList: {}
    }
  },
  computed: {
    hasMenuSelected() {
      return this.menuCheckKeys.includes(this.actionMenuCurrentId)
    }
  },
  methods: {
    // 初始化
    async init(row) {
      this.activeTab = '700001'
      this.menuTree = []
      this.actionMenuTree = []
      this.organizationTree = []
      this.saveActionList = {}
      this.roler = row
      await this.getMenuTree()
      await this.getCheckedKeys()
      this.visible = true
    },
    // 获取选中的节点信息
    async getCheckedKeys() {
      const param = {
        roleId: this.roler.id,
        category: ''
      }
      await getPermissionListByParam(param).then(({ data }) => {
        this.menuCheckKeys = []
        this.actionCheckKeys = []
        this.organizationCheckKeys = []
        data.forEach(permission => {
          if (permission.category === 700001) {
            if (permission.parentId !== '-1') {
              this.menuCheckKeys.push(permission.resourceId)
            } else if (permission.parentId === '-1') {
              this.menuTree.forEach(item => {
                if (
                  item.id === permission.resourceId &&
                  item.children == null
                ) {
                  this.menuCheckKeys.push(permission.resourceId)
                }
              })
            }
          } else if (permission.category === 700002) {
            this.actionCheckKeys.push(permission.resourceId)
          } else if (permission.category === 700000) {
            this.organizationCheckKeys.push(permission.resourceId)
          }
        })
      })
    },
    // 获取菜单
    async getMenuTree() {
      const { applicationId, type } = this.roler
      const param = {
        menuIdList: [],
        applicationId,
        type
      }
      if (this.menuTree.length !== 0) {
        return
      }
      await getMenuList(param).then(res => {
        if (res.code === 0) {
          this.menuTree = this.filterData(res.data)
        }
      })
    },
    changeMenuCheckKeys() {
      this.getMenuAllCheckKeys()
    },
    getMenuAllCheckKeys() {
      const halfCheckedKeys = this.$refs.menuTree.getHalfCheckedKeys()
      const menuCheckKeys = this.$refs.menuTree.getCheckedKeys()
      if (menuCheckKeys.length < this.menuCheckKeys.length) {
        this.menuCheckKeys.forEach(menuId => {
          if (!menuCheckKeys.includes(menuId)) {
            const param = {
              applicationId: this.roler.applicationId,
              menuId
            }
            getActionList(param).then(res => {
              if (res.code === 0) {
                const removeAction = res.data.map(item => item.id)
                const actionCheckKeys = this.actionCheckKeys.filter(item => {
                  return !removeAction.includes(item)
                })
                this.actionCheckKeys = actionCheckKeys
                this.toggleRowSelection()
              }
            })
          }
        })
      }
      this.menuCheckKeys = menuCheckKeys
      this.menuAllCheckKeys = this.menuCheckKeys.concat(halfCheckedKeys)
    },
    getActionMenuTree() {
      if (this.menuIdList.length === 0) {
        this.actionList = []
        this.actionMenuTree = []
      } else {
        const { applicationId, type } = this.roler
        const param = {
          applicationId,
          menuIdList: [],
          type
        }
        if (this.actionMenuTree.length !== 0) {
          return
        }
        getMenuList(param).then(({ data }) => {
          this.actionMenuTree = this.filterData(data)
          if (this.actionMenuTree.length > 0) {
            this.actionMenuCurrentId = this.actionMenuTree[0].id
            const param = {
              applicationId: this.roler.applicationId,
              menuId: this.actionMenuCurrentId
            }
            getActionList(param).then(({ data }) => {
              this.actionList = data.filter(item => item.status === 0)
              this.toggleRowSelection()
            })
          } else {
            this.actionList = []
          }
        })
      }
    },
    actionMenuNodeClick({ children, id }) {
      if (Array.isArray(children)) {
        return
      }
      this.actionMenuCurrentId = id
      const saveActionListItem = this.saveActionList[id]
      if (saveActionListItem) {
        this.actionList = saveActionListItem
        this.toggleRowSelection()
        return
      }
      const param = {
        applicationId: this.roler.applicationId,
        menuId: id
      }
      getActionList(param).then(res => {
        if (res.code === 0) {
          const result = res.data.filter(item => item.status === 0)
          // 将数据存储，避免重复请求
          this.saveActionList[id] = result
          this.actionList = result
          this.toggleRowSelection()
        }
      })
    },
    // 功能权限选中状态
    toggleRowSelection() {
      this.$nextTick(() => {
        this.actionList.forEach(action => {
          const checked = this.actionCheckKeys.indexOf(action.id) !== -1
          this.$refs.actionTable.toggleRowSelection(action, checked)
        })
      })
    },
    canActionSelect(row, index) {
      return row.status === 0
    },
    handleActionSelected(val) {
      this.$nextTick(() => {
        const actionSelect = []
        val.forEach(actSelect => {
          actionSelect.push(actSelect.id)
        })
        this.actionList.forEach(action => {
          if (
            this.actionCheckKeys.indexOf(action.id) !== -1 &&
            actionSelect.indexOf(action.id) === -1
          ) {
            const actionIndex = this.actionCheckKeys.indexOf(action.id)
            this.actionCheckKeys.splice(actionIndex, 1)
          } else if (
            this.actionCheckKeys.indexOf(action.id) === -1 &&
            actionSelect.indexOf(action.id) !== -1
          ) {
            this.actionCheckKeys.push(action.id)
          }
        })
      })
    },
    getOrganizationTree() {
      if (this.organizationTree.length !== 0) {
        return
      }
      getOrganizationList().then(({ data }) => {
        this.organizationTree = data
      })
    },
    changeOrganizationCheckKeys() {
      this.organizationCheckKeys = this.$refs.orgTree.getCheckedKeys()
    },
    // 设置权限
    submitPermissionConfig() {
      this.changeMenuCheckKeys()
      const params = {
        roleId: this.roler.id,
        menuIds: this.menuAllCheckKeys,
        actionIds: this.actionCheckKeys,
        orgIds: this.organizationCheckKeys
      }
      submitPermissionConfig(params).then(result => {
        this.visible = false
        if (result.code === 0) {
          this.$message({
            message: this.$t(
              'index.messageText.permissionConfiguredSuccessfully'
            ),
            type: 'success'
          })
          this.permissionDialogFormVisible = false
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    },
    filterData(arr) {
      const ret = []
      arr.map(item => {
        if (item.status !== 0) {
          return
        }
        if (item.children) {
          item.children = this.filterData(item.children)
        }
        ret.push(item)
      })

      return ret
    },
    // 切换tab
    handleTabClick(tab) {
      switch (tab.name) {
        case '700001':
          this.getMenuTree()
          break
        case '700002':
          this.getMenuAllCheckKeys()
          this.$nextTick(() => {
            this.menuIdList = this.menuAllCheckKeys
            this.getActionMenuTree()
          })
          break
        case '700000':
          this.getOrganizationTree()
          break
      }
    },
    closePermissionDialog() {
      this.menuCheckKeys = []
      this.actionCheckKeys = []
      this.organizationCheckKeys = []
    }
  }
}
</script>

<style></style>
