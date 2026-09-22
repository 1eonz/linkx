<script lang="ts" setup>
  import { ref } from 'vue';

  import { useI18n } from '@/hooks';
  import { voicePointCall } from '@/pages/resource/resourceHelper';

  defineProps<{
    data: any;
  }>();
  const { t } = useI18n();
  const isShowDetail = ref(true);

  function showDetail() {
    isShowDetail.value = !isShowDetail.value;
  }

  async function dialAudio(phoneNum) {
    voicePointCall(phoneNum, true);
  }
</script>

<template>
  <div class="map-defense-popup">
    <div class="info-box">
      <div class="name">{{ data.name }}</div>
      <div class="layer-name">{{ data.customLayerName }}</div>
      <div class="show-btn" @click="showDetail">
        {{ isShowDetail ? t('common.putAway') : t('common.open') }}
      </div>
      <div v-if="isShowDetail" class="detail">
        <div class="box">
          <div class="label">{{ `${t('planSafety.detailTabs.address')}：` }}</div>
          <div class="text">{{ data.address }}</div>
        </div>
        <div class="box">
          <div class="label">{{ `${t('monitor.monitorDetail.unit')}：` }}</div>
          <div class="text">{{ data.organizationUnit }}</div>
        </div>
        <div class="box">
          <div class="label">{{ `${t('resource.policeResourceData.contact')}：` }}</div>
          <div class="text">{{ data.contactPerson }}</div>
        </div>
        <div class="box">
          <div class="label">{{ `${t('planSafety.detailTabs.phone')}：` }}</div>
          <div class="text">{{ data.phoneNum }}</div>
          <div v-if="data.phoneNum" class="call" @click="dialAudio(data.phoneNum)">
            <Icon class="icon" name="defense_call" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .map-defense-popup {
    width: 100%;

    .info-box {
      align-items: center;

      .name {
        font-size: 16px;
        font-weight: 500;
        line-height: 24px;
        color: rgb(255 255 255 / 100%);
        letter-spacing: 0;
      }

      .layer-name {
        font-size: 14px;
        font-weight: 400;
        line-height: 22px;
        color: rgb(120 168 222 / 100%);
        letter-spacing: 0;
      }

      .show-btn {
        width: 30px;
        height: 20px;
        margin-bottom: 5px;
        margin-left: 90%;
        font-size: 12px;
        font-weight: 400;
        line-height: 17.38px;
        color: rgb(26 255 251 / 100%);
        text-align: center;
        letter-spacing: 0;
        cursor: pointer;
        background: rgb(1 205 253 / 10%);
        border: 1px solid rgb(0 0 0 / 10%);
      }

      .detail {
        padding: 10px;
        background: linear-gradient(
          90deg,
          rgb(31 115 159 / 16%) 0%,
          rgb(21 154 255 / 16%) 0%,
          rgb(153 206 251 / 16%) 100%
        );
        border: 1px solid rgb(77 147 201 / 60%);

        .box {
          display: flex;

          .label {
            color: rgb(120 168 222 / 100%);
          }

          .call {
            width: 12px;
            margin-left: 5px;

            .icon {
              width: 12px;
              height: 12px;
              margin-top: 5px;
              cursor: pointer;
            }
          }
        }
      }
    }
  }
</style>
