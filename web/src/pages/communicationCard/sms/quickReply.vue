<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  import { useI18n } from '@/hooks';

  defineProps({
    replyList: {
      required: true,
      type: Array as PropType<any[]>,
    },
  });
  const emit = defineEmits(['closeQuickReply', 'deleteReply', 'quickReply', 'addReply']);
  const { t } = useI18n();
  const customReply = ref('');
  const showAddCustomReply = ref(true);

  onMounted(() => {
    window.addEventListener('click', closeQuickReply);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('click', closeQuickReply);
  });

  function preventClick(event) {
    event.stopPropagation();
  }
  function deleteItem(reply) {
    emit('deleteReply', reply);
  }
  function clickReplyItem(reply) {
    emit('quickReply', t(reply));
  }
  function closeQuickReply() {
    emit('closeQuickReply');
  }
  function preventDrag(event) {
    event.stopPropagation();
  }
  function showAddCustomReplyBtn() {
    showAddCustomReply.value = false;
  }
  function addReply() {
    if (customReply.value) {
      emit('addReply', customReply.value);
      customReply.value = '';
    }
    showAddCustomReply.value = true;
  }
</script>

<template>
  <div class="quick-reply">
    <div v-for="reply in replyList" :key="reply" class="item-class" @click="clickReplyItem(reply)">
      <p class="item-inner">{{ t(reply) }}</p>
      <Icon class="delete-icon" name="delete" @click.stop="deleteItem(reply)" />
    </div>
    <div v-if="!showAddCustomReply" class="add-reply">
      <input
        v-model="customReply"
        autofocus
        class="input"
        :placeholder="t('communication.msgFunction.addCustomReply')"
        @click="preventClick($event)"
        @mousedown="preventDrag($event)"
      />
      <TdButton
        class="add-btn"
        :text="t('common.promptContent.determine')"
        @click.stop="addReply"
      />
    </div>
    <div v-if="showAddCustomReply" class="item-class" @click.stop="showAddCustomReplyBtn">
      <p class="item-inner">
        {{ t('communication.msgFunction.addCustomReply') }}
      </p>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .quick-reply {
    display: inline;
    width: 220px;
    height: fit-content;
    padding: 5px 0;
    background-color: var(--background-default);
    border: 1px solid #555;
    border-radius: 5px;

    .item-class {
      display: flex;
      justify-content: space-between;
      height: 30px;
      font-size: var(--font-size-small);

      &:hover {
        background: rgb(0 0 0 / 10%);

        .delete-icon {
          opacity: 1;
        }
      }

      .item-inner {
        width: 205px;
        margin-left: 15px;
        overflow: hidden;
        line-height: 30px;
        text-align: left;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .delete-icon {
        width: 12px;
        height: 12px;
        margin-top: 9px;
        margin-right: 10px;
        opacity: 0;
        fill: #7e7e7e;
      }
    }

    .add-reply {
      margin-left: 15px;

      .input {
        width: 150px;
        height: 30px;
        padding-left: 5px;
        color: black;
        border-radius: 4px;
      }

      .add-btn {
        width: 45px;
        height: 30px;
      }
    }
  }
</style>
