export default {
  extends: ['stylelint-config-standard', 'stylelint-config-standard-scss'],
  ignoreFiles: [
    'node_modules/**',
    'dist/**',
    'dist-ssr/**',
    '*.local',
    'public/**',
    'static/**',
    'mock/**',
    '.vscode/**',
    '.idea/**',
    '*.log',
    'src/static/**',
  ],
  rules: {
    // 允许使用自定义属性
    'custom-property-pattern': null,
    // 允许使用 BEM 风格的类名
    'selector-class-pattern': null,
    // 允许使用 ID 选择器
    'selector-id-pattern': null,
    // 允许使用嵌套选择器的深度
    'scss/selector-no-union-class-name': null,
    // 允许使用 !important
    'declaration-no-important': null,
    // 允许使用特定的颜色格式
    'color-function-notation': 'legacy',
    'unit-allowed-list': null,
    'scss/no-global-function-names': true,
  },
};
