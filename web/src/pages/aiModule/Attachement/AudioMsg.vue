<template>
    <div class="audio-msg">
        <div class="audio-content">
            <div class="audio-icon" @click="togglePlay">
                <el-icon :size="28" v-if="isLoading" class="loading-icon">
                    <Loading />
                </el-icon>
                <el-icon :size="28" v-else-if="!isPlaying"><VideoPlay /></el-icon>
                <el-icon :size="28" v-else><VideoPause /></el-icon>
            </div>
            <div class="audio-info">
                <div class="audio-progress" @click="handleProgressClick">
                    <div class="progress-bar" :style="{ width: `${progressPercent}%` }"></div>
                </div>
                <div class="audio-time">
                    <span>{{ formatTime(currentTime) }}</span>
                    <span>/</span>
                    <span>{{ formatTime(duration) }}</span>
                </div>
            </div>
        </div>
        <audio 
            ref="audioRef" 
            :src="props.fileUrl" 
            @timeupdate="handleTimeUpdate"
            @loadedmetadata="handleLoadedMetadata"
            @ended="handleEnded"
            @error="handleError"
            @play="handlePlay"
            @pause="handlePause"
            @waiting="handleWaiting"
            @canplay="handleCanPlay"
            @seeking="handleSeeking"
            @seeked="handleSeeked"
        />
    </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { VideoPlay, VideoPause, Loading } from '@element-plus/icons-vue'

const props = defineProps({
    fileUrl: {
        type: String,
        default: ''
    }
})

const audioRef = ref<HTMLAudioElement | null>(null)
const isPlaying = ref(false)
const isLoading = ref(false)
const currentTime = ref(0)
const duration = ref(0)
const progressPercent = ref(0)

// 播放/暂停
const togglePlay = async () => {
    if (!audioRef.value) return
    try {
        if(isLoading.value) {
            audioRef.value.pause()
            isLoading.value = false
        }else if (isPlaying.value) {
            audioRef.value.pause()
        } else {
            await audioRef.value.play()
        }
    } catch (e) {
        console.warn('播放控制异常:', e)
    }
}

// 播放事件
const handlePlay = () => {
    isPlaying.value = true
}

// 暂停事件
const handlePause = () => {
    isPlaying.value = false
}

// 缓冲开始
const handleWaiting = () => {
    isLoading.value = true
}

// 可以播放
const handleCanPlay = () => {
    isLoading.value = false
}

// 跳转中
const handleSeeking = () => {
    isLoading.value = true
}

// 跳转完成
const handleSeeked = () => {
    isLoading.value = false
}

// 时间更新
const handleTimeUpdate = () => {
    if (audioRef.value) {
        currentTime.value = audioRef.value.currentTime
        progressPercent.value = (currentTime.value / duration.value) * 100
    }
}

// 元数据加载完成
const handleLoadedMetadata = () => {
    if (audioRef.value) {
        duration.value = audioRef.value.duration
    }
}

// 播放结束
const handleEnded = () => {
    isPlaying.value = false
    isLoading.value = false
    currentTime.value = 0
    progressPercent.value = 0
}

// 错误处理
const handleError = (e: any) => {
    console.error('音频加载失败:', e)
    isPlaying.value = false
    isLoading.value = false
}

// 进度条点击
const handleProgressClick = async (e: MouseEvent) => {
    if (!audioRef.value) return
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect()
    const percent = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width))
    const newTime = percent * duration.value
    currentTime.value = newTime
    progressPercent.value = percent * 100
    audioRef.value.currentTime = newTime
    
    // 如果不是播放状态，从新的进度开始播放
    if (!isPlaying.value) {
        try {
            await audioRef.value.play()
        } catch (e) {
            console.warn('自动播放失败:', e)
        }
    }
}

// 格式化时间
const formatTime = (seconds: number): string => {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}
</script>

<style lang="less" scoped>
.audio-msg {
    width: 300px;
    background: #f0f2f5;
    border-radius: 16px;
    padding: 12px 16px;
    margin-bottom: 8px;
}

.audio-content {
    display: flex;
    align-items: center;
    gap: 12px;
}

.audio-icon {
    width: 44px;
    height: 44px;
    background: #409eff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
    cursor: pointer;
    transition: all 0.2s;

    &:hover {
        background: #66b1ff;
        transform: scale(1.05);
    }

    .loading-icon {
        animation: rotate 1s linear infinite;
    }
}

@keyframes rotate {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
    }
}

.audio-info {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.audio-progress {
    width: 100%;
    height: 6px;
    background: #e0e0e0;
    border-radius: 3px;
    cursor: pointer;
    position: relative;
    overflow: hidden;

    .progress-bar {
        height: 100%;
        background: #409eff;
        border-radius: 3px;
        transition: width 0.1s;
    }
}

.audio-time {
    display: flex;
    gap: 4px;
    font-size: 12px;
    color: #666;
}
</style>
