<template>
  <div class="table-container">
    <el-table
      v-loading="loading"
      :data="flattenTreeData"
      :span-method="spanMethod"
      border
      :stripe="true"
    >
      <el-table-column align="center" prop="level1" label="一级标签">
        <template slot-scope="scope">
          <span>{{ scope.row.level1 }}</span>
          <span
            v-if="scope.row.leaft === 1"
            class="look-btn"
            @click="showDialogFunc(scope.row)"
          >
            查看关联
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" prop="level2" label="二级标签">
        <template slot-scope="scope">
          <span>{{ scope.row.level2 }}</span>
          <span
            v-if="scope.row.leaft === 2"
            class="look-btn"
            @click="showDialogFunc(scope.row)"
          >
            查看关联
          </span>
        </template>
      </el-table-column>
      <el-table-column align="center" prop="level3" label="三级标签">
        <template slot-scope="scope">
          <span>{{ scope.row.level3 }}</span>
          <span
            v-if="scope.row.leaft === 3"
            class="look-btn"
            @click="showDialogFunc(scope.row)"
          >
            查看关联
          </span>
        </template></el-table-column
      >
      <el-table-column label="操作" align="center">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.scope == 0 || scope.row.scope == 1"
            type="primary"
            icon="el-icon-link"
            size="small"
            @click="handleEditCood(scope.row)"
          >
            关联协同岗
          </el-button>
          <el-button
            v-if="scope.row.scope == 0 || scope.row.scope == 2"
            type="primary"
            icon="el-icon-link"
            size="small"
            @click="handleEditPolice(scope.row)"
          >
            关联警员
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <bindCoodForm ref="editCoodRef" @success="getList" />
    <bindPoliceForm ref="editPoliceRef" @success="getList" />
    <colloration-label ref="collorationLabelRef" />
  </div>
</template>

<script>
import { labelList } from '@/api/h5/quick'
import bindCoodForm from './bindCoodForm'
import bindPoliceForm from './bindPoliceForm'
import collorationLabel from './collorationLabel'

export default {
  name: 'LookLabel',
  components: { bindCoodForm, bindPoliceForm, collorationLabel },
  data() {
    return {
      loading: true,
      // 原始四级树形结构数据
      treeData: [],
      // 扁平化后的表格数据
      flattenTreeData: [],
      // 合并单元格配置
      spanConfig: {},
      
    }
  },
  computed: {
    // 计算属性：获取当前数据的扁平化版本
    flattenedData() {
      const result = []
      this.treeData.forEach(level1 => {
        const level1Children = level1.children || []

        level1Children.forEach(level2 => {
          const level2Children = level2.children || []

          level2Children.forEach(level3 => {
            const level3Children = level3.children || []

            level3Children.forEach(level4 => {
              result.push({
                level1: level1.name,
                level2: level2.name,
                level3: level3.name,
                scope: level4.scope,
                rawData: { level1, level2, level3, level4 }
              })
            })

            // 如果三级节点没有子节点
            if (level3Children.length === 0) {
              result.push({
                level1: level1.name,
                level2: level2.name,
                level3: level3.name,
                scope: level3.scope,
                rawData: { level1, level2, level3 },
                leaft: 3,
                levelId: level3.id
              })
            }
          })

          // 如果二级节点没有子节点
          if (level2Children.length === 0) {
            result.push({
              level1: level1.name,
              level2: level2.name,
              level3: '',
              scope: level2.scope,
              rawData: { level1, level2 },
              leaft: 2,
              levelId: level2.id
            })
          }
        })

        // 如果一级节点没有子节点
        if (level1Children.length === 0) {
          result.push({
            level1: level1.name,
            level2: '',
            level3: '',
            scope: level1.scope,
            rawData: { level1 },
            leaft: 1,
            levelId: level1.id
          })
        }
      })

      return result
    }
  },
  watch: {
    // 监听树形数据变化，重新计算扁平化数据和合并配置
    treeData: {
      handler() {
        this.updateTableData()
      },
      deep: true
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    showDialogFunc(data) {
      this.$refs.collorationLabelRef.showDialogFuncs(data)
    },
    // 更新表格数据和合并配置
    updateTableData() {
      this.flattenTreeData = this.flattenedData
      this.calculateSpanConfig()
    },

    // 计算合并单元格配置
    calculateSpanConfig() {
      const config = {}
      const data = this.flattenTreeData

      // 处理一级节点合并
      let i = 0
      while (i < data.length) {
        const currentValue = data[i].level1
        let j = i + 1

        while (j < data.length && data[j].level1 === currentValue) {
          j++
        }

        const rowspan = j - i
        config[`level1-${i}`] = { rowspan, colspan: 1 }

        // 设置需要隐藏的单元格
        for (let k = i + 1; k < j; k++) {
          config[`level1-${k}`] = { rowspan: 0, colspan: 0 }
        }

        i = j
      }

      // 处理二级节点合并
      i = 0
      while (i < data.length) {
        const currentValue = data[i].level2

        // 如果二级节点为空，直接跳过
        if (!currentValue) {
          i++
          continue
        }

        let j = i + 1

        while (
          j < data.length &&
          data[j].level2 === currentValue &&
          data[j].level1 === data[i].level1
        ) {
          j++
        }

        const rowspan = j - i
        config[`level2-${i}`] = { rowspan, colspan: 1 }

        // 设置需要隐藏的单元格
        for (let k = i + 1; k < j; k++) {
          config[`level2-${k}`] = { rowspan: 0, colspan: 0 }
        }

        i = j
      }

      // 处理三级节点合并
      i = 0
      while (i < data.length) {
        const currentValue = data[i].level3

        // 如果三级节点为空，直接跳过
        if (!currentValue) {
          i++
          continue
        }

        let j = i + 1

        while (
          j < data.length &&
          data[j].level3 === currentValue &&
          data[j].level2 === data[i].level2 &&
          data[j].level1 === data[i].level1
        ) {
          j++
        }

        const rowspan = j - i
        config[`level3-${i}`] = { rowspan, colspan: 1 }

        // 设置需要隐藏的单元格
        for (let k = i + 1; k < j; k++) {
          config[`level3-${k}`] = { rowspan: 0, colspan: 0 }
        }

        i = j
      }

      this.spanConfig = config
    },

    // 单元格合并方法
    spanMethod({ row, column, rowIndex, columnIndex }) {
      if (columnIndex >= 0 && columnIndex <= 2) {
        const levelMap = ['level1', 'level2', 'level3']
        const key = levelMap[columnIndex]
        const config = this.spanConfig[`${key}-${rowIndex}`]

        if (config) {
          return {
            rowspan: config.rowspan,
            colspan: config.colspan
          }
        }
      }

      return {
        rowspan: 1,
        colspan: 1
      }
    },

    // 编辑协同岗
    handleEditCood(row) {
      const { level1, level2, level3 } = row.rawData
      this.$refs.editCoodRef.open(
        'update',
        level3 || level2 || level1,
        '关联协同岗'
      )
    },
    // 编辑警员
    handleEditPolice(row) {
      const { level1, level2, level3 } = row.rawData
      this.$refs.editPoliceRef.open(
        'update',
        level3 || level2 || level1,
        '关联警员'
      )
    },

    async getList(param) {
      this.loading = true
      const { code, data } = await labelList(param)
      if (code === 0) {
        this.treeData = data
        // 组件挂载后初始化表格数据
        this.updateTableData()
        this.loading = false
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.table-container {
  max-height: calc(100vh - 260px);
  overflow-y: auto;

  .btns {
    margin-bottom: 16px;
  }

  .look-btn {
    display: inline-block;
    width: 68px;
    height: 22px;
    line-height: 22px;
    opacity: 1;
    border-radius: 2px;
    background: rgba(235, 238, 247, 1);
    font-size: 12px;
    font-weight: 400;
    letter-spacing: 0px;
    color: rgba(134, 139, 152, 1);
    cursor: pointer;
    text-align: center;

    &:hover {
      background: rgba(235, 238, 247, 1);
      color: rgba(38, 78, 209, 1);
    }
    &:active {
      background: rgba(235, 238, 247, 1);
      color: rgba(38, 78, 209, 1);
    }
  }
}
</style>
