<script setup lang="ts">
  defineProps<{
    image: string;
    text: string;
  }>();
</script>

<template>
  <div
    :style="{
      display: 'flex',
      'flex-direction': 'column',
      'align-items': 'center',
    }"
  >
    <img alt="" :src="image" :style="{ width: '38px', height: '38px' }" />
    <div
      :style="{
        'white-space': 'nowrap',
        padding: '0 6px',
        height: '26px',
        'line-height': '26px',
        'font-size': '12px',
        color: '#fff',
        background: `linear-gradient(180deg, rgba(6 41 74 / 64%) 0%, rgba(6 41 74 / 26%) 100%)`,
        'backdrop-filter': `blur(8px)`,
        border: '1px solid transparent',
        'border-image': `linear-gradient(180deg, rgba(26 255 251 / 20%) 0%, rgba(26 255 251 / 100%) 100%)`,
        'border-image-slice': 1,
      }"
    >
      {{ text }}
    </div>
  </div>
</template>
