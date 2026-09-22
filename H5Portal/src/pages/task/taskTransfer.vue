<template>
  <div class="task-transfer">
    <view :style="{ 'padding-top': paddingTop + 'px', 'background-color': '#FFFFFF' }">
      <view class="taskNavBar">
        <van-nav-bar left-arrow @click-left="handleClickLeft">
          <template #title>
            <view class="nav-title">任务转派</view>
          </template>
        </van-nav-bar>
      </view>
    </view>
    <view class="task-content">
      <view class="card card-1">
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">所属系统：</view>
            <view class="value disabled">{{ taskDetail.system }}</view>
          </view>
        </view>
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">任务名称：</view>
            <view class="value disabled">{{ taskDetail.name }}</view>
          </view>
        </view>
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">任务内容：</view>
            <view class="value disabled content-value">{{ taskDetail.content }}</view>
          </view>
        </view>
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">业务类型：</view>
            <view class="value disabled">{{ taskDetail.businessType }}</view>
          </view>
        </view>
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">执行人：</view>
            <view class="value disabled">{{ taskDetail.executors?.[0]?.name || '' }}</view>
          </view>
        </view>
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">开始时间：</view>
            <view class="value disabled">{{ taskDetail.startTime }}</view>
          </view>
        </view>
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">结束时间：</view>
            <view class="value disabled">{{ taskDetail.endTime }}</view>
          </view>
        </view>
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">任务状态：</view>
            <view class="value disabled">{{ taskDetail.status }}</view>
          </view>
        </view>
      </view>
      <view class="card card-2">
        <view class="form-item">
          <view class="item-wrap">
            <view class="label">转派对象：</view>
            <view class="value" @click="showPersonPicker = true">
              <template v-if="selectedPerson">
                <view class="transfer-person">{{ selectedPerson.name }}</view>
              </template>
              <view v-else class="transfer-placeholder">请选择</view>
              <view class="transfer-spacer"></view>
              <van-icon name="arrow" class="transfer-arrow" />
            </view>
          </view>
        </view>
      </view>
    </view>
    <view class="bottom">
      <view class="btn-wrap">
        <van-button class="save-btn" @click="handleConfirm">确认转派</van-button>
      </view>
    </view>

    <van-popup
      v-model:show="showPersonPicker"
      position="bottom"
      :style="{ height: '70%' }"
      round
    >
      <view class="picker-popup">
        <view class="picker-header">
          <view class="picker-title">选择转派对象</view>
          <van-icon name="cross" class="picker-close" @click="showPersonPicker = false" />
        </view>
        <view class="search-bar">
          <van-icon name="search" class="search-icon" />
          <input
            class="search-input"
            v-model="searchKeyword"
            placeholder="搜索姓名"
            @input="handleSearchInput"
          />
          <van-icon
            v-if="searchKeyword"
            name="clear"
            class="clear-icon"
            @click="handleSearchClear"
          />
        </view>
        <view class="picker-body">
          <view v-if="listLoading && personList.length === 0" class="loading-wrap">
            <van-loading size="24px">加载中...</van-loading>
          </view>
          <van-list
            v-else
            v-model:loading="listLoading"
            :finished="listFinished"
            finished-text=""
            @load="onLoadMore"
            :immediate-check="true"
          >
            <view v-if="personList.length > 0" class="person-list">
              <view
                v-for="item in personList"
                :key="item.id"
                :class="['person-cell', selectedPerson?.id === item.id ? 'active' : '']"
                @click="handleSelectPerson(item)"
              >
                <img
                  v-if="item.avatar"
                  class="person-avatar"
                  :src="transformImageUrl(`/admin-api${item.avatar}`)"
                />
                <img
                  v-else
                  class="person-avatar"
                  src="@/assets/svg/avatar.svg"
                />
                <view class="person-cell-info">
                  <view class="person-cell-name">{{ item.name }}</view>
                  <view class="person-cell-dept">{{ item.departmentName }}</view>
                </view>
                <view :class="['check-circle', selectedPerson?.id === item.id ? 'checked' : '']">
                  <van-icon v-if="selectedPerson?.id === item.id" name="success" color="#fff" size="12" />
                </view>
              </view>
            </view>
            <view v-else-if="!listLoading" class="empty-wrap">
              <view class="empty-text">{{ searchKeyword ? '未找到相关人员' : '暂无人员数据' }}</view>
            </view>
          </van-list>
        </view>
      </view>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useCommunicationStore } from '@/stores/communication.js';
import { taskApi } from '@/common/api/index.js';
import { usersPage } from '@/common/api/customGroup.js';
import { transformImageUrl } from '@/utils/imgUrlParse.js';
import { onMounted } from 'vue';
import { showToast } from 'vant';

const communicationStore = useCommunicationStore();
const paddingTop = ref(0);

const taskNumber = ref('');
const accessToken = ref('');
const taskDetail = ref({});
const attachments = ref([]);
const selectedPerson = ref(null);
const showPersonPicker = ref(false);
const searchKeyword = ref('');
const personList = ref([]);
const listLoading = ref(false);
const listFinished = ref(false);
const listParams = ref({ pageNo: 1, pageSize: 20 });
const totalCount = ref(0);
let listAbortController = null;

async function fetchTaskDetailByTaskNumber(taskNumber, token) {
  try {
    const taskDetailRes = await taskApi.getTaskDetailByTaskNumber({ taskNumber, token });
    taskDetail.value = taskDetailRes;
    await fetchAttachments(taskNumber);
  } catch (e) {
    taskDetail.value = {};
  }
}

async function fetchAttachments(taskNumber) {
  try {
    const res = await taskApi.getAttachment({ taskNumber });
    if (res && Array.isArray(res)) {
      attachments.value = res;
    }
  } catch (error) {
    console.error('获取附件失败', error);
    attachments.value = [];
  }
}

function mapPersonItem(item) {
  const dept = item.primaryDepartment && Object.keys(item.primaryDepartment).length > 0
    ? item.primaryDepartment
    : (item.userDepartments && item.userDepartments.length > 0 ? item.userDepartments[0] : {});
  return {
    id: item.id,
    name: item.name || '',
    idCard: item.idCard || '',
    department: dept.departmentName || '',
    departmentId: dept.departmentId || '',
    departmentCode: dept.departmentCode || '',
    departmentName: dept.departmentName || '',
    avatar: item.avatar || '',
  };
}

async function loadPersonList(isLoadMore = false) {
  if (!isLoadMore) {
    listAbortController?.abort();
    listAbortController = new AbortController();
    listParams.value.pageNo = 1;
    personList.value = [];
    listFinished.value = false;
  }

  listLoading.value = true;
  try {
    const res = await usersPage(
      { ...listParams.value },
      {},
      { signal: listAbortController?.signal },
    );
    const records = res?.records || [];
    const mapped = records.map(mapPersonItem);

    if (isLoadMore) {
      personList.value = [...personList.value, ...mapped];
    } else {
      personList.value = mapped;
    }

    totalCount.value = res?.total || 0;
    // 返回空页视为没有更多数据，防止后端分页异常时 van-list 无限触发加载
    listFinished.value = records.length === 0 || personList.value.length >= totalCount.value;

    if (!listFinished.value) {
      listParams.value.pageNo += 1;
    }
  } catch (error) {
    if (error.name === 'CanceledError' || error.name === 'AbortError') return;
    console.error('获取人员列表失败', error);
    listFinished.value = true;
    if (!isLoadMore) {
      personList.value = [];
    }
  } finally {
    listLoading.value = false;
  }
}

function onLoadMore() {
  loadPersonList(true);
}

let searchAbortController = null;
let searchDebounceTimer = null;

function handleSearchInput() {
  if (searchDebounceTimer) {
    clearTimeout(searchDebounceTimer);
  }
  searchDebounceTimer = setTimeout(() => {
    searchPersonList();
  }, 300);
}

function handleSearchClear() {
  searchKeyword.value = '';
  searchAbortController?.abort();
  searchAbortController = null;
  loadPersonList(false);
}

async function searchPersonList() {
  searchAbortController?.abort();
  searchAbortController = new AbortController();

  if (!searchKeyword.value.trim()) {
    loadPersonList(false);
    return;
  }

  listAbortController?.abort();
  listLoading.value = true;
  listFinished.value = true;
  personList.value = [];

  try {
    const res = await usersPage(
      { pageNo: 1, pageSize: 20, keywords: searchKeyword.value.trim() },
      {},
      { signal: searchAbortController?.signal },
    );
    const records = res?.records || [];
    personList.value = records.map(mapPersonItem);
    listFinished.value = personList.value.length >= (res?.total || 0);
  } catch (error) {
    if (error.name === 'CanceledError' || error.name === 'AbortError') return;
    console.error('搜索人员失败', error);
    personList.value = [];
  } finally {
    listLoading.value = false;
  }
}

watch(showPersonPicker, (val) => {
  if (val) {
    searchKeyword.value = '';
    searchAbortController?.abort();
    searchAbortController = null;
    loadPersonList(false);
  }
});

function handleSelectPerson(person) {
  if (selectedPerson.value?.id === person.id) {
    selectedPerson.value = null;
  } else {
    selectedPerson.value = { ...person };
  }
  showPersonPicker.value = false;
}

async function handleConfirm() {
  if (!selectedPerson.value) {
    showToast('请选择转派人');
    return;
  }
  try {
    const now = new Date();
    const pad = (n) => String(n).padStart(2, '0');
    const operateTime = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`;
    console.log('[handleConfirm]: params', {
      taskNumber: taskNumber.value,
      nextExecutors: [{
        name: selectedPerson.value.name,
        idCard: selectedPerson.value.idCard,
        department: selectedPerson.value.department,
        departmentId: selectedPerson.value.departmentId,
        departmentCode: selectedPerson.value.departmentCode,
        operateTime,
      }],
    });
    await taskApi.updateTaskStatus({
      taskNumber: taskNumber.value,
      status: taskDetail.value.status,
      attachments: attachments.value.filter(item => !item.id),
      nextExecutors: [{
        name: selectedPerson.value.name,
        idCard: selectedPerson.value.idCard,
        department: selectedPerson.value.department,
        departmentId: selectedPerson.value.departmentId,
        departmentCode: selectedPerson.value.departmentCode,
        operateTime,
      }],
    });
    showToast('转派成功');
  } catch (error) {
    showToast('转派失败');
  } finally {
    setTimeout(() => {
      communicationStore.close();
    }, 1000);
  }
}

function handleClickLeft() {
  communicationStore.close();
}

onMounted(async () => {
  paddingTop.value = await communicationStore?.fetchStatusBarHeight() || 0;

  taskNumber.value = new URLSearchParams(window.location.search).get('taskNumber');
  accessToken.value = new URLSearchParams(window.location.search).get('accessToken');

  fetchTaskDetailByTaskNumber(taskNumber.value, accessToken.value);
})
</script>

<style scoped lang="scss">
.nav-title {
  font-size: 20px;
}

.task-transfer {
  height: 100%;
  background-color: #F5F5F5;
  display: flex;
  flex-direction: column;

  .task-content {
    flex: 1;
    overflow: auto;

    .card {
      background-color: #FFFFFF;
      margin-top: 16px;

      .form-item {
        border-bottom: 1px solid #fafafa;

        &:last-child {
          border-bottom: none;
        }

        .item-wrap {
          margin:0 16px;
          display: flex;
          flex-direction: row;
          align-items: flex-start;
          padding: 12px 0;

          .label {
            flex-shrink: 0;
            width: 80px;
            text-align: left;
            line-height: 1.5;
            white-space: nowrap;
            color: rgba(90, 99, 131, 1);
          }

          .value {
            flex: 1;
            min-width: 0;
            margin-left: 16px;
            text-align: left;
            word-break: break-word;
            line-height: 1.5;
            color: rgba(51, 51, 51, 1);
            display: flex;
            align-items: center;
            justify-content: flex-start;

            &.disabled {
              color: rgba(51, 51, 51, 1);
            }

            &.content-value {
              white-space: pre-wrap;
            }

            .transfer-person {
              color: #007AFF;
            }

            .transfer-placeholder {
              color: #999;
            }

            .transfer-spacer {
              flex: 1;
            }

            .transfer-arrow {
              margin-left: 4px;
              font-size: 14px;
              color: #999;
            }
          }

        }
      }
    }
  }

  .bottom {
    flex-shrink: 0;

    .btn-wrap {
      padding: 16px;
      border-top: 1px solid #E5E5E5;

      .save-btn {
        width: 100%;
        background-color: #007AFF;
        color: #fff;
        border-radius: 8px;
      }
    }
  }
}

.picker-popup {
  display: flex;
  flex-direction: column;
  height: 100%;

  .picker-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 16px;
    border-bottom: 1px solid #E5E5E5;

    .picker-title {
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }

    .picker-close {
      font-size: 20px;
      color: #999;
    }
  }

  .search-bar {
    display: flex;
    align-items: center;
    margin: 8px 12px;
    padding: 8px 12px;
    background-color: #F7F8FA;
    border-radius: 8px;

    .search-icon {
      font-size: 16px;
      color: #999;
      margin-right: 8px;
      flex-shrink: 0;
    }

    .search-input {
      flex: 1;
      border: none;
      outline: none;
      background: transparent;
      font-size: 14px;
      color: #333;
      line-height: 20px;

      &::placeholder {
        color: #C8C9CC;
      }
    }

    .clear-icon {
      font-size: 16px;
      color: #C8C9CC;
      margin-left: 8px;
      flex-shrink: 0;
    }
  }

  .picker-body {
    flex: 1;
    overflow: auto;

    .loading-wrap {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px 0;
    }

    .empty-wrap {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px 0;

      .empty-text {
        font-size: 14px;
        color: #999;
      }
    }

    .person-list {
      .person-cell {
        display: flex;
        align-items: center;
        padding: 12px 16px;
        border-bottom: 1px solid #F5F5F5;

        &.active {
          background-color: #F0F7FF;
        }

        .person-avatar {
          width: 36px;
          height: 36px;
          border-radius: 50%;
          flex-shrink: 0;
          margin-right: 10px;
        }

        .person-cell-info {
          flex: 1;

          .person-cell-name {
            font-size: 15px;
            color: #333;
          }

          .person-cell-dept {
            font-size: 12px;
            color: #999;
            margin-top: 2px;
          }
        }

        .check-circle {
          width: 20px;
          height: 20px;
          border-radius: 50%;
          border: 2px solid #D9D9D9;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
          margin-left: 8px;
          transition: all 0.2s;

          &.checked {
            border-color: #007AFF;
            background-color: #007AFF;
          }
        }
      }
    }
  }
}
</style>
