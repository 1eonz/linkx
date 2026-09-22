<script lang="ts" setup>
  import { computed, ref, watch, watchEffect } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useI18n } from '@/hooks';
  import { mapManager } from '@/plugins/map';
  import { useAddressStore } from '@/store';
  import { addSearchLayer } from '@/utils/addLayerUtil';

  import { debounce } from 'lodash-es';

  import CollectPopup from './popup/collectPopup.vue';

  const props = defineProps({
    mapId: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const addressStore = useAddressStore();

  const keyword = ref('');
  const checkedId = ref('');
  const resultData = ref<any[]>([]);
  const currentPage = ref(1);
  const total = ref(0);
  const showEmpty = ref(false);

  const showResult = computed(() => {
    const length = resultData.value.length;
    return Boolean(length);
  });

  const getSearchData = debounce(handleSearch, 800);

  watch(keyword, (val) => {
    if (val) {
      getSearchData();
    } else {
      currentPage.value = 1;
      total.value = 0;
      clearInput();
    }
  });

  watchEffect(() => {
    const addressData = addressStore.addressData;
    resultData.value.forEach((item) => {
      const has = addressData.findIndex((i) => {
        return i.nid === item.nid;
      });
      if (has !== -1) {
        item.favoriteId = addressData[has].id;
      }
      item.has = has !== -1;
    });
  });

  // 搜索
  async function handleSearch() {
    clearInput();
    addressStore.initAddressData();
    const addressData = addressStore.addressData;
    // 关键词搜索
    const map = mapManager.get(props.mapId);
    const { count, pois } = await map.queryAllSearch(keyword.value, currentPage.value);
    if (!count) {
      showEmpty.value = true;
      return;
    }
    total.value = Number(count);
    const now = Date.now();
    pois.forEach((item, index) => {
      const { address, city, district, location, name, nid, phone, province, tel, type, typeName } =
        item;
      const position = location?.split(',');
      const region = province + city + district || '';
      const has = addressData.findIndex((i) => i.nid === nid);

      resultData.value.push({
        address: region + address,
        has: has !== -1,
        id: addressData[has]?.id || nid || now + index,
        location,
        name,
        nid,
        number: index + 1,
        phone: phone || tel,
        position,
        remark: addressData[has]?.remark,
        typeName: addressData[has]?.typeName || typeName || type,
      });
    });

    addSearchLayer({
      data: resultData.value,
      layerId: 'searchResource',
      map,
    });
  }

  function handleCurrentChange(num) {
    currentPage.value = num;
    handleSearch();
  }

  function handleCollected(item) {
    const { favoriteId, has, nid } = item;
    if (has) {
      addressStore.delAddressData(favoriteId);
      return;
    }

    const cid = `${nid}CollectPopup`;
    Dialog({
      cid,
      content: CollectPopup,
      data: {
        cid,
        data: item,
      },
      shade: false,
    });
  }

  // 选中搜索结果
  function handleClick(item) {
    const map = mapManager.get(props.mapId);
    map.closeCustomPopup();
    const { number, position } = item;
    checkedId.value = checkedId.value === number ? '' : number;
    if (checkedId.value) {
      const maxZoom = Number(appConfig.settingData.MAX_ZOOM);
      const zoom = Math.min(maxZoom, 17);
      map.setCenter(position, zoom);
      map.addMarkerCustomPopup('searchResource', item);
    }
  }

  // 初始化
  function clearInput() {
    const map = mapManager.get(props.mapId);
    map?.closeCustomPopup();
    map?.deleteLayer(['searchResource']);
    resultData.value = [];
    checkedId.value = '';
    showEmpty.value = false;
  }
</script>

<template>
  <div class="map-search">
    <div class="search-input">
      <TdInput
        v-model="keyword"
        :placeholder="t('resource.address.searchLocation')"
        type="searchInput"
      />
    </div>
    <!-- 结果 -->
    <div v-if="showResult" class="search-result ground-glass">
      <div
        v-for="item in resultData"
        :key="item.val"
        class="result-item"
        :class="{ checked: checkedId === item.number }"
        @click.stop="handleClick(item)"
      >
        <div class="text">
          <div class="number">{{ item.number }}</div>
          <TdTooltip :content="item.name" placement="top">
            <div class="name">{{ item.name }}</div>
          </TdTooltip>
          <Icon
            class="collect"
            :name="item.has ? 'btn_uncollected' : 'btn_collected'"
            @click.stop="handleCollected(item)"
          />
        </div>
        <TdTooltip :content="item.address" placement="top">
          <div class="address">{{ item.address }}</div>
        </TdTooltip>
      </div>

      <ElPagination
        v-model:current-page="currentPage"
        background
        class="search-pagination"
        layout="prev, pager, next"
        :pager-count="3"
        size="small"
        :total="total"
        @current-change="handleCurrentChange"
      />
    </div>
    <TdEmpty v-if="showEmpty" class="search-empty ground-glass">
      {{ t('resource.address.keywordSearch') }}
    </TdEmpty>
  </div>
</template>

<style lang="less" scoped>
  @import url('@/styles/mixin.less');

  .map-search {
    position: absolute;
    top: 100px;
    right: 94px;
    z-index: 1;

    .search-input {
      display: flex;
      align-content: center;
      width: 310px;
      background: rgb(6 41 74 / 60%);
      backdrop-filter: blur(8px);
    }

    .search-result {
      width: 310px;
      padding-top: 10px;
      margin-top: 16px;

      .result-item {
        padding: 4px 10px;
        margin-bottom: 4px;
        cursor: pointer;

        .text {
          display: flex;
          align-items: center;
          height: 22px;

          .number {
            width: 16px;
            height: 16px;
            margin-right: 4px;
            font-size: 10px;
            font-weight: bold;
            line-height: 16px;
            color: var(--text-title-first);
            text-align: center;
            background: url('@/assets/images/marker/simple_marker_blue.png') no-repeat center;
            background-size: 16px 16px;
          }

          .name {
            width: 250px;
            font-size: 14px;
            font-weight: bold;
            color: var(--text-title-first);
            .ellipsis1();
          }

          .collect {
            width: 16px;
            height: 16px;
            cursor: pointer;
          }
        }

        .address {
          margin-left: 20px;
          font-size: 12px;
          line-height: 16px;
          color: rgb(153 206 251 / 100%);
          word-break: break-all;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .address {
            color: var(--text-title-first);
          }
        }
      }

      .checked {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

        .text {
          .address {
            color: var(--text-title-first);
          }
        }
      }
    }

    .search-pagination {
      justify-content: center;
      padding: 12px 0 16px;
    }

    .search-empty {
      width: 310px;
      height: 560px;
      padding-top: 10px;
      margin-top: 10px;
    }
  }
</style>
