<script setup lang="ts">
  import { ref, watch } from 'vue';
  import { useArchiveStore } from '@/store/modules/archiveStore';

  import ArchiveTable from './ArchiveTable.vue';
  import ArchivedTable from './ArchivedTable.vue';
  import SectionHeader from './SectionHeader.vue';
  // const hasQzgdAuth = inject('hasQzgdAuth'); //是否群组归档权限
  const hasQzgdAuth = true; //是否群组归档权限
  const archiveStore = useArchiveStore();
  const archiveUpdateKey = ref(0);
  const archivedUpdateKey = ref(0);

  const props = defineProps({
    refreshTagStatistics: {
      type: Number,
      default: 0,
    },
  });

  const handleArchiveSuccess = () => {
    archiveUpdateKey.value += 1;
  };

  // 监听收藏操作后的刷新
  watch(
    () => archiveStore.refreshCount,
    () => {
      handleRefreshList();
    },
  );

  // 监听关联任务操作后的刷新
  watch(
    () => archiveStore.refreshRelatedCount,
    () => {
      handleRefreshList();
    },
  );

  watch(
    () => archiveStore.refreshArchivedCount,
    () => {
      handleArchivedRefreshList();
    },
  );

  // 监听标签统计变化，刷新未归档和已归档列表（替代原 :key 销毁重建方案，避免子组件状态丢失）
  watch(
    () => props.refreshTagStatistics,
    () => {
      handleRefreshList();
      handleArchivedRefreshList();
    },
  );

  const handleRefreshList = () => {
    archivedUpdateKey.value += 1;
  };

  const handleArchivedRefreshList = () => {
    handleArchiveSuccess();
  };
</script>

<template>
  <div class="table-area">
    <el-row :gutter="hasQzgdAuth ? 20 : 0" style="height: 100%">
      <!-- 左侧模块 -->
      <el-col :md="hasQzgdAuth ? 12 : 24" :sm="24" style="height: 100%; padding-right: 0" :xs="24">
        <el-card class="table-card" shadow="hover">
          <div class="table-title">
            <SectionHeader :title="hasQzgdAuth ? '未归档' : '群组列表'" :extra-title="hasQzgdAuth ? '' : '（无权限只能展示未归档群组）'" />
          </div>
          <div class="table-content">
            <ArchiveTable :update-key="archivedUpdateKey" @archive-success="handleArchiveSuccess" />
          </div>
        </el-card>
      </el-col>
      <!-- 右侧模块 -->
      <el-col v-if="hasQzgdAuth" :md="12" :sm="24" style="height: 100%" :xs="24">
        <el-card class="table-card table-card1" shadow="hover">
          <div class="table-title">
            <SectionHeader title="已归档" />
          </div>
          <div class="table-content">
            <ArchivedTable :update-key="archiveUpdateKey" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style lang="less" scoped>
  .table-area {
    height: calc(100% - 106px);

    .group-card {
      margin-bottom: 20px;

      .sub-card {
        padding: 15px 0;
        background: #fff;
        border-radius: 4px;
        //   height: 100%;
      }
    }

    .table-card,
    .sub-card {
      height: 100%;
      background: var(--background-color-white);
      border: 1px solid var(--border-color);

      .table-title {
        padding: 15px;
        margin-bottom: 0;
        font-size: 16px;
        font-weight: bold;
        color: #43cf7c;
        background: var(--table-title-bg);

        :deep(.title-decoration) {
          background: #43cf7c !important;
        }

        :deep(.section-title) {
          color: #43cf7c !important;
        }
      }

      .table-content {
        height: calc(100% - 46px);
        overflow-y: auto;
        background: var(--background-color);
      }

      :deep(.el-card__body) {
        height: 100%;
        padding: 0;
      }
    }

    .table-card1 {
      background: var(--background-color-white);

      .table-title {
        margin-bottom: 0;
        background: var(--table-title-bg);

        :deep(.title-decoration) {
          background: var(--section-title-color) !important;
        }

        :deep(.section-title) {
          color: var(--section-title-color) !important;
        }
      }
    }

    .user-cell {
      display: flex;
      align-items: center;

      .user-name {
        margin-left: 10px;
      }
    }
  }

  @media (max-width: 768px) {
    .table-area {
      .el-col {
        margin-bottom: 15px;
      }
    }
  }
</style>
