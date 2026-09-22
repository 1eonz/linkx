// composables/useLongPressCopy.js
import { ref } from 'vue';
import { copyText } from '@/utils/copyText';

export function useLongPressCopy(showToast, bubbleRef, delay = 500) {
  const pressTimer = ref(null);
  const showMenu = ref(false);
  const menuTop = ref(0);
  const menuLeft = ref(0);
  const startX = ref(0);
  const startY = ref(0);
  const isMoving = ref(false);
  const pendingText = ref('');

  const handleTouchStart = (e, text) => {
    pendingText.value = text;
    startX.value = e.touches[0].clientX;
    startY.value = e.touches[0].clientY;
    isMoving.value = false;

    pressTimer.value = setTimeout(() => {
      if (!isMoving.value) {
        showMenu.value = true;
        
        // 计算相对位置
        if (bubbleRef && bubbleRef.value) {
          const rect = bubbleRef.value.getBoundingClientRect ? bubbleRef.value.getBoundingClientRect() : { left: 0, top: 0 };
          menuTop.value = e.touches[0].clientY - rect.top - 10;
          menuLeft.value = e.touches[0].clientX - rect.left - 30;
        } else {
          // 降级方案：使用视口坐标
          menuTop.value = e.touches[0].clientY - 10;
          menuLeft.value = e.touches[0].clientX - 30;
        }
      }
    }, delay);
  };

  const handleTouchEnd = () => {
    clearTimeout(pressTimer.value);
  };

  const handleTouchMove = (e) => {
    const moveX = Math.abs(e.touches[0].clientX - startX.value);
    const moveY = Math.abs(e.touches[0].clientY - startY.value);
    if (moveX > 5 || moveY > 5) {
      isMoving.value = true;
      showMenu.value = false;
      clearTimeout(pressTimer.value);
    }
  };

  const handleAction = () => {
    showMenu.value = false;
    if (pendingText.value) {
      copyText(pendingText.value, { showToast });
    }
  };

  const onMenuClick = (e) => {
    e.stopPropagation();
  };

  const closeMenu = () => {
    showMenu.value = false;
  };

  return {
    showMenu,
    menuTop,
    menuLeft,
    handleTouchStart,
    handleTouchEnd,
    handleTouchMove,
    handleAction,
    onMenuClick,
    closeMenu,
  };
}