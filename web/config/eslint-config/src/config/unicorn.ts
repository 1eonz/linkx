import type { Linter } from 'eslint';

import { interopDefault } from '../utils';

export async function unicorn(): Promise<Linter.Config[]> {
  const [pluginUnicorn] = await Promise.all([
    interopDefault(import('eslint-plugin-unicorn')),
  ] as const);

  return [
    {
      plugins: {
        unicorn: pluginUnicorn,
      },
      rules: {
        ...pluginUnicorn.configs.recommended.rules,
        'unicorn/better-regex': 'off',
        'unicorn/consistent-destructuring': 'off',
        'unicorn/consistent-function-scoping': 'off',
        'unicorn/filename-case': 'off',
        'unicorn/import-style': 'off',
        'unicorn/no-array-for-each': 'off',
        'unicorn/no-array-push-push': 'off',
        'unicorn/no-array-reduce': 'off',
        'unicorn/no-invalid-remove-event-listener': 'off',
        'unicorn/no-nested-ternary': 'off',
        'unicorn/no-null': 'off',
        'unicorn/no-this-assignment': 'off',
        'unicorn/no-unsafe-optional-chaining': 'off',
        'unicorn/no-useless-undefined': 'off',
        'unicorn/prefer-add-event-listener': 'off',
        'unicorn/prefer-array-index-of': 'off',
        'unicorn/prefer-at': 'off',
        'unicorn/prefer-code-point': 'off',
        'unicorn/prefer-dom-node-text-content': 'off',
        'unicorn/prefer-export-from': ['error', { ignoreUsedVariables: true }],
        'unicorn/prefer-global-this': 'off',
        'unicorn/prefer-logical-operator-over-ternary': 'off',
        'unicorn/prefer-node-protocol': 'off',
        'unicorn/prefer-number-properties': 'off',
        'unicorn/prefer-query-selector': 'off',
        'unicorn/prefer-set-has': 'off',
        'unicorn/prefer-spread': 'off',
        'unicorn/prefer-string-slice': 'off',
        'unicorn/prefer-ternary': 'off',
        'unicorn/prefer-top-level-await': 'off',
        'unicorn/prevent-abbreviations': 'off',
      },
    },
    {
      files: ['scripts/**/*.?([cm])[jt]s?(x)', 'internal/**/*.?([cm])[jt]s?(x)'],
      rules: {
        'unicorn/no-process-exit': 'off',
      },
    },
  ];
}
