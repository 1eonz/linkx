#!/usr/bin/env node

/**
 * AI Code Review — Pre-commit Hook 脚本
 *
 * 职责单一：校验 review-passed 标记 → 阻断/放行提交
 * 所有审查逻辑由 Trae code-review Skill 完成（使用 Trae 内置模型，无需 API Key）
 *
 * 审查通过标记：当 .trae/review-passed 存在且 diffHash 匹配时，放行提交并清理标记文件
 *
 * 使用方式:
 *   node scripts/ai-code-review.mjs                    # 审查暂存区变更（pre-commit）
 *   node scripts/ai-code-review.mjs --from main        # 审查与 main 分支的差异
 *   node scripts/ai-code-review.mjs --no-block         # 仅收集，不阻断
 *   node scripts/ai-code-review.mjs --mark-passed      # 创建 review-passed 标记（审查通过后调用）
 *   node scripts/ai-code-review.mjs --clean            # 清理 passed 标记文件
 *
 * 环境变量:
 *   AI_REVIEW_NO_BLOCK  - 设为 true 则仅收集不阻断
 */

import { execSync } from "node:child_process";
import { existsSync, writeFileSync, unlinkSync, mkdirSync, readFileSync } from "node:fs";
import { extname, dirname } from "node:path";
import { createHash } from "node:crypto";

// ─── 配置 ───────────────────────────────────────────────────────────────────────

const CONFIG = {
  noBlock: process.env.AI_REVIEW_NO_BLOCK === "true",
  passedFile: ".trae/review-passed",
  maxDiffSize: 200 * 1024, // 200KB
};

// 只审查的文件扩展名
const REVIEW_EXTENSIONS = new Set([
  ".java", ".kt", ".py", ".go", ".rs", ".ts", ".tsx", ".js", ".jsx",
  ".mjs", ".cjs", ".vue", ".css", ".less", ".scss", ".sql", ".xml",
  ".yaml", ".yml", ".sh", ".bash", ".toml", ".properties",
]);

// 不审查的路径模式
const IGNORE_PATTERNS = [
  /node_modules/, /package-lock/, /pnpm-lock/, /yarn\.lock/,
  /\.git\//, /dist\//, /build\//, /target\//, /\.min\./,
  /\.d\.ts$/, /auto-generated/i, /codegen/i,
];

// ─── 工具函数 ────────────────────────────────────────────────────────────────────

function log(level, msg) {
  const colors = { info: "\x1b[36m", warn: "\x1b[33m", error: "\x1b[31m", success: "\x1b[32m", dim: "\x1b[2m" };
  const reset = "\x1b[0m";
  const prefix = { info: "ℹ", warn: "⚠", error: "✖", success: "✔", dim: "→" };
  console.log(`${colors[level] || ""}${prefix[level] || " "} ${msg}${reset}`);
}

function shouldReview(filePath) {
  const ext = extname(filePath);
  if (!REVIEW_EXTENSIONS.has(ext)) return false;
  if (IGNORE_PATTERNS.some((p) => p.test(filePath))) return false;
  return true;
}

function cleanFile(filePath) {
  try {
    if (existsSync(filePath)) {
      unlinkSync(filePath);
      return true;
    }
  } catch { /* skip */ }
  return false;
}

function cleanAll() {
  const results = [];
  const c = cleanFile(CONFIG.passedFile);
  if (c) results.push(CONFIG.passedFile);
  return results;
}

// 计算 diff 哈希，用于 review-passed 完整性校验
function diffHash(diffContent) {
  return createHash("sha256").update(diffContent).digest("hex").slice(0, 16);
}

// 检查 review-passed 标记是否有效（包含正确的 diff 哈希）
function checkPassedMarker(currentDiffContent) {
  if (!existsSync(CONFIG.passedFile)) return false;
  try {
    const data = JSON.parse(readFileSync(CONFIG.passedFile, "utf-8"));
    const expectedHash = diffHash(currentDiffContent);
    return data.diffHash === expectedHash;
  } catch {
    return false;
  }
}

// 创建 review-passed 标记（审查通过后由 Skill 调用）
// 计算当前暂存区 diff 的哈希并写入标记文件，下次 commit 时 hook 校验哈希一致则放行
function writePassedMarker(diffContent) {
  const markerData = {
    diffHash: diffHash(diffContent),
    timestamp: new Date().toISOString(),
  };
  const dir = dirname(CONFIG.passedFile);
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
  writeFileSync(CONFIG.passedFile, JSON.stringify(markerData, null, 2), "utf-8");
  return markerData.diffHash;
}

// ─── Git 操作 ────────────────────────────────────────────────────────────────────

function getDiff(from, to) {
  try {
    if (from === "staged") {
      // 首次 commit 时没有 HEAD，用 --cached 即可（不需要比较对象）
      return execSync("git diff --cached", { encoding: "utf-8", maxBuffer: 10 * 1024 * 1024 });
    }
    const range = to ? `${from}...${to}` : `${from}...HEAD`;
    return execSync(`git diff ${range}`, { encoding: "utf-8", maxBuffer: 10 * 1024 * 1024 });
  } catch {
    return "";
  }
}

function parseDiff(diffContent) {
  const files = [];
  let currentFile = null;
  let currentContent = [];

  for (const line of diffContent.split("\n")) {
    if (line.startsWith("diff --git")) {
      if (currentFile) {
        files.push({ path: currentFile, diff: currentContent.join("\n") });
      }
      const parts = line.split(" b/");
      currentFile = parts.length > 1 ? parts[1].trim() : "";
      currentContent = [line];
    } else if (currentFile) {
      currentContent.push(line);
    }
  }
  if (currentFile) {
    files.push({ path: currentFile, diff: currentContent.join("\n") });
  }

  return files.filter((f) => shouldReview(f.path));
}

// ─── 主流程 ──────────────────────────────────────────────────────────────────────

function parseArgs() {
  const args = process.argv.slice(2);
  const opts = { from: "staged", to: "", noBlock: CONFIG.noBlock, clean: false, markPassed: false };
  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case "--from": opts.from = args[++i]; break;
      case "--to": opts.to = args[++i]; break;
      case "--staged": opts.from = "staged"; break;
      case "--no-block": opts.noBlock = true; break;
      case "--clean": opts.clean = true; break;
      case "--mark-passed": opts.markPassed = true; break;
    }
  }
  return opts;
}

function main() {
  const opts = parseArgs();

  // --clean: 清理 passed 标记文件后退出
  if (opts.clean) {
    const cleaned = cleanAll();
    if (cleaned.length > 0) {
      log("success", `已清理: ${cleaned.join(", ")}`);
    } else {
      log("info", "无需清理，无残留文件");
    }
    process.exit(0);
  }

  // --mark-passed: 审查通过后创建放行标记（由 git-commit-push Skill 调用）
  if (opts.markPassed) {
    const diffContent = getDiff(opts.from, opts.to);
    if (!diffContent.trim()) {
      log("warn", "无暂存区变更，无需创建审查通过标记");
      process.exit(1);
    }
    const hash = writePassedMarker(diffContent);
    log("success", `已创建审查通过标记 ${CONFIG.passedFile} (diffHash: ${hash})`);
    process.exit(0);
  }

  console.log("\n🤖 AI Code Review (pre-commit)");
  console.log("━".repeat(50));

  // 1. 获取 diff
  log("info", `获取代码变更 (${opts.from === "staged" ? "暂存区" : `${opts.from}...${opts.to || "HEAD"}`})`);
  const diffContent = getDiff(opts.from, opts.to);

  // 2. 检查审查通过标记：diff 哈希一致则放行
  if (diffContent.trim() && checkPassedMarker(diffContent)) {
    log("success", "AI Code Review 已通过（review-passed 标记验证通过），放行提交");
    cleanAll();
    process.exit(0);
  }

  // 标记存在但哈希不匹配（代码被修改），清理无效标记
  if (existsSync(CONFIG.passedFile)) {
    log("warn", "review-passed 标记与当前 diff 不匹配（代码已变更），需要重新审查");
    cleanFile(CONFIG.passedFile);
  }

  if (!diffContent.trim()) {
    log("success", "无代码变更，跳过审查");
    process.exit(0);
  }

  // 3. 解析文件（仅用于控制台展示待审查文件列表）
  const files = parseDiff(diffContent);
  if (files.length === 0) {
    log("success", "无可审查的代码文件（已过滤非代码文件）");
    process.exit(0);
  }

  log("info", `待审查文件: ${files.length} 个`);
  for (const f of files) {
    log("dim", `  ${f.path} (${f.diff.split("\n").length} 行)`);
  }

  // 4. 检查 diff 是否过大
  const totalSize = Buffer.byteLength(diffContent, "utf-8");
  if (totalSize > CONFIG.maxDiffSize) {
    log("warn", `Diff 大小 ${(totalSize / 1024).toFixed(0)}KB 超过 ${CONFIG.maxDiffSize / 1024}KB 限制，可能影响审查质量`);
  }

  // 5. 阻断提交（等待 Skill 审查后用户修复再重新提交）
  if (!opts.noBlock) {
    console.log("━".repeat(50));
    log("warn", "提交已暂停！请在 Trae 对话框中输入「提交代码」完成审查和提交");
    log("info", "禁止直接使用 git commit --no-verify 绕过审查");
    log("dim", "紧急情况除外：git commit --no-verify -m \"your message\"");
    process.exit(1);
  } else {
    log("info", "--no-block 模式，仅收集变更不阻断提交");
    process.exit(0);
  }
}

main().catch((err) => {
  log("error", `AI Code Review 执行失败: ${err.message}`);
  process.exit(0); // 不阻断 git 操作
});
