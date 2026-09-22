<template>
  <div class="countdown-picker">
    <div class="time-set-btn" @click="openPopup">
      <img :src="isCounting ? activeIcon : defaultIcon" alt="timer" style="width: 20px; height: 20px;" />
      <span :class="{ counting: isCounting }">
        {{ formatRemainTime }}
      </span>
    </div>

    <van-popup round v-model:show="isPopupVisible" position="bottom" get-container="body">
      <div class="time-popup-header">
        <span class="time-popup-title">{{ title }}</span>
      </div>

      <div class="time-popup-content">
        <!-- ✅ loading 状态 -->
        <template v-if="isLoadingOptions">
          <div class="time-option-loading">
            <van-loading size="24px" color="#264ED1" />
          </div>
        </template>

        <template v-else>
          <div
            class="time-option"
            v-for="option in timerOptions"
            :key="option.value"
            @click="handleSelect(option)"
          >
            <div class="time-option-text">{{ option.label }}</div>
            <van-icon v-if="selectedTime === option.value" name="success" class="time-option-check" />
          </div>
        </template>
      </div>

      <div class="time-popup-footer">
        <div class="cancel-btn" @click="isPopupVisible = false">取消</div>
      </div>
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { useCountdown } from "./useCountdown";
import activeIcon from "@/assets/svg/timer_active.svg";
import defaultIcon from "@/assets/svg/timer_default.svg";
const props = defineProps<{
  api: () => Promise<{ label: string; value: number }[]>;
  title?: string;
}>();

const emit = defineEmits<{
  (e: "countdown-end"): void;
}>();

// ── 内部数据 ──────────────────────────────────────────────────────────
const timerOptions = ref<{ label: string; value: number }[]>([]);
const isLoadingOptions = ref(false);

async function fetchOptions() {
  isLoadingOptions.value = true;
  try {
    console.log(props.api)
    timerOptions.value = await props.api();
  } catch (err) {
    console.error("[CountdownPicker] 获取定时选项失败", err);
  } finally {
    isLoadingOptions.value = false;
  }
}

// ── 弹窗 ──────────────────────────────────────────────────────────────
const isPopupVisible = ref(false);

function openPopup() {
  isPopupVisible.value = true;
  fetchOptions();
}

// ── 倒计时 ────────────────────────────────────────────────────────────
const selectedTime = ref(0);
const { isCounting, formatRemainTime, stopCountdown } = useCountdown(selectedTime);

// 是否手动结束定时
const isManualStop = ref(false);

watch(isCounting, (cur, prev) => {
  // isCounting: true → false，即倒计时停止
  if (prev && !cur) {
    if (isManualStop.value) {
      // 手动停止：只重置标志，不触发 countdown-end
      isManualStop.value = false;
    } else {
      // 自然结束：才触发 countdown-end
      emit("countdown-end");
    }
    selectedTime.value = 0;
  }
});

function handleSelect(option: { label: string; value: number }) {
  isPopupVisible.value = false;

  if (option.value === -1) {
    // ✅ 先设置标志，再停止——Vue watch 是异步刷新，标志在回调前已就绪
    isManualStop.value = true;
    stopCountdown();
    // selectedTime 在 watch 回调里统一重置，无需在此重复设置
  } else {
    selectedTime.value = option.value;
  }
}
</script>

<style scoped lang="scss">
.countdown-picker {
  .time-set-btn {
    display: flex;
    align-items: center;
    flex-direction: column;
    padding: 8px;
    padding-left: 8px;
    padding-right: 8px;
    border-radius: 7px;
    background: #fff;
    cursor: pointer;
    box-shadow: 0px 2px 4px 0px rgba(0, 0, 0, 0.1);
    min-width: 52px;
    width: 52px;

    span {
      font-size: 12px;
      font-weight: 400;
      letter-spacing: 0px;
      line-height: 17.38px;
      color: #5B6072;
      margin-top: 4px;
      text-align: center;
      white-space: nowrap;

      &.counting {
        color: #264ED1;
        font-weight: bold;
      }
    }
  }
}

.time-popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #f0f0f0;

  .time-popup-title {
    font-size: 16px;
    font-weight: 600;
    color: #333;
  }

  .time-popup-close {
    font-size: 20px;
    color: #999;
    cursor: pointer;
  }
}

.time-popup-content {
  .time-option {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 16px;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;

    .time-option-text {
      font-size: 14px;
      color: #333;
    }

    .time-option-check {
      font-size: 20px;
      color: #264ED1;
    }
  }
}

.time-popup-footer {
  border-top: 4px solid #f0f0f0;

  .cancel-btn {
    text-align: center;
    font-size: 14px;
    color: #666;
    cursor: pointer;
    padding: 16px;
  }
}

// 在 style 中补充 loading 样式
.time-option-loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 100px 0;  // 撑开高度，避免弹窗太矮
}
</style>
