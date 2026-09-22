<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue';

// 拖拽位置（距离右侧与底部的像素值）
type Pos = { right: number; bottom: number };

interface Props {
  // v-model 控制显隐
  modelValue: boolean;
  // 标题文本，可通过具名插槽覆盖
  title?: string;
  // 叠放顺序
  zIndex?: number;
  // 与窗口边缘的最小间距
  minMargin?: number;
  // 是否可拖拽
  draggable?: boolean;
  // 是否显示蒙层
  showMask?: boolean;
  // 是否显示头部
  showHeader?: boolean;
  // 是否显示内容
  showContent?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  zIndex: 10000,
  minMargin: 20,
  draggable: true,
  showMask: false,
  showHeader: true,
  showContent: true
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'close'): void;
}>();

const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v),
});

// 弹窗容器引用
const boxRef = ref<HTMLElement | null>(null);
// 当前吸附位置
const pos = ref<Pos>({ right: props.minMargin, bottom: props.minMargin });

// 是否处于拖拽中
const isDragging = ref(false);
// 拖拽起始点与位置
const dragStart = ref({ mouseX: 0, mouseY: 0, startRight: 0, startBottom: 0 });

// 将位置限制在可视区域内
const clampPos = (p: Pos): Pos => {
  const w = boxRef.value?.offsetWidth ?? 360;
  const h = boxRef.value?.offsetHeight ?? 684;
  const maxRight = Math.max(props.minMargin, window.innerWidth - w - props.minMargin);
  const maxBottom = Math.max(props.minMargin, window.innerHeight - h - props.minMargin);
  return {
    right: Math.max(props.minMargin, Math.min(p.right, maxRight)),
    bottom: Math.max(props.minMargin, Math.min(p.bottom, maxBottom)),
  };
};

// 重置到右下角（考虑窗口和最小边距）
const resetToBottomRight = async () => {
  await nextTick();
  pos.value = clampPos({ right: props.minMargin, bottom: props.minMargin });
};

// 拖拽过程中的位置更新
const onMouseMove = (e: MouseEvent) => {
  if (!isDragging.value) return;
  const dx = e.clientX - dragStart.value.mouseX;
  const dy = e.clientY - dragStart.value.mouseY;
  // right/bottom: 往右拖 => right 变小；往下拖 => bottom 变小
  pos.value = clampPos({
    right: dragStart.value.startRight - dx,
    bottom: dragStart.value.startBottom - dy,
  });
};

// 结束拖拽并移除监听
const endDrag = () => {
  if (!isDragging.value) return;
  isDragging.value = false;
  document.removeEventListener('mousemove', onMouseMove, true);
  document.removeEventListener('mouseup', endDrag, true);
};

// 按下头部开始拖拽
const startDrag = (e: MouseEvent) => {
  if (!visible.value || !props.draggable) return;
  isDragging.value = true;
  dragStart.value = {
    mouseX: e.clientX,
    mouseY: e.clientY,
    startRight: pos.value.right,
    startBottom: pos.value.bottom,
  };
  document.addEventListener('mousemove', onMouseMove, true);
  document.addEventListener('mouseup', endDrag, true);
  e.preventDefault();
};

// 关闭弹窗并通知外部
const close = () => {
  visible.value = false;
  emit('close');
};

watch(
  () => visible.value,
  (v) => {
    if (v) resetToBottomRight();
    else endDrag();
  },
);

// 窗口尺寸变化时重新约束位置
const onResize = () => {
  if (!visible.value) return;
  pos.value = clampPos(pos.value);
};

window.addEventListener('resize', onResize);
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize);
  endDrag();
});
</script>

<template>
  <teleport to="body">
    <div
      v-show="visible"
      class="floating-chat-modal"
      :class="{ 'floating-chat-modal--with-mask': showMask }"
      :style="{ zIndex: String(zIndex) }"
    >
      <!-- 蒙层 -->
      <div
        v-if="showMask"
        class="floating-chat-modal__mask"
        @click="close"
      ></div>
      <div
        ref="boxRef"
        class="floating-chat-modal__box"
        :class="{ 'floating-chat-modal__box--show-content': showContent }"
        :style="{
          right: `${pos.right}px`,
          bottom: `${pos.bottom}px`,
        }"
        @mousedown.stop
        @click.stop
      >
        <div
          v-if="showHeader"
          class="floating-chat-modal__header"
          :class="{ 'floating-chat-modal__header--draggable': props.draggable }"
          @mousedown="startDrag"
        >
          <div class="floating-chat-modal__title" :title="title">
            <!-- 支持外部自定义标题内容，未提供时回退到 title 文本 -->
            <slot name="title">
              {{ title }}
            </slot>
          </div>
          <button class="floating-chat-modal__close" type="button" @click="close">×</button>
        </div>
        <div class="floating-chat-modal__body">
          <slot />
        </div>
      </div>
    </div>
  </teleport>
</template>

<style scoped lang="less">
.floating-chat-modal {
  position: fixed;
  inset: 0;
  // 默认不挡住页面点击
  pointer-events: none;
  transition: background-color 0.2s ease, opacity 0.2s ease;
}

.floating-chat-modal--with-mask {
  // 显示蒙层时，阻止点击穿透
  pointer-events: auto;
}

.floating-chat-modal__mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45); // 添加不透明度
  pointer-events: auto; // 蒙层可点击
  z-index: 1;
}

.floating-chat-modal__box {
  position: fixed;
  pointer-events: auto; // 只有弹窗本体可交互
  width: 360px;
  height: 684px;
  max-width: calc(100vw - 40px);
  max-height: calc(100vh - 40px);
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  z-index: 2; // 确保弹窗在蒙层之上
}
.floating-chat-modal__box--show-content{
  background: var(--background-white-color, #fff);
  border: 1px solid var(--el-card-border, #e4e7ed);
  box-shadow: 0 8px 24px rgb(0 0 0 / 14%);
}

.floating-chat-modal__header {
  flex: 0 0 auto;
  width: 100%;
  height: 56px;
  padding: 14.5px 16px;
  user-select: none;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--el-card-border, #e4e7ed);
}

.floating-chat-modal__header--draggable {
  cursor: move;
}

.floating-chat-modal__title {
  flex: 1;
  min-width: 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--text-color, #303133);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.floating-chat-modal__close {
  flex: 0 0 auto;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 20px;
  line-height: 28px;
  color: var(--text-color, #303133);
}

.floating-chat-modal__body {
  flex: 1;
//   padding: 10px 12px 12px 12px;
}
</style>


