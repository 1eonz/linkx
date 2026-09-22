<template>
  <grid-layout
      :layout.sync="layout"
      :col-num="colNum||12"
      :row-height="rowHeight||30"
      :is-draggable="true"
      :is-resizable="true"
      :is-responsive="true"
      :is-mirrored="false"
      :vertical-compact="true"
      :margin="[12, 12]"
      :use-css-transforms="true"
      @layout-updated="layoutUpdated"
      >
      <grid-item
        v-for="item in backLayout"
        :key="item.i+9999"
        class="grid-item"
        :x="item.x"
        :y="item.y"
        :w="item.w"
        :h="item.h"
        :i="item.i"
        :is-draggable="false"
        :is-resizable="false"
        />
      <grid-item
        v-for="item in layout"
        :key="item.i"
        :x="item.x"
        :y="item.y"
        :w="item.w"
        :h="item.h"
        :i="item.i"
        @resized="changeSize(item.i)"
        >
        <chart-card
          v-if="item.obj"
          :id="'iframe_' + item.i"
          :ref="`chart${item.i}`"
          :look-config="getLookConfig(item.obj)"
          />
       </grid-item>
  </grid-layout>
</template>

<script>
import { GridLayout, GridItem } from 'vue-grid-layout'
import chartCard from '@/views/board/chart/chartCard.vue'
export default {
  name: 'GridCustom',
  components: {
    chartCard,
    GridLayout,
    GridItem,
  },
  data() {
    return {
      backLayout: [
        { 'x': 0, 'y': 0, 'w': 2, 'h': 2, 'i': '0', },
        { 'x': 2, 'y': 0, 'w': 2, 'h': 2, 'i': '1', },
        { 'x': 4, 'y': 0, 'w': 2, 'h': 2, 'i': '2' },
        { 'x': 6, 'y': 0, 'w': 2, 'h': 2, 'i': '3' },
        { 'x': 8, 'y': 0, 'w': 2, 'h': 2, 'i': '4' },
        { 'x': 10, 'y': 0, 'w': 2, 'h': 2, 'i': '5' },

        { 'x': 0, 'y': 2, 'w': 2, 'h': 2, 'i': '10' },
        { 'x': 2, 'y': 2, 'w': 2, 'h': 2, 'i': '11' },
        { 'x': 4, 'y': 2, 'w': 2, 'h': 2, 'i': '12' },
        { 'x': 6, 'y': 2, 'w': 2, 'h': 2, 'i': '13' },
        { 'x': 8, 'y': 2, 'w': 2, 'h': 2, 'i': '14' },
        { 'x': 10, 'y': 2, 'w': 2, 'h': 2, 'i': '15' },

        { 'x': 0, 'y': 4, 'w': 2, 'h': 2, 'i': '20' },
        { 'x': 2, 'y': 4, 'w': 2, 'h': 2, 'i': '21' },
        { 'x': 4, 'y': 4, 'w': 2, 'h': 2, 'i': '22' },
        { 'x': 6, 'y': 4, 'w': 2, 'h': 2, 'i': '23' },
        { 'x': 8, 'y': 4, 'w': 2, 'h': 2, 'i': '24' },
        { 'x': 10, 'y': 4, 'w': 2, 'h': 2, 'i': '25' },

        { 'x': 0, 'y': 6, 'w': 2, 'h': 2, 'i': '30' },
        { 'x': 2, 'y': 6, 'w': 2, 'h': 2, 'i': '31' },
        { 'x': 4, 'y': 6, 'w': 2, 'h': 2, 'i': '32' },
        { 'x': 6, 'y': 6, 'w': 2, 'h': 2, 'i': '33' },
        { 'x': 8, 'y': 6, 'w': 2, 'h': 2, 'i': '34' },
        { 'x': 10, 'y': 6, 'w': 2, 'h': 2, 'i': '35' },

        { 'x': 0, 'y': 8, 'w': 2, 'h': 2, 'i': '40' },
        { 'x': 2, 'y': 8, 'w': 2, 'h': 2, 'i': '41' },
        { 'x': 4, 'y': 8, 'w': 2, 'h': 2, 'i': '42' },
        { 'x': 6, 'y': 8, 'w': 2, 'h': 2, 'i': '43' },
        { 'x': 8, 'y': 8, 'w': 2, 'h': 2, 'i': '44' },
        { 'x': 10, 'y': 8, 'w': 2, 'h': 2, 'i': '45' },

        { 'x': 0, 'y': 10, 'w': 2, 'h': 2, 'i': '50' },
        { 'x': 2, 'y': 10, 'w': 2, 'h': 2, 'i': '51' },
        { 'x': 4, 'y': 10, 'w': 2, 'h': 2, 'i': '52' },
        { 'x': 6, 'y': 10, 'w': 2, 'h': 2, 'i': '53' },
        { 'x': 8, 'y': 10, 'w': 2, 'h': 2, 'i': '54' },
        { 'x': 10, 'y': 10, 'w': 2, 'h': 2, 'i': '55' },

        { 'x': 0, 'y': 12, 'w': 2, 'h': 2, 'i': '60' },
        { 'x': 2, 'y': 12, 'w': 2, 'h': 2, 'i': '61' },
        { 'x': 4, 'y': 12, 'w': 2, 'h': 2, 'i': '62' },
        { 'x': 6, 'y': 12, 'w': 2, 'h': 2, 'i': '63' },
        { 'x': 8, 'y': 12, 'w': 2, 'h': 2, 'i': '64' },
        { 'x': 10, 'y': 12, 'w': 2, 'h': 2, 'i': '65' },

        { 'x': 0, 'y': 14, 'w': 2, 'h': 2, 'i': '70' },
        { 'x': 2, 'y': 14, 'w': 2, 'h': 2, 'i': '71' },
        { 'x': 4, 'y': 14, 'w': 2, 'h': 2, 'i': '72' },
        { 'x': 6, 'y': 14, 'w': 2, 'h': 2, 'i': '73' },
        { 'x': 8, 'y': 14, 'w': 2, 'h': 2, 'i': '74' },
        { 'x': 10, 'y': 14, 'w': 2, 'h': 2, 'i': '75' },
      ],
      showChartList: [],
      layout: [],
      index: 0,
      colNum: 12,
      rowHeight: 46,
    }
  },
  computed: {
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

  methods: {
    dragend(obj) {
      const len = this.layout.length
      this.layout.push({
        x: (len * 2) % this.colNum,
        y: len + this.colNum,
        w: 2,
        h: 4,
        i: this.index,
        obj
      })
      this.index++
    },
    changeSize(id) {
      console.log(' ++++id', id)
    },
    layoutUpdated(data) {
      this.$emit('updated', data)
    }
  }
}
</script>

<style scoped lang="scss">
.grid-item{
  border:1px solid #3c6374 ;
}
</style>
