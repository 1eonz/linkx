<script lang="ts" setup>
  import { computed, nextTick, ref } from 'vue';

  import { useEmitter } from '@/hooks';

  const voiceRef = ref();
  const type = ref<any>('');

  const isEnglish = computed(() => {
    return localStorage.getItem('localLanguage') === 'en';
  });

  useEmitter('playVoiceRemind', playVoice);

  function playVoice(val) {
    if (voiceRef.value?.paused === false) {
      return;
    }
    type.value = val;
    nextTick(() => {
      voiceRef.value?.play();
    });
  }
</script>

<template>
  <div class="voice-remind">
    <div v-if="isEnglish">
      <audio v-if="type === 1" ref="voiceRef">
        <source src="@/assets/audio/warn_en.mp3" type="audio/mpeg" />
      </audio>
      <audio v-if="type === 2" ref="voiceRef">
        <source src="@/assets/audio/task_en.mp3" type="audio/mpeg" />
      </audio>
      <audio v-if="type === 3" ref="voiceRef">
        <source src="@/assets/audio/message_en.mp3" type="audio/mpeg" />
      </audio>
    </div>
    <div v-else>
      <audio v-if="type === 1" ref="voiceRef">
        <source src="@/assets/audio/warn_cn.mp3" type="audio/mpeg" />
      </audio>
      <audio v-else-if="type === 2" ref="voiceRef">
        <source src="@/assets/audio/task_cn.mp3" type="audio/mpeg" />
      </audio>
      <audio v-else-if="type === 3" ref="voiceRef">
        <source src="@/assets/audio/message_cn.mp3" type="audio/mpeg" />
      </audio>
    </div>
  </div>
</template>

<style lang="less" scoped></style>
