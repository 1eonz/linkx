<template>
  <div class="app-container">
    <!-- 搜索工作栏 -->
    <el-form
      v-if="false"
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="68px"
      @submit.prevent="handleQuery"
    >
      <el-form-item label="" prop="title">
        <el-input
          v-model="queryParams.name"
          placeholder="标签名称"
          style="width: 300px;"
          clearable
          class="filter-item"
          @clear="handleReset"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="handleQuery"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-refresh"
          @click="handleReset"
        >
          重置
        </el-button>
      </el-form-item>
    </el-form>
    <!-- 列表 -->
    <el-card>
      <el-tabs v-model="activeName">
        <el-tab-pane label="标签查看" name="1">
          <look-label ref="lookRef" />
        </el-tab-pane>
        <el-tab-pane label="标签编辑" name="2">
          <tree-label ref="treeRef" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script>
import lookLabel from './lookLabel.vue'
import treeLabel from './treeLabel.vue'

export default {
  name: 'QuickGroup',
  components: { lookLabel, treeLabel },
  data() {
    return {
      activeName: '1',
      queryParams: {
        name: ''
      }
    }
  },
  watch: {
    activeName() {
      this.handleQuery()
    }
  },
  mounted() {},
  methods: {
    async handleQuery() {
      switch (this.activeName) {
        case '1':
          this.$refs.lookRef.getList(this.queryParams)
          return
        case '2':
          this.$refs.treeRef.getList(this.queryParams)
          return
        default:
          break
      }
    },

    /** 重置按钮操作 */
    handleReset() {
      this.queryParams.name = ''
      this.handleQuery()
    }
  }
}
</script>
