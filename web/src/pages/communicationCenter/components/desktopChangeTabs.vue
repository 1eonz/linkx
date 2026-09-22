<script lang="ts" setup>
  withDefaults(
    defineProps<{
      activeId?: string;
      screenChange?: boolean;
      tabsArr: any;
    }>(),
    {
      tabsArr: [],
    },
  );
  const emit = defineEmits(['change']);

  function changeFunc(item) {
    emit('change', item);
  }
</script>

<template>
  <div :class="screenChange ? 'screen-change-tabs' : 'desktop-change-tabs'">
    <template v-for="item in tabsArr" :key="item.id">
      <TdButton
        v-if="item.show"
        active
        class="layout-item"
        :class="{ 'active-layout': activeId === item.id }"
        type="guide"
        @click="changeFunc(item)"
      >
        <Icon
          class="screen-p-icon"
          :color="activeId === item.id ? '#1AFFFB' : 'rgba(255, 255, 255, 1)'"
          :name="item.iconName"
          prefix="bigScreen"
        />
        <span>{{ item.name }}</span>
      </TdButton>
    </template>
  </div>
</template>

<style lang="less" scoped>
  .desktop-change-tabs {
    display: flex;
    align-items: center;
    justify-content: flex-start;

    .layout-item {
      box-sizing: border-box;
      display: flex;
      align-items: center;
      justify-content: center;
      width: auto;
      height: 32px;
      margin-right: 12px;
      font-size: 16px;
      font-weight: 500;
      line-height: 32px;
      color: rgb(153 206 251 / 100%);
      text-align: center;
      vertical-align: center;
      cursor: pointer;

      .screen-p-icon {
        width: 14px;
        height: 14px;
        padding: 0 !important;
        margin-right: 4px;
      }
    }

    .active-layout {
      span {
        color: rgb(26 255 251 / 100%);
      }
    }
  }

  .screen-change-tabs {
    display: flex;
    align-items: center;
    justify-content: flex-start;

    .layout-item {
      width: 60px;
      height: 40px;
      margin-right: 12px;
      font-size: 12px;
      font-weight: 500;
      line-height: 40px;
      color: rgb(224 240 255 / 100%);
      text-align: center;
      vertical-align: top;
      cursor: pointer;
      background: #ccc;
    }

    .active-layout {
      color: white;
    }
  }
</style>
