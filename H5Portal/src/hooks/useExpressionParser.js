/**
 * 表达式解析 Hook
 * 用于解析变量表达式模板，支持动态获取用户信息、部门信息、时间信息等
 * 
 * @example
 * const { parse, buildContext } = useExpressionParser();
 * 
 * // 解析表达式
 * const groupName = await parse('【${funDept.firstChar}】${user.name}-${date:yyyyMMdd}', { name: '大数据支撑' });
 * // 结果: 【大】张三-20260507
 */
import { useCommunicationStore } from '@/stores/communication.js';

/**
 * 星期映射
 */
const WEEK_MAP = ['日', '一', '二', '三', '四', '五', '六'];

/**
 * 支持的变量白名单
 */
const SUPPORTED_VARIABLES = {
  user: ['name', 'firstName', 'lastName', 'id', 'phone', 'email'],
  dept: ['name', 'firstChar', 'code', 'fullName'],
  funDept: ['name', 'firstChar'],
  tag: ['name', 'firstChar'],
  date: ['year', 'month', 'day', 'week', 'full'],
  time: ['hour', 'minute', 'second'],
};

/**
 * 支持的特殊变量
 */
const SUPPORTED_SPECIAL = ['timestamp', 'timestamp.seconds'];

/**
 * 支持的日期格式表达式
 */
const SUPPORTED_DATE_FORMATS = ['date', 'datetime'];

/**
 * 格式化日期变量
 */
function formatDateVariables(date) {
  const year = date.getFullYear().toString();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const week = WEEK_MAP[date.getDay()];
  const full = `${year}-${month}-${day}`;

  return { year, month, day, week, full };
}

/**
 * 格式化时间变量
 */
function formatTimeVariables(date) {
  const hour = date.getHours().toString().padStart(2, '0');
  const minute = date.getMinutes().toString().padStart(2, '0');
  const second = date.getSeconds().toString().padStart(2, '0');

  return { hour, minute, second };
}

/**
 * 格式化日期
 * @param date 日期对象
 * @param format 格式字符串，如 'yyyyMMdd', 'yyyy-MM-dd', 'yyyy年MM月dd日'
 */
function formatDate(date, format) {
  const year = date.getFullYear();
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  const hour = date.getHours().toString().padStart(2, '0');
  const minute = date.getMinutes().toString().padStart(2, '0');
  const second = date.getSeconds().toString().padStart(2, '0');

  return format
    .replace(/yyyy/g, year.toString())
    .replace(/yy/g, year.toString().slice(-2))
    .replace(/MM/g, month)
    .replace(/M/g, (date.getMonth() + 1).toString())
    .replace(/dd/g, day)
    .replace(/d/g, date.getDate().toString())
    .replace(/HH/g, hour)
    .replace(/H/g, date.getHours().toString())
    .replace(/mm/g, minute)
    .replace(/m/g, date.getMinutes().toString())
    .replace(/ss/g, second)
    .replace(/s/g, date.getSeconds().toString());
}

/**
 * 验证变量表达式是否合法
 * @param expression 表达式，如 'user.name', 'date:yyyyMMdd'
 * @returns {valid: boolean, reason?: string, type?: string}
 */
function validateVariable(expression) {
  // 1. 处理条件表达式 ${var|default:value}
  const defaultMatch = expression.match(/^(.+?)\|default:(.+)$/);
  if (defaultMatch) {
    return validateVariable(defaultMatch[1]);
  }

  // 2. 处理日期格式 ${date:yyyyMMdd}, ${datetime:yyyy-MM-dd HH:mm}
  const dateFormatMatch = expression.match(/^(date|datetime):(.+)$/);
  if (dateFormatMatch) {
    const [, type] = dateFormatMatch;
    if (SUPPORTED_DATE_FORMATS.includes(type)) {
      return { valid: true };
    }
    return { valid: false, reason: `不支持的日期类型: ${type}`, type: 'unsupported_date_type' };
  }

  // 3. 处理特殊变量 timestamp
  if (SUPPORTED_SPECIAL.includes(expression)) {
    return { valid: true };
  }

  // 4. 处理普通变量 ${user.name}, ${dept.firstChar}
  const parts = expression.split('.');
  if (parts.length !== 2) {
    return { valid: false, reason: `变量格式错误: ${expression}`, type: 'invalid_format' };
  }

  const [prefix, property] = parts;
  
  // 检查前缀是否支持
  if (!SUPPORTED_VARIABLES[prefix]) {
    return { valid: false, reason: `不支持的变量前缀: ${prefix}`, type: 'unsupported_prefix' };
  }

  // 检查属性是否支持
  if (!SUPPORTED_VARIABLES[prefix].includes(property)) {
    return { valid: false, reason: `不支持的变量属性: ${prefix}.${property}`, type: 'unsupported_property' };
  }

  return { valid: true };
}

/**
 * 获取对象嵌套属性的值
 * @param obj 对象
 * @param path 属性路径，如 'user.name'
 */
function getNestedValue(obj, path) {
  if (!obj || !path) return undefined;
  
  const keys = path.split('.');
  let value = obj;
  
  for (const key of keys) {
    if (value === null || value === undefined) {
      return undefined;
    }
    value = value[key];
  }
  
  return value;
}

/**
 * 解析单个变量表达式
 * @param expression 表达式，如 'user.name', 'date:yyyyMMdd'
 * @param context 上下文
 */
function parseVariable(expression, context) {
  //  先验证表达式是否合法
  const validation = validateVariable(expression);
  if (!validation.valid) {
    console.warn(`[表达式解析] 变量验证失败: ${expression}, 原因: ${validation.reason}`);
    
    //  根据错误类型决定返回值
    if (validation.type === 'unsupported_prefix') {
      // 对象不支持，返回空字符串
      return '';
    } else {
      // 其他错误，返回原表达式
      return `\${${expression}}`;
    }
  }

  // 处理条件表达式 ${var|default:value}
  const defaultMatch = expression.match(/^(.+?)\|default:(.+)$/);
  if (defaultMatch) {
    const [, varExpression, defaultValue] = defaultMatch;
    const value = parseVariable(varExpression, context);
    return value || defaultValue;
  }

  // 处理日期格式 ${date:yyyyMMdd}, ${datetime:yyyy-MM-dd HH:mm}
  const dateFormatMatch = expression.match(/^(date|datetime):(.+)$/);
  if (dateFormatMatch) {
    const [, type, format] = dateFormatMatch;
    const now = new Date();
    try {
      return formatDate(now, format);
    } catch (error) {
      console.warn('日期格式化失败:', format, error);
      return format; // 返回原格式字符串
    }
  }

  // 处理 timestamp
  if (expression === 'timestamp') {
    return Date.now().toString();
  }
  if (expression === 'timestamp.seconds') {
    return Math.floor(Date.now() / 1000).toString();
  }

  // 处理普通变量 ${user.name}, ${dept.firstChar}, ${funDept.name}
  const value = getNestedValue(context, expression);
  
  // 如果值为 undefined 或 null，返回空字符串
  if (value === undefined || value === null) {
    return '';
  }
  
  // 如果值为对象或数组，返回空字符串（不支持嵌套对象）
  if (typeof value === 'object') {
    return '';
  }
  
  return String(value);
}

/**
 * 解析表达式模板
 * @param template 模板字符串，如 '【${funDept.firstChar}】${user.name}-${date:yyyyMMdd}'
 * @param context 上下文
 */
function evaluateExpression(template, context) {
  if (!template || typeof template !== 'string') {
    return template || '';
  }

  // 如果模板中没有 ${}，直接返回
  if (!template.includes('${')) {
    return template;
  }

  try {
    //  改进：递归解析，支持嵌套表达式
    // 先匹配最外层的 ${...}
    let result = template;
    let hasMatch = true;
    let maxIterations = 10; // 防止无限循环
    let iteration = 0;

    while (hasMatch && iteration < maxIterations) {
      hasMatch = false;
      iteration++;

      // 匹配 ${...}，支持嵌套
      result = result.replace(/\$\{([^{}]*(?:\$\{[^{}]*\}[^{}]*)*)\}/g, (match, expression) => {
        hasMatch = true;
        
        // 检查是否包含嵌套表达式
        if (expression.includes('${')) {
          // 递归解析嵌套表达式
          const parsed = evaluateExpression(expression, context);
          
          // 如果解析后还包含 ${}，说明格式错误
          if (parsed.includes('${')) {
            // 尝试解析内部的 ${...}
            const innerParsed = parsed.replace(/\$\{([^}]+?)\}/g, (innerMatch, innerExpr) => {
              try {
                return parseVariable(innerExpr.trim(), context);
              } catch (error) {
                console.warn('解析嵌套表达式失败:', innerExpr, error);
                return innerMatch;
              }
            });
            return innerParsed;
          }
          
          return parsed;
        }

        // 普通表达式，直接解析
        try {
          return parseVariable(expression.trim(), context);
        } catch (error) {
          console.warn('解析表达式失败:', expression, error);
          return match; // 返回原表达式
        }
      });
    }

    return result;
  } catch (error) {
    console.warn('解析模板失败:', template, error);
    return template; // 返回原模板
  }
}

/**
 * 表达式解析 Hook
 */
export function useExpressionParser() {
  const communicationStore = useCommunicationStore();

  /**
   * 从 SDK Storage 中解码获取缓存的 JSON 数据
   */
  function decodeCachedData(cachedStr) {
    if (!cachedStr) return null;
    try {
      return JSON.parse(decodeURIComponent(escape(cachedStr)));
    } catch (e) {
      console.error('[表达式解析] 解码缓存数据失败:', e);
      return null;
    }
  }

  /**
   * 获取完整的用户信息（包含部门信息）
   * 优先从缓存获取，如果缓存数据不完整则调用 ensureUserInfoWithDept
   */
  async function getFullUserInfo() {
    console.info('[表达式解析] 开始获取完整用户信息');
    
    // 1. 优先从缓存获取
    const cachedStr = await communicationStore.getStorage('cachedUserInfo');
    const cached = decodeCachedData(cachedStr);
    
    console.info('[表达式解析] 缓存用户信息', {
      hasCache: !!cached,
      hasUserId: cached?.userid,
      hasUserDepartments: Array.isArray(cached?.userDepartments),
      userDepartmentsLength: cached?.userDepartments?.length || 0,
    });
    
    // 2. 检查缓存数据是否完整
    if (cached && cached.userid && Array.isArray(cached.userDepartments) && cached.userDepartments.length > 0) {
      console.info('[表达式解析] 使用缓存用户信息（数据完整）');
      return cached;
    }
    
    // 3. 缓存数据不完整，调用 ensureUserInfoWithDept
    console.info('[表达式解析] 缓存数据不完整，调用 ensureUserInfoWithDept');
    const fullUser = await communicationStore.ensureUserInfoWithDept();
    
    console.info('[表达式解析] ensureUserInfoWithDept 返回', {
      hasUser: !!fullUser,
      hasUserId: fullUser?.userid,
      hasUserDepartments: Array.isArray(fullUser?.userDepartments),
      userDepartmentsLength: fullUser?.userDepartments?.length || 0,
    });
    
    return fullUser || {};
  }

  /**
   * 从用户部门列表中获取主部门
   * @param user 用户信息
   */
  function getPrimaryDept(user) {
    // 1. 检查是否有 userDepartments 数组
    if (!user || !Array.isArray(user.userDepartments) || user.userDepartments.length === 0) {
      console.info('[表达式解析] 用户没有部门信息');
      return {};
    }
    
    console.info('[表达式解析] 用户部门列表', user.userDepartments);
    
    // 2. 查找主部门（isPrimary === true）
    const primaryDept = user.userDepartments.find(dept => dept.isPrimary === true);
    
    // 3. 如果有主部门，返回主部门
    if (primaryDept) {
      console.info('[表达式解析] 找到主部门', primaryDept);
      return primaryDept;
    }
    
    // 4. 如果没有主部门，返回第一个部门
    const firstDept = user.userDepartments[0];
    console.info('[表达式解析] 未找到主部门，使用第一个部门', firstDept);
    return firstDept || {};
  }

  /**
   * 构建标签上下文
   * @param tags 选中的标签列表，每项包含 name 字段
   */
  function buildTagContext(tags) {
    if (!tags || tags.length === 0) {
      return undefined;
    }
    const names = tags.map((t) => t.name || '');
    const firstChars = names.map((n) => n.charAt(0));
    return {
      name: names.join('|'),
      firstChar: firstChars.join('|'),
    };
  }

  /**
   * 构建表达式上下文
   * @param funDept 职能部门信息（可选）
   * @param tags 选中的标签列表（可选）
   */
  async function buildContext(funDept, tags) {
    const user = await getFullUserInfo();
    const dept = getPrimaryDept(user);
    const now = new Date();

    console.info('[表达式解析] 构建上下文', {
      funDept,
      tags,
      userId: user.userid,
      userName: user.username || user.userName,
      deptName: dept.departmentName,
      deptCode: dept.departmentCode,
    });

    // 构建用户上下文
    const userContext = {
      name: user.username || user.userName || '',
      firstName: (user.username || user.userName || '').slice(1),
      lastName: (user.username || user.userName || '').slice(0, 1),
      id: user.userid || user.userId || '',
      phone: user.phone || '',
      email: user.email || '',
      user,
    };

    // 构建部门上下文（用户所属部门）
    const deptContext = {
      name: dept.departmentName || '',
      firstChar: (dept.departmentName || '').charAt(0),
      code: dept.departmentCode || '',
      fullName: dept.fullPathName || '',
      dept,
    };

    // 构建职能部门上下文（选中的职能部门节点）
    const funDeptContext = funDept ? {
      name: funDept.name || '',
      firstChar: (funDept.name || '').charAt(0),
    } : undefined;

    // 构建标签上下文（选中的标签列表）
    const tagContext = buildTagContext(tags);

    // 构建日期和时间上下文
    const dateContext = formatDateVariables(now);
    const timeContext = formatTimeVariables(now);

    const context = {
      user: userContext,
      dept: deptContext,
      funDept: funDeptContext,
      tag: tagContext,
      date: dateContext,
      time: timeContext,
      timestamp: now.getTime(),
    };

    console.info('[表达式解析] 上下文构建完成', context);

    return context;
  }

  /**
   * 解析表达式模板
   * @param template 模板字符串
   * @param funDept 职能部门信息（可选）
   * @param tags 选中的标签列表（可选）
   */
  async function parse(template, funDept, tags) {
    console.info('[表达式解析] 开始解析', {
      template,
      funDept,
      tags,
    });

    if (!template) {
      console.info('[表达式解析] 模板为空，返回空字符串');
      return '';
    }

    const context = await buildContext(funDept, tags);
    const result = evaluateExpression(template, context);

    console.info('[表达式解析] 解析完成', {
      template,
      result,
    });

    return result;
  }

  return {
    parse,
    buildContext,
  };
}
