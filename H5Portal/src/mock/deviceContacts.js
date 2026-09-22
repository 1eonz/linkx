// 设备通讯录和摄像头层级模拟数据及接口

// 组织模拟5个
const orgList = [
  { NodeType: "0", NodeName: "四川省", NodeDN: "1294393266" },
  { NodeType: "0", NodeName: "重庆市", NodeDN: "716984613" },
  { NodeType: "0", NodeName: "北京", NodeDN: "10000001" },
  { NodeType: "0", NodeName: "上海", NodeDN: "10000002" },
  { NodeType: "0", NodeName: "广州", NodeDN: "10000003" },
];

// 自研记录仪 10条 category=101
const selfRecorderList = Array.from({ length: 10 }, (_, i) => ({
  NodeType: "1",
  NodeName: `自研记录仪${i + 1}`,
  NodeDN: `SR${10000 + i}`,
  NodeAlias: `自研记录仪${i + 1}`,
  category: "101",
}));

// 三方记录仪用户 10条 category=11
const thirdRecorderList = Array.from({ length: 10 }, (_, i) => ({
  NodeType: "1",
  NodeName: `三方记录仪${i + 1}`,
  NodeDN: `TR${20000 + i}`,
  NodeAlias: `三方记录仪${i + 1}`,
  category: "11",
}));

// 终端 10条 category=100 apptype=109
const terminalList = Array.from({ length: 10 }, (_, i) => ({
  NodeType: "1",
  NodeName: `终端${i + 1}`,
  NodeDN: `TD${30000 + i}`,
  NodeAlias: `终端${i + 1}`,
  category: "100",
  apptype: "109",
}));

// 布控球 20条 category=103
const ballList = Array.from({ length: 20 }, (_, i) => ({
  NodeType: "1",
  NodeName: `布控球${i + 1}`,
  NodeDN: `BKQ${40000 + i}`,
  NodeAlias: `布控球${i + 1}`,
  category: "103",
}));

// 摄像头 20条 category=100 apptype=111
const cameraList = Array.from({ length: 20 }, (_, i) => ({
  NodeType: "1",
  NodeName: `摄像头${i + 1}`,
  NodeDN: `CAM${50000 + i}`,
  NodeAlias: `摄像头${i + 1}`,
  category: "100",
  apptype: "111",
}));

// getTreeDepartment接口mock
export async function getTreeDepartment(data) {
  // data: { nodeDN, offsetId, limit }
  // 这里只做简单分页模拟
  const allList = [
    ...orgList,
    ...selfRecorderList,
    ...thirdRecorderList,
    ...terminalList,
    ...ballList,
    ...cameraList,
  ];
  const limit = Number(data.limit) || 60;
  const offset = allList.findIndex((item) => item.NodeDN === data.offsetId);
  const start = offset >= 0 ? offset + 1 : 0;
  const list = allList.slice(start, start + limit);
  const nextOffsetId =
    start + limit < allList.length ? allList[start + limit - 1].NodeDN : "0";
  return {
    offsetId: nextOffsetId,
    organizationList: list,
  };
}

// 摄像头层级模拟：层级2个，摄像头10条
const cameraLevelList = [
  {
    NodeType: 0,
    subLevel: { LevelId: 13, LevelName: "一级层级" },
    camera: null,
  },
  {
    NodeType: 0,
    subLevel: { LevelId: 8, LevelName: "二级层级" },
    camera: null,
  },
];
const cameraLeafList = Array.from({ length: 10 }, (_, i) => ({
  NodeType: 1,
  subLevel: null,
  camera: {
    CameraDN: `CAMERA${60000 + i}`,
    CameraName: `摄像头${i + 1}`,
    Department: 0,
    PTZControl: 1,
    state: null,
  },
}));

// getCamera接口mock
export async function getCamera(data) {
  // data: { nodeId, offsetId, limit }
  // 这里只做简单分页模拟
  const allNodes = [...cameraLevelList, ...cameraLeafList];
  const limit = Number(data.limit) || 20;
  const offset = allNodes.findIndex(
    (item, idx) => idx.toString() === data.offsetId
  );
  const start = offset >= 0 ? offset + 1 : 0;
  const list = allNodes.slice(start, start + limit);
  const nextOffsetId =
    start + limit < allNodes.length ? (start + limit - 1).toString() : "0";
  return {
    offsetid: nextOffsetId,
    cameraTreeSubNodeList: list,
    nodeInfo: {
      id: data.nodeId || 0,
      name: "DummyLevel",
      path: "/",
    },
  };
}
