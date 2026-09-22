<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { useI18n } from '@/hooks';
  import { remindColor } from '@/pages/videoControl/common';

  import { Timeout } from '#/index';

  const props = defineProps({
    infoData: {
      default() {
        return null;
      },
      type: Object,
    },
    popTime: {
      default: '',
      type: String,
    },
    remindLevel: {
      default: '',
      type: String,
    },
    type: {
      default: 1,
      type: Number,
    },
  });

  const emit = defineEmits(['closeDialog']);

  const { t } = useI18n();
  const loading = ref(true);
  const detail = ref('');
  const typeName = ref('');
  const iconName = ref('');
  let timer: Timeout;

  watch(
    () => props.type,
    (val) => {
      switch (val) {
        case 1: {
          typeName.value = t('homePage.mapToolData.alarm');
          iconName.value = 'coverage_alarm';
          break;
        }
        case 2: {
          typeName.value = t('homePage.mapToolData.task');
          iconName.value = 'coverage_task';
          break;
        }
        case 3: {
          typeName.value = t('homePage.menus.emergencyMessages');
          iconName.value = 'coverage_messages';
          break;
        }
      }
    },
    { immediate: true },
  );
  const isEnglish = computed(() => {
    return localStorage.getItem('localLanguage') === 'en';
  });
  const describe = computed(() => {
    if (isEnglish.value) {
      return `You have a new ${typeName.value}, please act on it.`;
    }
    return `您有一条新${typeName.value}，请及时处理。`;
  });

  const remindColorBg = computed(() => {
    const color = remindColor(props.remindLevel);
    return color;
  });

  onMounted(() => {
    getDetail();
  });

  async function getDetail() {
    const time: any = props.popTime || 3;
    const { name, title } = props.infoData;
    detail.value = title || name;
    loading.value = false;
    timer = setTimeout(() => {
      emit('closeDialog');
    }, time * 1000);
  }
  function closeCard() {
    clearTimeout(timer);
    loading.value = true;
    emit('closeDialog');
  }
</script>

<template>
  <div class="alarm-remind-card" :class="remindColorBg">
    <div class="remind-header">
      <div class="header-left">
        <Icon class="icon-type" :name="iconName" />
        <div class="header-name">
          {{ typeName }}<span v-if="isEnglish">&nbsp;</span>{{ t('common.windowTitle.tips') }}
        </div>
      </div>
      <Icon class="close-btn" name="close_red" @click="closeCard" />
    </div>
    <div
      v-loading="loading"
      class="remind-box"
      element-loading-background="rgba(122, 122, 122, 0.8)"
    >
      <div class="remind-type"> {{ describe }} </div>
      <div class="remind-title"> {{ detail }} </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .alarm-remind-card {
    width: 310px;
    height: 94px;

    .remind-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 34px;
      padding: 0 8px;

      .header-left {
        display: flex;
        align-items: center;
        height: 34px;

        .icon-type {
          width: 18px;
          height: 18px;
          margin-right: 4px;

          .header-name {
            font-size: 16px;
          }
        }
      }

      .close-btn {
        width: 20px;
        height: 20px;
        cursor: pointer;
      }
    }

    .remind-box {
      padding: 8px;

      .remind-type {
        font-size: 14px;
        font-weight: 500;
        line-height: 20px;
        color: var(--text-title-first);
      }

      .remind-title {
        font-size: 12px;
        font-weight: 400;
        line-height: 22px;
        .ellipsis1();
      }
    }
  }

  .red {
    background: url('@/assets/images/popup/popup_red.png') no-repeat;
    background-size: 100% 100%;
  }

  .orange {
    background: url('@/assets/images/popup/popup_orange.png') no-repeat;
    background-size: 100% 100%;
  }

  .yellow {
    background: url('@/assets/images/popup/popup_yellow.png') no-repeat;
    background-size: 100% 100%;
  }

  .blue {
    background: url('@/assets/images/popup/popup_blue.png') no-repeat;
    background-size: 100% 100%;
  }
</style>
