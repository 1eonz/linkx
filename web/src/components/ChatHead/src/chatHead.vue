<script setup lang="ts">
  import { computed, ref, watchEffect } from 'vue';

  import groupBlueImg from '@/assets/images/pim/group.png';
  import groupGrayImg from '@/assets/images/pim/group_gray.png';
  import defaultImg from '@/assets/images/pim/im_person.svg';
  import { usePIMStore } from '@/store';

  defineOptions({
    name: 'TdChatHead',
  });

  const props = defineProps<{
    avatarId?: string;
    avatarUrl?: string;
    offline?: Boolean;
    sessionType?: null | number;
  }>();

  const PIMStore = usePIMStore();
  const imgUrl = ref(defaultImg);
  const groupImg = computed(() => {
    return props.offline ? groupGrayImg : groupBlueImg;
  });

  watchEffect(() => {
    const { avatarId, avatarUrl } = props;

    if (avatarId || avatarUrl) {
      loadImage(avatarId, avatarUrl);
    } else {
      imgUrl.value = defaultImg;
    }
  });

  function loadImage(avatarId, avatarUrl) {
    // 优先使用avatarUrl
    if (avatarUrl) {
      imgUrl.value = '/linkx/desktop' + avatarUrl;
      return;
    }

    if (!avatarId) {
      console.warn('TdChatHead: avatarId is empty');
      imgUrl.value = defaultImg;
      return;
    }

    const url = PIMStore.userAvatar.get(avatarId);
    if (url) {
      imgUrl.value = url;
    } else {
      // console.log('TdChatHead: fetching avatar for id:', avatarId);
      PIMStore.getUserAvatar(avatarId)
        .then(() => {
          const newUrl = PIMStore.userAvatar.get(avatarId);
          if (newUrl) {
            imgUrl.value = newUrl;
          }
        })
        .catch((error) => {
          console.error('TdChatHead: failed to load avatar:', error);
        });
    }
  }

  function handleImageError() {
    imgUrl.value = defaultImg;
  }
</script>

<template>
  <img v-if="sessionType === 2" alt="" class="td-chat-head" :src="groupImg" />
  <img v-else alt="" class="td-chat-head" :src="imgUrl" @error="handleImageError" />
</template>

<style scoped>
  .td-chat-head {
    display: inline-block;
    width: 36px;
    height: 36px;
    border-radius: 50%;
  }
</style>
