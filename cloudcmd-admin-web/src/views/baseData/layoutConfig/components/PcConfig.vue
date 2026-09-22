<template>
  <div class="pc-config">
    <div class="config-header">
      <div class="title">自定义页签</div>
      <el-button type="primary" @click="addTab">新增页签</el-button>
    </div>
    <el-table v-loading="listLoading" class="config-table" border :data="tabList" style="width: 100%;" align="center">
      <el-table-column type="index" label="序号" width="60" align="center"/>
      <el-table-column prop="name" label="名称" width="200" align="center"/>
      <el-table-column prop="url" label="页面URL" align="center"/>
      <el-table-column prop="order" label="排序" width="120" align="center"/>
      <el-table-column prop="openWay" label="打开方式" width="120" align="center">
        <template slot-scope="scope">
          {{ scope.row.openWay === 1 ? '弹窗' : 'iframe嵌入' }}
        </template>
      </el-table-column>
      <el-table-column prop="" label="操作" width="200" align="center">
        <template slot-scope="scope">
          <el-button type="primary" size="mini" @click="editTab(scope.row, scope.$index)">编辑</el-button>
          <el-button type="danger" size="mini" @click="deleteTab(scope.$index)">删除</el-button> 
        </template>
      </el-table-column>
    </el-table>
    <!-- <div v-if="tabList.length === 0 && !listLoading" class="empty">
      <div>暂无页签数据</div>
    </div> -->
  </div>
</template>
<script>
import EditPcTabModal from './EditPcTabModal.vue'
import { createDialog } from '@/utils/createDialog'
import { getSystemConfig, setSystemConfig } from '@/api/h5/layoutConfig'

const addTabDialog = createDialog(EditPcTabModal, { title: '新增页签', width: '500px' })
const editTabDialog = createDialog(EditPcTabModal, { title: '编辑页签', width: '500px' })

export default {
  name: 'PcConfig',
  data() {
    return {
      tabList: [],
      listLoading: false,
      configItem: null, // PC_NAV_CUSTOM 配置项
    }
  },
  mounted() {
    this.getConfig()
  },
  methods: {
    // 获取配置
    async getConfig() {
      try {
        this.listLoading = true
        const res = await getSystemConfig()
        const result = res.data || []
        const item = result.find(d => d.key === 'PC_NAV_CUSTOM')
        this.configItem = item
        if (item && item.value) {
          try {
            this.tabList = JSON.parse(item.value)
          } catch (e) {
            this.tabList = []
          }
        } else {
          this.tabList = []
        }
      } finally {
        this.listLoading = false
      }
    },
    // 新增页签
    async addTab() {
      try {
        const formData = await addTabDialog({ props: { existingNames: this.tabList.map(t => t.name) } })
        // 名称重复校验
        if (this.tabList.some(t => t.name === formData.name)) {
          return this.$message.warning('页签名称已存在，请使用其他名称')
        }
        const newList = [...this.tabList, formData]
        await this.saveConfig(newList, '新增页签成功')
      } catch (e) {
        console.log(e)
      }
    },
    // 编辑页签
    async editTab(row, index) {
      try {
        const formData = await editTabDialog({ 
          props: { 
            tabData: row,
            existingNames: this.tabList.map((t, i) => i === index ? null : t.name).filter(Boolean)
          } 
        })
        // 名称重复校验（排除当前编辑项）
        if (this.tabList.some((t, i) => i !== index && t.name === formData.name)) {
          return this.$message.warning('页签名称已存在，请使用其他名称')
        }
        const newList = [...this.tabList]
        newList[index] = formData
        await this.saveConfig(newList, '编辑页签成功')
      } catch (e) {
        console.log(e)
      }
    },
    // 删除页签
    async deleteTab(index) {
      try {
        await this.$confirm(
          '确认删除该页签吗？',
          '删除确认',
          { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
        )
        const newList = this.tabList.filter((_, i) => i !== index)
        await this.saveConfig(newList, '删除页签成功')
      } catch (e) {
        console.log(e)
      }
    },
    // 保存配置
    async saveConfig(newList, tips) {
      if (!this.configItem || !this.configItem.id) {
        return this.$message.error('配置项不存在，无法保存')
      }
      try {
        this.listLoading = true
        // 按 order 升序排序后再保存
        const sortedList = [...newList].sort((a, b) => a.order - b.order)
        const res = await setSystemConfig({
          id: this.configItem?.id,
          value: JSON.stringify(sortedList)
        })
        if (res.code === 0) {
          this.getConfig()
          tips && this.$message.success(tips)
        }
      } finally {
        this.listLoading = false
      }
    }
  }
}
</script>
<style scoped>
.pc-config {
  padding: 0px;
  min-height: 500px;
}
.config-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .title {
    font-size: 18px;
    font-weight: 500;
  }
}
.config-table {
  margin-top: 20px;
}
.empty {
  color: #909399;
  height: 400px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}
</style>