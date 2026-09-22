<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="isAdd ? $t('index.list.layoutCreate') :$t('index.list.layoutEdit')"
      :before-close="handleClose"
      :close-on-click-modal="false"
      width="800px"
    >
      <el-form
        ref="tempForm"
        :rules="rules"
        :model="form"
        label-position="left"
        label-width="120px"
        style="width: 700px; margin-left:30px"
      >
        <el-form-item :label="$t('index.list.layoutName')" prop="dashboardName">
          <el-input
            v-model.trim="form.dashboardName"
            :placeholder="$t('mapConfig.enterName')"
             maxlength="100"
          />
        </el-form-item>
         <!-- <el-form-item :label="$t('index.list.defaultTemplate')" prop="isDefault">
            <el-radio-group v-model="form.isDefault" @input="handleChange">
              <el-radio :label="1">{{ $t('index.operations.yes') }}</el-radio>
              <el-radio :label="0">{{ $t('index.operations.no') }}</el-radio>
            </el-radio-group>
        </el-form-item> -->
        <el-form-item :label="$t('index.list.type')" prop="type">
          <div class="layout-wrap">
              <div class="btn-arrow left" @click="$refs.carousel.prev()">
                <i class="el-icon-arrow-left"></i>
              </div>
              <div class="btn-arrow right" @click="$refs.carousel.next()">
                <i class="el-icon-arrow-right"></i>
              </div>
              <el-carousel
                ref="carousel"
                trigger="click"
                height="110px"
                :autoplay="false"
                indicator-position="none"
              >
                <el-carousel-item
                  v-for="(item, index) in typeList"
                  :key="index"
                >
                  <div class="box-title">{{ item.title }}</div>
                  <div class="box-wrap">
                    <grid-box
                      v-for="(b, i) in item.types"
                      :key="i"
                      :nums="b"
                      class="box-item"
                      :class="{ 'box-active': activeType(b) }"
                      is-hide-num
                      @click.native="selectType(b)"
                    />
                  </div>
                </el-carousel-item>
              </el-carousel>
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
  </div>
</template>

<script>
import { getOrganizationList, getOrganizationById } from '@/api/resource/organization'
import { addDashboards, updateDashboards } from '@/api/board/layout.js'
import { deepCopy } from '@/utils'
import _ from 'lodash'
import selectTree from '@/components/SelectTree'
import gridBox from './gridBox.vue'

const form = {
  dashboardName: '',
  // isDefault: 1,
  type: '',
  orgId: '-1',
  orgName: '',
  remark: ''
}

export default {
  name: 'LayoutEdit',
  components: { selectTree, gridBox },
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
      dashboardName: [
        {
          required: true,
          validator: validateName,
          trigger: 'change'
        }
      ],
      type: [
        {
          required: true,
          trigger: 'change',
          message: this.$t('index.operations.selects')
        }
      ],
      // isDefault: [
      //   {
      //     required: true,
      //     trigger: 'change',
      //   }
      // ]
    }

    return {
      rules,
      visible: false,
      loading: false,
      isAdd: true,
      orgList: [],
      form: deepCopy(form),
      type: 0,
      layouts: window.$chart,
      options: []
    }
  },
  computed: {
    isAdmin() {
      return this.$store.state.app.isAdmin
    },
    activeType() {
      return v => this.form.type === v
    },
    typeList() {
      const newArr = []
      for (const o of this.layouts) {
        for (const i = 0; i < o.types.length;) {
          const obj = {
            title: o.px,
            types: []
          }
          if (o.px === '1920X1080') {
            obj.types = o.types.splice(0, 5)
          } else {
            obj.types = o.types.splice(0, 3)
          }
          newArr.push(obj)
        }
      }
      return newArr
    }
  },
  mounted() {
    this.getOrgList()
  },
  methods: {
    selectType(t) {
      this.form.type = t
    },
    // 新增
    async init(row) {
      this.isAdd = !row
      if (row) {
        const { id, dashboardName, type, orgId, remark } = row
        this.form = {
          id,
          dashboardName,
          type,
          orgId,
          orgName: '',
          remark
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
          const param = JSON.parse(JSON.stringify(this.form))
          const { id } = this.form
          if (this.isAdd) {
            delete param.id
          } else {
            param.dashboardChartList = []
          }
          const api = this.isAdd ? addDashboards : updateDashboards
          api(param, id).then(result => {
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

    typeChange() {
      this.isAutoClear = true
    },

    getOrgList() {
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
    },
    clearOrganizationType() {
      this.form.orgId = '-1'
      this.form.orgName = ''
    },
    parentCurrentChange(data) {
      this.form.orgId = data.id
      this.form.orgName = data.name
    },
    handleChange(val) {
      this.$refs['tempForm'].clearValidate()
    }
  }
}
</script>

<style scoped lang="scss">
.chart-url {
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  > span {
    position: absolute;
    color: #409eff;
    right: -50px;
    cursor: pointer;
    &:hover {
      color: #64affa;
    }
  }
}
.layout-wrap {
  width: 100%;
  height: 80px;
  box-sizing: border-box;
  .btn-arrow {
    position: absolute;
    top: 50%;
    font-size: 22px;
    background: rgba(173, 170, 170, 0.404);
    width: 30px;
    height: 30px;
    border-radius: 50%;
    line-height: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fff;
    cursor: pointer;
    display: flex;
    width: 0;
    transition: width 0s;
    transition-delay: 0.5s;
    &:hover {
      background: rgba(173, 170, 170, 0.63);
    }
  }
  &:hover {
    .btn-arrow {
      display: flex;
      width: 30px;
      transition-delay: 0s;
    }
  }
  .left {
    left: -35px;
  }
  .right {
    right: -35px;
  }
  .box-wrap {
    height: 100%;
    display: flex;
    .box-item {
      flex-shrink: 0.8;
      margin: 5px;
      border: 1px solid rgb(131, 154, 218);
      &:nth-of-type(1) {
        margin-left: 1px;
      }
      &:nth-last-of-type(1) {
        margin-right: 0px;
      }
      &:hover {
        cursor: pointer;
        ::v-deep div {
          // background: rgba(148, 173, 241, 0.904);
        }
      }
    }
    .box-active {
      border: 1px solid rgba(64, 160, 255, 0.849);
      ::v-deep div {
        background: rgba(64, 160, 255, 0.849);
      }
    }
  }
  .box-title {
    background: rgb(131, 154, 218);
    color: #fff;
    height: 20px;
    margin-top: 10px;
    line-height: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    letter-spacing: 2px;
  }
}
::v-deep .el-carousel__arrow--left {
  display: none;
}

//右箭头
::v-deep .el-carousel__arrow--right {
  display: none;
}
</style>
