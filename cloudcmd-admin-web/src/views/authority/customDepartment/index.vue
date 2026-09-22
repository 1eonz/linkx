<template>
  <div class="app-container">
    <div class="custom-department-manage">
    <!-- ======= 左侧：组织树 ======= -->
    <div class="level-tree-panel" :style="{ width: leftWidth + 'px' }">
      <div class="panel-header">
        <span class="panel-title">组织结构</span>
      </div>

      <div class="tree-body">
        <el-tree
          ref="deptTreeRef"
          class="level-tree"
          node-key="id"
          :data="treeData"
          :props="treeProps"
          :load="loadTreeNode"
          lazy
          highlight-current
          :expand-on-click-node="false"
          :empty-text="treeLoading ? '' : '暂无组织数据'"
          @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="custom-tree-node">
              <i class="el-icon-files node-folder-icon" />
              <span class="node-label" :title="data.name">{{ data.name }}</span>
              <el-dropdown
                class="node-more"
                :class="{ 'is-open': openDropdownId === data.id }"
                trigger="click"
                placement="bottom-end"
                @click.native.stop
                @visible-change="(visible) => handleDropdownVisibleChange(visible, data.id)"
                @command="(cmd) => handleNodeCommand(cmd, node, data)"
              >
                <span class="node-more-icon" @click.stop>
                  <i class="el-icon-more" />
                </span>
                <el-dropdown-menu slot="dropdown">
                  <!-- 组织节点操作 -->
                  <template v-if="data.isOrg">
                    <el-dropdown-item command="addChild" icon="el-icon-plus">新增单位/部门</el-dropdown-item>
                    <el-dropdown-item command="edit" icon="el-icon-edit">编辑组织</el-dropdown-item>
                    <el-dropdown-item command="delete" icon="el-icon-delete" class="dropdown-item--danger">
                      删除组织
                    </el-dropdown-item>
                  </template>
                  <!-- 部门节点操作 -->
                  <template v-else>
                    <el-dropdown-item command="addChild" icon="el-icon-plus">新增单位/部门</el-dropdown-item>
                    <el-dropdown-item command="edit" icon="el-icon-edit">编辑单位/部门</el-dropdown-item>
                    <el-dropdown-item command="delete" icon="el-icon-delete" class="dropdown-item--danger">
                      删除单位/部门
                    </el-dropdown-item>
                  </template>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="tree-add-root">
        <el-button type="primary" size="small" icon="el-icon-plus" style="width: 100%;" @click="openOrgDialog('create')">
          新增组织
        </el-button>
      </div>
    </div>

    <!-- ======= 分割线 ======= -->
    <div class="split-divider" :class="{ 'is-dragging': isDragging }" @mousedown="startDrag">
      <div class="split-divider__line" />
      <div class="split-divider__handle">
        <i class="el-icon-more" />
      </div>
    </div>

    <!-- ======= 右侧：人员列表 ======= -->
    <div class="level-content-panel">
      <div class="panel-header" v-if="currentNode" >
        <span class="panel-title">
          {{ currentNode ? `「${currentNode.name}」下的人员` : '人员列表' }}
        </span>
        <div class="panel-header__actions">
          <div class="search-wrap">
            <el-input
              v-model="userKeyword"
              placeholder="请输入关键词搜索"
              clearable
              size="small"
              style="width: 140px; margin-right: 10px;"
              @keyup.enter.native="handleUserSearch"
            />
            <el-button size="small" type="primary" @click="handleUserSearch" icon="el-icon-search">搜索</el-button>
            <el-button size="small" type="info" @click="handleUserSearchReset" icon="el-icon-circle-close">重置</el-button>
          </div>
          <el-button size="small" type="primary" icon="el-icon-plus" @click="openBindDialog">
            绑定警员
          </el-button>
        </div>
      </div>

      <!-- 未选中节点时的占位 -->
      <div v-if="!currentNode" class="empty-placeholder">
        <el-empty description="请在左侧选择一个组织节点" />
      </div>

      <!-- 已选中节点 -->
      <template v-else>
        <div class="table-wrap">
          <pro-table
            ref="userTable"
            :columns="userColumns"
            :data="userList"
            :loading="userLoading"
            :total="userPage.total"
            :page.sync="userPage.pageNum"
            :limit.sync="userPage.pageSize"
            auto-height
            @pagination="onUserPagination"
          >
            <!-- 操作列 -->
            <template #operation="{ row }">
              <el-button type="danger" size="mini" @click="handleUnbindUser(row)">
                解绑
              </el-button>
            </template>
          </pro-table>
        </div>
      </template>
    </div>

    <!-- ======= 组织新增 / 编辑 弹窗 ======= -->
    <el-dialog
      class="node-dialog"
      :title="orgDialog.type === 'create' ? '新增组织' : '编辑组织'"
      :visible.sync="orgDialog.visible"
      width="500px"
      append-to-body
      @close="resetOrgDialog"
    >
      <el-form ref="orgFormRef" :model="orgDialog.form" :rules="orgRules" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="orgDialog.form.name" placeholder="请输入名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="值班类型" prop="dutyType">
          <el-select
            v-model="orgDialog.form.dutyType"
            placeholder="请选择值班类型"
            clearable
            filterable
            :loading="dutyTypeLoading"
            style="width: 100%"
          >
            <el-option
              v-for="item in dutyTypeOptions"
              :key="item.type"
              :label="item.name"
              :value="item.type"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orgDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="orgDialog.loading" @click="submitOrgDialog">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======= 部门新增 / 编辑 弹窗 ======= -->
    <el-dialog
      class="node-dialog"
      :title="nodeDialog.type === 'create' ? '新增节点' : '编辑节点'"
      :visible.sync="nodeDialog.visible"
      width="500px"
      append-to-body
      @close="resetNodeDialog"
    >
      <el-form ref="nodeFormRef" :model="nodeDialog.form" :rules="nodeRules" label-width="100px">
        <el-form-item label="编码" prop="code">
          <el-input v-model="nodeDialog.form.code" placeholder="请输入编码" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="nodeDialog.form.name" placeholder="请输入名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item v-if="nodeDialog.type === 'create'" label="节点类型" prop="type">
          <el-radio-group v-model="nodeDialog.form.type">
            <el-radio :label="1" :disabled="nodeDialog.parentData && nodeDialog.parentData.type === 2">单位</el-radio>
            <el-radio :label="2">部门</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="nodeDialog.type === 'create' && nodeDialog.parentData" label="父节点">
          <el-input :value="nodeDialog.parentData.name" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="nodeDialog.loading" @click="submitNodeDialog">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======= 绑定警员 弹窗 ======= -->
    <police-select-dialog
      :visible.sync="bindDialogVisible"
      :node-id="currentNode ? currentNode.id : ''"
      :node-name="currentNode ? currentNode.name : ''"
      @confirm="handleBindConfirm"
    />
    </div>
  </div>
</template>

<script>
import { debounce } from 'lodash'
import ProTable from '@/components/ProTable'
import PoliceSelectDialog from '@/components/PoliceSelectDialog'
import { getAllDutyTypes } from '@/api/shiftScheduling/dutyType'
import {
  getOrganizationTree,
  createOrganization,
  updateOrganization,
  deleteOrganization,
  createCustomDepartment,
  updateCustomDepartment,
  deleteCustomDepartment,
  getCustomDepartmentChildren,
  getCustomDepartmentUserPage,
  bindCustomDepartmentUsers,
  unbindCustomDepartmentUsers,
} from '@/api/authority/customDepartment'

const ROOT_DEPT_ID = 0
const LEFT_MIN = 180
const LEFT_MAX = 480
const LEFT_DEFAULT = 260

export default {
  name: 'CustomDepartmentManage',

  components: {
    ProTable,
    PoliceSelectDialog
  },

  data() {
    return {
      // ---------- 拖拽分割线 ----------
      leftWidth: LEFT_DEFAULT,
      isDragging: false,
      dragStartX: 0,
      dragStartWidth: 0,

      // ---------- 树 ----------
      openDropdownId: null,
      treeLoading: false,
      treeData: [], // 组织列表（一级）
      treeProps: { label: 'name', children: 'children', isLeaf: 'isLeaf' },

      currentNode: null,

      // ---------- 组织弹窗 ----------
      orgDialog: {
        visible: false,
        type: 'create',
        loading: false,
        editData: null,
        form: { name: '', dutyType: null }
      },
      orgRules: {
        name: [
          { required: true, message: '请输入名称', trigger: 'blur' },
          { max: 100, message: '最多 100 个字符', trigger: 'blur' }
        ]
      },

      // ---------- 人员列表 ----------
      userLoading: false,
      userList: [],
      userPage: { pageNum: 1, pageSize: 10, total: 0 },
      userKeyword: '',

      // ---------- 节点弹窗 ----------
      nodeDialog: {
        visible: false,
        type: 'create',
        loading: false,
        parentData: null,
        editData: null,
        form: { code: '', name: '', type: 1 }
      },
      nodeRules: {
        code: [
          { required: true, message: '请输入编码', trigger: 'blur' },
          { max: 50, message: '最多 50 个字符', trigger: 'blur' }
        ],
        name: [
          { required: true, message: '请输入名称', trigger: 'blur' },
          { max: 100, message: '最多 100 个字符', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '请选择节点类型', trigger: 'change' }
        ]
      },

      // ---------- 绑定弹窗 ----------
      bindDialogVisible: false,

      // ---------- 值班类型下拉 ----------
      dutyTypeOptions: [],
      dutyTypeLoading: false,
    }
  },

  computed: {
    /** 用户表格列配置 */
    userColumns() {
      return [
        { title: '姓名', dataIndex: 'userName', minWidth: 120 },
        { title: '手机号', dataIndex: 'mobile', minWidth: 180 },
        { title: '所属部门', dataIndex: 'departmentName', minWidth: 150 },
        { title: '绑定时间', dataIndex: 'operateTime', width: 180, align: 'center' },
        { title: '操作', slot: 'operation', width: 120, align: 'center' }
      ]
    }
  },

  created() {
    // 创建防抖搜索方法
    this.debouncedFetchUsers = debounce(this.fetchUsers, 300)
    this.fetchOrganizations()
  },

  beforeDestroy() {
    this._removeDragListeners()
  },

  methods: {
    // =========================================================
    //  拖拽分割线
    // =========================================================
    startDrag(e) {
      this.isDragging = true
      this.dragStartX = e.clientX
      this.dragStartWidth = this.leftWidth

      document.body.style.userSelect = 'none'
      document.body.style.cursor = 'col-resize'

      this._onMouseMove = (ev) => this._doDrag(ev)
      this._onMouseUp = () => this._stopDrag()
      document.addEventListener('mousemove', this._onMouseMove)
      document.addEventListener('mouseup', this._onMouseUp)
    },

    _doDrag(e) {
      const delta = e.clientX - this.dragStartX
      const newWidth = Math.min(LEFT_MAX, Math.max(LEFT_MIN, this.dragStartWidth + delta))
      this.leftWidth = newWidth
    },

    _stopDrag() {
      this.isDragging = false
      document.body.style.userSelect = ''
      document.body.style.cursor = ''
      this._removeDragListeners()
    },

    _removeDragListeners() {
      if (this._onMouseMove) document.removeEventListener('mousemove', this._onMouseMove)
      if (this._onMouseUp) document.removeEventListener('mouseup', this._onMouseUp)
      this._onMouseMove = null
      this._onMouseUp = null
    },

    // =========================================================
    //  组织管理（一级）
    // =========================================================

    /** 获取组织列表 */
    async fetchOrganizations() {
      this.treeLoading = true
      try {
        const res = await getOrganizationTree()
        const orgList = (res.data?.records || res.data || []).map(item => ({
          ...item,
          isOrg: true, // 标记为组织节点
          isLeaf: false // 组织下还有部门，不是叶子节点
        }))
        this.treeData = orgList
      } catch (e) {
        this.$message.error('加载组织列表失败')
      } finally {
        this.treeLoading = false
      }
    },

    /** 打开组织弹窗 */
    openOrgDialog(type, data = null) {
      this.orgDialog.type = type
      this.orgDialog.visible = true
      this.orgDialog.editData = data
      this.orgDialog.form = { name: data ? data.name : '', dutyType: data ? (data.dutyType !== null && data.dutyType !== undefined ? data.dutyType : null) : null }
      this.fetchDutyTypeOptions()
    },

    /** 重置组织弹窗 */
    resetOrgDialog() {
      this.orgDialog.form = { name: '', dutyType: null }
      this.orgDialog.editData = null
      this.$nextTick(() => {
        this.$refs.orgFormRef && this.$refs.orgFormRef.clearValidate()
      })
    },

    async fetchDutyTypeOptions() {
      this.dutyTypeLoading = true
      try {
        const res = await getAllDutyTypes()
        const list = res.data || []
        this.dutyTypeOptions = list.map(item => ({ ...item, type: Number(item.type) }))
      } catch (e) {
        this.dutyTypeOptions = []
      } finally {
        this.dutyTypeLoading = false
      }
    },

    /** 提交组织弹窗 */
    async submitOrgDialog() {
      try {
        await this.$refs.orgFormRef.validate()
      } catch (_) {
        return
      }

      this.orgDialog.loading = true
      try {
        const { type, form, editData } = this.orgDialog

        if (type === 'create') {
          const res = await createOrganization({ name: form.name, dutyType: form.dutyType })
          if (!res || res.code !== 0) {
            this.$message.error(res?.msg || res?.message || '创建失败，请重试')
            return
          }
          this.$message.success('组织创建成功')
          await this.fetchOrganizations()
        } else {
          const res = await updateOrganization(editData.id, { name: form.name, dutyType: form.dutyType })
          if (!res || res.code !== 0) {
            this.$message.error(res?.msg || res?.message || '更新失败，请重试')
            return
          }
          this.$message.success('组织更新成功')
          // 用接口返回值更新树节点数据
          const updatedData = res.data
          if (updatedData) {
            Object.assign(editData, updatedData, { isOrg: true, isLeaf: false })
          }
          // 用接口返回值更新当前选中节点
          if (this.currentNode && this.currentNode.id === editData.id) {
            this.currentNode = { ...this.currentNode, ...(updatedData || { name: form.name, dutyType: form.dutyType }) }
          }
        }

        this.orgDialog.visible = false
      } catch (e) {
        this.$message.error(e?.message || '操作失败，请重试')
      } finally {
        this.orgDialog.loading = false
      }
    },

    /** 删除组织 */
    async confirmDeleteOrg(node, data) {
      try {
        await this.$confirm(
          `确定删除组织「${data.name}」？删除后将无法恢复。`,
          '删除确认',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
            confirmButtonClass: 'el-button--danger'
          }
        )

        const { code, msg } = await deleteOrganization(data.id)
        if (code !== 0) {
          this.$message.error(msg || '删除失败，请重试')
          return
        }
        this.$message.success('组织删除成功')

        if (this.currentNode && this.currentNode.id === data.id) {
          this.currentNode = null
          this.userList = []
        }

        await this.fetchOrganizations()
      } catch (e) {
        if (e === 'cancel' || e?.toString() === 'cancel') return
        this.$message.error(e?.message || '删除失败，请重试')
      }
    },

    // =========================================================
    //  部门管理（二级及以下）
    // =========================================================

    /** el-tree lazy 加载子节点 */
    async loadTreeNode(node, resolve) {
      if (node.level === 0) {
        resolve(this.treeData)
        return
      }

      try {
        const parentNode = node.data
        // 如果父节点是组织，需要传入 departmentCustomId
        const params = {
          parentId: parentNode.isOrg ? ROOT_DEPT_ID : parentNode.id,
          departmentCustomId: parentNode.isOrg ? parentNode.id : parentNode.departmentCustomId
        }
        const res = await getCustomDepartmentChildren(params)
        const children = this._normalizeNodes(res.data?.records || res.data || [], parentNode)
        resolve(children)
      } catch (e) {
        resolve([])
        this.$message.error('加载子节点失败')
      }
    },

    /** 统一格式化节点数据 */
    _normalizeNodes(list, parentData = null) {
      return list.map((item) => ({
        ...item,
        isOrg: false, // 标记为部门节点
        departmentCustomId: parentData?.isOrg ? parentData.id : parentData?.departmentCustomId,
        isLeaf: item.hasChildren === false
      }))
    },

    /** 点击节点 → 加载右侧人员列表 */
    handleNodeClick(data, node) {
      // 组织节点不显示人员列表
      if (data.isOrg) {
        node.expanded ? node.collapse() : node.expand()
        return
      }

      this.currentNode = data
      this.userKeyword = ''
      this.userPage.pageNum = 1
      this.fetchUsers(data.id)

      if (!node.isLeaf) {
        node.expanded ? node.collapse() : node.expand()
      }
    },

    /** 树节点操作菜单 */
    handleNodeCommand(command, node, data) {
      switch (command) {
        case 'addChild':
          this.openNodeDialog('create', data)
          break
        case 'edit':
          if (data.isOrg) {
            this.openOrgDialog('edit', data)
          } else {
            this.openNodeDialog('edit', data)
          }
          break
        case 'delete':
          if (data.isOrg) {
            this.confirmDeleteOrg(node, data)
          } else {
            this.confirmDeleteNode(node, data)
          }
          break
      }
    },

    // =========================================================
    //  部门 CRUD 弹窗
    // =========================================================

    openNodeDialog(type, data) {
      this.nodeDialog.type = type
      this.nodeDialog.visible = true

      if (type === 'create') {
        this.nodeDialog.parentData = data
        this.nodeDialog.editData = null
        // 如果父节点是部门，子节点只能是部门
        const defaultType = data && !data.isOrg && data.type === 2 ? 2 : 1
        this.nodeDialog.form = { code: '', name: '', type: defaultType }
      } else {
        this.nodeDialog.parentData = null
        this.nodeDialog.editData = data
        this.nodeDialog.form = { code: data.code || '', name: data.name, type: data.type }
      }
    },

    resetNodeDialog() {
      this.nodeDialog.form = { code: '', name: '', type: 1 }
      this.nodeDialog.parentData = null
      this.nodeDialog.editData = null
      this.$nextTick(() => {
        this.$refs.nodeFormRef && this.$refs.nodeFormRef.clearValidate()
      })
    },

    async submitNodeDialog() {
      try {
        await this.$refs.nodeFormRef.validate()
      } catch (_) {
        return
      }

      // 校验：部门下不能添加单位
      if (this.nodeDialog.type === 'create' && this.nodeDialog.parentData) {
        if (!this.nodeDialog.parentData.isOrg && this.nodeDialog.parentData.type === 2 && this.nodeDialog.form.type === 1) {
          this.$message.error('部门下不能添加单位')
          return
        }
      }

      this.nodeDialog.loading = true
      try {
        const { type, form, parentData, editData } = this.nodeDialog

        if (type === 'create') {
          // 确定parentId和departmentCustomId
          const parentId = parentData.isOrg ? ROOT_DEPT_ID : parentData.id
          const departmentCustomId = parentData.isOrg ? parentData.id : parentData.departmentCustomId

          const res = await createCustomDepartment({
            code: form.code,
            name: form.name,
            parentId,
            type: form.type,
            departmentCustomId
          })
          if (!res || res.code !== 0) {
            this.$message.error(res?.msg || res?.message || '创建失败，请重试')
            return
          }

          this.$message.success('节点创建成功')
          this._refreshNodeChildren(parentData)
        } else {
          // 编辑时传入 parentId、type 和 departmentCustomId（沿用当前数据）
          const res = await updateCustomDepartment(editData.id, {
            code: form.code,
            name: form.name,
            parentId: editData.parentId || editData.parent_id,
            type: editData.type,
            departmentCustomId: editData.departmentCustomId
          })

          if (!res || res.code !== 0) {
            this.$message.error(res?.msg || res?.message || '更新失败，请重试')
            return
          }

          this.$message.success('节点更新成功')
          editData.code = form.code
          editData.name = form.name
          if (this.currentNode && this.currentNode.id === editData.id) {
            this.currentNode = { ...this.currentNode, code: form.code, name: form.name }
          }
        }

        this.nodeDialog.visible = false
      } catch (e) {
        this.$message.error(e?.message || '操作失败，请重试')
      } finally {
        this.nodeDialog.loading = false
      }
    },

    async confirmDeleteNode(node, data) {
      try {
        const confirmMessage = `确定删除该${data.type === 1 ? '单位' : '部门'}「${data.name}」？删除后将无法恢复。`

        await this.$confirm(
          confirmMessage,
          '删除确认',
          {
            confirmButtonText: '确定删除',
            cancelButtonText: '取消',
            type: 'warning',
            confirmButtonClass: 'el-button--danger'
          }
        )

        const { code, msg } = await deleteCustomDepartment(data.id)
        if (code !== 0) {
          this.$message.error(msg || '删除失败，请重试')
          return
        }
        this.$message.success('节点删除成功')

        if (this.currentNode && this.currentNode.id === data.id) {
          this.currentNode = null
          this.userList = []
        }

        // 刷新父节点的子节点列表
        const treeRef = this.$refs.deptTreeRef
        if (treeRef) {
          const parentNode = treeRef.getNode(data.parentId || ROOT_DEPT_ID)
          if (parentNode) {
            this._refreshNodeChildren(parentNode.data)
          } else {
            // 如果父节点是组织，找到对应的组织节点
            const orgNode = this.treeData.find(org => org.id === data.departmentCustomId)
            if (orgNode) {
              this._refreshNodeChildren(orgNode)
            }
          }
        }
      } catch (e) {
        if (e === 'cancel' || e?.toString() === 'cancel') return
        this.$message.error(e?.message || '删除失败，请重试')
      }
    },

    /** 刷新某节点的子节点列表 */
    _refreshNodeChildren(parentData) {
      const treeRef = this.$refs.deptTreeRef
      if (!treeRef) return
      const parentNode = treeRef.getNode(parentData.id)
      if (parentNode) {
        parentNode.loaded = false
        parentNode.collapse()
        parentNode.expand()
      }
    },

    // =========================================================
    //  右侧人员列表
    // =========================================================

    fetchUsers(deptId) {
      this.userLoading = true

      getCustomDepartmentUserPage(deptId, {
        keyword: this.userKeyword,
        pageNum: this.userPage.pageNum,
        pageSize: this.userPage.pageSize
      }).then(res => {
        const records = res.data?.records || []
        this.userList = records.map(item => ({
          id: item.id,
          userId: item.userId,
          userName: item.user?.name || '-',
          idCard: item.user?.idCard || '-',
          departmentName: item.user?.departmentName || '-',
          mobile: item.user?.mobile || '-',
          gender: item.user?.gender || '-',
          operateTime: item.gmtCreated || '-'
        }))

        this.userPage.total = Number(res.data?.total) || 0
      }).catch(e => {
        this.$message.error('加载人员列表失败')
      }).finally(() => {
        this.userLoading = false
      })
    },

    onUserPagination() {
      this.fetchUsers(this.currentNode.id)
    },

    handleUserSearch() {
      this.userPage.pageNum = 1
      if (this.currentNode) {
        this.debouncedFetchUsers(this.currentNode.id)
      }
    },

    handleUserSearchReset() {
      this.userKeyword = ''
      this.userPage.pageNum = 1
      if (this.currentNode) {
        this.fetchUsers(this.currentNode.id)
      }
    },

    handleUnbindUser(row) {
      this.$confirm(
        `确定解绑「${row.userName}」？`,
        '解绑确认',
        { type: 'warning', confirmButtonText: '确定解绑', cancelButtonText: '取消' }
      ).then(() => {
        return unbindCustomDepartmentUsers(this.currentNode.id, { userIds: row.userId })
      }).then(() => {
        this.$message.success('解绑成功')
        this.fetchUsers(this.currentNode.id)
      }).catch(e => {
        if (e !== 'cancel') {
          this.$message.error(e?.message || '解绑失败')
        }
      })
    },

    // =========================================================
    //  绑定警员 弹窗
    // =========================================================

    openBindDialog() {
      this.bindDialogVisible = true
    },

    handleBindConfirm(selectedList) {
      const users = selectedList.map(user => ({
        departmentCode: user.departmentCode,
        departmentId: user.departmentId,
        id: user.id,
      }))
      if (!users.length) return

      bindCustomDepartmentUsers(this.currentNode.id, { users }).then(res => {
        if (!res || res.code !== 0) {
          this.$message.error(res?.msg || res?.message || '绑定失败，请重试')
          return
        }

        this.$message.success(`成功绑定 ${users.length} 人`)
        this.fetchUsers(this.currentNode.id)
      }).catch(e => {
        this.$message.error(e?.message || '绑定失败，请重试')
      })
    },

    /** dropdown 展开/收起回调 */
    handleDropdownVisibleChange(visible, nodeId) {
      this.openDropdownId = visible ? nodeId : null
    }
  }
}
</script>

<style lang="scss" scoped>
/* 整体容器 - 撑满视口 */
.app-container {
  height: calc(100vh - 50px);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 10px 0;
  box-sizing: border-box;
}

/* 内部容器 */
.custom-department-manage {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  background: #ffffff;
  border-radius: 6px;
}

/* 公共 panel 样式 */
%panel-base {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 0px;
  overflow: hidden;
  box-shadow: none;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid #e8ecf0;
  flex-shrink: 0;
  background: #fefefe;

  &__actions {
    display: flex;
    align-items: center;
    flex-shrink: 0;
  }
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
}

/* 左侧树面板 */
.level-tree-panel {
  @extend %panel-base;
  flex-shrink: 0;
  margin: 8px 0 8px 8px;
  min-width: 180px;
  max-width: 480px;
  transition: width 0s;
}

.tree-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-thumb {
    border-radius: 2px;
    background: #d8dde6;
  }
}

.level-tree {
  width: 100%;
}

/* 自定义树节点 */
.custom-tree-node {
  display: flex;
  align-items: center;
  width: 100%;
  font-size: 13px;
  overflow: hidden;

  .node-folder-icon {
    flex-shrink: 0;
    margin-right: 6px;
    font-size: 15px;
    color: #bbc1c7;
  }

  .node-label {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    min-width: 0;
  }

  .node-more {
    flex-shrink: 0;
    visibility: hidden;
    margin-left: 4px;

    &.is-open {
      visibility: visible;

      .node-more-icon {
        background: #f5f5f5;
      }
    }
  }

  &:hover .node-more {
    visibility: visible;
  }

  .node-more-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 4px;
    cursor: pointer;
    color: #8a9ab5;
    transform: rotate(90deg);

    &:hover {
      background: #e8ecf2;
      color: #409eff;
    }
  }
}

::v-deep .el-tree-node.is-current > .el-tree-node__content {
  background: #e6f0fe;
  color: #409eff;
  font-weight: 600;

  .node-folder-icon {
    color: #409eff;
  }
}

::v-deep .el-dropdown-menu__item {
  display: flex;
  align-items: center;

  [class^='el-icon-'],
  [class*=' el-icon-'] {
    display: inline-block;
    width: 20px;
    font-size: 14px;
    text-align: center;
    margin-right: 6px;
    flex-shrink: 0;
  }
}

::v-deep .el-tree-node__content {
  height: 32px;
  line-height: 32px;
  border-radius: 2px;
}

.tree-add-root {
  flex-shrink: 0;
  padding: 10px 12px;
  border-top: 1px solid #e8ecf0;
  background: #fafbfc;
}

/* 拖拽分割线 */
.split-divider {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 10px;
  flex-shrink: 0;
  cursor: col-resize;
  z-index: 10;

  &__line {
    width: 1px;
    height: 100%;
    background: #e0e4ea;
    transition: background 0.2s;
  }

  &__handle {
    position: absolute;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    width: 10px;
    height: 40px;
    border-radius: 3px;
    background: #e0e4ea;
    transition: background 0.2s, opacity 0.2s;
    opacity: 0;

    span {
      display: block;
      width: 2px;
      height: 2px;
      border-radius: 50%;
      background: #8a9ab5;
    }
  }

  &:hover {
    .split-divider__line {
      background: #409eff;
    }

    .split-divider__handle {
      opacity: 1;
      background: #d0e8ff;
    }
  }

  &.is-dragging {
    .split-divider__line {
      background: #409eff;
      width: 2px;
    }

    .split-divider__handle {
      opacity: 1;
      background: #409eff;

      span {
        background: #fff;
      }
    }
  }
}

/* 右侧人员面板 */
.level-content-panel {
  @extend %panel-base;
  flex: 1;
  min-width: 0;
  margin: 8px 8px 8px 0;
  display: flex;
  flex-direction: column;
}

.empty-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.table-wrap {
  flex: 1;
  overflow: hidden;
  padding: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;

  ::v-deep .pro-table {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
    height: 100%;

    .el-table {
      flex: 1;
      min-height: 0;
      height: 100%;

      .el-table__header-wrapper th {
        background: #f7f8fa;
        color: #5e6d82;
        font-weight: 600;
        font-size: 13px;
      }

      .el-table__body tr:hover > td {
        background: #f0f7ff;
      }

      td {
        font-size: 13px;
        color: #1f2329;
      }
    }
  }
}

.pagination-wrap {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 10px 14px;
  border-top: 1px solid #e8ecf0;
}

.search-wrap {
  display: flex;
  margin-right: 12px;
}

/* 弹窗样式 */
::v-deep .el-dialog {
  display: flex;
  display: -ms-flex;
  flex-direction: column;
  -ms-flex-direction: column;
  margin: 0 !important;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  max-height: calc(100% - 30px);
  max-width: calc(100% - 30px);
}

::v-deep .el-dialog .el-dialog__body {
  max-height: 100%;
  flex: 1;
  -ms-flex: 1 1 auto;
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
