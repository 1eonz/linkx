<script setup lang="ts">
  import { computed, watch } from 'vue';

  import { getLang } from '@/locales';
  import { useMissionStore } from '@/store';
  import { getIp } from '@/utils';

  import MissionDetailsItem from './missionDetailsItem.vue';

  const props = withDefaults(
    defineProps<{
      info: any;
    }>(),
    {
      info: {},
    },
  );

  const { flowConfig } = useMissionStore();

  watch(
    () => props.info,
    (val) => {
      console.log(val, flowConfig.get(val.type));
    },
  );

  const detailsList = computed(() => {
    const lang = getLang() === 'en' ? 'en' : 'zh';
    const ip = `${getIp()}/iap`;
    const { info } = props;
    const config = flowConfig.get(info.type);
    if (!config) {
      return [];
    }

    const { i18nConf, view } = flowConfig.get(info.type);
    console.log(info.type, view);

    const list: any = [];
    view.definition.forEach((v) => {
      const { typeReference } = v.extensions;
      const ret: any = {
        label: i18nConf[lang][v.i18n],
        value: info.payload[v.name],
      };

      if (typeReference) {
        const { reference, type } = typeReference;

        if (type === 'HIDE') {
          return;
        }

        switch (type) {
          case 'ENUM': {
            ret.value = i18nConf[lang][reference[ret.value]];
            break;
          }
          case 'JSON': {
            if (!ret.value) {
              return;
            }
            const children: any = [];
            Object.keys(ret.value).forEach((field) => {
              const current = reference[field] || {};
              const { i18n, type } = current;
              const val = ret.value[field];
              const obj = {
                label: i18nConf[lang][i18n],
                type,
                value: type === 'PICTURE' ? ip + val : val,
              };
              if (type === 'ENUM') {
                obj.value = i18nConf[lang][current.reference[val]];
              }
              // 后端如果返回的label在配置中找不到，则由前端过滤处理掉
              if (obj.label) {
                children.push(obj);
              }
            });
            ret.children = children;
            break;
          }
        }
      }

      if (ret.value || ret.children) {
        list.push(ret);
      }
    });

    return list;
  });
</script>

<template>
  <div class="mission-details-list">
    <div v-for="item in detailsList" :key="item.label" class="common-list-item">
      <div v-if="item.children" class="inner-item">
        <template v-for="child in item.children" :key="child.label">
          <MissionDetailsItem :info="child" />
        </template>
      </div>
      <MissionDetailsItem v-else :info="item" />
    </div>
  </div>
</template>

<style scoped lang="less">
  .mission-details-list {
    max-height: 400px;
    padding: 16px 0;
    overflow: auto;
  }
</style>
