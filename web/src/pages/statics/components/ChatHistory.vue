<script setup>
  import { ref, onMounted, nextTick, computed, watch } from 'vue';
  import { getChatHistory, getChatHistoryCount, getUserInfo, getCollocation } from '@/api/statics';
  import { usePIMStore } from '@/store';
  import { filterMessage } from '@/pages/coordination/common';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import MsgFile from '@/pages/coordination/chatPanel/msgFile.vue';
  import MsgForward from '@/pages/coordination/chatPanel/msgForward.vue';
  import MsgCard from '@/pages/coordination/chatPanel/msgCard.vue';
  import quoteMsg2 from '@/pages/coordination/chatPanel/quoteMsg2.vue';
  import GroupNotice from '@/pages/coordination/chatPanel/groupNotice.vue';
  import CustomSelect from './CustomSelect.vue';
  import msgVoiceHistory from '@/pages/notification/components/msgVoiceHistory.vue';
  import { getIp } from '@/utils';
  import { useWebView2DatePicker } from '@/composables/useWebView2DatePicker';
  import { formatDateTimeLocal } from '@/utils/dateTimeHelper';
  import defaultImg from '@/assets/images/pim/im_person.svg';
  import { debounce } from 'lodash-es';
  import { appConfig } from '@/config';

  const props = defineProps({
    groupId: {
      type: String,
      required: true,
    },
    // 历史消息中：群组通知不将姓名替换为“你”
    disableYouAlias: {
      type: Boolean,
      default: false,
    },
  });
  const showDialog = ref(false);
  const showVideoDialog = ref(false);
  const imagePreviewUrl = ref('');
  const videoPreviewUrl = ref('');

  const PIMStore = usePIMStore();
  const messagesContainer = ref(null);

  // 聊天历史数据
  const chatHistory = ref([]);
  const isLoadingMore = ref(false);
  const hasMore = ref(true);
  const currentPage = ref(1);
  const pageSize = ref(40);
  const isInitialLoad = ref(true);

  const groupMembers = ref([]);

  // 用户信息
  const userInfo = computed(() => PIMStore.user);
  const userId = computed(() => userInfo.value?.userid);
  const xietongId = computed(() => {
    const { cooperationUsers, cooperationUser } = userInfo.value;
    let cooperationUserId = '';
    if (
      appConfig.settingData?.MULTIPLE_COLLABORATION === 'true' ||
      appConfig.settingData?.MULTIPLE_COLLABORATION === true
    ) {
      cooperationUsers?.map((item) => {
        if (item.groupIds.includes(Number(props.groupId))) {
          cooperationUserId = item.userId;
        }
      });
    } else {
      cooperationUserId = cooperationUser?.userId || '';
    }
    return cooperationUserId;
  });

  const videoPlayer = ref(null);
  const isPlaying = ref(false);
  const togglePlay = () => {
    if (videoPlayer.value) {
      if (videoPlayer.value.paused) {
        videoPlayer.value.play();
        isPlaying.value = true;
      } else {
        videoPlayer.value.pause();
        isPlaying.value = false;
      }
    }
  };

  // 自定义过滤函数，排除operationType=4的已读回执
  const customFilterMessage = (message) => {
    // 先使用原有的过滤逻辑
    if (!filterMessage(message)) {
      return false;
    }

    // 额外过滤：排除operationType=4的已读回执
    if (message.msgType === 4) {
      return false;
    }

    // 排除msgType=10且operationType=4的群组事件
    // if (message.msg.includes('operationType=4')) {
    //   return false;
    // }

    return true;
  };

  // 解析群组事件消息
  const parseGroupEventMessage = (message) => {
    try {
      if (message.msgType === 10 && message.msg.includes('cmcontainer')) {
        // 处理 {cmcontainer={...}} 格式的消息
        const cmcontainerMatch = message.msg.match(/{cmcontainer=({.*})}/);
        if (cmcontainerMatch && cmcontainerMatch[1]) {
          const cmcontainerStr = cmcontainerMatch[1];

          // 转换为标准JSON格式
          let jsonStr = cmcontainerStr
            .replaceAll(/([a-zA-Z_][a-zA-Z0-9_]*)=/g, '"$1":') // 将 key= 转换为 "key":
            .replaceAll("'", '"'); // 将单引号转换为双引号

          // 处理数组中的对象
          jsonStr = jsonStr.replaceAll(/\[([^\]]+)\]/g, (match, arrayContent) => {
            if (arrayContent.includes('=')) {
              const arrayItems = arrayContent
                .split(/(?<=}),?\s*/)
                .map((item) => {
                  if (item.trim() && item.includes('=')) {
                    return `{${item.replaceAll(/([a-zA-Z_][a-zA-Z0-9_]*)=/g, '"$1":').replaceAll("'", '"')}}`;
                  }
                  return item;
                })
                .filter((item) => item.trim());
              return `[${arrayItems.join(',')}]`;
            }
            return match;
          });

          // 处理嵌套的对象（如newGroupProfile）
          // 找到所有key={...}格式的字符串并正确处理
          const nestedObjectRegex = /"([a-zA-Z_][a-zA-Z0-9_]*)":\{([^}]*)\}/g;
          jsonStr = jsonStr.replaceAll(nestedObjectRegex, (match, key, content) => {
            // 处理嵌套对象内部的key=value格式
            const processedContent = content
              .replaceAll(/([a-zA-Z_][a-zA-Z0-9_]*)=/g, '"$1":')
              .replaceAll("'", '"');
            return `"${key}":{${processedContent}}`;
          });

          try {
            const cmcontainer = JSON.parse(jsonStr);

            // 确保operationType存在
            if (cmcontainer.operationType === undefined) {
              // 尝试从字符串中提取operationType
              const opTypeMatch = cmcontainerStr.match(/operationType=(\d+)/);
              if (opTypeMatch) {
                cmcontainer.operationType = parseInt(opTypeMatch[1]);
              } else {
                cmcontainer.operationType = 0;
              }
            }

            // 确保memberList是数组
            if (cmcontainer.memberList === '[]') {
              cmcontainer.memberList = [];
            }

            return {
              ...message,
              msg: {
                cmcontainer,
              },
            };
          } catch {
            // 备用解析：直接提取关键字段
            const cmcontainer = {};
            const fields = ['operationType', 'operateId', 'groupId', 'operateRealUserId', 'tag'];

            fields.forEach((field) => {
              const regex = new RegExp(`${field}=([^,}]+)`);
              const match = cmcontainerStr.match(regex);
              if (match) {
                let value = match[1];
                // 尝试转换为数字
                if (/^\d+$/.test(value)) {
                  value = Number.parseInt(value);
                }
                cmcontainer[field] = value;
              }
            });

            // 处理memberList
            const memberListMatch = cmcontainerStr.match(/memberList=\[([^\]]*)\]/);
            if (memberListMatch && memberListMatch[1]) {
              try {
                const members = memberListMatch[1]
                  .split(/(?<=}),?\s*/)
                  .filter((m) => m.trim())
                  .map((memberStr) => {
                    const member = {};
                    const memberFields = memberStr.match(/([a-zA-Z_][a-zA-Z0-9_]*)=([^,}]+)/g);
                    if (memberFields) {
                      memberFields.forEach((field) => {
                        const [key, value] = field.split('=');
                        member[key] = /^\d+$/.test(value) ? Number.parseInt(value) : value;
                      });
                    }
                    return member;
                  });
                cmcontainer.memberList = members;
              } catch {
                cmcontainer.memberList = [];
              }
            }

            // 处理newGroupProfile
            const newGroupProfileMatch = cmcontainerStr.match(/newGroupProfile=\{([^}]+)\}/);
            if (newGroupProfileMatch && newGroupProfileMatch[1]) {
              try {
                const profileStr = newGroupProfileMatch[1];
                const profile = {};

                // 解析newGroupProfile中的字段
                const profileFields = profileStr.split(',');
                profileFields.forEach((field) => {
                  const [key, value] = field.split('=');
                  if (key && value) {
                    profile[key.trim()] = /^\d+$/.test(value) ? parseInt(value) : value.trim();
                  }
                });

                cmcontainer.newGroupProfile = profile;
              } catch {
                cmcontainer.newGroupProfile = {};
              }
            }

            // 处理coUserGroupRelations - 解析为数组格式
            const coUserGroupMatch = cmcontainerStr.match(/coUserGroupRelations=\[([^\]]*)\]/);
            if (coUserGroupMatch && coUserGroupMatch[1]) {
              try {
                const relationsStr = coUserGroupMatch[1].trim();
                // 解析数组中的对象，格式如：{groupId=xxx, operType=1, realUserId=xxx, userId=xxx}
                const relations = {};
                
                // 去掉外层的花括号
                const content = relationsStr.replaceAll(/^\{|\}$/g, '').trim();
                const fieldsArr = content.split(',');
                fieldsArr.forEach((field) => {
                  const eqIndex = field.indexOf('=');
                  if (eqIndex > -1) {
                    const key = field.substring(0, eqIndex).trim();
                    const value = field.substring(eqIndex + 1).trim();
                    if (key && value) {
                      relations[key] = /^\d+$/.test(value) ? parseInt(value) : value;
                    }
                  }
                });
                // 保持为数组格式，与原始数据结构一致
                cmcontainer.coUserGroupRelations = [relations];
              } catch {
                cmcontainer.coUserGroupRelations = [];
              }
            }

            return {
              ...message,
              msg: {
                cmcontainer,
              },
            };
          }
        }
      }
      return message;
    } catch {
      return message;
    }
  };

  // 处理后的消息列表
  const processedMessages = ref([]);
  // 用户信息缓存，避免重复请求
  const userInfoCache = ref(new Map());
  // 保存所有查到的聊天历史记录
  const allChatHistorySet = ref(new Set());

  const addGroupMembers = (realUser, realName) => {
    const exitMember = groupMembers.value.find((item) => item.id === realUser.id);
    if (exitMember) {
      exitMember.realName = realName || exitMember?.name;
    } else {
      groupMembers.value.push({ ...realUser, realName });
    }
  };
  // 异步处理消息列表
  const processMessages = async () => {
    const needFetchRealUserIds = new Set();
    const needFetchRealUsesInfosMap = new Map();
    const members = groupMembers.value;
    // 将所有消息存入allChatHistorySet,防止查询时获取不到引用消息
    chatHistory.value.forEach((item) => allChatHistorySet.value.add(item));
    // 先进行基础处理（同步部分）
    const filteredMessages = chatHistory.value
      .filter((item) => customFilterMessage(item))
      .map((item) => {
        // 处理群组事件消息
        if (item.msgType === 10) {
          const parsedMessage = parseGroupEventMessage(item);
          return parsedMessage;
        }

        // 处理用户信息 - 优先使用群成员信息
        let fromUser = {};

        // 如果群成员信息存在，优先使用群成员中的姓名和头像
        const groupMember = members.find((member) => member.id === item.from);

        if (groupMember) {
          fromUser = {
            ...fromUser,
            avatar: groupMember.tumbAvatar, // 使用 tumbAvatar 字段作为头像
            name: groupMember.name,
          };
        }

        let name = fromUser.name || `用户${item.from}`;
        // 如果没有名字，添加到set重新查询
        if (item.fromRealUserId) {
          name = '';
          needFetchRealUserIds.add(item.from);
        }
        return {
          ...item,
          avatar: fromUser.avatar,
          name,
          showTime: false,
        };
      });

    // 收集所有需要查询的 fromRealUserId
    const realUserIdsToFetch = new Set();
    filteredMessages.forEach((item) => {
      if (item.fromRealUserId) {
        const userId = item.fromRealUserId.toString();
        if (!userInfoCache.value.has(userId)) {
          realUserIdsToFetch.add(item.fromRealUserId);
        }
      }

      // 协同岗用户姓名 - coUserGroupRelations 可能是数组
      const coUserGroupRelations = item?.msg?.cmcontainer?.coUserGroupRelations;
      const coRelation = Array.isArray(coUserGroupRelations) ? coUserGroupRelations[0] : coUserGroupRelations;
      const coRealUserId = coRelation?.realUserId;
      if (coRealUserId) {
        const userId = coRealUserId.toString();
        if (!userInfoCache.value.has(userId)) {
          realUserIdsToFetch.add(userId);
        }
      }

      // 协同岗名称
      const coUserId = coRelation?.userId;
      if (coUserId) {
        const userId = coUserId.toString();
        if (!needFetchRealUserIds.has(item.from)) {
          needFetchRealUserIds.add(userId);
        }
      }

      // 被移除userId
      if (item?.msg?.cmcontainer?.operationType === 3) {
        const { memberList } = item.msg.cmcontainer;
        memberList.forEach((member) => {
          if (member.isDel) {
            const userId = member.userId.toString();
            if (!userInfoCache.value.has(userId)) {
              realUserIdsToFetch.add(userId);
            }
          }
        });
      }
    });

    // 批量获取用户信息
    if (realUserIdsToFetch.size > 0) {
      try {
        const userIdsArray = [...realUserIdsToFetch];
        const res = await getUserInfo({ userIds: userIdsArray });
        if (res.data && res.data.results && Array.isArray(res.data.results)) {
          res.data.results.forEach((user) => {
            const userId = (user.id || user.userId).toString();
            userInfoCache.value.set(userId, user);
            PIMStore.setUserInfo(user);
          });
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
      }
    }

    // 更新消息名称，添加真实用户名
    const finalMessages = filteredMessages.map((item) => {
      if (item.fromRealUserId) {
        const userId = item.fromRealUserId.toString();
        const realUser = userInfoCache.value.get(userId);
        if (realUser && realUser.name) {
          if (item.name) {
            item.name += `(${realUser.name})`;
          } else {
            item.name = realUser.name;
          }
          item.realName = realUser.name;
          addGroupMembers(realUser, item.name);
        }
      }
      // 处理协同岗关系中的 realUserId
      const coUserGroupRelations2 = item?.msg?.cmcontainer?.coUserGroupRelations;
      const coRelation2 = Array.isArray(coUserGroupRelations2) ? coUserGroupRelations2[0] : coUserGroupRelations2;
      const coUserUserId = coRelation2?.realUserId;
      if (coUserUserId) {
        const userId = coUserUserId.toString();
        const realUser = userInfoCache.value.get(userId);
        if (realUser && realUser.name) {
          if (item.name) {
            item.name += `(${realUser.name})`;
          } else {
            item.name = realUser.name;
          }
          item.realName = realUser.name;
          addGroupMembers(realUser, item.name);
        }
      }
      return item;
    });
    // 批量获取用户信息
    if (needFetchRealUserIds.size > 0) {
      try {
        const realUserIdsArray = [...needFetchRealUserIds];
        const realUserInfosRes = await getCollocation({ userIds: realUserIdsArray });
        const realUserInfos = realUserInfosRes?.data;
        if (realUserInfos && Array.isArray(realUserInfos)) {
          realUserInfos.forEach((user) => {
            needFetchRealUsesInfosMap.set(user.id, user);
            PIMStore.setUserInfo(user);
          });
        }
      } catch (error) {
        console.error('批量获取用户信息失败:', error);
      }
    }
    if (needFetchRealUsesInfosMap.size > 0) {
      filteredMessages.forEach((item) => {
        if (item.fromRealUserId && needFetchRealUserIds.has(item.from)) {
          const realUserInfo = needFetchRealUsesInfosMap.get(item.from);
          if (realUserInfo && realUserInfo.postName) {
            item.name = `${realUserInfo.postName}(${item.realName})`;
            const findItem = groupMembers.value.find((groupItem) => groupItem.id === item.from);
            if (findItem) {
              findItem.realName = `${realUserInfo.postName}(${item.realName})`;
            } else {
              groupMembers.value.push({
                ...realUserInfo,
                realName: `${realUserInfo.postName}(${item.realName})`,
              });
            }
          }
        }
        if (!item.name) {
          item.name = `用户${item.from}(${item.realName})`;
        }
      });
    } else {
      filteredMessages
        .filter((item) => !item.name)
        .forEach((item) => {
          item.name = `用户${item.from}(${item.realName})`; // 如果没查到，展示默认名称
        });
    }

    // 排序并更新
    processedMessages.value = finalMessages.sort(
      (a, b) => Number.parseInt(a.time) - Number.parseInt(b.time),
    );
  };

  // 判断是否为有效的群组事件消息
  const isValidGroupEvent = (message) => {
    if (message.msgType !== 10) return false;
    if (!message.msg || !message.msg.cmcontainer) return false;

    const cmcontainer = message.msg.cmcontainer;
    const validOperationTypes = [1, 2, 3, 4, 5, 6, 7, 11]; // 有效的操作类型

    return validOperationTypes.includes(cmcontainer.operationType);
  };

  // 判断消息是否为自己发送
  const isSelf = (message) => {
    const currentUserId = userId.value;
    const xietongUserId = xietongId.value;
    return (
      Number(message.from) === Number(currentUserId) ||
      Number(message.from) === Number(xietongUserId) ||
      Number(message.fromRealUserId) === Number(currentUserId)
    );
  };

  function parseKeyValueString(str) {
    const result = {};
    let currentKey = '';
    let currentValue = '';
    let inValue = false;
    const depth = 0; // 用于处理嵌套结构
    for (let i = 0; i < str.length; i++) {
      const char = str[i];

      if (!inValue && char === '=') {
        inValue = true;
        continue;
      }
      if (inValue) {
        // 在读取value
        currentValue += char;

        // 检查是否到达下一个键值对的开始
        if (
          char === ',' &&
          depth === 0 && // 检查下一个字符是否是单词字符（新key的开始）
          i + 1 < str.length &&
          /\w/.test(str[i + 1])
        ) {
          result[currentKey] = currentValue.slice(0, -1).trim(); // 去掉最后的逗号
          currentKey = '';
          currentValue = '';
          inValue = false;
        }
      } else {
        // 还在读取key
        if (char === ',' && depth === 0) {
          // 逗号分隔不同的键值对
          if (currentKey) {
            result[currentKey] = currentValue.trim();
            currentKey = '';
            currentValue = '';
          }
        } else {
          currentKey += char;
        }
      }
    }
    // 处理最后一个键值对
    if (currentKey) {
      result[currentKey] = currentValue.trim();
    }
    return result;
  }

  function parseMessage2(cleanMsg) {
    const msgData = {};
    let currentKey = '';
    let currentValue = '';
    let parsingValue = false;

    // 手动遍历字符串
    for (let i = 0; i < cleanMsg.length; i++) {
      const char = cleanMsg[i];

      if (parsingValue) {
        // 正在解析value阶段
        currentValue += char;
        // 检查是否到达下一个键值对的开始
        // 下一个键值对的特征：逗号后面跟着字母数字（新key的开始）
        if (char === ',' && i + 1 < cleanMsg.length) {
          const nextChar = cleanMsg[i + 1];
          const nextNextChar = cleanMsg[i + 2];

          // 如果逗号后面是空格+字母数字的组合，说明是新键值对开始了
          if (nextChar === ' ' && /\w/.test(nextNextChar)) {
            // 结束当前value的解析
            finishCurrentPair();
            parsingValue = false;
            currentKey = '';
            currentValue = '';
            i++; // 跳过空格
            continue;
          }

          // 或者直接检查是否遇到了已知的其他key
          const remaining = cleanMsg.substring(i + 1);
          const knownKeys = ['endFlag=', 'msgSeq=', 'srcMsgId=', 'stopFlag=', 'text='];
          const foundKnownKey = knownKeys.some((key) => remaining.startsWith(key));

          if (foundKnownKey) {
            finishCurrentPair();
            parsingValue = false;
            currentKey = '';
            currentValue = '';
            continue;
          }
        }

        // 检查字符串结束
        if (i === cleanMsg.length - 1) {
          finishCurrentPair();
        }
      } else {
        // 还在解析key阶段
        if (char === '=') {
          parsingValue = true;
        } else if (char === ',') {
          // 如果遇到逗号且还没遇到等号，说明是key的一部分（比如key包含空格）
          if (currentKey) {
            currentKey += char;
          }
        } else {
          currentKey += char;
        }
      }
    }

    function finishCurrentPair() {
      const key = currentKey.trim();
      let value = currentValue.trim();
      // 移除value末尾可能的逗号（如果有）
      if (value.endsWith(',')) {
        value = value.slice(0, -1);
      }
      // 处理值的边界情况
      msgData[key] = value === 'null' ? null : value;
    }

    return msgData;
  }

  // 解析普通消息内容
  const parseMessage = (message) => {
    try {
      switch (message.msgType) {
        case 1: {
          // 文本消息 {endFlag=0, msgSeq=0, stopFlag=0, text=内容} 或 {srcMsgId=xxx, text=内容}
          if (message.msg.startsWith('{') && message.msg.endsWith('}')) {
            let cleanMsg = message.msg.slice(1, -1);

            // 临时保护：将 text= 后面到结尾（或下一个字段）中的逗号替换
            cleanMsg = cleanMsg.replace(
              /(text=)([^,]*?(?:,[^,]*?)*?)(?=,?\s*(?:endFlag|msgSeq|srcMsgId|stopFlag|stopLength)=|$)/,
              (match, prefix, content) => {
                return prefix + content.replace(/,/g, '\u0000'); // 用不可见字符替代
              },
            );
            const result = parseMessage2(cleanMsg);
            // 恢复逗号
            if (result?.text) {
              result.text = result.text.replace(/\u0000/g, ',');
            }
            return result;
          }
          return { text: message.msg };
        }
      case 2: {
        // 文件消息 - 尝试JSON解析，失败则使用简单解析
        try {
          return JSON.parse(message.msg);
        } catch {
          // 使用与文本消息相同的解析方法
          if (message.msg.startsWith('{') && message.msg.endsWith('}')) {
            const msgData = {};
            const cleanMsg = message.msg.slice(1, -1);
            const parts = cleanMsg.split(', ');
            parts.forEach((part) => {
              const equalIndex = part.indexOf('=');
                if (equalIndex !== -1) {
                  const key = part.substring(0, equalIndex).trim();
                  const value = part.substring(equalIndex + 1).trim();
                msgData[key] = value === 'null' ? null : value;
              }
            });
            return msgData;
          }
          return { text: message.msg };
        }
      }
      case 5: {
        // 个人名片消息
        try {
          return JSON.parse(message.msg);
        } catch {
          return message.msg;
        }

      break;
      }
      case 6: {
        return parseGroupJoiningMessage(message);
      }
      case 8: {
        // 转发消息
        try {
          return JSON.parse(message.msg);
        } catch {
          return message.msg;
        }

        break;
        }
        // No default
      }

      return message.msg;
    } catch (error) {
      console.error('解析消息失败:', error);
      return message.msg;
    }
  };

  // 获取被引用的消息
  const getQuotedMessage = (srcMsgId) => {
    if (!srcMsgId) return null;
    const allChatList = [...allChatHistorySet.value];
    return allChatList.find((msg) => msg.msgId === srcMsgId.toString());
  };

  // 获取消息显示内容
  const getMessageContent = (message) => {
    const parsedMsg = parseMessage(message);
    switch (message.msgType) {
      case 1: {
        // 文本消息
        if (parsedMsg && parsedMsg.srcMsgId) {
          const quotedMsg = getQuotedMessage(parsedMsg.srcMsgId);
          console.log('引用消息详情:', {
            quotedMsg,
            text: parsedMsg.text,
            quotedMsg: quotedMsg,
          });

          return {
            content: parsedMsg.text || '',
            quotedMsg,
            srcMsgId: parsedMsg.srcMsgId,
            type: 'quote',
          };
        }
        // 普通文本消息
        let textContent = '';
        if (parsedMsg && parsedMsg.text) {
          textContent = parsedMsg.text;
        } else if (typeof parsedMsg === 'string') {
          textContent = parsedMsg;
        } else {
          textContent = message.msg;
        }

        return {
          content: textContent,
          type: 'text',
        };
      }

      case 2: {
        // 文件消息
        return {
          content: parsedMsg,
          type: 'file',
        };
      }
      case 5: {
        // 个人名片
        return {
          content: parsedMsg,
          type: 'userCard',
        };
      }
      case 6: {
        // 群接龙
        return {
          content: parsedMsg,
          type: 'groupJoining',
        };
      }

      case 7: {
        // 撤回消息
        return {
          content: '[消息已撤回]',
          type: 'system',
        };
      }

      case 8: {
        // 转发消息
        return {
          content: parsedMsg,
          type: 'forward',
        };
      }
      case 10: {
        // 群组变更事件
        return {
          content: parsedMsg,
          type: 'forward',
        };
      }

      default: {
        return {
          content: `[未知消息类型: ${message.msgType}]`,
          type: 'unknown',
        };
      }
    }
  };

  // 处理@消息显示
  const formatTextMessage = (text) => {
    if (!text) return '';
    return text.replaceAll(/\[@(\d+):@([^\]]+)\]/g, (match, userId, userName) => {
      // 如果是@all，显示为"@所有人"
      if (userName === 'all') {
        return '@所有人 ';
      }
      // 其他情况正常显示@用户名
      return `@${userName} `;
    });
  };

  // 格式化时间参数（开始时间末尾加 :00，结束时间末尾加 :59）
  const formatTimeParam = (time, isEnd = false) => {
    if (!time) return '';
    // 如果已经包含秒，直接返回
    if (time.includes(':') && time.split(':').length === 3) {
      return time;
    }
    // 否则添加秒
    return isEnd ? `${time}:59` : `${time}:00`;
  };

  // 获取聊天历史记录
  const fetchChatHistory = async (page = 1, isLoadMore = false) => {
    if (!props.groupId || (isLoadMore && !hasMore.value) || isLoadingMore.value) return;
    isLoadingMore.value = true;
    try {
      const keywords = encodeURIComponent(searchText.value);
      const res = await getChatHistory({
        endTime: formatTimeParam(endTime.value, true),
        groupId: props.groupId,
        keywords,
        pageNum: page,
        pageSize: pageSize.value,
        startTime: formatTimeParam(startTime.value, false),
        from: selectedMemberId.value, // 新增：群成员筛选
      });

      if (res.data && res.data.records) {
        if (isLoadMore) {
          const oldScrollHeight = messagesContainer.value?.scrollHeight || 0;
          const oldScrollTop = messagesContainer.value?.scrollTop || 0;

          chatHistory.value = [...res.data.records, ...chatHistory.value];

          // 处理消息列表
          await processMessages();

          nextTick(() => {
            if (messagesContainer.value) {
              const newScrollHeight = messagesContainer.value.scrollHeight;
              messagesContainer.value.scrollTop = newScrollHeight - oldScrollHeight + oldScrollTop;
            }

            // 获取聊天记录后，补充缺失成员
            if (!isLoadMore) {
              fetchAllMissingMembersFromChatHistory();
            }
          });
        } else {
          chatHistory.value = res.data.records;

          // 处理消息列表
          await processMessages();

          nextTick(() => {
            scrollToBottom();
            // 获取聊天记录后，补充缺失成员
            fetchAllMissingMembersFromChatHistory();
          });
        }

        hasMore.value = res.data.records.length === pageSize.value;
        currentPage.value = page;
      }
    } catch (error) {
      console.error('获取聊天记录失败:', error);
    } finally {
      isLoadingMore.value = false;
      isInitialLoad.value = false;
    }
  };

  // 解析群接龙消息
  const parseGroupJoiningMessage = (message) => {
    if (message.msgType === 6) {
      try {
        const result = JSON.parse(message.msg);
        return result;
      } catch {
        return message.msg;
      }
    }
    return message.msg;
  };

  // 获取群接龙内容
  const getGroupJoiningContent = (message) => {
    const content = getMessageContent(message).content;

    // 处理参与者数据
    let participants = [];
    const missingUserIds = [];
    if (content.userTxts && Array.isArray(content.userTxts)) {
      // 按时间戳排序，时间最早的排在最前面
      const sortedUserTxts = [...content.userTxts].sort((a, b) => {
        const timeA = a.timeStamp || a.clientTimeStamp || 0;
        const timeB = b.timeStamp || b.clientTimeStamp || 0;
        return timeA - timeB;
      });
      participants = sortedUserTxts.map((userTxt, index) => {
        const memberInfo = groupMembers.value.find((member) => member.id === userTxt.userId);
        if (memberInfo) {
          // 判断是否为协同岗身份
          const realName = memberInfo.realName || '';
          const postName = memberInfo.postName || '';
          let txt = (userTxt.txt || '').split(' ')[0];
          try {
            txt = txt.split(' ')[0];
          } catch {
            txt = userTxt.txt || '';
          }
          
          // 三种情况判断为协同岗身份：
          // 1. txt 等于岗位名
          // 2. txt 符合 "岗位名(真实姓名)" 格式
          // 3. txt 包含 "(真实姓名)" 但不以真实姓名开头
          const isCollaborationIdentity = 
            (postName && txt === postName) ||
            (postName && realName && txt === `${postName}(${realName})`) ||
            (realName && !txt.startsWith(realName) && txt.includes(`(${realName})`));
          
          
          // 获取用户头像（支持 avatar 和 tumbAvatar 两种字段名）
          const userAvatar = memberInfo.tumbAvatar || memberInfo.avatar || '';
          
          // 如果是协同岗身份，优先使用协同岗头像；否则使用普通用户头像
          const avatar = isCollaborationIdentity 
            ? (memberInfo.collaborationAvatar || userAvatar)
            : userAvatar;
          
          
          return {
            avatar: avatar,
            isCreator: index === 0, // 第一个参与者就是发起人
            name: userTxt.txt || `用户${userTxt.userId}`,
            timeStamp: userTxt.timeStamp || userTxt.clientTimeStamp,
            userId: userTxt.userId,
          };
        } else {
          // 将不在groupMembers里面的成员id收集起来
          missingUserIds.push(userTxt.userId);
          return {
            avatar: '', // 暂时留空
            isCreator: index === 0,
            isMissing: true, // 标记为需要获取信息的用户
            name: userTxt.txt || `用户${userTxt.userId}`,
            timeStamp: userTxt.timeStamp || userTxt.clientTimeStamp,
            userId: userTxt.userId,
          };
        }
      });
    }

    // 计算去重后的参与人数：协同岗账号和其对应的真实用户算作同一人
    const getRealUserIdForDedup = (userId) => {
      const member = groupMembers.value.find((m) => m.id == userId);
      if (!member || !member.postName) return userId.toString();
      if (member.name === member.postName) {
        const realUser = groupMembers.value.find(
          (m) => m.postName === member.postName && m.name !== member.postName,
        );
        return realUser ? realUser.id.toString() : userId.toString();
      }
      return userId.toString();
    };

    // 按真实用户去重计算人数，但参与者列表保留全部条目
    const uniqueRealUserIds = new Set();
    participants.forEach((p) => {
      uniqueRealUserIds.add(getRealUserIdForDedup(p.userId));
    });
    const participantCount = uniqueRealUserIds.size;

    // 获取发起人名称 - 第一个参与者就是发起人
    let creatorName = '未知用户';
    if (participants.length > 0) {
      creatorName = participants[0].name;
    } else if (content.creator) {
      // 如果没有参与者但有creator字段，使用creator
      const creatorUser = PIMStore.userMap.get(Number(content.creator)) || {};
      creatorName = creatorUser.name || `用户${content.creator}`;
    }

    return {
      creatorName,
      participantCount,
      participants,
      text: content.text || '未命名接龙',
    };
  };

  // 获取群成员信息
  const getGroupMembers = async () => {
    const res = await getChatHistoryCount({ groupId: props.groupId, pageSize: 100 });
    if (res.data) {
      groupMembers.value = res.data.records;
      // 补充协同岗信息（头像优先使用协同岗头像）
      await supplementCollaborationInfo();
      // 处理消息列表
      await processMessages();
      nextTick(async () => {
        await fetchAllMissingMembersFromChatHistory();
      });
    }
  };

  // 补充协同岗信息（头像优先使用协同岗头像）
  const supplementCollaborationInfo = async () => {
    try {
      const memberIds = groupMembers.value.map((m) => m.id.toString()).filter((id) => id);
      if (memberIds.length === 0) return;

      // 获取协同岗信息
      const collorationRes = await getCollocation({ userIds: memberIds });
      if (collorationRes?.data && Array.isArray(collorationRes.data)) {
        // 构建两个映射：
        // 1. 协同岗ID -> 协同岗信息（协同岗本身）
        // 2. 关联用户ID -> 协同岗信息（关联的真实用户）
        const collorationMap = new Map();
        const relatedUserMap = new Map();
        
        collorationRes.data.forEach((item) => {
          if (item && item.id) {
            // 映射1：协同岗ID -> 协同岗信息
            collorationMap.set(item.id.toString(), {
              postName: item.postName,
              iconUrl: item.iconUrl, // 协同岗头像
            });
            
            // 映射2：关联用户ID -> 协同岗信息
            if (item.relatedUserIds) {
              item.relatedUserIds.split(',').forEach((userId) => {
                relatedUserMap.set(userId.trim(), {
                  postName: item.postName,
                  iconUrl: item.iconUrl, // 协同岗头像
                  realName: item.relatedUserNames, // 关联用户的真实名称
                });
              });
            }
          }
        });
        
        // 更新群成员信息，添加协同岗头像字段
        groupMembers.value = groupMembers.value.map((member) => {
          // 先确保所有成员都有 realName
          const baseMember = {
            ...member,
            realName: member.realName || member.name,
          };
          
          // 先检查是否是协同岗本身
          const collorationInfo = collorationMap.get(member.id.toString());
          if (collorationInfo) {
            return {
              ...baseMember,
              // 保留协同岗头像字段，用于群接龙中判断身份后使用
              collaborationAvatar: collorationInfo.iconUrl,
              // 保留岗位名称，用于群接龙身份判断
              postName: collorationInfo.postName,
            };
          }
          
          // 再检查是否是关联用户
          const relatedInfo = relatedUserMap.get(member.id.toString());
          if (relatedInfo) {
            return {
              ...baseMember,
              // 保留协同岗头像字段，用于群接龙中判断身份后使用
              collaborationAvatar: relatedInfo.iconUrl,
              // 保留岗位名称，用于群接龙身份判断
              postName: relatedInfo.postName,
            };
          }
          
          return baseMember;
        });
      }
    } catch (error) {
      console.error('补充协同岗信息失败:', error);
    }
  };
  // 获取聊天记录中涉及的所有非本群成员
  const fetchAllMissingMembersFromChatHistory = async () => {
    try {
      // 从聊天记录中提取所有用户ID
      const allUserIds = new Set();

      chatHistory.value.forEach((message) => {
        // 消息发送者
        if (message.from) {
          allUserIds.add(message.from.toString());
        }

        // 群接龙参与者
        if (message.msgType === 6) {
          try {
            const content = JSON.parse(message.msg);
            if (content.userTxts && Array.isArray(content.userTxts)) {
              content.userTxts.forEach((userTxt) => {
                if (userTxt.userId) {
                  allUserIds.add(userTxt.userId.toString());
                }
              });
            }
          } catch {}
        }

        // 协同岗上下岗消息 (operationType=11) - 收集 realUserId 和 userId
        if (message.msgType === 10) {
          try {
            // 尝试从原始消息字符串中提取 coUserGroupRelations
            const msgStr = message.msg || '';
            if (msgStr.includes('operationType=11') && msgStr.includes('coUserGroupRelations')) {
              // 提取 realUserId
              const realUserIdMatch = msgStr.match(/realUserId=(\d+)/);
              if (realUserIdMatch && realUserIdMatch[1]) {
                allUserIds.add(realUserIdMatch[1]);
              }
              // 提取 userId（协同岗ID）- 这个用于查询协同岗信息
              const userIdMatch = msgStr.match(/userId=(\d+)/);
              if (userIdMatch && userIdMatch[1]) {
                allUserIds.add(userIdMatch[1]);
              }
            }
          } catch (e) {
            console.error('[ChatHistory] 解析协同岗消息失败:', e);
          }
        }
      });

      // 从聊天记录中提取所有协同岗ID（用于查询协同岗信息，获取关联用户信息）
      const collabUserIds = new Set();
      chatHistory.value.forEach((message) => {
        if (message.msgType === 10) {
          try {
            const msgStr = message.msg || '';
            if (msgStr.includes('operationType=11') && msgStr.includes('coUserGroupRelations')) {
              const userIdMatch = msgStr.match(/userId=(\d+)/);
              if (userIdMatch && userIdMatch[1]) {
                collabUserIds.add(userIdMatch[1]);
              }
            }
          } catch {}
        }
      });
      
      
      // 过滤掉已经在群成员中的用户
      const existingIds = groupMembers.value.map((m) => m.id.toString());
      
      const missingIds = [...allUserIds].filter((id) => !existingIds.includes(id));
      if (missingIds.length > 0) {
        // 先从已有的群成员中提取协同岗信息（用于匹配关联用户）
        const existingRelatedUserMap = new Map();
        groupMembers.value.forEach((member) => {
          if (member.postName && member.collaborationAvatar) {
            // 该成员有协同岗信息，可能是协同岗本身或关联用户
            existingRelatedUserMap.set(member.id.toString(), {
              postName: member.postName,
              iconUrl: member.collaborationAvatar,
              realName: member.realName,
            });
          }
        });
        
        // 并发获取警信用户信息和协同岗信息
        // 注意：getCollocation 需要用协同岗ID查询，而不是关联用户ID
        const collabIdsToQuery = [...collabUserIds].filter((id) => !existingIds.includes(id));
        const [userRes, collorationRes] = await Promise.all([
          getUserInfo({ userIds: missingIds }),
          getCollocation({ userIds: collabIdsToQuery.length > 0 ? collabIdsToQuery : missingIds }),
        ]);

        // 构建两个映射：
        // 1. 协同岗ID -> 协同岗信息（协同岗本身）
        // 2. 关联用户ID -> 协同岗信息（关联的真实用户）
        const collorationMap = new Map();
        const relatedUserMap = new Map(existingRelatedUserMap); // 初始化为已有的协同岗信息
        if (collorationRes?.data && Array.isArray(collorationRes.data)) {
          collorationRes.data.forEach((item) => {
            if (item && item.id) {
              // 映射1：协同岗ID -> 协同岗信息
              collorationMap.set(item.id.toString(), {
                postName: item.postName,
                iconUrl: item.iconUrl, // 协同岗头像
              });
              
              // 映射2：关联用户ID -> 协同岗信息
              if (item.relatedUserIds) {
                item.relatedUserIds.split(',').forEach((userId) => {
                  relatedUserMap.set(userId.trim(), {
                    postName: item.postName,
                    iconUrl: item.iconUrl, // 协同岗头像
                    realName: item.relatedUserNames, // 关联用户的真实名称
                  });
                });
              }
            }
          });
        }
        
        // 从已有群成员中查找协同岗成员，提取其关联用户信息
        // 这样即使关联用户不在群成员列表中，也能获取到协同岗信息
        const collaborationMembers = groupMembers.value.filter((m) => m.postName && m.collaborationAvatar);
        for (const collabMember of collaborationMembers) {
          // 查询该协同岗的详细信息，获取关联用户ID
          try {
            const collabDetailRes = await getCollocation({ userIds: [collabMember.id.toString()] });
            if (collabDetailRes?.data && Array.isArray(collabDetailRes.data)) {
              collabDetailRes.data.forEach((item) => {
                if (item && item.relatedUserIds) {
                  item.relatedUserIds.split(',').forEach((userId) => {
                    const trimmedUserId = userId.trim();
                    // 为所有关联用户添加映射（无论是否在 missingIds 中）
                    relatedUserMap.set(trimmedUserId, {
                      postName: item.postName,
                      iconUrl: item.iconUrl,
                      realName: item.relatedUserNames,
                    });
                  });
                }
              });
            }
          } catch (e) {
            console.error('获取协同岗详细信息失败:', e);
          }
        }
        
        // 为已有的群成员补充协同岗信息（如果他们之前没有获取到）
        groupMembers.value = groupMembers.value.map((member) => {
          const memberIdStr = member.id.toString();
          // 如果该成员没有协同岗信息，检查是否在 relatedUserMap 中
          if (!member.collaborationAvatar && !member.postName) {
            const relatedInfo = relatedUserMap.get(memberIdStr);
            if (relatedInfo) {
              return {
                ...member,
                collaborationAvatar: relatedInfo.iconUrl,
                postName: relatedInfo.postName,
              };
            }
          }
          return member;
        });

        // 构建要添加的成员列表
        const additionalMembers = [];

        // 1. 从 getUserInfo 结果中添加用户
        if (userRes?.data?.results) {
          additionalMembers.push(...userRes.data.results.map((user) => {
            // 先检查是否是协同岗本身
            const collorationInfo = collorationMap.get(user.id.toString());
            if (collorationInfo) {
              return {
                id: user.id,
                name: user.name || `用户${user.userId}`,
                tumbAvatar: user.avatar || '',
                realName: user.name || '',
                collaborationAvatar: collorationInfo.iconUrl,
                postName: collorationInfo.postName,
                isExternal: true, // 标记为非本群成员
              };
            }
            
            // 再检查是否是关联用户
            const relatedInfo = relatedUserMap.get(user.id.toString());
            if (relatedInfo) {
              return {
                id: user.id,
                name: user.name || `用户${user.userId}`,
                tumbAvatar: user.avatar || '',
                realName: user.name || '',
                collaborationAvatar: relatedInfo.iconUrl,
                postName: relatedInfo.postName,
                isExternal: true, // 标记为非本群成员
              };
            }
            
            // 普通用户
            return {
              id: user.id,
              name: user.name || `用户${user.userId}`,
              tumbAvatar: user.avatar || '',
              realName: user.name || '',
              isExternal: true, // 标记为非本群成员
            };
          }));
        }

        // 2. 对于 missingIds 中还没有被处理的用户，从 relatedUserMap 中添加
        const processedIds = new Set(additionalMembers.map((m) => m.id.toString()));
        for (const missingId of missingIds) {
          if (!processedIds.has(missingId)) {
            const relatedInfo = relatedUserMap.get(missingId);
            if (relatedInfo) {
              additionalMembers.push({
                id: missingId,
                name: relatedInfo.realName || `用户${missingId}`,
                tumbAvatar: '',
                realName: relatedInfo.realName || '',
                collaborationAvatar: relatedInfo.iconUrl,
                postName: relatedInfo.postName,
                isExternal: true,
              });
            }
          }
        }

        if (additionalMembers.length > 0) {
          groupMembers.value = [...groupMembers.value, ...additionalMembers];
          // 更新群成员后，重新处理消息列表
          await processMessages();
        }
      }
    } catch (error) {
      console.error('获取非本群成员信息失败:', error);
    }
  };
  // 格式化日期（使用统一的本地化时间格式化）
  const formatDate = formatDateTimeLocal;
  // 获取所有附件
  const getAllAttachments = () => {
    const attachments = [];
    chatHistory.value.forEach((message) => {
      if (message.msgType === 2) {
        const fileData = parseMessage(message);
        if (fileData.fileKey && fileData.filePath) {
          attachments.push({
            fileKey: fileData.fileKey,
            fileType: fileData.fileType,
            url: `${getIp()}/linkx/desktop${fileData.filePath}`,
          });
        }
        if (fileData.videoThumb) {
          attachments.push({
            fileKey: fileData.videoThumb,
            fileType: 1,
            url: `${getIp()}/linkx/desktop${fileData.filePath.replace(/\.[^/.]+$/, '')}_thumb.jpg`, // 假设缩略图路径
          });
        }
      }
    });
    return attachments;
  };
  const lookImage = (url) => {
    imagePreviewUrl.value = url;
    showDialog.value = true;
  };
  const lookVideo = (url) => {
    videoPreviewUrl.value = url;
    showVideoDialog.value = true;
    // 重置播放状态
    nextTick(() => {
      if (videoPlayer.value) {
        videoPlayer.value.pause();
        isPlaying.value = false;
      }
    });
  };

  // 滚动到底部
  const scrollToBottom = () => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  };

  // 加载更多历史消息
  const loadMoreMessages = async () => {
    if (!hasMore.value || isLoadingMore.value) return;
    await fetchChatHistory(currentPage.value + 1, true);
  };

  // 滚动事件处理
  const handleScroll = () => {
    if (!messagesContainer.value || isLoadingMore.value) return;

    const { scrollTop } = messagesContainer.value;
    if (scrollTop < 100 && hasMore.value) {
      loadMoreMessages();
    }
  };

  // 监听groupId变化
  watch(
    () => props.groupId,
    (newVal) => {
      if (newVal) {
        currentPage.value = 1;
        hasMore.value = true;
        isInitialLoad.value = true;
        chatHistory.value = [];
        groupMembers.value = [];
        selectedMemberId.value = ''; // 清空成员筛选
        getGroupMembers();
        fetchChatHistory();
        fetchMemberList(); // 重新获取群成员列表
      }
    },
  );

  // 组件挂载时获取数据
  onMounted(() => {
    if (props.groupId) {
      setFetchChatHistoryTime();
      getGroupMembers();
      fetchChatHistory();
      fetchMemberList(); // 获取群成员列表
    }
    if (videoPlayer.value) {
      videoPlayer.value.addEventListener('play', () => {
        isPlaying.value = true;
      });
      videoPlayer.value.addEventListener('pause', () => {
        isPlaying.value = false;
      });
      videoPlayer.value.addEventListener('ended', () => {
        isPlaying.value = false;
      });
    }
  });

  // 搜索
  const searchText = ref('');
  const startTime = ref('');
  const endTime = ref('');
  const days = ref(7); // 当前点击标签的索引
  
  // 群成员筛选
  const selectedMemberId = ref('');
  const memberList = ref([]);
  const memberLoading = ref(false);
  const memberPage = ref(1);
  const memberPageSize = ref(20);
  const memberHasMore = ref(true);
  
  // 获取群成员列表（分页）
  const fetchMemberList = async (isLoadMore = false) => {
    if (!props.groupId || memberLoading.value) return;
    if (isLoadMore && !memberHasMore.value) return;
    
    memberLoading.value = true;
    try {
      const res = await getChatHistoryCount({
        groupId: props.groupId,
        pageSize: memberPageSize.value,
        pageNum: isLoadMore ? memberPage.value + 1 : 1,
      });
      
      if (res.data && res.data.records) {
        if (isLoadMore) {
          memberList.value = [...memberList.value, ...res.data.records];
        } else {
          memberList.value = res.data.records;
        }
        memberHasMore.value = res.data.records.length === memberPageSize.value;
        if (isLoadMore) {
          memberPage.value++;
        } else {
          memberPage.value = 1;
        }
      }
    } catch (error) {
      console.error('获取群成员列表失败:', error);
    } finally {
      memberLoading.value = false;
    }
  };
  
  // 群成员选择变化
  const handleMemberChange = () => {
    currentPage.value = 1;
    hasMore.value = true;
    fetchChatHistory();
  };
  
  // 清除成员筛选
  const clearMemberFilter = () => {
    selectedMemberId.value = '';
    currentPage.value = 1;
    hasMore.value = true;
    fetchChatHistory();
  };
  
  // 计算属性：群成员选项
  const memberOptions = computed(() => {
    return memberList.value.map(member => ({
      label: member.name,
      value: member.id,
      avatar: member.tumbAvatar || member.avatar ? `${getIp()}/linkx/desktop${member.tumbAvatar || member.avatar}` : undefined,
    }));
  });
  
  // 加载更多群成员
  const loadMoreMembers = () => {
    fetchMemberList(true);
  };
  
  function getRecentDateRange(days, endDate = new Date()) {
    // 确保 days 是正整数
    days = Math.abs(Number.parseInt(days)) || 1;

    // 处理结束日期
    const end = new Date(endDate);
    end.setHours(23, 59, 59, 999);

    // 计算开始日期（往前推 days-1 天）
    const start = new Date(end);
    start.setDate(start.getDate() - (days - 1));
    start.setHours(0, 0, 0, 0);

    // 格式化函数
    const formatDate = (date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');

      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    };

    return {
      endTime: formatDate(end),
      startTime: formatDate(start),
    };
  }
  const setFetchChatHistoryTime = (rangeTime = null) => {
    if (!rangeTime) {
      rangeTime = getRecentDateRange(days.value);
    }
    if (timeRange.value.length === 0) {
      timeRange.value = [rangeTime.startTime, rangeTime.endTime];
    }
    // 时间格式已包含时分秒，直接赋值
    startTime.value = rangeTime.startTime;
    endTime.value = rangeTime.endTime;
  };
  const fetchChatHistoryDeunce = debounce(() => {
    searchText.value = searchText.value.trim(); // 去除两端空格
    currentPage.value = 1;
    hasMore.value = true;
    fetchChatHistory();
  }, 500);
  watch(
    () => searchText.value,
    () => {
      fetchChatHistoryDeunce();
    },
  );

  // 日期改造组件
  const timeRange = ref([]);
  const shortcuts = [
    {
      text: '最近一周',
      value: () => {
        const end = new Date();
        end.setHours(23, 59, 0);
        const start = new Date();
        start.setDate(end.getDate() - 6);
        start.setHours(0, 0, 0);
        return [start, end];
      },
    },
    {
      text: '最近一月',
      value: () => {
        const end = new Date();
        end.setHours(23, 59, 0);
        const start = new Date();
        start.setMonth(start.getMonth() - 1);
        start.setHours(0, 0, 0);
        return [start, end];
      },
    },
    {
      text: '最近半年',
      value: () => {
        const end = new Date();
        end.setHours(23, 59, 0);
        const start = new Date();
        start.setMonth(start.getMonth() - 6);
        start.setHours(0, 0, 0);
        return [start, end];
      },
    },
    {
      text: '最近一年',
      value: () => {
        const end = new Date();
        end.setHours(23, 59, 0);
        const start = new Date();
        start.setFullYear(end.getFullYear() - 1);
        start.setHours(0, 0, 0);
        return [start, end];
      },
    },
    {
      text: '全部',
      value: () => {
        // 使用特殊日期（1970-01-01）作为"全部"的标记
        // 在 useWebView2DatePicker 中会检测并转换为空数组
        const specialDate = new Date(0);
        return [specialDate, specialDate];
      },
    },
  ];
  // 禁止选择大于今天的日期
  function disabledDate(time) {
    return time.getTime() > Date.now();
  }

  // WebView2 日期选择 workaround（支持时分秒）
  const { 
    datePickerRef, 
    datePickerVisible, 
    handleCalendarChange,
    handlePanelChange,
    handleDateChange, 
    handleVisibleChange,
    // popperOptions,
    // teleported,
  } = useWebView2DatePicker(timeRange, undefined, { includeTime: true });

  watch(
    () => timeRange.value,
    (data) => {
      // 处理"全部"选项（空数组）
      if (!data || data.length === 0) {
        startTime.value = '';
        endTime.value = '';
        currentPage.value = 1;
        hasMore.value = true;
        fetchChatHistory();
        return;
      }
      let date = { endTime: '', startTime: '' };
      date = { endTime: data[1], startTime: data[0] };
      setFetchChatHistoryTime(date);
      fetchChatHistoryDeunce();
    },
  );
  // 搜索end
</script>

<template>
  <div class="search">
    <!-- 搜索框 -->
    <div class="search-container">
      <el-input v-model="searchText" clearable placeholder="请输入聊天内容搜索" />
    </div>
    
    <!-- 群成员筛选 -->
    <div class="member-filter">
      <CustomSelect
        v-model="selectedMemberId"
        :options="memberOptions"
        :loading="memberLoading"
        :has-more="memberHasMore"
        clearable
        placeholder="请选择群成员"
        @change="handleMemberChange"
        @load-more="loadMoreMembers"
      />
    </div>

    <!-- 日期切换部分 -->
    <div class="time-range">
      <el-date-picker
        ref="datePickerRef"
        v-model="timeRange"
        v-model:visible="datePickerVisible"
        :disabled-date="disabledDate"
        end-placeholder="结束时间"
        format="YYYY-MM-DD HH:mm"
        popper-class="chat-history-date-picker"
        range-separator="至"
        :shortcuts="shortcuts"
        start-placeholder="开始时间"
        type="datetimerange"
        value-format="YYYY-MM-DD HH:mm"
        :teleported="true"
        :popper-options="{
          strategy: 'fixed',
          modifiers: [
            { name: 'preventOverflow', options: { padding: 8 } },
            { name: 'flip', options: { fallbackPlacements: ['bottom-start', 'bottom-end', 'top-start', 'top-end'] } }
          ]
        }"
        @calendar-change="handleCalendarChange"
        @panel-change="handlePanelChange"
        @change="handleDateChange"
        @visible-change="handleVisibleChange"
      />
    </div>
  </div>
  <div class="chat-history-container">
    <div ref="messagesContainer" class="chat-messages" @scroll="handleScroll">
      <!-- 加载更多提示 -->
      <div v-if="isLoadingMore" class="loading-more">
        <div class="loading-spinner"></div>
        <span>加载历史消息...</span>
      </div>

      <el-empty v-if="processedMessages.length === 0" description="没有更多数据了" />
      <!-- 消息列表 -->
      <template v-for="(message, index) in processedMessages" :key="message.msgId">
        <!-- 时间显示 -->
        <div v-if="message.msgType !== 10 && isValidGroupEvent(message)">
          <div
            v-if="
              index === 0 ||
              parseInt(message.time) - parseInt(processedMessages[index - 1].time) > 3 * 60 * 1000
            "
            class="time-divider"
          >
            {{ formatDate(parseInt(message.time)) }}
          </div>
        </div>

        <!-- 群组通知消息 -->
        <GroupNotice
          v-if="isValidGroupEvent(message)"
          :disable-you-alias="props.disableYouAlias"
          :hide-time="true"
          :member-list="groupMembers"
          :message="message"
        />

        <!-- 普通消息 -->
        <div
          v-else-if="message.msgType !== 10"
          class="message-item"
          :class="[isSelf(message) ? 'own-message' : 'other-message']"
        >
          <!-- 对方消息（左侧） -->
          <div v-if="!isSelf(message)" class="message-left">
            <div class="avatar">
              <img
                v-if="message.avatar"
                alt=""
                class="user-avatar"
                :src="`${getIp()}/linkx/desktop${message.avatar}`"
              />
              <el-icon v-else class="item-icon">
                <component is="Avatar" :message="message" />
              </el-icon>
            </div>
            <div class="message-content">
              <div class="sender-name">{{ message.name }}</div>
              <!-- 消息时间 -->
              <div class="message-time">
                {{ formatDate(parseInt(message.time)) }}
              </div>
              <div style="display: flex; justify-content: flex-start">
                <div class="message-bubble">
                  <!-- 引用消息 -->
                  <div v-if="getMessageContent(message).type === 'quote'" class="quote-message">
                    <div class="quote-text">
                      <EmojiView
                        :is-list="false"
                        style="color: var(--text-color)"
                        :text="formatTextMessage(getMessageContent(message).content)"
                        :highlight-keyword="searchText"
                      />
                    </div>
                    <quoteMsg2
                      v-if="groupMembers.length > 0"
                      :attach-list="getAllAttachments()"
                      :member-list="groupMembers"
                      :quote-msg="getMessageContent(message).quotedMsg"
                      :quote-msg-id="getMessageContent(message).srcMsgId"
                    />
                  </div>

                  <!-- 文本消息 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'text'"
                    class="text-message text-message-left"
                  >
                    <EmojiView
                      :is-list="false"
                      :text="formatTextMessage(getMessageContent(message).content)"
                      :highlight-keyword="searchText"
                    />
                  </div>

                  <!-- 文件消息 -->
                  <div v-else-if="getMessageContent(message).type === 'file'" class="file-message">
                    <!-- 图片 -->
                    <img
                      v-if="getMessageContent(message).content.fileType === 1"
                      alt=""
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                      @click="
                        lookImage(
                          `${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`,
                        )
                      "
                    />
                    <!-- 视频 -->
                    <!-- <video controls preload="metadata"
                      v-if="getMessageContent(message).content.fileType == 3"
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                      @click="lookVideo(`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`)"
                    ></video> -->
                    <div
                      v-if="getMessageContent(message).content.fileType === 3"
                      class="video-container"
                      @click="
                        lookVideo(
                          `${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`,
                        )
                      "
                    >
                      <video
                        playsinline
                        :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                        style="max-width: 300px; max-height: 200px"
                      ></video>
                      <div class="play-button">
                        <svg height="48" viewBox="0 0 64 64" width="48">
                          <circle cx="32" cy="32" fill="rgba(0,0,0,0.5)" r="32" />
                          <polygon fill="white" points="26,22 26,42 42,32" />
                        </svg>
                      </div>
                    </div>
                    <!-- 文件 -->
                    <MsgFile
                      v-if="
                        getMessageContent(message).content.fileType === 4 ||
                        getMessageContent(message).content.fileType === 5 ||
                        getMessageContent(message).content.fileType === 6 ||
                        getMessageContent(message).content.fileType === 7 ||
                        getMessageContent(message).content.fileType === 8 ||
                        getMessageContent(message).content.fileType === 9 ||
                        getMessageContent(message).content.fileType === 10 ||
                        getMessageContent(message).content.fileType === 11
                      "
                      :card-data="getMessageContent(message).content"
                    />
                    <!-- 语音 -->
                    <msgVoiceHistory
                      v-if="getMessageContent(message).content.fileType === 2"
                      :audio-time="getMessageContent(message).content.duration || 0"
                      :is-self="isSelf(message)"
                      :self-id="message.msgId"
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                    />
                  </div>

                  <!-- 转发消息 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'forward'"
                    class="forward-message"
                  >
                    <MsgForward
                      :card-data="getMessageContent(message).content"
                      :group-id="groupId"
                      :member-list="groupMembers"
                    />
                  </div>

                  <!-- 个人名片 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'userCard'"
                    class="userCard-message"
                  >
                    <MsgCard :card-data="getMessageContent(message).content" />
                  </div>

                  <!-- 群接龙 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'groupJoining'"
                    class="group-joining-message"
                  >
                    <div class="group-joining-content">
                      <!-- 接龙标题 -->
                      <div class="joining-header">#接龙</div>

                      <!-- 接龙内容 -->
                      <div class="joining-text">{{ getGroupJoiningContent(message).text }}</div>

                      <!-- 发起人和参与人数 -->
                      <div class="joining-info">
                        {{ getGroupJoiningContent(message).creatorName }}发起，参与共{{
                          getGroupJoiningContent(message).participantCount
                        }}人
                      </div>

                      <!-- 参与人员列表 -->
                      <div class="joining-participants">
                        <div
                          v-for="(participant, index) in getGroupJoiningContent(message)
                            .participants"
                          :key="participant.userId"
                          class="participant-item"
                        >
                          <span class="participant-index">{{ index + 1 }}</span>
                          <img
                            v-if="participant.avatar"
                            alt=""
                            class="participant-avatar"
                            :src="`${getIp()}/linkx/desktop${participant.avatar}`"
                          />
                          <img v-else alt="" class="participant-avatar" :src="defaultImg" />
                          <span class="participant-name">{{ participant.name }}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 系统消息 -->
                  <div v-else class="text-message">
                    <!-- {{ getMessageContent(message).content }} -->
                    <EmojiView :is-list="false" :text="getMessageContent(message).content" :highlight-keyword="searchText" />
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 当前用户消息（右侧） -->
          <div v-else class="message-right">
            <div class="avatar">
              <img
                v-if="message.avatar"
                alt=""
                class="user-avatar"
                :src="`${getIp()}/linkx/desktop${message.avatar}`"
              />
              <el-icon v-else class="item-icon">
                <component is="Avatar" :item="message" />
              </el-icon>
            </div>
            <div class="message-content">
              <div style="text-align: right">
                <div class="sender-name">{{ message.name }}</div>
                <!-- 消息时间 -->
                <div class="message-time">
                  {{ formatDate(parseInt(message.time)) }}
                </div>
              </div>
              <div style="display: flex; justify-content: flex-end">
                <div class="message-bubble own-bubble">
                  <!-- 引用消息 -->
                  <div v-if="getMessageContent(message).type === 'quote'" class="quote-message">
                    <div class="quote-text">
                      <EmojiView
                        :is-list="false"
                        :text="formatTextMessage(getMessageContent(message).content)"
                        :highlight-keyword="searchText"
                      />
                    </div>
                    <quoteMsg2
                      v-if="groupMembers.length > 0"
                      :attach-list="getAllAttachments()"
                      :member-list="groupMembers"
                      :quote-msg="getMessageContent(message).quotedMsg"
                      :quote-msg-id="getMessageContent(message).srcMsgId"
                    />
                  </div>

                  <!-- 文本消息 -->
                  <div v-else-if="getMessageContent(message).type === 'text'" class="text-message">
                    <EmojiView
                      :is-list="false"
                      :text="formatTextMessage(getMessageContent(message).content)"
                      :highlight-keyword="searchText"
                    />
                  </div>

                  <!-- 文件消息 -->
                  <div v-else-if="getMessageContent(message).type === 'file'" class="file-message">
                    <!-- 图片 -->
                    <img
                      v-if="getMessageContent(message).content.fileType === 1"
                      alt=""
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                      @click="
                        lookImage(
                          `${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`,
                        )
                      "
                    />
                    <!-- 视频 -->
                    <!-- <video controls preload="metadata"
                      v-if="getMessageContent(message).content.fileType == 3"
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                      @click="lookVideo(`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`)"
                    ></video> -->
                    <div
                      v-if="getMessageContent(message).content.fileType === 3"
                      class="video-container"
                      @click="
                        lookVideo(
                          `${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`,
                        )
                      "
                    >
                      <video
                        playsinline
                        :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                        style="max-width: 300px; max-height: 200px"
                      ></video>
                      <div class="play-button">
                        <svg height="48" viewBox="0 0 64 64" width="48">
                          <circle cx="32" cy="32" fill="rgba(0,0,0,0.5)" r="32" />
                          <polygon fill="white" points="26,22 26,42 42,32" />
                        </svg>
                      </div>
                    </div>
                    <!-- 文件 -->
                    <MsgFile
                      v-if="
                        getMessageContent(message).content.fileType === 4 ||
                        getMessageContent(message).content.fileType === 5 ||
                        getMessageContent(message).content.fileType === 6 ||
                        getMessageContent(message).content.fileType === 7 ||
                        getMessageContent(message).content.fileType === 8 ||
                        getMessageContent(message).content.fileType === 9 ||
                        getMessageContent(message).content.fileType === 10 ||
                        getMessageContent(message).content.fileType === 11
                      "
                      :card-data="getMessageContent(message).content"
                    />
                    <!-- 语音 -->
                    <msgVoiceHistory
                      v-if="getMessageContent(message).content.fileType === 2"
                      :audio-time="getMessageContent(message).content.duration || 0"
                      :is-self="isSelf(message)"
                      :self-id="message.msgId"
                      :src="`${getIp()}/linkx/desktop${getMessageContent(message).content.filePath}`"
                    />
                  </div>

                  <!-- 转发消息 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'forward'"
                    class="forward-message"
                  >
                    <MsgForward
                      :card-data="getMessageContent(message).content"
                      :group-id="groupId"
                      :member-list="groupMembers"
                    />
                  </div>

                  <!-- 个人名片 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'userCard'"
                    class="userCard-message"
                  >
                    <MsgCard :card-data="getMessageContent(message).content" />
                  </div>

                  <!-- 群接龙 -->
                  <div
                    v-else-if="getMessageContent(message).type === 'groupJoining'"
                    class="group-joining-message"
                  >
                    <div class="group-joining-content">
                      <!-- 接龙标题 -->
                      <div class="joining-header">#接龙</div>

                      <!-- 接龙内容 -->
                      <div class="joining-text">{{ getGroupJoiningContent(message).text }}</div>

                      <!-- 发起人和参与人数 -->
                      <div class="joining-info">
                        {{ getGroupJoiningContent(message).creatorName }}发起，参与共{{
                          getGroupJoiningContent(message).participantCount
                        }}人
                      </div>

                      <!-- 参与人员列表 -->
                      <div class="joining-participants">
                        <div
                          v-for="(participant, index) in getGroupJoiningContent(message)
                            .participants"
                          :key="participant.userId"
                          class="participant-item"
                        >
                          <span class="participant-index">{{ index + 1 }}</span>
                          <img
                            v-if="participant.avatar"
                            alt=""
                            class="participant-avatar"
                            :src="`${getIp()}/linkx/desktop${participant.avatar}`"
                          />
                          <img v-else alt="" class="participant-avatar" :src="defaultImg" />
                          <span class="participant-name">{{ participant.name }}</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <!-- 系统消息 -->
                  <div v-else class="text-message">
                    <!-- {{ getMessageContent(message).content }} -->
                    <EmojiView :is-list="false" :text="getMessageContent(message).content" :highlight-keyword="searchText" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 无法解析的群组消息 -->
        <!-- <div v-else-if="message.msgType === 10" class="system-message-item">
          <div class="system-content">
            [系统消息]
          </div>
        </div> -->
      </template>
    </div>
    <el-dialog v-model="showDialog" style="height: 700px" title="预览" width="1000px">
      <div
        style="
          display: flex;
          align-items: center;
          justify-content: center;
          width: 100%;
          height: 600px;
        "
      >
        <img alt="预览图片" :src="imagePreviewUrl" style="max-width: 100%; max-height: 600px" />
      </div>
    </el-dialog>
    <el-dialog v-model="showVideoDialog" style="height: 700px" title="预览" width="1000px">
      <div
        style="
          display: flex;
          align-items: center;
          justify-content: center;
          width: 1000px;
          height: 600px;
        "
      >
        <!-- <video
         controls
          playsinline
          :src="videoPreviewUrl"
           style="max-width: 95%; max-height: 600px;"
        ></video> -->
        <div class="video-container" @click="togglePlay">
          <video
            ref="videoPlayer"
            playsinline
            :src="videoPreviewUrl"
            style="max-width: 95%; max-height: 600px"
          ></video>
          <div v-if="!isPlaying" class="play-button">
            <svg height="64" viewBox="0 0 64 64" width="64">
              <circle cx="32" cy="32" fill="rgba(0,0,0,0.5)" r="32" />
              <polygon fill="white" points="26,22 26,42 42,32" />
            </svg>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="less">
  @media (max-width: 768px) {
    .chat-messages {
      padding: 12px;

      .message-left,
      .message-right {
        max-width: 85%;
      }

      .avatar {
        width: 32px;
        height: 32px;
      }

      .message-content {
        max-width: calc(100% - 48px);
        margin: 0 8px;
      }
    }
  }

  .chat-history-container {
    display: flex;
    flex-direction: column;
    height: 500px;
    background: var(--message-list-bg);
  }

  :deep(.el-dialog__title) {
    color: var(--text-color) !important;
  }

  :deep(.el-dialog__headerbtn .el-dialog__close) {
    color: var(--text-color) !important;
  }

  .video-container {
    position: relative;
    display: inline-block;
    cursor: pointer;
  }

  .play-button {
    position: absolute;
    top: 50%;
    left: 50%;
    z-index: 10;
    cursor: pointer;
    transition: opacity 0.3s;
    transform: translate(-50%, -50%);
  }

  .video-container:hover .play-button {
    opacity: 0.8;
  }

  .chat-messages {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 16px;
    padding: 16px;
    overflow-y: auto;

    .loading-more {
      @keyframes spin {
        0% {
          transform: rotate(0deg);
        }

        100% {
          transform: rotate(360deg);
        }
      }

      display: flex;
      align-items: center;
      justify-content: center;
      padding: 16px;
      font-size: 14px;
      color: var(--message-text-color);

      .loading-spinner {
        width: 16px;
        height: 16px;
        margin-right: 8px;
        border: 2px solid #f3f3f3;
        border-top: 2px solid var(--light-text-color);
        border-radius: 50%;
        animation: spin 1s linear infinite;
      }
    }

    .time-divider {
      margin: 16px 0;
      font-size: 12px;
      color: var(--message-text-color);
      text-align: center;
      background: rgb(255 255 255 / 10%);
      border-radius: 16px;
    }

    .message-item {
      display: flex;
      margin-bottom: 16px;

      &.own-message {
        justify-content: flex-end;
      }
    }

    .system-message-item {
      display: flex;
      justify-content: center;
      margin-bottom: 16px;

      .system-content {
        max-width: 80%;
        padding: 8px 16px;
        font-size: 12px;
        color: var(--message-text-color);
        text-align: center;
        background: rgb(255 255 255 / 10%);
        border-radius: 16px;
      }
    }

    .message-left,
    .message-right {
      display: flex;
      align-items: flex-start;
      max-width: 70%;
    }

    .message-right {
      flex-direction: row-reverse;
    }

    .avatar {
      flex-shrink: 0;
      width: 36px;
      height: 36px;
      padding: 2px;
      overflow: hidden;
      border: 1px solid #2e94e5;
      border-radius: 50%;

      .user-avatar {
        width: 100%;
        height: 100%;
        border-radius: 50%;
      }

      .item-icon {
        width: 36px;
        height: 36px;

        svg {
          width: 36px;
          height: 36px;
          color: #2e94e5;
          border-radius: 50%;
        }
      }
    }

    .message-content {
      max-width: calc(100% - 60px);
      margin: 0 12px;

      .quote-message {
        .quote-text {
          padding-bottom: 4px;
          margin-bottom: 8px;
          border-bottom: 1px solid rgb(255 255 255 / 20%);
        }
      }

      .group-joining-message {
        .group-joining-content {
          width: 240px;
          padding: 12px;
          background: var(--message-info-left-bg);
          border-radius: 8px;

          .joining-header {
            margin-bottom: 8px;
            font-size: 14px;
            font-weight: 600;
            color: #1b61f0;
          }

          .joining-text {
            margin-bottom: 8px;
            font-size: 14px;
            font-weight: 500;
            color: var(--text-color);
            word-wrap: break-word;
          }

          .joining-info {
            margin-bottom: 12px;
            font-size: 12px;
            color: var(--text-color);
          }

          .joining-participants {
            .participant-item {
              display: flex;
              align-items: center;
              margin-bottom: 8px;

              &:last-child {
                margin-bottom: 0;
              }

              .participant-index {
                min-width: 20px;
                margin-right: 8px;
                font-size: 12px;
                color: var(--text-color);
              }

              .participant-avatar {
                width: 24px;
                height: 24px;
                margin-right: 8px;
                border-radius: 50%;
              }

              .participant-name {
                font-size: 12px;
                color: var(--text-color);
              }
            }
          }
        }
      }
    }

    .sender-name {
      display: inline-block;
      margin-bottom: 4px;
      margin-left: 8px;
      font-size: 12px;
      color: var(--message-text-color);
    }

    .message-right .sender-name {
      margin-right: 8px;
      text-align: right;
    }

    .message-bubble {
      position: relative;
      padding: 8px 12px;
      word-wrap: break-word;
      background: var(--message-info-left-bg);
      border-radius: 8px;

      &.own-bubble {
        width: fit-content;
        color: white;
        background: var(--message-info-right-bg);
      }
    }

    .text-message {
      font-size: 14px;
      line-height: 1.4;
      word-break: break-word;
      white-space: pre-wrap;
    }

    .text-message-left {
      :deep(.emoji-view) {
        color: #333;
      }
    }

    .message-time {
      display: inline-block;
      margin-top: 4px;
      margin-left: 8px;
      font-size: 12px;
      color: var(--message-text-color);
    }

    .message-right .message-time {
      margin-right: 8px;
      text-align: right;
    }

    .system-message {
      font-size: 12px;
      font-style: italic;
      color: var(--message-text-color);
    }

    .file-message,
    .forward-message,
    .card-message {
      max-width: 300px;
    }
  }

  .chat-messages::-webkit-scrollbar {
    width: 6px;
  }

  .chat-messages::-webkit-scrollbar-track {
    background: var(--td-input-inner-bg);
    border-radius: 3px;
  }

  .chat-messages::-webkit-scrollbar-thumb {
    background: var(--light-text-color);
    border-radius: 3px;
  }

  .chat-messages::-webkit-scrollbar-thumb:hover {
    background: var(--tabs-active-color);
  }

  /* 样式保持不变，与之前相同 */
  .search-container {
    flex: 1;
    // margin-bottom: 5px;

    :deep(.el-input__inner) {
      color: var(--search-text-color);
      background: var(--search-bg);
      border: 1px solid var(--border-color);

      &::placeholder {
        color: var(--tabs-color);
      }
    }
  }

  .time-range {
    flex-shrink: 0;
    margin-left: 10px;

    :deep(.el-input__wrapper) {
      background: var(--background-white-color) !important;
      border: 1px solid var(--border-color) !important;
    }
  }

  // 日期选择器弹窗样式，确保不被屏幕边缘截断
  :deep(.chat-history-date-picker) {
    // 确保弹窗不超出视口左侧
    &[data-popper-placement^='bottom'],
    &[data-popper-placement^='top'] {
      left: max(8px, var(--el-popover-left, 8px)) !important;
    }
  }

  .search {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .member-filter {
    margin-left: 10px;
    width: 150px; // 固定宽度
    flex-shrink: 0; // 防止被压缩
    
    :deep(.el-input__wrapper) {
      background: var(--background-white-color) !important;
      border: 1px solid var(--border-color) !important;
    }
  }
</style>
