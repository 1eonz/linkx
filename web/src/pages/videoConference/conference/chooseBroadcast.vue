<script lang="ts" setup>
  import type { ConfMember } from '@/pages/types/conference';

  import { computed, onMounted, ref, watch } from 'vue';

  import { Message } from '@/components/Message';
  import { useEmitter, useI18n } from '@/hooks';
  import { getResourceAvatar } from '@/pages/resource/resourceHelper';
  import { confFunc } from '@/plugins/mspPlayer';
  import { useConferenceStore } from '@/store';

  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();
  const conferenceStore = useConferenceStore();
  const searchVal = ref('');
  const checkedMember = ref<string[]>([]);
  const layouts = [
    ['1_1'],
    ['2_1', '2_2', '2_6', '2_7'],
    ['3_1', '3_2', '3_3', '3_4', '3_6', '3_8'],
    ['4_1', '4_2', '4_3', '4_4', '4_5', '4_7'],
    ['5_1', '5_2', '5_3', '5_4', '5_6', '5_7'],
    ['6_1', '6_2', '6_3', '6_5', '6_7', '6_8'],
    ['7_1', '7_2', '7_3', '7_4', '7_6'],
    ['8_1', '8_2', '8_3', '8_4', '8_6'],
    ['9_1'],
    ['10_1', '10_2', '10_3', '10_4', '10_5', '10_6'],
    ['13_1', '13_2', '13_3', '13_4', '13_5'],
    ['13_1', '13_2', '13_3', '13_4', '13_5'],
    ['13_1', '13_2', '13_3', '13_4', '13_5'],
    ['15_2'],
    ['15_2'],
    ['16_1'],
    ['18_2'],
    ['18_2'],
    ['20_1'],
    ['20_1'],
  ];
  const selectLayout = ref('');

  const showMember = computed(() => {
    const ret: ConfMember[] = [];
    conferenceStore.confMember.forEach((item) => {
      const text = searchVal.value;
      const { name, number } = item;

      if (text !== '' && !number.includes(text) && !name.includes(text)) return;

      const checked = checkedMember.value.includes(number);
      ret.push({ ...item, checked });
    });
    return ret;
  });
  const showLayouts = computed(() => {
    const len = checkedMember.value.length;
    if (len === 0) {
      return [];
    } else if (len <= 25) {
      const a = layouts[len - 1];
      const b = conferenceStore.supportCPModes;
      const ret = a.filter((v) => b.includes(v));
      return ret;
    } else {
      return layouts[24];
    }
  });
  const checkedAll = computed(() => {
    const memberlen = showMember.value.length;
    return Boolean(memberlen && memberlen === checkedMember.value.length);
  });
  const indeterminate = computed(() => {
    const len = checkedMember.value.length;
    return len > 0 && len < showMember.value.length;
  });

  watch(conferenceStore.confMember, (val) => {
    getCheckedVal(val);
  });

  onMounted(() => {
    confFunc.queryContinuousPresenceInfo(conferenceStore.confer.conferenceId);

    const { broadcastMember, flexType, setBroadcastOrWatch, setFlexType } = conferenceStore;
    selectLayout.value = flexType.replace('CP_', '');
    broadcastMember.forEach((item) => {
      checkedMember.value.push(item);
    });

    useEmitter('OnEndConfSuccess', () => {
      cancel();
    });

    useEmitter('OnBroadcastMixPictureSuccess', () => {
      const type = `CP_${selectLayout.value}`;
      setFlexType(type);
      setBroadcastOrWatch('broadcast');
      cancel();
    });

    useEmitter('OnWatchMixPictureSuccess', () => {
      const type = `CP_${selectLayout.value}`;
      setFlexType(type);
      setBroadcastOrWatch('watch');
      cancel();
    });
    getCheckedVal(conferenceStore.confMember);
  });

  function getCheckedVal(val) {
    const numbers = new Set(val.map((item) => item.number));
    checkedMember.value = checkedMember.value.filter((item) => numbers.has(item));
  }
  function cancel() {
    emit('closeDialog');
  }
  function confirm(type) {
    if (checkedMember.value.length === 0) {
      Message(t('videoConference.conferenceMember'));
      return;
    }
    const { conferenceId } = conferenceStore.confer;
    const flexType = `CP_${selectLayout.value}`;
    const memberInfos: any[] = [];

    checkedMember.value.forEach((number) => {
      memberInfos.push({ number });
    });
    const params: [string, any[], string] = [conferenceId, memberInfos, flexType];
    if (type === 'broadcast') {
      confFunc.broadcastMixPicture(...params);
    } else if (type === 'watch') {
      confFunc.watchMixPicture(...params);
    }
  }
  function checkMember(data) {
    const { number } = data;
    const index = checkedMember.value.indexOf(number);
    if (index === -1) {
      checkedMember.value.push(number);
    } else {
      checkedMember.value.splice(index, 1);
    }
    defaultLayout();
  }
  function changeLayout(data) {
    selectLayout.value = data;
  }
  function checkAll() {
    if (checkedAll.value) {
      checkedMember.value = [];
    } else {
      checkedMember.value = showMember.value.map((item) => {
        return item.number;
      });
      defaultLayout();
    }
  }
  function defaultLayout() {
    const len = checkedMember.value.length;
    if (len) {
      selectLayout.value = layouts[len - 1][0];
    }
  }
  function showName(data) {
    const { category, name, number } = data;
    if (category === 'group') return name;
    return `${name}(${number})`;
  }
</script>

<template>
  <TdFrameBox
    class="choose-broadcast"
    :dragger="true"
    size="small"
    :title="t('videoConference.conferenceButton.select')"
    @close-frame-box="cancel"
  >
    <div class="content">
      <div class="choose-item left-member">
        <p class="title">
          {{ t('resource.group.peopleSelectShow') }}：
          {{ checkedMember.length }}
        </p>
        <div class="left-content">
          <div class="search">
            <TdInput
              v-model="searchVal"
              class="td-input"
              :placeholder="t('resource.policeResourceData.searchPlaceHolder')"
              type="searchInput"
            />
          </div>

          <div class="member-list">
            <div v-if="showMember.length > 0" class="member" @click.stop="checkAll">
              <TdCheckbox v-model="checkedAll" class="checkbox" :indeterminate="indeterminate" />
              <span class="choose-item-label">
                <Icon class="folder" name="folder" />
                <span class="name">
                  {{ t('common.tdcomp.allChoose') }}
                </span>
              </span>
            </div>

            <div
              v-for="item in showMember"
              :key="item.number"
              class="member"
              @click.stop="checkMember(item)"
            >
              <TdCheckbox v-model="item.checked" class="checkbox" />
              <span class="choose-item-label">
                <Icon class="icon" :name="getResourceAvatar(item)" prefix="tree" />
                <TdTooltip :content="showName(item)">
                  <span class="name"> {{ showName(item) }} </span>
                </TdTooltip>
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="choose-item right-broadcast">
        <p class="title">{{ t('videoConference.chooseLayout') }}</p>
        <div class="layout">
          <div
            v-for="item in showLayouts"
            :key="item"
            class="layout-item"
            :class="{ 'layout-item_1': showLayouts.length === 1 }"
            @click.stop="changeLayout(item)"
          >
            <Icon
              class="layout-icon"
              :fill="selectLayout === item ? '#00C2FF' : '#387eaf'"
              :name="`CP_${item}`"
              prefix="broadcast"
            />
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-btn">
      <TdButton
        class="cancel-btn"
        :text="t('videoConference.confFunc.watch')"
        type="normal"
        @click.stop="confirm('watch')"
      />
      <TdButton
        :text="t('videoConference.confFunc.broadcast')"
        type="normal"
        @click.stop="confirm('broadcast')"
      />
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .choose-broadcast {
    :deep(.frame-box-container) {
      padding: 15px 10px;
    }

    .content {
      display: flex;
      justify-content: space-between;
      margin-bottom: 20px;

      .choose-item {
        .title {
          margin-bottom: 8px;
          font-size: 14px;
          line-height: 24px;
        }

        & > div {
          width: 300px;
          height: 450px;
          padding: 10px;
        }

        .left-content {
          background: rgb(173 204 240 / 7%);
          border: 1px solid rgb(255 255 255 / 20%);
        }
      }

      .left-member {
        .search {
          position: relative;
          display: flex;
          justify-content: space-between;
          width: 100%;
          height: 32px;
          margin-bottom: 11px;
        }

        .member-list {
          max-height: 370px;
          overflow: hidden auto;

          .choose-item-label {
            position: relative;
            z-index: 0;
            display: flex;
            align-items: center;
            padding-left: 20px;

            .folder {
              width: 16px;
              height: 16px;
            }

            .icon {
              width: 18px;
              height: 18px;
            }
          }

          .member {
            position: relative;
            display: flex;
            align-items: center;
            margin-bottom: 5px;

            .checkbox {
              position: absolute;
              left: 0;
            }

            .name {
              z-index: 1;
              width: 240px;
              padding-left: 4px;
              overflow: hidden;
              font-size: 14px;
              text-overflow: ellipsis;
              white-space: nowrap;
              cursor: pointer;
            }
          }
        }
      }

      .right-broadcast {
        .layout {
          display: flex;
          flex-wrap: wrap;
          background: rgb(173 204 240 / 7%);
          border: 1px solid rgb(255 255 255 / 20%);

          &-item {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 50%;
          }

          &-item_1 {
            width: 100%;
          }

          &-icon {
            width: 72px;
            height: 68px;
            cursor: pointer;
          }
        }
      }
    }

    .bottom-btn {
      display: flex;
      justify-content: center;

      .cancel-btn {
        margin-right: 40px;
      }
    }
  }
</style>
