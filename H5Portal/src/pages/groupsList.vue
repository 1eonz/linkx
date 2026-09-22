<template>
  <view class="groups-list">
    <!-- 头部区域 -->
    <view class="top-nav-bar" :style="{ 'padding-top': paddingTop + 'px' }">
      <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="goBack" />
      <text class="title">协同群组</text>
      <view class="right">已归档</view>
    </view>

    <!-- 筛选栏 -->
    <view class="filter-bar">
      <view
        class="filter-item"
        v-for="(item, index) in filterArr"
        :key="index"
        :class="{ active: activeId === item.id }"
        @click="clickItem(item)"
        >{{ item.name }}</view
      >
    </view>

    <!-- 群组列表 -->
    <view class="groups-container">
      <view class="groups-container2">
        <view class="group-item" v-for="(group, index) in groups" :key="index">
          <view class="group-icon">
            <img
              src="@/static/5110/groups.png"
              :width="adaptationSize.groupIconWidth"
              :height="adaptationSize.groupIconWidth"
            />
          </view>
          <view class="group-content">
            <view class="group-title">{{ group.title }}</view>
            <view class="group-tag">{{ group.tag }}</view>
            <view class="group-time">创建时间: {{ group.createTime }}</view>
          </view>
          <view class="group-actions">
            <view class="edit-tag" @click="editLabel(group, index)">
              <view class="edit-icon">
                <img
                  src="@/static/5110/edit.png"
                  :width="adaptationSize.editIconWidth"
                  :height="adaptationSize.editIconHeight"
                />
                ></view
              >
              <text>编辑标签</text>
            </view>
            <view
              class="archive-btn"
              :class="{ 'file-btn': group?.file === 1 }"
              @click="fileClick(group)"
            >
              <view class="folder-icon">
                <img
                  src="@/static/5110/file.png"
                  :width="adaptationSize.folderIconWidth"
                  :height="adaptationSize.folderIconHeight"
                />
              </view>
              <text>{{ group?.file === 1 ? '已归档' : '归档' }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- 编辑标签模态框 -->
    <view class="modal-overlay" v-if="showEditModal" @click="closeModal">
      <view class="modal-content" @click.stop>
        <view class="modal-header">
          <div class="modal-left"></div>
          <text class="modal-title">编辑标签</text>
          <view class="close-btn" @click="closeModal">×</view>
        </view>
        <view class="modal-body">
          <text class="instruction">请选择</text>
          <view class="tags-grid">
            <view
              class="tag-item"
              v-for="(tag, index) in availableTags"
              :key="index"
              :class="{ active: selectedTag === tag }"
              @click="selectTag(tag)"
            >
              {{ tag }}
            </view>
          </view>
        </view>
        <view class="modal-footer">
          <view class="btn btn-cancel" @click="closeModal">取消</view>
          <view class="btn btn-confirm" @click="confirmEdit">确定</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
  import { showConfirmDialog } from 'vant';
  import { ref, onMounted } from 'vue';
  import { useRouter } from 'vue-router';

  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';

  const router = useRouter();
  const communicationStore = useCommunicationStore();
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const filterArr = [
    { id: 1, name: '全部' },
    { id: 2, name: '刑事' },
    { id: 3, name: '治安' },
    { id: 4, name: '交通' },
    { id: 5, name: '群众求助' },
    { id: 6, name: '社会联动' },
    { id: 7, name: '群体事件' },
  ];

  const activeId = ref(1);

  // 模态框相关状态
  const showEditModal = ref(false);
  const selectedTag = ref('');
  const currentEditIndex = ref(-1);

  // 可用标签列表
  const availableTags = ['协同', '刑事', '治安', '交通', '群众求助', '社会联动', '群体事件'];

  // 群组数据
  const groups = ref([
    {
      title: '容城县北大街金星酒店XX',
      tag: '刑事',
      createTime: '2025-7-12 13:23',
      file: 1,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '协同',
      createTime: '2025-7-12 13:23',
      file: 1,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '交通',
      createTime: '2025-7-12 13:23',
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '群众求助',
      createTime: '2025-7-12 13:23',
      file: 0,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '社会联动',
      createTime: '2025-7-12 13:23',
      file: 0,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '群体事件',
      createTime: '2025-7-12 13:23',
      file: 0,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '其他',
      createTime: '2025-7-12 13:23',
      file: 0,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '其他',
      createTime: '2025-7-12 13:23',
      file: 0,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '其他',
      createTime: '2025-7-12 13:23',
      file: 0,
    },
    {
      title: '容城县北大街金星酒店XX',
      tag: '其他',
      createTime: '2025-7-12 13:23',
      file: 0,
    },
  ]);

  function clickItem(item) {
    activeId.value = item.id;
  }

  function editLabel(group, index) {
    if (group.file === 1) return;
    currentEditIndex.value = index;
    selectedTag.value = group.tag;
    showEditModal.value = true;
  }

  function selectTag(tag) {
    selectedTag.value = tag;
  }

  function closeModal() {
    showEditModal.value = false;
    selectedTag.value = '';
    currentEditIndex.value = -1;
  }

  function confirmEdit() {
    if (currentEditIndex.value >= 0 && selectedTag.value) {
      groups.value[currentEditIndex.value].tag = selectedTag.value;
    }
    closeModal();
  }

  function fileClick(group) {
    if (group.file === 1) return;
    showConfirmDialog({
      title: '提示',
      message: '是否确定要归档？',
    })
      .then(() => {
        console.log('归档');
      })
      .catch(() => {
        console.log('点击取消');
      });
  }

  function goBack() {
    router.back();
  }

  // 获取状态栏高度
  const paddingTop = ref(0);
  // uni.getSystemInfo({
  //   success: (res) => {
  //     paddingTop.value = res.statusBarHeight;
  //   },
  // });
  onMounted(() => {
    communicationStore.fetchStatusBarHeight().then((res) => (paddingTop.value = res));
  });
</script>

<style lang="scss" scoped>
  .groups-list {
    display: flex;
    flex-direction: column;
    // height: 100vh;
    background-color: #ededed;
    position: relative;
  }

  .top-nav-bar {
    display: flex;
    background-color: #ededed;
    align-items: center;
    justify-content: space-between;
    padding-left: 16px;
    padding-right: 16px;

    .uv-icon {
      width: 60px;
    }

    .title {
      color: rgba(3, 8, 26, 1);
      font-size: 18px;
      height: 44px;
      line-height: 44px;
    }
    .right {
      width: 50px;
    }
    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .title {
        height: 88px;
        line-height: 88px;
        font-size: 0.8rem;
      }
      .right {
        width: 130px;
        font-size: 0.8rem;
      }
    }
    @media screen and (min-height: 2001px) {
      .title {
        font-size: 0.6rem;
      }
      .right {
        font-size: 0.6rem;
      }
    }
  }

  // 头部样式
  .header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background-color: #fff;
    border-bottom: 1px solid #e5e5e5;
    position: sticky;
    top: 0;
    z-index: 100;

    .back-btn {
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;

      .arrow-icon {
        width: 12px;
        height: 12px;
        border-left: 2px solid #333;
        border-bottom: 2px solid #333;
        transform: rotate(45deg);
      }
    }

    .title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }

    .archived-btn {
      font-size: 14px;
      color: #666;
      cursor: pointer;
    }
  }

  // 筛选栏样式
  .filter-bar {
    display: flex;
    padding: 16px 12px 12px;
    border-radius: 16px 16px 0px 0px;
    background: rgba(255, 255, 255, 1);
    overflow-x: auto;
    white-space: nowrap;
    box-sizing: border-box;

    .filter-item {
      margin-right: 12px;
      border-radius: 16px;
      height: 24px;
      line-height: 24px;
      padding: 0 12px;
      font-size: 14px;
      color: rgba(90, 99, 131, 1);
      background: rgba(245, 245, 245, 1);
      cursor: pointer;
      transition: all 0.3s ease;

      &.active {
        background-color: #007aff;
        color: #fff;
      }
    }
    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .filter-item {
        height: 76px;
        line-height: 76px;
        font-size: 0.8rem;
        padding: 0 30px;
        border-radius: 44px;
      }
    }
    @media screen and (min-height: 2001px) {
      .filter-item {
        font-size: 0.6rem;
      }
    }
  }

  // 群组列表样式
  .groups-container {
    padding: 12px;
    background-color: #fff;
    overflow: hidden;
  }

  .groups-container2 {
    width: 100%;
    height: 100%;
    overflow-y: scroll;
  }
  .group-item {
    display: flex;
    align-items: flex-start;
    padding: 16px;
    margin-bottom: 12px;
    border-radius: 8px;
    background: rgba(245, 248, 253, 1);

    .group-icon {
      margin-right: 12px;
    }

    .group-content {
      flex: 1;
      margin-right: 12px;

      .group-title {
        font-size: 16px;
        font-weight: 500;
        color: #333;
        margin-bottom: 4px;
        line-height: 1.4;
      }

      .group-tag {
        display: inline-block;
        padding: 2px 8px;
        background: rgba(38, 99, 255, 0.1);
        font-size: 12px;
        border-radius: 4px;
        margin-bottom: 8px;
        color: rgba(38, 99, 255, 1);
      }

      .group-time {
        font-size: 12px;
        color: #999;
      }
      /* 平板适配 */
      @media screen and (min-height: 1200px) {
        .group-title,
        .group-tag {
          font-size: 0.8rem;
        }
        .group-time {
          font-size: 0.8rem;
        }
      }
      @media screen and (min-height: 2001px) {
        .group-title,
        .group-tag {
          font-size: 0.6rem;
        }
        .group-time {
          font-size: 0.6rem;
        }
      }
    }

    .group-actions {
      display: flex;
      flex-direction: column;
      align-items: flex-end;
      justify-content: space-between;

      .edit-tag {
        display: flex;
        align-items: center;
        font-size: 14px;
        color: rgba(134, 139, 152, 1);

        .edit-icon {
          margin-right: 4px;
        }
      }

      .archive-btn {
        display: flex;
        align-items: center;
        padding: 4px 10px;
        color: #007aff;
        border-radius: 4px;
        font-size: 14px;
        cursor: pointer;
        border-radius: 4px;
        border: 1px solid rgba(38, 99, 255, 1);
        box-sizing: border-box;
        margin-top: 23px;

        .folder-icon {
          margin-right: 4px;
        }
      }
      /* 平板适配 */
      @media screen and (min-height: 1200px) {
        .edit-tag {
          font-size: 0.8rem;
        }
        .archive-btn {
          font-size: 0.8rem;
          padding: 4px 20px;
          margin-top: 34px;
        }
      }
      @media screen and (min-height: 2001px) {
        .edit-tag {
          font-size: 0.6rem;
        }
        .archive-btn {
          font-size: 0.6rem;
        }
      }

      .file-btn {
        position: relative;
        opacity: 0.6;

        &::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background-color: rgba(38, 99, 255, 0.1);
          border-radius: 4px;
          pointer-events: none;
        }

        .folder-icon,
        text {
          position: relative;
          z-index: 1;
        }
      }
    }
  }

  // 模态框样式
  .modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: flex-end;
    justify-content: center;
    z-index: 1000;
  }

  .modal-content {
    width: 100%;
    background-color: #fff;
    border-radius: 16px 16px 0 0;
    max-height: 60vh;
    display: flex;
    flex-direction: column;
  }

  .modal-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 16px 16px;
    border-bottom: 1px solid #f0f0f0;

    .modal-left {
      width: 40px;
    }
    .modal-title {
      font-size: 18px;
      font-weight: 600;
      color: #333;
    }

    .close-btn {
      width: 24px;
      height: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
      color: #999;
      cursor: pointer;
    }
  }

  .modal-body {
    padding: 16px;
    flex: 1;
    max-height: 40vh;
    overflow-y: auto;

    .instruction {
      font-size: 14px;
      color: #666;
      margin-bottom: 16px;
      display: block;
      position: sticky;
      top: 0;
      background: #fff;
      z-index: 1;
    }

    .tags-grid {
      display: flex;
      flex-wrap: wrap;

      .tag-item {
        padding: 0 12px;
        height: 30px;
        line-height: 30px;
        border: 1px solid #e0e0e0;
        border-radius: 6px;
        text-align: center;
        font-size: 14px;
        color: #333;
        cursor: pointer;
        margin: 0 10px 10px 0;
        box-sizing: border-box;

        &.active {
          border-color: rgba(38, 99, 255, 1);
          color: rgba(38, 99, 255, 1);
          background: rgba(38, 99, 255, 0.1);
        }
      }
    }
  }

  .modal-footer {
    display: flex;
    padding: 16px;
    gap: 12px;
    border-top: 1px solid #f0f0f0;

    .btn {
      flex: 1;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      font-size: 16px;
      cursor: pointer;
      transition: all 0.3s ease;

      &.btn-cancel {
        background-color: #fff;
        color: #666;
        border: 1px solid #e0e0e0;
      }

      &.btn-confirm {
        background-color: rgba(38, 99, 255, 1);
        color: #fff;
        border: 1px solid rgba(38, 99, 255, 1);
      }
    }
  }
</style>
