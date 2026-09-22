<script lang="ts" setup>
  // 1 车牌 2 人脸
  import { computed, ref, watchEffect } from 'vue';

  import defaultImg from '@/assets/images/event/photos.png';
  import { useI18n } from '@/hooks';
  import { getIp } from '@/utils';

  const props = defineProps({
    controlPersonInfo: {
      default: () => {},
      type: Object,
    },
    isIdentifyObj: {
      default: false,
      type: Boolean,
    },
    messageInfo: {
      default: () => {},
      type: Object,
    },
    metaData: {
      default: () => {},
      type: Object,
    },
    missonCard: {
      default: false,
      type: Boolean,
    },
    type: {
      default: 1,
      type: Number,
    },
    vehicleObjectInfo: {
      default: () => {},
      type: Object,
    },
  });

  const emit = defineEmits(['clickItem']);
  const { t } = useI18n();
  const photoImg = ref('');
  const optionsType1 = [
    {
      field: 'plateNo',
      name: t('videoControl.warningInformation.plateNo'),
    },
    {
      field: 'ownerId',
      name: t('videoControl.warningInformation.ownerId'),
    },
    {
      field: 'ownerName',
      name: t('videoControl.warningInformation.ownerName'),
    },
    {
      field: 'description',
      name: t('videoControl.warningInformation.description'),
    },
  ];
  const optionsType2 = [
    {
      field: 'credentialNumber',
      name: '',
    },
    {
      field: 'country',
      name: t('videoControl.warningInformation.nationality'),
    },
    {
      field: 'bornTime',
      name: t('videoControl.warningInformation.dateBirth'),
    },
  ];
  const optionsType3 = [
    {
      field: 'OfficerName',
      name: t('videoControl.warningInformation.policeOfficerName'),
    },
    {
      field: 'affiliatedUnit',
      name: t('videoControl.warningInformation.affiliatedUnit'),
    },
    {
      field: 'phone',
      name: t('videoControl.warningInformation.phone'),
    },
  ];

  const metaDataInfo = computed(() => {
    return props.metaData || {};
  });
  const showGender = computed(() => {
    return [-1, 0, 1].includes(props.controlPersonInfo?.gender);
  });
  const showOptions = computed(() => {
    // 1 车牌 2 人脸 3紧急事件
    let val;
    switch (props.type) {
      case 1: {
        val = optionsType1;
        break;
      }
      case 2: {
        val = optionsType2;
        break;
      }
      case 3: {
        val = optionsType3;
        break;
      }
    }
    return val;
  });

  watchEffect(() => {
    const type = props.controlPersonInfo?.credentialType || 0;
    const credentialName = [
      t('videoControl.warningInformation.idCard'),
      t('videoControl.warningInformation.passport'),
      t('videoControl.warningInformation.studentCard'),
      t('videoControl.warningInformation.militaryOfficer'),
      t('videoControl.warningInformation.drivingLicense'),
      t('videoControl.warningInformation.other'),
    ];
    optionsType2[0].name = credentialName[type];
  });

  watchEffect(() => {
    const { faceImageUrl } = props.controlPersonInfo || {};
    setPhotoIamge(faceImageUrl);
  });

  function openWarningPersonCard() {
    if (props.isIdentifyObj) {
      emit('clickItem');
    }
  }
  function getGender() {
    const gender = props.controlPersonInfo?.gender;
    // 新增未知性别
    if (gender === -1) {
      return t('event.eventCenter.unknown');
    }
    return gender === 0 ? t('event.eventCenter.male') : t('event.eventCenter.female');
  }

  function setPhotoIamge(url) {
    if (props.type === 3) {
      photoImg.value = defaultImg;
      return;
    }
    const src = url ? `${getIp()}/iap${url}` : '';
    const img = new Image();

    img.addEventListener('load', () => {
      photoImg.value = src;
    });
    img.onerror = () => {
      photoImg.value = defaultImg;
    };
    img.src = src;
  }
</script>

<template>
  <div class="common-list-item warning-notice-item">
    <div class="info-left">
      <div v-if="[2, 3].includes(type)" class="head-photo">
        <img alt="" class="head-photo-image" :src="photoImg" />
      </div>
    </div>

    <div v-if="!missonCard && type === 2 && metaDataInfo.scr" class="seal-result">
      {{ t('videoControl.warningInformation.match') }} {{ metaDataInfo.scr }}%
    </div>

    <div class="info-right">
      <div v-if="type === 2" class="info-right-flex">
        <span class="info-name" @click="openWarningPersonCard">
          {{ controlPersonInfo.name }}
        </span>
        <TdTag
          v-if="controlPersonInfo.occupation"
          class="info-occupation"
          :label="controlPersonInfo.occupation"
          type="red"
        />
      </div>

      <div v-if="showGender" class="info-right-flex">
        <span class="item-name">
          {{ t('personCenter.sex') }}
        </span>
        <div class="item-description">
          {{ getGender() }}
        </div>
      </div>

      <div v-for="item in showOptions" :key="item.field" class="info-right-flex">
        <span class="item-name">
          {{ item.name }}
        </span>
        <div class="item-description">
          <template v-if="type === 1 && vehicleObjectInfo">
            {{ vehicleObjectInfo[item.field] || '' }}
          </template>
          <template v-if="type === 2 && controlPersonInfo">
            {{ controlPersonInfo[item.field] || '' }}
          </template>
          <template v-if="type === 3 && messageInfo">
            {{ messageInfo[item.field] || '' }}
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .warning-notice-item {
    position: relative;
    display: flex;
    width: 100%;

    .info-left {
      display: flex;
      align-items: center;
      justify-content: center;

      .head-photo {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 53px;
        height: 72px;
        margin-right: 8px;
        overflow: hidden;
        line-height: 24px;

        .head-photo-image {
          max-width: 100%;
          max-height: 100%;
          border-radius: 4px;
        }
      }
    }

    .info-right {
      position: relative;
      display: flex;
      flex-direction: column;

      .info-right-flex {
        display: flex;
        font-size: 14px;

        .info-name {
          margin-right: 8px;
          font-weight: 600;
          word-break: break-all;
        }

        .item-name {
          min-width: 42px;
          margin-right: 8px;
          color: var(--text-title-second);
        }
      }
    }

    .seal-result {
      position: absolute;
      top: 2px;
      right: 2px;
      width: 60px;
      height: 60px;
      font-size: 12px;
      font-weight: bold;
      color: var(--icon-color-light);
      text-align: center;
      border: solid 2px var(--icon-color-light);
      border-radius: 100%;
      transform: rotate(26deg);
    }
  }
</style>
