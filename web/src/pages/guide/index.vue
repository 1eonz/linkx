<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { getGuideList } from '@/api/guide';

  import { throttle } from 'lodash-es';

  import GuideDetail from './components/guideDetail.vue';
  import GuideItem from './components/guideItem.vue';

  const showGuideDetail = ref(false);
  const guideList = ref<any[]>([]);
  const activeGuide = ref<any>({});
  const pageSize = 20;
  let pageNum = 1;
  let total = 0;
  let element: Element; // 用来控制切换tab页的时候 滚动条回到最上方

  onMounted(() => {
    getList();
  });

  // 滚动触发显示更多任务
  const scroll = throttle((e) => {
    if (pageSize * pageNum >= total) {
      return;
    }

    element = e.srcElement;
    const scrollTop = element.scrollTop; // 滚动高度
    const scrollHeight = element.scrollHeight; // 内容高度
    const clientHeight = element.clientHeight; // 可见高度
    if (scrollTop + clientHeight >= scrollHeight) {
      pageNum = pageNum + 1;
      getList();
    }
  }, 500);
  async function getList() {
    const params = {
      pageNum,
      pageSize,
    };
    const res: any = await getGuideList(params);
    if (res?.code === 0) {
      total = res?.data?.total;
      guideList.value = [...guideList.value, ...(res?.data?.records || [])];
    }
  }
  function clickGiude(item: any) {
    activeGuide.value = item;
    showGuideDetail.value = true;
  }

  function closeDialog() {
    showGuideDetail.value = false;
  }
</script>

<template>
  <div class="guide-law">
    <div class="all-list" @scroll="scroll">
      <GuideItem
        v-for="item in guideList"
        :key="item.id"
        :guide-item="item"
        @click="clickGiude(item)"
      />
    </div>
    <GuideDetail v-show="showGuideDetail" :active-guide="activeGuide" @close-dialog="closeDialog" />
  </div>
</template>

<style lang="less" scoped>
  .guide-law {
    display: flex;
    height: 100%;
    margin-top: 10px;

    .all-list {
      height: calc(100% - 10px);
      overflow: auto;
    }
  }
</style>
