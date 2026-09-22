import { getProcess } from '@/api/h5/collaboration'
import ElementUI from 'element-ui'
export const pageLoadingUtils = {
  shouldContinue: false,
  loadingState: false,
  loadingInstance: null,
  async openPageLoadingPolling(callback) {
    while (this.shouldContinue) {
      try {
        const result = await getProcess() // 调用接口
        if (result?.data === true) {
          if (callback) {
            setTimeout(() => {
              this.closePageLoading()
              callback()
            }, 3000)
          } else {
            this.closePageLoading()
          }
          break // 停止轮询
        } else {
          await new Promise(resolve => setTimeout(resolve, 5000)) // 等待 5 秒
        }
      } catch (error) {
        console.error(`请求 queryPageLoadingStatus 出错：`, error)
        await new Promise(resolve => setTimeout(resolve, 5000)) // 出错也等待 5 秒后重试
      }
    }
  },
  async openPageLoading(callback) {
    const result = await getProcess() // 调用接口
    if (this.loadingState || result?.data) {
      return
    }
    this.loadingState = true
    this.shouldContinue = true
    this.loadingInstance = ElementUI.Loading.service({
      lock: true, // 是否锁定屏幕（阻止用户操作）
      text: '数据同步中...', // 加载中显示的文字
      spinner: 'el-icon-loading', // 默认就是这个，可省略
      background: 'rgba(255, 255, 255, 0.6)', // 背景色与透明度，可选
    })
    this.openPageLoadingPolling(callback)
  },
  closePageLoading() {
    if (!this.loadingState) {
      return
    }
    this.loadingState = false
    this.shouldContinue = false
    this.loadingInstance.close()
  }
}
