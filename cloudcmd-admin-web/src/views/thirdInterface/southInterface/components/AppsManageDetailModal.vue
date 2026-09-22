<template>
  <el-dialog v-loading="loading" :visible.sync="dialogVisible" title="应用详情" width="800px" :close-on-click-modal="false"
    @close="handleClose">
    <!-- 基本信息 -->
    <el-descriptions class="custom-descriptions" title="基本信息" :column="2" border>
      <el-descriptions-item v-for="item in basicInfoList" :key="item.label" :label="item.label">
        {{ item.value }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 接口配置 -->
    <el-descriptions class="custom-descriptions" border v-if="row.type === 1" title="接口配置" :column="2" style="margin-top: 20px">
      <el-descriptions-item v-for="item in interfaceInfoList" :key="item.label" :label="item.label">
        {{ item.value }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 数据库配置 -->
    <el-descriptions class="custom-descriptions" v-if="row.type === 2" title="数据库配置" :column="2" border style="margin-top: 20px">
      <el-descriptions-item v-for="item in databaseInfoList" :key="item.label" :label="item.label">
        {{ item.value }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 字段映射配置 -->
    <el-descriptions class="custom-descriptions" v-if="parsedMapperList.length > 0" title="字段映射配置" :column="2" border style="margin-top: 20px">
      <el-descriptions-item v-for="(item, index) in parsedMapperList" :key="index" :label="item.value">
        {{ item.key }}
      </el-descriptions-item>
    </el-descriptions>

    <!-- 其他信息 -->
    <el-descriptions class="custom-descriptions" title="其他信息" :column="2" border style="margin-top: 20px">
      <el-descriptions-item label="创建时间" :span="2">
        {{ row.gmtCreated || '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </el-dialog>
</template>

<script>
import { getCallableAppDetail } from '@/api/thirdInterface/southInterface.js'
export default {
  data() {
    return {
      dialogVisible: false,
      row: {},
      loading: false,
      periodMap: {
        30: '30min',
        60: '1h',
        360: '6h',
        720: '12h',
        1440: '24h'
      }
    }
  },
  computed: {
    basicInfoList() {
      return [
        { label: '名称', value: this.row.name || '-' },
        { label: '所属系统', value: this.row.systemName || '-' },
        { label: '系统编码', value: this.row.systemCode || '-' },
        { label: '应用类型', value: { 1: 'RESTful接口', 2: '数据库' }[this.row.type] || '-' },
        { label: '展示范围', value: this.row.scope === 0 ? '全部' : this.row.scope === 1 ? 'PC端' : '-' },
        { label: '唯一标识字段', value: this.row.uniqueId || '-' },
        { label: '数据刷新周期', value: this.periodMap[this.row.period] || '-' },
        { label: 'IP', value: this.row.ip || '-' },
        { label: '端口', value: this.row.port || '-' }
      ]
    },
    interfaceInfoList() {
      return [
        { label: '访问协议', value: this.row.protocol || '-' },
        { label: '请求方式', value: this.row.method || '-' },
        { label: '接口 URI', value: this.row.uri || '-' },
        { label: '请求头', value: this.row.reqHeader || '-' },
        { label: '请求体', value: this.row.reqBody || '-' },
        { label: '请求参数', value: this.row.reqParam || '-' },
        { label: '是否分页', value: this.row.pagenation === 1 ? '是' : '否' || '-' },
        { label: '分页类型', value: this.row.pagenationType === 1 ? '页码模式' : this.row.pagenationType === 2 ? '偏移模式' : '-' },
        { label: '分页参数类型', value: this.row.pageParamLocation === 1 ? 'Body 参数' : 'Query 参数' || '-' },
        { label: '页码字段', value: this.row.pageFieldName || '-' },
        { label: '每页条数字段', value: this.row.pageSizeFieldName || '-' },
        { label: '响应数据路径', value: this.row.responseDataPath || '-' },
      ]
    },
    databaseInfoList() {
      return [
        { label: '数据库类型', value: this.getDbTypeLabel(this.row.dbType) },
        { label: '账号', value: this.row.account || '-' },
        { label: '表名/视图名', value: this.row.dataName || '-' },
        { label: '数据库名', value: this.row.databaseName || '-' },
      ]
    },
    parsedMapperList() {
      if (!this.row.mapper) return []
      try {
        const parsed = JSON.parse(this.row.mapper)
        if (Array.isArray(parsed)) {
          return parsed.map(item => ({
            key: item.key || item.source || '-',
            value: item.value || item.target || '-'
          }))
        }
        if (typeof parsed === 'object') {
          return Object.entries(parsed).map(([key, value]) => ({
            key,
            value: typeof value === 'object' ? JSON.stringify(value) : String(value)
          }))
        }
        return []
      } catch (e) {
        return []
      }
    }
  },
  
  methods: {
    open(row) {
      this.dialogVisible = true
      this.getAppDetail(row.id)
    },
    handleClose() {
      this.dialogVisible = false
    },
    getDbTypeLabel(dbType) {
      const typeMap = {
        1: 'MySQL',
        2: 'Oracle',
        3: 'PostgreSQL',
        4: 'SQL Server'
      }
      return typeMap[dbType] || dbType || '-'
    },
    async getAppDetail(id) {
      try {
        this.loading = true
        const res = await getCallableAppDetail(id)
        this.loading = false
        const { data, msg, code} = res || {}
        this.row = code === 0 ? data : {}
        if(code !== 0) this.$message.error(msg)
      } catch(e){
        this.loading = false
        this.row = {}
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.el-descriptions ::v-deep .el-descriptions__table,.is-bordered {
  // 将表格的 table-layout 设置为 fixed，这样单元格的宽度将基于显式的宽度设置，而不是内容
  table-layout: fixed;
}
.el-descriptions ::v-deep .el-descriptions__body .el-descriptions__table .el-descriptions__cell {
  // 内容换行，而不是溢出
  word-break: break-all;
}

.el-descriptions ::v-deep .el-descriptions__label.el-descriptions__cell.is-bordered-label {
  width: 200px;
}

::v-deep.el-dialog__wrapper {
  text-align: center;
  white-space: nowrap;
  overflow: auto;
  &:after {
    content: "";
    display: inline-block;
    vertical-align: middle;
    height: 100%;
  }
  .el-dialog {
    margin: auto !important;
    display: inline-block;
    vertical-align: middle;
    text-align: center;
    white-space: normal;
    .el-dialog__body {
      // 处理内容溢出
      max-height: 70vh;  // 限制最大高度为视口的70%
      overflow-y: auto;   // 内容过多时显示垂直滚动条
      overflow-x: hidden; // 隐藏水平滚动条
      padding: 20px;      // 保持原有内边距
      
      // 处理长文本溢出
      word-wrap: break-word;      // 允许长单词换行
      word-break: break-all;       // 强制换行（针对特别长的连续字符）
      
      // 优雅的滚动条样式（可选）
      &::-webkit-scrollbar {
        width: 6px;
      }
      &::-webkit-scrollbar-thumb {
        background-color: #dcdfe6;
        border-radius: 3px;
      }
      &::-webkit-scrollbar-track {
        background-color: #f5f7fa;
      }
    }
  }
}

// 针对特定长内容的处理
::v-deep .el-descriptions .el-descriptions__cell {
  .el-descriptions__content {
    display: inline-block;
    max-width: 100%;
    overflow-wrap: break-word;
    word-wrap: break-word;
    word-break: break-all;
  }
}
</style>