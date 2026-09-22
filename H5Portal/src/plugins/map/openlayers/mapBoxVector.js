import { Circle as CircleStyle, Fill, Icon, Style, Text } from 'ol/style';
import Stroke from 'ol/style/Stroke';

import spriteConfig from './sprite.json';

import spritePng from '@/assets/images/openlayers/sprite.png';

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
        let v = null;
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

export function createStyleFromLayerConfigs(layerConfigs) {
  const styleCache = new Map();

  return function (feature, resolution) {
    const sourceLayer = feature.get('layer');
    if (!sourceLayer) return _defaultStyle();

    const matchedConfigs = layerConfigs.filter(l => l['source-layer'] === sourceLayer);
    if (!matchedConfigs.length) return _defaultStyle();

    for (const matchedConfig of matchedConfigs) {
      if (matchedConfig.layout?.visibility === 'none') continue;

      const zoom = _resolutionToZoom(resolution);
      if (matchedConfig.minzoom !== undefined && zoom < matchedConfig.minzoom) continue;
      if (matchedConfig.maxzoom !== undefined && zoom > matchedConfig.maxzoom) continue;

      if (matchedConfig.minResolution !== undefined && resolution < matchedConfig.minResolution) continue;
      if (matchedConfig.maxResolution !== undefined && resolution > matchedConfig.maxResolution) continue;

      if (!_matchFilter(matchedConfig.filter, feature)) continue;

      const cacheKey = `${sourceLayer}-${matchedConfig.id}`;
      if (styleCache.has(cacheKey)) {
        const cached = styleCache.get(cacheKey);
        if (matchedConfig.type === 'symbol') {
          return _buildSymbolStyle(matchedConfig, feature);
        }
        return cached;
      }

      const style = _buildStyleFromPaint(matchedConfig, feature);
      if (matchedConfig.type !== 'symbol') {
        styleCache.set(cacheKey, style);
      }
      return style;
    }

    return [];
  };
}

function _matchFilter(filter, feature) {
  if (!filter || !Array.isArray(filter) || filter.length === 0) return true;

  const operator = filter[0];

  if (operator === 'all') {
    return filter.slice(1).every(f => _matchFilter(f, feature));
  }

  if (operator === 'any') {
    return filter.slice(1).some(f => _matchFilter(f, feature));
  }

  if (operator === '==' && filter.length === 3) {
    const propertyName = filter[1];
    const expectedValue = filter[2];
    return feature.get(propertyName) === expectedValue;
  }

  if (operator === '!=' && filter.length === 3) {
    const propertyName = filter[1];
    const expectedValue = filter[2];
    return feature.get(propertyName) !== expectedValue;
  }

  return true;
}

function _buildStyleFromPaint(layerConfig, feature) {
  const { type, paint = {}, layout = {} } = layerConfig;

  switch (type) {
    case 'fill': {
      const fillColor = _withOpacity(
        paint['fill-color'] || 'rgba(0,0,0,0)',
        paint['fill-opacity']
      );
      const outlineColor = paint['fill-outline-color'];
      return new Style({
        fill: new Fill({ color: fillColor }),
        stroke: outlineColor
          ? new Stroke({ color: outlineColor, width: 1 })
          : undefined,
      });
    }
    case 'line': {
      const lineColor = _withOpacity(
        paint['line-color'] || '#000',
        paint['line-opacity']
      );
      const lineWidth = Number(paint['line-width']) || 1;
      return new Style({
        stroke: new Stroke({ color: lineColor, width: lineWidth }),
      });
    }
    case 'circle': {
      const circleColor = _withOpacity(
        paint['circle-color'] || '#000',
        paint['circle-opacity']
      );
      const circleRadius = Number(paint['circle-radius']) || 5;
      return new Style({
        image: new CircleStyle({
          radius: circleRadius,
          fill: new Fill({ color: circleColor }),
          stroke: new Stroke({ color: 'rgba(255,255,255,0.8)', width: 1 }),
        }),
      });
    }
    case 'symbol': {
      return _buildSymbolStyle(layerConfig, feature);
    }
    case 'fill-extrusion': {
      const fillColor = paint['fill-extrusion-color'] || '#05A2F0';
      const resolvedColor = typeof fillColor === 'string' ? fillColor : '#05A2F0';
      return new Style({
        fill: new Fill({ color: resolvedColor }),
        stroke: new Stroke({ color: 'rgba(0,0,0,0.3)', width: 1 }),
      });
    }
    default:
      return _defaultStyle();
  }
}

function _buildSymbolStyle(layerConfig, feature) {
  const { paint = {}, layout = {} } = layerConfig;
  const textColor = paint['text-color'] || paint['fill-color'] || '#fff';
  const haloColor = paint['text-halo-color'] || '#444';
  const haloWidth = Number(paint['text-halo-width']) || 1;
  const textField = layout['text-field'] || '{name}';
  const fieldName = String(textField).replace(/^\{(.+)\}$/, '$1') || 'name';
  const text = feature?.get?.(fieldName) || '';
  if (!text) return [];
  return new Style({
    text: new Text({
      text,
      fill: new Fill({ color: textColor }),
      stroke: new Stroke({ color: haloColor, width: haloWidth }),
      font: '12px Calibri,sans-serif',
    }),
  });
}

function _defaultStyle() {
  return new Style({
    fill: new Fill({ color: 'rgba(0,0,0,0.1)' }),
    stroke: new Stroke({ color: 'rgba(0,0,0,0.3)', width: 1 }),
  });
}

function _withOpacity(color, opacity) {
  if (!color) return color;
  if (typeof opacity === 'object' && opacity !== null) {
    if (Array.isArray(opacity.stops) && opacity.stops.length > 0) {
      opacity = opacity.stops[0][1];
    } else {
      opacity = 1;
    }
  }
  if (opacity === undefined || opacity === 1) return color;
  if (typeof color === 'string' && color.startsWith('#')) {
    const hex = color.replace('#', '');
    const r = parseInt(hex.substring(0, 2), 16);
    const g = parseInt(hex.substring(2, 4), 16);
    const b = parseInt(hex.substring(4, 6), 16);
    return `rgba(${r},${g},${b},${opacity})`;
  }
  return color;
}

function _resolutionToZoom(resolution) {
  if (!resolution || resolution <= 0) return 0;
  return Math.round(Math.log2(156543.03392804097 / resolution));
}
