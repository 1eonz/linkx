<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { countEquipmentAndFacilities } from '@/api/count';
  import allImg from '@/assets/images/screen/all.png';
  import offlineImg from '@/assets/images/screen/offline.png';
  import onlineImg from '@/assets/images/screen/online.png';
  import { useBaseData, useI18n, useSetInterval } from '@/hooks';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';

  const props = defineProps({
    adCode: {
      default: '',
      type: String,
    },
    organizationId: {
      default: '',
      type: String,
    },
  });

  const emit = defineEmits(['change']);

  watch(
    () => props.adCode,
    () => {
      getOnlineCount();
    },
    { deep: true },
  );

  const { t } = useI18n();

  const activeTab = ref('all');
  let clearTimer: any = null;
  const online = ref(0);
  const offline = ref(0);

  const tabData = computed(() => {
    const on = online.value;
    const off = offline.value;
    return [
      {
        count: on + off,
        id: 'all',
        name: t('resource.equipmentNum.total'),
        src: allImg,
      },
      {
        count: on,
        id: 'online',
        name: t('resource.equipmentNum.online'),
        src: onlineImg,
      },
      {
        count: off,
        id: 'offline',
        name: t('resource.equipmentNum.offline'),
        src: offlineImg,
      },
    ];
  });

  onMounted(() => {
    getOnlineCount();
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  async function getOnlineCount() {
    clearTimer?.();

    const { adCode, organizationId } = props;
    clearTimer = adCode
      ? useSetInterval(
          async () => {
            const param: any = {
              adcode: adCode,
              organizationId,
            };
            const { code, data } = await countEquipmentAndFacilities(param);
            if (code === 0) {
              online.value = data.onlineCnt;
              offline.value = data.offlineCnt;
            }
          },
          15 * 1000,
          true,
        )
      : useSetInterval(
          () => {
            const { getResourceOrigin } = useBaseData();
            let on = 0;
            let off = 0;
            const origin = getResourceOrigin;
            Object.keys(origin).forEach((category) => {
              origin[category].forEach((item) => {
                if (getOnlineStatus(item) === 'online') {
                  on++;
                } else {
                  off++;
                }
              });
            });
            online.value = on;
            offline.value = off;
          },
          3 * 1000,
          true,
        );
  }

  function tabChange({ id }) {
    activeTab.value = id;
    emit('change', id);
  }
</script>

<template>
  <div class="equipment-count">
    <div
      v-for="(item, index) in tabData"
      :key="index"
      class="item"
      :class="{ active: activeTab === item.id }"
      @click="tabChange(item)"
    >
      <img alt="" class="img" :src="item.src" />
      <div class="text">
        <span class="name">{{ item.name }}</span>
        <span class="count">{{ item.count }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .equipment-count {
    display: flex;
    height: 58px;
    margin-top: 10px;
    background: linear-gradient(180deg, rgb(19 52 80 / 30%) 0%, rgb(19 54 83 / 80%) 100%);

    .item {
      display: flex;
      flex: 1;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      .count {
        font-size: 20px;
        font-weight: 900;
        line-height: 23.44px;
        letter-spacing: 0;
      }

      .text {
        display: flex;
        flex-direction: column;
        opacity: 0.8;
      }

      .img {
        width: 28px;
        height: 28px;
      }

      .name {
        font-size: 12px;
        font-weight: 400;
        line-height: 17.38px;
        letter-spacing: 0;
      }

      &.active .text {
        opacity: 1;
      }

      &:nth-of-type(1) {
        .count {
          color: rgb(10 224 240 / 100%);
        }

        .name {
          color: rgb(10 224 240 / 100%);
        }
      }

      &:nth-of-type(2) {
        .count {
          color: rgb(0 255 128 / 100%);
        }

        .name {
          color: rgb(0 255 128 / 100%);
        }
      }

      &:nth-of-type(3) {
        .count {
          color: rgb(196 196 196 / 100%);
        }

        .name {
          color: rgb(196 196 196 / 100%);
        }
      }
    }
  }
</style>
