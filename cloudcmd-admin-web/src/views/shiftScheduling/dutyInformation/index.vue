<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <!-- <el-input
          v-model="listQuery.postName"
          placeholder="协同岗名称"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        /> -->
        <el-input
          v-model="listQuery.userId"
          placeholder="人员ID"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.userName"
          placeholder="姓名"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQuery.type"
          placeholder="排班类型"
          style="width: 200px;"
          class="filter-item"
          clearable
          popper-class="duty-type-select-popper"
          @change="handleFilter"
        >
          <el-option
            v-for="item in dutyTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
          <!-- 加载更多提示 -->
          <div
            v-if="dutyTypeLoading"
            style="padding: 10px; text-align: center; color: #909399; font-size: 14px;"
          >
            加载中...
          </div>
          <div
            v-else-if="!dutyTypePage.hasMore && dutyTypeOptions.length > 1"
            style="padding: 10px; text-align: center; color: #909399; font-size: 14px;"
          >
            已加载全部
          </div>
        </el-select>
         <el-date-picker
          v-model="listQuery.date"
          type="daterange"
          range-separator="-"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="filter-item"
          value-format="yyyy-MM-dd"
          style="display: inline-flex;align-items: center;"
          @change="handleFilter"
        />
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-refresh"
          @click="resetQuery"
        >
          重置
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-download"
          @click="handleExport"
        >
          模板下载
        </el-button>
        <el-upload
          ref="upload"
          class="filter-item"
          action="#"
          accept=".xlsx,.xls,.csv"
          :http-request="uploadFileBtn"
          :show-file-list="false"
          :on-success="onSuccess"
          :on-error="onError"
        >
          <el-button
            style="margin-left: 10px;"
            type="primary"
            icon="el-icon-upload2"
          >
            导入
          </el-button>
        </el-upload>
        <el-button
          v-if="showType==='list'"
          style="margin-left: 10px;"
          v-waves
          class="filter-item"
          type="danger"
          icon="el-icon-delete"
          @click="delBatch"
        >
          {{ $t('index.operations.batchRemove') }}
        </el-button>
      </div>
      <div
        style="margin-bottom: 20px;display: flex;justify-content: space-between;align-items: center;"
      >
        <div class="calendarPagetation no-select">
          <template v-if="showType === 'calendar'">
            <i
              class="el-icon-arrow-left"
              style="cursor: pointer;"
              @click="goPrevMonth"
            ></i>
            <span
              >{{ calendarObj.searchObj.year }}年{{
                String(calendarObj.searchObj.month).padStart(2, '0')
              }}月</span
            >
            <i
              class="el-icon-arrow-right"
              style="cursor: pointer;"
              @click="goNextMonth"
            ></i>
          </template>
        </div>
        <el-button-group>
          <el-button
            :type="showType === 'calendar' ? 'primary' : ''"
            icon="el-icon-date"
            size="small"
            @click="showType = 'calendar'"
            >日历查看</el-button
          >
          <el-button
            :type="showType === 'list' ? 'primary' : ''"
            icon="el-icon-menu"
            size="small"
            @click="showType = 'list'"
            >列表</el-button
          >
        </el-button-group>
      </div>

      <div v-if="showType === 'calendar'" class="calendarList">
        <div class="weekName">
          <div
            v-for="(weekName, index) in calendarObj.weekDayNameList"
            :key="`weekDayNameList${index}`"
          >
            {{ weekName }}
          </div>
        </div>
        <div class="weekDay">
          <div
            v-for="(item, index) in calendarObj.prevMonthDays"
            :key="`prevMonthDays${index}`"
            class="day"
            style="border-color: transparent;"
          >
            {{ item }}
          </div>
          <div
            v-for="(item, index) in calendarObj.lastDayNum"
            :key="`lastDayNum${index}`"
            class="day"
          >
            <div class="dayNum">
              {{ item }}
            </div>
            <el-popover
              v-if="getCalendarObj(item)"
              placement="top-start"
              width="320"
              trigger="hover"
            >
              <div class="zbInfo popover-content" style="max-height: 20vh; overflow-y: auto;">
                <div
                  v-for="(calendarObj, index) in getCalendarObj(item)"
                  :key="calendarObj.id"
                  class="zblist zblist-popover-item"
                  :class="getDutyColorClass(calendarObj, index)"
                >
                  <div class="zblist-item-title">
                    {{ formatTimeRemoveSeconds(calendarObj.dutyStartTime) }}-{{ formatTimeRemoveSeconds(calendarObj.dutyEndTime) }}
                    {{ calendarObj.departmentName ? `${calendarObj.departmentName}-` : '' }}{{ calendarObj.userName }}{{ listQuery.type !== '' ? (calendarObj.dutyTypeName ? `(${calendarObj.dutyTypeName})` : '') : (calendarObj.postName ? `(${calendarObj.postName})` : '') }}
                  </div>
                  <div>{{ calendarObj.dutyContent }}</div>
                </div>
              </div>
              <div slot="reference" class="zbInfo">
                <div
                  v-for="(calendarObj, index) in getCalendarObj(item).slice(0, 4)"
                  :key="calendarObj.id"
                  class="zblist text-single"
                  :class="getDutyColorClass(calendarObj, index)"
                >
                  {{ formatTimeRemoveSeconds(calendarObj.dutyStartTime) }}-{{ formatTimeRemoveSeconds(calendarObj.dutyEndTime) }}
                  {{ calendarObj.departmentName ? `${calendarObj.departmentName}-` : '' }}{{ calendarObj.userName }}{{ listQuery.type !== '' ? (calendarObj.dutyTypeName ? `(${calendarObj.dutyTypeName})` : '') : (calendarObj.postName ? `(${calendarObj.postName})` : '') }}
                </div>
                <div v-if="getCalendarObj(item).length > 4" class="zblist more">
                  还有 {{ getCalendarObj(item).length - 4 }} 条...
                </div>
              </div>
            </el-popover>
          </div>
          <div
            v-for="(item, index) in calendarObj.nextMonthDays"
            :key="`nextMonthDays${index}`"
            class="day"
            style="border-color: transparent;"
          >
            {{ item }}
          </div>
        </div>
      </div>
      <el-table
        v-else
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%;"
        @selection-change="handleSelection"
      >
        <!-- <el-table-column label="部门名称" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.deptName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="协同岗名称" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.postName }}</span>
          </template>
        </el-table-column> -->
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column label="人员ID" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="姓名" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属组织" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.departmentName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="协同岗名称" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.postName ? scope.row.postName : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排班类型" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.dutyTypeName ? scope.row.dutyTypeName : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="排班类型标识" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.dutyType ? scope.row.dutyType : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="值班开始日期" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.dutyStartDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="值班开始时间" align="center">
          <template slot-scope="scope">
            <span>{{ formatTimeRemoveSeconds(scope.row.dutyStartTime) }}</span>
          </template>
        </el-table-column>
         <el-table-column label="值班结束日期" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.dutyEndDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="值班结束时间" align="center">
          <template slot-scope="scope">
            <span>{{ formatTimeRemoveSeconds(scope.row.dutyEndTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column
          label="排班任务"
          align="center"
          :show-overflow-tooltip="true"
        >
          <template slot-scope="scope">
            <span class="text-single">{{ scope.row.dutyContent }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.gmtCreated }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.gmtModified }}</span>
          </template>
        </el-table-column>
        <el-table-column label="更新人" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.importUserName }}</span>
          </template>
        </el-table-column>
         <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="{ row }">
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="delBatch(row.id)"
            >
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0 && showType==='list'"
        :total="total"
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import {
  uploadDutyInformationFile,
  exportDutyInformationTemplate,
  getScheduleCalendar,
  getSchedulePage,
  delBatchSchedule
} from '@/api/shiftScheduling'
import { getDutyTypes } from '@/api/shiftScheduling/dutyType'
export default {
  name: 'Approval',
  components: { pagination },
  data() {
    return {
      listLoading: false,
      list: [],
      calendarList: null,
      clearable: true,
      total: 0,
      multipleSelection: [],
      listQuery: {
        date: '',
        // postName: '',
        userId:'',
        userName: '',
        type: '',
        pageSize: 10,
        pageNum: 1
      },
      dutyTypeOptions: [], // 排班类型选项
      dutyTypePage: {
        pageNum: 1,
        pageSize: 20,
        total: 0,
        hasMore: true
      },
      dutyTypeLoading: false,
      showType: 'list', // calendar 日历 list 列表
      calendarObj: {
        weekDayNameList: [
          '周日',
          '周一',
          '周二',
          '周三',
          '周四',
          '周五',
          '周六'
        ],
        firstDayWeek: 0,
        lastDayNum: 0,
        emptyWeekDayNum: 0, // 空日期的数量
        prevMonthDays: [], // 上月展示日期
        nextMonthDays: [], // 下月展示日期
        searchObj: {
          year: 0,
          month: 0
        }
      }
    }
  },
  watch: {
    showType() {
      this.getList()
    }
  },
  mounted() {
    this.getMonthDayDate()
    this.getDutyTypeOptions()
    this.getList()

    // 监听下拉框滚动事件
    this.$nextTick(() => {
      setTimeout(() => {
        const popper = document.querySelector('.duty-type-select-popper .el-select-dropdown__wrap')
        if (popper) {
          popper.addEventListener('scroll', this.handleDutyTypeScroll)
        }
      }, 1000)
    })
  },
  methods: {
    formatTimeRemoveSeconds(timeStr) {
      if (!timeStr) return ''
      const [hours, minutes] = timeStr.split(':')
      const hour = String(hours).padStart(2, '0')
      const minute = String(minutes).padStart(2, '0')
      return `${hour}:${minute}`
    },
    // 判断值班是否已过期（当前日期 + dutyEndTime < 当前时间）
    isDutyExpired(dutyDate, dutyEndTime) {
      if (!dutyDate || !dutyEndTime) return false
      const endDateTime = new Date(`${dutyDate} ${dutyEndTime}`)
      return endDateTime < new Date()
    },
    // 获取值班项的颜色类名
    getDutyColorClass(duty, index) {
      if (this.isDutyExpired(duty.dutyEndDate, duty.dutyEndTime)) {
        return 'listColor_disable'
      }
      return `listColor${index % 3}`
    },
    getYearAndMonth(dateStr) {
      const targetDate = dateStr ? new Date(dateStr) : new Date()
      const year = targetDate.getFullYear()
      const month = targetDate.getMonth() + 1
      this.calendarObj.searchObj.year = year
      this.calendarObj.searchObj.month = month
    },
    getCalendarObj(dayNumber) {
      return this.calendarList?.[
        `${this.calendarObj.searchObj.year}-${String(
          this.calendarObj.searchObj.month
        ).padStart(2, '0')}-${String(dayNumber).padStart(2, 0)}`
      ]
    },
    getList() {
      this.listLoading = true
      const [startDate, endDate] = this.listQuery.date

      let typeParam = this.listQuery.type
      // if (typeParam === -2) {
      //   typeParam = ''
      // }

      const params = {
        startDate,
        endDate,
        // postName: this.listQuery.postName,
        userId:this.listQuery.userId,
        userName: this.listQuery.userName,
        dutyType: typeParam
      }
      if (this.showType === 'calendar') {
        params.month = `${this.calendarObj.searchObj.year}-${String(
          this.calendarObj.searchObj.month
        ).padStart(2, '0')}`
        getScheduleCalendar(params).then(res => {
          this.calendarList = res?.data
          this.listLoading = false
        })
      } else {
        params.pageNum = this.listQuery.pageNum
        params.pageSize = this.listQuery.pageSize
        getSchedulePage(params).then(res => {
          this.list = res?.records || []
          this.total = res.total * 1
          this.listLoading = false
        })
      }
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      if (this.showType === 'calendar') {
         const [startDate] = this.listQuery.date
        if(startDate){
          this.getYearAndMonth(startDate)
        }
      }
      this.getList()
    },
    // 重置
    resetQuery() {
      this.listQuery = {
        pageNum: 1,
        pageSize: 10,
        date: '',
        // postName: '',
        userName: '',
        type: ''
      }
      //重置年和月
      this.getYearAndMonth()
      this.getList()
    },
    // 获取排班类型选项（分页）
    async getDutyTypeOptions(isLoadMore = false) {
      if (this.dutyTypeLoading) return
      if (isLoadMore && !this.dutyTypePage.hasMore) return

      this.dutyTypeLoading = true

      try {
        const res = await getDutyTypes({
          pageNum: isLoadMore ? this.dutyTypePage.pageNum + 1 : 1,
          pageSize: this.dutyTypePage.pageSize
        })

        const types = res.data?.records || res.data || []
        const total = res.data?.total || 0

        if (isLoadMore) {
          // 加载更多
          this.dutyTypeOptions = [
            ...this.dutyTypeOptions,
            ...types.map(item => ({
              label: `${item.name}(${item.type})`,
              value: item.type
            }))
          ]
          this.dutyTypePage.pageNum += 1
        } else {
          // 首次加载，添加"全部"和"其他"选项
          this.dutyTypeOptions = [
            // {
            //   label: '全部',
            //   value: -1
            // },
            // {
            //   label: '其他',
            //   value: -2
            // },
            ...types.map(item => ({
              label: `${item.name}(${item.type})`,
              value: item.type
            }))
          ]
          this.dutyTypePage.pageNum = 1
        }

        this.dutyTypePage.total = total
        this.dutyTypePage.hasMore = this.dutyTypeOptions.length - 2 < total
      } catch (e) {
        console.error('获取排班类型失败', e)
      } finally {
        this.dutyTypeLoading = false
      }
    },

    // 下拉框滚动加载更多
    handleDutyTypeScroll(e) {
      const target = e.target
      const scrollHeight = target.scrollHeight
      const scrollTop = target.scrollTop
      const clientHeight = target.clientHeight

      // 距离底部 50px 时加载更多
      if (scrollHeight - scrollTop - clientHeight < 50) {
        this.getDutyTypeOptions(true)
      }
    },
    goPrevMonth() {
      this.getTargetMonth('prev')
      // 重新获取日期
      this.getMonthDayDate(
        this.calendarObj.searchObj.year,
        this.calendarObj.searchObj.month
      )
      // 获取日历列表数据
      this.getList()
    },
    goNextMonth() {
      this.getTargetMonth('next')
      // 重新获取日期
      this.getMonthDayDate(
        this.calendarObj.searchObj.year,
        this.calendarObj.searchObj.month
      )
      // 获取日历列表数据
      this.getList()
    },
    getTargetMonth(type) {
      // 当前值是【1-12】的标准年份和月份，无需任何+1/-1转换
      const currentYear = this.calendarObj.searchObj.year
      const currentMonth = this.calendarObj.searchObj.month
      let targetYear, targetMonth

      if (type === 'prev') {
        // 上月逻辑：月份是1月 → 上月为去年12月；其他月份正常减1
        if (currentMonth === 1) {
          targetMonth = 12
          targetYear = currentYear - 1
        } else {
          targetMonth = currentMonth - 1
          targetYear = currentYear
        }
      } else if (type === 'next') {
        // 下月逻辑：月份是12月 → 下月为明年1月；其他月份正常加1
        if (currentMonth === 12) {
          targetMonth = 1
          targetYear = currentYear + 1
        } else {
          targetMonth = currentMonth + 1
          targetYear = currentYear
        }
      } else {
        throw new Error('参数只能传入 "prev"(上月) 或 "next"(下月)')
      }
      // 赋值回去的也是【1-12】的标准月份，彻底解决0月/无12月问题
      this.calendarObj.searchObj.year = targetYear
      this.calendarObj.searchObj.month = targetMonth
    },
    getMonthDaysObj(prevShowDay, nextShowDay) {
      const prevLen =
        (typeof prevShowDay === 'number' &&
          prevShowDay > 0 &&
          Math.floor(prevShowDay)) ||
        0
      const nextLen =
        (typeof nextShowDay === 'number' &&
          nextShowDay > 0 &&
          Math.floor(nextShowDay)) ||
        0

      const now = new Date()
      const lastDayOfPrevMonth = new Date(
        now.getFullYear(),
        now.getMonth(),
        0
      ).getDate()

      const prevMonthDays = []
      for (let i = 0; i < prevLen; i++) {
        prevMonthDays.unshift(lastDayOfPrevMonth - i)
      }
      const nextMonthDays = []

      for (let i = 0; i < nextLen; i++) {
        nextMonthDays.push(i + 1)
      }
      this.calendarObj.prevMonthDays = prevMonthDays
      this.calendarObj.nextMonthDays = nextMonthDays
    },
    getMonthDayDate(year, month) {
      const now = new Date()
      this.calendarObj.searchObj.year = year || now.getFullYear()
      this.calendarObj.searchObj.month = month || now.getMonth() + 1
      const firstDayWeek = new Date(
        this.calendarObj.searchObj.year,
        this.calendarObj.searchObj.month - 1,
        1
      ).getDay()
      const lastDayNum = new Date(
        this.calendarObj.searchObj.year,
        this.calendarObj.searchObj.month,
        0
      ).getDate()
      const lastDayWeek = new Date(
        this.calendarObj.searchObj.year,
        this.calendarObj.searchObj.month - 1,
        lastDayNum
      ).getDay()
      this.calendarObj.firstDayWeek = firstDayWeek
      this.calendarObj.lastDayNum = lastDayNum
      this.calendarObj.lastDayWeek = lastDayWeek
      const list = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      this.calendarObj.emptyWeekDayNum = this.calendarObj.weekDayNameList.indexOf(
        list[this.calendarObj.firstDayWeek]
      )
      this.calendarObj.emptyLastWeekDayNum = this.calendarObj.weekDayNameList.indexOf(
        list[this.calendarObj.lastDayWeek]
      )
      this.getMonthDaysObj(
        this.calendarObj.emptyWeekDayNum,
        6 - this.calendarObj.emptyLastWeekDayNum
      )
    },
    onSuccess(response) {
      // this.$message.success('上传成功')
      // 处理后端返回的响应数据
    },
    onError() {
      this.$message.error('上传失败')
      // 处理后端返回的响应数据
    },
    formatErrorMsg(errorData) {
      let errorText = ''
      for (const lineNum in errorData) {
        if (errorData.hasOwnProperty(lineNum)) {
          const errArr = errorData[lineNum]
          const errMsgStr = errArr.join('，')
          const singleLineError = `第${lineNum}行${errMsgStr}`
          errorText += singleLineError + '<br/>'
        }
      }
      // 去除最后多余的一个换行符（可选，看业务是否需要）
      return errorText
    },
    customAlertMsg(data) {
      const formatMsg = this.formatErrorMsg(data)
      if (formatMsg) {
        // this.$message.error({
        //   message: formatMsg,
        //   showClose: false, // 显示关闭按钮
        //   duration: 3000, // 0表示永不自动关闭，按需改成 5000 就是5秒后关闭
        //   dangerouslyUseHTMLString: true // 必须加！解析HTML标签（div换行用）
        // })
        this.$msgbox({
          message: formatMsg,
          title: '导入失败',
          confirmButtonText: '确定',
          dangerouslyUseHTMLString: true,
          customClass: 'alert-width-800'
        })
      }
    },
    uploadFileBtn(param) {
      const formData = new FormData()
      formData.append('file', param.file)
      this.$refs.upload.clearFiles()
      uploadDutyInformationFile(formData).then(res => {
        if (res.code === 0) {
          this.customAlertMsg(res?.data?.errorMap)
          if (res?.data?.successList?.length > 0) {
            this.getList() // 刷新列表
          }
          if (
            Object.keys(res?.data?.errorMap).length === 0 &&
            res?.data?.successList?.length > 0
          ) {
            this.$message.success('上传成功')
          }
        } else if (res.code === 1) {
          if (res?.data) {
            this.customAlertMsg(res?.data)
          } else if (res?.msg) {
            this.$msgbox({
              message: res?.msg,
              title: '导入失败',
              confirmButtonText: '确定',
              dangerouslyUseHTMLString: true
            })
          }
        } else {
          this.$message({
            message: res.msg,
            type: 'error'
          })
        }
      })
    },

    getExportFileName(headers) {
      const contentDisposition = headers['content-disposition'] || ''
      if (!contentDisposition) return '值班信息.xlsx'
      const reg = /filename\*=\s*utf-8''([^;]+)|filename="?([^;"]+)"?/i
      const match = contentDisposition.match(reg)
      if (!match) return '值班信息.xlsx'
      const raw = (match[1] || match[2] || '').trim()
      const fileName = decodeURIComponent(raw)
      return fileName || '值班信息.xlsx'
    },
    async handleExport() {
      try {
        const data = await exportDutyInformationTemplate()
        if (!data) {
          return
        }
        const fileName = this.getExportFileName(data?.headers)
        // 指定正确的 MIME 类型，确保 Excel 文件可以被正确打开
        const blob = new Blob([data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.style.display = 'none'
        link.href = url
        link.setAttribute('download', fileName)
        document.body.appendChild(link)
        link.click()
        // 释放 URL 对象
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error) {
        console.log(error)
      }
    },
    handleSelection(val) {
      this.multipleSelection = val.map(item=>item.id)
    },
    async delBatch(id) {
      let singleDel = []
      if(id){
        singleDel = [id]
      }else{
        if (this.multipleSelection.length === 0) {
          this.$message({
            message: '未勾选排班数据',
            type: 'error'
          })
          return
        }
      }
      this.$confirm(`确定${id ? '' : '批量'}删除吗？`, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          delBatchSchedule(id ? singleDel : this.multipleSelection).then(
            result => {
              if (result.code === 0) {
                this.$message({
                  message: `${id ? '' : '批量'}删除成功`,
                  type: 'success'
                })
              } else {
                this.$message({
                  message: result.msg,
                  type: 'error'
                })
              }
              this.getList()
            }
          )
        })
        .catch(err => {
          console.log(err)
        })
    }
  }
}
</script>

<style scoped lang="scss">
.card {
  height: 100%;
  overflow-y: auto;
}
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }

  // 确保 el-select 清除按钮显示
  ::v-deep .el-select {
    .el-input__icon {
      &.el-select__caret {
        // 确保清除图标可见
        &.is-show-close {
          display: inline-block !important;
          visibility: visible !important;
          opacity: 1 !important;
        }
      }
    }
  }
}
.edit-input {
  padding-right: 50px;
  width: 360px;
}
.edit-select {
  padding-right: 50px;
  width: 360px;
}

.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}

.calendarList{
  .weekName{
    display: flex;
    justify-content: space-between;
    div{
      width: calc(100%/7 - 10px);
      height: 40px;
      line-height: 40px;
      text-align: center;
      background: rgba(239, 246, 255, 1);
      font-size: 20px;
      font-weight: 500;
      color: rgba(71, 85, 105, 1);
    }
  }
  .weekDay{
    display: flex;
    flex-wrap: wrap;
    justify-content: space-between;
    .day{
      width: calc(100%/7 - 10px);
      height:126px;
      padding: 10px;
      border-radius: 4px;
      border:1px solid rgba(148, 163, 184, 0.5);
      margin-top: 10px;
      font-size: 20px;
      font-weight: 500;
      color: rgba(148, 163, 184, 1);
      display: flex;
      flex-direction: column;
      .dayNum{
        color: rgba(71, 85, 105, 1);
      }
    }
  }
}
.zbInfo{
  flex: 1;
  max-height: 80px;
  overflow-y: auto;
  // 显示2px宽度滚动条
  scrollbar-width: thin; // Firefox
  -ms-overflow-style: auto; // IE/Edge
  .listColor0{
    border-color: rgba(37, 99, 235, 1);
    color: rgba(37, 99, 235, 1);
    background: rgba(219, 234, 254, 1);

  }
  .listColor1{
    border-color: rgba(22, 163, 74, 1);
    color: rgba(22, 163, 74, 1);
    background: rgba(220, 252, 231, 1);
  }
  .listColor2{
    border-color: rgba(202, 138, 4, 1);
    color: rgba(202, 138, 4, 1);
    background: rgba(254, 249, 195, 1);
  }
  .listColor_disable{
    border-color: #D9DADE;
    color: #C0C2C9;
    background: #F5F5F5;
  }
  .zblist{
    height: 20px;
    width: 100%;
    border-width: 1px;
    border-style: dotted;
    line-height: 20px;
    font-size: 12px;
    font-weight: 400;
    line-height: 20px;
    padding: 0px 4px 0px 4px;
    margin-top:6px;
  }
  // Popover 内容项样式
  .zblist-popover-item {
    height: auto;
    min-height: 40px;
    padding: 6px 8px;
    margin-top: 8px;
    display: flex;
    flex-direction: column;
    gap: 4px;

    .zblist-item-title {
      font-weight: 500;
    }
  }

  // "还有 X 条..." 样式
  .zblist.more {
    text-align: center;
    color: #666;
    border: none;
    background: transparent;
    height: auto;
    margin-top: 4px;
  }
}
.no-select {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}
.text-single {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
<style>
.alert-width-800 {
  width: 600px !important;
  min-width: 600px !important;
}
/* 滚动条样式 - 非scoped才能生效 */
.zbInfo::-webkit-scrollbar {
  width: 2px;
}
.zbInfo::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 1px;
}
.zbInfo::-webkit-scrollbar-track {
  background: transparent;
}
</style>
