<template>
  <el-select v-model="localValue" :placeholder="placeholder" :multiple="multiple" :style="{ width: '100%' }"
    v-loadmore="handleScroll" filterable remote :remote-method="remoteMethod" @change="handleChange" @visible-change="visibleChange">
    <el-option v-for="item in options" :key="item[bindField]" :label="item[showField]" :value="item[bindField]" />
  </el-select>
</template>

<script>
export default {
  name: 'SelectPagination',
  props: {
    // 绑定值
    value: {
      type: [String, Number, Array],
      default: () => multiple ? [] : ''
    },
    // 已选项信息，用于回显
    valueMap: {
      type: Object,
      default: () => ({})
    },
    // 是否多选
    multiple: {
      type: Boolean,
      default: false
    },
    // 占位符
    placeholder: {
      type: String,
      default: '请选择'
    },
    // api：数据源获取方法
    // (params) => new Promise((resolve, reject) => {resolve({data)})
    // params 为分页参数，包含 {pageNum, pageSize, keywords, ...initParams}
    // data 为接口返回数据，数据格式为 {pages, records: [], size, total}
    api: {
      type: Function,
      required: true
    },
    // 初始参数
    initParams: {
      type: Object,
      default: () => ({})
    },
    // 页大小
    pageSize: {
      type: Number,
      default: 100
    },
    // 唯一标识字段名，默认为 'id'
    itemKey: {
      type: String,
      default: 'id'
    },
    // 显示字段名，用于 el-option 的 label
    showField: {
      type: String,
      default: 'name'
    },
    // 绑定字段名，用于 el-option 的 value
    bindField: {
      type: String,
      default: 'id'
    },
    // 字段名映射，格式：[{to: '收集字段', from: '接口数据源字段'}]
    trans: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      // 本地值，用于处理双向绑定
      localValue: this.multiple ? [] : '',
      // 内部数据源
      dataList: [],
      total: 0,
      pageNum: 0,
      // 用于回显
      targetMap: {}
    }
  },
  computed: {
    // 确保已选项能正确显示
    options() {
      const arr = []
      // 如果有已选中的目标对象，但不在 dataList 中，添加到数组中
      if (this.multiple) {
        if (Array.isArray(this.localValue)) {
          this.localValue.forEach(key => {
            const index = this.dataList.findIndex(i => i[this.bindField] === key)
            if (index === -1) {
              const targetInfo = this.targetMap[key]
              if (targetInfo) {
                arr.push({
                  [this.bindField]: key,
                  [this.showField]: targetInfo[this.showField],
                  ...targetInfo
                })
              }
            }
          })
        }
      } else {
        if (this.localValue) {
          const index = this.dataList.findIndex(i => i[this.bindField] === this.localValue)
          if (index === -1) {
            const targetInfo = this.targetMap[this.localValue]
            if (targetInfo) {
              arr.push({
                [this.bindField]: this.localValue,
                [this.showField]: targetInfo[this.showField],
                ...targetInfo
              })
            }
          }
        }
      }
      return [...arr, ...this.dataList]
    }
  },
  watch: {
    // 监听外部值变化
    value: {
      handler(newVal) {
        this.localValue = newVal
      },
      immediate: true
    },
    // 监听 valueMap 变化
    valueMap: {
      handler(newVal) {
        this.targetMap = { ...this.targetMap, ...newVal }
      },
      immediate: true
    },
    // 监听 initParams 变化，当参数变化时重新加载数据
    // initParams: {
    //   handler() {
    //     this.fetchInitialData()
    //   },
    //   deep: true
    // }
  },
  mounted() {
    // 初始化数据
    this.fetchInitialData()
  },
  methods: {
    // 初始化数据
    fetchInitialData() {
      this.clearLoading()
      this.api({
        pageNum: 1,  // 总是从第一页开始
        pageSize: this.pageSize,
        ...this.initParams
      }).then(res => {
        this.pageNum = 1  // 重置为第一页
        this.handleData(res)
      }).catch(err => {
        console.error('fetchInitialData catch error:', err)
      })
    },
    // 处理数据
    handleData(res) {
      // 检查接口返回的数据格式
      let list = []
      let total = 0

      // 首先检查接口调用是否成功
      if (res) {
        // 只处理标准格式的返回数据：{pages: num, records: [], size: num, total: num}
        console.log('handleData res:', res)
        if (res?.records) {
          // 标准分页格式
          list = Array.isArray(res.records) ? res.records : []
          total = res.total || 0
        } else {
          // 非标格式，用户自行处理
          console.log('handleData: 非标格式返回数据，用户需自行处理')
          list = []
          total = 0
        }
      } else {
        console.log('handleData: 接口返回为空')
      }

      // 构建字段映射关系
      const fieldMap = {}
      this.trans.forEach(item => {
        if (item?.from && item?.to) {
          fieldMap[item.from] = item.to
        }
      })

      // 更新映射
      list.forEach(item => {
        console.log('handleData item:', item)

        // 使用字段映射关系转换字段名
        const transformedItem = { ...item }
        Object.keys(item).forEach(key => {
          if (fieldMap[key]) {
            transformedItem[fieldMap[key]] = item[key]
            delete transformedItem[key]
          }
        })

        // 获取标识字段和名称字段
        const id = transformedItem[this.bindField]
        const name = transformedItem[this.showField]
        
        if (id && name) {
          const rest = { ...transformedItem }
          delete rest[this.bindField]
          delete rest[this.showField]
          this.targetMap[id] = { [this.showField]: name, ...rest }
        }
      })

      // 合并数据
      const newItems = list.map(item => {
        // 使用字段映射关系转换字段名
        const transformedItem = { ...item }
        Object.keys(item).forEach(key => {
          if (fieldMap[key]) {
            transformedItem[fieldMap[key]] = item[key]
            delete transformedItem[key]
          }
        })

        // 获取标识字段和名称字段
        const id = transformedItem[this.bindField]
        const name = transformedItem[this.showField]
        
        if (id && name) {
          const rest = { ...transformedItem }
          delete rest[this.bindField]
          delete rest[this.showField]
          return { ...rest, [this.bindField]: id, [this.showField]: name }
        }
      }).filter(Boolean)

      // 只有在初始化或搜索时才清空数据，滚动加载时追加数据
      if (this.pageNum === 1) {
        this.dataList = newItems
      } else {
        this.dataList = [...this.dataList, ...newItems]
      }
      this.total = total
    },
    // 清空加载状态
    clearLoading() {
      this.dataList = []
      this.total = 0
      this.pageNum = 0  // 重置为0，下次请求从第1页开始
    },
    // 触底加载
    handleScroll() {
      // 确保有更多数据可加载，且不在加载中，且当前页码小于总页数
      const hasMoreData = this.dataList.length < this.total && this.pageNum < Math.ceil(this.total / this.pageSize);
      if (hasMoreData) {
        this.api({
          pageNum: this.pageNum + 1,  // 下一页
          pageSize: this.pageSize,
          ...this.initParams
        }).then(res => {
          this.pageNum = this.pageNum + 1  // 成功后才增加页码
          this.handleData(res)
        }).catch(err => {
          console.error('handleScroll catch error:', err)
        })
      }
    },
    // 远程搜索
    remoteMethod(keywords) {
      this.clearLoading()
      this.api({
        pageNum: 1,  // 搜索总是从第一页开始
        pageSize: this.pageSize,
        keywords,
        ...this.initParams
      }).then(res => {
        this.pageNum = 1  // 重置为第一页
        this.handleData(res)
      }).catch(err => {
        console.error('remoteMethod catch error:', err)
      })
    },
    // 处理值变化
    handleChange(val) {
      this.$emit('input', val)
      this.$emit('change', val, this.getChangeInfo(val))
    },
    // 展开时重新获取数据
    visibleChange(val) {
      if (val) {
        this.fetchInitialData()
      }
    },
    // 获取变化信息
    getChangeInfo(val) {
      if (this.multiple) {
        return val.map(key => {
          const item = this.targetMap[key] || this.dataList.find(item => item[this.bindField] === key)
          return item || { [this.bindField]: key }
        })
      } else {
        const item = this.targetMap[val] || this.dataList.find(item => item[this.bindField] === val)
        return item || { [this.bindField]: val }
      }
    }
  }
}
</script>

<style scoped></style>