<script setup lang="ts">
  import { computed, onMounted, ref, watch } from 'vue';

  import { useI18n } from '@/hooks';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';

  const props = defineProps<{
    navId?: string;
    showPim?: boolean;
    showResource?: any[];
  }>();
  const emit = defineEmits(['click']);
  const { t } = useI18n();

  const navOptions = computed(() => {
    const arr: any = [...getResourceTypes()];
    const all = {
      icon: 'all',
      id: 'all',
      name: t('resource.resourceType.all'),
      navShow: true,
      show: true,
      total: 0,
    };
    if (props.showPim) {
      arr.splice(1, 0, all);
    } else {
      arr.unshift(all);
    }

    return arr;
  });
  const activeId = ref(navOptions.value[0].id);

  watch(
    () => props.navId,
    (val) => {
      if (val) {
        activeId.value = val;
      }
    },
  );

  onMounted(() => {
    init();
  });

  function init() {
    const { showResource } = props;
    if (showResource?.length) {
      navOptions.value.forEach((item) => {
        if (item.show && item.id !== 'all') {
          item.show = showResource.includes(item.type);
        }
      });
    }
  }

  function handleClick(data) {
    activeId.value = data.id;
    emit('click', data.id);
  }
</script>

<template>
  <div class="tree-nav">
    <template v-for="item in navOptions" :key="item.id">
      <div
        v-if="item.show && item.navShow"
        class="nav-item"
        :class="{ 'nav-active': activeId === item.id }"
        @click.stop="handleClick(item)"
      >
        <TdTooltip :content="item.name" placement="right">
          <Icon class="icon" fill="rgba(153, 206, 251, 1)" :name="`nav_${item.icon}`" />
        </TdTooltip>
      </div>
    </template>
  </div>
</template>

<style scoped lang="less">
  .tree-nav {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 34px;
    height: 100%;
    padding: 5px 0;
    background: rgb(0 0 0 / 40%);

    .nav-item {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      margin-bottom: 16px;
      font-size: 16px;
      font-weight: 400;
      word-wrap: break-word;
      cursor: pointer;

      .icon {
        width: 16px;
        height: 16px;
        fill: rgb(153 206 251 / 100%);
      }
    }

    .nav-item:nth-of-type(1) {
      .icon {
        // width: 24px;
        // height: 24px;
      }
    }

    .nav-active {
      background: linear-gradient(90deg, rgb(26 255 251 / 10%) 0%, rgb(26 255 251 / 0%) 100%);

      .icon {
        fill: rgb(26 255 251 / 100%);
      }

      &::before {
        position: absolute;
        top: 0;
        left: 0;
        width: 4px;
        height: 100%;
        content: '';
        background-color: rgb(26 255 251 / 100%);
      }
    }
  }
</style>
