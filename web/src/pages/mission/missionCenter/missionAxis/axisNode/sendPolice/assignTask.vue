<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { getLocationByAddress } from '@/api/lbsLocation';
  import { useEmitter, useI18n } from '@/hooks';

  import { debounce } from 'lodash-es';

  interface SearchAddress {
    address: string;
    filters: string;
    id: string;
    location: string;
    score: string;
  }

  const props = defineProps({
    item: {
      default: () => {},
      type: Object,
    },
  });
  const emit = defineEmits([
    'getTaskData',
    'isShowList',
    'deleteSelectedItem',
    'searchPersonAndOrg',
    'sendPolice',
  ]);
  defineExpose({
    onSendPoliceBack,
    resetData,
  });

  const { t } = useI18n();
  const disable = ref(true);
  const buttonType = ref('normal');
  const isShow = ref(false);
  const searchAddresses = ref<SearchAddress[]>([]);
  const searchKey = ref('');
  const alreadyClickItem = ref(false);
  const typeText = ref(t('mission.asignTask.dispose'));
  const location = ref({});
  const addressText = ref('');
  const timeInterval = ref();
  const taskLoading = ref(false);

  const selectItem = computed(() => {
    return props.item.selectItem || [];
  });
  const isSendPolice = computed(() => {
    return (
      unref(typeText) && unref(addressText) && unref(timeInterval) && unref(selectItem).length > 0
    );
  });

  // 人员与组织模糊搜索
  const search = debounce(() => {
    emit('searchPersonAndOrg', unref(searchKey));
  }, 500);
  const doDataEmit = debounce(() => {
    const { item } = props;
    const singleTaskData = {
      addressText: unref(addressText),
      position: {
        address: unref(addressText),
        latLon: item.position.latLon,
        location: unref(location),
      },
      searchKey: item.searchKey || '',
      selectItem: unref(selectItem),
      timeInterval: unref(timeInterval),
      typeText: unref(typeText),
    };
    emit('getTaskData', { taskData: singleTaskData });
  }, 500);

  // 黎曼地址搜索
  const searchAddress = debounce(async () => {
    if (unref(alreadyClickItem)) {
      alreadyClickItem.value = false;
      return;
    }
    if (unref(addressText) === '') {
      searchAddresses.value = [];
      return;
    }
    const param = { Address: unref(addressText) };
    const { code, data } = await getLocationByAddress(param);

    if (code === 0) {
      const arr: any[] = [];
      data?.forEach((item) => {
        const obj = {
          address: item.address,
          filters: item.filters,
          id: item.id,
          location: item.location,
          score: item.score,
        };
        arr.push(obj);
      });
      searchAddresses.value = arr;
    }
  });

  watch(
    () => props.item,
    () => {
      const { position } = props.item;
      if (position?.latLon) {
        location.value = {
          lat: position.latLon[1],
          lon: position.latLon[0],
        };
      }
      addressText.value = position?.address;
      timeInterval.value = props.item.timeInterval;
    },
    {
      immediate: true,
    },
  );
  watch(isSendPolice, (val) => {
    val && doDataEmit();
  });
  watch(selectItem, () => {
    searchKey.value = '';
  });
  watch(searchKey, search, { immediate: true });
  watch(typeText, doDataEmit);
  watch(addressText, searchAddress);
  watch(addressText, doDataEmit);

  onMounted(() => {
    useEmitter('changeButtonState', (item: any) => {
      disable.value = item.disable;
      buttonType.value = item.buttonType;
    });
  });

  function resetData() {
    typeText.value = t('mission.asignTask.dispose');
    addressText.value = '';
    searchKey.value = '';
    searchAddresses.value = [];
    location.value = {};
    timeInterval.value = '';
  }

  function sendPolice() {
    taskLoading.value = true;
    // 点击派警后按钮应该禁用 因为界面会关闭
    emit('sendPolice');
  }

  function onSendPoliceBack() {
    taskLoading.value = false;
  }

  function changeIsShow() {
    isShow.value = !isShow.value;
  }

  function focusTaskName() {
    emit('isShowList', false);
  }

  function focusAddress() {
    emit('isShowList', false);
  }

  // 删除派警人
  function deleteSelectItem(item) {
    emit('deleteSelectedItem', item);
  }

  // 选中搜索中的地址
  function chooseAddress(data) {
    alreadyClickItem.value = true;
    addressText.value = data.address;
    location.value = data.location;
    searchAddresses.value = [];
  }
</script>

<template>
  <div class="assign-task-content">
    <div class="task-police">
      <Icon class="police-icon" name="nav_person" />
      <div class="police-content">
        <div v-show="selectItem.length > 0" class="select-man">
          <div v-for="obj in selectItem" :key="obj.id" class="show-select-man">
            <span>{{ obj.name }}</span>
            <Icon class="delete" name="delete" @click="deleteSelectItem(obj)" />
          </div>
        </div>

        <TdInput
          v-model="searchKey"
          :placeholder="t('mission.timeline.unitNameSimplifiedPinNumber')"
          type="searchInput"
        />
      </div>
    </div>

    <template v-if="isShow">
      <div class="task-type">
        <Icon class="type-icon" name="mission_goal" />
        <TdInput v-model="typeText" :placeholder="typeText" @focus="focusTaskName" />
      </div>

      <div class="task-address">
        <Icon class="address-icon" name="category_position" />
        <TdInput v-model="addressText" :placeholder="item.position.address" @focus="focusAddress" />
        <div v-if="searchAddresses.length > 0" class="address-result">
          <ul class="search-droplist-box">
            <li
              v-for="_item in searchAddresses"
              :key="_item.id"
              class="search-droplist-item"
              @click="chooseAddress(_item)"
            >
              <span class="place">{{ _item.address }}</span>
            </li>
          </ul>
        </div>
      </div>
    </template>

    <div class="btn-group">
      <div class="event-more">
        <span class="more-event-info" @click="changeIsShow">
          {{ isShow ? t('common.putAway') : t('common.more') }}
        </span>
        <Icon class="more-icon" :name="isShow ? 'drop_up' : 'drop_down'" @click="changeIsShow" />
      </div>
      <TdButton
        class="left-btn"
        :disable="selectItem.length > 0 ? disable : true"
        :loading="taskLoading"
        :text="t('mission.asignTask.distribute')"
        :type="buttonType"
        @click="sendPolice"
      />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .assign-task-content {
    display: flex;
    flex-direction: column;

    .task-type {
      display: flex;
      align-items: center;
      margin-top: 10px;

      .type-icon {
        width: 18px;
        height: 18px;
        margin-right: 8px;
        fill: var(--text-title-second);
      }
    }

    .task-address {
      position: relative;
      display: flex;
      align-items: center;
      margin-top: 10px;

      .address-icon {
        width: 18px;
        height: 18px;
        margin-right: 8px;
        fill: var(--text-title-second);
      }

      .address-result {
        .search-droplist-box {
          position: absolute;
          bottom: -45px;
          left: 29px;
          z-index: 10;
          width: 200px;
          max-height: 200px;
          overflow-y: auto;

          .search-droplist-item {
            height: 2.5rem;
            padding: 0 1.25rem;
            clear: both;
            font-size: 1rem;
            line-height: 2.5rem;
            background: var(--text-color-button);

            .place {
              padding-top: 1px;
              color: var(--text-title-second);
            }

            &:hover {
              background: var(--text-default);
            }
          }
        }
      }
    }

    .task-police {
      display: flex;
      align-items: center;

      .police-icon {
        width: 18px;
        height: 18px;
        margin-right: 8px;
        fill: var(--text-title-second);
      }

      .police-content {
        display: flex;
        flex-direction: column;
        width: calc(100% - 26px);

        .select-man {
          display: flex;
          flex-wrap: wrap;
          width: 100%;
          max-height: 70px;
          margin-bottom: 10px;
          overflow: auto;
          background-color: rgb(50 152 226 / 10%);
          backdrop-filter: blur(20px);
          border: 1px solid rgb(255 255 255 / 20%);

          .show-select-man {
            display: flex;
            align-items: center;
            justify-content: center;
            height: 24px;
            padding: 0 8px;
            margin: 5px 0 5px 5px;
            cursor: pointer;
            background-color: var(--button-color-guide-default);

            .delete {
              display: inline-flex;
              width: 16px;
              height: 16px;
              margin-left: 5px;
            }

            span {
              max-width: 17rem;
              overflow: hidden;
              line-height: 24px;
              color: var(--text-color-button);
              text-overflow: ellipsis;
              white-space: nowrap;
              vertical-align: middle;
            }
          }

          .search-key {
            display: inline-flex;
            flex-grow: 1;
            min-width: 80px;
            color: var(--text-title-second);
            background-color: var(--text-color-button);
          }
        }
      }
    }

    .btn-group {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 10px 0;

      .event-more {
        display: flex;
        align-items: center;
        margin-right: 8px;
        cursor: pointer;

        .more-event-info {
          font-size: var(--font-size-small);
          color: var(--text-title-second);
          letter-spacing: 0.5px;
        }

        .more-icon {
          width: 13px;
          height: 13px;
          margin-left: 5px;
          fill: var(--text-title-second);
        }
      }

      .left-btn {
        height: 24px;
        padding: 0 10px !important;
      }
    }
  }
</style>
