<template>
  <div class="section-manage">
    <!-- 工具栏：标题统计 + 新增按钮 -->
    <div class="section-manage__toolbar">
      <div class="section-manage__meta">
        <span class="section-manage__count">共 {{ sectionList.length }} 个板块</span>
      </div>
      <el-button
        type="primary"
        icon="el-icon-plus"
        @click="addLayoutSection"
      >
        新增板块
      </el-button>
    </div>

    <el-table
      v-loading="listLoading"
      class="section-manage__table"
      :data="sectionList"
      style="width: 100%;"
      align="center"
      :header-cell-style="{ background: '#fafbfc', color: '#303133', fontWeight: 600 }"
    >
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column prop="name" label="名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="show" label="状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.show === 1 ? 'success' : 'danger'" size="mini" effect="light">
            {{ scope.row.show === 1 ? '显示' : '隐藏' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="板块类型" width="140" align="center">
        <template slot-scope="scope">
          <span class="section-manage__type-tag">{{ AppH5ConfigTypeMap[scope.row.type] }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="url" label="跳转 URL" min-width="200" show-overflow-tooltip />
      <el-table-column prop="sort" label="顺序" align="center" width="80" />
      <el-table-column label="操作" width="160" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" icon="el-icon-edit" @click="editLayoutSection(scope.row)">编辑</el-button>
          <el-button type="text" class="section-manage__btn-danger" icon="el-icon-delete" @click="deleteLayoutSection(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <div v-if="!listLoading && sectionList.length === 0" class="section-manage__empty">
      <i class="el-icon-files section-manage__empty-icon"></i>
      <p class="section-manage__empty-text">暂无板块数据</p>
      <el-button type="text" @click="addLayoutSection">新增第一个板块</el-button>
    </div>
  </div>
</template>
<script>
import EditSectionModal from './EditSectionModal.vue'
import { createDialog } from '@/utils/createDialog'
import { createSection, updateSection, deleteSection, getSectionList } from '@/api/h5/layoutConfig'
const addSectionDialog = createDialog(EditSectionModal, { title: '新增板块', width: '600px' })
const editSectionDialog = createDialog(EditSectionModal, { title: '编辑板块', width: '600px' })
export default {
  name: 'SectionManage',
  data() {
    return {
      sectionList: [],
      listLoading: false,
      detailLoading: false,
      addLoading: false,
      updateLoading: false,
      deleteLoading: false,
    }
  },
  computed: {
    AppH5ConfigTypeMap() {
      return {
        1: '轮播图',
        2: '常用应用',
        3: '协同群组',
        4: '三方网页',
        5: '分割条',
        6: '消息列表',
      }
    }
  },
  mounted() {
    this.getLayoutSectionList(-1)
  },
  methods: {
    // 查询板块列表
    async getLayoutSectionList(show) {
      try {
        this.listLoading = true
        const res = await getSectionList({ show })
        console.log(res, 'getSectionList:res')
        this.sectionList = res.data || []
      } finally {
        this.listLoading = false
      }
    },
    // 新增板块
    async addLayoutSection() {
      // 拿到弹窗返回结果
      try {
        const formData = await addSectionDialog()
        const custom = formData.custom ? JSON.stringify(formData.custom) : ''
        const addRes = await createSection({
          name: formData.name,
          type: formData.type,
          url: formData.url,
          custom: custom,
          sort: formData.sort,
          show: formData.show,
        })
        addRes.code === 0 ? this.$message.success('新增板块成功') : this.$message.error(addRes.msg || '新增板块失败')
        this.getLayoutSectionList(-1)
      } catch (e) {
        console.log(e)
      }
    },
    // 编辑板块
    async editLayoutSection(row) {
      // 传参数，拿到弹窗返回结果
      try {
        const formData = await editSectionDialog({ props: { sectionId: row.id }})
        console.log(formData, 'editLayoutSection formData')
        const custom = formData.custom ? JSON.stringify(formData.custom) : ''
        const editRes = await updateSection(row.id, {
          name: formData.name,
          type: formData.type,
          url: formData.url,
          custom: custom,
          sort: formData.sort,
          show: formData.show,
        })
        editRes.code === 0 ? this.$message.success('更新板块成功') : this.$message.error(editRes.msg || '更新板块失败')
        this.getLayoutSectionList(-1)
      } catch (e) {
        // 编辑弹窗取消时无需处理
      }
    },
    // 删除板块
    async deleteLayoutSection(row) {
      // 确认删除
      await this.$confirm(
        '确认删除该板块吗？',
        '删除确认',
        { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
      )
      try {
        this.deleteLoading = true
        // 调用删除接口
        if (!row.id) return this.$message.error('删除板块失败，参数id不存在')
        const delRes = await deleteSection(row.id)
        delRes.code === 0 ? this.$message.success('删除板块成功') : this.$message.error(delRes.msg || '删除板块失败')
        this.getLayoutSectionList(-1)
      } finally {
        this.deleteLoading = false
      }
    },
  }
}
</script>
<style lang="scss" scoped>
.section-manage {
  padding: 4px 0 16px;
}

// 工具栏：统计 + 新增按钮
.section-manage__toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 12px;
}

.section-manage__meta {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.section-manage__count {
  font-size: 13px;
  color: #606266;
  font-weight: 500;
}

// 表格
.section-manage__table {
  border-radius: 4px;
  overflow: hidden;

  ::v-deep .el-table__header th {
    border-bottom: 1px solid #ebeef5;
  }
}

// 板块类型标签（轻量 inline 样式）
.section-manage__type-tag {
  display: inline-block;
  padding: 2px 10px;
  background: #f4f6fa;
  color: #606266;
  border-radius: 3px;
  font-size: 12px;
  line-height: 18px;
}

// 操作列：危险按钮红色文字
.section-manage__btn-danger {
  color: #f56c6c;

  &:hover {
    color: #f78989;
  }
}

// 空状态
.section-manage__empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 56px 0 32px;
  color: #909399;
  font-size: 14px;
}

.section-manage__empty-icon {
  font-size: 48px;
  color: #c0c4cc;
  margin-bottom: 12px;
}

.section-manage__empty-text {
  margin: 0 0 8px;
  color: #606266;
}
</style>
