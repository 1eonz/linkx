import { ref } from 'vue'

/**
 * 消费待回答的任务队列
 * 用于管理审批通过待消费的任务（待消费队列）
 * 当前正在进行流式输出（消费者进行消费）
*/
export class ConsumerQueue {
  constructor(onConsume) {
    this._pendingQueue = ref([])
    this._currentTaskId = ref(null)
    this._onConsume = onConsume
  }

  get pendingQueue() {
    return this._pendingQueue.value
  }

  get currentTaskIdRef() {
    return this._currentTaskId
  }

  get currentTaskId() {
    return this._currentTaskId.value
  }

  get isIdle() {
    return this._currentTaskId.value === null && this._pendingQueue.value.length === 0
  }

  get hasPending() {
    return this._pendingQueue.value.length > 0
  }

  enqueue(taskInfo, immediate = true) {
    console.log('[ConsumerQueue] enqueue:', { taskId: taskInfo.taskId, immediate, queueLength: this._pendingQueue.value.length })
    // 标记任务是否需要等待审批
    taskInfo.waitForApproval = !immediate
    this._pendingQueue.value.push(taskInfo)
    if (immediate) {
      this.processNext(true)
    }
  }

  dequeue() {
    return this._pendingQueue.value.shift()
  }

  processNext(forceStart = false) {
    console.log('[ConsumerQueue] processNext:', { forceStart, currentTaskId: this._currentTaskId.value, queueLength: this._pendingQueue.value.length })
    if (this._currentTaskId.value && !forceStart) {
      return
    }

    // 如果 forceStart 为 true 且当前有任务在运行，重置它
    if (this._currentTaskId.value && forceStart) {
      this._currentTaskId.value = null
    }

    // 找到第一个不需要等待审批的任务
    let next = null
    let nextIndex = -1
    for (let i = 0; i < this._pendingQueue.value.length; i++) {
      if (!this._pendingQueue.value[i].waitForApproval) {
        next = this._pendingQueue.value[i]
        nextIndex = i
        break
      }
    }

    if (nextIndex !== -1) {
      // 移除找到的任务
      this._pendingQueue.value.splice(nextIndex, 1)
      this._currentTaskId.value = next.taskId
      this._onConsume(next)
    } else {
      console.log('[ConsumerQueue] processNext: 队列中没有可立即消费的任务（都在等待审批）')
    }
  }

  // 直接消费指定任务（用于审批通过后）
  consumeTask(taskId, messageIndex) {
    console.log('[ConsumerQueue] consumeTask:', { taskId, messageIndex, queueLength: this._pendingQueue.value.length, queueContent: this._pendingQueue.value.map(t => t.taskId) })
    const taskIndex = this._pendingQueue.value.findIndex(t => t.taskId === taskId)
    if (taskIndex !== -1) {
      const task = this._pendingQueue.value.splice(taskIndex, 1)[0]
      console.log('[ConsumerQueue] consumeTask: 从队列中找到任务并消费', { taskId: task.taskId })
      if (this._currentTaskId.value) {
        this._currentTaskId.value = null
      }
      this._currentTaskId.value = task.taskId
      this._onConsume(task)
    } else {
      console.log('[ConsumerQueue] consumeTask: 队列中未找到任务，直接消费', { taskId, messageIndex })
      if (this._currentTaskId.value) {
        this._currentTaskId.value = null
      }
      this._currentTaskId.value = taskId
      this._onConsume({ taskId, messageIndex })
    }
  }

  onTaskComplete() {
    this._currentTaskId.value = null
    this.processNext()
  }

  clear() {
    this._pendingQueue.value = []
    this._currentTaskId.value = null
  }
}