<script setup lang="ts">
  import { onMounted, ref, unref, watch } from 'vue';

  import { getStaticCounts, listZeroOnDutyPosts } from '@/api/statics';
  import icon1 from '@/assets/images/pim/static-icon1.svg';
  import icon2 from '@/assets/images/pim/static-icon2.svg';
  import icon3 from '@/assets/images/pim/static-icon3.svg';
  import icon4 from '@/assets/images/pim/static-icon4.svg';
  import icon5 from '@/assets/images/pim/static-icon5.svg';
  import { useEmitter } from '@/hooks';
  import { useStaticsState } from '@/store';

  import SectionHeader from './SectionHeader.vue';
  import SimpleTable from './SimpleTable.vue';
  import StatCardItem from './StatCardItem.vue';

  interface TableColumn {
    align?: 'center' | 'left' | 'right'; // 明确定义为字面量类型
    label: string;
    prop: string;
    width?: number | string;
  }

  const zeroColumns: TableColumn[] = [
    { align: 'center', label: '名称', prop: 'postName', width: '80' },
    { align: 'center', label: '归属组织', prop: 'orgName', width: '120' },
    { align: 'center', label: '在线人数', prop: 'lastPeopleNum', width: '100' },
    { align: 'center', label: '下岗时间', prop: 'createTime', width: '100' },
  ];

  const staticArr = ref<any>([
    { color: '#409EFF', countName: 'total', title: '总数', url: icon1, value: 0 },
    { color: '#67C23A', countName: 'userTotal', title: '关联人员总数', url: icon2, value: 0 },
    {
      color: '#E6A23C',
      countName: 'userOnlineTotal',
      title: '关联人员在岗总数',
      url: icon3,
      value: 0,
    },
    {
      color: '#F56C6C',
      countName: 'userOnlineRatio',
      title: '关联人员在岗比例',
      url: icon4,
      value: '0',
    },
    {
      color: '#9C27B0',
      countName: 'zeroUserOnlineTotal',
      specialStyle: true,
      title: '无人员在岗协同岗个数',
      url: icon5,
      value: 0,
    },
  ]);
  const StaticsStore = useStaticsState();
  const zeroPersonnelList = ref<any>([]);

  const currentPage = ref(1);
  const pageSize = ref(10);
  const total = ref(0);

  watch(
    [() => StaticsStore.peerId, () => StaticsStore.departmentCode, () => StaticsStore.dateRang],
    ([peerId, code, date]) => {
      init(peerId, code, date);
    },
    { deep: true, immediate: true },
  );

  onMounted(() => {
    // 监听刷新事件
    useEmitter('refreshAllData', () => {
      init(StaticsStore.peerId, StaticsStore.departmentCode, StaticsStore.dateRang);
    });
  });
  async function init(peerId, departmentCode, date) {
    if (!departmentCode || !date.endTime) return;
    const params = { peerId, departmentCode, ...date };
    const { code, data } = await getStaticCounts(params);
    if (code === 0 && data) {
      const arr: any = [...unref(staticArr.value)];
      arr.map((item) => {
        if (item.countName === 'zeroUserOnlineTotal') {
          StaticsStore.setZeroUserOnlineTotal(Number(item.value));
          return item;
        }
        item.value = data[item.countName] || 0;
        if (item.countName === 'userOnlineRatio') {
          item.value += '%';
        }
        return item;
      });
      staticArr.value = [...arr];
    }
    listZeroOnDutyPostsFunc(params);
  }

  async function listZeroOnDutyPostsFunc(params) {
    const res = await listZeroOnDutyPosts({
      ...params,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    });
    // 响应结构为 { code, msg, data: { records, total } }，records/total 在 data 层级
    const data = (res as any)?.data;
    if (res && res.code === 0 && data) {
      const arr: any = [...unref(staticArr.value)];
      arr[arr.length - 1].value = data.total;
      total.value = Number(data.total) || 0; // 确保是数字类型
      arr.forEach((item) => {
        if (item.countName === 'zeroUserOnlineTotal') {
          StaticsStore.setZeroUserOnlineTotal(Number(item.value));
        }
      });
      staticArr.value = arr;
      zeroPersonnelList.value = Array.isArray(data.records)
        ? data.records.map((item) => ({
            ...item,
            createTime: item.createTime ? item.createTime : '—',
          }))
        : [];
    }
  }

  // 处理分页变化
  const handlePageChange = (page: number, size: number) => {
    currentPage.value = page;
    pageSize.value = size;
    const params = {
      peerId: StaticsStore.peerId,
      departmentCode: StaticsStore.departmentCode,
      ...StaticsStore.dateRang,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
    };
    listZeroOnDutyPostsFunc(params);
  };
</script>

<template>
  <div class="stat-cards-container">
    <!-- 标题区域 -->
    <div class="title-container">
      <SectionHeader title="协同岗统计" />
    </div>
    <!-- 卡片区域 -->
    <div class="cards-container">
      <el-card
        v-for="(item, index) in staticArr.slice(0, staticArr.length - 1)"
        :key="index"
        class="stat-card"
        shadow="hover"
      >
        <StatCardItem
          :color="item.color || '#409EFF'"
          :special-style="false"
          :title="item.title"
          type="StatCards"
          :url="item.url"
          :value="item.value"
        />
      </el-card>

      <!-- 气泡弹出框内显示列表 -->

      <el-popover placement="bottom" trigger="click" width="600">
        <template #reference>
          <el-card
            v-for="item in staticArr.slice(-1)"
            :key="item.countName"
            class="stat-card other-stat-card"
            shadow="hover"
          >
            <StatCardItem
              :color="item.color || '#409EFF'"
              :special-style="item.specialStyle"
              :title="item.title"
              type="StatCards"
              :url="item.url"
              :value="item.value"
            />
          </el-card>
        </template>
        <SectionHeader style="margin: 10px 0" title="无人员在岗协同岗个数" />
        <SimpleTable
          :columns="zeroColumns"
          :current-page="currentPage"
          height="30vh"
          :page-size="pageSize"
          :show-pagination="true"
          :table-data="zeroPersonnelList"
          :total="Number(total)"
          @page-change="handlePageChange"
        />
      </el-popover>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .stat-cards-container {
    box-sizing: border-box;
    width: 100%;
    padding-bottom: 20px;
    // margin-bottom: 15px;
    background: var(--background-white-color);
    border: 1px solid var(--border-color);
    border-radius: 6px;

    .title-container {
      padding: 15px 0 15px 15px;
    }
  }

  :deep(.el-popper) {
    padding: 10px !important;
  }

  :deep(.el-card__body) {
    padding: 10px;
  }

  /* 卡片容器 */
  .cards-container {
    box-sizing: border-box;
    display: flex;
    gap: 16px;
    // flex-wrap: wrap;
    // flex-wrap: wrap;
    justify-content: space-between;
    width: 100%;
    padding: 0 16px;
  }

  :deep(.el-card) {
    background-color: var(--background-white-color);
    border: 1px solid var(--el-card-border);
  }

  /* 卡片样式 */
  .stat-card {
    position: relative; /* 关键修复点 */
    // padding-left: 4%;
    box-sizing: border-box;
    width: 15%;
    // flex: 1 1 auto;
    transition: all 0.3s ease;

    // &:hover {
    //   box-shadow: 0 4px 12px rgb(0 0 0 / 10%);
    //   transform: translateY(-5px);
    // }

    &:first-child {
      padding-left: 0;
    }
  }

  .stat-card:not(:last-child)::after {
    position: absolute;
    top: 20%;
    right: 0;
    z-index: 1; /* 确保在卡片内容之上 */
    width: 2px;
    height: 60%;
    pointer-events: none;
    content: '';
    background-color: var(--card-right-border);
  }

  :deep(.el-card) {
    box-shadow: none !important;

    &:is-hover-shadow:hover {
      box-shadow: none !important;
    }
  }

  .other-stat-card {
    cursor: pointer;
  }
</style>
