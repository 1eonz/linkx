import type { Linter } from 'eslint';

import { interopDefault } from '../utils';

export async function regexp(): Promise<Linter.Config[]> {
  const [pluginRegexp] = await Promise.all([
    interopDefault(import('eslint-plugin-regexp')),
  ] as const);

  return [
    {
      plugins: {
        regexp: pluginRegexp,
      },
      rules: {
        ...pluginRegexp.configs.recommended.rules,
        'prefer-regex-literals': ['error', { disallowRedundantWrapping: true }],
      },
    },
  ];
}
