<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { computed } from 'vue';

  import { useI18n } from '@/hooks';

  type Tabs = {
    count: number;
    isPending?: boolean;
    label: string;
    value: number | string;
  };

  defineOptions({
    name: 'TdTabCount',
  });

  const props = defineProps({
    active: {
      default: '',
      type: [Number, String],
    },
    tabs: {
      default: () => [],
      type: Array as PropType<Tabs[]>,
    },
  });

  const emit = defineEmits(['click']);

  const { t } = useI18n();

  const tabsLen = computed(() => props.tabs.length);
  const showTabs = computed(() => {
    const { tabs } = props;
    if (tabsLen.value <= 1) {
      return tabs;
    }
    const ret: Tabs[] = [];
    if (tabs.length === 0) {
      return ret;
    }
    let activeItem;
    tabs.forEach((item: Tabs) => {
      if (item.value === props.active) {
        activeItem = item;
      } else {
        ret.push(item);
      }
    });
    return [ret[0], activeItem, ret[1]];
  });

  function clickTab(data) {
    emit('click', data);
  }
  function maxCount(count) {
    // count 只考虑为整数情况
    count = Number(count) || 0;
    if (count <= 999) {
      count = count * 1;
    } else if (count > 999 && count <= 99_999) {
      count = `${Number.parseInt(`${count / 1000}`)}k`;
    } else {
      count = '99k+';
    }
    return count;
  }
</script>

<template>
  <div class="data-count-tab">
    <div class="tab-container">
      <div
        v-for="(item, index) in showTabs"
        :key="index"
        class="tab-item"
        :class="{
          active: index === 1 || tabsLen === 1,
          'only-one': tabsLen === 1,
        }"
        @click.stop="clickTab(item)"
      >
        <div class="count" :class="{ pending: item?.isPending && item?.count > 0 }">
          {{ maxCount(item?.count) }}
        </div>
        <div class="label">{{ t(item?.label) }}</div>

        <svg v-if="index === 1 || tabsLen === 1" class="svg-bg">
          <use xlink:href="#icon-data_count_active" />
        </svg>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .data-count-tab {
    display: flex;
    align-items: center;
    width: 290px;
    height: 104px;

    .tab-container {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 82px;
      background-color: rgb(25 44 66 / 60%);
      border: 1px solid #1f3453;
    }

    .tab-item {
      position: relative;
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 82px;
      cursor: pointer;

      .count {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 36px;
        font-size: 36px;
        color: #01cdfd;
      }

      .pending {
        color: #ff173d;
      }

      .label {
        margin-top: 10px;
        font-size: 14px;
        line-height: 10px;
        color: #fff;
        text-align: center;
      }
    }

    .active {
      flex: 1;

      .count {
        z-index: 1;
        font-size: 40px;
        font-weight: bold;
      }

      .label {
        z-index: 1;
      }

      .svg-bg {
        position: absolute;
        top: -22px;
        width: 122px;
        height: 102px;
      }
    }
  }

  .only-one {
    flex: none;
    width: 122px;
  }
</style>
