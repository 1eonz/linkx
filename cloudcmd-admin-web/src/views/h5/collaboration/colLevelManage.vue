<template>
  <div class="col-level-manage">
    <!-- ======= 左侧：层级树 ======= -->
    <div class="level-tree-panel" :style="{ width: leftWidth + 'px' }">
      <div class="panel-header">
        <span class="panel-title">层级结构</span>
      </div>

      <div class="tree-body">
        <el-tree ref="levelTreeRef" class="level-tree" node-key="id" :data="treeData" :props="treeProps"
          :load="loadTreeNode" lazy highlight-current :expand-on-click-node="false"
          :empty-text="treeLoading ? '' : '暂无层级数据'" @node-click="handleNodeClick">
          <template #default="{ node, data }">
            <div class="custom-tree-node">
              <!-- 固定文件夹图标，不区分展开/收起 -->
              <i class="el-icon-files node-folder-icon" />
              <span class="node-label" :title="data.name">{{ data.name }}</span>
              <!-- 改动1：node-more 动态绑定 active class -->
              <el-dropdown class="node-more" :class="{ 'is-open': openDropdownId === data.id }" trigger="click"
                placement="bottom-end" @click.native.stop
                @visible-change="(visible) => handleDropdownVisibleChange(visible, data.id)"
                @command="(cmd) => handleNodeCommand(cmd, node, data)">
                <span class="node-more-icon" @click.stop>
                  <i class="el-icon-more" />
                </span>
                <el-dropdown-menu slot="dropdown">
                  <el-dropdown-item command="addChild" icon="el-icon-plus">添加子层级</el-dropdown-item>
                  <el-dropdown-item command="edit" icon="el-icon-edit">编辑层级</el-dropdown-item>
                  <el-dropdown-item command="delete" icon="el-icon-delete" class="dropdown-item--danger">
                    删除层级
                  </el-dropdown-item>
                </el-dropdown-menu>
              </el-dropdown>
            </div>
          </template>
        </el-tree>
      </div>

      <div class="tree-add-root">
        <el-button type="primary" size="small" icon="el-icon-plus" style="width: 100%;"
          @click="openNodeDialog('create', null)">
          新增层级
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

    <!-- ======= 右侧：协同岗列表 ======= -->
    <div class="level-content-panel">
      <div class="panel-header">
        <span class="panel-title">
          {{ currentNode ? `「${currentNode.name}」下的协同岗` : '协同岗列表' }}
        </span>
        <div class="panel-header__actions">
          <div class="search-wrap">
            <el-input v-model="memberKeyword" placeholder="搜索协同岗名称" clearable size="small" style="width: 140px; margin-right: 6px;" />
           <el-date-picker v-model="startTime" type="date" placeholder="开始时间" clearable size="small" style="width: 140px; margin-right: 6px;" format="yyyy-MM-dd" value-format="yyyy-MM-dd" />
            <el-date-picker v-model="endTime" type="date" placeholder="结束时间" clearable size="small" style="width: 140px; margin-right: 10px;" format="yyyy-MM-dd" value-format="yyyy-MM-dd" />
            <el-button size="small" type="primary" @click="handleMemberSearch" icon="el-icon-search">搜索</el-button>
            <el-button size="small" type="info" @click="handleMemberSearchReset" icon="el-icon-circle-close">重置</el-button>
          </div>
          <el-button v-if="currentNode"  size="small" type="primary" icon="el-icon-plus" @click="openBindDialog">
            挂靠协同岗
          </el-button>
        </div>
      </div>

      <!-- 未选中层级时的占位 -->
      <div v-if="!currentNode" class="empty-placeholder">
        <el-empty description="请在左侧选择一个层级" />
      </div>

      <!-- 已选中层级 -->
      <template v-else>
        <div class="table-wrap">
          <el-table v-loading="memberLoading" :data="pagedMemberList" border size="small" height="100%"
            style="width: 100%;">
            <el-table-column type="index" label="序号" width="100" align="center" />
            <el-table-column prop="name" label="协同岗名称" show-overflow-tooltip />
            <el-table-column prop="orgName" label="所属组织" show-overflow-tooltip />
            <el-table-column prop="relatedUserNames" label="关联人员" show-overflow-tooltip />
            <el-table-column prop="operateTime" label="操作时间" align="center" />
            <el-table-column label="操作" align="center" width="150">
              <template #default="{ row }">
                <el-button type="danger" size="mini" @click="handleUnbindMember(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="pagination-wrap">
          <el-pagination background :current-page="memberPage.page" :page-size="memberPage.pageSize"
            :page-sizes="[10, 20, 50]" :total="memberPage.total" layout="total, sizes, prev, pager, next, jumper"
            @size-change="(v) => { memberPage.pageSize = v; memberPage.page = 1; fetchMembers(currentNode.id) }"
            @current-change="(v) => { memberPage.page = v; fetchMembers(currentNode.id) }" />
        </div>
      </template>
    </div>

    <!-- ======= 层级新增 / 编辑 弹窗 ======= -->
    <el-dialog class="node-dialog" :title="nodeDialog.type === 'create' ? '新增层级' : '编辑层级'" :visible.sync="nodeDialog.visible"
      width="420px" append-to-body @close="resetNodeDialog">
      <el-form ref="nodeFormRef" :model="nodeDialog.form" :rules="nodeRules" label-width="80px">
        <el-form-item label="层级名称" prop="name">
          <el-input v-model="nodeDialog.form.name" placeholder="请输入层级名称" maxlength="32" show-word-limit />
        </el-form-item>
        <el-form-item v-if="nodeDialog.type === 'create' && nodeDialog.parentData" label="父层级">
          <el-input :value="nodeDialog.parentData.name" disabled />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="nodeDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="nodeDialog.loading" @click="submitNodeDialog">确定</el-button>
      </template>
    </el-dialog>

    <!-- ======= 挂靠协同岗 弹窗 ======= -->
    <el-dialog title="挂靠协同岗" :visible.sync="bindDialog.visible" width="680px" append-to-body @close="resetBindDialog">
      <div class="bind-dialog-body">
        <div class="bind-search-bar">
          <el-input v-model="bindDialog.keyword" placeholder="搜索协同岗名称" prefix-icon="el-icon-search" clearable
            size="small" style="width: 220px;" @input="handleBindSearch" />
        </div>
        <el-table ref="bindTableRef" v-loading="bindDialog.loading" :data="bindDialog.list" border size="small"
          max-height="360" row-key="id" @selection-change="(rows) => (bindDialog.selected = rows)">
          <el-table-column type="selection" width="46" reserve-selection :selectable="checkSelectable"/>
          <el-table-column prop="postName" label="协同岗名称" show-overflow-tooltip />
          <el-table-column prop="orgName" label="所属组织" show-overflow-tooltip />
          <el-table-column prop="relatedUserNames" label="关联人员" show-overflow-tooltip />
        </el-table>
        <div class="pagination-wrap">
          <el-pagination background :current-page="bindDialog.page" :page-size="bindDialog.pageSize"
            :total="bindDialog.total" layout="total, prev, pager, next"
            @current-change="(v) => { bindDialog.page = v; fetchBindList() }" />
        </div>
      </div>
      <template #footer>
        <el-button @click="bindDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="bindDialog.submitLoading" :disabled="bindDialog.selected.length === 0"
          @click="submitBind">
          确定挂靠（已选 {{ bindDialog.selected.length }} 个）
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { debounce } from 'lodash'
import {
  getCoopLevelChildren,
  createCoopLevel,
  updateCoopLevel,
  deleteCoopLevel,
  getCoopLevelMembers,
  updateCoopLevelMembers,
  deleteCoopLevelMembers,
  getCoopLevelMembersBySearch,
} from '@/api/h5/coopLevel.js'
import { getCollaborationPage, queryUserByIdCard } from '@/api/h5/collaboration'
import { getIdCardNum } from '@/utils/auth'

const ROOT_LEVEL_ID = 0
const LEFT_MIN = 180   // 左侧最小宽度 px
const LEFT_MAX = 480   // 左侧最大宽度 px
const LEFT_DEFAULT = 260

export default {
  name: 'ColLevelManage',

  props: {
    isAdmin: { type: Boolean, default: false }
  },

  data() {
    return {
      // ---------- 拖拽分割线 ----------
      leftWidth: LEFT_DEFAULT,
      isDragging: false,
      dragStartX: 0,
      dragStartWidth: 0,

      // ---------- 树 ----------
      openDropdownId: null,   // ← 新增：记录当前展开 dropdown 的节点 id
      treeLoading: false,
      treeData: [],
      treeProps: { label: 'name', children: 'children', isLeaf: 'isLeaf' },

      currentNode: null,

      // ---------- 成员列表 ----------
      memberLoading: false,
      allMemberList: [],
      memberPage: { page: 1, pageSize: 10, total: 0 },
      memberKeyword: '',
      startTime: '',
      endTime: '',
      // ---------- 层级弹窗 ----------
      nodeDialog: {
        visible: false,
        type: 'create',
        loading: false,
        parentData: null,
        editData: null,
        form: { name: '' }
      },
      nodeRules: {
        name: [
          { required: true, message: '请输入层级名称', trigger: 'blur' },
          { max: 32, message: '最多 32 个字符', trigger: 'blur' }
        ]
      },

      // ---------- 挂靠弹窗 ----------
      bindDialog: {
        visible: false,
        loading: false,
        submitLoading: false,
        keyword: '',
        list: [],
        selected: [],
        page: 1,
        pageSize: 10,
        total: 0
      },

      // ---------- 用户信息 ----------
      userInfo: null,
      departmentCode: '',
      departmentId: '',
      idCardNum: getIdCardNum()
    }
  },

  computed: {
    pagedMemberList() {
      return this.allMemberList
    }
  },

  created() {
    this.getUserInfoByIdCard()
    this.handleBindSearch = debounce(this._doBindSearch, 300)
    this.handleMemberSearch = debounce(this._doMemberSearch, 300)
    this.fetchRootNodes()
  },

  beforeDestroy() {
    this._removeDragListeners()
  },

  methods: {
    // =========================================================
    //  拖拽分割线
    // =========================================================
    checkSelectable(row) {
      return !row.selected
    },
    startDrag(e) {
      this.isDragging = true
      this.dragStartX = e.clientX
      this.dragStartWidth = this.leftWidth

      // 拖拽期间在 body 上禁止文本选中
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
    //  树形结构
    // =========================================================

    /** 首次加载根层级 */
    async fetchRootNodes() {
      this.treeLoading = true
      try {
        const res = await getCoopLevelChildren(ROOT_LEVEL_ID)
        this.treeData = this._normalizeNodes(res.data || [])
      } catch (e) {
        this.$message.error('加载层级结构失败')
      } finally {
        this.treeLoading = false
      }
    },

    /** el-tree lazy 加载子层级 */
    async loadTreeNode(node, resolve) {
      if (node.level === 0) {
        resolve(this.treeData)
        return
      }
      try {
        const res = await getCoopLevelChildren(node.data.id)
        resolve(this._normalizeNodes(res.data || []))
      } catch (e) {
        resolve([])
        this.$message.error('加载子层级失败')
      }
    },

    /** 统一格式化层级数据 */
    // 只有明确返回 false 才是叶子
    _normalizeNodes(list) {
      return list.map((item) => ({ ...item, isLeaf: item.hasChildren === false }))
    },

    /** 点击层级 → 加载右侧成员列表 */
    handleNodeClick(data, node) {
      this.currentNode = data
      this.memberKeyword = ''
       this.startTime = ''
      this.endTime = ''
      this.memberPage.page = 1
      this.fetchMembers(data.id)

      if (!node.isLeaf) {
        node.expanded ? node.collapse() : node.expand() // 展开触发懒加载
      }
    },

    /** 树层级操作菜单 */
    handleNodeCommand(command, node, data) {
      switch (command) {
        case 'addChild':
          this.openNodeDialog('create', data)
          break
        case 'edit':
          this.openNodeDialog('edit', data)
          break
        case 'delete':
          this.confirmDeleteNode(node, data)
          break
      }
    },

    // =========================================================
    //  层级 CRUD 弹窗
    // =========================================================

    openNodeDialog(type, data) {
      this.nodeDialog.type = type
      this.nodeDialog.visible = true

      if (type === 'create') {
        // data 为父层级数据（根层级时 data 为 null）
        this.nodeDialog.parentData = data
        this.nodeDialog.editData = null
        this.nodeDialog.form = { name: '' }
      } else {
        // edit：data 为当前层级数据
        this.nodeDialog.parentData = null
        this.nodeDialog.editData = data
        this.nodeDialog.form = { name: data.name }
      }
    },

    resetNodeDialog() {
      this.nodeDialog.form = { name: '' }
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
      // 创建层级深度不能超过5层
      if (this.nodeDialog.type === 'create' && this.nodeDialog.parentData) {
        const parentNode = this.$refs.levelTreeRef.getNode(this.nodeDialog.parentData.id)
        if (parentNode && parentNode.level >= 5) {
          this.$message.error('创建层级深度不能超过5层')
          return
        }
      }

      this.nodeDialog.loading = true
      try {
        const { type, form, parentData, editData } = this.nodeDialog

        if (type === 'create') {
          const parentId = parentData ? parentData.id : ROOT_LEVEL_ID
          const res = await createCoopLevel({ name: form.name, parentId })
          if (!res || res.code !== 0) {
            this.$message.error(res?.msg || res?.message || '创建失败，请重试')
            return
          }

          this.$message.success('层级创建成功')
          if (!parentData) {
            await this.fetchRootNodes()
          } else {
            this._refreshNodeChildren(parentData)
          }
        } else {
          const res = await updateCoopLevel(editData.id, { name: form.name })

          if (!res || res.code !== 0) {
            this.$message.error(res?.msg || res?.message || '更新失败，请重试')
            return
          }

          this.$message.success('层级更新成功')
          editData.name = form.name
          if (this.currentNode && this.currentNode.id === editData.id) {
            this.currentNode = { ...this.currentNode, name: form.name }
          }
        }

        this.nodeDialog.visible = false
      } catch (e) {
        this.$message.error(e?.message || '操作失败，请重试')
      } finally {
        this.nodeDialog.loading = false
      }
    },

    confirmDeleteNode(node, data) {
      this.$confirm(
        `确定删除层级「${data.name}」？删除后将无法恢复。`,
        '删除确认',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }
      )
        .then(async () => {
          try {
            const { code, msg } = await deleteCoopLevel(data.id)
            if (code !== 0) {
              this.$message.error(msg || '删除失败，请重试')
              return
            }
            this.$message.success('层级删除成功')

            // 若当前选中的层级是被删层级或其子层级，清空右侧
            if (this.currentNode && this._isDescendantOrSelf(data, this.currentNode)) {
              this.currentNode = null
              this.allMemberList = []
            }

            // 根层级（level === 1）直接重拉整棵树
            // 否则只刷新父层级子列表，范围更小
            if (node.level === 1) {
              await this.fetchRootNodes()
            } else {
              this._refreshNodeChildren(node.parent.data)
            }
          } catch (e) {
            this.$message.error(e?.message || '删除失败，请重试')
          }
        })
        .catch(() => { })
    },


    /**
     * 刷新某层级的子层级列表
     * 通过 el-tree 内部 store 找到对应 Node 并 reload
     */
    _refreshNodeChildren(parentData) {
      const treeRef = this.$refs.levelTreeRef
      if (!treeRef) return
      const parentNode = treeRef.getNode(parentData.id)
      if (parentNode) {
        parentNode.loaded = false   // 标记为未加载，触发懒加载重新请求
        parentNode.collapse()       // 先收起
        parentNode.expand()         // 再展开，触发 loadTreeNode 回调
      }
    },

    /** 判断 target 是否是 ancestor 本身或其后代（通过 id 简单比对） */
    _isDescendantOrSelf(ancestor, target) {
      if (ancestor.id === target.id) return true
      // 由于树是懒加载，只能简单通过 id 前缀或路径判断；
      // 此处保守处理：仅精确比对 id，避免误清空
      return false
    },

    // =========================================================
    //  右侧成员列表
    // =========================================================

    async fetchMembers(levelId) {
      this.memberLoading = true
      try {
        const res = await getCoopLevelMembers(levelId, {
          pageNum: this.memberPage.page,
          pageSize: this.memberPage.pageSize,
          orgId: this.departmentId,
        })
        // 接口返回格式按实际调整（records / list / data）
        this.allMemberList = res.data?.records || res.data || []
        this.memberPage.total = Number(res.data?.total) || 0
      } catch (e) {
        this.$message.error('加载协同岗列表失败')
      } finally {
        this.memberLoading = false
      }
    },
    async fetchMembersBySearch(params) {
      this.memberLoading = true
      try {
        const res = await getCoopLevelMembersBySearch(params)
        // 接口返回格式按实际调整（records / list / data）
        this.allMemberList = res.data?.records || res.data || []
        this.memberPage.total = Number(res.data?.total) || 0
      } catch (e) {
        this.$message.error('加载协同岗列表失败')
      } finally {
        this.memberLoading = false
      }
    },
    _doMemberSearch() {
      this.memberPage.page = 1
      this.fetchMembersBySearch({
        pageNum: this.memberPage.page,
        pageSize: this.memberPage.pageSize,
        orgId: this.departmentId,
        name: this.memberKeyword,
        levelId: this.currentNode.id,
        startTime: this.startTime,
        endTime: this.endTime
      })
    },
    handleMemberSearchReset() {
      this.memberKeyword = ''
      this.startTime = ''
      this.endTime = ''
      this.memberPage.page = 1
      if (this.currentNode) {
        this.fetchMembers(this.currentNode.id)
      }
    },
    async handleUnbindMember(row) {
      try {
        await this.$confirm(
          `确定将「${row.name}」从当前层级移除？`,
          '移除确认',
          { type: 'warning', confirmButtonText: '确定移除', cancelButtonText: '取消' }
        )
      } catch (_) {
        return
      }
      try {
        await deleteCoopLevelMembers([String(row.uid)])
        this.$message.success('移除成功')
        await this.fetchMembers(this.currentNode.id)
      } catch (e) {
        this.$message.error(e?.message || '移除失败')
      }
    },

    // =========================================================
    //  挂靠协同岗 弹窗
    // =========================================================

    openBindDialog() {
      this.bindDialog.visible = true
      this.bindDialog.keyword = ''
      this.bindDialog.page = 1
      this.bindDialog.selected = []
      this.fetchBindList()
    },

    resetBindDialog() {
      this.$refs.bindTableRef && this.$refs.bindTableRef.clearSelection()
      this.bindDialog.list = []
      this.bindDialog.selected = []
      this.bindDialog.keyword = ''
    },

    async fetchBindList() {
      this.bindDialog.loading = true
      try {
        const res = await getCollaborationPage({
          type: 0, // 排除人员核查岗位
          orgId: this.departmentId,
          postName: this.bindDialog.keyword,   // ← 参数名对齐
          selectionType: 'coopLevel',
          selectionId: this.currentNode.id,
          pageNum: this.bindDialog.page,       // ← 参数名对齐
          pageSize: this.bindDialog.pageSize
        })
        this.bindDialog.list = res.records || []          // ← records
        this.bindDialog.total = Number(res.total) || 0     // ← total 是字符串，转数字
      } catch (e) {
        this.$message.error('获取协同岗列表失败')
      } finally {
        this.bindDialog.loading = false
      }
    },

    _doBindSearch() {
      this.bindDialog.page = 1
      this.fetchBindList()
    },
    // 挂靠协同岗
    async submitBind() {
      const ids = this.bindDialog.selected.map((r) => r.id)
      if (!ids.length) return

      this.bindDialog.submitLoading = true
      try {
        const postIds = ids.map(String)

        const res = await updateCoopLevelMembers(this.currentNode.id, postIds)
        if (!res || res.code !== 0) {
          this.$message.error(res?.msg || res?.message || '挂靠失败，请重试')
          return
        }

        this.$message.success(`成功挂靠 ${ids.length} 个协同岗`)
        this.bindDialog.visible = false
        await this.fetchMembers(this.currentNode.id)
      } catch (e) {
        this.$message.error(e?.message || '挂靠失败，请重试')
      } finally {
        this.bindDialog.submitLoading = false
      }
    },

    /** dropdown 展开/收起回调 */
    handleDropdownVisibleChange(visible, nodeId) {
      this.openDropdownId = visible ? nodeId : null
    },
     /** 根据身份证号查询用户信息 */
    async getUserInfoByIdCard() {
      try {
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        this.userInfo = userRes?.data
        const userDepartments = userRes?.data?.userDepartments
        if (userDepartments?.length > 0) {
          this.departmentCode = userDepartments[0].departmentCode
          this.departmentId = userDepartments[0].departmentId
        }
      } catch (error) {
        console.log(error)
      }
    },
  }
}
</script>

<style lang="scss" scoped>
/* =====================================================
   整体容器：横向弹性布局
   ===================================================== */
.col-level-manage {
  display: flex;
  height: 100%;
  overflow: hidden;
  background: #ffffff;
  /* ← 改为白色 */
  border-radius: 6px;
}

/* =====================================================
   公共 panel 样式
   ===================================================== */
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
    /* ← 防止被压缩 */
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

/* =====================================================
   左侧树面板
   ===================================================== */
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

/* ---- 自定义树节点 ---- */
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
    min-width: 0; /* 确保 flex 项目可以收缩 */
  }

  /* more 图标：默认隐藏 */
  .node-more {
    flex-shrink: 0;
    visibility: hidden;
    margin-left: 4px;

    /* ★ dropdown 展开时：强制显示 + 高亮背景 */
    &.is-open {
      visibility: visible;

      .node-more-icon {
        background: #f5f5f5;
      }
    }
  }

  /* hover 整行时显示 more 图标 */
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

/* 选中节点：不额外显示 more icon，移除旧的 is-current 规则 */
::v-deep .el-tree-node.is-current>.el-tree-node__content {
  background: #e6f0fe;
  color: #409eff;
  font-weight: 600;

  .node-folder-icon {
    color: #409eff;
  }
}

::v-deep .el-tree-node.is-current>.el-tree-node__content {
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

  // 统一图标占位宽度，文字自然对齐
  [class^="el-icon-"],
  [class*=" el-icon-"] {
    display: inline-block;
    width: 20px; // 固定宽度
    font-size: 14px;
    text-align: center;
    margin-right: 6px;
    flex-shrink: 0;
  }
}

/*节点高度*/
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

/* =====================================================
   拖拽分割线
   ===================================================== */
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

/* =====================================================
   右侧协同岗面板                  ← 关键修复
   模板中用的是 .level-content-panel，这里要对应上
   ===================================================== */
.level-content-panel {
  @extend %panel-base;
  flex: 1;
  /* 占满剩余宽度 */
  min-width: 0;
  /* 防止 flex 子项溢出 */
  margin: 8px 8px 8px 0;
  display: flex;
  flex-direction: column;
}

/* 未选中层级时的空状态 */
.empty-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 表格容器：撑满剩余高度 */
.table-wrap {
  flex: 1;
  overflow: hidden;
  padding: 0;
  /* el-table 的 height="100%" 需要父元素有明确高度 */
  display: flex;
  flex-direction: column;

  ::v-deep .el-table {
    flex: 1;

    .el-table__header-wrapper th {
      background: #f7f8fa;
      color: #5e6d82;
      font-weight: 600;
      font-size: 13px;
    }

    .el-table__body tr:hover>td {
      background: #f0f7ff;
    }

    td {
      font-size: 13px;
      color: #1f2329;
    }
  }
}

/* 分页 */
.pagination-wrap {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 10px 14px;
  border-top: 1px solid #e8ecf0;
}

/* 挂靠弹窗内部 */
.bind-dialog-body {
  padding: 0;
}

.bind-search-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #ebeef5;
}

::v-deep .bind-table {
  .el-table__header-wrapper th {
    background: #f7f8fa;
    color: #5e6d82;
    font-weight: 600;
    font-size: 13px;
  }

  td {
    font-size: 13px;
  }
}

.bind-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  width: 100%;

  .bind-selected-tip {
    font-size: 12px;
    color: #8a9ab5;

    strong {
      color: #409eff;
      font-weight: 700;
    }
  }
}
.search-wrap {
  display: flex;
  margin-right: 12px;
}
/* 弹窗样式 */
::v-deep .el-dialog{
  display: flex;
  display: -ms-flex; /* 兼容IE */
  flex-direction: column;
  -ms-flex-direction: column; /* 兼容IE */
  margin:0 !important;
  position:absolute;
  top:50%;
  left:50%;
  transform:translate(-50%,-50%);
  max-height:calc(100% - 30px);
  max-width:calc(100% - 30px);
}
::v-deep .el-dialog .el-dialog__body{
  max-height: 100%;
  flex: 1;
  -ms-flex: 1 1 auto; /* 兼容IE */
  overflow-y: auto;
  overflow-x: hidden;
}
</style>
