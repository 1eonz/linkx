<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { videoFunc } from '@/plugins/mspPlayer';
  import { useCommunicationStore } from '@/store';

  const props = withDefaults(
    defineProps<{
      category?: number;
      isdn: string;
      ptzWidth: number;
    }>(),
    {
      ptzWidth: 40,
    },
  );

  const communicationStore = useCommunicationStore();

  const coordsObj = ref<any>({});
  const splitWidth = ref(0);

  const showPtz = computed(() => {
    // 连云港记录仪与国标记录仪不显示云台控制图标
    if (props.category && (props.category === 500_005 || props.category === 500_009)) {
      return false;
    }
    const { comm } = communicationStore;
    const target = comm[props.isdn] || {};
    let show = false;
    Object.keys(target).forEach((key) => {
      if (['0', '1'].includes(target[key].ptz)) {
        show = true;
      }
    });
    return show;
  });
  const imgMapId = computed(() => {
    return props.isdn + Math.random() * 10_000;
  });

  watch(
    () => props.ptzWidth,
    (width) => {
      computeCoords(width);
    },
  );

  onMounted(() => {
    computeCoords(props.ptzWidth);
  });

  function computeCoords(width) {
    const splitW = Number.parseInt(`${width / 4}`);
    splitWidth.value = Number.parseInt(`${width / 2}`);
    coordsObj.value = {
      '1': [splitW, splitW, splitW * 2, 0, splitW * 3, splitW].toString(),
      '2': [splitW, splitW * 3, splitW * 3, splitW * 3, splitW * 2, splitW * 4].toString(),
      '3': [splitW, splitW, 0, splitW * 2, splitW, splitW * 3].toString(),
      '4': [splitW * 3, splitW, splitW * 4, splitW * 2, splitW * 3, splitW * 3].toString(),
      '11': [0, 0, splitW * 2, 0, 0, splitW * 2].toString(),
      '12': [0, splitW * 2, splitW * 2, splitW * 4, 0, splitW * 4].toString(),
      '13': [splitW * 2, 0, splitW * 4, 1, splitW * 4, splitW * 2].toString(),
      '14': [splitW * 4, splitW * 2, splitW * 4, splitW * 4, splitW * 2, splitW * 4].toString(),
    };
  }

  function arrowKeyClick(param) {
    videoFunc.videoPtzctrlCamera(props.isdn, param, '5');
  }
</script>

<template>
  <div v-if="showPtz" class="ptz-control" @dblclick.stop>
    <img
      class="arrow-key"
      src="@/assets/images/camera/arrowKey.png"
      :style="{ width: `${ptzWidth}px`, height: `${ptzWidth}px` }"
      :usemap="`#${imgMapId}`"
    />
    <map class="map-poly" :name="imgMapId" @mouseup="arrowKeyClick('15')">
      <area
        v-for="(item, index) in coordsObj"
        :key="index"
        :coords="item"
        shape="poly"
        @mousedown="arrowKeyClick(index)"
      />
    </map>
    <div class="focal-length" @mouseup="arrowKeyClick('15')">
      <img
        class="change-key"
        src="@/assets/images/camera/increase.png"
        :style="{ width: `${splitWidth}px`, height: `${splitWidth}px` }"
        @mousedown="arrowKeyClick('5')"
      />
      <img
        class="change-key"
        src="@/assets/images/camera/narrow.png"
        :style="{ width: `${splitWidth}px`, height: `${splitWidth}px` }"
        @mousedown="arrowKeyClick('6')"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .ptz-control {
    position: absolute;
    top: 20%;
    right: 2%;
    z-index: 100;
    display: flex;
    flex-direction: column;
    align-items: center;

    .focal-length {
      display: flex;
    }

    .arrow-key {
      width: 60px;
      height: 60px;
    }

    .map-poly {
      cursor: pointer;
    }

    .change-key {
      width: 25px;
      height: 25px;
      cursor: pointer;
    }
  }
</style>
