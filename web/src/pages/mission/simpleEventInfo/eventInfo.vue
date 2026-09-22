<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';

  import { useI18n } from '@/hooks';

  import EventListType from './eventListType.vue';

  const props = defineProps({
    eventStatus: {
      default: '',
      type: String,
    },
    info: {
      default: () => {},
      type: Object,
    },
    type: {
      default: 'mission',
      type: String,
    },
  });

  const { t } = useI18n();
  const componentData = ref<any>({});
  const editing = ref(false);
  const highLightKeys = ref<any[]>([]);
  const resultString = ref('');
  const histroyData = ref('');

  const title = computed(() => {
    return componentData.value?.typeFullName || t('event.eventCenter.unknown');
  });
  const showDetail = computed(() => {
    const { details } = componentData.value;
    if (details) {
      return details.replaceAll(/(<(.[^>]*)>)|(&nbsp;)/g, '');
    }
    return t('event.eventCenter.unknown');
  });

  watch(
    () => props.info,
    (val) => {
      componentData.value = val;
      resultString.value = '';
      highLightKeys.value = [];
      const { details } = componentData.value;
      highLightWord(details);
      histroyData.value = details ? details.replaceAll(/(<(.[^>]*)>)|(&nbsp;)/g, '') : '';
    },
    {
      deep: true,
      immediate: true,
    },
  );

  onMounted(() => {});

  function highLightWord(description) {
    const desc = description;
    if (!description || !description.includes('em')) {
      resultString.value = description;
      return;
    }
    const descArrayTmp = desc.split('<em>');
    let descArray: any = [];
    descArrayTmp.forEach((string) => {
      descArray = [...descArray, ...string.split('</em>')];
    });
    for (const [i, element] of descArray.entries()) {
      highLightKeys.value.push({
        content: element,
        filterText: i % 2 === 1 ? element : '',
      });
    }
    highLightKeys.value.forEach((item) => {
      const replaceReg = new RegExp(item.filterText, 'g');
      // 高亮替换v-html值
      const replaceString = `<span style="color:#1F90F2;">${item.filterText}</span>`;
      // 开始替换
      item.content = item.content.replace(replaceReg, replaceString);
      resultString.value += item.content;
    });
  }
  function edit() {
    editing.value = true;
  }
</script>

<template>
  <div class="event-info common-list-item">
    <div class="event-type">
      <EventListType
        v-show="type === 'simpleEvent'"
        :event-status="eventStatus"
        style="margin-right: 20px"
        type="simpleEvent"
      />
      <span class="event-title" :class="type === 'event' ? 'event-tittle' : ''">
        {{ title }}
      </span>
    </div>
    <TdTooltip :content="showDetail">
      <span class="words-show" @click="edit">
        {{ showDetail }}
      </span>
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .tips-tittle {
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--button-color-guide-default);

    .tips-icon {
      width: 15px;
      height: 13px;
      margin-left: 3px;
    }
  }

  .event-info {
    display: flex;
    align-items: flex-start;
    width: 100%;
    height: fit-content;
    max-height: 140px;
    margin-right: 4px;

    .event-type {
      position: relative;
      display: flex;
      align-items: center;

      .event-title {
        display: inline-block;
        min-width: 56px;
        max-width: 70px;
        margin-right: 4px;
        color: var(--text-title-second);
      }

      .info-button {
        position: absolute;
        right: 5px;
        display: flex;
        height: 100%;
      }

      .el-icon-loading {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 24px;
        height: 24px;
      }

      .operate-evidence-icon {
        width: 20px;
        height: 20px;
        cursor: pointer;
      }
    }

    .words-show {
      display: inline-block;
      width: 200px;
      color: var(--text-color-normal);
      word-break: break-all;
      .ellipsis(2);
    }

    .event-more {
      display: flex;
      flex-direction: row-reverse;
      margin-right: 5px;
      margin-bottom: 5px;
      cursor: pointer;

      .more-event-info {
        font-size: var(--font-size-small);
        color: var(--text-color-content);
        letter-spacing: 0.5px;
      }

      .more-icon {
        width: 13px;
        height: 13px;
        margin-top: 5px;
        margin-left: 5px;
        fill: var(--icon-color-drop-down);
        transform: rotate(-90deg);
      }
    }
  }
</style>
