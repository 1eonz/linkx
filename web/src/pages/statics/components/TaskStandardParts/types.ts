// web/src/pages/statics/components/TaskStandardParts/types.ts
// 字段定义对齐后端 TasksController 接口文档（见 requirements/attachments/interface-任务管理接口.md）

/** 任务标准件模块配置（对应后端 TasksConfigVO） */
export interface TasksConfigVO {
  /** ID */
  id: number;
  /** 开放接口的系统编号 */
  systemCode: string;
  /** 任务标准件模块名称 */
  module: string;
  /** 是否在 PC 显示。1：要显示；0：不显示 */
  showInPc: number;
  /** 最后修改时间 */
  gmtLastModified: string;
  /** 创建时间 */
  gmtCreated: string;
}

/** 任务执行人/创建人（对应后端 TasksExecutors） */
export interface TasksExecutors {
  /** 姓名 */
  name: string;
  /** 身份证号 */
  idCard: string;
  /** 部门名称 */
  department: string;
  /** 部门 ID */
  departmentId: string;
  /** 部门编码 */
  departmentCode: string;
  /** 操作时间（执行人适用） */
  operateTime?: string;
}

/** 任务列表项（对应后端 TasksVO） */
export interface TasksVO {
  /** 任务 ID */
  id: number;
  /** 业务系统生成的任务编号 */
  number: string;
  /** 任务名称（2-64） */
  name: string;
  /** 任务内容（2-512） */
  content: string;
  /** 任务所属系统 */
  system: string;
  /** 任务所属模块 */
  module: string;
  /** 业务类型 */
  businessType: string;
  /** 任务状态（业务系统文字描述） */
  status: string;
  /** 任务等级 */
  level: string;
  /** 是否紧急。0-不紧急（默认） */
  urgent: number;
  /** 签收类型。0-不涉及、1-会签、2-或签 */
  approvalType: number;
  /** 详情地址 */
  url: string;
  /** URL 打开方式。1-普通H5、2-全屏H5、3-警信H5小程序、4-协同小程序 */
  urlOpenType: number;
  /** 审批页面地址 */
  approvalUrl: string;
  /** 创建人 */
  creator: TasksExecutors;
  /** 执行人列表 */
  executors: TasksExecutors[];
  /** 任务开始时间 */
  startTime: string;
  /** 任务结束时间 */
  endTime: string;
  /** 任务完成时间 */
  completeTime: string;
  /** 任务扩展描述 */
  extend: string;
  /** 操作时间 */
  operateTime: string;
  /** 修改时间 */
  updateTime: string;
  /** 是否已收藏。true-已收藏 */
  isFavorite: boolean;
  /** 任务类型。0-外部系统提交（默认）、1-系统内部生成 */
  type: number;
  /** 关联附件列表 */
  attachments: TasksAttachment[];
}

/** 任务附件（对齐后端 attachment 结构） */
export interface TasksAttachment {
  /** 附件 ID */
  id: number;
  /** 任务编号 */
  taskNumber: string;
  /** 文件名 */
  fileName: string;
  /** 文件路径 */
  filePath: string;
  /** 文件 URL */
  fileUrl: string;
  /** 文件类型 */
  fileType: string;
  /** 文件大小（字节） */
  fileSize: number;
  /** 创建人 ID */
  userId: number;
  /** 创建时间 */
  gmtCreated: string;
}

/** 任务列表查询参数（对应接口二 Query） */
export interface TaskListParams {
  /** 任务标准件模块名称（必填） */
  module: string;
  /** 页码，默认 1 */
  pageNum?: number;
  /** 每页条数，默认 10 */
  pageSize?: number;
  /** 任务名称关键字（模糊匹配） */
  name?: string;
  /** 任务内容关键字（模糊匹配） */
  content?: string;
  /** 范围：1全部、2创建人、3执行人、4创建人+执行人 */
  scope?: number;
  /** 身份证号；scope != 1 时用于过滤；未传则取当前登录人身份证号 */
  idCard?: string;
}

/** 分页结果（对应后端 Page<TasksVO>） */
export interface TaskListResult {
  /** 总条数 */
  total: number;
  /** 总页数 */
  pages: number;
  /** 当前页 */
  current: number;
  /** 每页条数 */
  size: number;
  /** 当前页数据 */
  records: TasksVO[];
}

/** 任务处置记录（对齐后端 TasksProcesses） */
export interface TasksProcesses {
  /** 处置记录 ID */
  id: number;
  /** 任务编号 */
  taskNumber: string;
  /** 处置动作。1-认领、2-转发、3-回退、4-处置、5-完成 */
  action: number;
  /** 任务状态 */
  status: string;
  /** 下一个处理人信息 */
  nextExecutors: TasksExecutors[];
  /** 操作人姓名 */
  operatorName: string;
  /** 操作人 ID */
  operatorId: number;
  /** 备注 */
  remark: string;
  /** 创建时间 */
  gmtCreated: string;
}

/** 处置记录查询参数（对应接口 Query） */
export interface ProcessListParams {
  /** 页码，默认 1 */
  pageNum?: number;
  /** 每页条数，默认 10 */
  pageSize?: number;
}

/** 处置记录分页结果（对应后端 Page<TasksProcesses>） */
export interface ProcessListResult {
  /** 总条数 */
  total: number;
  /** 总页数 */
  pages: number;
  /** 当前页 */
  current: number;
  /** 每页条数 */
  size: number;
  /** 当前页数据 */
  records: TasksProcesses[];
}
