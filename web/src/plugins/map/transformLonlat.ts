/**
 * 地球坐标系——WGS84：EPSG:4326，常见于 GPS 设备，Google 地图等国际标准的坐标体系
 * 火星坐标系——GCJ-02：中国国内使用的被强制加密后的坐标体系，高德坐标就属于该种坐标体系
 * 百度坐标系——BD-09：百度地图所使用的坐标体系，是在火星坐标系的基础上又进行了一次加密处理
 */

import type { Position } from './type';

import { appConfig } from '@/config';

import { formatLonLat } from './helper';

const PI = Math.PI;
const a = 6_378_160; // 长半轴
const ee = 1 / 298.256; // 扁率

/**
 * GCJ02 转换为 WGS84
 * @param lng
 * @param lat
 * @returns {*[]}
 */
export function gcj02towgs84([lng, lat]: Position): Position {
  lat = +lat;
  lng = +lng;
  if (out_of_china(lng, lat)) {
    return [lng, lat];
  } else {
    let dlat = transformlat(lng - 105, lat - 35);
    let dlng = transformlng(lng - 105, lat - 35);
    const radlat = (lat / 180) * PI;
    let magic = Math.sin(radlat);
    magic = 1 - ee * magic * magic;
    const sqrtmagic = Math.sqrt(magic);
    dlat = (dlat * 180) / (((a * (1 - ee)) / (magic * sqrtmagic)) * PI);
    dlng = (dlng * 180) / ((a / sqrtmagic) * Math.cos(radlat) * PI);
    const mglat = lat + dlat;
    const mglng = lng + dlng;
    return [lng * 2 - mglng, lat * 2 - mglat];
  }
}

/**
 * WGS84 转换为 GCJ02
 * @param lng
 * @param lat
 * @returns {*[]}
 */
export function wgs84togcj02([lng, lat]: Position): Position {
  lat = +lat;
  lng = +lng;
  if (out_of_china(lng, lat)) {
    return [lng, lat];
  } else {
    let dlat = transformlat(lng - 105, lat - 35);
    let dlng = transformlng(lng - 105, lat - 35);
    const radlat = (lat / 180) * PI;
    let magic = Math.sin(radlat);
    magic = 1 - ee * magic * magic;
    const sqrtmagic = Math.sqrt(magic);
    dlat = (dlat * 180) / (((a * (1 - ee)) / (magic * sqrtmagic)) * PI);
    dlng = (dlng * 180) / ((a / sqrtmagic) * Math.cos(radlat) * PI);
    return [lng + dlng, lat + dlat];
  }
}

/**
 * GCJ02 转换为 BD09
 * @param lng
 * @param lat
 * @returns {*[]}
 */
export function gcj02tobd09([lng, lat]: Position): Position {
  const z = Math.hypot(lng, lat) + 0.000_02 * Math.sin(lat * PI);
  const theta = Math.atan2(lat, lng) + 0.000_003 * Math.cos(lng * PI);
  const bd_lng = z * Math.cos(theta) + 0.0065;
  const bd_lat = z * Math.sin(theta) + 0.006;
  return [bd_lng, bd_lat];
}

/**
 * BD09 转换为 GCJ02
 * @param lng
 * @param lat
 * @returns {*[]}
 */
export function bd09togcj02([lng, lat]: Position): Position {
  const x = lng - 0.0065;
  const y = lat - 0.006;
  const z = Math.hypot(x, y) - 0.000_02 * Math.sin(y * PI);
  const theta = Math.atan2(y, x) - 0.000_003 * Math.cos(x * PI);
  const gc_lng = z * Math.cos(theta);
  const gc_lat = z * Math.sin(theta);
  return [gc_lng, gc_lat];
}

/**
 * WGS84 转换为 BD09
 * @param lng
 * @param lat
 * @returns {*[]}
 */
export function wgs84tobd09([lng, lat]: Position): Position {
  const cj = wgs84togcj02([lng, lat]);
  const bd = gcj02tobd09(cj);
  return bd;
}

/**
 * BD09 转换为 WGS84
 * @param lng
 * @param lat
 * @returns {*[]}
 */
export function bd09towgs84([lng, lat]: Position): Position {
  const cj = bd09togcj02([lng, lat]);
  const wgs = gcj02towgs84(cj);
  return wgs;
}

/**
 * 判断是否在国内，不在国内则不做偏移
 * @param lng
 * @param lat
 * @returns {boolean}
 */
function out_of_china(lng: number, lat: number) {
  lat = +lat;
  lng = +lng;
  // 纬度3.86~53.55,经度73.66~135.05
  return !(lng > 73.66 && lng < 135.05 && lat > 3.86 && lat < 53.55);
}

function transformlat(lng: number, lat: number) {
  lat = +lat;
  lng = +lng;
  let ret =
    -100 + 2 * lng + 3 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
  ret += ((20 * Math.sin(6 * lng * PI) + 20 * Math.sin(2 * lng * PI)) * 2) / 3;
  ret += ((20 * Math.sin(lat * PI) + 40 * Math.sin((lat / 3) * PI)) * 2) / 3;
  ret += ((160 * Math.sin((lat / 12) * PI) + 320 * Math.sin((lat * PI) / 30)) * 2) / 3;
  return ret;
}

function transformlng(lng, lat) {
  lat = +lat;
  lng = +lng;
  let ret =
    300 + lng + 2 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
  ret += ((20 * Math.sin(6 * lng * PI) + 20 * Math.sin(2 * lng * PI)) * 2) / 3;
  ret += ((20 * Math.sin(lng * PI) + 40 * Math.sin((lng / 3) * PI)) * 2) / 3;
  ret += ((150 * Math.sin((lng / 12) * PI) + 300 * Math.sin((lng / 30) * PI)) * 2) / 3;
  return ret;
}

// 3857转4326
export function epsg3857to4326([lng, lat]: Position): Position {
  const x = (lng / 20_037_508.34) * 180;
  const y = (lat / 20_037_508.34) * 180;
  const elat = (180 / Math.PI) * (2 * Math.atan(Math.exp((y * Math.PI) / 180)) - Math.PI / 2);
  return [x, elat];
}

// 4326转3857
export function epsg4326to3857([lng, lat]: Position): Position {
  const earthRad = 6_378_137;
  const elng = ((lng * Math.PI) / 180) * earthRad;
  const a = (lat * Math.PI) / 180;
  const elat = (earthRad / 2) * Math.log((1 + Math.sin(a)) / (1 - Math.sin(a)));
  return [elng, elat];
}

/**
 * 设备坐标系 转 地图坐标系
 * @param coordinate 需要转换的坐标
 * @param reverse 反转
 */
export function coordinateTransform(coordinate, reverse?: boolean) {
  const { DeviceCoordinate, MapCoordinate } = appConfig.settingData;
  if (MapCoordinate === DeviceCoordinate) {
    return formatLonLat(coordinate);
  }

  const transformApi = {
    bd09togcj02,
    bd09towgs84,
    gcj02tobd09,
    gcj02towgs84,
    wgs84tobd09,
    wgs84togcj02,
  };

  const transform = reverse
    ? `${MapCoordinate}to${DeviceCoordinate}`
    : `${DeviceCoordinate}to${MapCoordinate}`;

  const api = transformApi[transform.toLowerCase()];

  return api ? api(formatLonLat(coordinate)) : coordinate;
}
