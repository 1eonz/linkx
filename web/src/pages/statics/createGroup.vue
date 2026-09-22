<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';
  // import { useRouter } from 'vue-router';

  import { getGlobalsList } from '@/api/dictionary';
  import { getLicenseInfo } from '@/api/statics';
  // import { getCurrentOrganization } from '@/api/statics';
  import { collaborationLabelList, labelCreateGroup } from '@/api/xietong';
  import { openChat } from '@/bridge/post.js';
  import { Message } from '@/components/Message';
  import { usePIMStore } from '@/store';
  import OtherNodeCoopPicker from './components/OtherNodeCoopPicker.vue';

  const props = defineProps({
    currentTheme: {
      type: String,
      default: 'light',
    },
    onSuccess: {
      type: Function,
      default: null,
    },
  });

  interface LicenseInfo {
    HADR?: string;
    LINKXACF?: string;
    LINKXBCF?: string;
    LINKXBS?: string;
    LINKXCCF?: string;
    LINKXGCF?: string;
    LINKXNDI?: string;
    LINKXNum?: string;
    LINKXSDF?: string;
    LINKXTCF?: string;
  }
  const emit = defineEmits(['closeDialog', 'success']);
  const licenseInfo = ref<LicenseInfo>({});
  const PIMStore = usePIMStore();
  // const router = useRouter();
  const isChildren = ref(false);
  const selectedIds = ref<string[]>([]);
  const selectedNames = ref<string[]>([]);

  const options = ref<any>([]);
  const selectIds = ref<string[]>([]);
  const info = computed(() => {
    const { user } = PIMStore;
    return {
      departmentCode: user?.department?.departmentCode || '',
      departmentFullPath: user?.department?.fullPath || '',
      departmentId: user?.department?.departmentId || '',
      departmentName: user?.department?.departmentName || '',
      ownerId: user?.userid || '',
      ownerName: user?.username || '',
    };
  });
  const loading = ref(false);
  // 是否添加其它节点
  const isAddOtherNode = ref(false);
  // 其它节点协同岗选择弹窗
  const showCoopPicker = ref(false);
  // 已选的其它节点协同岗人员
  const selectedCoopPersons = ref<any[]>([]);
  const getLicense = async () => {
    const res = await getLicenseInfo();
    if (res.code === 0) {
      licenseInfo.value = res.data as LicenseInfo;
    }
  };
  // 是否允许多类型协作
  const multipleCollaboration = ref(false);
  // 修改后
  const getMultipleCollaboration = async () => {
    const res = await getGlobalsList();
    if (res.code === 0) {
      const value = res.data?.MULTIPLE_COLLABORATION;
      multipleCollaboration.value = value === 'true' || value === true;
    }
  };

  onMounted(async () => {
    await getLicense();
    await getMultipleCollaboration();
    getLabelList();
  });

  function closeWindow() {
    emit('closeDialog');
  }

  async function getLabelList() {
    const { code, data } = await collaborationLabelList(1);
    if (code === 0) {
      if (licenseInfo.value.LINKXACF === '0') {
        // 当LINKXACF为'0'时，过滤掉name为'人员核查'的项
        options.value = (data as Array<{ children?: any[]; name: string }>).filter(
          (item) => item.name !== '人员核查',
        );
      } else {
        options.value = data;
      }
      // 判断options.value中是否有任意对象的children属性有值
      isChildren.value = options.value.some((item) => item.children && item.children.length > 0);
    }
  }

  function mapCancelCheck(data, cancel?: boolean) {
    (data || []).forEach((item) => {
      if (!item.children && !cancel) {
        return;
      }
      item.check = false;
      mapCancelCheck(item.children, cancel);
    });
  }

  function findParentNode(node) {
    for (const item of options.value) {
      if (item.children) {
        // 检查一级子节点
        if (item.children.some((child) => child.id === node.id)) {
          return item;
        }
        // 检查二级子节点
        for (const child of item.children) {
          if (child.children && child.children.some((grandChild) => grandChild.id === node.id)) {
            return item;
          }
        }
      }
    }
    return null;
  }

  // 获取所有已选中节点的类型集合
  function getSelectedTypes() {
    const types = new Set<number>();
    function traverseNodes(nodes) {
      if (!nodes) return;
      nodes.forEach((item) => {
        if (!item.children && item.check && item.type !== undefined) {
          types.add(item.type);
        }
        if (item.children) {
          traverseNodes(item.children);
        }
      });
    }
    traverseNodes(options.value);
    return types;
  }

  // 判断节点是否应该被禁用
  function isNodeDisabled(node) {
    if (!node || node.type === undefined) return false;
    if (multipleCollaboration.value === false) return false;
    const selectedTypes = getSelectedTypes();
    // 如果节点已选中，不禁用
    if (node.check) return false;
    // 如果已选中列表中有 type === 0 的节点，则禁用 type === 1 的节点
    if (selectedTypes.has(0) && node.type === 1) return true;
    // 如果已选中列表中有 type === 1 的节点，则禁用 type === 0 的节点
    if (selectedTypes.has(1) && node.type === 0) return true;
    return false;
  }
  function handleSelect(brother, node) {
    // 如果是只显示一级标签的情况（isChildren为false）
    if (!isChildren.value) {
      // 清除之前的选择
      selectedIds.value.length = 0;
      selectedNames.value.length = 0;
      selectIds.value.length = 0;

      // 添加当前选择
      selectedIds.value.push(node.id);
      selectedNames.value.push(node.name);
      selectIds.value.push(node.id);

      return;
    }

    if (!node.children) {
      // 如果是取消选中，直接处理
      if (node.check) {
        node.check = false;
        // 从选中数组中移除
        const index = selectedIds.value.indexOf(node.id);
        if (index !== -1) {
          selectedIds.value.splice(index, 1);
          selectedNames.value.splice(index, 1);
        }
        const selectIndex = selectIds.value.indexOf(node.id);
        if (selectIndex !== -1) {
          selectIds.value.splice(selectIndex, 1);
        }
        // 检查父节点是否还有其他选中的子节点
        const parentNode = findParentNode(node);
        if (parentNode) {
          const hasCheckedChild = parentNode.children?.some((child) => {
            if (child.children) {
              return child.children.some((grandChild) => grandChild.check);
            }
            return child.check;
          });
          if (!hasCheckedChild) {
            parentNode.check = false;
          }
        }
        return;
      }
      // 如果是选中操作，检查是否允许选中
      if (isNodeDisabled(node)) {
        return; // 如果节点被禁用，不允许选中
      }
      // 切换当前节点状态
      node.check = true;
      // 找到父节点并设置其选中状态
      const parentNode = findParentNode(node);
      if (parentNode) {
        parentNode.check = true;
      }
      // 添加到选中数组
      selectedIds.value.push(node.id);
      selectedNames.value.push(node.name);
      selectIds.value.push(node.id);
      return;
    }

    brother.forEach((item) => {
      if (item.id === node.id) {
        item.check = true;
      } else {
        if (item.children) {
          item.check = false;
        }
        mapCancelCheck(item.children);
      }
    });
  }
  function handleCancel() {
    mapCancelCheck(options.value, true);
    selectIds.value = [];
    selectedIds.value = []; // 清空多选数组
    selectedNames.value = [];
  }
  // 分享协同岗特性主线版本下车，屏蔽前端入口
  const isShow930 = ref(false)

  async function handleCreate() {
    // 是否添加其它节点：为 true 时打开弹窗选择其它节点协同岗，不直接建群
    if (isShow930.value && isAddOtherNode.value ) {
      showCoopPicker.value = true;
      return;
    }
    await createGroup();
  }

  // 协同岗选择确认回调
  async function handleCoopPickerConfirm(persons: any[]) {
    selectedCoopPersons.value = persons;
    showCoopPicker.value = false;
    await createGroup();
  }

  // 核心建群逻辑
  async function createGroup() {
    loading.value = true;
    const { user } = PIMStore;
    console.log('user111111111', user);
    const res: any = await labelCreateGroup({
      ...info.value,
      idCard: user.idCard,
      ids: selectIds.value,
      // 其它节点协同岗人员ID列表
      coopUserIds: selectedCoopPersons.value.map((p) => p.id),
    });
    const { code, data, msg } = res;
    if (code === 0) {
      Message('创建成功');
      emit('success');
      props.onSuccess?.();
      closeWindow();
      setTimeout(() => {
        openChat({ groupId: data });
      }, 1000);
    } else {
      Message({
        duration: 4000, // 设置显示时间为3秒
        message: msg || '创建失败',
        type: 'error',
      });
    }
    loading.value = false;
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    :show-line="true"
    size="small"
    title="一键建群"
    @close-frame-box="closeWindow"
  >
    <div class="create-group-content">
      <div class="select-box">
        <div v-if="isChildren" class="selection">
          <div class="list sub1">
            <div
              v-for="sub1 in options"
              :key="sub1.id"
              class="item min-h50"
              :class="{
                'sub1-active': sub1.check,
                'sub1-select': sub1.check && !sub1.children,
                'sub1-disabled': !sub1.children && isNodeDisabled(sub1),
              }"
              @click.stop="handleSelect(options, sub1)"
            >
              {{ sub1.name }}
              <div v-if="sub1.check && !sub1.children" class="sign">✓</div>
              <div v-if="sub1.check" class="list sub2">
                <div
                  v-for="sub2 in sub1.children"
                  :key="sub2.id"
                  class="item min-h50"
                  :class="{
                    'sub2-active': sub2.check,
                    'sub2-select': sub2.check && !sub2.children,
                    'sub2-disabled': !sub2.children && isNodeDisabled(sub2),
                  }"
                  @click.stop="handleSelect(sub1.children, sub2)"
                >
                  {{ sub2.name }}
                  <div v-if="sub2.check && !sub2.children" class="sign">✓</div>
                  <div v-if="sub2.check" class="list sub3">
                    <div
                      v-for="sub3 in sub2.children"
                      :key="sub3.id"
                      class="item"
                      :class="{
                        'sub3-active': sub3.check,
                        'sub3-disabled': isNodeDisabled(sub3),
                      }"
                      @click.stop="handleSelect(sub2.children, sub3)"
                    >
                      <el-tooltip
                        class="box-item"
                        :content="sub3.name"
                        :effect="props.currentTheme === 'light' ? 'light' : 'dark'"
                        placement="top-start"
                      >
                        <div class="item-text">
                          {{ sub3.name }}
                        </div>
                      </el-tooltip>
                      <div v-if="sub3.check" class="sign">✓</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <!-- 只显示一级标签 -->
        <div v-if="!isChildren" class="onlyFirst">
          <div
            v-for="sub1 in options"
            :key="sub1.id"
            class="item"
            :class="{ selected: selectedIds.includes(sub1.id) }"
            :style="`background:${sub1.color}`"
            @click.stop="handleSelect(options, sub1)"
          >
            <div class="imgContent">
              <i :class="sub1.icon" :style="`color:${sub1.color}`"></i>
            </div>
            <div class="text" :class="{ selectText: selectedIds.includes(sub1.id) }">
              {{ sub1.name }}
            </div>
            <el-icon
              v-if="selectedIds.includes(sub1.id)"
              class="check-icon"
              color="#FFFFFF"
              name="checkmark"
              size="18"
            >
              <Select />
            </el-icon>
            <div class="whiteCircle"></div>
            <div v-if="selectedIds.includes(sub1.id)" class="rightCircle"></div>
          </div>
        </div>
      </div>

      <div class="footer">
        <el-checkbox v-if="isShow930" v-model="isAddOtherNode">是否添加其它节点</el-checkbox>
        <TdButton text="取消选中" type="normal" @click="handleCancel" />
        <TdButton
          :active="true"
          :disable="selectIds.length === 0"
          :is-light="true"
          :loading="loading"
          type="normal"
          @click="handleCreate"
        >
          确定
        </TdButton>
      </div>
    </div>

    <!-- 其它节点协同岗选择弹窗 -->
    <OtherNodeCoopPicker
      v-if="isShow930"
      v-model:visible="showCoopPicker"
      :confirm-loading="loading"
      @confirm="handleCoopPickerConfirm"
    />
  </TdFrameBox>
</template>

<style scoped lang="less">
  .create-group-content {
    width: 630px;
    height: 500px;
    padding: 24px 40px;

    .select-box {
      height: calc(100% - 60px);
      overflow: auto;
    }

    .selection {
      position: relative;
      display: flex;
      width: 100%;
      height: 100%;
      overflow: auto;

      .list {
        .item {
          padding: 0 4px;
          margin-bottom: 4px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
          font-weight: 400;
          // line-height: 50px;
          color: var(--text-color);
          cursor: pointer;
          word-break: break-all;
          .item-text {
            width: 80%;
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
          &.min-h50 {
            min-height: 50px;
          }
        }
        .sign {
          margin-left: 5px;
          color: var(--tabs-active-color);
        }

        .el-icon {
          margin-left: 4px;
        }

        .sub1-active {
          font-weight: 500;
          color: var(--tabs-active-color);
          background: var(--is-blur-bg);
        }

        .sub1-select {
          color: var(--tabs-active-color);
          background: var(--is-blur-bg);
        }

        .sub2-active {
          font-weight: 500;
          color: var(--tabs-active-color);
        }

        .sub2-select {
          color: var(--tabs-active-color);
        }

        .sub3-active {
          display: flex;
          align-items: center;
          font-weight: 500;
          color: var(--tabs-active-color);
          background: rgb(38 99 255 / 10%) !important;
          border: 1px solid var(--tabs-active-color) !important;
        }

        .sub1-disabled,
        .sub2-disabled,
        .sub3-disabled {
          color: var(--text-color-disabled, #999) !important;
          pointer-events: none;
          cursor: not-allowed !important;
          opacity: 0.5;
        }
      }

      .sub1 {
        width: 100px;
        overflow-y: auto;
        text-align: center;
        background: var(--is-blur-bg);
      }

      .sub2 {
        position: absolute;
        top: 0;
        left: 100px;
        width: 122px;
      }

      .sub3 {
        position: absolute;
        top: 0;
        left: 122px;
        width: 165px;

        .item {
          display: flex;
          justify-content: space-between;
          height: 29px;
          padding: 0 12px;
          margin: 11px 0;
          font-size: 14px;
          font-weight: 400;
          line-height: 29px;
          background: rgb(214 214 214 / 10%);
          border: 1px solid rgb(222 222 222 / 100%);
          border-radius: 4px;
        }
      }
    }

    .onlyFirst {
      display: flex;
      flex-wrap: wrap;
      // justify-content: center;
      align-items: center; /* 垂直居中 */
      justify-content: space-between;
      width: 100%;

      .item {
        position: relative;
        box-sizing: border-box;
        // background: url('../../assets/images/common/type_bg1.png') no-repeat center center;
        // background-size: cover;
        display: flex;
        align-items: center;
        justify-content: flex-start;
        width: calc(50% - 3vh); /* 调整间距 */
        padding: 2.5vh 0 2.5vh 1vw;
        margin: 1.5vh;
        overflow: hidden;
        color: #333;
        border-radius: 8px;

        .imgContent {
          display: flex;
          align-items: center;
          padding: 1.5vh;
          margin-right: 1vw;
          background: #fff;
          border-radius: 20px;
        }

        .whiteCircle {
          position: absolute;
          right: 2vw;
          bottom: -2vw;
          width: 4vw;
          height: 4vw;
          background: rgb(255 255 255 / 10%);
          border-radius: 4vw;
        }

        .rightCircle {
          position: absolute;
          top: -2vw;
          right: -2vw;
          width: 4vw;
          height: 4vw;
          background: rgb(255 255 255 / 20%);
          border-radius: 4vw;
        }

        .text {
          font-size: 16px;
          color: #fff;
          text-align: center;
        }

        .check-icon {
          position: absolute;
          top: 8px;
          right: 8px;
        }

        &.selected {
          // background: url('../../assets/images/common/type_bg2.png') no-repeat center center;
          // background-size: cover;
          // color: #FFFFFF !important;
        }

        &:deep(.selectText) {
          color: #fff !important;
        }

        &.disabled {
          pointer-events: none;
          cursor: not-allowed !important;
          opacity: 0.5;
        }
      }
    }
  }

  .footer {
    position: absolute;
    right: 64px;
    bottom: 41px;

    .td-button,
    .td-button.disabled {
      margin-left: 12px;
      color: var(--text-color) !important;
      background: var(--button-text-inner) !important;
      border: 1px solid var(--button-border-color) !important;

      :deep(.button-text) {
        color: var(--text-color) !important;
      }
    }

    .normal_active {
      color: #fff !important;
      background: var(--button-active-color) !important;
      border-color: var(--button-active-color) !important;
    }
  }
</style>
