<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { useI18n } from '@/hooks';
  import { useCommunicationStore } from '@/store';

  import { throttle } from 'lodash-es';

  const props = defineProps({
    groupInfo: {
      default: () => {},
      type: Object,
    },
    personList: {
      required: true,
      type: Array as PropType<any[]>,
    },
  });

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const speakerIsdn = ref('');
  const end = ref(10);

  const listData = computed(() => {
    return props.personList.slice(0, unref(end));
  });

  watch(communicationStore.comm, handleComm);

  onMounted(() => {
    handleComm(true);
  });

  function handleComm(init?: boolean) {
    const { comm } = communicationStore;
    const { groupId } = props.groupInfo;

    let isdn = groupId;
    if (!init) {
      const { opt, type } = comm.updateInfo;
      isdn = comm.updateInfo?.isdn;
      if (type !== 'group' || !isdn || isdn !== groupId) {
        return;
      }
      if (opt === 'delete') {
        speakerIsdn.value = '';
        return;
      }
    }

    speakerIsdn.value = comm[isdn]?.group?.speaker;
  }

  /**
   * 滚动加载更多
   */
  const handleScroll = throttle((e) => {
    const el = e.srcElement;
    const scrollTop = el.scrollTop; // 滚动高度
    const scrollHeight = el.scrollHeight; // 内容高度
    const clientHeight = el.clientHeight; // 可见高度

    if (scrollTop + clientHeight >= scrollHeight - 1) {
      const len = unref(listData).length;
      const { personList } = props;
      if (len < personList.length) {
        end.value += 10;
      }
    }
  }, 200);
</script>

<template>
  <!-- 群组人员列表 -->
  <div class="group-person" @scroll="handleScroll">
    <div v-for="item in listData" :key="item.index" class="person-item">
      <div class="person-left">
        <TdAvatar :info="item" />
        <span class="name">
          {{ item.name }}
        </span>
        <span class="isdn"> ({{ item.isdn }})</span>
        <span v-show="item.onlineStatus === 'offline'" class="name">
          ( {{ t('resource.resourcePublic.unableReceiveMessages') }} )
        </span>
      </div>
      <div class="person-right">
        <span v-if="speakerIsdn === item.isdn" class="roles">
          <Icon class="sub-call" name="speaking" prefix="bigScreen" />
        </span>
        <span class="roles"> {{ item.roleName }}</span>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .group-person {
    max-height: 205px;
    padding-top: 10px;
    overflow-y: scroll;

    .person-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 28px;

      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
      }

      .person-left {
        display: flex;
        align-items: center;

        .name {
          margin-left: 4px;
          font-size: 14px;
          color: var(--text-default);
          cursor: pointer;
        }

        .isdn {
          font-size: 14px;
          color: var(--text-default);
        }
      }

      .person-right {
        display: flex;
        align-items: center;
        justify-content: right;

        .roles {
          margin-left: 10px;
          color: var(--text-default);

          .sub-call {
            width: 15px;
            height: 15px;
          }
        }
      }
    }
  }
</style>
