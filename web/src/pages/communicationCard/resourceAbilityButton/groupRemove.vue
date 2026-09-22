<script setup lang="ts">
  import { closeGroupDetailsPopup } from '@/comm/group/index';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { useI18n } from '@/hooks';
  import { groupFunc } from '@/plugins/mspPlayer';
  import { useResourceStore } from '@/store';

  const props = defineProps<{
    info: any;
  }>();
  const emit = defineEmits(['callClick']);

  const { t } = useI18n();
  const resourceStore = useResourceStore();

  async function handleRemove() {
    const userChoose = await MessageBox({
      offset: ['40%', '35%'],
      text: t('resource.contact.removeNotice'),
    });
    if (userChoose) {
      const { groupId } = props.info;
      const param = {
        grpid: groupId,
      };
      const data = await groupFunc.deleteDynamicGroupPersonData(param);

      if (data.rsp === '0') {
        resourceStore.deleteDynamicGroupById(groupId);
        Message(t('common.deleteSuccessfully'));
        closeGroupDetailsPopup(groupId);
        emit('callClick');
      } else {
        Message(t('common.deleteFailed'));
      }
    }
  }
</script>

<template>
  <!-- 群组移除 -->
  <div class="group-remove">
    <TdTooltip :content="t('common.delete')">
      <TdButton
        class="p-icon"
        icon-name="remove"
        size="auto"
        type="icon"
        @click.stop="handleRemove"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .group-remove {
    display: flex;
    align-items: center;

    .p-icon {
      width: 32px;
      height: 32px;
      cursor: pointer;
      fill: var(--icon-color-normal);
    }
  }
</style>
