<script lang="ts" setup>
  import { computed } from 'vue';

  import { useI18n } from '@/hooks';

  const props = defineProps({
    eventStatus: {
      default: '',
      type: String,
    },
    type: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();

  const eventStatusColor = computed(() => {
    switch (props.eventStatus) {
      case t('mission.timeline.disposal'): {
        return '#ff8f0a';
      }
      case t('mission.timeline.disposalCompleted'): {
        return '#14b58d';
      }
      case t('mission.timeline.disposed'): {
        return '#1E9FFF';
      }
      case t('mission.timeline.feedbacked'): {
        return '#29b5c7';
      }
      case t('mission.timeline.important'): {
        return '#F15050';
      }
      case t('mission.timeline.notDisposed'): {
        return '#F15050';
      }
      default: {
        return '#8699ac';
      }
    }
  });
</script>

<template>
  <div
    class="event-list-type"
    :class="type === 'simpleEvent' ? 'simple-list-type' : 'other-list-type'"
    :style="{ 'background-color': eventStatusColor }"
  >
    {{ eventStatus }}
  </div>
</template>

<style lang="less" scoped>
  .simple-list-type {
    padding-right: 5px;
    padding-left: 5px;
  }

  .other-list-type {
    width: 100px;
  }

  .event-list-type {
    height: 25px;
    line-height: 25px;
    text-align: center;
    border-radius: 4px;
  }

  .in-processing {
    background-color: var(--background-yellow);
  }

  .untreated {
    background-color: var(--background-red);
  }
</style>
