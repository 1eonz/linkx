import { computed } from "vue"
import { usePIMStore } from '@/store/modules/pim';

export const useLocaAgentId = () => {

    const pimStore = usePIMStore();
    const userId = computed(() => pimStore.user?.userid || '');

    const key = computed(() => `activeModuleId-${userId.value}`);

    const getLocaAgentId: ()=> string = () => {
        return localStorage.getItem(key.value) ?? '';
    }

    const setLocaAgentId = (agentId: string | number) => {
        if(userId.value) localStorage.setItem(key.value, String(agentId || ''));
    }

    const removeLocaAgentId = () => {
         if(userId.value) localStorage.removeItem(key.value)
    }

    return {
        getLocaAgentId,
        setLocaAgentId,
        removeLocaAgentId
    }
}