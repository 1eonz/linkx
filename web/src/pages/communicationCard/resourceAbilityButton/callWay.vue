<script lang="ts" setup>
  import type { PropType } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import commOpt from '@/plugins/mspPlayer/commOpt';

  type ServiceAccounts = {
    account: string;
    id: string;
    typeName: string;
  };

  defineProps({
    serviceAccounts: {
      default: () => [],
      type: Array as PropType<ServiceAccounts[]>,
    },
  });
  const emit = defineEmits(['select', 'closeDialog']);

  const { t } = useI18n();

  function manPointCall(item: any) {
    if (!commOpt.isCommReady()) {
      return;
    }

    emit('select', item.account, item);
    Dialog('CallWay')?.close();
  }

  function handleClickOutside() {
    emit('closeDialog');
  }
</script>

<template>
  <TdClickOutSide class="voice-call-btn" @click-outside="handleClickOutside">
    <div class="account-content">
      <p>{{ t('communication.communicationFunction.accountContent') }}</p>
      <ul>
        <li v-for="item in serviceAccounts" :key="item.id" @click="manPointCall(item)">
          <span class="type-name">{{ item.typeName }}</span>
          <span class="account">{{ item.account }}</span>
        </li>
      </ul>
    </div>
  </TdClickOutSide>
</template>

<style lang="less" scoped>
  .account-content {
    width: 200px;
    padding: 10px 14px;
    background-color: rgb(24 41 61 / 90%);
    border: 1px solid rgb(50 152 226);

    li {
      display: flex;
      padding: 5px 0;

      &:hover {
        cursor: pointer;
        background-color: var(--button-color-normal-hover);
      }

      .type-name {
        margin-right: 10px;
        font-size: 14px;
        color: #98bfec;
      }

      .account {
        font-size: 14px;
        word-break: keep-all;
      }
    }
  }
</style>
