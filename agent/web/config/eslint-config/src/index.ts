import type { Linter } from 'eslint';

import {
  command,
  comments,
  disableds,
  ignores,
  importPluginConfig,
  javascript,
  node,
  perfectionist,
  prettier,
  // regexp,
  typescript,
  unicorn,
  vue,
} from './config';

type FlatConfig = Linter.Config;

type FlatConfigPromise = FlatConfig | FlatConfig[] | Promise<FlatConfig> | Promise<FlatConfig[]>;

async function defineConfig(config: FlatConfig[] = []) {
  const configs: FlatConfigPromise[] = [
    vue(),
    javascript(),
    ignores(),
    prettier(),
    typescript(),
    disableds(),
    importPluginConfig(),
    node(),
    perfectionist(),
    comments(),
    unicorn(),
    // regexp(),
    command(),
    ...config,
  ];

  const resolved = await Promise.all(configs);

  return resolved.flat();
}

export { defineConfig };
