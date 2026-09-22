import { glassesGroupinfo } from '@/common/api/collaborativeGroup.js';
import { labelCreateGroup, collaborationLabelList } from '@/common/api/h5.js';
import { useCommunicationStore } from '@/stores/communication.js';
/**
 * 小乔推送消息指令处理 Hook
 * 用于监听和处理小乔（AI助手）推送的各种指令
 */
export const useIntentExecutor = () => {
  const communicationStore = useCommunicationStore();
  /**
   * 指令一：查询标签配置
   */
  const queryFastGroupConfig = async (params) => {
    let resultInfo = {
      seqId: params.seqId,
      code: -1,
      result: {
        items: [
          {
            entityName: 'FastGroupConfig',
            entityId: 'C' + Math.floor(10000000 + Math.random() * 90000000),
            keyWord: '协同群',
            lables: [],
          },
        ],
      },
    };
    try {
      const labelList = await collaborationLabelList(1);
      let lables = labelList.map((item) => item.name);
      // 执行结果返回给小乔
      resultInfo = {
        seqId: params.seqId,
        code: 0,
        result: {
          items: [
            {
              entityName: 'FastGroupConfig',
              entityId: 'C' + Math.floor(10000000 + Math.random() * 90000000),
              keyWord: '协同群',
              lables,
            },
          ],
        },
      };
      console.log(JSON.stringify(resultInfo), 'QueryFastGroupConfig返回的结果');
      window.WeSpaceSDK.setIntentExecutorResult(JSON.stringify(resultInfo));
    } catch (error) {
      console.log(JSON.stringify(resultInfo), 'QueryFastGroupConfig返回的结果');
      window.WeSpaceSDK.setIntentExecutorResult(JSON.stringify(resultInfo));
      console.error('查询标签配置失败:', error);
    }
  };

  /**
   * 获取GIS定位信息
   */
  const getGis = () => {
    return new Promise(async (resolve) => {
      communicationStore
        .getGisInfo()
        .then(resolve)
        .catch(() => resolve(''));
      // 超时处理
      setTimeout(() => {
        resolve('');
      }, 1000);
    });
  };

  /**
   * 确保本地 info 中有完整的人员及部门信息
   * @param {number} maxRetry - 最大重试次数
   * @param {number} delay - 重试延迟（毫秒）
   */
  const ensureOwnerInfo = async (maxRetry = 2, delay = 400) => {
    const user = await communicationStore.ensureUserInfoWithDept(maxRetry, delay);
    if (!user) return false;
    const depts = Array.isArray(user.userDepartments) ? user.userDepartments : [];
    let isDepartmentId = '';
    depts.forEach((item) => {
      if (item.isPrimary) {
        isDepartmentId = item.departmentId;
      }
    });
    return !!isDepartmentId;
  };

  /**
   * 指令二：一键建群
   * @param {Object} params - 推送数据
   */
  const fastCreateGroup = async (params) => {
    let resultInfo = {
      seqId: params.seqId,
      code: -1,
      result: {
        items: [
          {
            entityName: 'FastCreateGroup',
            entityId: 'C' + Math.floor(10000000 + Math.random() * 90000000),
            groupId: '',
            name: '',
            ownerId: '',
            ownerName: '',
          },
        ],
      },
    };
    try {
      console.log(params, '收到的一键建群消息');
      // 获取绑定标签信息
      const labelList = await collaborationLabelList(1);
      const lables = (params.jsonParams?.lable).split(',') || [];
      console.log(lables, '标签列表');
      const lablesId = labelList
        .filter((item) => lables.includes(item.name))
        .map((item) => item.id);
      console.log(lablesId, '标签ID列表');
      // 发起创建前再次确保人员及部门信息完整
      const ok = await ensureOwnerInfo();
      if (!ok) {
        return;
      }

      // 获取定位信息
      const gisData = await getGis();
      const location = gisData?.longitude != null && gisData?.longitude !== 'undefined' && gisData?.latitude != null && gisData?.latitude !== 'undefined'? `${gisData.longitude},${gisData.latitude}` : '';

      // 获取用户信息
      const { userInfo } = communicationStore;
      const info = {
        ownerId: '',
        ownerName: '',
        departmentId: '',
        departmentCode: '',
        departmentName: '',
      };

      if (userInfo) {
        info.ownerId = userInfo.userid;
        info.ownerName = userInfo.username;
        const depts = Array.isArray(userInfo.userDepartments) ? userInfo.userDepartments : [];
        depts.forEach((item) => {
          if (item.isPrimary) {
            info.departmentId = item.departmentId;
            info.departmentCode = item.departmentCode;
            info.departmentName = item.departmentName;
            info.departmentFullPath = item.fullPath;
          }
        });
      }
      //  获取是有可用眼镜（去掉判断）
      // 创建群组
      const res = await labelCreateGroup({
        ...info,
        ids: lablesId,
        idCard: userInfo.idCard,
        location: location,
      });

      if (res) {
        // 返回成功结果给小乔
        let groupinfo = await glassesGroupinfo({ groupId: res });
        resultInfo = {
          seqId: params.seqId,
          code: 0,
          result: {
            items: [
              {
                entityName: 'FastCreateGroup',
                entityId: 'C' + Math.floor(10000000 + Math.random() * 90000000),
                groupId: groupinfo.groupId,
                name: groupinfo.groupName,
                ownerId: groupinfo.ownerId,
                ownerName: groupinfo.ownerName,
              },
            ],
          },
        };
        console.log(JSON.stringify(resultInfo), '一键建群返回的结果');
        window.WeSpaceSDK.setIntentExecutorResult(JSON.stringify(resultInfo));
      } else {
        console.log(JSON.stringify(resultInfo), '一键建群返回的结果');
        window.WeSpaceSDK.setIntentExecutorResult(JSON.stringify(resultInfo));
      }
    } catch (error) {
      console.log(JSON.stringify(resultInfo), '一键建群返回的结果');
      window.WeSpaceSDK.setIntentExecutorResult(JSON.stringify(resultInfo));
      console.error('创建群组失败:', error);
    }
  };

  /**
   * 处理小乔推送消息
   * @param {Object} data - 推送数据
   */

  /**
   * 初始化小乔推送监听
   * @data {Object} data - 推送数据
   * {
      " seqId ":"XXX",
      " intentId":"Receipt",
      " jsonParams ":"XX",
      }
   */

  const initIntentExecutor = () => {
    // 注册监听
    window.WeSpaceSDK.onIntentExecutor((data) => {
      switch (data.intentId) {
        case 'QueryFastGroupConfig':
          // 查询标签
          queryFastGroupConfig(data);
          break;
        case 'FastCreateGroup':
          // 一键建群
          fastCreateGroup(data);
          break;
        default:
          break;
      }
      console.log('小乔推送监听已初始化');
    });
  };

  return {
    initIntentExecutor,
    fastCreateGroup,
    queryFastGroupConfig,
  };
};

export default useIntentExecutor;
