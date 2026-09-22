import { defineStore } from 'pinia';

interface AiModuleState {
  streamingTaskId: string | null;
}

export const useAiModuleStore = defineStore({
  id: 'aiModule',
  state: (): AiModuleState => ({
    streamingTaskId: null,
  }),
  actions: {
    setStreamingTaskId(taskId: string | null) {
      this.streamingTaskId = taskId;
    },
    clearStreamingTaskId() {
      this.streamingTaskId = null;
    },
  },
  getters: {
    isStreamingTaskId: (state) => (taskId: string) => {
      return state.streamingTaskId === taskId;
    },
  },
});
