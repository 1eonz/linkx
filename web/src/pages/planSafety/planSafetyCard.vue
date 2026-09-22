<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { collectPlanGroup, deletePlanGroup, stickPlanGroup } from '@/api/plan';
  import MessageBox from '@/components/MessageBox';
  import { useI18n, useSetInterval } from '@/hooks';
  import { getTimeStr } from '@/pages/alarmRecord/alarmCommon';
  import {
    getLevelColor,
    getStatusColor,
    getStatusText,
    getSupportLevel,
    getSupportType,
  } from '@/pages/planSafety/common';
  import dateUtil from '@/utils/dateUtil';

  type dataType = {
    active: boolean;
    collectStatus: number;
    id: string;
    state: number;
    stickStatus: number;
    supportAddress: string;
    supportBeginTime: string;
    supportEndTime: string;
    supportLevel: string;
    supportName: string;
    supportRemindTime: number;
    supportType: string;
  };

  const props = defineProps<{
    cardData: dataType;
  }>();
  const emit = defineEmits(['cardClick', 'click', 'detailClick', 'operation']);
  const { t } = useI18n();

  let clearTimer: any = null; // 倒计时
  const status = ref(props.cardData.state);
  const isCollect = ref(props.cardData.collectStatus === 1);
  const isUp = ref(props.cardData.stickStatus === 1);
  const lastTime = ref('');

  watch(
    () => props.cardData.collectStatus,
    (val) => {
      isCollect.value = val === 1;
    },
  );

  watch(
    () => props.cardData.stickStatus,
    (val) => {
      isUp.value = val === 1;
    },
  );

  onMounted(() => {
    // 倒计时计算
    clearTimer = useSetInterval(() => {
      handleTime();
    }, 1000);
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  // 倒计时时间格式化
  function handleTime() {
    // 只有未开始的保障才需要计算倒计时
    if (status.value !== 0) {
      return;
    }
    const endTimeDate = new Date(Number(props.cardData.supportBeginTime)).getTime();
    const currentTime = Date.now();
    const time = endTimeDate - currentTime;
    const { day, hour, minute, second } = dateUtil.getHMSByMsec(time);
    const countHour = day * 24 + Number(hour); // 换算成小时累加
    lastTime.value =
      t('planSafety.remainder') +
      countHour +
      t('statistics.unit.hour') +
      minute +
      t('common.dateDay.minute') +
      second +
      t('common.dateDay.second');
    if (time <= 1000) {
      clearTimer?.();
      status.value = 1;
    }
  }

  function editPlan() {
    emit('operation', 'edit');
  }

  function detailPlan() {
    emit('detailClick', props.cardData);
  }

  async function handleDeleteClick() {
    const affirm = await MessageBox({
      cancelText: t('planSafety.message.cancel'),
      confirmText: t('planSafety.message.delete'),
      offset: ['40%', '35%'],
      text: t('planSafety.message.deleteInfo'),
    });
    if (!affirm) return;
    const { code } = await deletePlanGroup({ id: props.cardData.id });
    if (code === 0) {
      emit('click');
    }
  }

  async function handleCopyClick() {
    // const affirm = await MessageBox({
    //   text: t('planSafety.message.copyInfo'),
    //   offset: ['40%', '35%'],
    //   confirmText: t('planSafety.message.copy'),
    //   cancelText: t('planSafety.message.cancel'),
    // });
    // if (!affirm) return;
    emit('operation', 'copy');
  }

  async function handleUpClick() {
    const { code } = await stickPlanGroup({ id: props.cardData.id, state: !isUp.value });
    if (code === 0) {
      emit('click');
    }
  }

  function cardClick() {
    emit('cardClick', props.cardData.id);
  }

  async function handleCollectClick() {
    const { code } = await collectPlanGroup({ id: props.cardData.id, state: !isCollect.value });
    if (code === 0) {
      emit('click');
    }
    // isCollect.value = !isCollect.value;
  }
</script>

<template>
  <div class="card-box" :class="{ active: cardData.active }" @click="cardClick">
    <div class="card-header">
      <div class="header-tag">
        <TdTag class="img" :label="getStatusText(status)" :type="getStatusColor(status)" />
        <TdTag class="img" :label="getSupportType(cardData.supportType)" type="blue" />
        <TdTag
          class="img"
          :label="getSupportLevel(cardData.supportLevel)"
          :type="getLevelColor(cardData.supportLevel)"
        />
      </div>
      <div class="header-button">
        <TdTooltip :content="t('common.delete')">
          <Icon class="icon" name="option_delete" @click.stop="handleDeleteClick()" />
        </TdTooltip>
        <TdTooltip :content="t('planSafety.message.copy')">
          <Icon class="icon" name="table_copy" @click.stop="handleCopyClick()" />
        </TdTooltip>
        <TdTooltip
          :content="
            isUp ? t('planSafety.message.cancel') + t('planSafety.top') : t('planSafety.top')
          "
        >
          <Icon
            class="icon"
            :class="{ 'icon-active': isUp }"
            name="table_up"
            @click.stop="handleUpClick()"
          />
        </TdTooltip>
        <TdTooltip
          :content="
            isCollect
              ? t('planSafety.message.cancel') + t('planSafety.collect')
              : t('planSafety.collect')
          "
        >
          <Icon
            class="icon"
            :name="isCollect ? 'btn_uncollected' : 'btn_collected'"
            @click.stop="handleCollectClick()"
          />
        </TdTooltip>
      </div>
    </div>
    <ElDivider class="divider" />
    <div class="card-body">
      <TdTooltip :content="cardData.supportName" placement="top">
        <div class="title">{{ cardData.supportName }}</div>
      </TdTooltip>
      <div class="address">
        <Icon class="icon" name="category_position" />
        <TdTooltip :content="cardData.supportAddress" placement="top">
          <span>{{ cardData.supportAddress }}</span>
        </TdTooltip>
      </div>
      <div class="time">
        <div class="time-text">
          <Icon class="icon" name="category_time" />
          <span>
            {{
              `${getTimeStr(Number(cardData.supportBeginTime))} ~ ${getTimeStr(
                Number(cardData.supportEndTime),
              )}`
            }}
          </span>
        </div>
        <div v-if="status === 0 && lastTime" class="time-text">
          <Icon class="icon" name="card_end_time" />
          <span class="after">{{ lastTime }}</span>
        </div>
      </div>
    </div>
    <div class="card-button">
      <TdButton class="btn" :text="t('mission.missionList.edit')" @click="editPlan" />
      <TdButton class="btn" :text="t('planSafety.detail')" @click="detailPlan" />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .card-box {
    width: 470px;
    min-height: 170px;
    padding: 16px;
    margin-top: 15px;
    border: 1px solid rgb(77 147 201 / 60%);

    &:hover {
      border: 1px solid rgb(26 255 251 / 100%);
    }
  }

  .active {
    border: 1px solid rgb(26 255 251 / 100%);
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 18px;

    .header-tag {
      .img {
        height: 18px;
        margin-left: 5px;
      }
    }

    .header-button {
      .icon {
        display: inline-block;
        margin-left: 10px;
        cursor: pointer;

        &-active {
          fill: #fca701;
        }
      }
    }
  }

  .card-body {
    width: 100%;

    .icon {
      width: 13px;
      height: 15px;
    }

    .title {
      max-width: 430px;
      font-size: 14px;
      font-weight: 700;
      line-height: 22px;
      color: rgb(255 255 255 / 100%);
      letter-spacing: 0;
      .ellipsis1();
    }

    .address {
      display: flex;

      span {
        max-width: 400px;
        margin-left: 5px;
        font-size: 12px;
        font-weight: 400;
        line-height: 20px;
        color: rgb(153 206 251 / 100%);
        letter-spacing: 0;
        .ellipsis1();
      }
    }

    .time {
      display: flex;
      justify-content: space-between;

      .time-text {
        display: flex;
        align-items: center;

        .icon {
          width: 14px;
          height: 14px;
        }

        span {
          margin-left: 5px;
          font-size: 12px;
          font-weight: 400;
          line-height: 20px;
          color: rgb(153 206 251 / 100%);
          letter-spacing: 0;
        }

        .after {
          color: rgb(255 96 96 / 100%);
        }
      }
    }
  }

  .divider {
    width: 436px;
    margin: 10px 0;
    border-color: rgb(77 147 201 / 60%);
  }

  .card-button {
    display: flex;
    justify-content: space-between;
    margin-top: 10px;

    .btn {
      width: 200px;
      height: 32px;
    }
  }
</style>
