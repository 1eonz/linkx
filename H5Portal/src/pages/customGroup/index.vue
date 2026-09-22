<template>
  <view class="page">
    <view
      :style="{
        width: '100%',
        height: paddingTop + 'px',
        'background-color': '#F5F5F5',
      }"
    ></view>
    <!-- 顶部区域 -->
    <view class="header">
      <van-icon
        name="arrow-left"
        :size="adaptationSize.iconSize"
        color="#333"
        @click="handleBack"
      />
      <text>选择联系人</text>
      <text></text>
    </view>
    <!-- 搜索框 -->
    <view class="search-bar">
      <van-search
        v-model="keywords"
        shape="square"
        :show-action="false"
        placeholder="姓名/手机号"
        class="my-custom-search"
        :clearable="true"
        @search="handleSearch"
        @update:modelValue="handleChange"
        @cancel="handleCancel"
      />
    </view>
    <view class="app-grid">
      <van-grid :column-num="3" :border="false">
        <van-grid-item
          v-for="(item, index) in createWayList"
          :key="index"
          @click="handleType(item)"
        >
          <view class="app-item" style="width: 100%">
            <view class="app-item-img">
              <van-image width="100%" height="100%" :src="item.icon" />
            </view>
            <view class="app-name">{{ item.name }}</view>
          </view>
        </van-grid-item>
      </van-grid>
    </view>
    <!-- 人员列表 -->
    <view class="groups-container">
      <!-- 空状态 -->
      <template v-if="true">
        <view class="empty-container" v-if="showEmptyState">
          <van-empty description="暂无内容" />
        </view>
        <van-list
          v-else-if="personList.length > 0"
          v-model:loading="listLoading"
          v-model:finished="listFinished"
          finished-text="没有更多数据了"
          :immediate-check="false"
          :offset="10"
          @load="loadMore"
          class="groups-container2"
        >
          <view class="group-item" v-for="(item, index) in personList" :key="index">
            <van-checkbox
              v-model="item.isSelected"
              icon-size="16px"
              @click="handlePersonChange(item)"
              style="width: 100%"
            >
              <view class="group-item-content">
                <view class="person-avatar">
                  <img
                    v-if="item.avatar"
                    class="avatar-img"
                    :src="transformImageUrl(`/admin-api/${item?.avatar}`)"
                    alt=""
                    :width="adaptationSize.groupIconWidth"
                    :height="adaptationSize.groupIconWidth"
                    style="border-radius: 18%"
                  />
                  <img
                    v-else
                    class="avatar-img"
                    src="@/assets/svg/avatar.svg"
                    alt=""
                    :width="adaptationSize.groupIconWidth"
                    :height="adaptationSize.groupIconWidth"
                    style="border-radius: 18%"
                  />
                  <!-- <img v-if="item?.state" class="avatar-state" src="@/assets/svg/admin-state.svg" />
                <view v-else class="avatar-state point"></view> -->
                </view>
                <view class="group-content">
                  <view class="group-title">
                    <view class="group-name">{{ item.name || '暂无' }}</view>
                  </view>
                  <view class="group-dep"> {{ item.departmentFullName || '暂无' }} </view>
                </view>
              </view>
            </van-checkbox>
          </view>
        </van-list>
      </template>
      <van-index-bar v-else class="groups-container2" :sticky="false">
        <template v-for="j in indexPersonList">
          <van-index-anchor :index="j.letter" />
          <van-cell v-for="item in j.children">
            <view class="group-item">
              <van-checkbox v-model="item.isSelected" icon-size="16px">
                <view class="group-item-content">
                  <view class="person-avatar">
                    <img
                      v-if="item.avatar"
                      class="avatar-img"
                      :src="transformImageUrl(`/admin-api${item.avatar}`)"
                      alt=""
                      :width="adaptationSize.groupIconWidth"
                      :height="adaptationSize.groupIconWidth"
                      style="border-radius: 18%"
                    />
                    <img
                      v-else
                      class="avatar-img"
                      src="@/static/5110/groups.png"
                      alt=""
                      :width="adaptationSize.groupIconWidth"
                      :height="adaptationSize.groupIconWidth"
                      style="border-radius: 18%"
                    />
                  </view>
                  <view class="group-content">
                    <view class="group-title">
                      <view class="group-name">{{ item.name || '暂无' }}</view>
                    </view>
                    <view class="group-dep"> 304部门 </view>
                  </view>
                </view>
              </van-checkbox>
            </view>
          </van-cell>
        </template>
      </van-index-bar>
    </view>
    <SelectedList />
  </view>
</template>

<script setup>
  import { showFailToast, showSuccessToast } from 'vant';
  import { ref, onMounted, computed, watch } from 'vue';
  import { useRouter } from 'vue-router';

  import SelectedList from './components/selectedList.vue';

  // 导入图标
  import cooperationIcon from '@/assets/svg/cooperation.svg';
  import netIcon from '@/assets/svg/net.svg';
  import starIcon from '@/assets/svg/star.svg';
  import { usersPage } from '@/common/api/customGroup.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useSelectedPersons } from '@/stores/selectedPerson.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { debounce } from '@/utils';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  const router = useRouter();
  const selectedPersonStore = useSelectedPersons();
  // 使用设备适配
  const { adaptationSize } = useDeviceAdapter();

  const communicationStore = useCommunicationStore();
  const paddingTop = ref(0);

  // 搜索关键词
  const keywords = ref('');

  // 列表状态
  const listLoading = ref(false);
  const listFinished = computed(() => !hasMore.value);
  const loadingMore = ref(false);
  const hasMore = ref(true);
  const total = ref(0);
  // 创建方式列表
  const createWayList = ref([
    {
      name: '按组织选',
      icon: netIcon,
      path: '/pages/departmentList',
    },
    {
      name: '按关注选',
      icon: starIcon,
      path: '/pages/followList',
    },
    // {
    //   name: '选择一个群',
    //   icon: usersIcon,
    //   path: '/pages/myGroupList',
    // },
    {
      name: '协同岗',
      icon: cooperationIcon,
      path: '/pages/collaborativePositionList',
    },
  ]);

  const personList = ref([]);
  // 模拟人员数据（按字母索引分组）
  const indexPersonList = ref([
    {
      letter: 'A',
      children: [
        {
          id: 'a1',
          name: '艾米',
          avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-1.jpeg',
          tag: '协同岗',
          department: '技术研发部',
          phone: '13800138001',
          isSelected: false,
        },
        {
          id: 'a2',
          name: '安琪',
          avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-2.jpeg',
          tag: '普通员工',
          department: '产品设计部',
          phone: '13800138002',
          isSelected: false,
        },
      ],
    },
    {
      letter: 'B',
      children: [
        {
          id: 'b1',
          name: '白露',
          avatarImg: '',
          tag: '协同岗',
          department: '市场营销部',
          phone: '13800138003',
          isSelected: false,
        },
      ],
    },
    {
      letter: 'C',
      children: [
        {
          id: 'c1',
          name: '陈明',
          avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-3.jpeg',
          tag: '协同岗',
          department: '技术研发部',
          phone: '13800138004',
          isSelected: false,
        },
        {
          id: 'c2',
          name: '程伟',
          avatarImg: '',
          tag: '普通员工',
          department: '客户服务部',
          phone: '13800138005',
          isSelected: false,
        },
      ],
    },
    {
      letter: 'D',
      children: [
        {
          id: 'd1',
          name: '邓超',
          avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-4.jpeg',
          tag: '协同岗',
          department: '行政管理部',
          phone: '13800138006',
          isSelected: false,
        },
      ],
    },
    {
      letter: 'F',
      children: [
        {
          id: 'f1',
          name: '方婷',
          avatarImg: '',
          tag: '普通员工',
          department: '财务部',
          phone: '13800138007',
          isSelected: false,
        },
      ],
    },
    {
      letter: 'G',
      children: [
        {
          id: 'g1',
          name: '高远',
          avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-5.jpeg',
          tag: '协同岗',
          department: '技术研发部',
          phone: '13800138008',
          isSelected: false,
        },
      ],
    },
    {
      letter: 'H',
      children: [
        {
          id: 'h1',
          name: '黄磊',
          avatarImg: '',
          tag: '普通员工',
          department: '人力资源部',
          phone: '13800138009',
          isSelected: false,
        },
      ],
    },
    {
      letter: 'J',
      children: [
        {
          id: 'j1',
          name: '江涛',
          avatarImg: 'https://fastly.jsdelivr.net/npm/@vant/assets/apple-6.jpeg',
          tag: '协同岗',
          department: '技术研发部',
          phone: '13800138010',
          isSelected: false,
        },
      ],
    },
  ]);
  // 分页相关状态
  const listParams = ref({
    pageNo: 1,
    pageSize: 10,
    keywords: '', //搜索关键字
  });

  // 已选人员ID列表
  const selectedPersonIds = computed(() => {
    return selectedPersonStore.getSelectedPersonsId;
  });
  // 是否显示空状态
  const showEmptyState = computed(() => {
    return personList.value.length === 0 && !listLoading.value;
  });
  watch(
    () => selectedPersonIds.value,
    (newVal, oldVal) => {
      personList.value.forEach((item) => {
        item.isSelected = newVal.includes(item.id) || false;
      });
    },
  );
  // 返回上一页
  const handleBack = () => {
    selectedPersonStore.clearSelectedPersons();
    communicationStore.close();
  };
  // 输入变化
  const handleChange = debounce((val) => {
    if (!val) {
      // 清空搜索关键词
      keywords.value = '';
      // 清空列表
      personList.value = [];
      hasMore.value = false;
    } else {
      // 重置分页参数
      listParams.value.pageNo = 1;
      // 重新获取所有数据
      getUserList();
    }
  }, 1000);
  const handleSearch = () => {
    listParams.value.pageNo = 1;
    getUserList();
  };
  const handleCancel = () => {
    personList.value = [];
  };
  // 处理类型选择
  const handleType = async (item) => {
    router.push(item.path);
  };

  // 加载更多
  const loadMore = async () => {
    if (loadingMore.value || !hasMore.value) return;

    listParams.value.pageNo += 1;
    await getUserList(true);
  };
  // 获取人员列表
  async function getUserList(isLoadMore = false) {
    listLoading.value = true;
    if (isLoadMore) {
      loadingMore.value = true;
    }
    listParams.value.keywords = keywords.value;
    try {
      let res = await usersPage({
        ...listParams.value,
      });

      if (res) {
        const records = res.records.map((item) => {
          item.departmentFullName = item.userDepartments.map((item) => item.departmentName).join('/');
          item.isSelected = selectedPersonIds.value.includes(item.id);
          return item;
        });
        if (isLoadMore) {
          // 加载更多，追加数据
          personList.value = [...personList.value, ...records];
        } else {
          // 刷新，替换数据
          personList.value = records;
        }
        console.log(personList.value, '===personList.value===');
        // 更新分页信息
        total.value = res.total || 0;
        hasMore.value = personList.value.length < total.value;
        // 如果当前页数据不足一页，说明没有更多数据了
        if (records.length < listParams.value.pageSize) {
          hasMore.value = false;
        }
      }
    } catch (error) {
      console.error('获取人员列表失败:', error);
      showFailToast('获取数据失败');
    } finally {
      loadingMore.value = false;
      listLoading.value = false;
    }
  }
  // 点击人员
  const handlePersonChange = (item) => {
    selectedPersonStore.changeSelectedPersons(item);
  };
  onMounted(async () => {
    paddingTop.value = await communicationStore.fetchStatusBarHeight();
    await communicationStore.getUserInfo();
  });
</script>

<style lang="scss" scoped>
  .page {
    overflow: hidden;
    height: 100vh;
    display: flex;
    flex-direction: column;
    align-items: center;
    background: #fff;
  }

  .header {
    width: 100%;
    height: 44px;
    padding: 0 19px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 16px;
    font-weight: 500;
  }

  .search-bar {
    width: 100%;
    padding: 6px 16px;
    display: flex;
    align-items: center;
  }

  .my-custom-search {
    flex: 1;
    padding: 0;
    border-radius: 8px;
  }

  .app-grid {
    width: 100%;
    background: #fff;
    margin-top: 10px;
    padding: 10px 0;
  }

  .app-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    .app-name {
      margin-top: 8px;
      font-size: 12px;
      color: #999;
    }
    .app-item-img {
      width: 50px;
      height: 50px;
      padding: 10px;
      background-color: #f5f5f5;
      border-radius: 16%;
    }
  }

  .groups-container {
    flex: 1;
    width: 100%;
    overflow: hidden;
    margin-top: 10px;
    background: #fff;
  }

  .groups-container2 {
    height: 100%;
    overflow-y: auto;
  }

  .empty-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 200px;
  }

  .group-item {
    padding: 12px 16px;
    .group-item-content {
      display: flex;
      align-items: center;
    }
    .person-avatar {
      flex-shrink: 0;
      margin-right: 12px;
      position: relative;
      .avatar-img {
        display: block;
      }
      .avatar-state {
        position: absolute;
        bottom: -1px;
        right: -1px;
        border-radius: 50%;
      }
      .point {
        border: 1px solid #fff;
        background-color: #c0c2c9;
        width: 12px;
        height: 12px;
      }
    }

    .group-content {
      flex: 1;
      .group-title {
        display: flex;
        align-items: center;
        .group-tag {
          display: flex;
          align-items: center;
          padding: 2px 8px;
          background: #f0f2f5;
          border-radius: 4px;
          font-size: 12px;
          color: #2151d7;
          margin-right: 5px;
        }
        .group-name {
          font-size: 14px;
          color: #333;
        }
      }
      .group-dep {
        text-align: left;
        font-size: 12px;
        color: #999;
      }
    }
  }
  .footer {
    width: 100%;
    padding: 12px 16px;
    background-color: #f7f8fa;
    display: flex;
    align-items: center;
    justify-content: space-between;
    .footer-selected-num {
      font-size: 14px;
      color: #2151d7;
      margin-right: 5px;
    }
    .footer-selected-img {
      flex: 1;
      margin-right: 20px;
      overflow: hidden;
      .van-image {
        margin-right: 5px;
      }
    }
  }

  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .header {
      height: 100px;
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .header {
      font-size: 0.6rem;
    }
  }
</style>
