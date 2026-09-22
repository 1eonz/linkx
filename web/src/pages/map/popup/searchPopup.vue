<script lang="ts" setup>
  import { computed, ref, watchEffect } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useAddressStore } from '@/store';

  import CollectPopup from './collectPopup.vue';

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
  });

  const addressStore = useAddressStore();
  const collect = ref(false);

  const iconName = computed(() => {
    return collect.value ? 'btn_uncollected' : 'btn_collected';
  });

  watchEffect(() => {
    const has = addressStore.addressData.findIndex((i) => {
      return i.nid === props.data.nid;
    });
    collect.value = has !== -1;
  });

  async function handleCollected() {
    // 取消收藏
    const { nid } = props.data;
    const addressData = addressStore.addressData;
    const index = addressData.findIndex((item) => {
      return item.nid === nid;
    });
    if (collect.value) {
      await addressStore.delAddressData(addressData[index].id);
      return;
    }

    // 收藏
    const cid = `${nid}CollectPopup`;
    Dialog({
      cid,
      content: CollectPopup,
      data: {
        cid,
        data: props.data,
      },
      shade: false,
    });
  }
</script>

<template>
  <div class="map-search-popup">
    <Icon class="collect" :name="iconName" @click.stop="handleCollected" />
    <div class="search-title"> {{ data.name }} </div>
    <div class="search-details">
      <div class="details-item">{{ data.address }}</div>
      <div v-if="data.phone" class="details-item">
        <Icon class="item-icon" name="phone_call" />
        <span>{{ data.phone }}</span>
      </div>
      <div v-if="data.typeName" class="details-item">
        <Icon class="item-icon" name="category_type" />
        <span>{{ data.typeName }}</span>
      </div>
      <div v-if="data.remark" class="details-item">
        <Icon class="item-icon" name="category_file" />
        <span>{{ data.remark }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .map-search-popup {
    position: relative;
    width: 100%;

    .collect {
      position: absolute;
      top: 4px;
      right: 0;
      width: 16px;
      height: 16px;
      cursor: pointer;
    }

    .search-title {
      width: 274px;
      font-size: 16px;
      font-weight: bold;
      line-height: 24px;
      color: var(--text-title-first);
      word-break: break-all;
    }

    .search-details {
      .details-item {
        display: flex;
        font-size: 12px;
        font-weight: 400;
        line-height: 20px;
        color: var(--text-title-second);
        word-break: break-all;

        .item-icon {
          width: 12px;
          height: 12px;
          margin-top: 4px;
          margin-right: 5px;
          fill: var(--text-default);
        }

        span {
          display: inline-block;
          width: 270px;
          font-size: 12px;
          line-height: 20px;
          color: var(--text-default);
          word-break: break-all;
        }
      }
    }

    .btn {
      width: 100%;
      height: 40px;
      margin-top: 10px;
    }
  }
</style>
