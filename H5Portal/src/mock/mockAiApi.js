// 模拟API模块 增量返回
// export const mockAiApi = {
//   // 模拟创建新任务，返回任务ID
//   async newTask(params) {
//     console.log("模拟创建新任务:", params);
//     await new Promise(resolve => setTimeout(resolve, 500)); // 模拟网络延迟
//     return Math.floor(Math.random() * 10000); // 返回随机任务ID
//   },

//   // 预设的超长MD格式文本（分批次返回，转义反引号）
//   longMarkdownContent: '根据您提供的信息，我将从刑事案件定性角度进行分析：\n\n 一、案情分析关键点 \n1. 案件发生地点性质（开放道路 / 封闭区域）\n2. 行为人主观状态（过失 / 故意）\n3. 危害结果（伤亡程度）\n\n 二、典型罪名区分 \n1. 交通肇事罪（刑法第 133 条）\n- 适用条件：发生在公共交通管理范围内的道路 \n- 主观方面：过失 \n- 量刑：3 年以下；逃逸 3 - 7 年；逃逸致人死亡 7 年以上 \n2. 过失致人死亡罪（刑法第 233 条）\n- 适用条件：非公共交通场所 \n- 量刑：3 - 7 年；情节较轻 3 年以下 \n\n 三、法条依据 \n《刑法》第 133 条：" 违反交通运输管理法规... 发生重大事故..."\n\n《刑法》第 233 条：" 过失致人死亡..."\n\n 四、量刑建议框架 \n1. 基准刑确定 \n2. 量刑情节调节（自首、赔偿等）\n3. 宣告刑确定 \n\n 注：由于您未提供具体案情细节，建议补充以下信息：\n- 事故具体发生地点性质 \n- 行为人是否有违反特定管理法规 \n- 是否涉及特殊主体（如公职人员）等 \n\n（根据刑事法律规定，实际分析需结合具体案件证据材料）',

//   // 模拟获取任务结果（分批次返回超长文本）
//   async getAnswer({ id }) {
//     console.log(`模拟获取任务 ${id} 的结果`);
//     await new Promise(resolve => setTimeout(resolve, 800)); // 模拟网络延迟

//     // 记录当前回复的批次（闭包状态）
//     if (!mockAiApi.batchCount) {
//       mockAiApi.batchCount = 0;
//     }

//     // 批次大小（每批返回的字符数）
//     const batchSize = 80;

//     // 检查是否已完成所有批次
//     if (mockAiApi.batchCount * batchSize >= mockAiApi.longMarkdownContent.length) {
//       mockAiApi.batchCount = 0; // 重置批次计数器
//       return mockAiApi.longMarkdownContent; // 返回完整内容（包含[end]）
//     }

//     // 计算当前批次的起始和结束位置
//     const start = mockAiApi.batchCount * batchSize;
//     const end = Math.min(start + batchSize, mockAiApi.longMarkdownContent.length);

//     // 如果是最后一批，添加结束标记
//     const isLastBatch = end >= mockAiApi.longMarkdownContent.length;
//     const batchContent = mockAiApi.longMarkdownContent.substring(start, end);
//     const result = isLastBatch ? batchContent + '[end]' : batchContent;

//     // 增加批次计数器
//     mockAiApi.batchCount++;

//     return result;
//   }
// };

// 模拟API模块（返回全量覆盖内容）
export const mockAiApi = {
  // 模拟创建新任务，返回任务ID
  async newTask(params) {
    console.log("模拟创建新任务:", params);
    await new Promise((resolve) => setTimeout(resolve, 500)); // 模拟网络延迟
    return Math.floor(Math.random() * 10000); // 返回随机任务ID
  },
  // 预设的超长MD格式文本
  // fullMarkdownContent: '根据您提供的信息，我将从刑事案件定性角度进行分析：\n\n 一、案情分析关键点 \n1. 案件发生地点性质（开放道路 / 封闭区域）\n2. 行为人主观状态（过失 / 故意）\n3. 危害结果（伤亡程度）\n\n 二、典型罪名区分 \n1. 交通肇事罪（刑法第 133 条）\n- 适用条件：发生在公共交通管理范围内的道路 \n- 主观方面：过失 \n- 量刑：3 年以下；逃逸 3 - 7 年；逃逸致人死亡 7 年以上 \n2. 过失致人死亡罪（刑法第 233 条）\n- 适用条件：非公共交通场所 \n- 量刑：3 - 7 年；情节较轻 3 年以下 \n\n 三、法条依据 \n《刑法》第 133 条：" 违反交通运输管理法规... 发生重大事故..."\n\n《刑法》第 233 条：" 过失致人死亡..."\n\n 四、量刑建议框架 \n1. 基准刑确定 \n2. 量刑情节调节（自首、赔偿等）\n3. 宣告刑确定 \n\n 注：由于您未提供具体案情细节，建议补充以下信息：\n- 事故具体发生地点性质 \n- 行为人是否有违反特定管理法规 \n- 是否涉及特殊主体（如公职人员）等 \n\n（根据刑事法律规定，实际分析需结合具体案件证据材料）',
  fullMarkdownContent:
    "```text\n一、人员信息：\n姓名：高龙雨\n身份证号码：130629198807020872\n性别：男\n出生日期：19880702\n年龄：37\n民族：汉族\n婚姻状况：已婚\n与户主关系：长子\n身高：170厘米\n手机号码：15233125355\n文化程度：小学教育\n所属派出所名称：河北省保定市容城县公安局大河镇派出所\n出生地地址：大河镇东孙村富民路5号\n户籍行政区划：河北省容城县\n户号：130629426029078\n籍贯行政区划：河北省容城县\n\n二、轨迹信息：\n当前日期时间为：2025-07-03 16:15:24\n铁路售票信息：没有查询到近一周以来的信息。\n民航离港信息：没有查询到近一周以来的信息。\n旅店住宿信息：没有查询到近一周以来的信息。\n\n三、标签信息：\n当前风险标签：犯罪前科、违法前科、有矛盾纠纷报警记录\n标签变动情况：暂无标签变动信息\n\n四、在逃信息：\n当前人员非在逃人员，暂无人员信息。\n\n五、前科信息：\n未查询到此人前科信息\n\n六、机动车信息：\n未查询到此人的机动车信息\n```",

  // 模拟获取任务结果
  async getAnswer({ id }) {
    console.log(`模拟获取任务 ${id} 的全量内容`);
    await new Promise((resolve) => setTimeout(resolve, 800)); // 模拟网络延迟

    // 记录当前回复的进度
    if (!mockAiApi.progress) {
      mockAiApi.progress = 0;
    }

    // 每次增加20%进度
    mockAiApi.progress = Math.min(mockAiApi.progress + 0.2, 1.0);

    // 计算当前应该返回的内容长度
    const contentLength = Math.floor(
      mockAiApi.fullMarkdownContent.length * mockAiApi.progress
    );

    // 获取当前进度的内容
    let currentContent = mockAiApi.fullMarkdownContent.substring(
      0,
      contentLength
    );

    // 如果达到100%，添加结束标记
    if (mockAiApi.progress >= 1.0) {
      currentContent += "[end]";
      mockAiApi.progress = 0; // 重置进度
    }

    return currentContent;
  },
};
// 导出默认对象
export default mockAiApi;
