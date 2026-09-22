<script lang="ts" setup>
  import type { TabOptions } from '#/components';

  import { computed, nextTick, ref, unref, watch } from 'vue';

  import { Timeout } from '#/index';

  defineOptions({
    name: 'TdTab',
  });

  const props = withDefaults(
    defineProps<{
      data: TabOptions[];
      defaultValue?: any;
      label?: string;
      tabStyle?: string;
      tabType?: string;
      totalType?: string;
    }>(),
    {
      label: 'name',
      tabStyle: 'left',
      totalType: 'count',
    },
  );

  const emit = defineEmits(['click']);

  const tabBarRef = ref();
  const tabContentRef = ref();
  let timer: Timeout;
  const activeId = ref('');

  const isCard = computed(() => props.tabType === 'card');

  watch(
    () => props.defaultValue,
    (val) => {
      activeId.value = val || props.data?.[0]?.id || '';
      initBar();
    },
    { deep: true, immediate: true },
  );

  function tabClick(item, e) {
    activeId.value = item.id;
    initBar();
    emit('click', item, e);
  }

  function initBar() {
    nextTick(() => {
      if (!unref(tabBarRef)) return;
      const index = props.data.findIndex((i) => i.id === unref(activeId));
      const node = unref(tabContentRef).children[unref(index)]?.children[0];

      if (!node) return;
      const step = node.offsetLeft;
      const width = node.clientWidth || node.style.width;

      if (step === 0) {
        defaultNoneStep();
        return;
      }

      tabBarRef.value.style.width = `${width}px`;
      tabBarRef.value.style.transform = `translateX(${step}px)`;
      tabBarRef.value.style.left = '0px';
    });
  }

  function defaultNoneStep() {
    clearTimeout(timer);

    const index = props.data.findIndex((i) => i.id === unref(activeId));
    const node = unref(tabContentRef)?.children[unref(index)].children[0];
    const step = node?.offsetLeft || 0;

    if (step === 0) {
      timer = setTimeout(() => {
        defaultNoneStep();
      }, 500);
    } else {
      initBar();
    }
  }
</script>

<template>
  <div class="td-tab" :class="{ 'td-tab-default': !isCard, 'td-tab-card': isCard }">
    <div v-if="!isCard" ref="tabBarRef" class="td-tabs__active-bar"></div>

    <div
      ref="tabContentRef"
      :class="{ 'is-center': tabStyle === 'center' }"
      :style="{ display: 'flex' }"
    >
      <div
        v-for="(item, index) in data"
        :key="item[label] + index"
        class="td-tabs-items"
        :class="{ 'td-tabs-items-is-active': item.id === activeId }"
        @click="(e) => tabClick(item, e)"
      >
        <div class="td-tabs__item" :class="{ 'is-active': item.id === activeId }">
          <div v-if="item.iconName" class="icon-content">
            <Icon
              class="icon"
              :class="{ 'icon-is-active': item.id === activeId }"
              :name="item.iconName"
            />
          </div>
          {{ item[label] }}
          <span v-if="item.totalCount" v-show="totalType === 'showCount'" class="td-tab-count">
            ({{ item.totalCount }})
          </span>
          <TdLabel
            v-if="item.show && item.totalCount"
            v-show="totalType === 'label'"
            class="td-tab-count"
            :number="item.totalCount"
          />
          <TdTooltip v-if="item.tips" :content="item.tips">
            <Icon class="tips" name="tips" />
          </TdTooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .td-tab {
    position: relative;
    height: 40px;

    .is-center {
      display: flex;
      flex-flow: row;
    }

    .td-tabs__active-bar {
      position: absolute;
      bottom: 0;
      left: 0;
      z-index: 2;
      width: 50%;
      height: 2px;
      background-color: var(--button-active-color);
      transition: 0.3s;
      transform: translateX(50%);
    }

    .td-tabs-items {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 40px;
      font-size: 14px;
      cursor: pointer;

      .td-tabs__item {
        display: flex;
        align-items: center;
        color: var(--text-title-second);
        text-align: center;
        cursor: pointer;

        .icon-content {
          display: flex;
          align-items: center;
          justify-content: space-around;
          width: 1.5rem;
          height: 1.5rem;

          .icon {
            width: 1rem;
            height: 1rem;
            fill: var(--text-title-second);
          }

          .icon-is-active {
            fill: #264ed1 !important;
          }
        }

        .td-tab-count {
          color: var(--text-default);
        }

        .td-tab-count-is-active {
          color: #264ed1;
        }

        .tips {
          width: 16px;
          height: 16px;
          margin-left: 4px;
        }
      }

      .td-tabs__item:hover {
        color: #264ed1;

        span {
          color: #264ed1;
        }

        .icon {
          fill: #264ed1;
        }
      }

      .td-tabs__item.is-active {
        color: #264ed1;
      }
    }

    &-default {
      &::after {
        position: absolute;
        bottom: 0;
        left: 0;
        z-index: 1;
        width: 100%;
        height: 1px;
        content: '';
        border-bottom: var(--cut-line);
      }
    }

    &-card {
      height: 36px;
      padding: 4px 2px;
      background: rgb(173 204 240 / 7%);
      backdrop-filter: blur(8px);

      .td-tabs-items {
        height: 28px;

        .is-active {
          color: #264ed1 !important;
        }

        &-is-active {
          font-size: 14px;
          font-weight: bold;
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }

        .td-tabs__item {
          margin-right: 0;
        }
      }
    }
  }
</style>
