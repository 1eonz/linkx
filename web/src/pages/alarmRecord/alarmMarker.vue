<script setup lang="ts">
  defineProps<{
    cluster?: boolean;
    clusterImage?: string;
    color: string;
    count?: number | string;
    image?: string;
    text: string;
  }>();
</script>

<template>
  <div
    :style="{
      display: 'flex',
      'flex-direction': 'column',
      'justify-content': 'center',
      'align-items': 'center',
      width: '54px',
      height: '64px',
      position: 'relative',
    }"
  >
    <img v-if="cluster" alt="" :src="clusterImage" style="width: 36px; height: 32px" />
    <img v-else alt="" :src="image" style="width: 24px; height: 32px" />

    <span :style="{ color: '#ff3b55', position: 'absolute', top: '12px', 'font-size': '12px' }">
      {{ count }}
    </span>

    <div
      :style="{
        color: '#fff',
        'font-size': '12px',
        padding: '0 1px',
        'background-color': color,
      }"
    >
      {{ text }}
    </div>
  </div>
</template>

<style scoped></style>
