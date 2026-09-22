import { appConfig } from '@/config';

type PermissionsType =
  | 'ADMIN' // eICSLEMF  勤务管理
  | 'ALARM' // eICSANF  预警
  | 'CONFERENCE' // eICSVCF   视频会商
  | 'CONTROL' // eICSCF    布控
  | 'DISPATCH' // eICSCDF   指挥调度(图上指挥+通信调度)
  | 'eBC' // eICSFeBC  eBC解决方案
  | 'EMERGENCY' //           紧急短信
  | 'HOME' // eICSVDF   可视化看板
  | 'MISSION' // eICSTDF   任务处置
  | 'REGION' //           辖区
  | 'SAFETY'; // eICSSSF   专项保障

// 菜单|按钮权限控制
export const usePermissions = (type: PermissionsType) => {
  const {
    eICSANF,
    eICSCDF,
    eICSCF,
    eICSFeBC,
    // eICSVDF,
    eICSLEMF,
    eICSSSF,
    eICSTDF,
    eICSVCF,
  } = appConfig.settingData;

  switch (type) {
    case 'ADMIN': {
      if (eICSLEMF === '0') {
        return false;
      }
      return true;
    }
    case 'ALARM': {
      if (eICSFeBC === '0') {
        return false;
      }
      if (eICSFeBC === '1' && eICSANF === '0') {
        return false;
      }
      return true;
    }
    case 'CONFERENCE': {
      if (eICSFeBC === '1' && eICSVCF === '0') {
        return false;
      }
      return true;
    }
    case 'CONTROL': {
      if (eICSCF === '0') {
        return false;
      }
      return true;
    }
    case 'DISPATCH': {
      if (eICSFeBC === '1' && eICSCDF === '0') {
        return false;
      }
      return true;
    }
    case 'eBC': {
      if (eICSFeBC === '0') {
        return false;
      }
      return true;
    }
    case 'EMERGENCY': {
      return true;
    }
    case 'HOME': {
      // 可视化看板需求（首页改造）还没做 先注释掉
      // if (eICSFeBC === '1' && eICSVDF === '0') {
      //   return false;
      // }
      return true;
    }
    case 'MISSION': {
      if (eICSFeBC === '0') {
        return false;
      }
      if (eICSFeBC === '1' && eICSTDF === '0') {
        return false;
      }
      return true;
    }
    case 'REGION': {
      return true;
    }
    case 'SAFETY': {
      if (eICSFeBC === '1' && eICSSSF === '0') {
        return false;
      }
      return true;
    }
  }
};
