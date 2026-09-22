<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="isAdd ?$t('index.list.chartCreate') :$t('index.list.chartEdit') "
      :before-close="handleClose"
      :close-on-click-modal="false"
      width="840px"
    >
      <el-form
        ref="tempForm"
        :rules="rules"
        :model="form"
        label-position="left"
        label-width="120px"
        style="width: 700px; margin-left:30px"
      >
        <el-form-item :label="$t('index.list.chartName') " prop="chartName">
          <el-input
            v-model.trim="form.chartName"
            :placeholder="$t('mapConfig.enterName')"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.headStyle')" prop="headStyle">
          <div class="head-scr">
              <div class="wrap-card">
                <template v-for="(item, index) in lookConList">
                  <chart-card
                   v-if="true"
                    :key="index"
                    :look-config="item"
                    class="card"
                    :is-select="form.headStyle === item.head"
                    style="margin-bottom:10px;cursor: pointer;"
                    @click.native="setHeadStyle(item, index)"
                  />
                </template>
              </div>
            </div>
            <span class="head-p" @click="openLook">{{ $t('index.list.PreviewStyle') }}</span>
        </el-form-item>
        <el-form-item :label="$t('index.list.chartType')" prop="type">
            <el-radio-group v-model="form.type" @change="typeChange">
              <el-radio
                v-for="(o, i) in Object.keys(chartTypeEnum)"
                :key="i"
                :label="+o"
                >{{ chartTypeEnum[o] }}</el-radio>
            </el-radio-group>
        </el-form-item>
        <el-form-item v-if="[0, 1].includes(form.type)" :label="$t('index.list.chartAddress')" prop="chartUrl">
          <div class="chart-url">
            <el-input
            v-model.trim="form.chartUrl"
            :placeholder="$t('mapConfig.enterName')"
          />
          <span><a :href="supersetUrl" target="_blank">{{ $t('index.list.chartAdd') }}</a></span>
          </div>
        </el-form-item>
        <el-form-item :label="$t('index.list.org')" prop="orgId">
        <select-tree
            v-model="form.orgName"
            style="width: 580px"
            :data="orgList"
            :is-init-value="true"
            :placeholder="$t('index.list.addressType')"
            @clear-val="clearOrganizationType"
            @current-change="parentCurrentChange"
          />
        </el-form-item>

        <!-- <el-form-item :label="$t('index.list.isExport')" prop="isExport">
            <el-radio v-model="form.isExport" :label="1">
              {{ $t('index.operations.yes') }}
          </el-radio>
            <el-radio v-model="form.isExport" :label="0">
              {{ $t('index.operations.no') }}
            </el-radio>
        </el-form-item> -->
        <el-form-item :label="$t('index.list.remarks')" prop="remark">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleClose">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          @click="handleSubmit(true)"
        >
          {{ isAdd ? $t('index.create') : $t('index.operations.change') }}
        </el-button>
      </div>
    </el-dialog>

     <!-- 详情 -->
     <chart-detail ref="details" />
  </div>
</template>

<script>
import { getOrganizationList, getOrganizationById } from '@/api/resource/organization'
import { addDashboard, upDateDashboard } from '@/api/board/chart.js'
import { deepCopy } from '@/utils'
import _ from 'lodash'
import { chartTypeEnum } from '../enum.js'
import chartCard from './chartCard'
import chartDetail from './chatLook.vue'
import selectTree from '@/components/SelectTree'

const form = {
  chartName: '', // 图表名称
  chartUrl: '', // 图表地址
  headStyle: 0, // 表头样式
  type: 0, // 图表类型
  orgId: '', // 所属组织id
  orgName: '', // 所属组织名称
  remark: '', // 备注
  isExport: 0, // 是否导出数据,
  dataModel: '' // 导出的数据模型
}

export default {
  name: 'ChartEdit',
  components: { chartCard, selectTree, chartDetail },
  data() {
    const pattern = new RegExp(
      "[`~!@#$^&*()=|{}':;',\\[\\].<>《》/?~！@#￥……&*（）——|{}【】‘；：”“'。，、？]"
    )
    const validateName = (_, value, callback) => {
      if (value.length === 0) {
        callback(new Error(this.$t('index.messageText.nameCannotBeEmpty')))
      } else if (pattern.test(value)) {
        callback(
          new Error(
            this.$t('index.messageText.nameCannotContainSpecialCharacters')
          )
        )
      } else {
        callback()
      }
    }
    const rules = {
      chartName: [
        {
          required: true,
          validator: validateName,
          trigger: 'change'
        }
      ],
      chartUrl: [
        {
          required: true,
          trigger: 'change',
          message: this.$t('index.list.chartAddressEmpty')
        }
      ],
      dataModel: [
        {
          required: true,
          trigger: 'blur',
          message: this.$t('index.list.uploadedEmpty')
        }
      ]
    }

    return {
      rules,
      visible: false,
      loading: false,
      isAdd: true,
      orgList: [],
      lookConList: [],
      chartTypeEnum,
      form: deepCopy(form)
    }
  },
  computed: {
    supersetUrl() {
      return sessionStorage.supersetUrl
    }
  },
  watch: {
    form: {
      handler(o) {
        this.lookConList.forEach(e => {
          e.title = o.chartName
          e.src = o.chartUrl
          e.type = o.type
        })
      },
      deep: true
    }
  },
  mounted() {
    this.getOrgList()
  },
  methods: {
    // 新增
    async init(row) {
      this.isAdd = !row
      if (row) {
        const { id, chartName, chartUrl, headStyle, type, orgId, remark, isExport, dataModel } = row
        this.form = {
          id,
          chartName,
          chartUrl,
          headStyle,
          type,
          orgId,
          orgName: '',
          remark,
          isExport,
          dataModel
        }
        if (orgId && orgId !== '-1') {
          const { code, data } = await getOrganizationById(orgId)
          if (code === 0) {
            this.form.orgName = data.name
          }
        }
      } else {
        this.form = deepCopy(form)
      }
      this.visible = true
      this.initCard()
    },

    // 关闭
    handleClose() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
    },

    // 点击确定
    handleSubmit: _.debounce(function() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          this.loading = true
          const params = {
            ...this.getParams(this.form)
          }
          const { id } = this.form
          const api = this.isAdd ? addDashboard : upDateDashboard
          api(params, id).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.$emit('success', id)
              this.handleClose()
            } else if (result.code) {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.loading = false
          })
        }
      })
    }, 500),

    getParams(formInfo) {
      const params = {}
      const keyMap = {
        0: {
          key: [
            'chartName',
            'chartUrl',
            'headStyle',
            'type',
            'orgId',
            'remark',
            'isExport'
          ],
          countTimeType: {
            0: [],
            1: []
          }
        },
        1: {
          key: [
            'chartName',
            'chartUrl',
            'headStyle',
            'type',
            'orgId',
            'remark',
            'isExport',
            'dataModel'
          ],
          countTimeType: {
            0: ['startTime', 'endTime', 'countTimeType'],
            1: ['num', 'unit', 'countTimeType']
          }
        }
      }

      keyMap[formInfo.isExport].key.forEach(e => {
        params[e] = formInfo[e]
        if (e === 'dataModel') {
          params[e] = params[e].join(',')
        }
      })
      return params
    },

    initCard() {
      this.isAutoClear = false
      this.lookConList.splice(0)
      const headMax = 15
      const cardInfo = {
        src: '',
        type: 0,
        head: 0,
        title: this.$t('index.list.chartName')
      }
      for (let i = 0; i < headMax; i++) {
        this.lookConList.push({ ...cardInfo, head: i })
      }
    },

    openLook() {
      this.$refs.details.setData(this.form)
    },
    setHeadStyle(obj) {
      this.form.headStyle = obj.head
    },

    typeChange() {
      this.isAutoClear = true
      this.form.chartUrl = ''
    },

    getOrgList() {
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
    },
    clearOrganizationType() {
      this.form.orgId = ''
      this.form.orgName = ''
    },
    parentCurrentChange(data, node) {
      this.form.orgId = data.id
      this.form.orgName = data.name
    }
  }
}
</script>

<style scoped lang="scss">
.head-scr {
  height: 380px;
  padding: 5px 15px;
  overflow: auto;
  background-color: rgba(13, 39, 65, 0.55);
  background-color: rgba(139, 139, 139, 0.61);
  .wrap-card {
    display: grid;
    height: 1000px;
    grid-template-columns: repeat(2, 1fr);
    grid-template-rows: repeat(8, minmax(0, 1fr));
    grid-gap: 4px;
  }
  .card {
    width: 200%;
    height: 200%;
    margin-bottom: 10px;
    transform: scale(0.49, 0.49);
    transform-origin: left top;
    ::v-deep {
      .head-wrap {
        .title {
          // font-weight: 200;
        }
      }
      .def-img {
        font-size: 10px;
      }
    }
  }
}
.head-p {
  position: absolute;
  top: -4px;
  right: -60px;
  color: #409eff;
  cursor: pointer;
}

.chart-url {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  > span {
    position: absolute;
    color: #409eff;
    right: -60px;
    cursor: pointer;
    &:hover {
      color: #64affa;
    }
  }
}
</style>
