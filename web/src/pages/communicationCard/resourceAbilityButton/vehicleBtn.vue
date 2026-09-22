<script setup lang="ts">
  import { computed } from 'vue';

  import { useI18n } from '@/hooks';
  import { editVehicleIllusionPopup } from '@/pages/bigScreen/planSpecial/vehicle';
  import { useVehicleStore } from '@/store';

  const props = defineProps<{
    info: any;
  }>();

  const { t } = useI18n();

  const vehicleStore = useVehicleStore();

  const showBtn = computed(() => {
    if (vehicleStore.underProtection) {
      return false;
    }
    return vehicleStore.vehicleEquipmentType.includes(props.info.category);
  });

  // 车辆幻化
  function handleIllusion() {
    editVehicleIllusionPopup(props.info);
  }
</script>

<template>
  <div v-if="showBtn" class="vehicle-btn">
    <TdTooltip :content="t('resource.resourceTab.vehicle')" placement="top">
      <TdButton icon-name="illusion" type="radioSpecial" @click.stop="handleIllusion" />
    </TdTooltip>
  </div>
</template>

<style scoped lang="less"></style>
