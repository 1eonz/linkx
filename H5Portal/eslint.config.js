import typescript from '@typescript-eslint/eslint-plugin'
import tsParser from '@typescript-eslint/parser'
import prettierConfig from 'eslint-config-prettier'
import importPlugin from 'eslint-plugin-import'
import vue from 'eslint-plugin-vue'
import vueParser from 'vue-eslint-parser'

export default [
  {
    files: ['**/*.{js,mjs,cjs,ts,mts,cts}'],
    ignores: [
      'node_modules',
      'dist',
      'dist-ssr',
      '*.local',
      'static',
      'mock',
      '.vscode',
      '.idea',
      '*.log',
      'public/**/*',
      'src/static/**/*',
      'src/common/utils/x-utils/js_sdk/*',
      'unpackage/**'
    ],
    languageOptions: {
      ecmaVersion: 2021,
      sourceType: 'module',
      parser: tsParser,
      globals: {
        browser: true,
        node: true
      }
    },
    plugins: {
      '@typescript-eslint': typescript,
      import: importPlugin
    },
    rules: {
      '@typescript-eslint/no-unused-vars': 'error',
      'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
      'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
      // Import order rules
      'import/order': [
        'error',
        {
          groups: [
            ['builtin', 'external'],
            ['internal', 'parent', 'sibling', 'index']
          ],
          'newlines-between': 'always',
          alphabetize: {
            order: 'asc',
            caseInsensitive: true
          }
        }
      ],
      'import/first': 'error',
      'import/no-duplicates': 'error'
    }
  },
  {
    files: ['**/*.vue'],
    ignores: [
      'node_modules',
      'dist',
      'dist-ssr',
      '*.local',
      'public/**/*',
      'static',
      'src/static/**/*',
      'mock',
      '.vscode',
      '.idea',
      '*.log'
    ],
    languageOptions: {
      ecmaVersion: 2021,
      sourceType: 'module',
      parser: vueParser,
      parserOptions: {
        parser: tsParser,
        ecmaVersion: 2021,
        sourceType: 'module'
      },
      globals: {
        browser: true,
        node: true
      }
    },
    plugins: {
      vue,
      import: importPlugin
    },
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/no-unused-vars': 'error',
      'vue/no-unused-components': 'error',
      'no-console': process.env.NODE_ENV === 'production' ? 'error' : 'off',
      'no-debugger': process.env.NODE_ENV === 'production' ? 'error' : 'off',
      // Import order rules
      'import/order': [
        'error',
        {
          groups: [
            ['builtin', 'external'],
            ['internal', 'parent', 'sibling', 'index']
          ],
          'newlines-between': 'always',
          alphabetize: {
            order: 'asc',
            caseInsensitive: true
          }
        }
      ],
      'import/first': 'error',
      'import/no-duplicates': 'error'
    }
  },
  // Prettier config to disable eslint rules that conflict with prettier
  prettierConfig
]