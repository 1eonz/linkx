<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { useI18n } from '@/hooks';
  import { voicePointCall } from '@/pages/resource/resourceHelper';

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
  });

  const { t } = useI18n();

  const collect = ref(false);

  onMounted(() => {
    collect.value = props.data.favorite;
  });

  async function handleCall() {
    const account = props.data.phone;
    voicePointCall(account, true);
  }
</script>

<template>
  <div class="map-landmark-popup">
    <div class="landmark-title"> {{ data.name }} </div>
    <div class="landmark-details">
      <div class="item">{{ data.address }}</div>
      <div class="item">
        <span>{{ `${t('event.eventCenter.phone')}：${data.phone}` }}</span>
        <Icon class="icon" name="phone_call" @click.stop="handleCall" />
      </div>
      <div class="item">
        <span>{{ `${t('resource.policeResourceData.contact')}：${data.contact}` }}</span>
      </div>
      <div class="item">
        <span>{{ `${t('videoControl.warningInformation.description')}：${data.remark}` }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .map-landmark-popup {
    width: 100%;

    .landmark-title {
      width: 274px;
      font-size: 16px;
      font-weight: bold;
      line-height: 24px;
      color: var(--text-title-first);
      word-break: break-all;
    }

    .landmark-details {
      .item {
        display: flex;
        font-size: 12px;
        font-weight: 400;
        line-height: 20px;
        color: var(--text-title-second);
        word-break: break-all;

        span {
          font-size: 12px;
          line-height: 20px;
          color: var(--text-title-second);
        }

        .icon {
          width: 14px;
          height: 14px;
          margin-top: 3px;
          margin-left: 5px;
          cursor: pointer;
          fill: var(--button-link);
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
