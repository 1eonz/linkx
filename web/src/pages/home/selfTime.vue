<script lang="ts" setup>
  import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

  import { useI18n, useSetInterval } from '@/hooks';
  import dateUtil from '@/utils/dateUtil';

  defineProps<{
    showLunar?: boolean;
  }>();

  const isEnglish = computed(() => {
    return localStorage.getItem('localLanguage') === 'en';
  });
  const { t } = useI18n();

  const realTime = ref('');
  const simpleTime = ref('');
  const realDate = ref('');
  const simpleDate = ref('');
  const realWeek = ref('');
  const lunar = ref('');
  const enTime = ref('');
  const meridiem = ref('');
  const clearTimer: any = null;

  onMounted(() => {
    useSetInterval(updateTime, 1000, true);
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  function updateTime() {
    const now = new Date();
    // 英文日期格式需要单独处理
    if (isEnglish.value) {
      enTime.value = `${t(`common.dateDay.dayOfMonth${now.getMonth() + 1}`)} ${dateUtil.formatDate(
        now,
        'dd ,yyyy HH:mm',
      )}`;
      meridiem.value = now.getHours() <= 12 ? 'AM' : 'PM';
    } else {
      realTime.value = dateUtil.formatDate(now, 'HH:mm:ss');
      simpleTime.value = dateUtil.formatDate(now, 'HH:mm');
      realDate.value = dateUtil.formatDate(now, 'yyyy-MM-dd');
      simpleDate.value = dateUtil.formatDate(now, 'MM-dd');
      realWeek.value = t(`common.dateDay.dayOfWeek${now.getDay()}`);
    }
    getLunar();
  }

  // 获取农历
  function getLunar() {
    const m = new Intl.DateTimeFormat('zh-u-ca-chinese', {
      month: '2-digit',
    }).format(new Date());
    let d: number | string = Number(
      new Intl.DateTimeFormat('zh-u-ca-chinese', {
        day: '2-digit',
      })
        .format(new Date())
        .replace('日', ''),
    );
    const dStr = ['一', '二', '三', '四', '五', '六', '七', '八', '九', '十'];
    if (d <= 10) {
      d = `初${dStr[d - 1] || ''}`;
    } else if (d < 20) {
      d = `十${dStr[d - 10 - 1] || ''}`;
    } else if (d < 30) {
      d = `二十${dStr[d - 20 - 1] || ''}`;
    } else if (d < 40) {
      d = `三十${dStr[d - 30 - 1] || ''}`;
    }
    lunar.value = m + d;
  }
</script>

<template>
  <div class="self-time">
    <div v-if="isEnglish" class="top">
      <span>{{ enTime }}</span>
      <span>{{ meridiem }}</span>
    </div>
    <div v-else class="top">
      <span>{{ simpleDate }}</span>
      <span>{{ realWeek }}</span>
      <span>{{ simpleTime }}</span>
    </div>
    <div v-if="showLunar">
      <span>农历{{ lunar }}</span>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .self-time {
    span {
      margin-right: 6px;
      font-size: 14px;
      font-weight: 400;
      line-height: 14px;
      color: rgb(166 206 253 / 100%);
      letter-spacing: 0;
    }

    .top {
      display: flex;
      align-items: center;
      margin-bottom: 4px;
    }

    .date-normal {
      display: flex;
      align-items: center;
      margin-right: 5px;

      .data-shrink {
        margin-right: 6px;
      }
    }
  }
</style>
