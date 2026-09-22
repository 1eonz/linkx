<script setup lang="ts">
  import { addressStr } from '@/pages/policeAdmin/serviceStatus/common';

  defineProps<{
    data: any;
    image: string;
  }>();
</script>

<template>
  <div style="position: relative; display: flex; justify-content: center; text-align: center">
    <img alt="" :src="image" style="width: 29px; height: 29px" />
    <div
      style="
        position: absolute;
        bottom: -16px;
        left: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 54px;
        height: 16px;
        margin-left: -27px;
        font-size: 10px;
        line-height: 16px;
        color: #fff;
        text-align: center;
        background-color: rgb(50 153 227 / 100%);
      "
    >
      {{ addressStr(data.status) }}
    </div>
  </div>
</template>
