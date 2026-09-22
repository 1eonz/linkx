<script setup lang="ts">
  import { computed } from 'vue';

  import { statusOptions } from '@/pages/policeAdmin/serviceStatus/common';
  import { useMapStore } from '@/store';

  const props = defineProps<{
    data: any;
    image: string;
  }>();

  const mapStore = useMapStore();

  const showInfo = computed(() => mapStore.showIconInfo);
  const policeStatus = computed(() => {
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
      // width: '40px',
      // height: '48px',
      position: 'relative',
      fontSize: '8px',
      paddingTop: '20px',
    }"
  >
    <img alt="" :src="image" style="width: 40px; height: 48px" />
    <div
      v-if="policeStatus"
      :style="{
        height: '16px',
        position: 'absolute',
        bottom: '16px',
        color: 'black',
        'line-height': '16px',
        'text-align': 'center',
        'font-size': '12px',
        padding: '0 2px',
        ...statusClass,
      }"
    >
      {{ policeStatus }}
    </div>
    <div
      v-if="showInfo"
      :style="{
        position: 'absolute',
        bottom: '-4px',
      }"
    >
      <div
        :style="{
          'text-align': 'center',
          color: '#fff',
          'white-space': 'nowrap',
          backgroundColor: '#3299E2',
        }"
      >
        {{ data.name }}
      </div>
      <div
        :style="{
          color: '#fff',
          backgroundColor: '#3299E2',
        }"
      >
        {{ data.organizationName }}
      </div>
    </div>
  </div>
</template>
