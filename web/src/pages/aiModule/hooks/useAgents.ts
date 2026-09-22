import { ref, onMounted } from "vue"
import { getAgents } from '@/api/ai';

const PREFIX = '/linkx/desktop/XA-ics-agent';

export const useAgents = () => {

    const agentList = ref<any[]>([]);
    
    const init = async (params = {}) => {
        const { code, data } = await getAgents(params);

        if(code === 0) {
            agentList.value = handleAvatar(data)
        }

        return agentList.value
    }

    // 处理ai头像图片地址
    const handleAvatar = (agents: any[]) => {
       
        return (agents ?? []).map((agent: any) => {
            const url = agent?.picUrl ?? ''
            return {
                ...agent,
                picUrl: url.startsWith(PREFIX) ? url : url ? `${PREFIX}${url}` : '',
            }
        })
    }

    onMounted(async () => {
        await init()
    })
    
    return {
        agentList,
        init,
    }
}