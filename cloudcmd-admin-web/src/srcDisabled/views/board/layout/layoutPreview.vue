<!-- 看板查看 -->
<template>
  <div class="drawer-wrap">

    <!-- 编排 -->
    <el-drawer
      ref="drawer"
      :visible.sync="_drawer"
      :show-close="false"
      :wrapper-closable="false"
      :modal="false"
      :destroy-on-close="true"
    >
      <div class="box-wrap">
        <div class="left" :class="{ 'is-look': isLook }">
          <!-- 按钮 -->
          <div class="btns">
            <el-button v-if="!isLook" size="mini" @click="release">{{ $t('index.operations.release') }}</el-button>
            <el-button v-if="!isLook" size="mini" @click="save">{{ $t('index.operations.save') }}</el-button>
            <el-button v-if="!isLook" size="mini" @click="preview">{{ $t('index.operations.preview') }}</el-button>
            <el-button size="mini" @click="_drawer = false">{{ $t('index.operations.return') }}</el-button>
          </div>

          <!-- 默认栅格布局 -->
          <grid-box
            v-if="_drawer&& isDefault===1"
            ref="gridBox"
            :nums="nums"
            :style-object="gridBoxStyle"
            :is-draggable="!isLook"
            :show-chart-list="showChartList"
            :look-config="lookConfig"
            style="padding:12px"
            is-need-line
          >
            chart
          </grid-box>

          <!-- 自定义栅格布局 -->
          <grid-custom v-if="isDefault===0" ref="gridCustom" @updated="layoutUpdated" />
        </div>

        <!-- 图表栏 -->
        <div
          v-if="!isLook"
          class="right"
          @drop="dropRight"
          @dragover.prevent
        >
          <div class="top">
            <el-input
              v-model="chartName"
              size="mini"
              clearable
              :placeholder="$t('mapConfig.enterName')"
              @clear="clear"
              ><el-button
                slot="append"
                size="mini"
                icon="el-icon-search"
                @click="search"
              /></el-input>
          </div>

          <div class="list">
            <div
              v-for="item in chartList"
              :key="item.id"
              class="chartBox"
              draggable="true"
              @dragend="dragend(item)"
              @mouseup="mouseup(item)"
              @mousedown="mousedown(item)"
              @dragstart="dragstart($event, item)"
              @mouseenter="enterHandler(item)"
            >
              <chart-card
                :id="'iframe_' + item.id"
                :look-config="getLookConfig(item)"
              />
            </div>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 预览 -->
    <layout-look
      :dialog-visible.sync="previewVisible"
      :nums="nums"
      :style-object="gridBoxStyle"
      :show-chart-list="showChartList"
      @save="save"
      @release="release"
    />
  </div>
</template>

<script>
import gridBox from './gridBox.vue'
import gridCustom from './gridCustom.vue'
import layoutLook from './layoutLook.vue'
import chartCard from '@/views/board/chart/chartCard.vue'
import handlerChartUrl from '@/mixins/handler-chart-url.js'
import { lookDashboardList } from '@/api/board/chart.js'
import {
  updateDashboards,
  lookDashboardsById
} from '@/api/board/layout.js'

export default {
  name: 'Preview',
  components: {
    gridBox,
    layoutLook,
    chartCard,
    gridCustom
  },
  mixins: [handlerChartUrl],
  props: {
    drawer: {
      type: Boolean,
      default: false
    },
    nums: {
      type: Number,
      default: 0
    },
    fromInfo: {
      type: Object,
      default: () => {}
    },
    isLook: {
      tpye: Boolean,
      default: false
    }
  },
  data() {
    return {
      lookConfig: {},
      previewVisible: false,
      chartName: '',
      chartList: [],
      gridBoxStyle: {
        height: '100%',
        width: '100%'
      },
      timer: null,
      showChartList: [],
      layout: []
    }
  },
  computed: {
    _drawer: {
      get() {
        return this.drawer
      },
      set(v) {
        this.$emit('update:drawer', v)
      }
    },
    isDefault() {
      return this.fromInfo?.isDefault === 0 ? 0 : 1
    },
    getLookConfig() {
      return obj => {
        const { id, chartUrl, type, headStyle, chartName } = obj
        return {
          dom_id: 'iframe_' + id,
          src: chartUrl,
          type: type,
          head: headStyle,
          title: chartName
        }
      }
    }
  },
  watch: {
    drawer(v) {
      if (v) {
        this.init()
        this.resetWidth()
      } else {
        this.showChartList && this.showChartList.splice(0)
        this.chartList.splice(0)
      }
    }
  },
  mounted() {},
  methods: {
    dragend(obj) {
      this.$refs.gridCustom.dragend(obj)
    },
    preview() {
      this.showChartList = this.$refs.gridBox.getList()
      this.previewVisible = true
    },
    dropRight(e) {
      const fromObj = JSON.parse(e.dataTransfer.getData('obj') || '{}')
      this.$refs.gridBox.removeBox(fromObj.id)
      e.preventDefault()
    },
    async release() {
      const { isNotFull, dashboardChartList } = this.getChartList()
      if (isNotFull) {
        this.$message.error(this.$t('index.list.fillFirst'))
        return
      }
      await this.save()
      const params = {
        status: 1,
        dashboardChartList
      }
      const res = await updateDashboards(params, this.fromInfo.id)
      if (res.code === 0) {
        this.$message.success(this.$t('index.list.releaseSuccess'))
        this.$emit('initTable')
      }
    },
    getChartList() {
      const charListArr = this.$refs.gridBox.getList()
      const isNotFull = charListArr.some(e => e.num)
      const dashboardChartList = []
      charListArr.map((e, i) => {
        if (!e.id) return
        const obj = {
          dashboardId: this.fromInfo.id,
          chartId: e.chartId || e.id,
          area: `#chart${i + 1}`,
          chartUrl: e.chartUrl
        }
        dashboardChartList.push(obj)
      })
      return { isNotFull, dashboardChartList }
    },
    async save() {
      const { dashboardChartList } = this.getChartList()
      const params = {
        updateFlag: 1,
        dashboardChartList
      }
      if (this.isDefault === 0) {
        params.layout = this.layout
      }
      const res = await updateDashboards(params, this.fromInfo.id)
      if (res.code === 0) {
        this.$message.success(this.$t('index.list.saveSuccess'))
        this.init()
        this.$emit('initTable')
      }
    },
    mouseup(o) {
      clearTimeout(this.timer)
    },
    mousedown(o) {
      this.timer = setTimeout(() => {
        this.enterHandler(o)
      }, 500)
    },
    enterHandler(o) {
      const dom = document.querySelector('#iframe_' + o.id)
      if (!dom) return
      dom.style.pointerEvents = 'none'
    },
    dragstart(e, obj) {
      e.dataTransfer.setData('obj', JSON.stringify(obj))
    },
    onMove(obj, event) {
      this.$refs.gridBox.updateMove(obj)
    },
    search() {
      this.init()
    },
    clear() {
      this.chartName = ''
      this.init()
    },
    async init() {
      const res = await lookDashboardsById(this.fromInfo.id)
      this.showChartList = res.data.dashboardChartList
      this.handlerChartUrl('showChartList')
      const params = {
        chartName: this.chartName
      }
      const { data } = await lookDashboardList(params)
      this.chartList = data
      this.handlerChartUrl('chartList')
    },
    resetWidth() {
      const pDom = document.querySelector('.app-main')
      const dDom = this.$refs.drawer
      this.$nextTick(() => {
        let dom = dDom.$el.firstChild
        dom = dom.firstChild
        dom.style.width = pDom.clientWidth + 'px'
      })
    },
    layoutUpdated(data) {
      this.layout = data
    }
  }
}
</script>

<style scoped lang="scss">
.drawer-wrap {
  ::v-deep {
    .el-drawer__header {
      padding: 0;
      margin: 0;
    }
  }
  .box-wrap {
    position: relative;
    background: linear-gradient(0deg, #000505 0%, #015273 88%);
    display: flex;
    height: 100%;
    .left {
      width: 79.3651vw;
      height: inherit;
      position: relative;
      .btns {
        z-index: 999;
        position: absolute;
        right: 10px;
        top: 30px;
        ::v-deep .el-button {
          margin: 0 20px;
        }
      }
      ::v-deep {
        .grid-wrap > div {
          background: transparent;
          min-height: 0;
        }
      }
    }
    .right {
      width: 20.6349%;
      height: inherit;

      .top {
        box-sizing: border-box;
        padding: 30px 15px 20px;
      }

      .list {
        overflow: auto;
        height: 90vh;
        padding: 0 5px;
        .chartBox {
          display: flex;
          justify-content: center;
          align-items: center;
          background: transparent;
          height: 250px;
          margin: 0 0 15px;
        }
      }
    }
    .is-look {
      width: 100%;
    }
    ::v-deep .grid-wrap {
      grid-gap: 12px 12px;
      > div {
        background: transparent;
      }
    }
  }
}
</style>
