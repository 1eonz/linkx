<!-- CustomGroupDialog/index.vue -->
<template>
  <div class="custom-group-dialog glass-light">
    <div class="dialog-header">
      <span class="dialog-title dragger">自定义群组</span>
      <span class="dialog-close" @click="emit('cancel')">✕</span>
    </div>

    <div class="dialog-body">
      <MemberPicker class="left-panel" :userId="userId" :selectedIds="selectedIds" @toggle="toggleMember" />
      <div class="divider"></div>
      <SelectedPanel class="right-panel" :list="selectedList" @remove="removeMember" />
    </div>

    <div class="dialog-footer">
      <button class="btn btn-cancel" @click="emit('cancel')">取消</button>
      <button class="btn btn-confirm" @click="handleConfirm">
        确定(已选 {{ selectedList.length }} / 999 人)
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import MemberPicker from './MemberPicker.vue'
import SelectedPanel from './SelectedPanel.vue'
import type { Member } from './types'

defineProps<{
  userId: string
}>()

const emit = defineEmits<{
  (e: 'confirm', members: Member[]): void
  (e: 'cancel'): void
}>()

const selectedList = ref<Member[]>([])

const selectedIds = computed(() => selectedList.value.map(m => m.id))

function toggleMember(member: Member) {
  const idx = selectedList.value.findIndex(m => m.id === member.id)
  if (idx === -1) {
    selectedList.value.push(member)
  } else {
    selectedList.value.splice(idx, 1)
  }
}

function removeMember(member: Member) {
  const idx = selectedList.value.findIndex(m => m.id === member.id)
  if (idx !== -1) selectedList.value.splice(idx, 1)
}

function handleConfirm() {
  emit('confirm', [...selectedList.value])
}
</script>

<style scoped lang="less">
.custom-group-dialog {
  display: flex;
  flex-direction: column;
  width: 740px;
  height: 560px;
  border-radius: 2px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  background: var(--background-color, #fff);

  .dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    border-bottom: 1px solid var(--border-color, #f0f0f0);
    flex-shrink: 0;

    .dialog-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color, #1d2129);
    }

    .dialog-close {
      cursor: pointer;
      font-size: 16px;
      color: var(--text-color, #999);
      line-height: 1;
      padding: 2px 4px;
      border-radius: 3px;
      transition: all 0.15s;

      &:hover {
        background: var(--hover-color, #f5f5f5);
        color: var(--text-color, #333);
      }
    }
  }

  .dialog-body {
    flex: 1;
    display: flex;
    overflow: hidden;
    padding: 16px 20px;
    gap: 0;

    .left-panel {
      width: 340px;
      flex-shrink: 0;
    }

    .divider {
      width: 1px;
      background: var(--border-color, #f0f0f0);
      margin: 0 16px;
      flex-shrink: 0;
    }

    .right-panel {
      flex: 1;
      min-width: 0;
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    padding: 12px 20px;
    border-top: 1px solid var(--border-color, #f0f0f0);
    gap: 10px;
    flex-shrink: 0;

    .btn {
      padding: 7px 24px;
      border-radius: 2px;
      font-size: 14px;
      cursor: pointer;
      border: 1px solid transparent;
      outline: none;
      transition: all 0.2s;
    }

    .btn-cancel {
      background: var(--button-text-inner, #fff);
      border-color: var(--button-border-color, #d9d9d9);
      color: var(--text-color, #555);

      &:hover {
        border-color: var(--tabs-active-color, #264ed1);
        color: var(--tabs-active-color, #264ed1);
      }
    }

    .btn-confirm {
      background: var(--button-active-color, #264ed1);
      color: #fff;
    }
  }
}
</style>
