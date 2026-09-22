<script setup lang="ts">
  import { onMounted } from 'vue';

  import { downloadFile } from '@/bridge/post.js';

  import { getIp } from '@/utils';

  type dataType = {
    fileKey: string;
    fileName: string;
    filePath?: string;
    fileSize: number;
    fileType: number;
  };

  const props = defineProps<{
    cardData: dataType;
  }>();

  onMounted(() => {});

  async function fileClick() {
    const fileUrl = `${getIp()}/linkx/desktop${props.cardData.filePath}`
    await downloadFile({
      url: fileUrl,
      fileName: props.cardData.fileName,
    })
  }

  function getIconName() {
    switch (props.cardData.fileType) {
      case 1: {
        return 'file_image';
      }
      case 3: {
        return 'file_video';
      }
      case 5: {
        return 'file_word';
      }
      case 6: {
        return 'file_excel';
      }
      case 7: {
        return 'file_pdf';
      }
      case 8: {
        return 'file_txt';
      }
      case 10: {
        return 'file_ppt';
      }
      default: {
        return 'file_unidentified';
      }
    }
  }
</script>

<template>
  <div class="file-box" @click="fileClick">
    <div class="file-info">
      <span class="name">{{ cardData.fileName }}</span>
      <span class="size">{{ `${(cardData.fileSize / 1024).toFixed(2)}KB` }}</span>
    </div>
    <Icon class="file-icon" :name="getIconName()" prefix="im" />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .file-box {
    display: flex;
    width: 240px;
    padding: 10px;
    margin-top: 5px;
    margin-bottom: 5px;
    cursor: pointer;
    background: rgb(255 255 255);
    border: 1px solid rgb(38 78 209 / 10%);
    border-radius: 7px;

    .file-icon {
      width: 30px;
      height: 40px;
    }

    .file-info {
      display: grid;
      width: 190px;

      .name {
        font-size: 14px;
        font-weight: 500;
        color: rgb(26 26 26);
        .ellipsis1();
      }

      .size {
        font-size: 12px;
        color: rgb(102 102 102);
      }
    }
  }
</style>
