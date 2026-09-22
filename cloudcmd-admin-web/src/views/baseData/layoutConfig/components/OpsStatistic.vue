<template>
  <div class="ops-statistic">
    <div class="filter-container">
      <el-date-picker
        v-model="dateRange"
        type="datetimerange"
        range-separator="至"
        start-placeholder="开始时间"
        end-placeholder="结束时间"
        value-format="yyyy-MM-dd HH:mm:ss"
        :picker-options="pickerOptions"
        :default-time="['00:00:00', '23:59:59']"
        style="width: 560px;"
      />
      <el-button
        type="primary"
        icon="el-icon-download"
        :loading="exportLoading"
        @click="handleExport"
      >
      <!-- :disabled="!dateRange || dateRange.length < 2" -->
        导出
      </el-button>
    </div>
  </div>
</template>

<script>
import { exportLoginStatistic } from '@/api/statistic'

// 根据日期计算同月日前一年的日期
function getPrevYearDate(date) {
  const d = new Date(date)
  d.setFullYear(d.getFullYear() - 1)
  return d
}

// 根据日期计算同月日后一年的日期
function getNextYearDate(date) {
  const d = new Date(date)
  d.setFullYear(d.getFullYear() + 1)
  return d
}

// 判断两个日期的日历跨度是否超过一年（同月日±1年）
function isOverOneYear(start, end) {
  const startDate = new Date(start)
  startDate.setHours(0, 0, 0, 0)
  const endDate = new Date(end)
  endDate.setHours(0, 0, 0, 0)
  const minAllowed = getPrevYearDate(startDate)
  const maxAllowed = getNextYearDate(startDate)
  return endDate.getTime() > maxAllowed.getTime() || endDate.getTime() < minAllowed.getTime()
}

export default {
  name: 'OpsStatistic',
  data() {
    return {
      dateRange: null,
      exportLoading: false,
      pickDate: null,
      pickerOptions: {
        // 记录首次选择的日期，用于限制最大跨度
        onPick: ({ minDate }) => {
          this.pickDate = minDate
        },
        disabledDate: (time) => {
          // 将今天结束时刻作为不可选的判断基准，确保当天日期始终可选
          const endOfToday = new Date()
          endOfToday.setHours(23, 59, 59, 999)
          if (this.pickDate) {
            // 基于首次选择的日期，计算同月日±1年的范围
            const pickDayStart = new Date(this.pickDate)
            pickDayStart.setHours(0, 0, 0, 0)
            const maxDate = getNextYearDate(pickDayStart)
            maxDate.setHours(23, 59, 59, 999)
            const minDate = getPrevYearDate(pickDayStart)
            minDate.setHours(0, 0, 0, 0)
            return time.getTime() > maxDate.getTime() || time.getTime() < minDate.getTime() || time.getTime() > endOfToday.getTime()
          }
          return time.getTime() > endOfToday.getTime()
        }
      }
    }
  },
  watch: {
    // 清除日期时重置pickDate，避免残留导致日期面板仍受限
    dateRange(val) {
      if (!val || val.length === 0) {
        this.pickDate = null
      }
    }
  },
  methods: {
    // 从响应头获取导出文件名
    getExportFileName(headers) {
      const contentDisposition = headers['content-disposition'] || ''
      if (!contentDisposition) return '日活数据.xlsx'
      const reg = /filename\*=\s*utf-8''([^;]+)|filename="?([^;"]+)"?/i
      const match = contentDisposition.match(reg)
      if (!match) return '日活数据.xlsx'
      const raw = (match[1] || match[2] || '').trim()
      const fileName = decodeURIComponent(raw)
      return fileName || '日活数据.xlsx'
    },
    async handleExport() {
      // 有选择时间时校验跨度不超过一年（同月日±1年）
      if (this.dateRange && this.dateRange.length === 2) {
        if (isOverOneYear(this.dateRange[0], this.dateRange[1])) {
          this.$message.warning('时间范围不能超过一年')
          return
        }
      }
      this.exportLoading = true
      try {
        const params = {
          startTime: this.dateRange?.[0] || '',
          endTime: this.dateRange?.[1] || ''
        }
        const data = await exportLoginStatistic(params)
        console.log(data.type)
        if (!data) {
          this.$message.error('导出失败')
          return
        }
        // 判断后端是否返回了JSON错误体而非Excel文件
        if (data.type && data.type.includes('application/json')) {
          const text = await new Response(data).text()
          const err = JSON.parse(text)
          this.$message.error(err.msg || '导出失败')
          return
        }
        const fileName = this.getExportFileName(data?.headers)
        const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.style.display = 'none'
        link.href = url
        link.setAttribute('download', fileName)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error) {
        console.error('导出日活数据失败:', error)
        this.$message.error('导出失败')
      } finally {
        this.exportLoading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-container {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
}
</style>
