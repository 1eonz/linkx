<script setup lang="ts">
  import { onMounted, ref } from 'vue';

  import { themeService } from '../../data/useTheme';

  const currentTheme = ref('light');
  const isLocked = ref(false);

  const toggleTheme = () => {
    const newTheme = themeService.toggleTheme();
    currentTheme.value = newTheme;
  };

  onMounted(() => {
    currentTheme.value = themeService.getCurrentTheme();
    isLocked.value = themeService.isLocked();

    themeService.onThemeChange((theme) => {
      currentTheme.value = theme;
    });
  });
</script>

<template>
  <button v-if="!isLocked" class="theme-toggle-btn" title="切换主题" @click="toggleTheme">
    <span v-if="currentTheme === 'light'">🌙</span>
    <span v-else>☀️</span>
  </button>
</template>

<style scoped>
  .theme-toggle-btn {
    padding: 2px 12px;
    color: var(--text-color);
    cursor: pointer;
    background: var(--button-bg);
    border: 1px solid var(--border-color);
    border-radius: 4px;
    transition: all 0.3s;
  }

  .theme-toggle-btn:hover {
    background: var(--hover-color);
  }
</style>
