<template>
  <div
    class="grid-wrap"
    :class="'grid-wrap-' + showNums + '-' + showType"
    :style="styleObject"
  >
    <div
      v-for="(o, i) in renderChartList"
      :key="i"
      class="box"
      :draggable="o.id && isDraggable"
      :class="{'is-draw-line':!o.id && isNeedLine}"
      @dragenter="dragenter"
      @drop="drop($event, i)"
      @dragover="dragOver"
      @dragstart="dragstart($event, o)"
      @mouseenter="enterHandler(o)"
    >
      <template v-if="o.id">
        <chart-card :look-config="getLookConfig(o)" />
      </template>

      <template v-else>
        <slot v-if="$slots.default"> </slot>
        <div v-if="!isHideNum">
          {{ o.num }}
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import chartCard from '@/views/board/chart/chartCard.vue'
export default {
  name: '',
  components: {
    chartCard
  },
  props: {
    isHideNum: {
      type: Boolean,
      default: false
    },
    nums: { // 屏幕数量 100为1屏， 200为2屏
      type: Number,
      default: 100
    },
    option: {
      type: Object,
      default: () => {}
    },
    styleObject: {
      type: Object,
      default: () => {}
    },
    showChartList: {
      type: Array,
      default: () => []
    },
    isDraggable: {
      type: Boolean,
      default: true
    },
    isNeedLine: {
      type: Boolean,
      default: false
    },
    lookConfig: {
      type: Object,
      default: () => {
        return {
          src: '',
          type: '',
          head: '',
          title: ''
        }
      }
    }
  },
  data() {
    return {
      chartList: [],
      historyObj: null,
      type: 0,
      showNums: 0
    }
  },
  computed: {
    renderChartList() {
      return this.chartList
    },
    showType() {
      return this.toGridType(this.nums)
    },
    getLookConfig() {
      return obj => {
        return {
          ...obj,
          dom_id: 'iframeGrid_' + obj.id,
          src: obj.chartUrl,
          type: obj.type,
          head: obj.headStyle,
          title: obj.chartName
        }
      }
    }
  },
  watch: {
    showChartList: {
      handler(o) {
        this.init(o)
      },
      deep: true
    }
  },
  mounted() {},
  created() {
    this.showNums = this.toGridNum(this.nums)
    for (let i = 0; i < this.showNums; i++) {
      this.chartList.push({ num: i + 1 })
    }
  },
  methods: {
    toGridNum(n) {
      return Math.floor(n / 100)
    },
    toGridType(n) {
      return n % 100
    },
    init() {
      const list = this.showChartList
      for (let i = 0; i < this.showNums; i++) {
        const chartObj = list.find(e => {
          const index = e.area.split('#chart')[1]
          return e.area && Number(index) === i + 1
        })
        if (chartObj) {
          this.chartList[i] = chartObj
        } else {
          this.chartList[i] = { num: i + 1 }
        }
      }
      this.$forceUpdate()
    },

    getList() {
      return this.renderChartList
    },
    enterHandler(o) {
      const dom = document.querySelector('#iframeGrid_' + o.id)
      if (!dom) return
      dom.style.pointerEvents = 'none'
    },
    dragstart(e, o) {
      if (!this.isDraggable) return
      e.dataTransfer.setData('obj', JSON.stringify(o))
    },
    dragenter(e) {
      // console.log(e.dataTransfer.getData("obj"), "eeeee");
    },
    removeBox(id) {
      const index = this.chartList.findIndex(e => e.id === id)
      this.chartList[index] = { num: index + 1 }
      this.$forceUpdate()
    },
    drop(e, index) {
      if (!this.isDraggable) return
      this.swapObj(e, index)
      this.resetIndex()
      this.$forceUpdate()
    },
    swapObj(e, index) {
      const fromObj = JSON.parse(e.dataTransfer.getData('obj') || '{}')
      const toObj = this.chartList[index]
      if (!fromObj.chartId) fromObj.chartId = fromObj.id
      const fromIndex = this.chartList.findIndex(
        e => e.chartId === fromObj.chartId
      )
      if (fromIndex >= 0) {
        if (toObj.chartId) {
          /** 交换 */
          // [this.chartList[index], this.chartList[fromIndex]] = [
          //   this.chartList[fromIndex],
          //   this.chartList[index]
          // ];
        } else {
          //
        }
        this.chartList[fromIndex] = { ...toObj }
      }
      this.historyObj = { ...toObj }
      this.chartList[index] = { ...fromObj }
    },
    resetIndex() {
      const arr = this.chartList
      arr.map((e, i) => {
        if (!e.id) {
          e.num = i + 1
        } else {
          e.area = '#chart' + (i + 1)
        }
      })
    },
    dragOver(e) {
      e.preventDefault()
    }
  }
}
</script>

<style scoped lang="scss">
.grid-wrap {
  display: grid;
  width: 87px;
  height: 48.93749px;
  > div {
    background: rgb(131, 154, 218);
    margin: 1px;
    // margin-bottom:2px ;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #fff;
    font-size: 12px;
    line-height: 0;
    //
  }
  .is-draw-line{
    box-shadow: 0 0 0 1px #3c6374;
  }
}
.grid-wrap-1-0 {
  grid-template-columns: 100%;
  grid-template-rows: 100%;
}
.grid-wrap-4-0 {
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, minmax(0, 1fr));
}
.grid-wrap-6-0 {
  grid-template-columns: 1fr 2fr 1fr;
  grid-template-rows: repeat(2, minmax(0, 1fr));
}
.grid-wrap-6-1 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, minmax(0, 1fr));
  div:nth-child(1) {
    grid-column: 1 / 3;
    grid-row: 1 / 3;
  }
}
.grid-wrap-7-0 {
  width: 174px;
  grid-template-columns: 1fr 2fr 1fr;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  div:nth-child(2) {
    grid-column: 2 / 2;
    grid-row: 1 / 4;
  }
}
.grid-wrap-7-1 {
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, minmax(0, 1fr));
  div:nth-child(1) {
    grid-column: 1 / 1;
    grid-row: 1 / 4;
  }
}
.grid-wrap-8-0 {
  grid-template-columns: repeat(4, 1fr) 2.5fr;
  // grid-template-rows: repeat(2, minmax(0, 1fr));
  grid-template-rows: repeat(2, minmax(0, 1fr)) repeat(2, minmax(0, 2fr));
  div:nth-child(5) {
    grid-column: 5 / 6;
    grid-row: 1 / 3;
  }
  div:nth-child(6) {
    grid-column: 1 / 5;
    grid-row: 2 / 5;
  }
}
.grid-wrap-9-0 {
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(3, minmax(0, 1fr));
  div:nth-child(2) {
    grid-column: 2 / 4;
    grid-row: 1 / 3;
  }
}
.grid-wrap-12-0 {
  width: 174px;
  grid-template-columns: repeat(5, 1fr) repeat(2, 2.568fr);
  grid-template-rows: repeat(2, minmax(0, 2fr))  repeat(2, minmax(0, 1fr));
  div:nth-child(1) {
    grid-column: 1 / 6;
    grid-row: 1 / 4;
  }
  div:nth-child(11) {
    grid-column: 6 / 7;
    grid-row: 3 / 5;
  }
  div:nth-child(12) {
    grid-column: 7 / 8;
    grid-row: 3 / 5;
  }
}
.grid-wrap-13-0 {
  width: 174px;
  grid-template-columns: 1fr 1fr 4.5fr 1fr 1fr;
  grid-template-rows: repeat(3, minmax(0, 1fr));
  div:nth-child(3) {
    grid-row: 1/4;
    grid-column: 3 / 3;
  }
}
.grid-wrap-15-0 {
  width: 174px;
  grid-template-columns: repeat(2, 1fr) repeat(2, 2fr) repeat(2, 1fr);
  grid-template-rows: repeat(3, minmax(0, 1fr));
  div:nth-child(3) {
    grid-column: 3 / 5;
    grid-row: 1 / 3;
  }
}
</style>
