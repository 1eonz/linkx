<script setup lang="ts">
  import { ref, computed, watch } from 'vue';
  import { ElMessage } from 'element-plus';
  import {
    getGroupCoopUsers,
    submitGroupRating,
    getGroupRatingList,
    getGroupRatingStatus,
  } from '@/api/statics';
  import { ArrowUp, ArrowDown, Close } from '@element-plus/icons-vue';

  const props = defineProps({
    modelValue: {
      type: Boolean,
      default: false,
    },
    groupId: {
      type: [String, Number],
      default: '',
    },
    groupName: {
      type: String,
      default: '',
    },
  });

  const emit = defineEmits(['update:modelValue', 'submit-success']);

  const dialogVisible = computed({
    get: () => props.modelValue,
    set: (val) => emit('update:modelValue', val),
  });

  // 评价维度配置（当前硬编码，预留后端配置接口逻辑）
  const RATING_DIMENSIONS = [
    { id: 1, code: 'supportResponse', name: '支撑响应速度', type: 'star' },
    { id: 2, code: 'approvalResponse', name: '审批响应速度', type: 'star' },
    { id: 3, code: 'dataValidity', name: '数据内容有效性', type: 'star' },
    { id: 4, code: 'overall', name: '协同岗综合评分', type: 'star' },
    { id: 5, code: 'comment', name: '评价内容', type: 'textarea' },
  ];

  // 打星文案
  const RATE_TEXTS = ['很差', '差', '一般', '满意', '很满意'];
  // 打星颜色
  const RATE_COLORS = ['#fa781b', '#fa781b', '#fa781b'];

  // 打星维度（过滤出type为star的维度）
  const starDimensions = computed(() => RATING_DIMENSIONS.filter((d) => d.type === 'star'));
  // 文字评价维度（过滤出type为textarea的维度）
  const textareaDimensions = computed(() => RATING_DIMENSIONS.filter((d) => d.type === 'textarea'));

  // 协同岗列表
  const coopUsers = ref<any[]>([]);
  // 已评价的协同岗ID集合
  const ratedCoopUserIds = ref<Set<number>>(new Set());
  // 选中的协同岗
  const selectedCoopUsers = ref<any[]>([]);
  // 各维度评分
  const dimensionScores = ref<Record<string, number>>({});
  // 文字评价
  const comment = ref('');
  // 提交loading
  const submitLoading = ref(false);
  // 数据加载loading
  const dataLoading = ref(false);

  // 评价状态
  const ratingStatus = ref<{
    completed: boolean;
    totalCoopUsers: number;
    ratedCoopUsers: number;
  }>({
    completed: false,
    totalCoopUsers: 0,
    ratedCoopUsers: 0,
  });

  // 已评价列表
  const ratedList = ref<any[]>([]);
  const ratedListLoading = ref(false);
  const ratedListTotal = ref(0);
  const ratedListPageNum = ref(1);
  const ratedListPageSize = 10;
  const ratedListNoMore = computed(() => ratedList.value.length >= ratedListTotal.value);

  // 已评价列表展开/收起
  const ratedListExpanded = ref(true);

  // 是否显示评价部分
  const showRatingForm = computed(() => !ratingStatus.value.completed);

  // 可选择的协同岗（已评价的不显示）
  const availableCoopUsers = computed(() =>
    coopUsers.value.filter((u) => !ratedCoopUserIds.value.has(u.coopUserId)),
  );

  // 初始化维度评分
  const initDimensionScores = () => {
    const scores: Record<string, number> = {};
    RATING_DIMENSIONS.forEach((d) => {
      scores[d.code] = 0;
    });
    dimensionScores.value = scores;
  };

  // 切换协同岗选中状态
  const toggleCoopUser = (user: any) => {
    const idx = selectedCoopUsers.value.findIndex((u) => u.coopUserId === user.coopUserId);
    if (idx > -1) {
      selectedCoopUsers.value.splice(idx, 1);
    } else {
      selectedCoopUsers.value.push(user);
    }
  };

  // 判断协同岗是否选中
  const isCoopUserSelected = (user: any) =>
    selectedCoopUsers.value.some((u) => u.coopUserId === user.coopUserId);

  // 表单验证
  const validateForm = () => {
    if (selectedCoopUsers.value.length === 0) {
      ElMessage.warning('请选择评价对象');
      return false;
    }
    for (const d of starDimensions.value) {
      if (!dimensionScores.value[d.code] || dimensionScores.value[d.code] === 0) {
        ElMessage.warning(`请对"${d.name}"进行评分`);
        return false;
      }
    }
    if (comment.value.length > 500) {
      ElMessage.warning('评价内容不能超过500字');
      return false;
    }
    return true;
  };

  // 提交评价
  const handleSubmit = async () => {
    if (!validateForm()) return;

    submitLoading.value = true;
    try {
      const dimensions = starDimensions.value.map((d) => ({
        id: d.id,
        code: d.code,
        name: d.name,
        score: dimensionScores.value[d.code],
      }));

      const ratings = selectedCoopUsers.value.map((user) => ({
        coopUserId: user.coopUserId,
        coopUserName: user.coopUserName,
        ratingDetails: {
          dimensions,
          comment: comment.value,
        },
      }));

      const res = await submitGroupRating(props.groupId, { ratings });
      if (res && res.code === 0) {
        ElMessage.success('评价成功');
        emit('submit-success');
        // 重置表单
        selectedCoopUsers.value = [];
        comment.value = '';
        initDimensionScores();
        // 关闭弹窗
        dialogVisible.value = false;
      } else {
        ElMessage.error(res?.msg || '评价失败');
      }
    } catch (error) {
      console.error('提交评价失败:', error);
      ElMessage.error('评价失败，请重试');
    } finally {
      submitLoading.value = false;
    }
  };

  // 获取协同岗列表
  const fetchCoopUsers = () => {
    if (!props.groupId) return Promise.resolve();
    return getGroupCoopUsers(props.groupId).then((res: any) => {
      if (res && res.code === 0 && res.data) {
        coopUsers.value = res.data;
      }
    }).catch((error) => {
      console.error('获取协同岗列表失败:', error);
    });
  };

  // 获取评价状态
  const fetchRatingStatus = () => {
    if (!props.groupId) return Promise.resolve();
    return getGroupRatingStatus(props.groupId).then((res: any) => {
      if (res && res.code === 0 && res.data) {
        ratingStatus.value = {
          completed: res.data.completed || false,
          totalCoopUsers: res.data.totalCoopUsers || 0,
          ratedCoopUsers: res.data.ratedCoopUsers || 0,
        };
      }
    }).catch((error) => {
      console.error('获取评价状态失败:', error);
    });
  };

  // 更新已评价的协同岗ID集合
  const updateRatedCoopUserIds = () => {
    const ids = new Set<number>();
    ratedList.value.forEach((item: any) => {
      ids.add(item.coopUserId);
    });
    ratedCoopUserIds.value = ids;
  };

  // 获取已评价列表
  const fetchRatingList = async (reset = false) => {
    if (!props.groupId) return;
    if (ratedListLoading.value) return;

    if (reset) {
      ratedListPageNum.value = 1;
      ratedList.value = [];
    }

    ratedListLoading.value = true;

    updateRatedCoopUserIds();

    ratedListLoading.value = false;

    try {
      const res: any = await getGroupRatingList(props.groupId, {
        pageNum: ratedListPageNum.value,
        pageSize: ratedListPageSize,
      });
      if (res && res.code === 0 && res.data) {
        const records = res.data.records || [];
        if (reset) {
          ratedList.value = records;
        } else {
          ratedList.value = [...ratedList.value, ...records];
        }
        ratedListTotal.value = res.data.total || 0;
        ratedListPageNum.value += 1;
        updateRatedCoopUserIds();
      }
    } catch (error) {
      console.error('获取已评价列表失败:', error);
    } finally {
      ratedListLoading.value = false;
    }
  };

  // 已评价列表滚动加载
  const handleRatedListScroll = (e: Event) => {
    const target = e.target as HTMLElement;
    if (!target) return;
    const distanceToBottom = target.scrollHeight - target.scrollTop - target.clientHeight;
    if (distanceToBottom <= 50 && !ratedListLoading.value && !ratedListNoMore.value) {
      fetchRatingList();
    }
  };

  // 格式化评价时间
  const formatRatingTime = (time: string) => {
    if (!time) return '';
    const date = new Date(time);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    return `${year}/${month}/${day} ${hours}:${minutes}`;
  };

  // 弹窗关闭时重置状态，防止残留上一个群组的数据
  const resetState = () => {
    coopUsers.value = [];
    selectedCoopUsers.value = [];
    ratedCoopUserIds.value = new Set();
    ratedList.value = [];
    ratedListTotal.value = 0;
    ratedListPageNum.value = 1;
    comment.value = '';
    ratingStatus.value = {
      completed: false,
      totalCoopUsers: 0,
      ratedCoopUsers: 0,
    };
    initDimensionScores();
  };

  // 弹窗打开时初始化数据
  watch(dialogVisible, (val) => {
    if (val && props.groupId) {
      dataLoading.value = true;
      initDimensionScores();
      selectedCoopUsers.value = [];
      comment.value = '';
      ratedListExpanded.value = true;
      Promise.all([fetchCoopUsers(), fetchRatingStatus(), fetchRatingList(true)]).finally(() => {
        // 所有请求完成后，默认选中所有可选择的协同岗（排除已评价的）
        selectedCoopUsers.value = [...availableCoopUsers.value];
        dataLoading.value = false;
      });
    } else if (!val) {
      resetState();
    }
  });
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title=""
    width="600px"
    :close-on-click-modal="false"
    :show-close="false"
    align-center
    append-to-body
    class="group-rating-dialog"
    :class="{ 'rating-only-list': !showRatingForm && ratedList.length > 0 }"
    @close="dialogVisible = false"
  >
    <div class="rating-container" v-loading="dataLoading">
      <!-- 标题区域：三种情况 -->
      <!-- 情况1和3：有评价表单时，显示蓝色bar+评价+关闭图标 -->
      <div class="rating-header" v-if="showRatingForm">
        <div class="header-left">
          <div class="header-bar"></div>
          <span class="header-title">评价<span class="header-bar-tips">({{ groupName }})</span></span>
        </div>
        <div class="header-close" @click="dialogVisible = false">
          <el-icon :size="18"><Close /></el-icon>
        </div>
      </div>
      <!-- 情况2：只有已评价列表时，显示蓝色bar+已评价+关闭图标，隐藏toggle -->
      <div class="rating-header" v-else-if="ratedList.length > 0">
        <div class="header-left">
          <div class="header-bar"></div>
          <span class="header-title">已评价<span class="header-bar-tips">({{ groupName }})</span></span>
        </div>
        <div class="header-close" @click="dialogVisible = false">
          <el-icon :size="18"><Close /></el-icon>
        </div>
      </div>

      <!-- 评价表单部分 -->
      <div class="rating-form" v-if="showRatingForm">
        <!-- 评价对象 -->
        <div class="form-section title-section">
          <div class="section-label">评价对象:</div>
          <div class="coop-user-list" v-if="availableCoopUsers.length > 0">
            <div
              v-for="user in availableCoopUsers"
              :key="user.coopUserId"
              class="coop-user-item"
              :class="{ active: isCoopUserSelected(user) }"
              @click="toggleCoopUser(user)"
            >
              <svg class="coop-user-check" :width="14" :height="14" aria-hidden="true">
                <use v-if="isCoopUserSelected(user)" xlink:href="#icon-check-box-check-icon" />
                <use v-else xlink:href="#icon-check-box-icon" />
              </svg>
              <span class="coop-user-name">{{ user.coopUserName }}</span>
            </div>
          </div>
          <div class="coop-user-empty" v-else>暂无可评价的协同岗</div>
        </div>

        <!-- 评分区域 -->
        <div class="form-section rating-section">
          <div class="rating-left">
            <div v-for="dim in starDimensions" :key="dim.code" class="rating-item">
              <span class="rating-label">{{ dim.name }}:</span>
              <el-rate
                v-model="dimensionScores[dim.code]"
                :texts="RATE_TEXTS"
                show-text
                size="large"
                :colors="RATE_COLORS"
                />
            </div>
          </div>
          <div class="rating-right">
            <el-input
              v-model="comment"
              type="textarea"
              :placeholder="textareaDimensions.length > 0 ? `请输入${textareaDimensions[0].name}` : '请输入评价内容'"
              :maxlength="500"
              :rows="5"
              resize="none"
              />
              <!-- show-word-limit -->
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="form-actions">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交</el-button>
        </div>
      </div>

      <!-- 已评价列表部分 -->
      <div class="rated-section" v-if="ratedList.length > 0">
        <!-- 情况3：评价和已评价都有时，显示收起/展开toggle -->
        <div class="rated-header" v-if="showRatingForm" @click="ratedListExpanded = !ratedListExpanded">
          <span class="rated-title">已评价</span>
          <div class="rated-toggle">
            <span>{{ ratedListExpanded ? '收起' : '展开' }}</span>
            <el-icon>
              <ArrowUp v-if="ratedListExpanded" />
              <ArrowDown v-else />
            </el-icon>
          </div>
        </div>
        <div
          v-show="ratedListExpanded"
          class="rated-list"
          @scroll="handleRatedListScroll"
        >
          <div
            v-for="(item, index) in ratedList"
            :key="item.id"
            class="rated-item"
            :class="{ 'no-border': index === ratedList.length - 1 }"
          >
            <div class="rated-item-header">
              <span class="rated-coop-name">评价对象：<span class="name-highlight">{{ item.coopUserName }}:</span></span>
              <span class="rated-time">{{ formatRatingTime(item.gmtCreated) }}</span>
            </div>
            <div class="rated-item-body">
              <div class="rated-left">
                <div
                  v-for="dim in (item.ratingDetails?.dimensions || [])"
                  :key="dim.code"
                  class="rated-rating-item"
                >
                  <span class="rated-rating-label">{{ dim.name }}:</span>
                  <el-rate
                    :model-value="dim.score"
                    disabled
                    :texts="RATE_TEXTS"
                    show-text
                    size="large"
                    :colors="RATE_COLORS"
                    />
                </div>
              </div>
              <div class="rated-right" v-if="item.ratingDetails?.comment">
                {{ item.ratingDetails.comment }}
              </div>
            </div>
          </div>
          <!-- 加载状态 -->
          <div v-if="ratedListLoading" class="rated-loading">
            <el-icon class="is-loading"><svg viewBox="0 0 1024 1024" xmlns="http://www.w3.org/2000/svg"><path d="M512 64a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V96a32 32 0 0 1 32-32zm0 640a32 32 0 0 1 32 32v192a32 32 0 0 1-64 0V736a32 32 0 0 1 32-32zM195.2 195.2a32 32 0 0 1 45.248 0L376.32 331.008a32 32 0 0 1-45.248 45.248L195.2 240.448a32 32 0 0 1 0-45.248zm407.424 407.424a32 32 0 0 1 45.248 0l135.808 135.872a32 32 0 0 1-45.248 45.248l-135.808-135.872a32 32 0 0 1 0-45.248zM64 512a32 32 0 0 1 32-32h192a32 32 0 0 1 0 64H96a32 32 0 0 1-32-32zm640 0a32 32 0 0 1 32-32h192a32 32 0 0 1 0 64H736a32 32 0 0 1-32-32zM195.2 828.8a32 32 0 0 1 0-45.248l135.872-135.808a32 32 0 0 1 45.248 45.248L240.448 828.8a32 32 0 0 1-45.248 0zm407.424-407.424a32 32 0 0 1 0-45.248l135.872-135.808a32 32 0 0 1 45.248 45.248L647.872 421.376a32 32 0 0 1-45.248 0z" fill="currentColor"/></svg></el-icon>
            <span>加载中...</span>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style lang="less" scoped>

  .rating-container {
    padding: 24px 24px 10px;
  }

  .rating-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 24px;
    font-size: 17.69px;
    font-weight: 400;
    line-height: 24px;
    padding: 1px 0;
    color: rgba(3, 11, 38, 1);

    .header-left {
      display: flex;
      align-items: center;
    }

    .header-bar {
      width: 4px;
      height: 14px;
      margin-right: 6px;
      background: #2663ff;
      border-radius: 2px;
    }
    .header-bar-tips{
      color: rgba(91, 96, 114, 1);
    }

    .header-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color);
    }

    .header-close {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      cursor: pointer;
      border-radius: 4px;
      color: var(--text-color);
      transition: all 0.2s;

      &:hover {
        background: var(--tag-bg);
        color: #2663ff;
      }
    }
  }

  .form-section {

    .section-label {
      width: 110px;
      padding-right: 4px;
      margin-bottom: 21px;
      color: rgba(76, 76, 76, 1);
      font-size: 16px;
      font-weight: 400;
      line-height: 24px;
    }
  }
  .title-section{
    display: flex;
    margin-bottom: 0px;
  }

  .coop-user-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px 48px;
    align-items: flex-start;
    align-content: flex-start;
  }

  .coop-user-item {
    display: flex;
    align-items: center;
    gap: 4px;
    // padding: 4px 14px;
    font-size: 13px;
    color: rgba(3, 11, 38, 1);
    cursor: pointer;
    // background: var(--tag-bg);
    // border: 1px solid var(--border-color);
    border-radius: 16px;
    transition: all 0.2s;

    .coop-user-check {
      flex-shrink: 0;
      margin-right: 4px;
      vertical-align: middle;
    }

    .coop-user-name {
      line-height: 1;
    }

    &:hover {
      // color: #2663ff;
      // border-color: #2663ff;
    }

    &.active {
      // color: #2663ff;
      // background: rgba(38, 99, 255, 0.08);
      // border-color: #2663ff;
    }
  }

  .coop-user-empty {
    font-size: 13px;
    color: var(--group-text-color);
  }

  .rating-section {
    display: flex;
    align-items: center;
    gap: 16px;

    .rating-left {
      flex: 1;
      padding: 12px 10px 12px 0;
    }

    .rating-right {
      flex: 1;

      :deep(.el-textarea__inner) {
        padding: 10px;
        border: 1px solid rgba(237, 237, 237, 1);
        border-radius: 4px;
        background: rgba(250, 250, 250, 1);

        &:focus-within {
          border: 1px solid #00c2ff;
          background: var(--input-focus-bg);
        }
      }
    }
  }

  .rating-item {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    &:last-child{
      margin-bottom: 0px;
    }

    .rating-label {
      width: 110px;
      padding-right: 4px;
      flex-shrink: 0;
      color: rgba(76, 76, 76, 1);
      font-size: 14px;
      font-weight: 400;
      line-height: 17px;
    }

    :deep(.el-rate) {
      height: 24px;

      .el-rate__icon {
        font-size: 18px;
      }

      .el-rate__text {
        font-size: 12px;
      }
    }
  }

  .form-actions {
    display: flex;
    // gap: 17px;
    justify-content: flex-end;
    padding-top: 24px;
  }

  .rated-section {
    margin-top: 16px;
    border-top: 1px solid var(--border-color);
  }

  .rated-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 24px 0;
    cursor: pointer;
    color: rgba(3, 11, 38, 1);
    font-size: 17px;
    font-weight: 400;
    line-height: 24px;

    .rated-title {
      font-size: 14px;
      font-weight: 600;
    }

    .rated-toggle {
      display: flex;
      align-items: center;
      gap: 4px;
      color: rgba(76, 76, 76, 1);
      font-size: 14px;
      font-weight: 400;
      line-height: 17px;
    }
  }

  .rated-list {
    max-height: 300px;
    overflow-y: auto;
  }

  .rated-item {
    padding: 12px 0;
    border-bottom: 1px solid #e8e8e8;

    &.no-border {
      border-bottom: none;
    }
  }

  .rated-item-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    font-size: 12px;
    font-weight: 400;
    line-height: 21px;
    color: rgba(76, 76, 76, 1);

    .rated-coop-name {
      font-size: 13px;

      .name-highlight {
        color: #2663ff;
      }
    }

    .rated-time {
      font-size: 12px;
    }
  }

  .rated-item-body {
    display: flex;
    gap: 20px;

    .rated-left {
      flex: 1;
      :deep(.el-rate__text) {
        color: rgba(91, 96, 114, 1);
      }
    }

    .rated-right {
      flex: 1;
      border-radius: 4px;
      background: rgba(250, 250, 250, 1);
      color: rgba(76, 76, 76, 1);
      font-size: 14px;
      font-weight: 400;
      line-height: 22px;
      padding: 10px;
      height: fit-content;
    }
  }

  .rated-rating-item {
    display: flex;
    align-items: center;
    margin-bottom: 12px;

    .rated-rating-label {
      width: 110px;
      flex-shrink: 0;
      color: rgba(76, 76, 76, 1);
      font-size: 12px;
      font-weight: 400;
      line-height: 14px;
    }

    :deep(.el-rate) {
      height: 20px;

      .el-rate__icon {
        font-size: 18px;
      }

      .el-rate__item {
        font-size: 14px;
      }

      .el-rate__text {
        font-size: 12px;
      }
    }
  }

  .rated-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    padding: 10px 0;
    font-size: 12px;
    color: var(--group-text-color);
  }

  .rated-left,.rating-left {
    :deep(.el-rate__text) {
      color: rgba(91, 96, 114, 1);
    }
  }
</style>
<style lang="less">
  .group-rating-dialog {
    padding: 0 !important;

    .el-dialog__body {
      padding: 0;
    }

    .el-dialog__header {
      padding: 0 !important;
    }
  }

  .group-rating-dialog.rating-only-list {
    .el-dialog__body {
      min-height: 60vh;
      max-height: 80vh;
      overflow: hidden;
      display: flex;
      flex-direction: column;
    }

    .rating-container {
      flex: 1;
      display: flex;
      flex-direction: column;
      min-height: 0;
      overflow: hidden;
    }

    .rated-section {
      flex: 1;
      display: flex;
      flex-direction: column;
      margin-top: 0;
      border-top: none;
      min-height: 0;
      overflow: hidden;
    }

    .rated-header {
      flex-shrink: 0;
    }

    .rated-list {
      flex: 1;
      min-height: 0;
      max-height: none;
      overflow-y: auto;
    }
  }
</style>
