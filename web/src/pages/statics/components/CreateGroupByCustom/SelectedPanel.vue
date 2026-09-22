<template>
  <div class="selected-panel">
    <div class="selected-header">
      已选择：<span class="selected-count">{{ list.length }}</span> 人
    </div>

    <div class="selected-grid">
      <div v-for="member in list" :key="member.id" class="selected-item">
        <div class="selected-avatar-wrap">
          <img class="selected-avatar" :src="getAvatarUrl(member.avatar || '')" alt="" />
          <span class="remove-btn" @click.stop="emit('remove', member)">×</span>
        </div>
        <span class="selected-name" :title="member.name">{{ member.name }}</span>
      </div>

      <div v-if="!list.length" class="empty-tip">暂无选择</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { Member } from './types'
import { getIp } from '@/utils'

defineProps<{
  list: Member[]
}>()

const emit = defineEmits<{
  (e: 'remove', member: Member): void
}>()

const DEFAULT_AVATAR = 'https://cube.elemecdn.com/9/c2/f0ee8a3c7c9638a54940382568c9dpng.png'
// 获取头像 URL：全路径直接使用，相对路径拼接 getIp()
const getAvatarUrl = (avatar: string) => {
  if (!avatar) return DEFAULT_AVATAR
  // http(s):// 开头视为全路径，直接使用
  if (/^https?:\/\//i.test(avatar)) {
    return avatar
  }
  return `${getIp()}/linkx/desktop${avatar}`
}
</script>

<style scoped lang="less">
.selected-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .selected-header {
    padding: 12px 14px 8px;
    font-size: 13px;
    color: var(--tabs-color, #666);
    flex-shrink: 0;

    .selected-count {
      color: var(--tabs-active-color, #264ed1);
      font-weight: 600;
    }
  }

  .selected-grid {
    flex: 1;
    overflow-y: auto;
    padding: 10px 14px 8px;
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(52px, 1fr));
    gap: 16px 8px;
    align-content: flex-start;

    &::-webkit-scrollbar {
      width: 4px;
    }

    &::-webkit-scrollbar-thumb {
      background: var(--border-color, #ddd);
      border-radius: 4px;
    }
  }

  .selected-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    min-width: 0;

    .selected-avatar-wrap {
      position: relative;
      width: 32px;
      height: 32px;
      flex-shrink: 0;

      .selected-avatar {
        width: 32px;
        height: 32px;
        border-radius: 5px;
        object-fit: cover;
        background: var(--is-blur-bg, #e8e8e8);
        display: block;
      }

      .remove-btn {
        position: absolute;
        top: -6px;
        right: -6px;
        width: 14px;
        height: 14px;
        border-radius: 50%;
        background: var(--tabs-color, #999);
        color: #fff;
        font-size: 10px;
        line-height: 1;
        cursor: pointer;
        user-select: none;
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: background 0.2s;

        &:hover {
          background: var(--color-danger, #af3434);
        }
      }
    }

    .selected-name {
      font-size: 11px;
      color: var(--tabs-color, #666);
      width: 100%;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .empty-tip {
    grid-column: 1 / -1;
    text-align: center;
    color: var(--tabs-color, #bbb);
    font-size: 13px;
    padding: 40px 0;
  }
}
</style>
