/**
  *  问题背景：
  *  在 WebView2 环境下，点击 el-select dropdown 选项时，
  *  mousedown 正常落在 .el-select-dropdown__item 上，
  *  但 mouseup 触发时 WebView2 已将焦点转移，
  *  导致浏览器判定 click 目标变成两者公共祖先 body，
  *  element-plus 内部的 click 监听收不到，v-model 无法更新。
  *  
  *  解决思路：
  *  在 mousedown 时记录目标 item，
  *  mouseup 后手动向该 item 补发一个 click 事件，绕过 WebView2 的问题。
  *  
  *  使用方式：
  *  import { useWebview2SelectFix } from '@/composables/useWebview2SelectFix'
  *  useWebview2SelectFix()
*/

import { onMounted, onUnmounted } from 'vue'

export function useWebview2SelectFix(): void {
  let pendingItem: HTMLElement | null = null

  const onMousedown = (e: MouseEvent): void => {
    const target = e.target as HTMLElement
    const item = target.closest('.el-select-dropdown__item') as HTMLElement | null
    if (item) pendingItem = item
  }

  const onMouseup = (): void => {
    if (pendingItem) {
      const item = pendingItem
      pendingItem = null
      // 等 WebView2 处理完自身逻辑后再补发 click
      requestAnimationFrame(() => {
        item.dispatchEvent(new MouseEvent('click', {
          bubbles: true,
          cancelable: true,
          view: window
        }))
      })
    }
  }

  onMounted(() => {
    document.addEventListener('mousedown', onMousedown, true)
    document.addEventListener('mouseup', onMouseup, true)
  })

  onUnmounted(() => {
    document.removeEventListener('mousedown', onMousedown, true)
    document.removeEventListener('mouseup', onMouseup, true)
  })
}