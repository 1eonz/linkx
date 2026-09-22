import { defineStore } from 'pinia';

import { aiApi } from '@/common/api/index.js';
import { getGlobalsConfigByKey } from '@/common/utils';
import { getFullPageUrl } from '@/utils';

export const useAiStore = defineStore('aiAssistant', {
    state: () => ({
        agentsList: [], //智能体列表
        functionsList: [], // 智能体按钮列表
        currentAgentIndex: null, // 当前智能体id
        userid: null,
        streamingTaskId: null, // 正在流式的任务id
    }),
    getters: {
        isStreamingTaskId: (state) => (taskId) => {
            return state.streamingTaskId === taskId;
        },
    },
    actions: {
        setStreamingTaskId(taskId) {
            this.streamingTaskId = taskId;
        },
        clearStreamingTaskId() {
            this.streamingTaskId = null;
        },
        async getUserInfo() {
            try {
                const res = await window.WeSpaceSDK.getUserInfo();
                if (res) {
                    this.userid = res.userid;
                    console.log("打印用户信息", JSON.stringify(res, null, 2));
                } else {
                    console.error('获取用户信息失败或WeSpaceSDK不可用');
                }
            } catch (error) {
                console.error(`获取用户信息错误: ${error.message}`);
            }
        },

        // 查询智能体列表
        async getAgentsList(params) {
            try {
                await this.getUserInfo();

                // 保留常用智能体状态
                const existingAgentsMap = new Map();
                this.agentsList = await this.cachedFunctionsList();
                this.agentsList.forEach((agent) => {
                    existingAgentsMap.set(agent.index, {
                        selectedTime: agent.selectedTime,
                    });
                });

                // 获取最新数据
                const res = await aiApi.getAgents(params || {});
                let aiBaseUrl = getFullPageUrl();

                // 合并数据，最新数据及常用智能体状态
                this.agentsList = res
                    .map((item) => {
                        const existingAgent = existingAgentsMap.get(item.index);
                        return {
                            ...item,
                            // 优先使用缓存的值，如果没有缓存则使用原始数据中的值，最后才使用0
                            selectedTime: existingAgent?.selectedTime ?? item.selectedTime ?? 0,
                            picUrl: `${aiBaseUrl}/XA-ics-agent${item.picUrl}`,
                        };
                    })
                    .sort((a, b) => a.priority - b.priority);

                console.log('合并处理后的agentsList-----', JSON.stringify(this.agentsList, null, 2));

                return this.agentsList;
            } catch (e) {
                console.log('获取智能体列表失败:', JSON.stringify(e, null, 2));
                return [];
                throw e;
            }
        },

        async getAllTabs() {
            try {
                const res = await aiApi.getAllTabs();
                return res;
            } catch (e) {
                console.log('获取全部智能体分类列表失败:', JSON.stringify(e, null, 2));
                return [];
            }
        },

        // 更新智能体列表
        async changeAgentsList(arr, currentAgent) {
            if (currentAgent) {
                for (let item of arr) {
                    if (item.index === currentAgent.index) {
                        item.selectedTime = Date.now();
                        break;
                    }
                }
            }
            this.getCurrentFunc(arr);
        },

        // 获取常用智能体
        async getCurrentFunc(arr) {
            const sortedAgents = arr
                .filter(
                    (item) =>
                        item.selectedTime != null && item.selectedTime !== '' && item.selectedTime !== 0,
                )
                .sort((a, b) => b.selectedTime - a.selectedTime);
            console.log('sortedAgents------------', JSON.stringify(sortedAgents, null, 2));

            this.functionsList = sortedAgents.slice(0, 3);
            console.log('this.functionsList------------', JSON.stringify(this.functionsList, null, 2));

            // 存储
            await window.WeSpaceSDK.setStorage(
                `functionsList_${this.userid}`,
                unescape(encodeURIComponent(JSON.stringify(this.functionsList))),
            );
            return this.functionsList;
        },

        // 当前智能体
        async setCurrentAgent(data) {
            this.currentAgentIndex = data;
            // 存储
            await window.WeSpaceSDK.setStorage(
                `currentAgentIndex_${this.userid}`,
                unescape(encodeURIComponent(JSON.stringify(this.currentAgentIndex))),
            );
        },

        // 获取缓存中的functionsList
        async cachedFunctionsList() {
            try {
                const data = await window.WeSpaceSDK.getStorage(`functionsList_${this.userid}`);
                console.log(
                    '获取缓存的functionsList:',
                    JSON.stringify(JSON.parse(decodeURIComponent(escape(data))), null, 2),
                );
                return data ? JSON.parse(decodeURIComponent(escape(data))) : [];
            } catch (error) {
                console.log('获取缓存的functionsList失败:', error);
                return [];
            }
        },

        // 获取缓存中的currentAgentIndex
        async cachedCurrentAgentIndex() {
            const agents = await this.getAgentsList();
            const targetItem = agents.find((item) => item.name?.toLowerCase() === 'deepseek');
            try {
                const data = await window.WeSpaceSDK.getStorage(`currentAgentIndex_${this.userid}`);
                console.log(
                    '获取缓存的currentAgentIndex:',
                    JSON.parse(decodeURIComponent(escape(data))),
                    targetItem?.index,
                );
                return data ? JSON.parse(decodeURIComponent(escape(data))) : targetItem?.index;
            } catch (error) {
                return targetItem?.index;
                console.log('获取缓存的currentAgentIndex失败:', error, targetItem?.index);
            }
        },

        // 获取缓存中的aiBaseUrl
        async cachedAiBaseUrl() {
            let aiBaseUrl = '';
            try {
                const originData = await window.WeSpaceSDK.getStorage('aiBaseUrl');
                aiBaseUrl = JSON.parse(decodeURIComponent(escape(originData)));
                return aiBaseUrl;
            } catch (error) {
                console.error('解码失败', error);
                aiBaseUrl = (await getGlobalsConfigByKey('ai'))?.replace(/\/$/, '');
                await window.WeSpaceSDK.setStorage(
                    'aiBaseUrl',
                    unescape(encodeURIComponent(JSON.stringify(aiBaseUrl))),
                );
                return aiBaseUrl;
            }
        },
    },
});
