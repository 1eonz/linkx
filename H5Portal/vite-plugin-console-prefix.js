/**
 * Vite 插件：为 console 调用添加统一前缀
 * 
 * 转换规则：
 * - console.log(a, b) → console.log(title, a, b)
 * - console.info(a, b) → console.info(a, b); console.log(title + '-info', a, b)
 * - console.warn(a, b) → console.warn(a, b); console.log(title + '-warn', a, b)
 * - console.error(a, b) → console.error(a, b); console.log(title + '-error', a, b)
 * 
 * title 格式：'[LINKX]-[2026-04-24 16:31:29.687]-[相对路径]-[函数名]'
 */

import { relative } from 'path';

// 生成时间戳的运行时代码（带缓存优化）
// 格式：2026-04-30 16:01:29
const getTimestampCode = `
(function() {
  var cached = null;
  var cacheTime = 0;
  return function() {
    var now = Date.now();
    if (now - cacheTime > 100) {
      cacheTime = now;
      var d = new Date(now);
      var pad = function(n) { return n < 10 ? '0' + n : n; };
      cached = d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) + ' ' +
               pad(d.getHours()) + ':' + pad(d.getMinutes()) + ':' + pad(d.getSeconds());
    }
    return cached;
  };
})()
`;

// JavaScript 关键字列表（用于过滤控制语句）
const JS_KEYWORDS = new Set([
  'if', 'else', 'for', 'while', 'do', 'switch', 'case', 'break', 'continue',
  'return', 'throw', 'try', 'catch', 'finally', 'with', 'debugger',
  'class', 'extends', 'super', 'import', 'export', 'from', 'as',
  'static', 'get', 'set', 'typeof', 'instanceof', 'new',
  'delete', 'void', 'in', 'of', 'default', 'yield', 'await', 'async'
]);

/**
 * 从代码中提取 console 调用所在的函数名
 */
function extractFunctionName(code, position) {
  // 向前查找函数定义
  const beforeCode = code.substring(0, position);
  
  // 匹配各种函数定义模式
  const patterns = [
    /function\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\(/g,           // function name(
    /async\s+function\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\(/g,   // async function name(
    /const\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*(?:async\s*)?(?:function|\([^)]*\)\s*=>)/g,  // const name = function/() =>
    /let\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*(?:async\s*)?(?:function|\([^)]*\)\s*=>)/g,    // let name = function/() =>
    /var\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=\s*(?:async\s*)?(?:function|\([^)]*\)\s*=>)/g,    // var name = function/() =>
    /([a-zA-Z_$][a-zA-Z0-9_$]*)\s*:\s*(?:async\s*)?function/g,  // name: function
    /([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\([^)]*\)\s*\{/g,          // name() { (方法简写)
  ];
  
  // 收集所有匹配，按位置排序
  const matches = [];
  for (const pattern of patterns) {
    pattern.lastIndex = 0;
    let match;
    while ((match = pattern.exec(beforeCode)) !== null) {
      matches.push({ index: match.index, name: match[1] });
    }
  }
  
  // 按位置降序排序，找到最近的函数定义
  matches.sort((a, b) => b.index - a.index);
  
  // 找到第一个不是关键字的匹配
  for (const m of matches) {
    if (!JS_KEYWORDS.has(m.name)) {
      return m.name;
    }
  }
  
  // 检查是否在 <script setup> 中，查找顶层定义
  const scriptSetupMatch = beforeCode.match(/<script[^>]*setup[^>]*>/);
  if (scriptSetupMatch) {
    const scriptStart = beforeCode.lastIndexOf('<script');
    // 在 script setup 中，重新查找顶层定义
    const topLevelPatterns = [
      /const\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=/g,
      /let\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=/g,
      /var\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*=/g,
      /function\s+([a-zA-Z_$][a-zA-Z0-9_$]*)\s*\(/g,
    ];
    for (const pattern of topLevelPatterns) {
      pattern.lastIndex = scriptStart;
      let match;
      while ((match = pattern.exec(beforeCode)) !== null) {
        if (!JS_KEYWORDS.has(match[1])) {
          return match[1];
        }
      }
    }
  }
  
  return 'anonymous';
}

/**
 * 检查位置是否在注释中
 */
function isInComment(code, position) {
  const beforeCode = code.substring(0, position);
  
  // 检查单行注释
  const lastNewline = beforeCode.lastIndexOf('\n');
  const lineStart = lastNewline === -1 ? 0 : lastNewline + 1;
  const lineBefore = beforeCode.substring(lineStart);
  if (lineBefore.includes('//')) {
    return true;
  }
  
  // 检查多行注释
  const lastBlockStart = beforeCode.lastIndexOf('/*');
  if (lastBlockStart !== -1) {
    const lastBlockEnd = beforeCode.lastIndexOf('*/');
    if (lastBlockEnd < lastBlockStart) {
      return true;
    }
  }
  
  return false;
}

/**
 * 检查位置是否在字符串中
 */
function isInString(code, position) {
  const beforeCode = code.substring(0, position);
  let inString = false;
  let stringChar = null;
  let escaped = false;
  
  for (let i = 0; i < beforeCode.length; i++) {
    const char = beforeCode[i];
    
    if (escaped) {
      escaped = false;
      continue;
    }
    
    if (char === '\\') {
      escaped = true;
      continue;
    }
    
    if (inString) {
      if (char === stringChar) {
        inString = false;
        stringChar = null;
      }
    } else {
      if (char === '"' || char === "'" || char === '`') {
        inString = true;
        stringChar = char;
      }
    }
  }
  
  return inString;
}

/**
 * 检查 console 调用是否在箭头函数表达式体中（没有花括号）
 * 返回: { isInArrowBody: boolean, arrowStart: number, arrowEnd: number }
 */
function checkArrowFunctionBody(code, consoleStart, consoleEnd) {
  // 向前查找 =>
  const beforeCode = code.substring(0, consoleStart);
  
  // 从 console 位置向前找最近的 =>
  let arrowPos = -1;
  for (let i = beforeCode.length - 1; i >= 0; i--) {
    if (beforeCode[i] === '>' && i > 0 && beforeCode[i - 1] === '=') {
      // 检查这个 => 是否在注释或字符串中
      if (!isInComment(beforeCode, i) && !isInString(beforeCode, i)) {
        arrowPos = i - 1;
        break;
      }
    }
  }
  
  if (arrowPos === -1) return { isInArrowBody: false };
  
  // 检查 => 后面到 console 之前是否有 {
  const betweenArrowAndConsole = beforeCode.substring(arrowPos + 2, consoleStart);
  const trimmed = betweenArrowAndConsole.trim();
  
  // 如果 => 后面直接是 console（或只有空白），说明是表达式体
  if (trimmed === '' || /^[\s\n\r]*$/.test(trimmed)) {
    // 找到这个箭头函数表达式体的结束位置
    // 需要找到表达式结束的位置（逗号、分号、右括号等）
    const afterConsole = code.substring(consoleEnd);
    
    // 简单处理：找到第一个不在表达式内的 , 或 ) 或 } 或 ;
    let depth = 0;
    let endPos = consoleEnd;
    let foundEnd = false;
    
    for (let i = 0; i < afterConsole.length; i++) {
      const char = afterConsole[i];
      
      if (char === '(' || char === '[' || char === '{') {
        depth++;
      } else if (char === ')' || char === ']' || char === '}') {
        if (depth === 0) {
          endPos = consoleEnd + i;
          foundEnd = true;
          break;
        }
        depth--;
      } else if (char === ',' && depth === 0) {
        endPos = consoleEnd + i;
        foundEnd = true;
        break;
      }
    }
    
    if (!foundEnd) {
      endPos = code.length;
    }
    
    return { isInArrowBody: true, arrowStart: arrowPos, bodyEnd: endPos };
  }
  
  return { isInArrowBody: false };
}

/**
 * 转换代码中的 console 调用
 * @param {string} code 源代码
 * @param {string} relativePath 相对路径
 * @param {boolean} keepOriginal 是否保留原始 info/warn/error 调用
 */
function transformConsoleCalls(code, relativePath, keepOriginal = true) {
  // 匹配 console.log/info/warn/error 的正则
  const consoleRegex = /console\.(log|info|warn|error)\s*\(/g;
  
  const replacements = [];
  let match;
  
  while ((match = consoleRegex.exec(code)) !== null) {
    const method = match[1];
    const startIndex = match.index;
    const parenStart = startIndex + match[0].length - 1; // 左括号位置
    
    // 跳过注释和字符串中的 console
    if (isInComment(code, startIndex) || isInString(code, startIndex)) {
      continue;
    }
    
    // 找到匹配的右括号
    let depth = 1;
    let endIndex = parenStart + 1;
    while (depth > 0 && endIndex < code.length) {
      const char = code[endIndex];
      if (char === '(' && !isInString(code, endIndex) && !isInComment(code, endIndex)) depth++;
      else if (char === ')' && !isInString(code, endIndex) && !isInComment(code, endIndex)) depth--;
      endIndex++;
    }
    
    const argsString = code.substring(parenStart + 1, endIndex - 1).trim();
    
    // 获取函数名
    const funcName = extractFunctionName(code, startIndex);
    
    // 生成标题表达式（末尾加换行）
    const titleExpr = `'[LINKX]-['+__getTimestamp()+']-[${relativePath}]-[${funcName}]\\n'`;
    
    // 检查是否在箭头函数表达式体中
    const arrowCheck = checkArrowFunctionBody(code, startIndex, endIndex);
    
    // 根据方法类型生成替换代码
    let replacement;
    let replaceStart = startIndex;
    let replaceEnd = endIndex;
    
    if (method === 'log') {
      if (argsString) {
        replacement = `console.log(${titleExpr}, ${argsString})`;
      } else {
        replacement = `console.log(${titleExpr})`;
      }
    } else {
      // info/warn/error: 根据 keepOriginal 决定是否保留原调用
      if (keepOriginal) {
        // build:dev - 保留原调用 + 添加带前缀的 log
        if (argsString) {
          replacement = `console.${method}(${argsString});console.log(${titleExpr}+'-${method}', ${argsString})`;
        } else {
          replacement = `console.${method}();console.log(${titleExpr}+'-${method}')`;
        }
        
        // 如果在箭头函数表达式体中，需要用花括号包裹
        if (arrowCheck.isInArrowBody) {
          if (argsString) {
            replacement = `{console.${method}(${argsString});console.log(${titleExpr}+'-${method}', ${argsString});}`;
          } else {
            replacement = `{console.${method}();console.log(${titleExpr}+'-${method}');}`;
          }
        }
      } else {
        // build - 只用 log 替换
        if (argsString) {
          replacement = `console.log(${titleExpr}+'-${method}', ${argsString})`;
        } else {
          replacement = `console.log(${titleExpr}+'-${method}')`;
        }
        
        // 如果在箭头函数表达式体中，需要用花括号包裹
        if (arrowCheck.isInArrowBody) {
          if (argsString) {
            replacement = `{console.log(${titleExpr}+'-${method}', ${argsString});}`;
          } else {
            replacement = `{console.log(${titleExpr}+'-${method}');}`;
          }
        }
      }
    }
    
    replacements.push({
      start: replaceStart,
      end: replaceEnd,
      replacement,
    });
  }
  
  // 从后向前替换，避免位置偏移
  let result = code;
  for (let i = replacements.length - 1; i >= 0; i--) {
    const { start, end, replacement } = replacements[i];
    result = result.substring(0, start) + replacement + result.substring(end);
  }
  
  return result;
}

/**
 * Vite 插件主函数
 */
export function consolePrefixPlugin(options = {}) {
  const {
    include = /\.(js|ts|vue)$/,
    exclude = /node_modules/,
    enabled = true, // 是否启用插件
  } = options;
  
  let isProduction = false;
  let mode = 'production'; // 默认为生产模式
  
  return {
    name: 'vite-plugin-console-prefix',
    enforce: 'post', // 在其他转换之后执行
    
    configResolved(config) {
      isProduction = config.isProduction;
      mode = config.mode || 'production';
    },
    
    transform(code, id) {
      // 仅在生产环境启用
      if (!isProduction || !enabled) {
        return null;
      }
      
      // 检查文件是否匹配
      if (exclude.test(id) || !include.test(id)) {
        return null;
      }
      
      // 快速检查是否包含 console
      if (!code.includes('console.')) {
        return null;
      }
      
      // 计算相对路径
      const cwd = process.cwd();
      let relativePath = relative(cwd, id);
      
      // 统一路径分隔符为 /
      relativePath = relativePath.replace(/\\/g, '/');
      
      // 移除 src/ 前缀
      relativePath = relativePath.replace(/^src\//, '');
      
      // 根据 mode 决定是否保留原始调用
      // production: 只用 log 替换 (keepOriginal = false)
      // development: 保留原调用 + log (keepOriginal = true)
      const keepOriginal = mode === 'development';
      
      // 转换代码
      const transformedCode = transformConsoleCalls(code, relativePath, keepOriginal);
      
      // 如果没有变化，返回 null
      if (transformedCode === code) {
        return null;
      }
      
      // 注入时间戳函数
      const finalCode = `var __getTimestamp=${getTimestampCode};\n${transformedCode}`;
      
      return {
        code: finalCode,
        map: null, // 不生成 sourcemap
      };
    },
  };
}
