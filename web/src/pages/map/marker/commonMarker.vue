<script setup lang="ts">
  import { computed, unref } from 'vue';

  import { CategoryEnum } from '@/enums';
  import { statusOptions } from '@/pages/policeAdmin/serviceStatus/common';
  import { useMapStore } from '@/store';

  const props = defineProps<{
    data: any;
    image: string;
  }>();

  const mapStore = useMapStore();

  const speedBgImg =
    'data:image/png;base64,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';

  const showInfo = computed(() => mapStore.showIconInfo);
  const showSpeed = computed(() => {
    const { category, speed } = props.data;
    return unref(showInfo) && category === CategoryEnum.carPhoto && speed;
  });
  const status = computed(() => {
    const { attendance } = props.data;
    let statusName = '';
    statusOptions().forEach((item) => {
      if (item.value === attendance) {
        statusName = item.label;
      }
    });
    return statusName;
  });
  const statusClass = computed(() => {
    const { attendance } = props.data;
    let color = '';

    switch (attendance) {
      case 1: {
        color = '#7ec8ff';
        break;
      }
      case 2: {
        color = '#8d8e8e';
        break;
      }
      case 3: {
        color = '#fca701';
        break;
      }
    }

    return { 'background-color': color };
  });
</script>

<template>
  <div
    :style="{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      width: '100px',
      height: '100px',
      fontSize: '9px',
      position: 'relative',
      paddingTop: '20px',
    }"
  >
    <div
      v-if="showSpeed"
      :style="{
        color: '#fff',
        textAlign: 'center',
        backgroundImage: `url(${speedBgImg})`,
        backgroundRepeat: 'no-repeat',
        backgroundSize: '100% 100%',
        padding: '6px',
      }"
    >
      {{ Number(data.speed).toFixed(2) }}
    </div>
    <img alt="" :src="image" style="width: 40px; height: 48px" />
    <div
      v-if="status"
      :style="{
        height: '16px',
        position: 'absolute',
        top: '50%',
        marginTop: '-16px',
        color: 'black',
        'line-height': '16px',
        'text-align': 'center',
        'font-size': '12px',
        padding: '0 2px',
        ...statusClass,
      }"
    >
      {{ status }}
    </div>
    <div
      v-if="showInfo"
      :style="{
        position: 'absolute',
        bottom: '28px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
      }"
    >
      <div
        :style="{
          color: '#fff',
          backgroundColor: '#3299E2',
          textAlign: 'center',
        }"
      >
        {{ data.name }}
      </div>
      <div
        :style="{
          color: '#fff',
          backgroundColor: '#3299E2',
          textAlign: 'center',
        }"
      >
        {{ data.organizationName }}
      </div>
    </div>
  </div>
</template>

<style scoped></style>
