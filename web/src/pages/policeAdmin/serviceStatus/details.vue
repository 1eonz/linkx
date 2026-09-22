<script setup lang="ts">
  import { computed, onMounted } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useCreateApp, useI18n } from '@/hooks';
  import BottomMap from '@/pages/home/bottomMap.vue';
  import { locationImg } from '@/pages/map/marker/base64Marker';
  import serviceStatusMarker from '@/pages/map/marker/serviceStatusMarker.vue';
  import { mapIsReady, mapManager } from '@/plugins/map';
  import { domConvertsToBase64, getIp } from '@/utils';

  import { addressStr, timeStr } from './common';

  const props = defineProps<{
    cid: string;
    info: any;
    person: any;
  }>();
  const { t } = useI18n();
  const mapId = 'serviceStatusDetailsMap';

  const imgUrl = computed(() => {
    return `${getIp()}/iap${props.info.imageUrl}`;
  });

  onMounted(() => {
    addPoint();
  });

  function closeCard() {
    Dialog(props.cid as string)?.close();
  }
  async function addPoint() {
    await mapIsReady(mapId);
    const { info } = props;
    const mapObj = mapManager.get(mapId);
    const [point] = info.location?.coordinates || {};
    let position: string[] = [];

    if (point) {
      position = [point.lon, point.lat];
    }

    mapObj.addLayer({
      data: [{ ...info, position, statusName: addressStr(info.status) }],
      image: locationImg,
      isCluster: false,
      layerId: 'serviceStatusDetails',
      renderer(data, options) {
        const { layerConfig, map, mapType } = options || {};

        const getUrl = () => {
          const parent = document.createElement('div');
          const instance = useCreateApp(serviceStatusMarker, { data, image: locationImg });
          const el = instance.mount(parent).$el;
          const url = domConvertsToBase64(el);
          return url;
        };

        switch (mapType) {
          case 'amap': {
            return `<div style="display: flex; position: relative; justify-content: center; text-align: center">
            <img src="${locationImg}" alt="" style="width: 29px; height: 29px" />
            <div
              style="
                width: 54px;
                height: 16px;
                position: absolute;
                bottom: -16px;
                left: 50%;
                margin-left: -27px;
                color: #fff;
                line-height: 16px;
                text-align: center;
                font-size: 10px;
                display: flex;
                align-items: center;
                justify-content: center;
                background-color: rgba(50, 153, 227, 1);
              "
            >
              ${addressStr(data.status)}
            </div>
          </div>`;
          }
          case 'arcgis': {
            layerConfig.fields.push({
              alias: 'statusName',
              name: 'statusName',
              type: 'string',
            });
            layerConfig.labelingInfo = [
              {
                allowOverrun: false,
                deconflictionStrategy: 'static',
                labelExpressionInfo: {
                  expression: '$feature.statusName',
                },
                labelPlacement: 'center-center',
                symbol: {
                  color: '#fff',
                  font: {
                    size: '12px',
                    weight: 'bold',
                  },
                  haloColor: 'rgba(50, 153, 227, 1)',
                  haloSize: '15px',
                  type: 'text',
                  xoffset: 0,
                  yoffset: -16,
                },
              },
            ];
            layerConfig.renderer.symbol.width = `${32}px`;
            layerConfig.renderer.symbol.height = `${32}px`;

            break;
          }
          case 'mapabc': {
            const layerId = 'serviceStatusDetailsLayer';
            if (!data) {
              return layerId;
            }
            map.loadImage(getUrl(), (error, img) => {
              if (!error && !map.hasImage(layerId)) {
                map.addImage(layerId, img);
              }
            });

            break;
          }
          default: {
            return getUrl();
          }
        }
      },
    });
    mapObj.setCenter(position);
  }
</script>

<template>
  <div class="details-content ground-glass">
    <div class="header">
      <TdTitle show-close @close="closeCard">{{ t('policeAdmin.service.detail') }}</TdTitle>
    </div>

    <div class="main">
      <div class="info-box">
        <div class="profile">
          <TdAvatar class="avatar" :url="person.headShot || ''" />
          <div class="info">
            <span class="name">{{ info.executorName }}</span>
            <span class="org">{{ info.organizationName }}</span>
          </div>
        </div>

        <div class="post">
          <div>
            <span class="label">
              {{ t(addressStr(info.status)) }}
            </span>
            <span class="text">{{ info.address }}</span>
          </div>
          <div>
            <span class="label">{{ t(timeStr(info.status)) }}</span>
            <span class="text">{{ info.gmtCreated }}</span>
          </div>
        </div>

        <div class="photo">
          <div class="title">{{ t('policeAdmin.service.photo') }}</div>
          <div class="photo-img">
            <img alt="" :src="imgUrl" />
          </div>
        </div>
      </div>

      <div class="map-box">
        <BottomMap :map-id="mapId" :show-tool="false" />
      </div>
    </div>
  </div>
</template>

<style scoped lang="less">
  .details-content {
    display: flex;
    flex-direction: column;
    width: 808px;
    height: 452px;

    .header {
      position: relative;
      display: flex;
      align-items: center;
    }

    .main {
      display: flex;
      flex: 1;

      .info-box {
        width: 310px;
        height: 100%;
        padding: 10px;

        .profile {
          display: flex;
          margin-bottom: 10px;

          img {
            width: 54px;
            height: 54px;
            margin-right: 10px;
          }

          .info {
            display: flex;
            flex-direction: column;
            justify-content: center;

            .name {
              font-size: 16px;
              font-weight: bolder;
            }

            .org {
              font-size: 12px;
              color: var(--text-title-second);
            }
          }
        }

        .post {
          padding: 10px;
          margin-bottom: 20px;
          font-size: 12px;
          backdrop-filter: blur(20px);
          border: 1px solid rgb(255 255 255 / 20%);

          .label {
            display: inline-flex;
            width: 80px;
            color: var(--text-title-second);
          }
        }

        .photo {
          .title {
            margin-bottom: 5px;
            font-size: 12px;
            color: var(--text-title-second);
          }

          .photo-img {
            display: flex;
            align-items: center;
            justify-content: center;
            width: 100%;
            height: 200px;
            padding: 10px;
            backdrop-filter: blur(20px);
            border: 1px solid rgb(255 255 255 / 20%);

            img {
              max-height: 100%;
            }
          }
        }
      }

      .map-box {
        flex: 1;
      }
    }
  }
</style>
