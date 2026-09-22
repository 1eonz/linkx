import spritePng from '@/assets/images/openlayers/sprite.png';

import { Fill, Icon, Style, Text } from 'ol/style';
import Stroke from 'ol/style/Stroke';

import spriteConfig from './sprite.json';

const spriteKeys = Object.keys(spriteConfig);

export function createMapboxVectorStyle() {
  const fill = new Fill({
    color: '',
  });
  const stroke = new Stroke({
    color: '',
    width: 1,
  });
  const text = new Text({
    fill,
    font: '12px Calibri,sans-serif',
    stroke,
    text: '',
  });
  const n = new Style({
    fill,
  });
  const i = new Style({
    fill,
    stroke,
  });
  const d = new Style({
    stroke,
  });
  const g = new Style({
    text,
  });

  return function (feature, resolution) {
    const layer = feature.get('layer');
    const name = feature.get('name');
    const _class = feature.get('class');

    switch (layer) {
      case 'aerodrome_label':
      case 'mountain_peak':
      case 'place': // 地域、水域、路线名称、航空港标志、山名
      case 'transportation_name':
      case 'water_name': {
        fill.setColor('#ffffff');
        stroke.setColor('#444444');
        text.setText(name);

        if (layer === 'transportation_name' && resolution > 4) {
          return [];
        }

        // hamlet-小村庄 village-乡村 town-镇 suburb-城郊 city-城市 island-岛
        if (layer === 'place') {
          switch (_class) {
            case 'city': {
              if (resolution < 100) {
                return [];
              }
              break;
            }
            case 'hamlet': {
              if (resolution > 6) {
                return [];
              }
              break;
            }
            case 'suburb': {
              if (resolution < 70 || resolution > 100) {
                return [];
              }
              break;
            }
            case 'town': {
              if (resolution < 20 || resolution > 70) {
                return [];
              }
              break;
            }
            case 'village': {
              if (resolution < 6 || resolution > 20) {
                return [];
              }
              break;
            }
          }
        }

        return [g];
      }
      case 'aeroway': // 航道
      case 'boundary': {
        // 边界线
        stroke.setColor('#06789D');
        return [d];
      }
      // 建筑
      case 'building': {
        fill.setColor('#05A2F0');
        stroke.setColor('#06789D');
        return [i];
      }
      // 土地覆被
      case 'landcover': {
        fill.setColor('#43887f');
        return [n];
      }
      // 土地
      case 'landuse': {
        fill.setColor('#444444');
        stroke.setColor('#06789D');
        return [i];
      }
      // 公园
      case 'park': {
        return [
          new Style({
            fill: new Fill({ color: '#43ad7f' }),
            stroke: new Stroke({
              color: '#06789D',
              width: 1,
            }),
            text: new Text({
              fill: new Fill({
                color: '#FFF',
              }),
              stroke: new Stroke({
                color: '#444444',
                width: 1,
              }),
              text: name,
            }),
          }),
        ];
      }
      // poi
      case 'poi': {
        fill.setColor('#ffffff');
        stroke.setColor('#444444');
        text.setText(name);
        text.setOffsetY(20);

        if (resolution > 2) {
          return [];
        }

        const translation = {
          railway: 'bus-15',
          toilets: 'toilet-11',
        };
        let v: any = null;
        for (const key of spriteKeys) {
          if (translation[_class]) {
            v = spriteConfig[translation[_class]];
            break;
          }
          if (_class && key.startsWith(_class.replaceAll('_', '-'))) {
            v = spriteConfig[key];
            break;
          }
        }

        if (v && name) {
          const s = new Style({
            image: new Icon({
              height: v.height,
              offset: [v.x, v.y], // 偏移量，决定从雪碧图的哪个位置开始绘制图像
              size: [v.width, v.height], // 图标的宽和高
              src: spritePng,
              width: v.width,
            }),
            text,
          });
          return [s];
        }

        return [];
      }
      // 路线
      case 'transportation': {
        fill.setColor('#06789D');
        stroke.setColor('#06789D');
        return [i];
      }
      // 湖泊、海洋
      case 'water': {
        fill.setColor('#015179');
        return [n];
      }
      // 河流
      case 'waterway': {
        stroke.setColor('#015179');
        return [d];
      }
      //
    }
  };
}
