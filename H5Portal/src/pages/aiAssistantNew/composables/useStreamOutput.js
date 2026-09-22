import { ref, unref } from 'vue'
import { useCommunicationStore } from '@/stores/communication.js'
import { newTask, getAnswer, getUserInfoByIdCard } from '@/common/api/ai.js'
import { showCustomToast } from '@/utils/toast'
import { useAiStore } from '@/stores/ai.js'
import { ConsumerQueue } from './ConsumerQueue.js'
import { sendNotification } from '@/utils/glassUtils.js';
import { marked } from "marked";
import {
  stripHtml,
  formatMsgTime
} from "@/utils/aiAssistantUtils.js"
import AiAgentChatPausePosition from './AiAgentChatPausePosition.js';
import { getGlobalsConfigByKey } from '@/common/utils/index.js'
const ERROR_MESSAGE = '业务繁忙，请稍后再试'
const ERROR_TIMEOUT = '查询超时'
const SEARCHING_SHOW = '正在查询...'
const END_TAG = '[end]'
const INTERVAL = 1000
const DISPLAY_SPEED = 50

// 智能体响应超时时间（ms）：读取全局配置 AI_AGENT_RESPONSE_TIMEOUT
// 默认 10s；-1 表示不过期一直等待；> -1 按取值（秒）作为超时时间
async function getAgentResponseTimeout() {
  const val = Number(await getGlobalsConfigByKey('AI_AGENT_RESPONSE_TIMEOUT'))
  if (Number.isNaN(val)) return 10000
  return val === -1 ? Infinity : val * 1000
}

export function useStreamOutput(messages) {
  const communicationStore = useCommunicationStore()
  const isLoading = ref(false)
  const isReceiving = ref(false)
  const currentApprovalInfo = ref(null)
  const aiStore = useAiStore()

  const streamState = ref({
    isStreaming: false,
    isPaused: false,
    serverDone: false, // 服务端是否已返回结束（[end]）
    timer: null,
    rawContent: '', // 接口返回的原始content
    displayedContent: '', // 已展示的content
    pendingContent: '', // 待展示的content
    taskId: null,
    messageIndex: null,
  })
  
  const consumerQueue = new ConsumerQueue((taskInfo) => {
    startStreamOutput(taskInfo.taskId, taskInfo.messageIndex)
  })

  function resetStreamState() {
    if (streamState.value.timer) {
      clearInterval(streamState.value.timer)
    }
    streamState.value.isStreaming = false
    streamState.value.isPaused = false
    streamState.value.serverDone = false
    streamState.value.displayedContent = ''
    streamState.value.timer = null
    streamState.value.rawContent = ''
    streamState.value.pendingContent = ''
    // 重置 taskId 为 null，让旧循环检测到 taskId 变化而退出
    streamState.value.taskId = null
    streamState.value.messageIndex = null
    isReceiving.value = false
    aiStore.clearStreamingTaskId()
  }

  function updateMessageContent() {
    const messageIndex = streamState.value.messageIndex
    const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
    const shouldUpdate = targetIndex >= 0 &&
      (messages.value[targetIndex].isStreaming ||
        streamState.value.pendingContent !== '' ||
        streamState.value.timer)

    if (shouldUpdate) {
      messages.value[targetIndex].responseContent = streamState.value.displayedContent
      if (streamState.value.pendingContent !== '' || streamState.value.timer) {
        messages.value[targetIndex].isStreaming = true
      }
    }
  }

  function startDisplayTimer() {
    if (streamState.value.timer) {
      clearInterval(streamState.value.timer)
    }
    // 与 web 端一致：启动时先同步显示 20 字符，让 <attempt_completion> 等标签
    // 快速跨过未闭合的半截状态，避免 marked 把 "<" 转义成可见文本
    const step = (maxChars = 1) => {
      if (streamState.value.isPaused) return
      if (!streamState.value.pendingContent) return
      const chunk = streamState.value.pendingContent.slice(0, maxChars)
      streamState.value.displayedContent += chunk
      streamState.value.pendingContent = streamState.value.pendingContent.slice(chunk.length)
      updateMessageContent()
    }
    step(20)

    streamState.value.timer = setInterval(() => {
      if (streamState.value.isPaused) return

      if (streamState.value.pendingContent) {
        step(1)
      } else {
        if (streamState.value.timer) {
          clearInterval(streamState.value.timer)
        }
        streamState.value.timer = null
        streamState.value.isPaused = false
        AiAgentChatPausePosition.clearPosition(streamState.value.taskId)

        // 当 pendingContent 显示完成：
        // - 如果服务端已经结束（serverDone），这里才真正把会话标记为完成
        // - 如果服务端未结束，说明只是短暂停顿，继续轮询获取
        if (streamState.value.serverDone) {
          streamState.value.isStreaming = false
          streamState.value.pendingContent = ''
          streamState.value.taskId = null
          isReceiving.value = false
          aiStore.clearStreamingTaskId()

          const messageIndex = streamState.value.messageIndex
          const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
          if (targetIndex >= 0) {
            messages.value[targetIndex].isStreaming = false
          }
          consumerQueue.onTaskComplete()
        } else if (streamState.value.taskId) {
          // 服务端还没结束，继续轮询
          isReceiving.value = true
          continueStreamOutput(streamState.value.taskId, streamState.value.messageIndex)
        }
      }
    }, DISPLAY_SPEED)
  }

  function handleError(val) {
    const messageIndex = streamState.value.messageIndex
    const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
    resetStreamState()
    if (targetIndex >= 0) {
      messages.value[targetIndex].responseContent = val
      messages.value[targetIndex].isStreaming = false
    }
    console.log('[AI智能体响应数据]通知：上app通知栏，消息内容为：', val);
    sendNotification({ text: val });
    consumerQueue.onTaskComplete()
  }

  async function startStreamOutput(taskId, messageIndex, isHistory = false) {
    const currentLoopTaskId = taskId
    let lastResponseTime = Date.now()
    const MAX_TIMEOUT = await getAgentResponseTimeout()

    // 如果是历史恢复，先尝试从前端存储获取暂停位置
    let frontendPauseData = null
    if (isHistory) {
      frontendPauseData = AiAgentChatPausePosition.getPosition(taskId)
      console.log('startStreamOutput: 从前端存储获取的暂停状态', frontendPauseData)
    }

    streamState.value.taskId = taskId
    streamState.value.messageIndex = messageIndex
    streamState.value.isStreaming = true
    streamState.value.isPaused = false
    streamState.value.serverDone = false
    streamState.value.displayedContent = ''
    streamState.value.rawContent = ''
    streamState.value.pendingContent = ''
    aiStore.setStreamingTaskId(taskId)

    if (!isHistory) {
      const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
      if (targetIndex >= 0) {
        messages.value[targetIndex].responseContent = SEARCHING_SHOW
        messages.value[targetIndex].isStreaming = true
      }
    } else {
      // 检查当前消息是否有前端存储的暂停位置和完整内容
      const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
      const msg = messages.value[targetIndex]
      
      if (frontendPauseData && frontendPauseData.replyPosition !== undefined && msg) {
        const fullContent = msg.fullResponseContent || msg.responseContent || ''
        if (fullContent) {
          const displayedContent = fullContent.substring(0, frontendPauseData.replyPosition)
          const pendingContent = fullContent.substring(frontendPauseData.replyPosition)
          
          streamState.value.displayedContent = displayedContent
          streamState.value.rawContent = fullContent
          streamState.value.pendingContent = pendingContent
          
          // 更新消息的响应内容为已显示部分
          if (targetIndex >= 0) {
            messages.value[targetIndex].responseContent = displayedContent
            messages.value[targetIndex].isStreaming = true
          }
          
          // 启动显示定时器显示待显示内容
          if (pendingContent.length > 0 && !streamState.value.timer) {
            startDisplayTimer()
          }
          
          // 清除前端存储的暂停状态
          AiAgentChatPausePosition.clearPosition(taskId)
          frontendPauseData = null
        }
      }
    }

    isReceiving.value = true

    while (true) {
      try {
        if (streamState.value.taskId !== currentLoopTaskId) {
          break
        }
        if (streamState.value.isPaused) {
          await new Promise((resolve) => setTimeout(resolve, INTERVAL))
          continue
        }

        const res = await getAnswer({ id: unref(taskId) })
        if (streamState.value.taskId !== currentLoopTaskId) {
          continue
        }

        // detail 接口返回智能体名称、提问时间，写入对应消息用于 UI 展示
        if (res && typeof res === 'object') {
          const targetIdx = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
          const targetMsg = targetIdx >= 0 ? messages.value[targetIdx] : null
          if (targetMsg) {
            if (res.agentName) targetMsg.agentName = res.agentName
            if (res.queryTime) targetMsg.queryTime = formatMsgTime(res.queryTime)
            if (res.answerTime) targetMsg.answerTime = formatMsgTime(res.answerTime)
            if (res.userName) targetMsg.userName = res.userName
          }
        }

        if (!res || res.reply === undefined || res.reply === null || res.reply === '') {
          if (Date.now() - lastResponseTime > MAX_TIMEOUT) {
            handleError(ERROR_TIMEOUT)
            return
          }
          continue
        }
        lastResponseTime = Date.now()

        // 处理对象类型的响应（答案在 res.reply 字段）
        let newContent = res
        if (typeof res === 'object' && res.reply !== undefined) {
          newContent = res.reply
        }
    
        // 检查 ERROR（字符串或对象的 reply 字段）
        // 以 [ERROR] 结尾（去掉 [end] 后）都停止轮询；纯 [ERROR] 显示友好提示，其他原样展示
        if (typeof newContent === 'string' && newContent.includes('[ERROR]')) {
          const cleaned = newContent.replace(END_TAG, '').trim()
          if (cleaned.endsWith('[ERROR]')) {
            isReceiving.value = false
            if (cleaned === '[ERROR]') {
              handleError('业务繁忙，请稍后再试')
            } else {
              // xxxx [ERROR]：展示原始内容，不视为错误
              const targetIndex = streamState.value.messageIndex !== null && streamState.value.messageIndex !== undefined ? streamState.value.messageIndex : messages.value.length - 1
              if (targetIndex >= 0) {
                messages.value[targetIndex].responseContent = cleaned
                messages.value[targetIndex].isStreaming = false
              }
              streamState.value.isStreaming = false
              streamState.value.taskId = null
              aiStore.clearStreamingTaskId()
              AiAgentChatPausePosition.clearPosition(currentLoopTaskId)
            }
            consumerQueue.onTaskComplete()
            return
          }
        }

        let isEnd = false
        let replyPosition = 0
        let replyPaused = false

        if (typeof res === 'object') {
          // 对象类型响应
          if (res.reply !== undefined) {
            newContent = res.reply
          }
          if (res.replyPosition !== undefined) {
            replyPosition = res.replyPosition
          }
          if (res.replyPaused !== undefined) {
            replyPaused = res.replyPaused
          }
          
          // 如果是历史恢复且有前端存储的暂停位置，优先使用前端存储的位置
          // 但只有在没有通过消息本身设置的情况下才处理
          if (isHistory && frontendPauseData && frontendPauseData.replyPosition !== undefined && streamState.value.displayedContent === '') {
            const fullContent = res.reply
            const displayedContent = fullContent.substring(0, frontendPauseData.replyPosition)
            const pendingContent = fullContent.substring(frontendPauseData.replyPosition)
            
            streamState.value.displayedContent = displayedContent
            streamState.value.rawContent = fullContent
            streamState.value.pendingContent = pendingContent
            updateMessageContent()
            
            // 清除前端存储的暂停状态
            AiAgentChatPausePosition.clearPosition(taskId)
            frontendPauseData = null
          }
        }
      
        if (typeof newContent === 'string' && newContent.includes(END_TAG)) {
          isEnd = true
          newContent = newContent.replace(END_TAG, '')
          //将请求后的消息推送到眼镜 先转换为 HTML，再去除 HTML 标签
           const htmlContent = marked.parse(newContent);
           const plainText = stripHtml(htmlContent);
           console.log('[AI智能体响应数据]通知：上app通知栏，消息内容为：', plainText);
           sendNotification({ text: plainText });
        }

        const oldContent = streamState.value.rawContent

        // 如果是历史恢复且已经通过前端存储设置了 rawContent，跳过处理
        if (isHistory && oldContent === res.reply) {
          // 已经通过前端存储设置了内容，跳过自动处理
        } 
        // 对象类型响应，更新 rawContent
        else if (typeof newContent === 'string') {
          if (newContent.startsWith(oldContent)) {
            const increment = newContent.substring(oldContent.length)
            if (increment) {
              streamState.value.rawContent = newContent
              streamState.value.pendingContent += increment
              if (!streamState.value.timer) {
                startDisplayTimer()
              }
            }
          } else if (!oldContent.startsWith(newContent)) {
            streamState.value.rawContent = streamState.value.displayedContent + newContent
            streamState.value.pendingContent += newContent.substring(streamState.value.displayedContent.length)
            if (!streamState.value.timer) {
              startDisplayTimer()
            }
          }
        }

        if (isEnd) {
          isReceiving.value = false
          streamState.value.serverDone = true // 标记服务端已完成
          // 不要立即把 isStreaming 设为 false，等本地 pendingContent 显示完成后再设

          if (!streamState.value.timer) {
            if (streamState.value.pendingContent) {
              streamState.value.displayedContent += streamState.value.pendingContent
              streamState.value.pendingContent = ''
              updateMessageContent()
            }
            // 后端返回纯 [ERROR] 时，替换为友好提示（包含其他内容则原样展示）
            if (streamState.value.rawContent.trim() === '[ERROR]') {
              streamState.value.displayedContent = '业务繁忙，请稍后再试'
              streamState.value.rawContent = '业务繁忙，请稍后再试'
              updateMessageContent()
            }
            AiAgentChatPausePosition.clearPosition(taskId)
            aiStore.clearStreamingTaskId()
            streamState.value.isStreaming = false
            streamState.value.taskId = null
            const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
            if (targetIndex >= 0) {
              messages.value[targetIndex].isStreaming = false
            }
            consumerQueue.onTaskComplete()
          }
          break
        }
      } catch (err) {
        console.error('轮询错误:', err)
        isReceiving.value = false
        handleError(ERROR_MESSAGE)
        return
      }

      await new Promise((resolve) => setTimeout(resolve, INTERVAL))
    }
  }

  async function sendQuestion(params) {
    try {
      // 如果有正在进行的任务，先保存其状态并结束它
      const prevTaskId = streamState.value.taskId
      const prevMessageIndex = streamState.value.messageIndex

      // 如果有正在进行的任务，保存其暂停状态到后端
      if (prevTaskId && (streamState.value.isStreaming || streamState.value.isPaused || streamState.value.pendingContent)) {
        const replyPosition = streamState.value.displayedContent.length
        const replyPaused = streamState.value.isPaused || streamState.value.pendingContent.length > 0
        const prevAgentId = messages.value[prevMessageIndex]?.agentId
        await savePauseState(prevTaskId, replyPaused, replyPosition, prevAgentId)
      }

      // 重置所有消息的暂停和流式状态，确保只有最新的一条能有继续生成按钮
      messages.value.forEach((msg, index) => {
        msg.isStreaming = false
        msg.isPaused = false
      })

      // 重置流式状态，这会让旧的 while 循环检测到 taskId 变化而退出
      resetStreamState()

      // 清空待处理队列（审批通过但还没消费的任务）
      consumerQueue.clear()

      isLoading.value = true
      isReceiving.value = false

      const messageIndex = messages.value.length

      // 默认只允许上传一个文件 取第一个文件的attachement_path
      const attachement_path = params?.files?.[0]?.attachement_path

      console.log('sendQuestion: attachement_path', params)

      const userMessage = {
        queryContent: params.content,
        responseContent: '',
        timestamp: Date.now(),
        isStreaming: false,
        isPaused: false,
        approvalStatus: '',
        approveResult: 0,
        approveNo: '',
        approveUrl: '',
        approveDetailUrl: '',
        approvalRequired: false,
        approvalSubMode: null,
        taskId: null,
        agentId: params.agent,
        agentConfigId: params.agent,
        sessionId: params.sessionId,
        attachement_path,
        // 提问人名称（默认使用入参 userName，接口返回后覆盖）
        userName: params.userName || '',
        // 提问时间（接口返回后覆盖，格式 yyyy-MM-dd HH:mm:ss）
        queryTime: '',
      }
      messages.value.push(userMessage)
      const taskRes = await newTask({
        content: params.content,
        agent: params.agent,
        userName: params.userName,
        userID: params.userID,
        departmentCode: params.departmentCode || '',
        departmentId: params.departmentId || '',
        departmentName: params.departmentName || '',
        approver: params.approver || '',
        wsSessionId: params.wsSessionId || '',
        sessionId: params.sessionId || '',
        attachement_path,
      })
      if (taskRes) {
        const taskData = taskRes
        const taskId = typeof taskData === 'string' ? taskData : taskData.id

        isLoading.value = false

        console.log('[StreamOutput] ===== newTask 响应 =====')
        console.log('[StreamOutput] taskId:', taskId)
        console.log('[StreamOutput] approvalRequired:', taskData.approvalRequired)
        console.log('[StreamOutput] approvalStatus:', taskData.approvalStatus, '(0-无需审批 1-待审批 2-审批完成 3-待建单)')
        console.log('[StreamOutput] approveResult:', taskData.approveResult, '(0-审批成功 1-审批失败)')
        console.log('[StreamOutput] approveNo:', taskData.approveNo)
        console.log('[StreamOutput] wsSessionId:', params.wsSessionId)

        // 提问接口返回的提问人名称、提问时间，覆盖默认值
        if (messageIndex >= 0) {
          if (taskData.userName) {
            messages.value[messageIndex].userName = taskData.userName
          }
          if (taskData.queryTime) {
            messages.value[messageIndex].queryTime = formatMsgTime(taskData.queryTime)
          }
        }

        // 需要审批时
        if (taskData.approvalRequired || taskData.approveNo) {
          currentApprovalInfo.value = taskData
          if (messageIndex >= 0) {
            messages.value[messageIndex].approvalStatus = taskData.approvalStatus
            messages.value[messageIndex].approveResult = 0
            messages.value[messageIndex].approveNo = taskData.approveNo
            messages.value[messageIndex].approveUrl = taskData.approveUrl
            messages.value[messageIndex].approveDetailUrl = taskData.approveDetailUrl
            messages.value[messageIndex].approvalRequired = true
            messages.value[messageIndex].approvalSubMode = taskData.approvalSubMode
            messages.value[messageIndex].taskId = taskId
          }

          if (taskData.approvalSubMode == 0){
            // 审批模式为先问后审 当前需要实现 单据状态为待建单 但是不用等待 直接消费
            consumerQueue.enqueue({ taskId, messageIndex }, true)
          } else {
            // 审批模式为先审后答

            // approvalRequired: true → 需要审批,false 无需审批
            // approvalStatus: 0-无需审批, 1-待审批, 2-审批完成, 3-待建单
            // approveResult:  0-审批成功, 1-审批失败
            // 需要审批且审批未完成（状态1待审批、状态3待建单）→ 等待审批，打开审批页面
            if (taskData.approvalRequired 
              && taskData.approvalStatus == 0
              || taskData.approvalStatus == 1
              || taskData.approvalStatus == 3
            ) {
              console.log('[StreamOutput] 需要审批且审批未完成，入队等待（immediate=false）')
              consumerQueue.enqueue({ taskId, messageIndex }, false)
            } else if (taskData.approvalStatus == 2 && taskData.approveResult == 0) {
              // 审批已完成且审批成功 → 直接消费
              console.log('[StreamOutput] 审批已完成且审批成功，直接消费')
              if (messageIndex >= 0) {
                messages.value[messageIndex].responseContent = SEARCHING_SHOW
                messages.value[messageIndex].isStreaming = true
              }
              consumerQueue.enqueue({ taskId, messageIndex }, true)
            } else {
              console.log('[StreamOutput] 无需审批，直接消费')
              if (messageIndex >= 0) {
                messages.value[messageIndex].responseContent = SEARCHING_SHOW
                messages.value[messageIndex].isStreaming = true
              }
              consumerQueue.enqueue({ taskId, messageIndex }, true)
            }
          }

        } else {
          // 不需要审批：立即消费
          console.log('[StreamOutput] 不需要审批，立即消费')
          consumerQueue.enqueue({ taskId, messageIndex }, true)
        }
      } else if (taskRes.code == 6001) {
        handleError('当前智能体已删除，请重新选择')
        isLoading.value = false
        consumerQueue.currentTaskIdRef.value = null
      } else {
        handleError(taskRes.msg || ERROR_MESSAGE)
        isLoading.value = false
      }
    } catch (error) {
      console.error('调用 AI API 失败:', error)
      handleError(ERROR_MESSAGE)
      isLoading.value = false
    }
  }

  // 记录暂停状态到前端存储
  function savePauseState(taskId, replyPaused, replyPosition, agentId) {
    AiAgentChatPausePosition.savePosition(taskId, { replyPaused, replyPosition, agentId });
  }

  function pauseStream() {
    if ((streamState.value.isStreaming || streamState.value.pendingContent || streamState.value.timer) && !streamState.value.isPaused) {
      streamState.value.isPaused = true

      // 同步更新 messages 中对应消息的 isPaused 状态
      const messageIndex = streamState.value.messageIndex
      const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
      if (targetIndex >= 0) {
        messages.value[targetIndex].isPaused = true
      }

      // 记录暂停状态到后端
      const replyPosition = streamState.value.displayedContent.length
      const taskId = streamState.value.taskId
      if (taskId) {
        savePauseState(taskId, true, replyPosition, messages.value[targetIndex]?.agentId)
      }
    }
  }

  function resumeStream(messageIndex = null) {
    console.log('resumeStream11111 messageIndex', messageIndex)
    // 确定目标消息索引
    const targetIndex = messageIndex !== null && messageIndex !== undefined
      ? messageIndex
      : (streamState.value.messageIndex !== null && streamState.value.messageIndex !== undefined
        ? streamState.value.messageIndex
        : messages.value.length - 1)

    // 获取消息对象，检查是否有历史恢复的暂停状态
    const msg = messages.value[targetIndex]

    console.log('resumeStream22222 msg', messages.value, msg)

    if (msg?.isPaused) {
      // 标记为非暂停状态
      msg.isPaused = false
      streamState.value.isPaused = false

      // 设置 streamState
      streamState.value.messageIndex = targetIndex
      // 关键：恢复 taskId，保证 displayTimer 打完 pending 后能继续向后端轮询
      if (msg.taskId) {
        streamState.value.taskId = msg.taskId
      }

      // 先尝试从前端存储获取暂停位置
      let useFrontendPause = false
      if (msg.taskId) {
        const frontendPause = AiAgentChatPausePosition.getPosition(msg.taskId)
        console.log('resumeStream: 从前端存储获取的暂停状态', frontendPause)
        if (frontendPause && frontendPause.replyPosition !== undefined && (msg.responseContent || msg.fullResponseContent)) {
          // 使用前端存储的暂停位置
          useFrontendPause = true
          const fullContent = msg.fullResponseContent || msg.responseContent
          const displayedContent = fullContent.substring(0, frontendPause.replyPosition)
          const pendingContent = fullContent.substring(frontendPause.replyPosition)

          streamState.value.displayedContent = displayedContent
          streamState.value.rawContent = fullContent
          streamState.value.pendingContent = pendingContent
          streamState.value.isStreaming = true

          // 更新消息的响应内容为已显示部分
          messages.value[targetIndex].responseContent = displayedContent

          // 启动显示定时器显示待显示内容
          if (pendingContent.length > 0 && !streamState.value.timer) {
            startDisplayTimer()
          } else if (msg.taskId) {
            // 没有 pending 但仍未结束 → 直接向后端续流
            isReceiving.value = true
            continueStreamOutput(msg.taskId, targetIndex)
          }
          // 清除前端存储的暂停状态
          AiAgentChatPausePosition.clearPosition(msg.taskId)
        }
      }

      // 如果没有使用前端存储的暂停位置，再尝试使用消息中的 replyPosition
      if (!useFrontendPause) {
        if (msg.replyPosition > 0 && (msg.responseContent || msg.fullResponseContent)) {
          console.log('resumeStream: 使用消息中的 replyPosition', msg)
          const fullContent = msg.fullResponseContent || msg.responseContent
          const displayedContent = fullContent.substring(0, msg.replyPosition)
          const pendingContent = fullContent.substring(msg.replyPosition)

          streamState.value.displayedContent = displayedContent
          streamState.value.rawContent = fullContent
          streamState.value.pendingContent = pendingContent
          streamState.value.isStreaming = true

          // 更新消息的响应内容为已显示部分
          messages.value[targetIndex].responseContent = displayedContent

          // 启动显示定时器显示待显示内容
          if (pendingContent.length > 0 && !streamState.value.timer) {
            startDisplayTimer()
          } else if (msg.taskId) {
            // 没有 pending 但仍未结束 → 直接向后端续流
            isReceiving.value = true
            continueStreamOutput(msg.taskId, targetIndex)
          }
        } else if (streamState.value.pendingContent && !streamState.value.timer) {
          // 普通暂停恢复：继续显示待显示内容
          startDisplayTimer()
        } else if (streamState.value.isStreaming && streamState.value.taskId) {
          // 继续流式输出
          isReceiving.value = true
          continueStreamOutput()
        }
      }
    }
  }

  /**
   * 页面加载时对"未暂停但未完成"的历史消息自动续流。
   * 与 resumeStream 的区别：不经过 msg.isPaused 校验、不触发 savePauseState。
   *
   * @param {number} messageIndex - 目标消息在 messages.value 中的索引
   * @param {string} taskId - 后端任务 id
   */
  function autoContinue(messageIndex, taskId) {
    if (!taskId || messageIndex == null || !messages.value[messageIndex]) return
    // 如果正在流式输出，不执行自动续流
    if (streamState.value.isStreaming || isReceiving.value) return
    
    const msg = messages.value[messageIndex]

    streamState.value.taskId = taskId
    streamState.value.messageIndex = messageIndex
    streamState.value.isStreaming = true
    streamState.value.isPaused = false
    streamState.value.serverDone = false

    msg.isStreaming = true
    msg.isPaused = false

    // 先尝试从前端存储获取暂停位置
    const frontendPause = AiAgentChatPausePosition.getPosition(taskId)
    console.log('autoContinue: 从前端存储获取的暂停状态', frontendPause)
    if (frontendPause && frontendPause.replyPosition !== undefined && (msg.responseContent || msg.fullResponseContent)) {
      // 使用前端存储的暂停位置
      const fullContent = msg.fullResponseContent || msg.responseContent
      const displayedContent = fullContent.substring(0, frontendPause.replyPosition)
      const pendingContent = fullContent.substring(frontendPause.replyPosition)

      streamState.value.displayedContent = displayedContent
      streamState.value.rawContent = fullContent
      streamState.value.pendingContent = pendingContent

      // 更新消息的响应内容为已显示部分
      msg.responseContent = displayedContent

      // 启动显示定时器显示待显示内容
      if (pendingContent.length > 0 && !streamState.value.timer) {
        startDisplayTimer()
      } else {
        // 没有 pending 但仍未结束 → 直接向后端续流
        isReceiving.value = true
        continueStreamOutput(taskId, messageIndex)
      }
    } else {
      // 没有前端存储的暂停位置，直接向后端续流
      const displayed = msg.responseContent || ''
      streamState.value.rawContent = displayed
      streamState.value.displayedContent = displayed
      streamState.value.pendingContent = ''
      isReceiving.value = true
      continueStreamOutput(taskId, messageIndex)
    }
  }
  // 催办审批 
  async function handleUrgeApproval(approveNo) {
    if (!approveNo) return Promise.reject('approveNo 值为空');
    const idx = messages.value.findLastIndex(m => m.approveNo === approveNo)
    if (idx === -1) return Promise.reject('approveNo 不存在会话列表中');
    const msg = messages.value[idx]

    if (!msg.approveUser || !msg.toLeaderUrl) {
      return Promise.reject('催办缺少必要参数: approveUser或toLeaderUrl为空');
    }

    try {
      const res = await getUserInfoByIdCard(msg.approveUser)
      const assignUserId = res?.id || ''
      if (!assignUserId) {
        return Promise.reject('未获取到审批人信息，无法发送催办');
      }
      const url = msg.toLeaderUrl ? (msg.toLeaderUrl + (msg.toLeaderUrl.includes('?') ? '&' : '?') + 'needHiddenBack=true') : ''
      const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
      const cardParams = {
        level: 'blue',
        title: 'AI助手使用申请',
        describe: '您有一个问答审批待处理，请尽快处理',
        jumpType: '4',
        url: url,
        appUrl: url,
        thumb: '',
        type: '0',
        isAssignMembers: '1',
        assignMembers: `${assignUserId}`,
        id: appId,
      }
      const WeSpaceSDK = window.WeSpaceSDK
      if (!WeSpaceSDK) {
        showCustomToast('催办失败，SDK不可用')
        return Promise.reject('SDK不可用')
      }
      const appResult = await WeSpaceSDK.sendCustomCard(cardParams)
      if (appResult && (appResult.code === 0)) {
        showCustomToast('催办成功')
      } else {
        showCustomToast('催办失败')
      }
      return appResult
    } catch (e) {
      showCustomToast('催办失败')
      return Promise.reject(e)
    }
  }

  async function continueStreamOutput(taskId = null, messageIndex = null) {
    const targetTaskId = taskId || streamState.value.taskId
    const targetMessageIndex = messageIndex !== null && messageIndex !== undefined
      ? messageIndex
      : streamState.value.messageIndex

    if (!targetTaskId) return
    
    if (!streamState.value.isStreaming) {
      isReceiving.value = false
      return
    }

    const currentLoopTaskId = targetTaskId
    let lastResponseTime = Date.now()
    const MAX_TIMEOUT = await getAgentResponseTimeout()
    console.log(`[startStreamOutput] 智能体响应超时时间: ${MAX_TIMEOUT}ms`)

    isReceiving.value = true

    while (streamState.value.isStreaming) {
      try {
        if (streamState.value.taskId !== currentLoopTaskId) break
        if (streamState.value.isPaused) {
          await new Promise((resolve) => setTimeout(resolve, INTERVAL))
          continue
        }

        const params = { id: unref(targetTaskId) }
        const res = await getAnswer(params)
        if (!res) {
          if (Date.now() - lastResponseTime > MAX_TIMEOUT) {
            isReceiving.value = false
            handleError(ERROR_TIMEOUT)
            return
          }
          continue
        }
        lastResponseTime = Date.now()

        // 处理对象类型的响应（答案在 res.reply 字段）
        let newContent = res
        let isEnd = false

        if (typeof res === 'object' && res.reply !== undefined) {
          newContent = res.reply
        }

        if (typeof newContent === 'string' && newContent.includes('[ERROR]')) {
          // 以 [ERROR] 结尾（去掉 [end] 后）都停止轮询；纯 [ERROR] 显示友好提示，其他原样展示
          const cleaned = newContent.replace(END_TAG, '').trim()
          if (cleaned.endsWith('[ERROR]')) {
            isReceiving.value = false
            if (cleaned === '[ERROR]') {
              handleError('业务繁忙，请稍后再试')
            } else {
              // xxxx [ERROR]：展示原始内容，不视为错误
              const targetIndex = messageIndex !== null && messageIndex !== undefined ? messageIndex : messages.value.length - 1
              if (targetIndex >= 0) {
                messages.value[targetIndex].responseContent = cleaned
                messages.value[targetIndex].isStreaming = false
              }
              streamState.value.isStreaming = false
              streamState.value.taskId = null
              aiStore.clearStreamingTaskId()
              AiAgentChatPausePosition.clearPosition(currentLoopTaskId)
            }
            consumerQueue.onTaskComplete()
            return
          }
        }
        if (typeof newContent === 'string' && newContent.includes(END_TAG)) {
          isEnd = true
          newContent = newContent.replace(END_TAG, '')
          //将请求后的消息推送到眼镜 先转换为 HTML，再去除 HTML 标签
           const htmlContent = marked.parse(newContent);
           const plainText = stripHtml(htmlContent);
           console.log('[AI智能体响应数据]通知：上app通知栏，消息内容为：', plainText);
           sendNotification({ text: plainText });
        }

        const oldContent = streamState.value.rawContent

        if (typeof newContent === 'string') {
          if (newContent.startsWith(oldContent)) {
            const increment = newContent.substring(oldContent.length)
            if (increment) {
              streamState.value.rawContent = newContent
              streamState.value.pendingContent += increment
              if (!streamState.value.timer) {
                startDisplayTimer()
              }
            }
          } else if (!oldContent.startsWith(newContent)) {
            streamState.value.rawContent = streamState.value.displayedContent + newContent
            streamState.value.pendingContent += newContent.substring(streamState.value.displayedContent.length)
            if (!streamState.value.timer) {
              startDisplayTimer()
            }
          }
        }

        if (isEnd) {
          isReceiving.value = false
          streamState.value.serverDone = true // 标记服务端已完成
          // 不要立即把 isStreaming 设为 false，等本地 pendingContent 显示完成后再设
          // 推送ai消息到眼镜
          if (newContent) {
                      // 先转换为 HTML，再去除 HTML 标签
                      const htmlContent = marked.parse(newContent);
                      const plainText = stripHtml(htmlContent);
                      sendNotification({ text: plainText });
                  }
          if (!streamState.value.timer) {
            if (streamState.value.pendingContent) {
              streamState.value.displayedContent += streamState.value.pendingContent
              streamState.value.pendingContent = ''
              updateMessageContent()
            }
            savePauseState(taskId, false, 0, messages.value[messageIndex]?.agentId)
            streamState.value.isStreaming = false
          }
          break
        }
      } catch (err) {
        console.error('轮询错误:', err)
        isReceiving.value = false
        handleError(ERROR_MESSAGE)
        return
      }

      await new Promise((resolve) => setTimeout(resolve, INTERVAL))
    }
  }

  async function cleanup() {
    // 页面卸载时保存暂停状态
    if (streamState.value.taskId && (streamState.value.isStreaming || streamState.value.isPaused || streamState.value.pendingContent)) {
      const replyPosition = streamState.value.displayedContent.length
      const replyPaused = streamState.value.isPaused || streamState.value.pendingContent.length > 0
      const agentId = messages.value[streamState.value.messageIndex]?.agentId
      await savePauseState(streamState.value.taskId, replyPaused, replyPosition, agentId)
    }
    
    resetStreamState()
    consumerQueue.clear()
  }

  return {
    messages,
    currentTaskId: consumerQueue.currentTaskId,
    isLoading,
    isReceiving,
    streamState,
    currentApprovalInfo,
    sendQuestion,
    pauseStream,
    resumeStream,
    autoContinue,
    handleUrgeApproval,
    resetStreamState,
    cleanup,
    startApprovalStream: (taskId, messageIndex) => {
      console.log('[StreamOutput] startApprovalStream: 审批通过，开始消费任务', { taskId, messageIndex })
      consumerQueue.consumeTask(taskId, messageIndex)
    },
  }
}