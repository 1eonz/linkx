<template>
  <div class="title-container">
    <div class="scroll-arrow left" @click="scrollLeftCards" v-show="showArrows">
      <el-icon><ArrowLeft /></el-icon>
    </div>

    <div
      class="title-container-inner"
      ref="titleContainerRef"
      @mousedown="onMouseDown"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
      @mouseleave="onMouseLeave"
      :class="{ 'with-padding': showArrows }"
    >
      <template v-for="(item, index) in titles" :key="index">
       <template v-if="item.name === '全部'">
        <div
          v-if="isArchived"
          class="title-item"
          :class="{ active: activeIndex === index }"
          @click="handleAllGroupTypeChange(item.id, index)"
        >
          {{ item.name }}
        </div>

         <AllGroupTypeDropdown
          v-else
          v-model="allGroupType"
          class="title-item"
          :class="{ active: activeIndex === index }"
          @change="(type) => handleAllGroupTypeChange(type, index)"
        />
       </template>
        <div
          v-else
          class="title-item"
          :class="{ active: activeIndex === index }"
          @click="switchTitle(item, index)"
        >
          {{ item.name }}
        </div>
      </template>
    </div>

    <div class="scroll-arrow right" @click="scrollRightCards" v-show="showArrows">
      <el-icon><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref, onMounted, computed } from 'vue';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';
import AllGroupTypeDropdown from './AllGroupTypeDropdown.vue';
import { getCollaborationTagListAll } from '@/api/statics';
import { usePIMStore } from '@/store';

const props = defineProps({
  isArchived: {
    type: Boolean,
    default: false
  }
})

// // 默认的标题列表数据
// const defaultTitles = [
//   { id: '', name: '全部' },
//   {
//     "id": "1",
//     "name": "协同",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-tools",
//     "color": "rgba(254,126,44,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "2",
//     "name": "刑事",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-shield-alt",
//     "color": "rgba(255,150,45,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "3",
//     "name": "治安",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-user-shield",
//     "color": "rgba(78,128,255,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "4",
//     "name": "交通",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-bus-alt",
//     "color": "rgba(34,176,224,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "6",
//     "name": "社会联动",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-users",
//     "color": "rgba(93,212,156,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "7",
//     "name": "群体事件",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-user-friends",
//     "color": "rgba(100,179,255,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "1960581781146632194",
//     "name": "刑事案件",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-shield-alt",
//     "color": "rgba(76,135,250,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "1960584228741120001",
//     "name": "治安案件",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-user-shield",
//     "color": "rgba(61,203,241,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "1960584296554627074",
//     "name": "交通事故",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-car-crash",
//     "color": "rgba(22,192,151,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "1960584465035624450",
//     "name": "群众求助",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-users-cog",
//     "color": "rgba(229,182,109,0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "1960584553187311618",
//     "name": "人员核查",
//     "parentId": "0",
//     "level": 1,
//     "icon": "far fa-id-badge",
//     "color": "rgba(250,122,125,0.8)",
//     "type": 1,
//     "isDeleted": 0,
//     "scope": 1,
//     "gmtCreated": "2026-06-12 15:34:51",
//     "gmtModified": "2026-06-12 15:34:51",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "2065404069970644994",
//     "name": "1222",
//     "parentId": "0",
//     "level": 1,
//     "icon": "fas fa-home",
//     "color": "rgba(64, 158, 255, 0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 2,
//     "gmtCreated": "2026-06-12 20:01:18",
//     "gmtModified": "2026-06-12 20:01:18",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   },
//   {
//     "id": "2065404120126132225",
//     "name": "233343",
//     "parentId": "2065404069970644994",
//     "level": 2,
//     "icon": "fas fa-home",
//     "color": "rgba(64, 158, 255, 0.8)",
//     "type": 0,
//     "isDeleted": 0,
//     "scope": 2,
//     "gmtCreated": "2026-06-12 20:01:30",
//     "gmtModified": "2026-06-12 20:01:30",
//     "isCancel": null,
//     "isAssociatedCoop": null
//   }
// ];

const emit = defineEmits(['switch-title', 'all-group-type-change']);

// 获取用户信息
const PIMStore = usePIMStore();
const userInfo = computed(() => PIMStore.user);

const titles = ref([]);
const activeIndex = ref(0);
const titleContainerRef = ref(null);
const showArrows = ref(false);
const allGroupType = ref('');
const isDragging = ref(false);
const startX = ref(0);
const scrollLeft = ref(0);

// 获取标签列表
const fetchTagList = async () => {
  const userId = userInfo.value?.userid;
  if (!userId) {
    titles.value = [...defaultTitles];
    nextTick(() => {
      checkArrowsVisibility();
    });
    return;
  }

  try {
    const res = await getCollaborationTagListAll({
      userId,
    });
    if (res && res.data) {
      titles.value = [
        { name: '全部', id: '' },
        ...res.data,
      ];
      // 在DOM更新后初始化
      nextTick(() => {
        checkArrowsVisibility();
      });
    }
  } catch (error) {
    console.error('获取标签列表失败:', error);
    // 失败时使用默认数据
    titles.value = [...defaultTitles];
  }
};

const checkArrowsVisibility = () => {
  if (!titleContainerRef.value) return;
  const container = titleContainerRef.value;
  showArrows.value = container.scrollWidth > container.clientWidth;
};

const onMouseDown = (e) => {
  isDragging.value = true;
  startX.value = e.pageX - titleContainerRef.value.offsetLeft;
  scrollLeft.value = titleContainerRef.value.scrollLeft;
  titleContainerRef.value.style.cursor = 'grabbing';
  titleContainerRef.value.style.userSelect = 'none';
};

const onMouseMove = (e) => {
  if (!isDragging.value) return;
  e.preventDefault();
  const x = e.pageX - titleContainerRef.value.offsetLeft;
  const walk = (x - startX.value) * 2;
  titleContainerRef.value.scrollLeft = scrollLeft.value - walk;
};

const onMouseUp = () => {
  isDragging.value = false;
  if (titleContainerRef.value) {
    titleContainerRef.value.style.cursor = 'grab';
    titleContainerRef.value.style.removeProperty('user-select');
  }
};

const onMouseLeave = () => {
  isDragging.value = false;
  if (titleContainerRef.value) {
    titleContainerRef.value.style.cursor = 'grab';
    titleContainerRef.value.style.removeProperty('user-select');
  }
};

const switchTitle = (item, index) => {
  activeIndex.value = index;
  emit('switch-title', { item, index, allGroupType: allGroupType.value });
};

const handleAllGroupTypeChange = (type, index) => {
  activeIndex.value = index;
  allGroupType.value = type;
  emit('all-group-type-change', { type, index });
};

const scrollLeftCards = () => {
  if (titleContainerRef.value) {
    titleContainerRef.value.scrollBy({ behavior: 'smooth', left: -300 });
  }
};

const scrollRightCards = () => {
  if (titleContainerRef.value) {
    titleContainerRef.value.scrollBy({ behavior: 'smooth', left: 300 });
  }
};

const setActiveIndex = (index) => {
  activeIndex.value = index;
};

const init = () => {
  nextTick(() => {
    checkArrowsVisibility();
  });
};

const refresh = async () => {
  await fetchTagList();
  // 重置活动索引
  activeIndex.value = 0;
};

// 组件挂载时自动初始化
onMounted(() => {
  fetchTagList();
  nextTick(() => {
    checkArrowsVisibility();
  });
});

defineExpose({
  init,
  setActiveIndex,
  refresh,
});
</script>

<style lang="less" scoped>
.title-container {
  position: relative;
  width: 100%;

  .title-container-inner {
    box-sizing: border-box;
    display: flex;
    flex-wrap: nowrap;
    gap: 12px;
    justify-content: flex-start;
    width: 100%;
    padding: 10px 0;
    overflow-x: auto;
    cursor: grab;
    scrollbar-width: none;
    -ms-overflow-style: none;

    &.with-padding {
      padding: 10px 30px;
    }

    &::-webkit-scrollbar {
      display: none;
    }

    .title-item {
      flex-shrink: 0;
      padding: 3px 16px;
      font-size: 14px;
      color: var(--tabs-color2);
      cursor: pointer;
      user-select: none;
      background-color: var(--tabs-bg);
      border-radius: 16px;
      transition: all 0.3s;

      &:hover {
        color: var(--tabs-active-color2);
        background-color: var(--tabs-active-bg);
      }

      &.active {
        color: var(--tabs-active-color2);
        background-color: var(--tabs-active-bg);
      }
    }

    &:active {
      cursor: grabbing;
    }
  }

  .scroll-arrow {
    position: absolute;
    z-index: 10;
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 25px;
    height: 25px;
    color: rgb(134 139 152 / 100%);
    cursor: pointer;
    background-color: #fff;
    border: 1px solid #dcdfe6;
    border-radius: 50%;
    box-shadow: 0 2px 4px rgb(0 0 0 / 10%);
    transition: all 0.3s;

    &.left {
      top: 10px;
      left: 0;
    }

    &.right {
      top: 10px;
      right: 0;
    }

    &:hover {
      color: #254dd1;
      background-color: #f2f7ff;
      border-color: #254dd1;
    }

    .el-icon {
      font-size: 16px;
    }
  }
}
</style>
