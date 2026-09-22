import { computed } from 'vue';

export function useDeviceAdapter() {
  const deviceConfig = computed(() => {
    // 使用正确的设备像素比来计算
    let screenHeight = window.screen.height;
    let screenWidth = window.screen.width;
    const devicePixelRatio = screenHeight < 1000 ? 2 : window.devicePixelRatio || 1;
    screenHeight = screenHeight * devicePixelRatio;
    screenWidth = screenWidth * devicePixelRatio;
    // 添加更详细的调试信息
    console.log('screenHeight===screenWidth===', screenHeight, screenWidth);

    // 根据屏幕高度判断设备类型

    if (screenHeight >= 2800) {
      // 大屏设备 (如C7: 2800)
      return {
        width: '70px',
        height: '70px',
        weightSm: '60px',
        heightSm: '60px',
        createGroupWidth: '56px',
        createGroupHeight: '56px',
        createGroupBtnHeight: '100px',
        createMinGroupWidth: '52px',
        createMinGroupHeight: '52px',
        radius: '21px',
        iconSize: '24',
        iconSizeBread: '18',
        statusWidth: '24px',
        statusHeight: '24px',
        swiperHeight: '346px',
        fontSize: '22px',
        emptyWidth: '88px',
        emptyHeight: '88px',
        actionWidth: '45px',
        searchHeight: '55px',
        paddingTop: '80',
        navBarHeight: '44',
        radioSize: '46',
        groupIconWidth: '70px',
        groupIconHeight: '70px',
        editIconWidth: '30px',
        editIconHeight: '30px',
        folderIconWidth: '22px',
        folderIconHeight: '22px',
        btnPadding: '12px 0',
        shareIcon: '32',
      };
    } else if (screenHeight >= 2500) {
      // 大屏设备 (如M6: 2560)
      return {
        width: '50px',
        height: '50px',
        weightSm: '50px',
        heightSm: '50px',
        createGroupWidth: '45px',
        createGroupHeight: '45px',
        createGroupBtnHeight: '82px',
        createMinGroupWidth: '41px',
        createMinGroupHeight: '41px',
        radius: '20px',
        iconSize: '24',
        iconSizeBread: '18',
        statusWidth: '23px',
        statusHeight: '23px',
        swiperHeight: '261px',
        fontSize: '20px',
        emptyWidth: '88px',
        emptyHeight: '88px',
        actionWidth: '45px',
        searchHeight: '55px',
        paddingTop: '56',
        navBarHeight: '44',
        radioSize: '46',
        groupIconWidth: '70px',
        groupIconHeight: '70px',
        editIconWidth: '30px',
        editIconHeight: '30px',
        folderIconWidth: '22px',
        folderIconHeight: '22px',
        btnPadding: '12px 0',
        shareIcon: '32',
      };
    } else if (screenHeight >= 2000) {
      // 中等屏设备 (如C5: 2000)
      return {
        width: '50px',
        height: '50px',
        weightSm: '36px',
        heightSm: '36px',
        createGroupWidth: '44px',
        createGroupHeight: '44px',
        createGroupBtnHeight: '80px',
        createMinGroupWidth: '40px',
        createMinGroupHeight: '40px',
        radius: '18px',
        iconSize: '24',
        iconSizeBread: '18',
        statusWidth: '22px',
        statusHeight: '22px',
        swiperHeight: '226px',
        fontSize: '16px',
        emptyWidth: '88px',
        emptyHeight: '88px',
        actionWidth: '45px',
        searchHeight: '55px',
        paddingTop: '44',
        navBarHeight: '44',
        radioSize: '46',
        groupIconWidth: '60px',
        groupIconHeight: '60px',
        editIconWidth: '30px',
        editIconHeight: '30px',
        folderIconWidth: '22px',
        folderIconHeight: '22px',
        btnPadding: '12px 0',
        shareIcon: '30',
      };
    } else {
      // 手机设备
      return {
        width: '38px',
        height: '38px',
        weightSm: '36px',
        heightSm: '36px',
        createGroupWidth: '36px',
        createGroupHeight: '36px',
        createGroupBtnHeight: '56px',
        createMinGroupWidth: '33px',
        createMinGroupHeight: '33px',
        radius: '14px',
        iconSize: '20',
        statusWidth: '18px',
        statusHeight: '18px',
        swiperHeight: '140px',
        fontSize: '16px',
        emptyWidth: '88px',
        emptyHeight: '88px',
        searchHeight: '40px',
        actionWidth: '40px',
        paddingTop: '44',
        navBarHeight: '44',
        radioSize: '36',
        groupIconWidth: '40px',
        groupIconHeight: '40px',
        editIconWidth: '16px',
        editIconHeight: '16px',
        folderIconWidth: '20px',
        folderIconHeight: '20px',
        btnPadding: '7px 0px',
        shareIcon: '24',
      };
    }
  });

  return {
    adaptationSize: deviceConfig,
  };
}
