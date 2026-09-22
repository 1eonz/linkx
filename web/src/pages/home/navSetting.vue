<script lang="ts" setup>
  import { computed, ref } from 'vue';

  import { useI18n } from '@/hooks';

  import PasswordModify from '../personCenter/passwordModify.vue';
  import PersonCenter from '../personCenter/personCenter.vue';
  import EditionSetting from './editionSetting.vue';

  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();
  const chooseCompName = ref('PersonCenter');
  const settingConfig = ref([
    {
      compName: 'PersonCenter',
      label: t('personCenter.personCenter'),
    },
    // {
    //   compName: 'EditionSetting',
    //   label: t('homePage.navigateSetData.softwareVersion'),
    // },
    {
      compName: 'PasswordModify',
      label: t('personCenter.passModify.passModify'),
    },
  ]);

  const chooseComp = computed(() => {
    const comp = {
      EditionSetting,
      PasswordModify,
      PersonCenter,
    };

    return comp[chooseCompName.value];
  });

  function setActive(compName) {
    chooseCompName.value = compName;
  }
  function closeWindow() {
    emit('closeDialog');
  }
</script>

<template>
  <div class="setting-window">
    <div class="nav-top">
      <div
        v-for="(item, index) in settingConfig"
        :key="index"
        class="label"
        :class="chooseCompName === item.compName ? 'active-label' : ''"
        @click="setActive(item.compName)"
      >
        {{ item.label }}
      </div>
    </div>
    <div class="nav-content">
      <component :is="chooseComp" @close-dialog="closeWindow" />
    </div>

    <Icon class="close-pic" name="close" @click="closeWindow" />
  </div>
</template>

<style lang="less" scoped>
  .setting-window {
    left: 27%;
    width: 420px;
    height: 462px;
    background: var(--chat-history-bg);
    border: 1px solid rgb(0 0 0 / 6%);

    .nav-top {
      display: flex;
      align-items: center;
      height: 42px;
      border: 1px solid rgb(0 0 0 / 6%);

      .label {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 110px;
        height: 42px;
        font-size: 16px;
        font-weight: 400;
        color: var(--item-text-color);
        text-align: center;

        &:hover {
          cursor: pointer;
        }
      }

      .active-label {
        font-weight: 500;
        color: var(--text-color);
        border-bottom: 3px solid var(--button-active-color);
      }
    }

    .nav-content {
      width: 420px;
      height: 420px;
      padding: 20px 35px 20px 20px;
    }
  }

  .close-pic {
    position: absolute;
    top: 10px;
    right: 12px;
    width: 20px;
    height: 20px;
    cursor: pointer;
    filter: var(--svg-filter);
  }
</style>
