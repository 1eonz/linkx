# Lint 配置规范

> 本文件合并自原 ESLint / Prettier / Stylelint 三份规则，记录 4 个前端项目的配置差异，避免跨项目套用规则。

## 一、ESLint

### 1.1 配置体系

| 项目 | 配置格式 | 核心插件 |
|------|----------|----------|
| H5Portal | `eslint.config.js` (flat) | vue、@typescript-eslint、import、eslint-config-prettier |
| web/agent/web | `eslint.config.mjs` (flat) | @cs/eslint-config（vue/js/ts/import/prettier/unicorn/node/perfectionist/comments/command/unused-imports） |
| admin | `.eslintrc.js` (legacy) | babel-eslint、eslint-plugin-vue（Vue2，`plugin:vue/recommended` + `eslint:recommended`） |

### 1.2 通用规则（按项目生效情况）

> 各项目 ESLint 配置差异较大，下表标注每条规则的**实际生效范围**，不要假设全局统一。

| 规则 | web/agent/web | H5Portal | admin |
|------|---------------|----------|-------|
| 禁止 `var`，用 `let`/`const` | ✅ `no-var: error` | ❌ 未设置 | ✅ `no-var: 2` |
| `===`/`!==` 强等 | ✅ `eqeqeq: error`（严格） | ❌ 未设置 | ⚠️ `eqeqeq: ['error','always',{null:'ignore'}]`（null 比较允许 `==`） |
| `console` | ❌ `no-console: off`（**允许**，不限环境） | ⚠️ 仅生产环境 error | ❌ `off`（允许） |
| `debugger` | ✅ `no-debugger: error`（全环境） | ⚠️ 仅生产环境 error | ⚠️ 仅生产环境 2 |
| 未使用变量 | ✅ TS `argsIgnorePattern: '^_'`（`_` 前缀忽略） | ⚠️ `@typescript-eslint/no-unused-vars: error`，**不忽略 `_` 前缀** | ⚠️ `no-unused-vars: error`，`args: 'none'`，**不忽略 `_` 前缀** |
| 对象属性简写 | ✅ `object-shorthand: ['error','always']` + `vue/object-shorthand` | ❌ 未设置 | ❌ 未设置 |
| import 顶部、禁止重复 | ✅ import 插件 | ✅ `import/first`、`import/no-duplicates` | ❌ 未设置 |
| `prefer-const` | ❌ `off` | ❌ 未设置 | ✅ `2` |

补充:
- web/agent/web 由 `@cs/eslint-config` 统一管理，含 `eslint-plugin-unused-imports`（当前 `unused-imports/no-unused-imports: off`，未启用自动删除）、`unicorn`、`node`、`comments`、`command` 等附加规则集。
- H5Portal flat config 仅显式设置少量规则（no-console/no-debugger、import/order、@typescript-eslint/no-unused-vars），未启用 eqeqeq/object-shorthand/no-var 等，修改 H5Portal 代码时不要套用 web/ 的规则假设。

### 1.3 Vue3 专项（web/agent/web）

- SFC 块顺序: `script` → `template` → `style`（`vue/block-order`）
- 宏定义顺序: `defineOptions` → `defineProps` → `defineEmits` → `defineSlots`（`vue/define-macros-order`）
- 模板中组件名 PascalCase（`vue/component-name-in-template-casing`）
- 显式声明 emits（`vue/require-explicit-emits`）
- HTML 属性双引号（`vue/html-quotes: double`），void/组件标签自闭合（`vue/html-self-closing`）
- 允许 `v-html`（`vue/no-v-html: off`）、允许单词组件名（`vue/multi-word-component-names: off`）
- 属性按 `vue/attributes-order` 排序（DEFINITION → LIST_RENDERING → ... → CONTENT）

> H5Portal 的 vue 规则仅开 `vue/multi-word-component-names: off`、`vue/no-unused-vars`、`vue/no-unused-components`，未启用上述 block-order/define-macros-order 等 strongly-recommended 规则；新增 H5Portal 代码仍建议遵循约定（与 `.trae/rules/frontend.md` §13 一致），但不会在 lint 阶段强制。
> admin 用 `plugin:vue/recommended`（Vue2），不适用本节。

### 1.4 TypeScript 规则（web/agent/web）

- 允许 `any`（`@typescript-eslint/no-explicit-any: off`，渐进式迁移）
- 未使用 TS 变量报错，`_` 前缀忽略
- 允许非空断言（`no-non-null-assertion: off`）、命名空间（`no-namespace: off`）
- 不强制 `interface`/`type` 选择（`consistent-type-definitions: off`）
- `ban-ts-comment`: `ts-expect-error`/`ts-ignore`/`ts-nocheck` 需带描述

### 1.5 Import 排序

**H5Portal**（`eslint-plugin-import`，`import/order`）:
`[builtin, external]` → 空行 → `[internal, parent, sibling, index]`，字母升序（`alphabetize.order: asc`）。

**web/agent/web**（`eslint-plugin-perfectionist`，`sort-imports`，自然升序）:
`[external-type, builtin-type, type]` → 空行 → `[parent-type, sibling-type, index-type]` → 空行 → `[internal-type]` → 空行 → `builtin` → 空行 → `vue` → 空行 → `@`（别名） → 空行 → `external` → 空行 → `internal` → 空行 → `[parent, sibling, index]` → 空行 → `side-effect` / `side-effect-style` / `style` / `object` / `unknown`。

> 两个体系排序插件不同，修改代码时遵循对应项目规则，不要混用。admin 未配置 import 排序。

## 二、Prettier

### 2.1 通用规则（H5Portal/web/agent/web）

> web/agent/web 通过 `@cs/prettier-config`（`web/.prettierrc.mjs` → `export { default } from '@cs/prettier-config'`）下发；H5Portal 用 `.prettierrc.cjs`。下表「显式设置」列标注是否在配置文件中显式声明，「默认」表示依赖 Prettier 默认值。

| 选项 | H5Portal | web/agent/web（@cs/prettier-config） | 是否显式 |
|------|----------|--------------------------------------|----------|
| printWidth | 100 | 100 | ✅ |
| tabWidth | 2 | 2（默认） | H5Portal ✅ / web 默认 |
| useTabs | false | false（默认） | H5Portal ✅ / web 默认 |
| semi | true | true | ✅ |
| singleQuote | true | true | ✅ |
| trailingComma | 'all' | 'all' | ✅ |
| bracketSpacing | true | true（默认） | H5Portal ✅ / web 默认 |
| arrowParens | 'always' | 'always'（默认） | H5Portal ✅ / web 默认 |
| endOfLine | 'auto' | 'auto' | ✅ |
| vueIndentScriptAndStyle | true | true | ✅ |
| htmlWhitespaceSensitivity | 'css' | 'strict' | ✅ |
| proseWrap | 'preserve' | 'never' | ✅ |

### 2.2 cloudcmd-admin-web 差异

- admin `.prettierrc` 仅设置 `tabWidth: 2`、`singleQuote: true`、`semi: false`，其余全部走 Prettier 默认（如 `printWidth` 默认 80，非 100）。
- `semi: false`（不加分号，与其他项目不一致，修改 admin 代码时需注意）。

### 2.3 忽略项

dist/、dist-ssr/、node_modules/、public/、static/、*.log、*.local、.vscode/、.idea/、**/*.svg、**/*.sh

### 2.4 兼容性

- H5Portal: eslint-config-prettier（仅关闭冲突规则）
- web/agent/web: eslint-plugin-prettier（Prettier 作为 ESLint 规则运行）+ stylelint-prettier
- 修改 ESLint/Stylelint 规则时，需确保不与 Prettier 冲突

## 三、Stylelint

### 3.1 配置体系

| 项目 | 体系 | Stylelint | 配置入口 | 核心 extends / plugins |
|------|------|-----------|----------|------------------------|
| H5Portal | SCSS | 17 | `stylelint.config.js` | `stylelint-config-standard` + `stylelint-config-standard-scss` |
| web/ | Less | 16 | `stylelint.config.mjs` → `@cs/stylelint-config` | `stylelint-config-standard` + `stylelint-config-recess-order` + `stylelint-config-recommended-less` + `stylelint-config-recommended-vue`；plugins: `stylelint-order`、`@stylistic/stylelint-plugin`、`stylelint-prettier`、`stylelint-less` |
| agent/web/ | Less | 16 | `stylelint.config.mjs` → `@cs/stylelint-config` | 同 web/，但**本地 `@cs/stylelint-config` 内容与 web/ 不一致**（见下表） |
| admin | — | — | 无 Stylelint | 不参与 lint |

> web/ 与 agent/web/ 各自维护一份 `config/stylelint-config/index.mjs`，两者规则**不完全相同**，不要视为同一份配置。

### 3.2 web/ vs agent/web/ 差异

| 规则 | web/ | agent/web/ |
|------|------|------------|
| `selector-class-pattern` | `null`（允许任意类名） | **BEM 正则强约束**: `^(?:(?:o\|c\|u\|t\|s\|is\|has\|_\|js\|qa)-)?[a-zA-Z0-9]+(?:-[a-zA-Z0-9]+)*...` |
| `no-duplicate-selectors` | `null`（允许重复选择器） | 未关闭（默认 `true`，会报重复选择器） |
| `no-empty-source` | `null` | 未关闭 |
| `number-max-precision` | `null` | 未设置 |
| Vue `:is-hover-shadow` 伪类 | 允许 | **不允许**（`ignorePseudoClasses` 仅 `['global','deep']`） |
| `ignoreFiles` | 额外忽略 `src/config/bridge/**/*.js` | 仅忽略 `*.{js,jsx,tsx,ts,json,md}` |
| `at-rule-no-unknown` | 单份 | **重复定义两次**（配置 bug，行为以第二条为准，建议清理） |

### 3.3 通用规则（三者一致部分）

- `selector-class-pattern: null`（仅 web/ 与 H5Portal；agent/web/ 见上表）
- `custom-property-pattern: null` — 允许自定义属性命名
- `no-descending-specificity: null`（web/；agent/web/ 与 H5Portal 未关闭）

### 3.4 CSS 属性排序（web/、agent/web/）

使用 `stylelint-config-recess-order` + `order/order`，顺序:
1. `$` 变量 → 2. 自定义属性 → 3. `@` 规则 → 4. 声明 → 5. `@supports` → 6. `@media` → 7. `@include` → 8. 嵌套规则

### 3.5 Vue 深度选择器

| 项目 | 允许的伪类/伪元素 |
|------|------------------|
| web/ | `:global`、`:deep`、`:is-hover-shadow`；`::v-deep`、`::v-global`、`::v-slotted`、`::input-placeholder` |
| agent/web/ | `:global`、`:deep`；`::v-deep`、`::v-global`、`::v-slotted`、`::input-placeholder`（**不含 `:is-hover-shadow`**） |
| H5Portal | 未配置 `selector-pseudo-*-no-unknown` 白名单，使用非标准伪类可能报错，按需在本地配置补充 |

### 3.6 H5Portal 专项

- `color-function-notation: 'legacy'`（允许 `rgb()`/`hsl()` 旧语法）
- `unit-allowed-list: null`（允许任意单位）
- `declaration-no-important: null`（允许 `!important`）
- `scss/no-global-function-names: true`（禁止 SCSS 中使用与全局函数同名）
- `scss/selector-no-union-class-name: null`、`selector-id-pattern: null`
- **未集成 `stylelint-prettier`**，Stylelint 与 Prettier 独立运行

### 3.7 忽略项

- web/agent/web: `**/*.{js,jsx,tsx,ts,json,md}`（web/ 另含 `src/config/bridge/**/*.js`）
- H5Portal: `node_modules`、`dist`、`dist-ssr`、`*.local`、`public`、`static`、`mock`、`.vscode`、`.idea`、`*.log`、`src/static`

### 3.8 兼容性

- web/agent/web 使用 `stylelint-prettier`（`prettier/prettier: true`），修改 Stylelint 规则时需确保不与 Prettier 冲突。
- H5Portal 未集成 `stylelint-prettier`，无冲突顾虑。
- admin 不参与 Stylelint。
