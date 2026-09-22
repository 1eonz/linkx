import { ref, watch, computed, isRef, toRef, onUnmounted } from "vue";

// isRef / toRef 是纯运行时 API，.js 完全支持，去掉所有 TS 类型即可

export function useCountdown(totalTime) {

  // ── 归一化：ref 直接用，普通值包成 ref ──────────────────────
  const totalTimeRef = isRef(totalTime)
    ? totalTime
    : toRef(totalTime);

  // ── 内部状态 ──────────────────────────────────────────────
  const remainTime = ref(0);
  let timer = null;
  const isCounting = computed(() => remainTime.value > 0);

  const formatRemainTime = computed(() => {
    const s = remainTime.value
    if (!s || s <= 0) return '定时'

    // ✅ 纯 JS，去掉所有类型注解
    const pad = (n, len = 2) => String(n).padStart(len, '0')
    const total = totalTime.value
    const totalMin = Math.floor(total / 60)
    const minLen = String(totalMin).length

    if (total >= 3600 * 24) {
      const h = Math.floor(s / 3600)
      const m = Math.floor((s % 3600) / 60)
      const sec = s % 60
      return `${pad(h)}:${pad(m)}:${pad(sec)}`
    }

    const m = Math.floor(s / 60)
    const sec = s % 60
    return `${pad(m, minLen)}:${pad(sec)}`
  })



  function startCountdown(seconds) {
    stopCountdown();
    remainTime.value = seconds;
    timer = setInterval(() => {
      remainTime.value > 0 ? remainTime.value-- : stopCountdown();
    }, 1000);
  }

  function stopCountdown() {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
  remainTime.value = 0; // 重置剩余时间，formatRemainTime 自动回到"定时"
}

  // ── 监听 ──────────────────────────────────────────────────
  watch(totalTimeRef, (val) => {
    val > 0 ? startCountdown(val) : stopCountdown();
  });

  onUnmounted(stopCountdown);

  return { isCounting, formatRemainTime, remainTime, stopCountdown };
}