import { ref } from 'vue';
import { getEquipmentPoints, getImUserPoints } from '@/common/api/map.js';
import { useUserCacheStore } from '@/stores/userCache.js';

/**
 * 点位加载器 Hook
 *
 * 功能：
 * 1. 分批渐进加载点位数据
 * 2. 队列处理机制，避免并发冲突
 * 3. 智能暂停，控制内存占用
 * 4. 数据处理和合并
 *
 * @param {Object} options - 配置选项
 * @param {Function} options.onPointsUpdate - 点位更新回调
 * @param {Function} options.onProgress - 进度更新回调
 * @returns {Object} - 加载器方法和状态
 */
export function usePointLoader(options = {}) {
  const { onPointsUpdate, onProgress } = options;

  // ==================== 配置常量 ====================

  /**
   * 分批加载配置常量
   *
   * 优化策略：
   * 1. 首批快速加载2000个点位，用户2-3秒就能看到首批点位
   * 2. 后台每批加载2000个点位，保持与首批一致（后端不支持动态pageSize）
   * 3. 失败时自动重试2次，提高稳定性
   *
   * 性能平衡：
   * - 首屏速度：2.5-3.5秒（可接受）
   * - 后台效率：减少50%请求次数
   * - 网络开销：适中
   */
  const PAGE_SIZE = 2000; // 统一pageSize（首批和后台都使用2000）
  const MAX_RETRY_COUNT = 2; // 最大重试次数

  // ==================== 状态管理 ====================

  /**
   * 加载进度状态
   * - 用于显示加载进度提示
   */
  const loadingProgress = ref({
    total: 0, // 总数量
    loaded: 0, // 已加载数量
    isComplete: false, // 是否加载完成
  });

  /**
   * 后台加载控制标志
   * - 防止重复加载
   * - 支持停止加载
   */
  let isLoadingInBackground = false; // 是否正在后台加载
  let shouldStopLoading = false; // 是否停止加载

  // ==================== 数据处理工具函数 ====================

  /**
   * 处理设备数据（记录仪/布控球）
   * @param {Array} equipmentList - 设备数据列表
   * @returns {Object} - 分类后的点位数据
   */
  function processEquipmentData(equipmentList) {
    const recorderPoints = []; // 记录仪（apptype: 109）
    const recorder5gPoints = []; // 5G记录仪（apptype: 112）
    const recorderThirdPartyPoints = []; // 三方记录仪（apptype: 113）
    const terminalWecommPoints = []; // 终端 Wecomm（apptype: 108）
    const dronePoints = []; // 无人机（apptype: 115）
    const personalSoldierPoints = []; // 单兵（apptype: 116）
    const pdtPoints = []; // PDT（apptype: 117）
    const terminalPoints = []; // 终端（apptype: 200）
    const dingqiaoRecorderPoints = []; // 鼎桥记录仪（subusercategory: 1）
    const surveillancePoints = []; // 布控球（subusercategory: 3, apptype: 0
    const gatewayProxyPoints = []; // 网关代理用户（category: 10）
    const personPoints = []; // 人员 新增：处理设备中的 person 类型
    const skippedPoints = []; // 记录被跳过的设备

    equipmentList.forEach((item, index) => {
      const { lon, lat, statusValue } = item || {};

      // 检查坐标是否有效
      const hasValidCoords = lon && lat && !isNaN(Number(lon)) && !isNaN(Number(lat));

      if (!hasValidCoords) {
        // 记录无效坐标的设备（前5个）
        if (skippedPoints.length < 5) {
          skippedPoints.push({
            index,
            id: item?.id,
            reason: '无效坐标',
            lon,
            lat,
          });
        }
        return; // 跳过无效坐标
      }

      const type = getEquipmentType(item);
      console.info('[processEquipmentData] 处理设备type:', type);
      const info = {
        ...item,
        position: [Number(lon), Number(lat)],
        status: Number(statusValue) === 4011 ? 'online' : 'offline',
        type,
      };
      console.info('[processEquipmentData] 处理设备indo:', info);
      switch (type) {
        case 'terminalWecomm':
          terminalWecommPoints.push(info);
          break;
        case 'recorder':
          recorderPoints.push(info);
          break;
        case 'recorder5g':
          recorder5gPoints.push(info);
          break;
        case 'recorderThirdParty':
          recorderThirdPartyPoints.push(info);
          break;
        case 'drone':
          dronePoints.push(info);
          break;
        case 'personalSoldier':
          personalSoldierPoints.push(info);
          break;
        case 'pdt':
          pdtPoints.push(info);
          break;
        case 'terminal':
          terminalPoints.push(info);
          break;
        case 'dingqiaoRecorder':
          dingqiaoRecorderPoints.push(info);
          break;
        case 'surveillance':
          surveillancePoints.push(info);
          break;
        case 'gatewayProxy':
          gatewayProxyPoints.push(info);
          break;
        // case 'person':  //  新增：处理 person 类型 不需要与imuser重复
        //   personPoints.push(info);
        //   break;
        default:
          // 记录被分类为其他类型的设备（前5个）
          if (skippedPoints.length < 10) {
            skippedPoints.push({
              index,
              id: item.id,
              reason: `分类为 ${type}`,
              category: item.category,
              apptype: item.apptype,
              subusercategory: item.subusercategory,
            });
          }
          break;
      }
    });

    console.info('[processEquipmentData] 处理结果:', {
      total: equipmentList.length,
      recorder: recorderPoints.length,
      surveillance: surveillancePoints.length,
      terminalWecomm: terminalWecommPoints.length,
      drone: dronePoints.length,
      personalSoldier: personalSoldierPoints.length,
      pdt: pdtPoints.length,
      terminal: terminalPoints.length,
      dingqiaoRecorder: dingqiaoRecorderPoints.length,
      gatewayProxy: gatewayProxyPoints.length,
      recorder5g: recorder5gPoints.length,
      recorderThirdParty: recorderThirdPartyPoints.length,
      person: personPoints.length, //  新增
      skipped:
        equipmentList.length -
        recorderPoints.length -
        surveillancePoints.length -
        personPoints.length,
    });

    if (skippedPoints.length > 0) {
      console.info('[processEquipmentData] 跳过的设备:', skippedPoints);
    }
    return {
      terminalWecommPoints,
      recorderPoints,
      recorder5gPoints, // 5G记录仪 (apptype: 112)
      recorderThirdPartyPoints, // 三方记录仪 (apptype: 113)
      dronePoints, // 无人机 (apptype: 115)
      personalSoldierPoints, // 单兵 (apptype: 116)
      pdtPoints, // PDT (apptype: 117)
      terminalPoints, // 终端 (apptype: 200)
      dingqiaoRecorderPoints, // 鼎桥记录仪 (subusercategory: 1)
      surveillancePoints, // 布控球 (subusercategory: 3, apptype: 0)
      gatewayProxyPoints, // 网关代理用户 (category: 10)
      personPoints,
    }; //  新增返回 personPoints
  }

  /**
   * 处理用户数据（人员）
   * @param {Array} userList - 用户数据列表
   * @returns {Array} - 人员点位数据
   */
  function processUserData(userList) {
    const personPoints = [];

    userList.forEach((item, index) => {
      const { lon, lat, statusValue } = item || {};
      if (lon && lat && !isNaN(Number(lon)) && !isNaN(Number(lat))) {
        const info = {
          ...item,
          position: [Number(lon), Number(lat)],
          status: Number(statusValue) === 4011 ? 'online' : 'offline',
          type: 'person',
        };
        personPoints.push(info);

        // 调试日志：输出前3个用户的信息
        if (index < 3) {
          console.log('[usePointLoader] 用户数据:', {
            index,
            id: item.id,
            name: item.name || item.userName,
            lon,
            lat,
          });
        }
      }
    });

    console.log('[usePointLoader] 用户数据处理完成:', {
      total: userList.length,
      person: personPoints.length,
    });

    return personPoints;
  }

  /**
   * 合并点位数据
   * @param {Object} existing - 现有点位数据
   * @param {Object} newData - 新点位数据
   * @returns {Object} - 合并后的点位数据
   */
  function mergePointsData(existing, newData) {
    return {
      recorderLayer: [...(existing.recorderLayer || []), ...(newData.recorderPoints || [])],
      personLayer: [...(existing.personLayer || []), ...(newData.personPoints || [])],
      surveillanceLayer: [
        ...(existing.surveillanceLayer || []),
        ...(newData.surveillancePoints || []),
      ],
      //  新增设备类型
      terminalWecommLayer: [
        ...(existing.terminalWecommLayer || []),
        ...(newData.terminalWecommPoints || []),
      ],
      recorder5gLayer: [...(existing.recorder5gLayer || []), ...(newData.recorder5gPoints || [])],
      recorderThirdPartyLayer: [
        ...(existing.recorderThirdPartyLayer || []),
        ...(newData.recorderThirdPartyPoints || []),
      ],
      droneLayer: [...(existing.droneLayer || []), ...(newData.dronePoints || [])],
      personalSoldierLayer: [
        ...(existing.personalSoldierLayer || []),
        ...(newData.personalSoldierPoints || []),
      ],
      pdtLayer: [...(existing.pdtLayer || []), ...(newData.pdtPoints || [])],
      terminalLayer: [...(existing.terminalLayer || []), ...(newData.terminalPoints || [])],
      dingqiaoRecorderLayer: [
        ...(existing.dingqiaoRecorderLayer || []),
        ...(newData.dingqiaoRecorderPoints || []),
      ],
      gatewayProxyLayer: [
        ...(existing.gatewayProxyLayer || []),
        ...(newData.gatewayProxyPoints || []),
      ],
    };
  }

  /**
   * 获取设备类型
   * @param {Object} data - 设备数据
   * @returns {string} - 设备类型
   */
  function getEquipmentType(data) {
    const category = Number(data?.category || data?.userCategory);
    const apptype = Number(data?.apptype);
    const subusercategory = Number(data?.subusercategory);

    console.info('[getEquipmentType] 设备类型判断:', {
      id: data?.id,
      name: data?.name || data?.userName,
      category: data?.category,
      userCategory: data?.userCategory,
      categoryNumber: category,
      apptype: data?.apptype,
      apptypeNumber: apptype,
      subusercategory: data?.subusercategory,
      subusercategoryNumber: subusercategory,
    });

    if (category === 1) {
      if(apptype === 111){
        return 'fixedCamera'; // 固定摄像头
      }else{
        return 'camera'; // 摄像头
      }
    } else if (category === 9) {
      if (apptype === 108) {
        return 'terminalWecomm'; // 终端 Wecomm
      } else if (apptype === 109) {
        return 'recorder'; // 记录仪
      } else if (apptype === 112) {
        return 'recorder5g'; // 5G记录仪
      } else if (apptype === 113) {
        return 'recorderThirdParty'; // 三方记录仪
      } else if (apptype === 115) {
        return 'drone'; // 无人机
      } else if (apptype === 116) {
        return 'personalSoldier'; // 单兵
      } else if (apptype === 117) {
        return 'pdt'; // PDT
      } else if (apptype === 200) {
        return 'terminal'; // 终端
      } else if (subusercategory === 1) {
        return 'dingqiaoRecorder'; // 鼎桥记录仪
      } else if (subusercategory === 3) {
        return 'surveillance'; // 布控球
      }
    } else if (category === 10) {
      return 'gatewayProxy'; // 网关代理用户
    }
    return 'person';
  }

  // ==================== 队列处理机制 ====================

  /**
   * 更新队列
   * - 用于收集两个独立加载循环的数据
   * - 避免并发冲突
   */
  const pendingUpdates = [];
  let isProcessingUpdate = false;

  /**
   * 队列大小监控统计
   * - 用于监控队列堆积情况
   * - 便于调试和性能分析
   */
  const queueStats = {
    maxSize: 0, // 历史最大大小
    avgSize: 0, // 平均大小
    sampleCount: 0, // 采样次数
    pauseCount: 0, // 暂停次数
  };

  /**
   * 更新队列统计信息
   * - 记录队列大小的变化
   * - 便于监控和调试
   */
  function updateQueueStats() {
    const currentSize = pendingUpdates.length;

    // 更新最大值
    if (currentSize > queueStats.maxSize) {
      queueStats.maxSize = currentSize;
    }

    // 更新平均值
    queueStats.sampleCount++;
    queueStats.avgSize =
      (queueStats.avgSize * (queueStats.sampleCount - 1) + currentSize) / queueStats.sampleCount;
  }

  /**
   * 判断是否需要暂停请求
   * - 如果队列堆积过多，暂停请求
   * - 避免队列无限增长
   * @returns {boolean} - 是否暂停
   */
  function shouldPauseRequest() {
    // 队列超过30个批次时暂停
    const shouldPause = pendingUpdates.length > 30;

    if (shouldPause) {
      queueStats.pauseCount++;
    }

    return shouldPause;
  }

  /**
   * 添加更新到队列
   * @param {string} type - 更新类型：'equipment' 或 'users'
   * @param {Array} data - 数据列表
   */
  function queueUpdate(type, data) {
    if (data && data.length > 0) {
      pendingUpdates.push({ type, data, timestamp: Date.now() });
    }
  }

  /**
   * 处理队列中的更新（每帧处理一次）
   * - 合并所有待处理的更新
   * - 一次性更新地图，避免频繁渲染
   */
  async function processPendingUpdates() {
    // 防止并发处理
    if (isProcessingUpdate || pendingUpdates.length === 0) return;

    isProcessingUpdate = true;

    try {
      // 更新队列统计
      updateQueueStats();

      // 取出所有待处理的更新
      const updates = pendingUpdates.splice(0, pendingUpdates.length);

      // 合并数据
      const equipmentData = [];
      const userData = [];

      for (const update of updates) {
        if (update.type === 'equipment') {
          equipmentData.push(...update.data);
        } else {
          userData.push(...update.data);
        }
      }

      // 处理数据
      const equipmentPoints = processEquipmentData(equipmentData);
      const userPersonPoints = processUserData(userData);

      //  合并设备中的 personPoints 和用户数据中的 personPoints
      const allPersonPoints = [...equipmentPoints.personPoints, ...userPersonPoints];

      // 一次性更新地图
      if (equipmentData.length > 0 || userData.length > 0) {
        // 调用回调更新点位
        if (typeof onPointsUpdate === 'function') {
          onPointsUpdate({
            recorderPoints: equipmentPoints.recorderPoints,
            surveillancePoints: equipmentPoints.surveillancePoints,
            personPoints: allPersonPoints, //  包含设备中的 person + 用户数据

            terminalWecommPoints: equipmentPoints.terminalWecommPoints,
            recorder5gPoints: equipmentPoints.recorder5gPoints,
            recorderThirdPartyPoints: equipmentPoints.recorderThirdPartyPoints,
            dronePoints: equipmentPoints.dronePoints,
            personalSoldierPoints: equipmentPoints.personalSoldierPoints,
            pdtPoints: equipmentPoints.pdtPoints,
            terminalPoints: equipmentPoints.terminalPoints,
            dingqiaoRecorderPoints: equipmentPoints.dingqiaoRecorderPoints,
            gatewayProxyPoints: equipmentPoints.gatewayProxyPoints,
          });
        }

        // 更新进度
        loadingProgress.value.loaded += equipmentData.length + userData.length;

        // 进度回调
        if (typeof onProgress === 'function') {
          onProgress(loadingProgress.value);
        }
      }
    } catch (error) {
      console.error('[usePointLoader] 处理队列更新失败:', error);
    } finally {
      isProcessingUpdate = false;
    }
  }

  // ==================== 分批加载核心逻辑 ====================

  /**
   * 加载首批数据（快速渲染）
   * @returns {Promise<Object>} - 首批点位数据
   */
  async function loadFirstBatch() {
    console.log('[usePointLoader] 开始加载首批数据');

    // 并行请求首批数据
    const [equipmentRes, usersRes] = await Promise.all([
      getEquipmentPoints({ pageNum: 1, pageSize: PAGE_SIZE, hasLocation: 1 })
        .then((res) => {
          console.log('获取设备', '/icp/user', res);
          return res;
        })
        .catch((err) => {
          console.error('获取设备失败', '/icp/user', err);
          throw err;
        }),
      getImUserPoints({ pageNum: 1, pageSize: PAGE_SIZE, hasLocation: 1 })
        .then((res) => {
          console.log('获取IM用户', '/icp/imuser', res);
          return res;
        })
        .catch((err) => {
          console.error('获取IM用户失败', '/icp/imuser', err);
          throw err;
        }),
    ]);

    return {
      equipment: equipmentRes?.records || [],
      users: usersRes?.records || [],
      equipmentTotal: equipmentRes?.total || 0,
      usersTotal: usersRes?.total || 0,
    };
  }

  /**
   * 设备数据加载
   *
   * @param {Object} options - 配置选项
   * @param {number} options.total - 总数量
   * @param {Function} options.onComplete - 完成回调
   */
  async function loadEquipmentInBackground({ total, onComplete }) {
    let pageNum = 2;
    let retryCount = 0;

    console.log('[usePointLoader] 开始后台加载设备数据，总数:', total);

    // 使用total计算总页数
    const totalPages = Math.ceil(total / PAGE_SIZE);

    while (pageNum <= totalPages && !shouldStopLoading) {
      // 智能暂停：检查队列是否堆积
      if (shouldPauseRequest()) {
        console.log('[usePointLoader] 设备数据：队列堆积，暂停请求，等待处理...');
        await new Promise((resolve) => setTimeout(resolve, 100)); // 等待100ms
        continue; // 继续检查，不发起请求
      }

      // 等待下一帧（使用 requestAnimationFrame，兼容鸿蒙）
      await new Promise((resolve) => requestAnimationFrame(resolve));

      try {
        // 请求设备数据
        const res = await getEquipmentPoints({
          pageNum,
          pageSize: PAGE_SIZE,
          hasLocation: 1,
        });
        console.log('获取设备', '/icp/user', res);

        const list = res?.records || [];

        if (list.length > 0) {
          // 加入队列，不直接更新
          queueUpdate('equipment', list);
          pageNum++;
          retryCount = 0;
        } else {
          // 如果返回空数据，跳出循环
          console.warn('[usePointLoader] 设备数据：第', pageNum, '页返回空数据，提前结束');
          break;
        }
      } catch (error) {
        console.error('获取设备失败', '/icp/user', error);
        console.error('[usePointLoader] 设备数据加载失败:', error);
        retryCount++;

        // 重试次数超限，停止加载
        if (retryCount >= MAX_RETRY_COUNT) {
          console.error('[usePointLoader] 设备数据重试次数超限，停止加载');
          break;
        }

        // 等待一段时间后重试
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
    }

    console.log('[usePointLoader] 设备数据加载完成，加载页数:', pageNum - 1, '/', totalPages);
    console.log('[usePointLoader] 队列统计:', queueStats);

    // 调用完成回调
    if (typeof onComplete === 'function') {
      onComplete();
    }
  }

  /**
   * 用户数据加载
   *
   * @param {Object} options - 配置选项
   * @param {number} options.total - 总数量
   * @param {Function} options.onComplete - 完成回调
   */
  async function loadUserInBackground({ total, onComplete }) {
    let pageNum = 2;
    let retryCount = 0;

    console.log('[usePointLoader] 开始后台加载用户数据，总数:', total);

    // 使用total计算总页数
    const totalPages = Math.ceil(total / PAGE_SIZE);

    while (pageNum <= totalPages && !shouldStopLoading) {
      // 智能暂停：检查队列是否堆积
      if (shouldPauseRequest()) {
        console.log('[usePointLoader] 用户数据：队列堆积，暂停请求，等待处理...');
        await new Promise((resolve) => setTimeout(resolve, 100)); // 等待100ms
        continue; // 继续检查，不发起请求
      }

      // 等待下一帧（使用 requestAnimationFrame，兼容鸿蒙）
      await new Promise((resolve) => requestAnimationFrame(resolve));

      try {
        // 请求用户数据
        const res = await getImUserPoints({
          pageNum,
          pageSize: PAGE_SIZE,
          hasLocation: 1,
        });
        console.log('获取IM用户', '/icp/imuser', res);

        const list = res?.records || [];

        if (list.length > 0) {
          // 加入队列，不直接更新
          queueUpdate('users', list);
          pageNum++;
          retryCount = 0;
        } else {
          // 如果返回空数据，跳出循环
          console.warn('[usePointLoader] 用户数据：第', pageNum, '页返回空数据，提前结束');
          break;
        }
      } catch (error) {
        console.error('获取IM用户失败', '/icp/imuser', error);
        console.error('[usePointLoader] 用户数据加载失败:', error);
        retryCount++;

        // 重试次数超限，停止加载
        if (retryCount >= MAX_RETRY_COUNT) {
          console.error('[usePointLoader] 用户数据重试次数超限，停止加载');
          break;
        }

        // 等待一段时间后重试
        await new Promise((resolve) => setTimeout(resolve, 1000));
      }
    }

    console.log('[usePointLoader] 用户数据加载完成，加载页数:', pageNum - 1, '/', totalPages);
    console.log('[usePointLoader] 队列统计:', queueStats);

    // 调用完成回调
    if (typeof onComplete === 'function') {
      onComplete();
    }
  }

  /**
   * 后台加载剩余数据
   *
   * @param {Object} options - 配置选项
   * @param {boolean} options.equipmentHasMore - 设备数据是否还有更多
   * @param {boolean} options.usersHasMore - 用户数据是否还有更多
   * @param {number} options.equipmentTotal - 设备数据总数
   * @param {number} options.usersTotal - 用户数据总数
   * @param {number} options.firstBatchLoaded - 首批实际加载数量
   */
  async function loadRemainingInBackground({
    equipmentHasMore,
    usersHasMore,
    equipmentTotal,
    usersTotal,
    firstBatchLoaded,
  }) {
    // 初始化加载进度（使用首批实际加载数量）
    loadingProgress.value.total = equipmentTotal + usersTotal;
    loadingProgress.value.loaded = firstBatchLoaded;
    loadingProgress.value.isComplete = false;

    console.log('[usePointLoader] 开始后台加载剩余数据（独立运行模式）');

    // 完成计数器
    let equipmentCompleted = !equipmentHasMore; // 如果没有更多，直接标记为完成
    let userCompleted = !usersHasMore; // 如果没有更多，直接标记为完成

    /**
     * 检查是否全部完成
     */
    const checkAllCompleted = () => {
      if (equipmentCompleted && userCompleted) {
        loadingProgress.value.isComplete = true;
        console.log('[usePointLoader] 后台加载完成，总点位:', loadingProgress.value.loaded);
      }
    };

    /**
     * 队列处理循环
     * - 持续处理队列中的更新
     * - 直到两个加载循环都完成
     */
    const processLoop = async () => {
      while (!shouldStopLoading && !(equipmentCompleted && userCompleted)) {
        // 等待下一帧
        await new Promise((resolve) => requestAnimationFrame(resolve));

        // 处理队列中的更新
        await processPendingUpdates();
      }
    };

    // 构建任务列表
    const tasks = [];

    // 只有设备数据还有更多时才加载
    if (equipmentHasMore) {
      tasks.push(
        loadEquipmentInBackground({
          total: equipmentTotal,
          onComplete: () => {
            equipmentCompleted = true;
            checkAllCompleted();
          },
        }),
      );
    }

    // 只有用户数据还有更多时才加载
    if (usersHasMore) {
      tasks.push(
        loadUserInBackground({
          total: usersTotal,
          onComplete: () => {
            userCompleted = true;
            checkAllCompleted();
          },
        }),
      );
    }

    // 队列处理循环始终运行
    tasks.push(processLoop());

    // 并行运行所有任务
    await Promise.all(tasks);
  }

  // ==================== 主加载函数 ====================

  /**
   * 加载点位数据（主函数）
   *
   * @param {Object} initialPoints - 初始点位数据（可选）
   * @returns {Promise<Object>} - 首批点位数据
   */
  async function loadPoints(initialPoints = null) {
    // 防止重复加载
    if (isLoadingInBackground) return;
    isLoadingInBackground = true;
    shouldStopLoading = false;

    try {
      // ==================== 快速加载 ====================
      console.log('[usePointLoader] 开始加载首批数据');
      const firstBatch = initialPoints || (await loadFirstBatch());

      // 检查是否需要停止
      if (shouldStopLoading) return;

      // 处理首批数据
      const equipmentData = processEquipmentData(firstBatch.equipment);
      console.log('[usePointLoader] 设备数据处理完成，点位数量:', equipmentData);
      const userPersonPoints = processUserData(firstBatch.users);

      //  合并设备中的 personPoints 和用户数据中的 personPoints
      const allPersonPoints = [...equipmentData.personPoints, ...userPersonPoints];

      // 立即渲染首批点位
      const initialPointsData = {
        recorderPoints: equipmentData.recorderPoints, //  使用 recorderPoints
        surveillancePoints: equipmentData.surveillancePoints, //  使用 surveillancePoints
        personPoints: allPersonPoints, //  包含设备中的 person + 用户数据
         // 新增设备类型
        terminalWecommPoints: equipmentData.terminalWecommPoints,
        recorder5gPoints: equipmentData.recorder5gPoints,
        recorderThirdPartyPoints: equipmentData.recorderThirdPartyPoints,
        dronePoints: equipmentData.dronePoints,
        personalSoldierPoints: equipmentData.personalSoldierPoints,
        pdtPoints: equipmentData.pdtPoints,
        terminalPoints: equipmentData.terminalPoints,
        dingqiaoRecorderPoints: equipmentData.dingqiaoRecorderPoints,
        gatewayProxyPoints: equipmentData.gatewayProxyPoints,
      };

      // 调用回调更新点位
      if (typeof onPointsUpdate === 'function') {
        onPointsUpdate(initialPointsData);
      }

      // 计算首批实际加载数量
      const firstBatchLoaded = firstBatch.equipment.length + firstBatch.users.length;

      console.log('[usePointLoader] 首批渲染完成，点位数量:', firstBatchLoaded);

      // 批量预加载用户头像（异步，不阻塞）
      if (allPersonPoints.length > 0) {
        try {
          const userCacheStore = useUserCacheStore();
          const userIds = allPersonPoints.map((p) => p.id).filter((id) => id);
          console.log('[usePointLoader] 批量预加载用户头像，数量:', userIds.length);
          userCacheStore.batchFetchUsers(userIds, true).catch((e) => {
            console.warn('[usePointLoader] 预加载用户头像失败:', e);
          });
        } catch (e) {
          console.warn('[usePointLoader] 初始化用户缓存失败:', e);
        }
      }

      // ==================== 判断是否需要后台加载 ====================

      // 使用total判断是否还有更多数据
      const equipmentHasMore = firstBatch.equipmentTotal > PAGE_SIZE;
      const usersHasMore = firstBatch.usersTotal > PAGE_SIZE;

      // 如果都没有更多数据，直接返回
      if (!equipmentHasMore && !usersHasMore) {
        console.log('[usePointLoader] 首批数据已包含所有点位，无需后台加载', {
          equipmentTotal: firstBatch.equipmentTotal,
          usersTotal: firstBatch.usersTotal,
        });
        loadingProgress.value.total = firstBatch.equipmentTotal + firstBatch.usersTotal;
        loadingProgress.value.loaded = firstBatchLoaded;
        loadingProgress.value.isComplete = true;
        return initialPointsData;
      }

      console.log('[usePointLoader] 首批数据不完整，开始后台加载', {
        equipmentHasMore,
        usersHasMore,
        equipmentTotal: firstBatch.equipmentTotal,
        usersTotal: firstBatch.usersTotal,
      });

      // ==================== 后台渐进加载 ====================
      await loadRemainingInBackground({
        equipmentHasMore,
        usersHasMore,
        equipmentTotal: firstBatch.equipmentTotal,
        usersTotal: firstBatch.usersTotal,
        firstBatchLoaded,
      });

      return initialPointsData;
    } catch (error) {
      console.error('[usePointLoader] loadPoints 失败:', error);
      throw error;
    } finally {
      isLoadingInBackground = false;
    }
  }

  /**
   * 停止后台加载
   * - 页面卸载时调用
   */
  function stopLoading() {
    shouldStopLoading = true;
  }

  // ==================== 导出 ====================

  return {
    // 状态
    loadingProgress,

    // 方法
    loadPoints,
    stopLoading,
    processEquipmentData,
    processUserData,
    mergePointsData,
    getEquipmentType,
  };
}
